package dev.renkinProject.renkin.ai.style

import android.content.Context
import android.util.AtomicFile
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

internal enum class AnalysisStatus { PREPARED, REQUESTING, RESULT_SAVED, REVIEWED, FAILED, UNCERTAIN }
internal data class AnalysisRecord(val profileId: Long, val requestId: String, val fingerprint: String,
    val context: AnalysisContext?, val model: String, val description: String, val userInstruction: String,
    val hashes: List<String>, val promptHash: String, val createdAt: Long,
    val status: AnalysisStatus, val styleJson: String? = null, val responseId: String? = null,
    val responseHash: String? = null, val usage: AnalysisUsage? = null, val reason: String? = null,
    val reviewedRevision: Long? = null, val billingCertainty: String = "NOT_SUBMITTED", val localReport: ReferenceSampleReport? = null,
    val algorithm: String = REFERENCE_ALGORITHM) {
    val style: StyleProfileV1? get() = styleJson?.let(StyleProfileV1::parse)
    fun encode() = canonicalJson(json("schema" to 1, "profileId" to profileId, "requestId" to requestId,
        "fingerprint" to fingerprint, "context" to context?.encode(), "model" to model,
        "description" to description, "userInstruction" to userInstruction, "hashes" to JSONArray(hashes),
        "promptHash" to promptHash, "createdAt" to createdAt, "status" to status.name,
        "styleJson" to styleJson, "responseId" to responseId, "responseHash" to responseHash,
        "usage" to usage?.let { json("input" to it.inputTokens, "output" to it.outputTokens) },
        "reason" to reason, "reviewedRevision" to reviewedRevision, "billingCertainty" to billingCertainty,
        "localReport" to localReport?.encode()).also { if (algorithm != LEGACY_REFERENCE_ALGORITHM) it.put("algorithm", algorithm) })
    companion object {
        fun parse(text: String): AnalysisRecord {
            val r = strictObject(text, 65536, 12); require(r.longValue("schema") == 1L)
            r.exactKeys(setOf("schema", "profileId", "requestId", "fingerprint", "context", "model", "description", "userInstruction",
                "hashes", "promptHash", "createdAt", "status", "styleJson", "responseId", "responseHash", "usage", "reason",
                "reviewedRevision", "billingCertainty", "localReport") + if (r.has("algorithm")) setOf("algorithm") else emptySet())
            fun nullable(key: String) = if (r.isNull(key)) null else r.stringValue(key)
            val a = r.getJSONArray("hashes"); require(a.length() <= 12)
            val record = AnalysisRecord(r.longValue("profileId"), r.stringValue("requestId"), r.stringValue("fingerprint"),
                if (r.isNull("context")) null else AnalysisContext.parse(r.getJSONObject("context")), r.stringValue("model"),
                r.stringValue("description").also { cleanText(it, 6000, true) }, r.stringValue("userInstruction").also { cleanText(it, 2000, true) },
                (0 until a.length()).map { a.get(it) as? String ?: error("Invalid hash.") }, r.stringValue("promptHash"),
                r.longValue("createdAt"), AnalysisStatus.valueOf(r.stringValue("status")), nullable("styleJson"),
                nullable("responseId"), nullable("responseHash"), if (r.isNull("usage")) null else r.getJSONObject("usage").let { AnalysisUsage(it.longValue("input"), it.longValue("output")) },
                nullable("reason"), if (r.isNull("reviewedRevision")) null else r.longValue("reviewedRevision"), r.stringValue("billingCertainty"),
                if (r.isNull("localReport")) null else ReferenceSampleReport.parse(r.getJSONObject("localReport")),
                if (r.has("algorithm")) r.stringValue("algorithm") else LEGACY_REFERENCE_ALGORITHM)
            require(record.algorithm in setOf(REFERENCE_ALGORITHM, PREVIOUS_REFERENCE_ALGORITHM, LEGACY_REFERENCE_ALGORITHM))
            require(record.profileId > 0 && UUID.fromString(record.requestId).toString() == record.requestId)
            require(record.fingerprint.matches(Regex("[a-f0-9]{64}")) && record.createdAt > 0)
            require(record.billingCertainty in setOf("NOT_SUBMITTED", "UNKNOWN", "RESPONSE_RECEIVED"))
            require(record.context == null || record.context.profileId == record.profileId)
            if (record.context != null) {
                require(record.model in ANALYSIS_MODELS && record.promptHash == sha256(canonicalJson(json("description" to record.description, "instruction" to record.userInstruction)).toByteArray()))
                require(record.fingerprint == sha256(canonicalJson(json("context" to record.context.encode(),
                    "hashes" to JSONArray(record.hashes), "promptHash" to record.promptHash, "model" to record.model,
                    "detail" to "low", "schema" to 1, "algorithm" to record.algorithm, "localReport" to record.localReport?.encode())).toByteArray()))
            } else require(record.status == AnalysisStatus.FAILED && record.styleJson == null)
            require(record.hashes.all { it.matches(Regex("[a-f0-9]{64}")) } && record.hashes.distinct().size == record.hashes.size)
            if (record.styleJson != null) {
                require(record.styleJson == record.style!!.encode() && record.responseHash == sha256(record.styleJson.toByteArray()))
                require(record.status in setOf(AnalysisStatus.RESULT_SAVED, AnalysisStatus.REVIEWED, AnalysisStatus.FAILED))
            }
            if (record.status == AnalysisStatus.RESULT_SAVED || record.status == AnalysisStatus.REVIEWED) {
                require(record.context != null && record.hashes.isNotEmpty() && record.styleJson != null && !record.responseId.isNullOrBlank())
            }
            return record
        }
    }
}

