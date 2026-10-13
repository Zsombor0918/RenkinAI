package dev.renkinProject.renkin.ai.trial

import android.content.Context
import android.util.AtomicFile
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.inspectPng
import dev.renkinProject.renkin.data.project.readBounded
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import java.io.File
import java.util.UUID

internal enum class TrialStatus { PENDING, REQUESTING, PNG_SAVED, REVIEWABLE, FAILED, UNCERTAIN, SKIPPED }
internal data class TrialItemRecord(val requestId: String, val status: TrialStatus = TrialStatus.PENDING,
    val endpointCalls: Int = 0, val resultHash: String? = null, val reason: String? = null) {
    fun json() = json("requestId" to requestId, "status" to status.name, "endpointCalls" to endpointCalls,
        "resultHash" to resultHash, "reason" to reason)
}
internal data class StyleTrialRecord(val plan: StyleTrialPlan, val confirmedAt: Long,
    val prepared: Boolean, val items: List<TrialItemRecord>) {
    fun encode() = canonicalJson(json("schema" to 1, "plan" to plan.encode(), "fingerprint" to plan.fingerprint,
        "confirmedAt" to confirmedAt, "prepared" to prepared, "items" to JSONArray(items.map { it.json() })))
    companion object {
        fun parse(text: String): StyleTrialRecord {
            val r = strictObject(text, 65536, 12); require(r.longValue("schema") == 1L)
            r.exactKeys(setOf("schema", "plan", "fingerprint", "confirmedAt", "prepared", "items"))
            val plan = StyleTrialPlan.parse(r.stringValue("plan")); require(plan.fingerprint == r.stringValue("fingerprint"))
            require(r.get("prepared") is Boolean && r.longValue("confirmedAt") > 0)
            val a = r.getJSONArray("items"); require(a.length() == plan.inputs.size)
            val items = (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                o.exactKeys(setOf("requestId", "status", "endpointCalls", "resultHash", "reason"))
                TrialItemRecord(o.stringValue("requestId"), TrialStatus.valueOf(o.stringValue("status")),
                    o.longValue("endpointCalls").toInt().also { require(it in 0..2 && o.longValue("endpointCalls") == it.toLong()) },
                    if (o.isNull("resultHash")) null else o.stringValue("resultHash").also(::trialHash),
                    if (o.isNull("reason")) null else o.stringValue("reason").also { require(it.length <= 200) })
                    .also { row ->
                        require(row.requestId == plan.inputs[i].requestId)
                        require(row.endpointCalls <= if (plan.context.provider == dev.renkinProject.renkin.ai.AiSettings.STABILITY) 2 else 1)
                        require(row.status !in setOf(TrialStatus.PNG_SAVED, TrialStatus.REVIEWABLE) || row.resultHash != null)
                    }
            }
            return StyleTrialRecord(plan, r.longValue("confirmedAt"), r.getBoolean("prepared"), items)
        }
    }
}

