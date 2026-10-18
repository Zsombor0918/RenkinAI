package dev.renkinProject.renkin.ai.trial

import android.content.ComponentName
import org.robolectric.Shadows.shadowOf
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
    @Test fun montageContractBindsContactSheetsWithoutReinterpretingHistoricalReferences() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(refs = 2)
            val old = f.plan(listOf("a/Main")).freezeReferenceFirstPrompt()
            val legacy = old.encode()
            val context = JSONObject(old.context.encode()).put("montageFingerprint", "d".repeat(64))
            val next = old.copy(context = TrialContext.parse(context), promptVersion = 3, promptHashes = emptyMap())
            val frozen = next.copy(promptHashes = next.inputs.associate { it.key to sha256(next.effectivePrompt(it).toByteArray()) })
            frozen.validate()
            assertEquals(frozen, StyleTrialPlan.parse(frozen.encode()))
            val text = frozen.effectivePrompt(frozen.inputs.single())
            assertTrue(text.contains("contact-sheet montages"))
            assertTrue(text.contains("IMAGE 1")); assertTrue(text.contains("ONE standalone"))
            assertEquals(legacy, StyleTrialPlan.parse(legacy).encode())
            assertTrue(runCatching { old.copy(promptVersion = 3).validate() }.isFailure)
        }
    }
    @Test fun newPromptVersionBindsEffectiveBytesAndCannotMutateARestoredRequest() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(refs = 2)
            val old = f.plan(listOf("a/Main"))
            val frozenOld = old.encode(); val oldFingerprint = old.fingerprint
            val next = old.freezeReferenceFirstPrompt()
            assertEquals(1, StyleTrialPlan.parse(frozenOld).promptVersion)
            assertEquals(frozenOld, StyleTrialPlan.parse(frozenOld).encode())
            assertEquals(oldFingerprint, StyleTrialPlan.parse(frozenOld).fingerprint)
            assertFalse(frozenOld.contains("promptVersion")); assertFalse(frozenOld.contains("promptHashes"))
            assertNotEquals(old.fingerprint, next.fingerprint)
            assertEquals(next, StyleTrialPlan.parse(next.encode()))
            assertEquals(sha256(next.effectivePrompt(next.inputs.single()).toByteArray()), next.promptHashes.getValue("a/Main"))
            assertTrue(runCatching { next.copy(promptHashes = mapOf("a/Main" to "0".repeat(64))).validate() }.isFailure)
            assertTrue(runCatching { next.copy(specification = "changed", context = next.context.copy(
                styleFingerprint = styleFingerprint("changed", next.instruction))).validate() }.isFailure)
            assertTrue(runCatching { StyleTrialPlan.parse(JSONObject(next.encode()).put("promptVersion", 3).toString()) }.isFailure)
            assertTrue(runCatching { StyleTrialPlan.parse(JSONObject(next.encode()).also { it.remove("promptHashes") }.toString()) }.isFailure)
            assertTrue(runCatching { StyleTrialPlan.parse(JSONObject(next.encode()).put("promptVersion", 1).toString()) }.isFailure)
            val maximum = old.copy(specification = "\"".repeat(6000), instruction = "\\".repeat(2000),
                context = old.context.copy(dnaFingerprint = "e".repeat(64),
                    styleFingerprint = styleFingerprint("\"".repeat(6000), "\\".repeat(2000))),
                drawingRecipes = mapOf("a/Main" to "\\".repeat(6000))).freezeReferenceFirstPrompt()
            maximum.validate()
            assertEquals(maximum, StyleTrialPlan.parse(maximum.encode()))
        }
    }
    @Test fun historicalV1CanonicalFingerprintRemainsAnExactGolden() {
        val context = TrialContext(1, 1, 0, "CREATE", null, null, "a".repeat(64), "b".repeat(64),
            referenceFingerprint(emptyList()), "initial", "OpenAI", "gpt-image-2.5-flare", "medium")
        val plan = StyleTrialPlan(context, "outline", "", emptyList(), listOf(TrialInput("target/Main", "Target",
            "c".repeat(64), "component-resource", "22222222-2222-2222-2222-222222222222")), "11111111-1111-1111-1111-111111111111")
        // Android JSONObject.quote escapes the component slash as \/ in canonical bytes.
        assertEquals("ec4677faf200588ca7602d7e987b2006e69b57693892db901e3c4ee4585c8ebd", plan.fingerprint)
        assertEquals(plan.encode(), StyleTrialPlan.parse(plan.encode()).encode())
    }
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
    @Test fun optionalOriginalEvidenceRoundTripsWithoutRewritingLegacyPlanFingerprints() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            f.initialize(); val old = f.plan()
            assertEquals(old.encode(), StyleTrialPlan.parse(old.encode()).encode())
            assertFalse(old.encode().contains("originalEvidence"))
            val evidence = OriginalIconEvidence("APP_COMPONENT_RESOURCE", "adaptive", 0, 420, 123, false)
            val updated = old.copy(inputs = old.inputs.map { it.copy(originalEvidence = evidence) })
            assertNotEquals(old.fingerprint, updated.fingerprint)
            assertEquals(updated, StyleTrialPlan.parse(updated.encode()))
            assertTrue(runCatching { OriginalIconEvidence.parse(evidence.json().put("verification", "PRISTINE")) }.isFailure)
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
    @Test fun exactAliasResourceRemainsOriginalWhenTheActivityApiReturnsThemedArtwork() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val component = ComponentName(context.packageName, context.packageName + ".OriginalAlias")
        val shadow = shadowOf(context.packageManager)
        val info = shadow.addActivityIfNotPresent(component)
        info.icon = R.drawable.ic_github
        info.enabled = true; info.applicationInfo.enabled = true
        val sourcePackB = renderTrialOriginal(checkNotNull(context.getDrawable(R.mipmap.ic_launcher_foreground)))
        val genuineC = renderTrialOriginal(checkNotNull(context.getDrawable(R.drawable.ic_github)))
        assertNotEquals(sha256(sourcePackB), sha256(genuineC))
        for (appliedA in listOf(Color.WHITE, Color.GREEN)) {
            shadow.addActivityIcon(component, ColorDrawable(appliedA))
            val original = currentTrialOriginal(context, component.flattenToString())
            assertArrayEquals("A themed display/API icon must not replace exact component resource C", genuineC, original.png)
            assertTrue(original.origin.contains(component.flattenToString()))
            assertNotEquals(sha256(sourcePackB), original.hash)
        }
    }
    @Test fun wrongUserDisabledAndUnresolvableComponentResourcesFailWithoutSubstitution() = runBlocking {
        val context = RuntimeEnvironment.getApplication(); val shadow = shadowOf(context.packageManager)
        val component = ComponentName(context.packageName, context.packageName + ".UnsafeOriginalAlias")
        val info = shadow.addActivityIfNotPresent(component)
        info.icon = Int.MAX_VALUE; info.enabled = true; info.applicationInfo.enabled = true
        shadow.addActivityIcon(component, ColorDrawable(Color.WHITE))
        assertTrue(runCatching { currentTrialOriginal(context, component.flattenToString()) }.isFailure)
        info.icon = R.drawable.ic_github; val uid = info.applicationInfo.uid
        info.applicationInfo.uid = (android.os.Process.myUid() / 100000 + 1) * 100000 + 12345
        assertTrue(runCatching { currentTrialOriginal(context, component.flattenToString()) }.isFailure)
        info.applicationInfo.uid = uid; info.enabled = false
        assertTrue(runCatching { currentTrialOriginal(context, component.flattenToString()) }.isFailure)
    }
    @Test fun removedComponentNeverFallsBackToTheApplicationOrSourcePack() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val key = context.packageName + "/MissingLauncherAlias"
        assertTrue(runCatching { currentTrialOriginal(context, key) }.isFailure)
    }
}
