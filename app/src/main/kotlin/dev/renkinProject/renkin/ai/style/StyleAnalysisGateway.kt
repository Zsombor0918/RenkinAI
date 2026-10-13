package dev.renkinProject.renkin.ai.style

import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import java.util.UUID

internal const val DEFAULT_ANALYSIS_MODEL = "gpt-5.4-mini"
internal val ANALYSIS_MODELS = setOf(DEFAULT_ANALYSIS_MODEL, "gpt-5.4-mini-2026-03-17")

/** All identities are local context. They are never included in the provider body. */
internal data class AnalysisContext(val profileId: Long, val mode: String, val sourcePackage: String?,
    val setupStamp: String?, val referenceStamp: String?, val metadataVersion: Long,
    val selectionHash: String, val catalogStamp: String, val baseRevision: Long?,
    val storageEpoch: String = "initial:initial", val sourceIdentity: dev.renkinProject.renkin.apk.CoverageStamp? = null) {
    fun encode() = json("profileId" to profileId, "mode" to mode, "sourcePackage" to sourcePackage,
        "setupStamp" to setupStamp, "referenceStamp" to referenceStamp, "metadataVersion" to metadataVersion,
        "selectionHash" to selectionHash, "catalogStamp" to catalogStamp, "baseRevision" to baseRevision, "storageEpoch" to storageEpoch,
        "sourceIdentity" to sourceIdentity?.let { json("package" to it.packageName, "version" to it.versionCode,
            "updated" to it.lastUpdateTime, "index" to it.indexRevision, "night" to it.theme) })
    fun validate() {
        require(profileId > 0 && mode in setOf("CREATE", "EXTEND") && metadataVersion >= 0)
        require(baseRevision == null || baseRevision > 0)
        require(storageEpoch.matches(Regex("[a-zA-Z0-9:-]{1,128}")))
        require(selectionHash.matches(Regex("[a-f0-9]{64}")) && catalogStamp.matches(Regex("[a-f0-9]{64}")))
        require(if (mode == "CREATE") sourcePackage == null && setupStamp == null && referenceStamp == null
            else !sourcePackage.isNullOrBlank() && setupStamp?.matches(Regex("[a-f0-9]{64}")) == true && referenceStamp?.matches(Regex("[a-f0-9]{64}")) == true)
        require(if (mode == "CREATE") sourceIdentity == null else sourceIdentity?.packageName == sourcePackage && sourceIdentity?.versionCode != null)
        sourceIdentity?.let {
            require(it.packageName.length <= 255 && it.packageName.matches(Regex("[a-zA-Z0-9_.]+")))
            require(checkNotNull(it.versionCode) >= 0 && it.lastUpdateTime?.let { time -> time >= 0 } == true && it.indexRevision >= 0)
        }
    }
    companion object {
        fun parse(r: org.json.JSONObject): AnalysisContext {
            r.exactKeys(setOf("profileId", "mode", "sourcePackage", "setupStamp", "referenceStamp", "metadataVersion", "selectionHash", "catalogStamp", "baseRevision", "storageEpoch", "sourceIdentity"))
            val identity = if (r.isNull("sourceIdentity")) null else r.getJSONObject("sourceIdentity").let { i ->
                i.exactKeys(setOf("package", "version", "updated", "index", "night"))
                dev.renkinProject.renkin.apk.CoverageStamp(i.stringValue("package"),
                if (i.isNull("version")) null else i.longValue("version"), if (i.isNull("updated")) null else i.longValue("updated"),
                i.longValue("index"), if (i.isNull("night")) null else i.get("night") as? Boolean ?: error("Invalid theme.")) }
            return AnalysisContext(r.longValue("profileId"), r.stringValue("mode"),
            if (r.isNull("sourcePackage")) null else r.stringValue("sourcePackage"),
            if (r.isNull("setupStamp")) null else r.stringValue("setupStamp"),
            if (r.isNull("referenceStamp")) null else r.stringValue("referenceStamp"), r.longValue("metadataVersion"),
            r.stringValue("selectionHash"), r.stringValue("catalogStamp"), if (r.isNull("baseRevision")) null else r.longValue("baseRevision"), r.stringValue("storageEpoch"), identity)
            .also { it.validate() }
        }
    }
}

internal data class AnalysisImage(val hash: String, val png: ByteArray)
internal data class StyleAnalysisRequest(val context: AnalysisContext, val images: List<AnalysisImage>,
    val description: String, val userInstruction: String,
    val model: String = DEFAULT_ANALYSIS_MODEL, val detail: String = "low",
    val requestId: String = UUID.randomUUID().toString(), val localReport: ReferenceSampleReport? = null) {
    val promptHash: String get() = sha256(canonicalJson(json("description" to description, "instruction" to userInstruction)).toByteArray())
    val fingerprint: String get() = sha256(canonicalJson(json("context" to context.encode(),
        "hashes" to JSONArray(images.map { it.hash }), "promptHash" to promptHash,
        "model" to model, "detail" to detail, "schema" to 1, "algorithm" to REFERENCE_ALGORITHM,
        "localReport" to localReport?.encode())).toByteArray())
    fun validate() {
        context.validate(); require(UUID.fromString(requestId).toString() == requestId)
        require(model in ANALYSIS_MODELS && detail == "low" && images.size in 1..12)
        cleanText(description, 6000, true); cleanText(userInstruction, 2000, true)
        require(images.map { it.hash }.distinct().size == images.size && images.sumOf { it.png.size.toLong() } <= 4L * 1024 * 1024)
        images.forEach { validateAnalysisPng(it.png); require(it.hash == sha256(it.png)) }
        localReport?.let { it.validate(); require(it.valid >= images.size && it.lowSample == (images.size < 8)) }
    }
}

internal data class AnalysisUsage(val inputTokens: Long, val outputTokens: Long) {
    init { require(inputTokens in 0..1000000000 && outputTokens in 0..1000000000) }
}
internal sealed interface StyleAnalysisOutcome {
    data class Suggested(val style: StyleProfileV1, val responseId: String, val usage: AnalysisUsage?) : StyleAnalysisOutcome
    data object Refused : StyleAnalysisOutcome
    data object InvalidResponse : StyleAnalysisOutcome
    data object InvalidInput : StyleAnalysisOutcome
    data object Unauthorized : StyleAnalysisOutcome
    data object RateLimited : StyleAnalysisOutcome
    data object QuotaExceeded : StyleAnalysisOutcome
    data class RequestRejected(val httpStatus: Int) : StyleAnalysisOutcome {
        init { require(httpStatus in setOf(400, 402, 422)) }
    }
    data object Unavailable : StyleAnalysisOutcome
    data object Uncertain : StyleAnalysisOutcome
    data object StaleSource : StyleAnalysisOutcome
}
internal fun interface StyleAnalysisGateway { suspend fun analyze(request: StyleAnalysisRequest): StyleAnalysisOutcome }