/** Bounded analysis-only noBackup files. Shared path locks also cover separately constructed
 * restore/deletion helpers. Consumed markers remain after explicit image cleanup. */
internal class StyleAnalysisJournal(context: Context) {
    private val parent = context.noBackupFilesDir.canonicalFile
    private val root = File(parent, "style_analysis")
    private data class Live(val lock: Any = Any(), val requests: MutableMap<Long, String> = mutableMapOf())
    private val shared = synchronized(locks) { locks.getOrPut(root.absolutePath) { Live() } }
    companion object { private val locks = mutableMapOf<String, Live>(); private const val MAX_RECORDS = 512 }
    private fun profile(id: Long): File {
        require(id > 0)
        check(root.canonicalFile == File(parent, "style_analysis") && File(root, id.toString()).canonicalFile == File(root.canonicalFile, id.toString()))
        return File(root, id.toString())
    }
    private fun file(id: Long, request: String): File {
        require(UUID.fromString(request).toString() == request)
        return File(profile(id), "request-$request.json")
    }
    private fun epochFile(id: Long?) = if (id == null) File(root, "epoch") else File(profile(id), "epoch")
    private fun epochPart(id: Long?): String = epochFile(id).let {
        require(it.canonicalFile == it.absoluteFile)
        if (!exists(it)) "initial" else readText(it, 64).also { s -> require(s.matches(Regex("[a-zA-Z0-9-]{1,64}"))) }
    }
    private fun readText(path: File, limit: Int): String {
        listOf(path, File(path.path + ".bak"), File(path.path + ".new")).forEach { require(it.canonicalFile == it.absoluteFile) }
        val bytes = AtomicFile(path).openRead().use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count < 0) break
                require(output.size() + count <= limit); output.write(buffer, 0, count) }
            output.toByteArray()
        }
        return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }
    fun epoch(id: Long): String = synchronized(shared.lock) { "${epochPart(null)}:${epochPart(id)}" }
    private fun writeBytes(path: File, bytes: ByteArray) {
        check(path.canonicalFile == path.absoluteFile) { "Unsafe analysis path." }
        check(path.parentFile!!.mkdirs() || path.parentFile!!.isDirectory)
        val atomic = AtomicFile(path); val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) } catch (error: Throwable) { atomic.failWrite(output); throw error }
    }
    private fun write(record: AnalysisRecord) {
        val text = record.encode(); AnalysisRecord.parse(text); writeBytes(file(record.profileId, record.requestId), text.toByteArray())
    }
    private fun exists(file: File) = file.exists() || File(file.path + ".bak").exists() || File(file.path + ".new").exists()
    fun read(id: Long, request: String): AnalysisRecord? = synchronized(shared.lock) {
        val f = file(id, request); if (!exists(f)) null else {
            require(f.length() <= 65536 && f.canonicalFile == File(f.parentFile!!.canonicalFile, f.name))
            AnalysisRecord.parse(readText(f, 65536)).also { require(it.profileId == id && it.requestId == request) }
        }
    }
    private fun records(id: Long): List<AnalysisRecord> {
        val files = profile(id).listFiles().orEmpty().map { File(it.parentFile, it.name.removeSuffix(".bak").removeSuffix(".new")) }
            .filter { it.name.matches(Regex("request-[a-f0-9-]{36}\\.json")) }.distinctBy { it.name }
        require(files.size <= MAX_RECORDS)
        return files.map { checkNotNull(read(id, it.name.removePrefix("request-").removeSuffix(".json"))) }
    }
    fun latest(id: Long): AnalysisRecord? = synchronized(shared.lock) {
        val all = records(id).map { record ->
            if (record.status in setOf(AnalysisStatus.PREPARED, AnalysisStatus.REQUESTING) && shared.requests[id] != record.requestId) {
                record.copy(status = AnalysisStatus.UNCERTAIN, reason = "interrupted").also(::write)
            } else record
        }
        all.maxWithOrNull(compareBy<AnalysisRecord> { it.createdAt }.thenBy { it.requestId })
    }
    /** Called only after explicit consent. Once the UUID exists, it can never submit again. */
    fun claim(request: StyleAnalysisRequest): Boolean = synchronized(shared.lock) {
        request.validate()
        if (exists(file(request.context.profileId, request.requestId))) return false
        check(shared.requests[request.context.profileId] == null) { "An analysis is already running." }
        require(request.context.storageEpoch == epoch(request.context.profileId)) { "Project data was replaced." }
        val history = records(request.context.profileId)
        require(history.size < MAX_RECORDS) { "Local analysis history limit reached; manual style remains available." }
        val directory = File(profile(request.context.profileId), "images")
        val existingBytes = directory.listFiles().orEmpty().sumOf { it.length() }
        require(existingBytes + request.images.filter { !File(directory, "${it.hash}.png").exists() }.sumOf { it.png.size.toLong() } <= 8L * 1024 * 1024) {
            "Analysis staging is full. Explicitly discard an old candidate first."
        }
        // PREPARED is a consumed consent too; an interrupted preparation is never replayed.
        val initial = AnalysisRecord(request.context.profileId, request.requestId, request.fingerprint, request.context,
            request.model, request.description, request.userInstruction, request.images.map { it.hash }, request.promptHash,
            maxOf(System.currentTimeMillis(), (history.maxOfOrNull { it.createdAt } ?: 0L) + 1L), AnalysisStatus.PREPARED,
            localReport = request.localReport, algorithm = request.algorithm)
        write(initial)
        request.images.forEach { image ->
            val destination = File(directory, "${image.hash}.png")
            require(destination.canonicalFile == destination.absoluteFile)
            if (destination.exists()) require(destination.length() <= MAX_ANALYSIS_PNG && sha256(destination.readBytes()) == image.hash) else writeBytes(destination, image.png)
        }
        write(initial.copy(status = AnalysisStatus.REQUESTING, billingCertainty = "UNKNOWN")); shared.requests[request.context.profileId] = request.requestId
        true
    }
    fun finish(id: Long, request: String, outcome: StyleAnalysisOutcome): AnalysisRecord = synchronized(shared.lock) {
        val old = checkNotNull(read(id, request)); check(old.status == AnalysisStatus.REQUESTING)
        val result = if (outcome is StyleAnalysisOutcome.Suggested) {
            val encoded = outcome.style.encode()
            old.copy(status = AnalysisStatus.RESULT_SAVED, styleJson = encoded, responseId = outcome.responseId,
                responseHash = sha256(encoded.toByteArray()), usage = outcome.usage, billingCertainty = "RESPONSE_RECEIVED")
        } else old.copy(status = if (outcome == StyleAnalysisOutcome.Uncertain) AnalysisStatus.UNCERTAIN else AnalysisStatus.FAILED,
            reason = if (outcome is StyleAnalysisOutcome.RequestRejected) "REQUEST_REJECTED:${outcome.httpStatus}" else outcome.javaClass.simpleName)
        write(result); result
    }
    fun interrupted(id: Long, request: String) = synchronized(shared.lock) {
        read(id, request)?.takeIf { it.status == AnalysisStatus.REQUESTING || it.status == AnalysisStatus.PREPARED }
            ?.copy(status = AnalysisStatus.UNCERTAIN, reason = "interrupted")?.let(::write)
    }
    fun release(id: Long, request: String) = synchronized(shared.lock) { if (shared.requests[id] == request) shared.requests.remove(id); Unit }
    fun image(id: Long, hash: String): ByteArray = synchronized(shared.lock) {
        require(hash.matches(Regex("[a-f0-9]{64}")))
        val f = File(profile(id), "images/$hash.png"); require(f.length() in 1..MAX_ANALYSIS_PNG.toLong())
        require(f.canonicalFile == File(profile(id).canonicalFile, "images/$hash.png"))
        f.readBytes().also { validateAnalysisPng(it); require(sha256(it) == hash) }
    }
    fun reviewed(id: Long, request: String, revision: Long) = synchronized(shared.lock) {
        val old = checkNotNull(read(id, request)); check(old.status in setOf(AnalysisStatus.RESULT_SAVED, AnalysisStatus.REVIEWED))
        write(old.copy(status = AnalysisStatus.REVIEWED, reviewedRevision = revision))
        collectImages(id)
    }
    fun discard(id: Long, request: String) = synchronized(shared.lock) {
        val old = checkNotNull(read(id, request)); check(shared.requests[id] != request)
        write(old.copy(status = AnalysisStatus.FAILED, reason = "discarded")); collectImages(id)
    }
    private fun collectImages(id: Long) {
        val retained = records(id).filter { it.status in setOf(AnalysisStatus.PREPARED, AnalysisStatus.REQUESTING, AnalysisStatus.RESULT_SAVED) }
            .flatMap { it.hashes }.toSet()
        File(profile(id), "images").listFiles().orEmpty().filter { it.name.matches(Regex("[a-f0-9]{64}\\.png")) && it.name.removeSuffix(".png") !in retained }
            .forEach { check(it.delete()) }
    }
    fun invalidateProfile(id: Long) = synchronized(shared.lock) {
        writeBytes(epochFile(id), UUID.randomUUID().toString().toByteArray())
        records(id).forEach { old -> write(old.copy(context = null, description = "", userInstruction = "", hashes = emptyList(),
            status = AnalysisStatus.FAILED, styleJson = null, responseId = null, responseHash = null, usage = null, reason = "profile_deleted", localReport = null)) }
        collectImages(id)
    }
    /** Before replacing profile rows, stale all device-local proposals, retaining consumed IDs. */
    fun invalidateAll() = synchronized(shared.lock) {
        writeBytes(epochFile(null), UUID.randomUUID().toString().toByteArray())
        root.listFiles().orEmpty().filter { it.isDirectory && it.name.matches(Regex("[1-9][0-9]*")) }.forEach { directory ->
            val id = directory.name.toLong()
            records(id).forEach { old -> write(old.copy(status = AnalysisStatus.FAILED, reason = "data_restored")) }
            collectImages(id)
        }
    }
}