/** Only requests and uncertainty live here. Completed original/RAW assets and review live in Room. */
internal class StyleTrialJournal(context: Context) {
    private val root = File(context.noBackupFilesDir.canonicalFile, "style_trials")
    private data class Shared(val lock: Any = Any(), var active: String? = null)
    private val shared = synchronized(locks) { locks.getOrPut(root.absolutePath) { Shared() } }
    companion object { private val locks = mutableMapOf<String, Shared>(); private const val MAX_RECORDS = 256 }
    private fun profile(id: Long): File {
        require(id > 0); return safe(File(root, id.toString()))
    }
    private fun safe(f: File): File {
        require(root.canonicalFile == root.absoluteFile && f.canonicalFile == f.absoluteFile &&
            (f == root || f.absolutePath.startsWith(root.absolutePath + File.separator))) { "Unsafe trial storage." }
        listOf(".bak", ".new").forEach { require(File(f.path + it).canonicalFile == File(f.path + it).absoluteFile) }
        return f
    }
    private fun marker(id: Long, planId: String): File { trialId(planId); return safe(File(profile(id), "plan-$planId.json")) }
    private fun imageFile(id: Long, hash: String): File { trialHash(hash); return safe(File(profile(id), "images/$hash.png")) }
    private fun resultFile(id: Long, request: String): File { trialId(request); return safe(File(profile(id), "results/$request.png")) }
    private fun consumedFile(request: String): File { trialId(request); return safe(File(root, "consumed-$request")) }
    private fun exists(f: File) = f.exists() || File(f.path + ".bak").exists() || File(f.path + ".new").exists()
    // AtomicFile.openRead discards an unfinished first .new, then throws without a base.
    // Such a plan still reserves its UUID; partial images never prove a durable result.
    private fun committed(f: File) = f.exists() || File(f.path + ".bak").exists()
    private fun bytes(f: File, limit: Int) = AtomicFile(safe(f)).openRead().use { it.readBounded(limit) }
    private fun text(f: File, limit: Int) = Charsets.UTF_8.newDecoder()
        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        .decode(java.nio.ByteBuffer.wrap(bytes(f, limit))).toString()
    private fun writeBytes(f: File, data: ByteArray) {
        safe(f); check(f.parentFile!!.mkdirs() || f.parentFile!!.isDirectory)
        val atomic = AtomicFile(f); val out = atomic.startWrite()
        try { out.write(data); atomic.finishWrite(out) } catch (e: Throwable) { atomic.failWrite(out); throw e }
    }
    private fun write(r: StyleTrialRecord) {
        val text = r.encode(); StyleTrialRecord.parse(text)
        writeBytes(marker(r.plan.context.profileId, r.plan.id), text.toByteArray())
    }
    private fun epochPart(id: Long?): String {
        val f = safe(if (id == null) File(root, "epoch") else File(profile(id), "epoch"))
        return if (!exists(f)) "initial" else text(f, 64).also {
            require(it.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        }
    }
    fun epoch(id: Long): String = synchronized(shared.lock) { "${epochPart(null)}:${epochPart(id)}" }
    fun current(plan: StyleTrialPlan) = epoch(plan.context.profileId) == plan.context.epoch
    fun isActive(plan: StyleTrialPlan) = synchronized(shared.lock) { shared.active == plan.id }
    fun read(id: Long, planId: String): StyleTrialRecord? = synchronized(shared.lock) {
        val f = marker(id, planId)
        if (!committed(f)) null else StyleTrialRecord.parse(text(f, 65536)).also {
            require(it.plan.id == planId && it.plan.context.profileId == id)
        }
    }
    fun records(id: Long): List<StyleTrialRecord> = synchronized(shared.lock) {
        val files = profile(id).listFiles().orEmpty().map { it.name.removeSuffix(".bak").removeSuffix(".new") }
            .filter { it.matches(Regex("plan-[a-f0-9-]{36}\\.json")) }.distinct()
        require(files.size <= MAX_RECORDS)
        files.mapNotNull { read(id, it.removePrefix("plan-").removeSuffix(".json")) }
            .sortedWith(compareBy<StyleTrialRecord> { it.confirmedAt }.thenBy { it.plan.id })
    }
    private fun update(id: Long, planId: String, request: String, change: (TrialItemRecord) -> TrialItemRecord): StyleTrialRecord {
        val old = checkNotNull(read(id, planId)); require(old.items.any { it.requestId == request })
        return old.copy(items = old.items.map { if (it.requestId == request) change(it) else it }).also(::write)
    }
    private fun quota(id: Long, extra: Long) {
        val files = profile(id).walkTopDown().onEnter { safe(it); true }.map(::safe).filter { it.isFile }.take(8193).toList()
        require(files.size <= 8192)
        val used = files.sumOf { it.length() }
        require(used + extra <= 64L * 1024 * 1024) { "Trial staging is full; no request was sent." }
    }
    /** A consumed plan UUID is never reusable, including partially written PREPARED inputs. */
    fun claim(plan: StyleTrialPlan, images: Map<String, ByteArray>): Boolean = synchronized(shared.lock) {
        plan.validate(); val id = plan.context.profileId
        require(plan.context.styleFingerprint == styleFingerprint(plan.specification, plan.instruction)) { "Unknown style input cannot authorize a new request." }
        if (exists(marker(id, plan.id))) return false
        check(shared.active == null && current(plan)); records(id)
        // Orphan reservations count toward the same bounded plan history too.
        require(profile(id).listFiles().orEmpty().map { it.name.removeSuffix(".bak").removeSuffix(".new") }
            .filter { it.matches(Regex("plan-[a-f0-9-]{36}\\.json")) }.distinct().size < MAX_RECORDS)
        require(plan.inputs.none { exists(consumedFile(it.requestId)) }) { "A trial request UUID is already consumed." }
        val globalCount = root.listFiles().orEmpty().map { it.name.removeSuffix(".bak").removeSuffix(".new") }
            .filter { it.matches(Regex("consumed-[a-f0-9-]{36}")) }.distinct().size
        require(globalCount + plan.inputs.size <= 8192) { "Device trial request history limit reached." }
        val required = (plan.inputs.map { it.originalHash } + plan.referenceHashes).toSet()
        require(images.keys == required)
        images.forEach { (hash, png) -> require(sha256(png) == hash); inspectPng(png) }
        quota(id, images.filterKeys { !committed(imageFile(id, it)) }.values.sumOf { it.size.toLong() } + 65536)
        val oldTime = records(id).maxOfOrNull { it.confirmedAt } ?: 0L
        val r = StyleTrialRecord(plan, maxOf(System.currentTimeMillis(), oldTime + 1), false,
            plan.inputs.map { TrialItemRecord(it.requestId) })
        write(r)
        plan.inputs.forEach { writeBytes(consumedFile(it.requestId), "$id:${plan.id}".toByteArray()) }
        images.forEach { (hash, png) ->
            val f = imageFile(id, hash)
            if (committed(f)) require(sha256(bytes(f, 4 * 1024 * 1024)) == hash) else writeBytes(f, png)
        }
        write(r.copy(prepared = true)); shared.active = plan.id; true
    }
    fun begin(plan: StyleTrialPlan, request: String) = synchronized(shared.lock) {
        check(shared.active == plan.id && current(plan)); val r = checkNotNull(read(plan.context.profileId, plan.id)); check(r.prepared)
        update(plan.context.profileId, plan.id, request) { check(it.status == TrialStatus.PENDING); it.copy(status = TrialStatus.REQUESTING) }
    }
    /** The actual client calls this immediately before each potentially billable enqueue. */
    fun endpoint(plan: StyleTrialPlan, request: String, step: Int) = synchronized(shared.lock) {
        check(shared.active == plan.id && current(plan))
        require(text(consumedFile(request), 100) == "${plan.context.profileId}:${plan.id}")
        require(step in 1..if (plan.context.provider == dev.renkinProject.renkin.ai.AiSettings.STABILITY) 2 else 1)
        update(plan.context.profileId, plan.id, request) {
            check(it.status == TrialStatus.REQUESTING && it.endpointCalls == step - 1)
            it.copy(endpointCalls = step)
        }
    }
    fun image(id: Long, hash: String): ByteArray = synchronized(shared.lock) {
        bytes(imageFile(id, hash), 4 * 1024 * 1024).also { require(sha256(it) == hash); inspectPng(it) }
    }
    fun result(id: Long, request: String): ByteArray = synchronized(shared.lock) {
        bytes(resultFile(id, request), 8 * 1024 * 1024).also(::validateTrialPng)
    }
    fun stage(plan: StyleTrialPlan, request: String, png: ByteArray, fault: (String) -> Unit = {}) = synchronized(shared.lock) {
        validateTrialPng(png); val id = plan.context.profileId
        val item = checkNotNull(read(id, plan.id)).items.single { it.requestId == request }
        check(item.status == TrialStatus.REQUESTING)
        quota(id, png.size.toLong()); writeBytes(resultFile(id, request), png)
        fault("RESULT_FILE_SAVED")
        update(id, plan.id, request) { it.copy(status = TrialStatus.PNG_SAVED, resultHash = sha256(png)) }
    }
    fun reviewable(plan: StyleTrialPlan, request: String) = synchronized(shared.lock) {
        update(plan.context.profileId, plan.id, request) { check(it.status == TrialStatus.PNG_SAVED); it.copy(status = TrialStatus.REVIEWABLE, reason = null) }
    }
    fun failure(plan: StyleTrialPlan, request: String, reason: String) = synchronized(shared.lock) {
        update(plan.context.profileId, plan.id, request) {
            if (it.status == TrialStatus.PNG_SAVED) it.copy(reason = "local_persistence")
            else it.copy(status = if (it.endpointCalls > 0) TrialStatus.UNCERTAIN else TrialStatus.FAILED, reason = reason)
        }
    }
    fun skipPending(plan: StyleTrialPlan, reason: String) = synchronized(shared.lock) {
        read(plan.context.profileId, plan.id)?.let { r -> write(r.copy(items = r.items.map {
            if (it.status == TrialStatus.PENDING) it.copy(status = TrialStatus.SKIPPED, reason = reason) else it
        })) }
    }
    fun release(plan: StyleTrialPlan) = synchronized(shared.lock) { if (shared.active == plan.id) shared.active = null }
    /** Read/local repair only. No caller may turn a cold PENDING/REQUESTING into permission. */
    fun recoverRecords(id: Long): List<StyleTrialRecord> = synchronized(shared.lock) {
        records(id).map { r ->
            if (shared.active == r.plan.id) r else r.copy(items = r.items.map { item ->
                when (item.status) {
                    TrialStatus.PENDING -> item.copy(status = TrialStatus.SKIPPED, reason = "interrupted_before_submit")
                    TrialStatus.REQUESTING, TrialStatus.UNCERTAIN -> {
                        val saved = resultFile(id, item.requestId)
                        if (current(r.plan) && committed(saved)) {
                            val png = result(id, item.requestId)
                            item.copy(status = TrialStatus.PNG_SAVED, resultHash = sha256(png), reason = "local_recovery")
                        } else if (item.status == TrialStatus.REQUESTING) item.copy(status =
                            if (item.endpointCalls > 0) TrialStatus.UNCERTAIN else TrialStatus.FAILED, reason = "interrupted") else item
                    }
                    else -> item
                }
            }).also { if (it != r) write(it) }
        }
    }
    /** Release staging only after Room is durable. Consumed markers and paid results remain. */
    fun collectCompletedInputs(id: Long) = synchronized(shared.lock) {
        val keep = records(id).filter { it.items.any { item -> item.status in setOf(TrialStatus.PENDING, TrialStatus.REQUESTING, TrialStatus.PNG_SAVED) } }
            .flatMap { it.plan.inputs.map { input -> input.originalHash } + it.plan.referenceHashes }.toSet()
        File(profile(id), "images").listFiles().orEmpty().filter { it.name.matches(Regex("[a-f0-9]{64}\\.png")) && it.name.removeSuffix(".png") !in keep }
            .forEach { check(safe(it).delete()) }
        records(id).forEach { r -> r.items.filter { it.status == TrialStatus.REVIEWABLE }.forEach {
            val f = resultFile(id, it.requestId); if (f.exists()) check(f.delete())
        } }
    }
    fun invalidateProfile(id: Long) = synchronized(shared.lock) {
        writeBytes(safe(File(profile(id), "epoch")), UUID.randomUUID().toString().toByteArray())
        records(id).forEach { r ->
            // Keep the actual provider/endpoint count, but no deleted artwork or text.
            // Stability's strict plan schema still needs one non-identifying reference.
            val refs = if (r.plan.context.provider == dev.renkinProject.renkin.ai.AiSettings.STABILITY)
                listOf("0".repeat(64)) else emptyList()
            val c = r.plan.context.copy(mode = "CREATE", sourcePackage = null, sourceStamp = null,
                referenceFingerprint = referenceFingerprint(refs), styleFingerprint = styleFingerprint("deleted project", ""))
            val deleted = r.plan.copy(context = c, specification = "deleted project", instruction = "", referenceHashes = refs,
                inputs = r.plan.inputs.mapIndexed { i, input -> input.copy(key = "deleted/target$i", name = "", originalHash = "0".repeat(64), origin = "deleted", originalEvidence = null) })
            write(r.copy(plan = deleted, prepared = false, items = r.items.map { it.copy(status =
                if (it.endpointCalls > 0) TrialStatus.UNCERTAIN else TrialStatus.SKIPPED, resultHash = null, reason = "profile_deleted") }))
        }
        for (name in listOf("images", "results")) {
            val directory = safe(File(profile(id), name))
            directory.listFiles().orEmpty().forEach { check(safe(it).isFile && it.delete()) }
            if (directory.exists()) check(directory.delete())
        }
    }
    fun invalidateAll() = synchronized(shared.lock) {
        writeBytes(safe(File(root, "epoch")), UUID.randomUUID().toString().toByteArray())
        root.listFiles().orEmpty().filter { it.isDirectory && it.name.matches(Regex("[1-9][0-9]*")) }.forEach { stale(it.name.toLong()) }
    }
    private fun stale(id: Long) {
        records(id).forEach { r -> write(r.copy(items = r.items.map {
            when (it.status) {
                TrialStatus.PENDING -> it.copy(status = TrialStatus.SKIPPED, reason = "data_replaced")
                TrialStatus.REQUESTING -> it.copy(status = TrialStatus.UNCERTAIN, reason = "data_replaced")
                else -> it
            }
        })) }
    }
}
