package dev.renkinProject.renkin.ai.style

import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal const val DNA_PROMPT_VERSION = "per-icon-grammar-v1"
internal enum class DnaAnalysisMode { QUICK, STANDARD }
internal data class DnaSampleBinding(val id: String, val hash: String) {
    fun json() = json("id" to id.also(::dnaId),"hash" to hash.also(::dnaHash))
}
internal data class DnaStep(val id: String, val samples: List<String>, val synthesis: Boolean) {
    fun json() = json("id" to id,"samples" to JSONArray(samples),"synthesis" to synthesis)
}
/** Images stay separate private PNGs. All charged submissions are listed and frozen up front. */
internal data class StyleDnaPlan(val context: AnalysisContext, val mode: DnaAnalysisMode,
    val samples: List<DnaSampleBinding>, val report: ReferenceSampleReport, val description: String,
    val instruction: String, val draftHash: String, val model: String = DEFAULT_ANALYSIS_MODEL, val detail: String = "low",
    val algorithm: String = REFERENCE_ALGORITHM, val id: String = UUID.randomUUID().toString(),
    val steps: List<DnaStep> = makeDnaSteps(mode,samples), val promptVersion: String = DNA_PROMPT_VERSION) {
    val bindings get() = samples.associate { it.id to it.hash }
    val endpointMaximum get() = steps.size
    val promptHash get() = sha256(canonicalJson(json("description" to description,"instruction" to instruction,"prompt" to promptVersion)).toByteArray())
    val fingerprint get() = sha256(canonicalJson(json("context" to context.encode(),"mode" to mode.name,"samples" to JSONArray(samples.map { it.json() }),
        "report" to report.encode(),"promptHash" to promptHash,"draftHash" to draftHash,"model" to model,"detail" to detail,"algorithm" to algorithm,"schema" to 1)).toByteArray())
    fun validate() {
        context.validate(); require(UUID.fromString(id).toString() == id && promptVersion == DNA_PROMPT_VERSION && algorithm == REFERENCE_ALGORITHM)
        require(model in ANALYSIS_MODELS && detail in setOf("low","high")); dnaHash(draftHash)
        cleanText(description,6000,true); cleanText(instruction,2000,true); report.validate()
        require(samples.size in 1..(if(mode==DnaAnalysisMode.QUICK)12 else 24) && report.valid >= samples.size)
        require(samples.map { it.id }.distinct().size == samples.size && samples.map { it.hash }.distinct().size == samples.size)
        samples.forEach { it.json() }
        require(steps.map { it.id }.distinct().size == steps.size && steps.none { it.id == id }); steps.forEach { require(UUID.fromString(it.id).toString() == it.id) }
        val expected = if(mode==DnaAnalysisMode.QUICK) listOf(samples.map { it.id }) else samples.map { it.id }.chunked(6) + listOf(emptyList())
        require(steps.map { it.samples } == expected && steps.map { it.synthesis } == expected.indices.map { mode==DnaAnalysisMode.STANDARD && it==expected.lastIndex })
        require(endpointMaximum in 1..5)
    }
    fun json(): JSONObject { validate(); return json("schema" to 1,"context" to context.encode(),"mode" to mode.name,"samples" to JSONArray(samples.map { it.json() }),
        "report" to report.encode(),"description" to description,"instruction" to instruction,"draftHash" to draftHash,"model" to model,"detail" to detail,
        "algorithm" to algorithm,"id" to id,"steps" to JSONArray(steps.map { it.json() }),"promptVersion" to promptVersion) }
    companion object {
        fun parse(r: JSONObject): StyleDnaPlan {
            r.exactKeys(setOf("schema","context","mode","samples","report","description","instruction","draftHash","model","detail","algorithm","id","steps","promptVersion")); require(r.longValue("schema")==1L)
            val samples = r.getJSONArray("samples"); require(samples.length() in 1..24)
            val steps = r.getJSONArray("steps"); require(steps.length() in 1..5)
            return StyleDnaPlan(AnalysisContext.parse(r.getJSONObject("context")),DnaAnalysisMode.valueOf(r.stringValue("mode")),
                (0 until samples.length()).map { samples.getJSONObject(it).let { s -> s.exactKeys(setOf("id","hash")); DnaSampleBinding(s.stringValue("id"),s.stringValue("hash")) } },
                ReferenceSampleReport.parse(r.getJSONObject("report")),r.stringValue("description"),r.stringValue("instruction"),r.stringValue("draftHash"),
                r.stringValue("model"),r.stringValue("detail"),r.stringValue("algorithm"),r.stringValue("id"),
                (0 until steps.length()).map { steps.getJSONObject(it).let { s -> s.exactKeys(setOf("id","samples","synthesis"));
                    DnaStep(s.stringValue("id"),dnaStrings(s.getJSONArray("samples"),24,48),s.get("synthesis") as? Boolean ?: error("Invalid step.")) } },r.stringValue("promptVersion")).also { it.validate() }
        }
    }
}
private fun makeDnaSteps(mode: DnaAnalysisMode,samples: List<DnaSampleBinding>): List<DnaStep> {
    val groups = if(mode==DnaAnalysisMode.QUICK) listOf(samples.map { it.id }) else samples.map { it.id }.chunked(6)
    return groups.map { DnaStep(UUID.randomUUID().toString(),it,false) } +
        if(mode==DnaAnalysisMode.STANDARD) listOf(DnaStep(UUID.randomUUID().toString(),emptyList(),true)) else emptyList()
}
internal fun validateDnaImages(plan: StyleDnaPlan, images: Map<String,ByteArray>) {
    plan.validate(); require(images.keys==plan.samples.map { it.hash }.toSet() && images.values.sumOf { it.size.toLong() } <= 8L*1024*1024)
    images.forEach { (hash,png) -> validateAnalysisPng(png); require(sha256(png)==hash) }
}

