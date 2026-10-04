package dev.renkinProject.renkin.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

enum class AiBatchStatus { PENDING, REQUESTING, GENERATED, SUCCEEDED, FAILED, SKIPPED }
data class AiBatchItem(val key: String, val name: String, val instruction: String = "",
    val status: AiBatchStatus = AiBatchStatus.PENDING, val fileName: String? = null, val error: String? = null)
data class AiBatchSession(val profileId: Long, val settings: AiSettings, val items: List<AiBatchItem>,
    val message: String? = null) {
    val done: Int get() = items.count { it.status in setOf(AiBatchStatus.SUCCEEDED, AiBatchStatus.FAILED, AiBatchStatus.SKIPPED) }
}
enum class AiBatchTarget { MISSING, FILLED, UNAVAILABLE, PROFILE_CHANGED }

/** One request at a time. Durable state is written before a billable call and after every result.
 * No automatic retries, network/background scheduling, or work on construction. */
class AiBatchRunner(
    initial: AiBatchSession,
    private val persist: suspend (AiBatchSession) -> Unit,
    private val target: suspend (String) -> AiBatchTarget,
    private val cached: suspend (AiBatchItem) -> String?,
    private val generate: suspend (AiBatchItem) -> String,
    private val apply: suspend (AiBatchItem) -> AiBatchTarget,
    private val connected: () -> Boolean
) {
    private val mutable = MutableStateFlow(initial)
    val state = mutable.asStateFlow()
    private var pauseRequested = false
    private var running = false
    fun pause() { pauseRequested = true }
    private suspend fun publish(value: AiBatchSession) { persist(value); mutable.value = value }
    private suspend fun item(index: Int, transform: (AiBatchItem) -> AiBatchItem) {
        val value = mutable.value
        publish(value.copy(items = value.items.mapIndexed { i, it -> if (i == index) transform(it) else it }))
    }
    suspend fun record() { publish(mutable.value) }
    suspend fun recoverInterrupted() {
        check(!running)
        for ((index, value) in mutable.value.items.withIndex()) {
            if (value.status == AiBatchStatus.REQUESTING) {
                val availability = target(value.key)
                if (availability == AiBatchTarget.FILLED || availability == AiBatchTarget.UNAVAILABLE) {
                    item(index) { it.copy(status = AiBatchStatus.SKIPPED, error = "Target changed during interruption; no request was retried.") }
                    continue
                }
                val saved = try { cached(value) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
                item(index) { it.copy(status = if (saved == null) AiBatchStatus.FAILED else AiBatchStatus.GENERATED,
                    fileName = saved, error = if (saved == null)
                        "Interrupted request: billing is uncertain. Check usage before explicitly retrying." else null) }
            }
        }
    }
    suspend fun retry(key: String) {
        check(!running)
        val index = mutable.value.items.indexOfFirst { it.key == key && it.status == AiBatchStatus.FAILED }
        if (index >= 0) {
            val saved = if (target(key) == AiBatchTarget.MISSING) cached(mutable.value.items[index]) else null
            item(index) { it.copy(status = AiBatchStatus.PENDING, fileName = saved, error = null) }
        }
    }
    suspend fun updateInstruction(key: String, instruction: String) {
        check(!running)
        require(instruction.length <= 2000)
        val index = mutable.value.items.indexOfFirst { it.key == key && it.status in setOf(AiBatchStatus.PENDING, AiBatchStatus.FAILED) }
        if (index >= 0) item(index) {
            if (it.instruction == instruction) it else it.copy(instruction = instruction, fileName = null)
        }
    }
    suspend fun cancelRemaining() {
        check(!running)
        publish(mutable.value.copy(message = "Cancelled. Saved results remain available.", items = mutable.value.items.map {
            if (it.status == AiBatchStatus.PENDING) it.copy(status = AiBatchStatus.SKIPPED, error = "Cancelled before request") else it
        }))
    }
    suspend fun run(onlyKey: String? = null) {
        check(!running)
        running = true; pauseRequested = false
        try {
            publish(mutable.value.copy(message = null))
            for (index in mutable.value.items.indices) {
                if (pauseRequested) break
                var value = mutable.value.items[index]
                if (onlyKey != null && value.key != onlyKey) continue
                if (value.status !in setOf(AiBatchStatus.PENDING, AiBatchStatus.GENERATED)) continue
                when (target(value.key)) {
                    AiBatchTarget.PROFILE_CHANGED -> { publish(mutable.value.copy(message = "Profile changed. Reopen this profile to resume.")); break }
                    AiBatchTarget.FILLED, AiBatchTarget.UNAVAILABLE -> {
                        item(index) { it.copy(status = AiBatchStatus.SKIPPED, error = "Already has an icon, is locked, or is no longer installed") }; continue
                    }
                    AiBatchTarget.MISSING -> Unit
                }
                try {
                    val saved = value.fileName ?: cached(value)
                    if (saved == null) {
                        if (!connected()) { publish(mutable.value.copy(message = "No internet connection. Resume when connected.")); break }
                        item(index) { it.copy(status = AiBatchStatus.REQUESTING, error = null) }
                        val name = generate(mutable.value.items[index])
                        // A local cancellation must not discard a successfully returned PNG name.
                        withContext(NonCancellable) { item(index) { it.copy(status = AiBatchStatus.GENERATED, fileName = name) } }
                    } else item(index) { it.copy(status = AiBatchStatus.GENERATED, fileName = saved, error = null) }
                    value = mutable.value.items[index]
                    withContext(NonCancellable) {
                        when (apply(value)) {
                            AiBatchTarget.MISSING -> item(index) { it.copy(status = AiBatchStatus.SUCCEEDED) }
                            AiBatchTarget.PROFILE_CHANGED -> { pauseRequested = true; publish(mutable.value.copy(message = "Profile changed. The PNG is saved; reopen this profile to apply it.")) }
                            else -> item(index) { it.copy(status = AiBatchStatus.SKIPPED, error = "Target changed. The PNG remains in Upload.") }
                        }
                    }
                } catch (e: CancellationException) {
                    withContext(NonCancellable) {
                        if (mutable.value.items[index].status == AiBatchStatus.REQUESTING)
                            item(index) { it.copy(status = AiBatchStatus.FAILED, error = "Request cancelled locally; it may still be billed. Check usage before retrying.") }
                    }
                    throw e
                } catch (e: Exception) {
                    item(index) { it.copy(status = AiBatchStatus.FAILED, error = e.message?.take(400) ?: "Generation failed") }
                    if (e is IOException) { publish(mutable.value.copy(message = "Paused after an API/network error. No automatic retry was made.")); break }
                }
            }
            if (pauseRequested && mutable.value.message == null) publish(mutable.value.copy(message = "Paused. The current icon is saved; Resume continues the remaining items."))
        } finally { running = false }
    }
}
