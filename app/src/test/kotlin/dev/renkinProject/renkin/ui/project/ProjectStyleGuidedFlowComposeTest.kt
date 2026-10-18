package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Offline tap-to-persistence regressions for the unified style step. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectStyleGuidedFlowComposeTest {
    @get:Rule val compose = createComposeRule()
    private val flow = MutableStateFlow(ProjectAnalysisState(hasKey = false))
    private var saved = SavedProjectStyle(null, null, emptyList())
    private var dnaSaves = 0
    private var manualSaves = 0
    private var submissions = 0
    private var completions = 0
    private var saveGate: CompletableDeferred<Unit>? = null
    private var lastPreparedMode: DnaAnalysisMode? = null
    private var refreshes = 0
    private var failSave = false
    private val analysis = object : ProjectAnalysisBackend {
        override val supportsDna = true
        override fun state(id: Long) = flow
        override fun prepare(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?, draft: ProjectStyleDraft) {}
        override fun submit(id: Long, requestId: String) { submissions++ }
        override fun cancel(id: Long) {}
        override suspend fun refresh(id: Long) { refreshes++ }
        override suspend fun preview(id: Long, requestId: String, hash: String): ByteArray = error("No legacy artwork")
        override suspend fun use(id: Long, requestId: String, hashes: List<String>, allowStaleSource: Boolean): AnalyzedDraft =
            AnalyzedDraft(PromptComposer.specification(checkNotNull(flow.value.record?.style)), hashes, requestId, 1234, null)
        override suspend fun discardCandidate(id: Long, requestId: String) {}
        override fun prepareDna(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?,
            draft: ProjectStyleDraft, analysisMode: DnaAnalysisMode, model: String, detail: String) { lastPreparedMode = analysisMode }
        override fun submitDna(id: Long, planId: String) { submissions++ }
        override suspend fun dnaPreview(id: Long, hash: String): ByteArray = error("No preview needed")
        override suspend fun saveDna(id: Long, planId: String?, grammar: String, selectedFamily: String, hashes: List<String>,
            expected: Long?, draft: ProjectStyleDraft): SavedProjectStyle {
            dnaSaves++
            saveGate?.await()
            check(!failSave)
            val record = checkNotNull(flow.value.dnaRecord)
            val dna = checkNotNull(record.result).copy(dna = StyleDnaV1.parse(grammar), selectedFamily = selectedFamily)
            val revision = (expected ?: 0) + 1
            saved = SavedProjectStyle(ProjectAiMetadata(id, activeRevision = revision),
                StyleRevision(id, revision, specification = StyleDnaCompiler.specification(dna), userInstruction = draft.instruction), emptyList())
            flow.value = flow.value.copy(savedDna = dna, dnaRecord = record.copy(status = DnaStatus.REVIEWED))
            return saved
        }
    }
    private val backend = object : ProjectStyleBackend {
        override val analysis: ProjectAnalysisBackend = this@ProjectStyleGuidedFlowComposeTest.analysis
        override suspend fun load(id: Long) = saved
        override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset = error("No import")
        override suspend fun copyGlobal(id: Long) = ProjectStyleDraft("manual", "", emptyList())
        override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode,
            source: String?, stamp: String?): SavedProjectStyle {
            manualSaves++; saveGate?.await(); check(!failSave)
            val revision = (expected ?: 0) + 1
            return SavedProjectStyle(ProjectAiMetadata(id, mode.name, source, stamp, revision),
                StyleRevision(id, revision, specification = draft.specification, userInstruction = draft.instruction),
                draft.hashes.map { ReferenceAsset(id, it, ProjectAssetStore.path(id, it), 100, 256, 256) }).also { saved = it }
        }
    }
    private fun show() = compose.setContent {
        RenkinTheme { ProjectStylePanel(1, ProjectSetupMode.CREATE, null, null, true, backend, {}, onTrials = {},
            onStyleSaved = { completions++ }, guided = true) }
    }
    private fun scroll(tag: String) = compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag(tag))
    private fun choose(method: String = "QUICK") {
        scroll("project-style-analysis-options"); compose.onNodeWithTag("project-style-analysis-options").performClick()
        scroll("project-style-method-$method"); compose.onNodeWithTag("project-style-method-$method").assertIsEnabled().performClick()
    }
    private fun candidate() {
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        val reports = dnaFixtureReports(plan)
        val dna = synthesizeDnaLocally(reports, images)
        flow.value = flow.value.copy(dnaRecord = StyleDnaRecord(plan, DnaStatus.RESULT_SAVED,
            plan.steps.map { DnaStepState(it.id, "DONE", "offline-fixture") }, reports,
            SavedStyleDna(dna, reports, plan.model, plan.detail, plan.algorithm, dna.recommendedFamily)))
    }

    @Test fun freshProjectDefaultsToFreeReferencesAndAnalysisRequiresExplicitMethodSelection() {
        show()
        scroll("project-style-description")
        compose.onNodeWithTag("project-style-description").assertTextContains("reference icons", substring = true)
        compose.onNodeWithTag("style-dna-prepare").assertDoesNotExist()
        compose.onNodeWithTag("style-analysis-prepare").assertDoesNotExist()
        assertEquals(0, refreshes)
        scroll("project-style-analysis-options")
        compose.onNodeWithTag("project-style-analysis-options").assertIsEnabled().performClick()
        scroll("project-style-method-QUICK"); compose.onNodeWithTag("project-style-method-QUICK").performClick()
        scroll("style-dna-prepare"); compose.onNodeWithTag("style-dna-prepare").assertIsEnabled().performClick()
        assertEquals(DnaAnalysisMode.QUICK, lastPreparedMode)
        scroll("project-style-reference-mode"); compose.onNodeWithTag("project-style-reference-mode").performClick()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextReplacement("Owner edited brief")
        scroll("project-style-analysis-options"); compose.onNodeWithTag("project-style-analysis-options").assertIsNotEnabled()
        candidate() // Delayed historical results must not replace a manual draft.
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("Owner edited brief")
        assertEquals(0, submissions)
        assertEquals(0, dnaSaves)
        assertEquals(0, manualSaves)
    }

    @Test fun immutableDnaSaveFinishesStyleWithoutASecondManualSave() {
        candidate(); show(); choose()
        scroll("style-dna-reviewed"); compose.onNodeWithTag("style-dna-reviewed").performClick()
        scroll("style-dna-save"); compose.onNodeWithTag("style-dna-save").assertIsEnabled().performClick()
        compose.waitUntil(10000) { saved.revision != null }
        compose.onNodeWithTag("project-style-save").assertDoesNotExist()
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsEnabled()
        assertEquals(1, dnaSaves); assertEquals(0, manualSaves); assertEquals(0, submissions)
        assertEquals(1, completions)
    }

    @Test fun pendingDnaSaveCannotClaimSavedOrEnableNextStep() {
        saveGate = CompletableDeferred(); candidate(); show(); choose()
        scroll("style-dna-reviewed"); compose.onNodeWithTag("style-dna-reviewed").performClick()
        scroll("style-dna-save"); compose.onNodeWithTag("style-dna-save").performClick()
        compose.waitUntil(10000) { dnaSaves == 1 }
        assertNull(saved.revision)
        assertEquals(0, completions)
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsNotEnabled()
        compose.runOnIdle { saveGate!!.complete(Unit) }
        compose.waitUntil(10000) { saved.revision != null }
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsEnabled()
        assertEquals(0, submissions)
        assertEquals(1, completions)
    }

    @Test fun resavingDnaBlocksParentActionsUntilTheNewRevisionIsDurable() {
        saved = SavedProjectStyle(ProjectAiMetadata(1, activeRevision = 1),
            StyleRevision(1, 1, specification = "previous saved style", userInstruction = ""), emptyList())
        saveGate = CompletableDeferred(); candidate(); show(); choose()
        scroll("style-dna-reviewed"); compose.onNodeWithTag("style-dna-reviewed").performClick()
        scroll("style-dna-save"); compose.onNodeWithTag("style-dna-save").performClick()
        compose.waitUntil(10000) { dnaSaves == 1 }
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsNotEnabled()
        compose.runOnIdle { saveGate!!.complete(Unit) }
        compose.waitUntil(10000) { saved.revision?.specification != "previous saved style" }
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsEnabled()
        assertEquals(0, manualSaves); assertEquals(0, submissions)
    }

    @Test fun delayedReloadOfTheSameDnaCandidateKeepsItsEditedGrammar() {
        candidate(); show(); choose()
        val record = checkNotNull(flow.value.dnaRecord)
        val original = checkNotNull(record.result).dna.encode()
        val rule = record.result!!.dna.families.first().drawing.values.first().value
        val edited = original.replaceFirst(rule, "Owner edited drawing rule")
        scroll("style-dna-edit"); compose.onNodeWithTag("style-dna-edit").performClick()
        scroll("style-dna-grammar"); compose.onNodeWithTag("style-dna-grammar").performTextReplacement(edited)
        // A recreated controller may expose its empty initial flow before journal loading finishes.
        compose.runOnIdle { flow.value = flow.value.copy(dnaRecord = null) }
        compose.runOnIdle { flow.value = flow.value.copy(dnaRecord = record) }
        scroll("style-dna-grammar"); compose.onNodeWithTag("style-dna-grammar").assertTextContains(edited)
        assertEquals(0, dnaSaves); assertEquals(0, manualSaves); assertEquals(0, submissions)
    }

    @Test fun sourceFreeManualCreateSavesAndContinuesWithoutAKeyOrAnySubmission() {
        show()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextReplacement("simple outline")
        scroll("project-style-instruction"); compose.onNodeWithTag("project-style-instruction").performTextInput("keep original colors")
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        compose.waitUntil(10000) { completions == 1 }
        assertEquals("simple outline", saved.revision!!.specification)
        assertEquals("keep original colors", saved.revision!!.userInstruction)
        assertEquals("CREATE", saved.metadata!!.mode); assertNull(saved.metadata!!.sourcePackage)
        assertEquals(1, manualSaves); assertEquals(0, dnaSaves); assertEquals(0, submissions)
        compose.onNodeWithTag("project-style-save").assertDoesNotExist()
    }

    @Test fun guidedLegacyCandidateSavesOnceWhileCurrentFailureRemainsSeparateFromHistory() {
        val request = analysisFixtureRequest(1)
        val style = analysisFixtureStyle().encode()
        flow.value = flow.value.copy(error = "operation", record = AnalysisRecord(1, request.requestId,
            request.fingerprint, request.context, request.model, "", "", request.images.map { it.hash },
            request.promptHash, 123, AnalysisStatus.RESULT_SAVED, style, "offline-result", sha256(style.toByteArray())))
        show(); choose("LEGACY")
        scroll("style-analysis-error"); compose.onNodeWithTag("style-analysis-error").assertTextContains("earlier analysis", substring = true)
        scroll("style-analysis-suggestion"); compose.onNodeWithTag("style-analysis-suggestion").assertTextContains(analysisFixtureStyle().shortStyleDescription)
        scroll("style-analysis-use"); compose.onNodeWithTag("style-analysis-use").assertIsEnabled().performClick()
        compose.onNodeWithTag("style-analysis-confirm-use").performClick()
        compose.waitUntil(10000) { completions == 1 }
        assertEquals(PromptComposer.specification(analysisFixtureStyle()), saved.revision!!.specification)
        assertEquals(1, manualSaves); assertEquals(0, dnaSaves); assertEquals(0, submissions)
        compose.onNodeWithTag("project-style-save").assertDoesNotExist()
    }

    @Test fun failedDnaSaveKeepsCandidateAndNeverAdvancesOrClaimsPersistentSuccess() {
        failSave = true; candidate(); show(); choose()
        scroll("style-dna-reviewed"); compose.onNodeWithTag("style-dna-reviewed").performClick()
        scroll("style-dna-save"); compose.onNodeWithTag("style-dna-save").performClick()
        compose.waitUntil(10000) { dnaSaves == 1 }
        scroll("style-dna-error"); compose.onNodeWithTag("style-dna-error").assertExists()
        assertEquals(0, completions); assertNull(saved.revision); assertEquals(DnaStatus.RESULT_SAVED, flow.value.dnaRecord!!.status)
        assertEquals(0, submissions)
        failSave = false
        scroll("style-dna-save"); compose.onNodeWithTag("style-dna-save").assertIsEnabled().performClick()
        compose.waitUntil(10000) { completions == 1 }
        assertEquals(0, submissions); assertEquals(2, dnaSaves)
    }
}
