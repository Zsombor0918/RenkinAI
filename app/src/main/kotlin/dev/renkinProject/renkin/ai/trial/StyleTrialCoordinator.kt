package dev.renkinProject.renkin.ai.trial

import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.ai.style.PromptComposer
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class StyleTrialCoordinator(private val journal: StyleTrialJournal, private val gateway: IconGenerationGateway,
    private val repository: ProjectAiRepository, private val fault: (String) -> Unit = {}) {
    suspend fun submit(plan: StyleTrialPlan, images: Map<String, ByteArray>, confirmed: Boolean,
        current: suspend () -> Boolean, changed: suspend () -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!confirmed || !current()) return@withContext
        if (!journal.claim(plan, images)) return@withContext
        try {
            for (input in plan.inputs) {
                currentCoroutineContext().ensureActive()
                if (!current()) { journal.skipPending(plan, "stale"); break }
                journal.begin(plan, input.requestId); changed()
                try {
                    val result = gateway.generate(IconGenerationRequest(journal.image(plan.context.profileId, input.originalHash),
                        plan.referenceHashes.map { journal.image(plan.context.profileId, it) }, plan.specification,
                        provider = plan.context.provider, model = plan.context.model, quality = plan.context.quality,
                        requestId = input.requestId, profileId = plan.context.profileId, styleRevision = plan.context.revision,
                        composedPrompt = if (plan.context.provider == AiSettings.OPENAI)
                            PromptComposer.openAiTrialAppearance(plan.specification, plan.instruction)
                            else PromptComposer.composeSaved(plan.specification, plan.instruction),
                        beforeEndpoint = { step -> check(current()) { "Trial input changed." }; journal.endpoint(plan, input.requestId, step) }))
                    if (result is IconGenerationOutcome.Generated) withContext(NonCancellable) {
                        require(result.requestId == input.requestId && result.provider == plan.context.provider)
                        val calls = checkNotNull(journal.read(plan.context.profileId, plan.id)).items.single { it.requestId == input.requestId }.endpointCalls
                        require(calls == if (plan.context.provider == AiSettings.STABILITY) 2 else 1)
                        journal.stage(plan, input.requestId, result.png, fault)
                        persist(plan, input)
                    } else {
                        journal.failure(plan, input.requestId, when (result) {
                            is IconGenerationOutcome.InvalidInput -> "invalid_input"
                            is IconGenerationOutcome.CredentialsUnavailable -> "no_key"
                            is IconGenerationOutcome.Uncertain -> result.reason
                            else -> "remote_outcome_unknown"
                        })
                        journal.skipPending(plan, "previous_request_failed"); break
                    }
                } catch (cancel: CancellationException) {
                    withContext(NonCancellable) { journal.failure(plan, input.requestId, "cancelled"); journal.skipPending(plan, "stopped") }
                    throw cancel
                } catch (_: Exception) {
                    withContext(NonCancellable) { journal.failure(plan, input.requestId, "interrupted_or_storage"); journal.skipPending(plan, "previous_request_failed") }
                    break
                } finally { changed() }
            }
        } finally {
            withContext(NonCancellable) { journal.skipPending(plan, "interrupted"); journal.release(plan); changed() }
        }
    }
    private suspend fun persist(plan: StyleTrialPlan, input: TrialInput) {
        check(journal.current(plan)) { "Restored project data cannot inherit an old request." }
        val png = journal.result(plan.context.profileId, input.requestId)
        val item = checkNotNull(journal.read(plan.context.profileId, plan.id)).items.single { it.requestId == input.requestId }
        require(sha256(png) == item.resultHash)
        repository.installTrialVariant(plan, input, journal.image(plan.context.profileId, input.originalHash), png,
            { journal.current(plan) }, fault)
        journal.reviewable(plan, input.requestId)
    }
    /** Reopening repairs file→Room→marker gaps. It never calls the gateway. */
    suspend fun recover(id: Long) = withContext(Dispatchers.IO) {
        journal.recoverRecords(id).filter { journal.current(it.plan) && !journal.isActive(it.plan) }.forEach { r ->
            r.items.filter { it.status == TrialStatus.PNG_SAVED }.forEach { item ->
                try { persist(r.plan, r.plan.inputs.single { it.requestId == item.requestId }) }
                catch (_: Exception) { journal.failure(r.plan, item.requestId, "local_persistence") }
            }
        }
        if (journal.records(id).none { journal.isActive(it.plan) }) journal.collectCompletedInputs(id)
    }
}
