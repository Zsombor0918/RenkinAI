package dev.renkinProject.renkin.ai.style

import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Accepted provenance only. Candidate journals, API keys and paid consent are never exported. */
internal data class ReviewedAnalysisEvidence(val requestId: String, val fingerprint: String, val promptHash: String,
    val context: AnalysisContext, val model: String, val sampleHashes: List<String>, val selectedHashes: List<String>,
    val responseId: String, val responseHash: String, val reviewedAt: Long,
    val userEdited: Boolean, val staleSourceAccepted: Boolean, val needsRevalidation: Boolean = false) {
    fun validate(profileId: Long, profileJson: String, specification: String, origin: String, hashes: List<String>) {
        context.validate(); require(context.profileId == profileId && UUID.fromString(requestId).toString() == requestId)
        require(model in ANALYSIS_MODELS && reviewedAt > 0 && responseId.matches(Regex("[A-Za-z0-9_-]{1,128}")))
        require(listOf(fingerprint, promptHash, responseHash).all { it.matches(Regex("[a-f0-9]{64}")) })
        val dna = SavedStyleDna.optional(profileJson)
        require(sampleHashes.size in 1..(if (dna == null) 12 else 24) && sampleHashes.distinct().size == sampleHashes.size)
        require((sampleHashes + selectedHashes).all { it.matches(Regex("[a-f0-9]{64}")) })
        require(selectedHashes == hashes && selectedHashes.size <= 5)
        val profile = if (dna == null) StyleProfileV1.parse(profileJson) else null
        require(profileJson == (dna?.encode() ?: checkNotNull(profile).encode()) && responseHash == sha256(profileJson.toByteArray()))
        if (dna != null) require(dna.reports.map { it.sampleHash } == sampleHashes)
        require(origin == if (userEdited) "USER" else "AUTO_ANALYSIS")
        if (!userEdited) require(specification == (dna?.let(StyleDnaCompiler::specification) ?: PromptComposer.specification(checkNotNull(profile))) && selectedHashes.all { it in sampleHashes })
    }
    fun encode() = canonicalJson(json("schema" to 1, "requestId" to requestId, "fingerprint" to fingerprint,
        "promptHash" to promptHash, "context" to context.encode(), "model" to model,
        "sampleHashes" to JSONArray(sampleHashes), "selectedHashes" to JSONArray(selectedHashes),
        "responseId" to responseId, "responseHash" to responseHash, "reviewedAt" to reviewedAt,
        "userEdited" to userEdited, "staleSourceAccepted" to staleSourceAccepted, "needsRevalidation" to needsRevalidation))
    fun rekey(id: Long) = copy(context = context.copy(profileId = id), needsRevalidation = true)
    companion object {
        fun parse(text: String): ReviewedAnalysisEvidence {
            val r = strictObject(text, 16384, 5)
            r.exactKeys(setOf("schema", "requestId", "fingerprint", "promptHash", "context", "model", "sampleHashes",
                "selectedHashes", "responseId", "responseHash", "reviewedAt", "userEdited", "staleSourceAccepted", "needsRevalidation"))
            require(r.longValue("schema") == 1L)
            fun hashes(key: String) = r.getJSONArray(key).let { a -> require(a.length() <= 24); (0 until a.length()).map { a.get(it) as? String ?: error("Invalid sample hash.") } }
            fun flag(key: String) = r.get(key) as? Boolean ?: error("Invalid evidence flag.")
            return ReviewedAnalysisEvidence(r.stringValue("requestId"), r.stringValue("fingerprint"), r.stringValue("promptHash"),
                AnalysisContext.parse(r.getJSONObject("context")), r.stringValue("model"), hashes("sampleHashes"), hashes("selectedHashes"),
                r.stringValue("responseId"), r.stringValue("responseHash"), r.longValue("reviewedAt"), flag("userEdited"),
                flag("staleSourceAccepted"), flag("needsRevalidation"))
        }
    }
}
