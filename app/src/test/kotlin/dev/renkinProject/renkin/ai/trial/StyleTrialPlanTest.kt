package dev.renkinProject.renkin.ai.trial

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.style.PromptComposer
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleTrialPlanTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun frozenPlanRoundTripsAndEveryCostInputChangesFingerprint() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(); val p = f.plan(); assertEquals(p, StyleTrialPlan.parse(p.encode()))
            for (changed in listOf(p.copy(instruction = "changed", context = p.context.copy(styleFingerprint = styleFingerprint(p.specification, "changed"))), p.copy(context = p.context.copy(quality = "high")),
                p.copy(context = p.context.copy(catalogStamp = "b".repeat(64))), p.copy(context = p.context.copy(epoch = "restored:initial")),
                p.copy(inputs = p.inputs.reversed()), p.copy(inputs = p.inputs.map { it.copy(originalHash = "c".repeat(64)) })))
                assertNotEquals(p.fingerprint, changed.fingerprint)
            assertEquals(3, p.endpointMaximum)
        }
    }
    @Test fun strictSchemaTamperingDuplicateTargetsAndStabilityReferenceCountsReject() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(); val p = f.plan()
            assertTrue(runCatching { p.copy(inputs = p.inputs + p.inputs.first()).validate() }.isFailure)
            assertTrue(runCatching { p.copy(context = p.context.copy(provider = AiSettings.STABILITY)).validate() }.isFailure)
            assertTrue(runCatching { StyleTrialPlan.parse(JSONObject(p.encode()).put("schema", 2).toString()) }.isFailure)
            assertTrue(runCatching { StyleTrialPlan.parse(p.encode().dropLast(1) + ",\"schema\":1}") }.isFailure)
        }
    }
    @Test fun composerPreservesSeparateUserInstructionEscapingAndMaximumValidSavedInputs() {
        val text = PromptComposer.composeSaved("x".repeat(6000), "u".repeat(2000))
        assertTrue(text.length <= 16000); assertTrue(text.contains("u".repeat(2000)))
        assertTrue(PromptComposer.composeSaved("outline", "quoted \"value\"").contains("\\\"value\\\""))
        assertTrue(text.contains("original app identity")); assertTrue(text.contains("transparent PNG"))
        assertTrue(PromptComposer.composeSaved("\"".repeat(6000), "\"".repeat(2000)).length <= dev.renkinProject.renkin.ai.MAX_COMPOSED_ICON_PROMPT)
    }
    @Test fun numericalRevisionReuseCannotMakeDifferentStyleTextAnIdenticalInput() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(); val p = f.plan()
            val other = p.context.copy(styleFingerprint = styleFingerprint("different restored style", p.instruction))
            assertFalse(p.context.sameArtworkInputs(other))
            assertTrue(runCatching { p.copy(context = other).validate() }.isFailure)
            val legacy = p.context.copy(styleFingerprint = "")
            assertEquals(legacy, TrialContext.parse(org.json.JSONObject(legacy.encode())))
            assertFalse(legacy.sameArtworkInputs(p.context))
            assertTrue(runCatching { f.journal.claim(p.copy(context = legacy), f.images(p)) }.isFailure)
        }
    }
    @Test fun realVectorAdaptiveAndTransparentOriginalRenderingIsBoundedAndRejectsBrokenArtwork() {
        val application = RuntimeEnvironment.getApplication()
        val vector = checkNotNull(application.getDrawable(R.drawable.ic_github))
        val adaptive = AdaptiveIconDrawable(ColorDrawable(Color.BLUE), ColorDrawable(Color.RED))
        for (drawable in listOf(vector, adaptive)) assertTrue(renderTrialOriginal(drawable).size <= 4 * 1024 * 1024)
        assertTrue(runCatching { renderTrialOriginal(ColorDrawable(Color.TRANSPARENT)) }.isFailure)
        val broken = object : Drawable() {
            override fun draw(canvas: android.graphics.Canvas) { error("broken resource") }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(filter: android.graphics.ColorFilter?) {}
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
        assertTrue(runCatching { renderTrialOriginal(broken) }.isFailure)
    }
}
