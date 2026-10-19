package dev.renkinProject.renkin.ai.style

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal enum class StyleArchetype { OUTLINE, SOLID, DUOTONE, FLAT, MIXED, UNKNOWN }
internal enum class StyleBackground { TRANSPARENT, TILE, MIXED, UNKNOWN }
internal enum class StyleStroke { THIN, MEDIUM, THICK, UNKNOWN }
internal enum class StyleCorners { ROUND, SHARP, MIXED, UNKNOWN }
internal enum class StylePadding { SMALL, MEDIUM, GENEROUS, UNKNOWN }
internal enum class StyleDetail { SIMPLE, MODERATE, DETAILED, UNKNOWN }
/** Model self assessment only, never calibrated confidence or a probability. */
internal enum class StyleAssessment { UNKNOWN, TENTATIVE, CONSISTENT }

internal data class StyleProfileV1(
    val archetype: StyleArchetype = StyleArchetype.UNKNOWN,
    val paletteArgb: List<String> = emptyList(),
    val backgroundMode: StyleBackground = StyleBackground.UNKNOWN,
    val stroke: StyleStroke = StyleStroke.UNKNOWN,
    val cornerTreatment: StyleCorners = StyleCorners.UNKNOWN,
    val opticalPadding: StylePadding = StylePadding.UNKNOWN,
    val detailLevel: StyleDetail = StyleDetail.UNKNOWN,
    val forbiddenFeatures: List<String> = emptyList(),
    val identityPreservationNotes: List<String> = emptyList(),
    val shortStyleDescription: String,
    val uncertaintyNotes: List<String> = emptyList(),
    val mixedStyleWarning: Boolean = false,
    val selfAssessment: StyleAssessment = StyleAssessment.UNKNOWN
) {
    fun validate() {
        require(paletteArgb.size <= 6 && paletteArgb.distinct().size == paletteArgb.size)
        require(paletteArgb.all { it.matches(Regex("#[A-F0-9]{8}")) })
        require(cleanText(shortStyleDescription, 800).isNotEmpty())
        for (list in listOf(forbiddenFeatures, identityPreservationNotes, uncertaintyNotes)) {
            require(list.size <= 6 && list.all { cleanText(it, 160).isNotEmpty() })
        }
    }
    fun encode(): String { validate(); return canonicalJson(json(
        "schemaVersion" to 1, "archetype" to wire(archetype), "paletteArgb" to JSONArray(paletteArgb),
        "backgroundMode" to wire(backgroundMode), "stroke" to wire(stroke), "cornerTreatment" to wire(cornerTreatment),
        "opticalPadding" to wire(opticalPadding), "detailLevel" to wire(detailLevel),
        "forbiddenFeatures" to JSONArray(forbiddenFeatures), "identityPreservationNotes" to JSONArray(identityPreservationNotes),
        "shortStyleDescription" to shortStyleDescription, "uncertaintyNotes" to JSONArray(uncertaintyNotes),
        "mixedStyleWarning" to mixedStyleWarning, "selfAssessment" to wire(selfAssessment))) }

    companion object {
        private val keys = setOf("schemaVersion", "archetype", "paletteArgb", "backgroundMode", "stroke",
            "cornerTreatment", "opticalPadding", "detailLevel", "forbiddenFeatures", "identityPreservationNotes",
            "shortStyleDescription", "uncertaintyNotes", "mixedStyleWarning", "selfAssessment")
        private fun wire(e: Enum<*>) = e.name.lowercase(Locale.ROOT)
        private inline fun <reified T : Enum<T>> category(value: String): T =
            enumValues<T>().firstOrNull { wire(it) == value } ?: error("Unknown style category.")
        private fun strings(root: JSONObject, key: String): List<String> = root.getJSONArray(key).let { a ->
            require(a.length() <= 6); (0 until a.length()).map { cleanText(a.get(it) as? String ?: error("Expected text list."), 160) }
        }
        fun parse(text: String): StyleProfileV1 {
            val r = strictObject(text, 16384, 4); r.exactKeys(keys); require(r.longValue("schemaVersion") == 1L)
            return StyleProfileV1(category(r.stringValue("archetype")),
                strings(r, "paletteArgb").map { it.uppercase(Locale.ROOT) }, category(r.stringValue("backgroundMode")),
                category(r.stringValue("stroke")), category(r.stringValue("cornerTreatment")), category(r.stringValue("opticalPadding")),
                category(r.stringValue("detailLevel")), strings(r, "forbiddenFeatures"), strings(r, "identityPreservationNotes"),
                cleanText(r.stringValue("shortStyleDescription"), 800), strings(r, "uncertaintyNotes"),
                r.get("mixedStyleWarning") as? Boolean ?: error("Expected boolean."), category(r.stringValue("selfAssessment")))
                .also { it.validate() }
        }
    }
}