/** Additive tagged payload in the existing nullable revision JSON; V1 rows stay untouched. */
internal data class SavedStyleDna(val dna: StyleDnaV1, val reports: List<DnaIconReport>, val model: String, val detail: String,
    val algorithm: String, val selectedFamily: String, val promptVersion: String = DNA_PROMPT_VERSION) {
    fun encode(): String {
        dna.encode(); require(reports.size in 1..24 && reports.map { it.sampleId }.distinct().size==reports.size && reports.map { it.sampleHash }.distinct().size==reports.size)
        require(dna.families.flatMap { it.evidenceIds }.toSet()==reports.map { it.sampleId }.toSet() && dna.families.any { it.id==selectedFamily })
        require(model in ANALYSIS_MODELS && detail in setOf("low","high") && algorithm==REFERENCE_ALGORITHM && promptVersion==DNA_PROMPT_VERSION)
        return canonicalJson(json("kind" to "style_dna","schema" to 1,"dna" to dna.json(),"reports" to JSONArray(reports.map { it.json() }),
            "model" to model,"detail" to detail,"algorithm" to algorithm,"selectedFamily" to selectedFamily,"promptVersion" to promptVersion))
            .also { require(it.toByteArray().size<=192*1024) }
    }
    companion object {
        fun parse(text: String): SavedStyleDna {
            val r=strictObject(text,192*1024,12); r.exactKeys(setOf("kind","schema","dna","reports","model","detail","algorithm","selectedFamily","promptVersion"))
            require(r.stringValue("kind")=="style_dna" && r.longValue("schema")==1L)
            val a=r.getJSONArray("reports"); require(a.length() in 1..24)
            return SavedStyleDna(StyleDnaV1.parse(canonicalJson(r.getJSONObject("dna"))),(0 until a.length()).map { DnaIconReport.parse(a.getJSONObject(it)) },
                r.stringValue("model"),r.stringValue("detail"),r.stringValue("algorithm"),r.stringValue("selectedFamily"),r.stringValue("promptVersion")).also { it.encode() }
        }
        fun optional(text: String?): SavedStyleDna? {
            if(text==null) return null
            val r=strictObject(text,192*1024,12)
            return if(r.has("kind")) parse(text) else null
        }
    }
}
