package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleTrialComposeTest {
    @Test fun frozenConfirmationNamesTheAppOriginalAndSeparatesTheStylePack() {
        val b = Backend(preparation(ProjectSetupMode.EXTEND, 1)); show(b); prepare()
        compose.onNodeWithText("Original app icon").assertExists()
        compose.onNodeWithTag("trial-frozen-confirmation").performScrollToNode(hasTestTag("trial-consent-details"))
        compose.onNodeWithTag("trial-consent-details").performClick()
        compose.onNodeWithText("launcher:p0/Main:1", substring = true).assertExists()
        compose.onNodeWithText("Style reference pack: source.pack", substring = true).assertExists()
        val input = b.flow.value.plan!!.inputs.single()
        compose.onNodeWithTag("trial-frozen-${input.requestId}").assertExists()
        assertEquals(hash, input.originalHash); assertEquals(0, b.paidPlans)
    }
    @get:Rule val compose = createComposeRule()
    private val png = projectTestPng(size = 1024)
    private val hash = sha256(png)
    private fun preparation(mode: ProjectSetupMode = ProjectSetupMode.CREATE, n: Int = 3): TrialPreparation {
        val source = if (mode == ProjectSetupMode.EXTEND) "source.pack" else null
        val stamp = if (source == null) null else "b".repeat(64)
        val targets = (0 until n).map { CoverageTarget("p$it/Main", "App $it") }
        val context = TrialContext(1, 1, 2, mode.name, source, stamp, "a".repeat(64), "c".repeat(64),
            referenceFingerprint(emptyList()), "initial:initial", AiSettings.OPENAI, AiSettings.MODELS.first(), "medium",
            styleFingerprint("saved outline", "separate instruction"))
        val selection = ProjectSelection(mode.name, source, stamp, projectCatalogStamp(targets), targets.map { it.key })
        return TrialPreparation(context, SavedProjectStyle(ProjectAiMetadata(1, mode.name, source, stamp, 1,
            selectionJson = selection.encode(), version = 2),
            StyleRevision(1, 1, specification = "saved outline", userInstruction = "separate instruction"), emptyList()),
            targets.map { TrialCandidate(it, true, true, false, "eligible") })
    }
    private inner class Backend(var base: TrialPreparation? = preparation()) : ProjectTrialBackend {
        val flow = MutableStateFlow(ProjectTrialState(preparation = base, hasKey = true))
        var paidPlans = 0; var stops = 0; var loads = 0; var key = true
        val packageEvents = mutableListOf<String>()
        var originalEvidence: OriginalIconEvidence? = null
        var verifiedPlan: String? = null
        val consumed = mutableSetOf<String>(); var pending: StyleTrialPlan? = null
        override fun state(id: Long) = flow
        override fun load(id: Long, mode: ProjectSetupMode, source: String?) {
            loads++; if (!flow.value.network) flow.value = flow.value.copy(preparation = base, plan = null, hasKey = key)
        }
        override fun packageChanged(id: Long, mode: ProjectSetupMode, source: String?, packageName: String) {
            assertEquals(1L, id); assertEquals(ProjectSetupMode.valueOf(base!!.context.mode), mode)
            assertEquals(base!!.context.sourcePackage, source); packageEvents += packageName
        }
        override fun options(id: Long, provider: String, model: String, quality: String) {
            base = base!!.copy(context = base!!.context.copy(provider = provider, model = model, quality = quality))
            flow.value = flow.value.copy(preparation = base, plan = null, ready = false)
        }
        override fun prepare(id: Long, keys: List<String>, allowCovered: Boolean) {
            val p = checkNotNull(base); StyleTrialTargetSelector.validateChoice(p.candidates, keys, allowCovered)
            val plan = StyleTrialPlan(p.context, p.style.revision!!.specification, p.style.revision.userInstruction,
                p.style.references.map { it.hash }, keys.map { TrialInput(it, p.candidates.single { c -> c.target.key == it }.target.name, hash, "launcher:$it:1", originalEvidence = originalEvidence) })
            flow.value = flow.value.copy(plan = plan)
        }
        override fun verifyOriginals(id: Long, planId: String, verified: Boolean) { verifiedPlan = if (verified) planId else null }
        override fun submit(id: Long, planId: String) {
            val p = flow.value.plan ?: return
            if (!key || flow.value.busy || p.id != planId || !consumed.add(planId)) return
            paidPlans++; pending = p
            flow.value = flow.value.copy(plan = null, busy = true, network = true,
                records = flow.value.records + StyleTrialRecord(p, System.currentTimeMillis(), true, p.inputs.map { TrialItemRecord(it.requestId, TrialStatus.PENDING) }))
        }
        fun finish() {
            val p = checkNotNull(pending)
            val variants = p.inputs.map { IconAssetVariant(it.requestId, 1, it.key, p.context.revision, it.originalHash, hash,
                trialContextJson = TrialVariantEvidence(p.context, p.id, it.requestId, it.origin, p.referenceHashes).encode()) }
            flow.value = flow.value.copy(busy = false, network = false, plan = null, variants = flow.value.variants + variants,
                records = flow.value.records.map { if (it.plan.id == p.id) it.copy(items = it.items.map { row -> row.copy(status = TrialStatus.REVIEWABLE, endpointCalls = 1, resultHash = hash) }) else it })
        }
        override fun cancelPreparation(id: Long) { flow.value = flow.value.copy(plan = null) }
        override fun stop(id: Long) {
            stops++; val p = checkNotNull(pending)
            flow.value = flow.value.copy(plan = null, busy = false, network = false, records = flow.value.records.map {
                if (it.plan.id == p.id) it.copy(items = it.items.mapIndexed { i, row -> row.copy(status =
                    if (i == 0) TrialStatus.UNCERTAIN else TrialStatus.SKIPPED, endpointCalls = if (i == 0) 1 else 0) }) else it })
        }
        override fun release(id: Long) {}
        override fun review(id: Long, variantId: String, accepted: Boolean) {
            flow.value = flow.value.copy(variants = flow.value.variants.map { if (it.id == variantId) it.copy(reviewStatus = if (accepted) "ACCEPTED" else "REJECTED",
                trialContextJson = TrialVariantEvidence.parse(it.trialContextJson!!).let { e ->
                    e.copy(reviewContext = base!!.context, reviewOriginalOrigin = e.originalOrigin).encode() }) else it }, ready = false)
        }
        override fun approve(id: Long) { check(flow.value.variants.any { it.reviewStatus == "ACCEPTED" }); flow.value = flow.value.copy(ready = true) }
        override suspend fun preview(id: Long, hash: String) = png
    }
    private fun show(b: Backend, onBack: () -> Unit = {}) {
        compose.setContent { RenkinTheme { ProjectTrialPanel(1, "Project", ProjectSetupMode.valueOf(b.base?.context?.mode ?: "CREATE"),
            b.base?.context?.sourcePackage, true, b, onBack, {}, {}) } }
    }
    private fun scroll(tag: String) { compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag(tag)) }
    private fun prepare() { scroll("trial-prepare"); compose.onNodeWithTag("trial-prepare").performClick() }
    @Test fun packageBroadcastsReachTrialAsTypedInvalidationsWithoutAnUnconditionalReload() {
        val b = Backend(preparation(ProjectSetupMode.EXTEND, 1)); show(b); compose.waitForIdle()
        val loads = b.loads
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        for ((action, pack) in listOf(
            android.content.Intent.ACTION_PACKAGE_CHANGED to "background.service",
            android.content.Intent.ACTION_PACKAGE_CHANGED to "p0",
            android.content.Intent.ACTION_PACKAGE_REPLACED to "source.pack",
            android.content.Intent.ACTION_PACKAGE_ADDED to "new.launcher",
            android.content.Intent.ACTION_PACKAGE_REMOVED to "old.launcher")) {
            compose.runOnIdle { context.sendBroadcast(android.content.Intent(action, android.net.Uri.parse("package:$pack"))) }
            compose.waitForIdle()
        }
        assertEquals(listOf("background.service", "p0", "source.pack", "new.launcher", "old.launcher"), b.packageEvents)
        assertEquals(loads, b.loads); assertEquals(0, b.paidPlans)
    }
    private fun generate(b: Backend): List<IconAssetVariant> {
        prepare(); compose.onNodeWithTag("trial-confirm-paid").performClick(); compose.runOnIdle { b.finish() }; return b.flow.value.variants
    }
    @Test fun openingPreparationDeclineAndRestorationNeverSubmit() {
        val b = Backend(); val restoration = StateRestorationTester(compose)
        restoration.setContent { RenkinTheme { ProjectTrialPanel(1, "Project", ProjectSetupMode.CREATE, null, true, b, {}, {}, {}) } }
        assertEquals(0, b.paidPlans); prepare(); compose.onNodeWithTag("trial-frozen-confirmation").assertExists()
        compose.onNodeWithTag("trial-decline").performClick(); assertEquals(0, b.paidPlans)
        restoration.emulateSavedInstanceStateRestore(); assertEquals(0, b.paidPlans)
        compose.onNodeWithTag("trial-confirm-paid").assertDoesNotExist()
    }
    @Test fun unverifiedResourceOriginalNeedsVisibleConsciousVerificationBeforePaidConsent() {
        val b = Backend(preparation(ProjectSetupMode.EXTEND, 1))
        b.originalEvidence = OriginalIconEvidence("APP_COMPONENT_RESOURCE", "adaptive", 0, 420, 123, false)
        show(b); prepare()
        compose.onNodeWithTag("trial-confirm-paid").assertIsNotEnabled()
        compose.onNodeWithTag("trial-frozen-confirmation").performScrollToNode(hasTestTag("trial-consent-details"))
        compose.onNodeWithTag("trial-consent-details").performClick()
        compose.onNodeWithTag("trial-frozen-confirmation").performScrollToNode(hasTestTag("trial-original-unverified"))
        compose.onNodeWithText("UNVERIFIED_ORIGINAL", substring = true).assertExists()
        compose.onNodeWithTag("trial-frozen-confirmation").performScrollToNode(hasTestTag("trial-verify-originals"))
        compose.onNodeWithTag("trial-verify-originals").performClick()
        assertEquals(b.flow.value.plan!!.id, b.verifiedPlan)
        compose.onNodeWithTag("trial-confirm-paid").assertIsEnabled()
        compose.onNodeWithTag("trial-decline").performClick(); assertEquals(0, b.paidPlans)
        prepare(); compose.onNodeWithTag("trial-confirm-paid").assertIsNotEnabled()
    }
    @Test fun explicitConfirmationDoubleTapReviewAndSeparateApprovalAreDistinct() {
        val b = Backend(); show(b); prepare()
        compose.onNodeWithText("The exact provider charge cannot be guaranteed", substring = true).assertExists()
        compose.onNodeWithTag("trial-confirm-paid").performTouchInput { doubleClick() }
        assertEquals(1, b.paidPlans); compose.onNodeWithTag("trial-confirm-paid").assertDoesNotExist()
        compose.runOnIdle { b.finish() }
        val rows = b.flow.value.variants
        rows.forEachIndexed { i, row -> val tag = if (i == 0) "trial-accept-${row.id}" else "trial-reject-${row.id}"; scroll(tag); compose.onNodeWithTag(tag).performClick() }
        compose.onNodeWithTag("trial-ready").assertDoesNotExist()
        scroll("trial-approve-style"); compose.onNodeWithTag("trial-approve-style").performClick()
        scroll("trial-ready"); compose.onNodeWithTag("trial-ready").assertExists(); assertEquals(1, b.paidPlans)
    }
    @Test fun maxThreeDistinctTargetsManualChangeAndZeroOneTwoCasesAreVisible() {
        val b = Backend(preparation(n = 4)); show(b)
        // Recommendations are random; the capacity/replacement guarantee is independent of which three were suggested.
        val unselected = (0..3).single { index ->
            scroll("trial-target-p$index/Main")
            compose.onNodeWithTag("trial-target-p$index/Main").fetchSemanticsNode().config[
                androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.Off
        }
        val removed = (0..3).first { it != unselected }
        scroll("trial-target-p$unselected/Main"); compose.onNodeWithTag("trial-target-p$unselected/Main").assertIsNotEnabled()
        scroll("trial-target-p$removed/Main"); compose.onNodeWithTag("trial-target-p$removed/Main").performClick()
        scroll("trial-target-p$unselected/Main"); compose.onNodeWithTag("trial-target-p$unselected/Main").performClick()
        prepare(); assertEquals(3, b.flow.value.plan!!.inputs.size); assertEquals(3, b.flow.value.plan!!.inputs.map { it.key }.distinct().size)
        assertTrue(b.flow.value.plan!!.inputs.any { it.key == "p$unselected/Main" })
        assertFalse(b.flow.value.plan!!.inputs.any { it.key == "p$removed/Main" }); assertEquals(0, b.paidPlans)
    }
    @Test fun originalAndGenerated48PxAndLargeAlphaPreviewsHaveAccessibleLabels() {
        val b = Backend(preparation(n = 1)); show(b); val row = generate(b).single()
        scroll("trial-large-true-${row.id}"); compose.onNodeWithTag("trial-large-true-${row.id}").assertExists()
        scroll("trial-48-true-${row.id}")
        compose.onNodeWithContentDescription("Generated at 48 px launcher size").assertExists()
        val bounds = compose.onNodeWithTag("trial-48-true-${row.id}").getUnclippedBoundsInRoot()
        assertEquals(48f, (bounds.right - bounds.left).value, .1f)
        scroll("trial-preview-options-toggle"); compose.onNodeWithTag("trial-preview-options-toggle").performClick()
        scroll("trial-preview-theme"); compose.onNodeWithTag("trial-preview-theme").performClick()
        scroll("trial-preview-alpha"); compose.onNodeWithTag("trial-preview-alpha").performClick(); assertEquals(1, b.paidPlans)
    }
    @Test fun regenerationAsksForNewConsentAndKeepsAcceptedOldVariant() {
        val b = Backend(preparation(n = 1)); show(b); val old = generate(b).single()
        scroll("trial-accept-${old.id}"); compose.onNodeWithTag("trial-accept-${old.id}").performClick()
        scroll("trial-regenerate-${old.id}"); compose.onNodeWithTag("trial-regenerate-${old.id}").performClick()
        assertNotEquals(old.id, b.flow.value.plan!!.inputs.single().requestId); assertEquals(1, b.paidPlans)
        compose.onNodeWithTag("trial-decline").performClick()
        assertEquals("ACCEPTED", b.flow.value.variants.single().reviewStatus); assertEquals(1, b.paidPlans)
    }
    @Test fun noKeyAndStabilityWrongReferenceCountDisablePaidPreparation() {
        val b = Backend(); b.key = false; show(b)
        scroll("trial-prepare"); compose.onNodeWithTag("trial-prepare").assertIsNotEnabled(); assertEquals(0, b.paidPlans)
        compose.runOnIdle { b.key = true; b.load(1, ProjectSetupMode.CREATE, null); b.options(1, AiSettings.STABILITY, AiSettings.MODELS.first(), "medium") }
        scroll("trial-reference-error"); compose.onNodeWithTag("trial-reference-error").assertExists()
        scroll("trial-prepare"); compose.onNodeWithTag("trial-prepare").assertIsNotEnabled()
    }
    @Test fun emptySourceCoveredAndUncertainTargetsNeverFabricateThreeRequests() {
        val b = Backend(preparation(ProjectSetupMode.EXTEND, 2).let { p -> p.copy(candidates = p.candidates.map {
            it.copy(recommended = false, overrideRequired = true, reason = "covered_override")
        }) }); show(b)
        scroll("trial-empty"); compose.onNodeWithTag("trial-empty").assertExists()
        scroll("trial-prepare"); compose.onNodeWithTag("trial-prepare").assertIsNotEnabled()
        scroll("trial-covered-override"); compose.onNodeWithTag("trial-covered-override").performClick()
        for (i in 0..1) { scroll("trial-target-p$i/Main"); compose.onNodeWithTag("trial-target-p$i/Main").performClick() }
        prepare(); assertEquals(2, b.flow.value.plan!!.inputs.size); assertEquals(0, b.paidPlans)
    }
    @Test fun stopShowsUncertainAndSkippedAndReopenDoesNotContinue() {
        val b = Backend(); show(b); prepare(); compose.onNodeWithTag("trial-confirm-paid").performClick()
        scroll("trial-stop"); compose.onNodeWithTag("trial-stop").performClick()
        assertEquals(1, b.stops); assertEquals(TrialStatus.UNCERTAIN, b.flow.value.records.single().items.first().status)
        scroll("trial-options-toggle"); compose.onNodeWithTag("trial-options-toggle").performClick()
        scroll("trial-reload"); compose.onNodeWithTag("trial-reload").performClick(); assertEquals(1, b.paidPlans)
    }
    @Test fun staleRevisionPreservesHistoryButDisablesReviewAndRegeneration() {
        val b = Backend(preparation(n = 1)); show(b); val row = generate(b).single()
        compose.runOnIdle { b.base = b.base!!.copy(context = b.base!!.context.copy(revision = 2)); b.load(1, ProjectSetupMode.CREATE, null) }
        scroll("trial-history-toggle"); compose.onNodeWithTag("trial-history-toggle").performClick()
        scroll("trial-historical-${row.id}"); compose.onNodeWithTag("trial-historical-${row.id}").assertExists()
        scroll("trial-accept-${row.id}"); compose.onNodeWithTag("trial-accept-${row.id}").assertIsNotEnabled()
        assertEquals(1, b.flow.value.variants.size); assertEquals(1, b.paidPlans)
    }
    @Test @Config(qualifiers = "w900dp-h800dp-night") fun wideDarkTrialScreenRetainsSameLocalSelectionContract() {
        val b = Backend(preparation(n = 2)); show(b); prepare()
        assertEquals(2, b.flow.value.plan!!.inputs.size); assertEquals(0, b.paidPlans)
        compose.onNodeWithTag("trial-decline").performClick()
    }
    @Test fun changedEpochOffersExplicitLocalRecheckAndSeparateApprovalWithoutPaidRequest() {
        val b = Backend(preparation(n = 1)); show(b); val row = generate(b).single()
        scroll("trial-accept-${row.id}"); compose.onNodeWithTag("trial-accept-${row.id}").performClick()
        compose.runOnIdle { b.base = b.base!!.copy(context = b.base!!.context.copy(epoch = "restored:initial")); b.load(1, ProjectSetupMode.CREATE, null) }
        scroll("trial-history-toggle"); compose.onNodeWithTag("trial-history-toggle").performClick()
        scroll("trial-recheck-${row.id}"); compose.onNodeWithTag("trial-recheck-${row.id}").performClick()
        compose.onNodeWithTag("trial-recheck-cancel").performClick(); assertEquals(1, b.paidPlans)
        scroll("trial-approve-style"); compose.onNodeWithTag("trial-approve-style").assertIsNotEnabled()
        scroll("trial-recheck-${row.id}"); compose.onNodeWithTag("trial-recheck-${row.id}").performClick()
        compose.onNodeWithTag("trial-recheck-confirm").performClick()
        scroll("trial-approve-style"); compose.onNodeWithTag("trial-approve-style").performClick()
        assertTrue(b.flow.value.ready); assertEquals(1, b.paidPlans); assertEquals(1, b.flow.value.variants.size)
    }
    @Test fun actualShellTargetsStyleRefinementAndExternalEditorPriorityRemainConnected() {
        val b = Backend(preparation(n = 1)); var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
        var attention by mutableStateOf(false)
        val style = object : ProjectStyleBackend {
            override suspend fun load(id: Long) = b.base!!.style
            override suspend fun importReference(id: Long, uri: android.net.Uri): ReferenceAsset = error("unused")
            override suspend fun copyGlobal(id: Long) = ProjectStyleDraft("", "", emptyList())
            override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle {
                val old = b.base!!; val rev = StyleRevision(1, expected!! + 1, specification = draft.specification, userInstruction = draft.instruction)
                val saved = old.style.copy(revision = rev, metadata = old.style.metadata!!.copy(activeRevision = rev.revision, version = old.context.metadataVersion + 1))
                b.base = old.copy(context = old.context.copy(revision = rev.revision, metadataVersion = old.context.metadataVersion + 1,
                    styleFingerprint = styleFingerprint(rev.specification, rev.userInstruction)), style = saved)
                return saved
            }
        }
        compose.setContent { RenkinTheme {
            ProjectShell(ProjectHomeState(listOf(Profile(1, "Project")), availability = ProjectHomeAvailability.READY), operation,
                editorAttention = attention, onOpen = { operation = ProjectSessionState.Opened(it) }, onCreate = { _, _, _ -> false },
                onSave = {}, onCancel = {}, onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                readSetupContext = { b.base!!.style.metadata },
                setupData = { _, _, _, _ -> ProjectSetupData(targets = b.base!!.candidates.map { it.target }, savedMetadata = b.base!!.style.metadata) },
                stylePanel = { id, mode, source, stamp, back, trials -> ProjectStylePanel(id, mode, source, stamp, true, style, back, trials) },
                trialPanel = { id, mode, source, back, refine, editor -> ProjectTrialPanel(id, "Project", mode, source, true, b, back, refine, editor) }) {
                ProjectEditorBackHandler(it) {}; Column { Text("classic", Modifier.testTag("trial-classic-editor")) }
            }
        } }
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1"))
        compose.onNodeWithTag("project-setup-1").performClick(); compose.onNodeWithTag("setup-trials").performScrollTo().performClick()
        compose.onNodeWithTag("project-trials-1").assertExists(); val row = generate(b).single()
        scroll("trial-options-toggle"); compose.onNodeWithTag("trial-options-toggle").performClick()
        scroll("trial-refine"); compose.onNodeWithTag("trial-refine").performClick()
        compose.onNodeWithTag("project-style-description").performScrollTo().performTextReplacement("refined solid")
        // The expanded manual form is a LazyColumn: materialize the real
        // retained Save action before interacting with its semantics node.
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-save"))
        compose.onNodeWithTag("project-style-save").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithTag("style-trials").performScrollTo().performClick()
        scroll("trial-history-toggle"); compose.onNodeWithTag("trial-history-toggle").performClick()
        scroll("trial-historical-${row.id}"); compose.onNodeWithTag("trial-historical-${row.id}").assertExists()
        assertEquals(1, b.paidPlans); compose.runOnIdle { attention = true }
        compose.onNodeWithTag("trial-classic-editor").assertExists(); assertEquals(1, b.paidPlans)
    }
}
