package dev.renkinProject.renkin.ai.style

import org.json.JSONArray
import org.json.JSONObject

internal enum class DnaProvenance { OBSERVED, INFERRED, UNKNOWN }
internal enum class DnaFamilyVote { OUTLINE, SOLID, TILE, WORDMARK, DUOTONE, MIXED, UNKNOWN }
internal enum class DnaSymbolType { WORDMARK, MONOGRAM, GEOMETRIC_PICTOGRAM, COMPLEX_EMBLEM, UNKNOWN }
internal enum class DnaTile { NONE, ROUND, SQUARE, OTHER, MIXED, UNKNOWN }
internal enum class DnaDepth { NONE, SHADOW, GRADIENT, SHADOW_AND_GRADIENT, MIXED, UNKNOWN }
internal val DNA_TRAITS = listOf("tile_geometry", "logo_silhouette", "stroke_fill", "corners", "optical_scale",
    "negative_space", "shadow_gradient", "luminance", "color_roles", "wordmark_handling", "simplification")
internal val DNA_DRAWING = listOf("background_tile", "geometry", "stroke_fill", "rounding", "simplification", "contour",
    "scale_padding", "optical_alignment", "negative_space", "light_shadow_depth", "gradient_use",
    "wordmarks", "monograms", "geometric_pictograms", "complex_emblems")
internal fun dnaId(id: String) { require(id.matches(Regex("[a-z][a-z0-9_-]{0,47}"))) }
internal fun dnaHash(hash: String) { require(hash.matches(Regex("[a-f0-9]{64}"))) }
internal fun dnaStrings(a: JSONArray, max: Int, length: Int = 240): List<String> {
    require(a.length() <= max)
    return (0 until a.length()).map { cleanText(a.get(it) as? String ?: error("Expected bounded text."), length) }
}
internal fun dnaObject(properties: JSONObject) = json("type" to "object", "properties" to properties,
    "required" to JSONArray(properties.keys().asSequence().toList()), "additionalProperties" to false)
