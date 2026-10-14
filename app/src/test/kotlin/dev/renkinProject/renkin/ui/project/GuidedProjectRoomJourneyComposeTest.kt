package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.ai.AiSettings
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

/** Production guided routes, real Room/asset/journal/controller writes, synthetic provider only. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuidedProjectRoomJourneyComposeTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var fixture: StyleTrialFixture
    private lateinit var scope: CoroutineScope
    private lateinit var trials: OwnedStyleTrials
    private lateinit var style: ProjectStyleBackend
    private val mutex = Mutex()
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Project")),
        availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var metadata by mutableStateOf<ProjectAiMetadata?>(null)
    private var providerCalls = 0
    private var selectionSaves = 0
    private var styleSaves = 0
    private var saveGate: CompletableDeferred<Unit>? = null
    private var selectionStarted = false
    private suspend fun <T> owned(id: Long, action: suspend () -> T) = mutex.withLock {
        check(id == home.activeProfileId && home.actionsEnabled)
        action()
    }
    @Before fun setup() = runBlocking {
        fixture = StyleTrialFixture(temporary.root)
        fixture.targets = listOf(CoverageTarget("app/Main", "Recognizable application"))
        fixture.originals.clear()
        fixture.originals["app/Main"] = TrialOriginal(projectTestPng(size = 1024), "launcher:app/Main:1")
        RenkinPackRepository(fixture.db).replaceEverything(home.profiles, emptyList())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        trials = OwnedStyleTrials(scope, fixture.repo, fixture.journal,
            StyleTrialCoordinator(fixture.journal, fixture.successful { providerCalls++ }, fixture.repo),
            { true }, object : TrialSessionOwner {
                override suspend fun <T> owned(id: Long, action: suspend () -> T) =
                    this@GuidedProjectRoomJourneyComposeTest.owned(id, action)
                override suspend fun targets() = fixture.targets
                override suspend fun reader(source: String) = fixture.reader
                override suspend fun original(key: String) = fixture.originals.getValue(key)
                override suspend fun settings() = AiSettings()
            })
        style = object : ProjectStyleBackend {
            override suspend fun load(id: Long) = owned(id) { fixture.repo.load(id) }
            override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset = error("No picker in this test")
            override suspend fun copyGlobal(id: Long) = error("No global settings copied")
            override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode,
                source: String?, stamp: String?): SavedProjectStyle = owned(id) {
                fixture.repo.saveStyle(id, expected, draft.specification, draft.instruction, draft.hashes,
                    mode.name, source, stamp).also { metadata = it.metadata; styleSaves++ }
            }
        }
    }
    @After fun close() = runBlocking {
        scope.cancel()
        scope.coroutineContext[Job]?.join()
        fixture.close()
    }
    @Composable private fun Content() {
        RenkinTheme(false) {
            ProjectShell(home, operation, onOpen = {
                home = home.copy(activeProfileId = it); operation = ProjectSessionState.Opened(it)
            }, onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {},
                onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                readSetupContext = { fixture.repo.metadata(it) }, bindTrialContext = trials::bindContext,
                setupData = { _, _, id, _ ->
                    val profile = id ?: 1L
                    ProjectSetupData(targets = fixture.targets,
                        packs = listOf(IconPack("source.pack", "Lux-like fixture", 1, "1", 0)),
                        savedMetadata = metadata,
                        coverage = CoverageUiState.Ready(CoverageSnapshot(profile, fixture.index, fixture.targets.map {
                            CoverageEntry(it, SourceCoverage.UNMAPPED, null, CoverageReason.NO_MAPPING)
                        })), onSaveSelection = { version, selection ->
                            selectionStarted = true
                            saveGate?.await()
                            owned(profile) {
                                verifyProjectSelectionContext(selection, fixture.targets) { _, _ -> fixture.reader }
                                fixture.repo.saveSelection(profile, version, selection).also {
                                    metadata = it; selectionSaves++
                                }
                            }
                        })
                },
                stylePanel = { id, mode, source, stamp, back, next ->
                    ProjectStylePanel(id, mode, source, stamp, true, style, back, next,
                        onStyleSaved = { next() }, guided = true)
                },
                guidedWorkflow = true,
                guidedTrialPanel = { id, mode, source, back, refine, editor, complete ->
                    ProjectTrialPanel(id, "Project", mode, source, true, trials, back, refine, editor,
                        onComplete = complete, appName = { "Recognizable application" }, guided = true)
                },
                completionPanel = { id, mode, source, back, refine, targets, editor, homeRoute ->
                    ProjectCompletionPanel(id, "Project", mode, source, "Lux-like fixture", true, style,
                        trials, back, refine, targets, editor, homeRoute)
                }) { Text("Classic editor") }
        }
    }
    private fun click(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        val node = compose.onNodeWithTag(tag)
        compose.waitUntil(10000) { node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Disabled) == null }
        node.assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitForIdle()
    }
    private fun settled() {
        compose.waitUntil(10000) { !trials.state(1).value.busy }
        compose.waitForIdle()
    }
    private fun journey(extend: Boolean) {
        compose.setContent { Content() }
        if (extend) {
            click("project-home", "project-extend")
            click("setup-source", "setup-source-source.pack")
            click("setup-profile", "setup-profile-1")
        } else click("project-home", "project-setup-1")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-project-style").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("workflow-step").assertTextContains("1 of 4", substring = true)
        assertEquals(0, providerCalls)
        click("project-targets", "setup-project-style")
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.onNodeWithTag("project-style-description").performTextInput("manual centered outline")
        click("project-style-1", "project-style-save")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-target-app/Main").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        assertEquals(1L, runBlocking { fixture.repo.load(1).revision!!.revision })
        assertEquals(1, styleSaves); assertEquals(0, providerCalls)
        click("project-targets", "setup-target-app/Main")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) { trials.state(1).value.preparation != null && !trials.state(1).value.busy }
        assertEquals(listOf("app/Main"), runBlocking {
            ProjectSelection.decode(fixture.repo.metadata(1)!!.selectionJson)!!.componentKeys
        })
        assertEquals(1, selectionSaves); assertEquals(0, providerCalls)
        click("project-trials-1", "trial-prepare")
        compose.waitUntil(10000) { trials.state(1).value.plan != null && !trials.state(1).value.busy }
        compose.onNodeWithTag("trial-confirm-paid").assertIsEnabled().performClick()
        compose.waitUntil(10000) { trials.state(1).value.variants.isNotEmpty() && !trials.state(1).value.busy }
        settled()
        val variant = trials.state(1).value.variants.single()
        click("project-trials-1", "trial-accept-" + variant.id)
        settled()
        assertFalse(trials.state(1).value.ready)
        click("project-trials-1", "trial-approve-style")
        compose.waitUntil(10000) { trials.state(1).value.ready && !trials.state(1).value.busy }
        click("project-trials-1", "trial-complete")
        compose.waitUntil(10000) { trials.state(1).value.ready && !trials.state(1).value.busy }
        compose.onNodeWithTag("workflow-step").assertTextContains("4 of 4", substring = true)
        compose.onNodeWithTag("completion-approval").assertTextEquals("Trial set approved")
        assertEquals(1, providerCalls)
        assertEquals("NOT_APPLIED", runBlocking { fixture.repo.trialVariants(1).single().applyStatus })
        click("project-completion-1", "completion-home")
        click("project-home", "project-setup-1")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("workflow-source-style-saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("workflow-source-style-saved").assertTextContains("revision 1", substring = true)
        click("project-targets", "setup-project-style")
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-target-app/Main"))
        compose.onNodeWithTag("setup-target-app/Main").assertIsOn()
        click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) { trials.state(1).value.preparation != null && !trials.state(1).value.busy }
        assertEquals(1, selectionSaves)
        assertEquals(1, styleSaves)
        assertEquals(1, providerCalls)
        assertEquals(1, runBlocking { fixture.journal.records(1).size })
    }
    @Test fun createManualSaveTrialReviewCompletionAndReopenUseRealPersistence() = journey(false)
    @Test fun extendManualSaveTrialReviewCompletionAndReopenUseRealPersistence() = journey(true)
    @Test fun decliningLocalPreparationRestoresTheUnchangedDurableTrialApprovalWithoutRebilling() {
        journey(false)
        settled()
        assertTrue(trialReviewSummary(trials.state(1).value).ready)
        val approval = runBlocking { fixture.db.projectAiDao().approval(1, 1)!! }
        val variants = runBlocking { fixture.repo.trialVariants(1) }
        val records = runBlocking { fixture.journal.records(1) }
        assertEquals(1, providerCalls)
        click("project-trials-1", "trial-prepare")
        compose.waitUntil(10000) { trials.state(1).value.plan != null && !trials.state(1).value.busy }
        assertFalse(trials.state(1).value.ready)
        compose.onNodeWithTag("trial-decline").assertIsEnabled().performClick()
        compose.waitUntil(10000) { trialReviewSummary(trials.state(1).value).ready }
        compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-ready"))
        compose.onNodeWithTag("trial-ready").assertExists()
        assertNull(trials.state(1).value.plan)
        assertEquals(approval, runBlocking { fixture.db.projectAiDao().approval(1, 1) })
        assertEquals(variants, runBlocking { fixture.repo.trialVariants(1) })
        assertEquals(records, runBlocking { fixture.journal.records(1) })
        assertEquals(1, providerCalls)
    }
    @Test fun targetSaveDoesNotNavigateOrClaimSavedBeforeRoomWriteCompletes() {
        saveGate = CompletableDeferred()
        compose.setContent { Content() }
        click("project-home", "project-setup-1")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-project-style").fetchSemanticsNodes().isNotEmpty() }
        click("project-targets", "setup-project-style")
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.onNodeWithTag("project-style-description").performTextInput("manual centered outline")
        click("project-style-1", "project-style-save")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-target-app/Main").fetchSemanticsNodes().isNotEmpty() }
        click("project-targets", "setup-target-app/Main")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) { selectionStarted }
        compose.onNodeWithTag("setup-save-selection").assertIsNotEnabled()
        assertNull(runBlocking { ProjectSelection.decode(fixture.repo.metadata(1)!!.selectionJson) })
        assertNull(trials.state(1).value.preparation)
        assertEquals(0, providerCalls)
        compose.runOnIdle { saveGate!!.complete(Unit) }
        compose.waitUntil(10000) { trials.state(1).value.preparation != null && !trials.state(1).value.busy }
        assertEquals(1, selectionSaves)
        assertEquals(listOf("app/Main"), runBlocking {
            ProjectSelection.decode(fixture.repo.metadata(1)!!.selectionJson)!!.componentKeys
        })
    }
}
