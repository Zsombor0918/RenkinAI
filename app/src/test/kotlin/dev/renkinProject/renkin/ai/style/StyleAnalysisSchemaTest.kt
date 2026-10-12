package dev.renkinProject.renkin.ai.style

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class StyleAnalysisSchemaTest {
    private fun reject(text: String) { assertThrows(Exception::class.java) { StyleProfileV1.parse(text) } }
    private fun changed(key: String, value: Any?) = JSONObject(analysisFixtureStyle().encode()).put(key, value ?: JSONObject.NULL).toString()

    @Test fun canonicalRoundTripAndExplicitUnknownAreStable() {
        val style = analysisFixtureStyle(); val one = StyleProfileV1.parse(style.encode())
        assertEquals(style, one); assertEquals(one.encode(), StyleProfileV1.parse(one.encode()).encode())
        val unknown = StyleProfileV1(shortStyleDescription = "Uncertain artwork")
        assertEquals(unknown, StyleProfileV1.parse(unknown.encode()))
    }
    @Test fun strictSupportedSchemaRequiresAllKeysAndNoAdditionalProperties() {
        val schema = StyleProfileV1.jsonSchema()
        assertFalse(schema.getBoolean("additionalProperties"))
        assertEquals(schema.getJSONObject("properties").keys().asSequence().toSet(),
            schema.getJSONArray("required").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
        val text = schema.toString()
        assertFalse(text.contains("minLength")); assertFalse(text.contains("pattern"))
        assertEquals("integer", schema.getJSONObject("properties").getJSONObject("schemaVersion").getString("type"))
    }
    @Test fun missingExtraNullAndCoercedPropertiesAreRejected() {
        reject(JSONObject(analysisFixtureStyle().encode()).apply { remove("stroke") }.toString())
        reject(changed("extra", true)); reject(changed("stroke", null)); reject(changed("mixedStyleWarning", "false"))
        reject(changed("schemaVersion", "1")); reject(changed("schemaVersion", 1.0)); reject(changed("schemaVersion", 2))
    }
    @Test fun unsupportedCategoriesAndInventedConfidenceAreRejected() {
        listOf("archetype", "backgroundMode", "stroke", "cornerTreatment", "opticalPadding", "detailLevel", "selfAssessment")
            .forEach { reject(changed(it, "definitely100percent")) }
        reject(changed("confidence", .99)); reject(changed("selfAssessment", .99))
    }
    @Test fun paletteAndTextAreBoundedWithoutSilentTruncation() {
        reject(changed("paletteArgb", JSONArray(listOf("red"))))
        reject(changed("paletteArgb", JSONArray(listOf("#FFFFFFFF", "#FFFFFFFF"))))
        reject(changed("paletteArgb", JSONArray((0..6).map { "#FF00000$it" })))
        reject(changed("shortStyleDescription", "x".repeat(801))); reject(changed("shortStyleDescription", " "))
        reject(changed("uncertaintyNotes", JSONArray(listOf("x".repeat(161)))))
        reject(changed("identityPreservationNotes", JSONArray((0..6).map { "note $it" })))
        reject(changed("forbiddenFeatures", JSONArray(listOf(123))))
        assertEquals(listOf("#FFAABBCC"), StyleProfileV1.parse(changed("paletteArgb", JSONArray(listOf("#ffaabbcc")))).paletteArgb)
    }
    @Test fun controlCharactersDuplicateKeysAndPermissiveJsonAreRejected() {
        reject(changed("shortStyleDescription", "bad\u0000text")); reject(changed("shortStyleDescription", "bad\ntext"))
        val valid = analysisFixtureStyle().encode()
        reject(valid.replaceFirst("{", "{\"schemaVersion\":1,"))
        reject(valid.dropLast(1) + ",}"); reject(valid + "{}"); reject(valid.dropLast(5))
        reject("/*comment*/$valid"); reject(valid.replace("\"schemaVersion\"", "schemaVersion"))
    }
    @Test fun depthSizeNonfiniteAndMalformedSurrogatesFailStrictReader() {
        listOf("{\"a\":NaN}", "{\"a\":1e999}", "{\"a\":01}", "{\"a\":\"\\uD800\"}", "{\"a\":\"\\uDC00\"}")
            .forEach { text -> assertThrows(Exception::class.java) { strictObject(text) } }
        assertThrows(Exception::class.java) { strictObject("{\"a\":{\"b\":{\"c\":{}}}}", 100, 2) }
        assertThrows(Exception::class.java) { strictObject("{\"a\":\"" + "x".repeat(200) + "\"}", 100) }
        assertEquals("\uD83D\uDE00", strictObject("{\"a\":\"\\uD83D\\uDE00\"}").getString("a"))
    }
    @Test fun incompleteRefusedTruncatedAndUnexpectedToolOutputNeverBecomeStyle() {
        assertEquals(StyleAnalysisOutcome.InvalidResponse, parseAnalysisResponse(JSONObject(analysisFixtureResponse()).put("status", "incomplete").toString()))
        assertEquals(StyleAnalysisOutcome.InvalidResponse, parseAnalysisResponse(analysisFixtureResponse().dropLast(2)))
        val refused = JSONObject(analysisFixtureResponse()).apply {
            getJSONArray("output").getJSONObject(1).put("content", JSONArray().put(json("type" to "refusal", "refusal" to "no")))
        }
        assertEquals(StyleAnalysisOutcome.Refused, parseAnalysisResponse(refused.toString()))
        assertEquals(StyleAnalysisOutcome.InvalidResponse, parseAnalysisResponse(JSONObject(analysisFixtureResponse())
            .put("output", JSONArray().put(json("type" to "function_call"))).toString()))
    }
    @Test fun localReportOverflowUnknownKeysAndInconsistentCountsFail() {
        val valid = ReferenceSampleReport(2, 1, 1, 0, 0, false, true)
        assertEquals(valid, ReferenceSampleReport.parse(valid.encode()))
        assertThrows(Exception::class.java) { ReferenceSampleReport.parse(valid.encode().put("examined", 4294967298L)) }
        assertThrows(Exception::class.java) { ReferenceSampleReport.parse(valid.encode().put("extra", true)) }
        assertThrows(Exception::class.java) { ReferenceSampleReport.parse(valid.encode().put("valid", 2)) }
    }
    @Test fun analysisContextRejectsMalformedNestedIdentityAndOwner() {
        val own = analysisFixtureRequest().context
        assertEquals(own, AnalysisContext.parse(own.encode()))
        assertThrows(Exception::class.java) { AnalysisContext.parse(own.encode().put("sourceIdentity", "false")) }
        assertThrows(Exception::class.java) { own.copy(profileId = 0).validate() }
        assertThrows(Exception::class.java) { own.copy(mode = "EXTEND").validate() }
    }
}
