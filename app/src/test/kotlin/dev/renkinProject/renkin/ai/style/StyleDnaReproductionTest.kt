package dev.renkinProject.renkin.ai.style

import android.app.Application
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Test-only checkpoint: these assertions compile against PR6 without new production APIs. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaReproductionTest {
    @Test fun missingAndEmptyResourcesHaveIndependentSafeReasonCounts() = runBlocking {
        val result = ReferenceSampler().sample(FixtureReferenceReader(mapOf("empty" to analysisFixturePng(alpha = 0)),
            mapOf("missing/Main" to "missing", "empty/Main" to "empty")))
        val counts = result.report.encode().optJSONObject("reasonCounts")
        assertNotNull("A missing drawable must not disappear inside broken/empty", counts)
        assertEquals(1, counts!!.getInt("MISSING_DRAWABLE"))
        assertEquals(1, counts.getInt("EMPTY_ALPHA"))
        assertEquals(2, result.report.broken)
    }
    @Test fun distinctMeasuredColorsAreNotDeduplicatedByShapeAndCoarseRgbAlone() {
        val base = referenceFeatures(analysisFixturePng())
        val a = base.copy(palette = listOf("#FF13171C"), paletteWeights = listOf(1.0))
        val b = a.copy(palette = listOf("#FF192423"))
        assertFalse("Independent measured color distance must veto an otherwise near shape", a.near(b))
    }
    @Test fun twentyFourRepresentativesAreAnExplicitAdditiveSamplerMode() {
        val png = analysisFixturePng(); val features = referenceFeatures(png)
        val input = (0 until 40).map { SampledReference("icon$it", "fixture", sha256("$it".toByteArray()), png, features) }
        val result = runCatching { diverseReferences(input, 24) }
        assertTrue("Standard must accept 24 representatives independently of the legacy 12 limit", result.isSuccess)
        assertEquals(24, result.getOrThrow().size)
    }
    @Test fun perIconReportsRequireFrozenIdsAndRejectMissingDuplicateAndForeignIds() {
        val type = runCatching { Class.forName("dev.renkinProject.renkin.ai.style.StyleDnaCodec") }.getOrNull()
        assertNotNull("A versioned per-icon parser is required before aggregate synthesis", type)
        val instance = type!!.getField("INSTANCE").get(null)
        val parse = type.getMethod("parseReports", String::class.java, Map::class.java)
        val bindings = mapOf("s01" to sha256("one".toByteArray()), "s02" to sha256("two".toByteArray()))
        fun report(id: String) = dnaReportFixture(id, bindings[id] ?: sha256("foreign".toByteArray()))
        fun body(ids: List<String>) = json("schema" to 1, "reports" to JSONArray(ids.map(::report))).toString()
        val valid = parse.invoke(instance, body(listOf("s01", "s02")), bindings) as List<*>
        assertEquals(2, valid.size)
        for (ids in listOf(listOf("s01"), listOf("s01", "s01"), listOf("s01", "foreign"))) {
            val failure = runCatching { parse.invoke(instance, body(ids), bindings) }.exceptionOrNull()
            assertTrue("Missing, duplicated or foreign labels cannot become successful reports", failure is java.lang.reflect.InvocationTargetException && failure.cause is IllegalArgumentException)
        }
    }
    @Test fun familyAwareGrammarExistsSeparatelyFromTheLegacyAggregate() {
        assertNotNull("Style DNA needs a versioned family grammar, not an 800-character aggregate",
            runCatching { Class.forName("dev.renkinProject.renkin.ai.style.StyleDnaV1") }.getOrNull())
    }
    @Test fun finalNonNullEditContractProhibitsUnevidencedModernizationAndDarkTile() {
        val prompt = PromptComposer.openAiEditPrompt("make a beautiful dark rounded tile with glow", 3)
        assertTrue("Generic modernization must be explicitly prohibited at the actual edit boundary", prompt.contains("generic modernization"))
        assertTrue("A dark rounded tile requires recurring family evidence", prompt.contains("recurring family evidence"))
        assertTrue(prompt.contains("Image 1")); assertTrue(prompt.contains("STYLE REFERENCES ONLY")); assertTrue(prompt.contains("exactly ONE"))
    }
}

internal fun dnaReportFixture(id: String, hash: String, family: String = "OUTLINE", policy: String = "ADAPT_BRAND_COLORS") = json(
    "schema" to 1, "sample_id" to id, "sample_hash" to hash, "family_vote" to family,
    "symbol_type" to "GEOMETRIC_PICTOGRAM", "traits" to json(*listOf("tile_geometry", "logo_silhouette", "stroke_fill", "corners",
        "optical_scale", "negative_space", "shadow_gradient", "luminance", "color_roles", "wordmark_handling", "simplification").map {
        it to json("value" to if (it == "tile_geometry") "No tile; transparent canvas" else if (it == "shadow_gradient") "No depth or gradient observed" else "Consistent planar contours",
            "provenance" to "OBSERVED")
    }.toTypedArray()), "color_policy" to policy, "outlier_reason" to "", "do_not_copy_reference_logo" to true)
