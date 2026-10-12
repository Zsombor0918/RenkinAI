package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.data.IconPack
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

/** Actual HOME/SETUP/STYLE routes, dialogs and user gestures; the backend is offline.
 * Domain/HTTP/Room behavior is independently covered by the corresponding real tests. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectStyleAnalysisComposeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var activity: ComponentActivity
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Default"), Profile(2, "Same")), availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var dark by mutableStateOf(false)
    private var ingress by mutableLongStateOf(0)
    private var launched = -1
    private val pngs = linkedMapOf<String, ByteArray>()
    private var imports = 0; private var posts = 0; private var preparations = 0; private var uses = 0; private var saves = 0
    private var failRefresh = false; private var failSave = false
    private val records = mutableMapOf<Long, SavedProjectStyle>()
    private val state = MutableStateFlow(ProjectAnalysisState(hasKey = true))
    private val index = FixtureReferenceReader(emptyMap()).index
    private val targets = listOf(CoverageTarget("pkg/Main", "App"))
    private val data = ProjectSetupData(targets = targets, packs = listOf(IconPack("fixture.pack", "Fixture pack", 7, "7", 0)),
        coverage = CoverageUiState.Ready(CoverageSnapshot(1, index, listOf(CoverageEntry(targets.single(), SourceCoverage.UNMAPPED, null, CoverageReason.NO_MAPPING)))))
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) { launched = requestCode }
    }
    private val registryOwner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
    private val analysis = object : ProjectAnalysisBackend {
        override fun state(id: Long) = state
        override fun prepare(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?, draft: ProjectStyleDraft) {
            preparations++
            val images = if (mode == ProjectSetupMode.EXTEND) analysisFixtureRequest(id, 12).images else draft.hashes.map { AnalysisImage(it, pngs.getValue(it)) }
            if (images.isEmpty()) { state.value = state.value.copy(error = "empty"); return }
            val context = analysisFixtureRequest(id).context.copy(mode = mode.name, sourcePackage = source, setupStamp = setupStamp,
                referenceStamp = if (source == null) null else projectSourceStamp(index), sourceIdentity = if (source == null) null else index.stamp,
                baseRevision = expected, metadataVersion = records[id]?.metadata?.version ?: 0)
            val report = ReferenceSampleReport(images.size, images.size, 0, 0, 0, mode == ProjectSetupMode.EXTEND, images.size < 8)
            val request = StyleAnalysisRequest(context, images, draft.specification, draft.instruction, localReport = report)
            state.value = state.value.copy(preparation = request, preparedDraftHash = analysisDraftHash(draft), error = null,
                samples = ReferenceSamples(source, request.context.referenceStamp, images.mapIndexed { n, image ->
                    SampledReference("fixture$n", "fixture$n", image.hash, image.png, referenceFeatures(image.png)) }, report, sourceIdentity = context.sourceIdentity))
        }
        override fun submit(id: Long, requestId: String) { check(requestId == state.value.preparation!!.requestId); posts++; state.value = state.value.copy(busy = true, network = true) }
        override fun cancel(id: Long) { val request = state.value.preparation; state.value = state.value.copy(busy = false, network = false,
            record = request?.let { record(it, AnalysisStatus.UNCERTAIN) }, preparation = null) }
        override suspend fun refresh(id: Long) { check(!failRefresh) }
        override suspend fun preview(id: Long, requestId: String, hash: String) = checkNotNull(state.value.record).let { candidate ->
            assertEquals(requestId, candidate.requestId); pngs[hash] ?: analysisFixtureRequest(id, 12).images.first { it.hash == hash }.png }
        override suspend fun use(id: Long, requestId: String, hashes: List<String>, allowStaleSource: Boolean): AnalyzedDraft {
            uses++; require(hashes.size <= 5); val candidate = state.value.record!!
            assertEquals(requestId, candidate.requestId)
            return AnalyzedDraft(PromptComposer.specification(candidate.style!!), hashes, requestId, 1234, candidate.context?.referenceStamp)
        }
        override suspend fun discardCandidate(id: Long, requestId: String) { state.value = state.value.copy(record = state.value.record!!.copy(status = AnalysisStatus.FAILED, reason = "discarded")) }
        override fun releasePreview(id: Long) { state.value = state.value.copy(preparation = null, samples = null) }
    }
    private val backend = object : ProjectStyleBackend {
        override val analysis = this@ProjectStyleAnalysisComposeTest.analysis
        override suspend fun load(id: Long) = records[id] ?: SavedProjectStyle(null, null, emptyList())
        override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset {
            val png = analysisFixturePng(imports % 3, imports++); val hash = sha256(png); pngs[hash] = png
            return ReferenceAsset(id, hash, ProjectAssetStore.path(id, hash), png.size.toLong(), 256, 256)
        }
        override suspend fun referencePreview(id: Long, hash: String) = pngs[hash]
        override suspend fun copyGlobal(id: Long) = ProjectStyleDraft("copied global", "", emptyList())
        override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle {
            check(!failSave); check(id == home.activeProfileId); saves++
            val origin = if (draft.analysisRequestId != null && draft.specification == PromptComposer.specification(analysisFixtureStyle())) "AUTO_ANALYSIS" else "USER"
            return SavedProjectStyle(ProjectAiMetadata(id, mode.name, source, stamp, (expected ?: 0) + 1),
                StyleRevision(id, (expected ?: 0) + 1, specification = draft.specification, userInstruction = draft.instruction, origin = origin),
                draft.hashes.map { ReferenceAsset(id, it, ProjectAssetStore.path(id, it), 100, 256, 256) }).also { records[id] = it }
        }
    }
    private fun record(request: StyleAnalysisRequest, status: AnalysisStatus) = AnalysisRecord(request.context.profileId, request.requestId,
        request.fingerprint, request.context, request.model, request.description, request.userInstruction, request.images.map { it.hash },
        request.promptHash, 123, status, if (status == AnalysisStatus.RESULT_SAVED) analysisFixtureStyle().encode() else null,
        if (status == AnalysisStatus.RESULT_SAVED) "resp_fixture" else null,
        if (status == AnalysisStatus.RESULT_SAVED) sha256(analysisFixtureStyle().encode().toByteArray()) else null,
        billingCertainty = if (status == AnalysisStatus.RESULT_SAVED) "RESPONSE_RECEIVED" else "UNKNOWN", localReport = request.localReport)
    private fun complete() = compose.runOnIdle { state.value = state.value.copy(busy = false, network = false,
        record = record(checkNotNull(state.value.preparation), AnalysisStatus.RESULT_SAVED), preparation = null) }
    @Composable private fun Content() {
        activity = LocalContext.current as ComponentActivity
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) { RenkinTheme(dark) {
            ProjectShell(home, operation, ingress,
                onOpen = { id -> home = home.copy(activeProfileId = id); operation = ProjectSessionState.Opened(id) },
                onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {}, onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                setupData = { _, _, _, _ -> data }, readSetupContext = { records[it]?.metadata },
                stylePanel = { id, mode, source, stamp, back -> ProjectStylePanel(id, mode, source, stamp, id == home.activeProfileId, backend, back) }
            ) { back -> ProjectEditorBackHandler(back) {}; Column { Text("classic editor", Modifier.testTag("analysis-classic-editor")) } }
        } }
    }
    private fun open(extend: Boolean = false) {
        if (extend) { compose.onNodeWithTag("project-extend").performClick(); compose.onNodeWithTag("setup-source-fixture.pack").performScrollTo().performClick(); compose.onNodeWithTag("setup-profile-1").performScrollTo().performClick() }
        else { compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1")); compose.onNodeWithTag("project-setup-1").performClick() }
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
    }
    private fun scroll(tag: String) = compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag(tag))
    private fun click(tag: String) { scroll(tag); compose.onNodeWithTag(tag).performClick() }
    private fun advanced() = click("project-style-advanced")
    private fun reference() { click("project-style-add-reference"); compose.runOnIdle { registry.dispatchResult(launched, android.app.Activity.RESULT_OK, Intent().setData(Uri.parse("content://fixture/reference.png"))) } }
    private fun prepareOwn() { advanced(); reference(); click("style-analysis-prepare") }
    private fun confirm() { click("style-analysis-start"); compose.onNodeWithTag("style-analysis-confirm-paid").performClick() }
    private fun back() = compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }

    @Test fun createNeedsNoPackAndManualSaveWithoutKeyStillWorks() {
        state.value = state.value.copy(hasKey = false); compose.setContent { Content() }; open(); advanced()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("manual outline")
        reference(); click("style-analysis-prepare"); scroll("style-analysis-start"); compose.onNodeWithTag("style-analysis-start").assertIsNotEnabled()
        click("project-style-save"); assertEquals("USER", records[1]!!.revision!!.origin); assertEquals(0, posts)
    }
    @Test fun emptyOwnReferencesShowFallbackAndDoNotSend() {
        compose.setContent { Content() }; open(); click("style-analysis-prepare")
        scroll("style-analysis-error"); compose.onNodeWithTag("style-analysis-error").assertIsDisplayed()
        scroll("style-analysis-start"); compose.onNodeWithTag("style-analysis-start").assertIsNotEnabled(); assertEquals(0, posts)
    }
    @Test fun realConsentDeclineThenConfirmSubmitsOnceAndResultNeverAutosaves() {
        compose.setContent { Content() }; open(); prepareOwn(); click("style-analysis-start")
        compose.onNodeWithTag("style-analysis-decline").performClick(); assertEquals(0, posts)
        confirm(); assertEquals(1, posts); compose.onNodeWithTag("style-analysis-confirm-paid").assertDoesNotExist()
        scroll("style-analysis-start"); compose.onNodeWithTag("style-analysis-start").assertIsNotEnabled()
        complete(); scroll("style-analysis-suggestion"); compose.onNodeWithTag("style-analysis-suggestion").assertTextEquals(analysisFixtureStyle().shortStyleDescription)
        assertEquals(0, saves); assertNull(records[1]); assertEquals(1, posts)
    }
    @Test fun dirtyDraftKeepThenUsePreservesInstructionAndSaveIsSeparate() {
        compose.setContent { Content() }; open(); prepareOwn()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("unsaved draft")
        scroll("project-style-instruction"); compose.onNodeWithTag("project-style-instruction").performTextInput("keep mint")
        click("style-analysis-prepare"); confirm(); complete(); click("style-analysis-use")
        compose.onNodeWithTag("style-analysis-keep-draft").performClick(); assertEquals(0, uses)
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("unsaved draft")
        click("style-analysis-use"); compose.onNodeWithTag("style-analysis-confirm-use").performClick(); assertEquals(1, uses); assertEquals(0, saves)
        scroll("project-style-instruction"); compose.onNodeWithTag("project-style-instruction").assertTextContains("keep mint")
        click("project-style-save"); assertEquals(1, saves); assertEquals("keep mint", records[1]!!.revision!!.userInstruction); assertEquals("AUTO_ANALYSIS", records[1]!!.revision!!.origin)
    }
    @Test fun changedDraftRequiresFreshPreparationBeforePaidConfirmation() {
        compose.setContent { Content() }; open(); prepareOwn()
        scroll("project-style-instruction"); compose.onNodeWithTag("project-style-instruction").performTextInput("new instruction")
        scroll("style-analysis-start"); compose.onNodeWithTag("style-analysis-start").assertIsNotEnabled(); assertEquals(0, posts)
        click("style-analysis-prepare"); confirm(); assertEquals(1, posts)
    }
    @Test fun extendShowsTwelveLocalSamplesAndAdvancedLimitsAndOrdersFiveGenerationRefs() {
        compose.setContent { Content() }; open(extend = true); click("style-analysis-prepare")
        scroll("style-analysis-sample-count"); compose.onNodeWithTag("style-analysis-sample-count").assertTextContains("12 samples selected")
        assertEquals(0, posts); confirm(); complete(); click("style-analysis-advanced")
        scroll("style-analysis-reference-5"); compose.onNodeWithTag("style-analysis-reference-5").assertIsOff().performClick().assertIsOff()
        click("style-analysis-first-4"); click("style-analysis-use"); compose.onNodeWithTag("style-analysis-confirm-use").performClick()
        click("project-style-save"); assertEquals(5, records[1]!!.references.size)
        assertEquals(state.value.record!!.hashes[4], records[1]!!.references.first().hash); assertEquals(1, posts)
    }
    @Test fun restoredResultAndRecompositionKeepCandidateWithoutAnotherPostOrSave() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }; open(); prepareOwn(); confirm(); complete()
        restoration.emulateSavedInstanceStateRestore(); compose.runOnIdle { dark = true }
        scroll("style-analysis-suggestion"); compose.onNodeWithTag("style-analysis-suggestion").assertIsDisplayed()
        assertEquals(1, posts); assertEquals(0, saves); assertEquals(1, preparations)
    }
    @Test fun cancellationUncertaintyAndBackKeepManualStyleAndNeverResubmit() {
        compose.setContent { Content() }; open(); prepareOwn(); confirm(); click("style-analysis-cancel")
        scroll("style-analysis-uncertain"); compose.onNodeWithTag("style-analysis-uncertain").assertIsDisplayed()
        back(); compose.onNodeWithTag("project-style-keep").performClick(); assertEquals(1, posts); assertEquals(0, saves)
        back(); compose.onNodeWithTag("project-style-discard").performClick(); compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        assertEquals(1, posts); assertEquals(0, saves)
    }
    @Test fun sourceAndPreparationStalenessAreVisibleAndOnlySourceCanBeReviewed() {
        compose.setContent { Content() }; open(extend = true); click("style-analysis-prepare"); confirm(); complete()
        compose.runOnIdle { state.value = state.value.copy(validity = AnalysisValidity.PREPARATION_CHANGED) }
        scroll("style-analysis-use"); compose.onNodeWithTag("style-analysis-use").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(validity = AnalysisValidity.SOURCE_STALE) }
        click("style-analysis-use"); compose.onNodeWithTag("style-analysis-keep-draft").performClick(); assertEquals(0, uses); assertEquals(0, saves)
    }
    @Test fun readAndSaveFailuresRemainVisibleWithManualDraftKept() {
        failRefresh = true; compose.setContent { Content() }; open(); advanced()
        scroll("style-analysis-error"); compose.onNodeWithTag("style-analysis-error").assertIsDisplayed()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("keep this draft")
        failSave = true; click("project-style-save"); scroll("project-style-error"); compose.onNodeWithTag("project-style-error").assertIsDisplayed()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("keep this draft")
        assertEquals(0, saves); assertEquals(0, posts)
    }
    @Test @Config(qualifiers = "w840dp-h1000dp")
    fun wideStyleDarkLightRestorationRetainsAccessibleFortyEightDpActionsWithoutRequests() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }; open()
        scroll("style-analysis-prepare"); compose.onNodeWithTag("style-analysis-prepare").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        compose.runOnIdle { dark = true }; restoration.emulateSavedInstanceStateRestore()
        scroll("project-style-advanced"); compose.onNodeWithTag("project-style-advanced").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        assertEquals(0, posts); assertEquals(0, preparations); assertEquals(0, saves)
    }
}