internal fun dnaText(max: Int = 240) = json("type" to "string", "maxLength" to max)
internal fun dnaArray(items: JSONObject, max: Int) = json("type" to "array", "items" to items, "maxItems" to max)
internal inline fun <reified T : Enum<T>> dnaEnum() = json("type" to "string", "enum" to JSONArray(enumValues<T>().map { it.name }))
internal data class DnaObservation(val value: String, val provenance: DnaProvenance) {
    fun json() = json("value" to value.also { require(cleanText(it,240).isNotEmpty()) }, "provenance" to provenance.name)
    companion object {
        fun parse(r: JSONObject): DnaObservation { r.exactKeys(setOf("value","provenance"))
            return DnaObservation(r.stringValue("value"), DnaProvenance.valueOf(r.stringValue("provenance"))).also { it.json() } }
        fun schema() = dnaObject(json("value" to dnaText(), "provenance" to dnaEnum<DnaProvenance>()))
    }
}
internal data class DnaIconReport(val sampleId: String, val sampleHash: String, val familyVote: DnaFamilyVote,
    val symbolType: DnaSymbolType, val traits: Map<String,DnaObservation>, val colorPolicy: PackColorPolicy,
    val tileVote: DnaTile, val depthVote: DnaDepth, val outlierReason: String = "") {
    fun json(): JSONObject {
        dnaId(sampleId); dnaHash(sampleHash); require(traits.keys == DNA_TRAITS.toSet()); cleanText(outlierReason,240)
        return json("schema" to 1, "sample_id" to sampleId, "sample_hash" to sampleHash, "family_vote" to familyVote.name,
            "symbol_type" to symbolType.name, "traits" to json(*DNA_TRAITS.map { it to traits.getValue(it).json() }.toTypedArray()),
            "color_policy" to colorPolicy.name, "tile_vote" to tileVote.name, "depth_vote" to depthVote.name,
            "outlier_reason" to outlierReason, "do_not_copy_reference_logo" to true)
    }
    companion object {
        fun parse(r: JSONObject): DnaIconReport {
            r.exactKeys(setOf("schema","sample_id","sample_hash","family_vote","symbol_type","traits","color_policy",
                "tile_vote","depth_vote","outlier_reason","do_not_copy_reference_logo"))
            require(r.longValue("schema") == 1L && r.get("do_not_copy_reference_logo") == true)
            val traits = r.getJSONObject("traits"); traits.exactKeys(DNA_TRAITS.toSet())
            return DnaIconReport(r.stringValue("sample_id"),r.stringValue("sample_hash"),DnaFamilyVote.valueOf(r.stringValue("family_vote")),
                DnaSymbolType.valueOf(r.stringValue("symbol_type")), DNA_TRAITS.associateWith { DnaObservation.parse(traits.getJSONObject(it)) },
                PackColorPolicy.valueOf(r.stringValue("color_policy")),DnaTile.valueOf(r.stringValue("tile_vote")),DnaDepth.valueOf(r.stringValue("depth_vote")),
                r.stringValue("outlier_reason")).also { it.json() }
        }
        fun schema() = dnaObject(json("schema" to json("type" to "integer","enum" to JSONArray(listOf(1))),
            "sample_id" to dnaText(48),"sample_hash" to dnaText(64),"family_vote" to dnaEnum<DnaFamilyVote>(),
            "symbol_type" to dnaEnum<DnaSymbolType>(), "traits" to dnaObject(json(*DNA_TRAITS.map { it to DnaObservation.schema() }.toTypedArray())),
            "color_policy" to dnaEnum<PackColorPolicy>(),"tile_vote" to dnaEnum<DnaTile>(),"depth_vote" to dnaEnum<DnaDepth>(),
            "outlier_reason" to dnaText(),"do_not_copy_reference_logo" to json("type" to "boolean","enum" to JSONArray(listOf(true)))))
    }
}
internal data class DnaRule(val value: String, val provenance: DnaProvenance, val evidenceIds: List<String>) {
    fun json(): JSONObject {
        require(cleanText(value,240).isNotEmpty() && evidenceIds.size <= 24 && evidenceIds.distinct().size == evidenceIds.size)
        evidenceIds.forEach(::dnaId)
        require(provenance == DnaProvenance.UNKNOWN || evidenceIds.isNotEmpty())
        return json("value" to value,"provenance" to provenance.name,"evidence_ids" to JSONArray(evidenceIds))
    }
    companion object {
        fun parse(r: JSONObject): DnaRule { r.exactKeys(setOf("value","provenance","evidence_ids"))
            return DnaRule(r.stringValue("value"),DnaProvenance.valueOf(r.stringValue("provenance")),dnaStrings(r.getJSONArray("evidence_ids"),24,48)).also { it.json() } }
        fun schema() = dnaObject(json("value" to dnaText(),"provenance" to dnaEnum<DnaProvenance>(),"evidence_ids" to dnaArray(dnaText(48),24)))
    }
}
internal data class DnaColorRole(val role: String, val colors: List<String>, val observedCount: Int, val strength: String) {
    fun json(): JSONObject {
        require(role in setOf("neutral","accent","brand","background") && colors.size <= 6 && colors.distinct().size == colors.size)
        require(colors.all { it.matches(Regex("#FF[A-F0-9]{6}")) } && observedCount in 0..24 && strength in setOf("RECURRING","VARIABLE","TENTATIVE","UNKNOWN"))
        return json("role" to role,"colors" to JSONArray(colors),"observed_count" to observedCount,"strength" to strength)
    }
    companion object {
        fun parse(r: JSONObject): DnaColorRole { r.exactKeys(setOf("role","colors","observed_count","strength"))
            return DnaColorRole(r.stringValue("role"),dnaStrings(r.getJSONArray("colors"),6,9),r.longValue("observed_count").also { require(it in 0..24) }.toInt(),r.stringValue("strength")).also { it.json() } }
        fun schema() = dnaObject(json("role" to json("type" to "string","enum" to JSONArray(listOf("neutral","accent","brand","background"))),
            "colors" to dnaArray(dnaText(9),6),"observed_count" to json("type" to "integer","minimum" to 0,"maximum" to 24),
            "strength" to json("type" to "string","enum" to JSONArray(listOf("RECURRING","VARIABLE","TENTATIVE","UNKNOWN")))))
    }
}
internal data class DnaFamily(val id: String, val evidenceIds: List<String>, val applicability: List<DnaSymbolType>,
    val tile: DnaTile, val depth: DnaDepth, val colorPolicy: PackColorPolicy, val colorRoles: List<DnaColorRole>,
    val drawing: Map<String,DnaRule>, val must: List<DnaRule>, val should: List<DnaRule>, val mustNot: List<DnaRule>) {
    val observedCount get() = evidenceIds.size
    fun json(): JSONObject {
        dnaId(id); require(evidenceIds.size in 1..24 && evidenceIds.distinct().size == evidenceIds.size); evidenceIds.forEach(::dnaId)
        require(applicability.isNotEmpty() && applicability.distinct().size == applicability.size && drawing.keys == DNA_DRAWING.toSet())
        require(colorRoles.size <= 4 && colorRoles.map { it.role }.distinct().size == colorRoles.size && colorRoles.all { it.observedCount <= observedCount })
        (drawing.values + must + should + mustNot).forEach { rule -> rule.json(); require(rule.evidenceIds.all { it in evidenceIds }) }
        require(must.size <= 6 && should.size <= 6 && mustNot.size <= 6)
        return json("family_id" to id,"observed_count" to observedCount,"evidence_ids" to JSONArray(evidenceIds),
            "applicability" to JSONArray(applicability.map { it.name }),"tile" to tile.name,"depth" to depth.name,"color_policy" to colorPolicy.name,
            "color_roles" to JSONArray(colorRoles.map { it.json() }),"drawing" to json(*DNA_DRAWING.map { it to drawing.getValue(it).json() }.toTypedArray()),
            "must" to JSONArray(must.map { it.json() }),"should" to JSONArray(should.map { it.json() }),"must_not" to JSONArray(mustNot.map { it.json() }))
    }
    companion object {
        fun parse(r: JSONObject): DnaFamily {
            r.exactKeys(setOf("family_id","observed_count","evidence_ids","applicability","tile","depth","color_policy","color_roles","drawing","must","should","must_not"))
            val drawing = r.getJSONObject("drawing"); drawing.exactKeys(DNA_DRAWING.toSet())
            fun rules(key: String): List<DnaRule> = r.getJSONArray(key).let { a -> require(a.length() <= 6); (0 until a.length()).map { DnaRule.parse(a.getJSONObject(it)) } }
            val roles = r.getJSONArray("color_roles"); require(roles.length() <= 4)
            return DnaFamily(r.stringValue("family_id"),dnaStrings(r.getJSONArray("evidence_ids"),24,48),
                dnaStrings(r.getJSONArray("applicability"),5,32).map(DnaSymbolType::valueOf),DnaTile.valueOf(r.stringValue("tile")),
                DnaDepth.valueOf(r.stringValue("depth")),PackColorPolicy.valueOf(r.stringValue("color_policy")),
                (0 until roles.length()).map { DnaColorRole.parse(roles.getJSONObject(it)) },DNA_DRAWING.associateWith { DnaRule.parse(drawing.getJSONObject(it)) },
                rules("must"),rules("should"),rules("must_not")).also { require(r.longValue("observed_count") == it.observedCount.toLong()); it.json() }
        }
        fun schema() = dnaObject(json("family_id" to dnaText(48),"observed_count" to json("type" to "integer","minimum" to 1,"maximum" to 24),
            "evidence_ids" to dnaArray(dnaText(48),24),"applicability" to dnaArray(dnaEnum<DnaSymbolType>(),5),"tile" to dnaEnum<DnaTile>(),
            "depth" to dnaEnum<DnaDepth>(),"color_policy" to dnaEnum<PackColorPolicy>(),"color_roles" to dnaArray(DnaColorRole.schema(),4),
            "drawing" to dnaObject(json(*DNA_DRAWING.map { it to DnaRule.schema() }.toTypedArray())),
            "must" to dnaArray(DnaRule.schema(),6),"should" to dnaArray(DnaRule.schema(),6),"must_not" to dnaArray(DnaRule.schema(),6)))
    }
}
internal data class StyleDnaV1(val families: List<DnaFamily>, val recommendedFamily: String, val uncertainty: List<String>) {
    fun json(): JSONObject {
        require(families.size in 1..8 && families.map { it.id }.distinct().size == families.size && families.any { it.id == recommendedFamily })
        val ids = families.flatMap { it.evidenceIds }; require(ids.size in 1..24 && ids.distinct().size == ids.size)
        require(uncertainty.size <= 6); uncertainty.forEach { require(cleanText(it,240).isNotEmpty()) }
        return json("schema" to 1,"families" to JSONArray(families.map { it.json() }),"recommended_family" to recommendedFamily,"uncertainty" to JSONArray(uncertainty))
    }
    fun encode() = canonicalJson(json()).also { require(it.toByteArray().size <= 64*1024) }
    companion object {
        fun parse(text: String): StyleDnaV1 {
            val r = strictObject(text,64*1024,10); r.exactKeys(setOf("schema","families","recommended_family","uncertainty")); require(r.longValue("schema") == 1L)
            val a = r.getJSONArray("families"); require(a.length() in 1..8)
            return StyleDnaV1((0 until a.length()).map { DnaFamily.parse(a.getJSONObject(it)) },r.stringValue("recommended_family"),
                dnaStrings(r.getJSONArray("uncertainty"),6)).also { it.encode() }
        }
        fun schema() = dnaObject(json("schema" to json("type" to "integer","enum" to JSONArray(listOf(1))),
            "families" to dnaArray(DnaFamily.schema(),8),"recommended_family" to dnaText(48),"uncertainty" to dnaArray(dnaText(),6)))
    }
}
internal object StyleDnaCodec {
    fun parseReports(text: String, bindings: Map<String,String>): List<DnaIconReport> {
        require(bindings.size in 1..24); bindings.forEach { (id,hash) -> dnaId(id); dnaHash(hash) }
        val r = strictObject(text,128*1024,8); r.exactKeys(setOf("schema","reports")); require(r.longValue("schema") == 1L)
        val a = r.getJSONArray("reports"); require(a.length() == bindings.size)
        val reports = (0 until a.length()).map { DnaIconReport.parse(a.getJSONObject(it)) }
        require(reports.map { it.sampleId }.toSet() == bindings.keys && reports.map { it.sampleId }.distinct().size == reports.size)
        require(reports.all { bindings[it.sampleId] == it.sampleHash })
        return bindings.keys.map { id -> reports.single { it.sampleId == id } }
    }
    fun reportsSchema(max: Int) = dnaObject(json("schema" to json("type" to "integer","enum" to JSONArray(listOf(1))),
        "reports" to dnaArray(DnaIconReport.schema(),max)))
}
