package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Actual routed UI, VM-owned controllers, sampler, durable journals and real Room.
 * Only launcher/source artwork and the paid provider ports are offline fixtures. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectStyleTrialJourneyComposeTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var f: StyleTrialFixture
    private lateinit var scope: CoroutineScope
    private lateinit var trials: OwnedStyleTrials
    private lateinit var analysisJournal: StyleAnalysisJournal
    private lateinit var backend: ProjectStyleBackend
    private lateinit var activity: ComponentActivity
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Project"), Profile(2, "Other")),
        availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var metadata by mutableStateOf<ProjectAiMetadata?>(null)
    private var dark by mutableStateOf(false)
    private var key = true
    private var imageCalls = 0
    private val mutex = Mutex()
    // A diverse source pool, rather than assuming twelve shapes are all perceptually unique.
    private var reader = FixtureReferenceReader((0 until 30).associate { "icon$it" to analysisFixturePng(it % 3, it) })
    private suspend fun <T> owned(id: Long, action: suspend () -> T): T = mutex.withLock {
        check(home.activeProfileId == id && home.actionsEnabled); action()
    }
    @Before fun setup() = runBlocking {
        f = StyleTrialFixture(temporary.root)
        RenkinPackRepository(f.db).replaceEverything(home.profiles, emptyList())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        analysisJournal = StyleAnalysisJournal(f.context)
        trials = OwnedStyleTrials(scope, f.repo, f.journal, StyleTrialCoordinator(f.journal, f.successful { imageCalls++ }, f.repo),
            { key }, object : TrialSessionOwner {
                override suspend fun <T> owned(id: Long, action: suspend () -> T) = this@ProjectStyleTrialJourneyComposeTest.owned(id, action)
                override suspend fun targets() = f.targets
                override suspend fun reader(source: String): CoverageSourceReader = reader
                override suspend fun original(key: String) = f.originals.getValue(key)
                override suspend fun settings() = AiSettings()
            })
        backend = object : ProjectStyleBackend {
            override suspend fun load(id: Long) = owned(id) { f.repo.load(id) }
            override suspend fun importReference(id: Long, uri: android.net.Uri) = error("No picker is used by this journey")
            override suspend fun referencePreview(id: Long, hash: String) = f.repo.referenceBytes(id, hash)
            override suspend fun copyGlobal(id: Long) = error("No global copy is used by this journey")
            override suspend fun discard(id: Long, hashes: List<String>) { owned(id) { f.repo.discardDraftReferences(id, hashes) } }
            override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft,
                mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle {
                val saved = owned(id) { f.repo.saveStyle(id, expected, draft.specification, draft.instruction, draft.hashes, mode.name, source, stamp) }
                metadata = saved.metadata; return saved
            }
        }
    }
    @After fun close() = runBlocking { scope.cancel(); scope.coroutineContext[Job]?.join(); f.close() }
    @Composable private fun Content() {
        activity = LocalContext.current as ComponentActivity
        RenkinTheme(dark) {
            ProjectShell(home, operation, 0,
                onOpen = { home = home.copy(activeProfileId = it); operation = ProjectSessionState.Opened(it) },
                onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {},
                onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                readSetupContext = { f.repo.metadata(it) }, bindTrialContext = trials::bindContext,
                setupData = { _, _, id, _ ->
                    val profileId = id ?: home.activeProfileId
                    ProjectSetupData(targets = f.targets, savedMetadata = metadata,
                        packs = listOf(IconPack("fixture.pack", "Fixture", 7, "7", 0)),
                        coverage = CoverageUiState.Ready(CoverageSnapshot(profileId, reader.index, f.targets.map {
                            CoverageEntry(it, SourceCoverage.UNMAPPED, null, CoverageReason.NO_MAPPING)
                        })), onSaveSelection = { version, selection -> owned(profileId) {
                            verifyProjectSelectionContext(selection, f.targets) { _, _ -> reader }
                            f.repo.saveSelection(profileId, version, selection).also { metadata = it }
                        } })
                }, stylePanel = { id, mode, source, stamp, back, trial ->
                    ProjectStylePanel(id, mode, source, stamp, home.actionsEnabled && id == home.activeProfileId, backend, back, trial)
                }, trialPanel = { id, mode, source, back, refine, editor ->
                    ProjectTrialPanel(id, "Project", mode, source, home.actionsEnabled && id == home.activeProfileId,
                        trials, back, refine, editor)
                }) { Text("Classic editor") }
        }
    }
    private fun open(extend: Boolean = false) {
        if (extend) {
            compose.onNodeWithTag("project-extend").performClick()
            compose.onNodeWithTag("setup-source-fixture.pack").performScrollTo().performClick()
            compose.onNodeWithTag("setup-profile-1").performScrollTo().performClick()
        } else {
            compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1"))
            compose.onNodeWithTag("project-setup-1").performClick()
        }
        compose.onNodeWithTag("setup-project-style").assertExists()
    }
    private fun settle() {
        compose.waitForIdle()
        compose.waitUntil(10000) { !trials.state(1).value.busy }
        compose.waitForIdle()
    }
    private fun click(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertIsEnabled().performClick(); settle()
    }
    private fun saveTargets() {
        click("project-targets", "setup-select-all"); click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("setup-selection-saved", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("setup-selection-saved", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }
    private fun style() { click("project-targets", "setup-project-style") }
    private fun manualSave() {
        // Backend load completes independently of the frame clock. The removed
        // advanced-mode click previously happened to let that read settle.
        compose.waitUntil(10000) {
            compose.onAllNodes(hasTestTag("project-style-description") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.onNodeWithTag("project-style-description").performTextInput("manual outline")
        click("project-style-1", "project-style-save")
        awaitSavedStyle()
    }
    private fun awaitSavedStyle() {
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-status"))
        compose.waitUntil(10000) {
            compose.onAllNodes(hasTestTag("project-style-status") and hasText("Project style saved"))
                .fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("project-style-status").assertTextEquals("Project style saved")
    }
    private fun back() { compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }; settle() }
    private fun enterTrials() { click("project-targets", "setup-trials"); assertNull(trials.state(1).value.error) }
    @Test fun freshTargetsExplainMissingStyleAndDisableTrialsBeforeAnyCloudOperation() {
        compose.setContent { Content() }; open()
        compose.onNodeWithTag("setup-trials").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("setup-trials-reason").assertTextContains("Save a project style", substring = true)
        assertNull(analysisJournal.latest(1)); assertEquals(0, imageCalls)
    }
    @Test fun manualTargetsFirstSaveStyleShowsTrialEntryAboveAnalysisAndKeepsSavedSelection() {
        key = false; compose.setContent { Content() }; open(); saveTargets(); style(); manualSave()
        compose.onNodeWithTag("project-style-1").performScrollToIndex(0)
        compose.onNodeWithTag("style-trials").assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp).performClick(); settle()
        assertNull(trials.state(1).value.error)
        compose.onNodeWithTag("trial-no-key").assertExists()
        assertEquals(3, trials.state(1).value.preparation!!.candidates.size)
        assertNull(analysisJournal.latest(1)); assertEquals(0, imageCalls)
        assertEquals(0, runBlocking { f.repo.load(1).references.size })
        val saved = runBlocking { f.repo.load(1) }.metadata!!
        assertNull(saved.sourceStamp)
        assertEquals(f.targets.map { it.key }.sorted(), ProjectSelection.decode(saved.selectionJson)!!.componentKeys)
    }
    @Test fun manualStyleFirstThenTargetsReachesOneAndThreeFrozenAppsWithSeparatePaidConsent() {
        compose.setContent { Content() }; open(); style(); manualSave(); back(); saveTargets(); enterTrials()
        click("project-trials-1", "trial-prepare")
        assertEquals(3, trials.state(1).value.plan!!.inputs.size); assertEquals(0, imageCalls)
        compose.onNodeWithTag("trial-decline").performClick(); settle()
        click("project-trials-1", "trial-target-c/Main")
        click("project-trials-1", "trial-prepare")
        assertEquals(2, trials.state(1).value.plan!!.inputs.size); assertEquals(0, imageCalls)
        compose.onNodeWithTag("trial-decline").performClick(); settle()
        click("project-trials-1", "trial-target-b/Main")
        click("project-trials-1", "trial-prepare")
        assertEquals(1, trials.state(1).value.plan!!.inputs.size); assertEquals(0, imageCalls)
        compose.onNodeWithTag("trial-confirm-paid").performClick(); settle()
        val row = trials.state(1).value.variants.single()
        // The finished request starts a separate local context recheck. Wait for
        // this current review proof before materializing the retained Accept action.
        compose.waitUntil(10000) {
            val state = trials.state(1).value
            !state.busy && trialReviewSummary(state).unreviewed.any { it.id == row.id }
        }
        click("project-trials-1", "trial-accept-${row.id}")
        assertFalse(trials.state(1).value.ready)
        click("project-trials-1", "trial-approve-style")
        assertTrue(trials.state(1).value.ready); assertEquals(1, imageCalls); assertNull(analysisJournal.latest(1))
        assertEquals("NOT_APPLIED", runBlocking { f.repo.trialVariants(1).single().applyStatus })
    }
    @Test fun savedStyleWithoutTargetsExplainsExactlyWhatToSaveOnTheRoutedTrialScreen() {
        compose.setContent { Content() }; open(); style(); manualSave()
        click("project-style-1", "style-trials")
        compose.onNodeWithTag("trial-error").assertTextContains("Save selected targets", substring = true)
        compose.onNodeWithTag("trial-targets").assertIsEnabled().performClick(); settle()
        compose.onNodeWithTag("setup-save-selection").assertExists()
        assertEquals(0, imageCalls); assertNull(analysisJournal.latest(1))
    }
    @Test fun extendManualStyleWorksWithoutCloudAnalysisOrUploadedReferences() {
        dark = true; compose.setContent { Content() }; open(extend = true); style(); manualSave()
        back(); saveTargets(); enterTrials()
        assertEquals("EXTEND", trials.state(1).value.preparation!!.context.mode)
        assertEquals(0, trials.state(1).value.preparation!!.style.references.size)
        click("project-trials-1", "trial-prepare")
        assertEquals(3, trials.state(1).value.plan!!.inputs.size)
        compose.onNodeWithTag("trial-decline").performClick(); settle()
        assertNull(analysisJournal.latest(1)); assertEquals(0, imageCalls)
    }
    @Test fun dirtyStyleHasAccessibleSaveReasonBeforeTrialNavigation() {
        compose.setContent { Content() }; open(); saveTargets(); style(); manualSave()
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.onNodeWithTag("project-style-description").performTextInput(" refined")
        compose.onNodeWithTag("project-style-1").performScrollToIndex(0)
        compose.onNodeWithTag("style-trials").assertIsNotEnabled()
        compose.onNodeWithTag("style-trials-reason").assertTextContains("Save your style changes", substring = true)
        assertNull(analysisJournal.latest(1)); assertEquals(0, imageCalls)
    }
    @Test fun changedSourceOnReloadOffersTargetsRemediationWithoutAnyGeneration() {
        compose.setContent { Content() }; open(extend = true); saveTargets(); style(); manualSave(); back(); enterTrials()
        reader = FixtureReferenceReader(mapOf("replacement" to analysisFixturePng(2, 91)))
        click("project-trials-1", "trial-options-toggle")
        click("project-trials-1", "trial-reload")
        assertEquals("stale_targets", trials.state(1).value.error)
        compose.onNodeWithTag("trial-error").assertTextContains("saved targets or source changed", substring = true)
        compose.onNodeWithTag("trial-targets").assertIsEnabled().performClick(); settle()
        assertNull(trials.state(1).value.plan); assertEquals(0, imageCalls); assertNull(analysisJournal.latest(1))
    }
}
