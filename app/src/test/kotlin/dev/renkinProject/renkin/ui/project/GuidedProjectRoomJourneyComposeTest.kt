package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import dev.renkinProject.renkin.ai.style.*
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
    private var coverageLoaded by mutableStateOf(true)
    private var analysis: OwnedStyleAnalysis? = null
    private var localReferences: LocalStyleReferences? = null
    @Volatile private var hasKey = true
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
            { hasKey }, object : TrialSessionOwner {
                override suspend fun <T> owned(id: Long, action: suspend () -> T) =
                    this@GuidedProjectRoomJourneyComposeTest.owned(id, action)
                override suspend fun targets() = fixture.targets
                override suspend fun reader(source: String) = fixture.reader
                override suspend fun original(key: String) = fixture.originals.getValue(key)
                override suspend fun settings() = AiSettings()
            })
        style = object : ProjectStyleBackend {
            override val analysis get() = this@GuidedProjectRoomJourneyComposeTest.analysis
            override val localReferences get() = this@GuidedProjectRoomJourneyComposeTest.localReferences
            override suspend fun load(id: Long) = owned(id) { fixture.repo.load(id) }
            override suspend fun referencePreview(id: Long, hash: String) = owned(id) { fixture.repo.referenceBytes(id, hash) }
            override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset = error("No picker in this test")
            override suspend fun copyGlobal(id: Long) = error("No global settings copied")
            override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode,
                source: String?, stamp: String?): SavedProjectStyle {
                val saved = localReferences?.save(id, expected, draft, mode, source, stamp) ?: owned(id) {
                    fixture.repo.saveStyle(id, expected, draft.specification, draft.instruction, draft.hashes,
                        mode.name, source, stamp)
                }
                return saved.also { metadata = it.metadata; styleSaves++ }
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
                        coverage = if (!coverageLoaded) CoverageUiState.Loading() else CoverageUiState.Ready(CoverageSnapshot(profile, fixture.index, fixture.targets.map {
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
        compose.waitForIdle()
        // An awaited Room load/restored layout can move the materialized control.
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        node.assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitForIdle()
    }
    private fun settled() {
        // Let request-completion effects start the local context recheck first.
        compose.waitForIdle()
        compose.waitUntil(10000) {
            trials.state(1).value.let { !it.busy && !it.network && it.preparation != null }
        }
        compose.waitForIdle()
    }
    private fun enterManualStyle() {
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        // The real independent Room load keeps the field disabled until its base is known.
        compose.waitUntil(10000) {
            compose.onAllNodes(hasTestTag("project-style-description") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("project-style-description").assertIsEnabled().performTextReplacement("manual centered outline")
    }

    @Test fun referenceFirstPackSavesWithoutAKeyAndPreparesOriginalFirstOrderedTrial() {
        hasKey = false
        val artwork = FixtureReferenceReader((0 until 12).associate { "draw$it" to analysisFixturePng(it % 3, it) })
        val references = object : CoverageSourceReader by artwork {
            override val index = artwork.index.copy(stamp = fixture.index.stamp)
        }
        localReferences = LocalStyleReferences(fixture.repo, ReferenceSampler(), object : AnalysisSessionOwner {
            override suspend fun <T> owned(id: Long, operation: suspend () -> T) =
                this@GuidedProjectRoomJourneyComposeTest.owned(id, operation)
            override suspend fun catalogStamp() = error("Free style does not read targets")
            override suspend fun reader(source: String, referenceIndex: Boolean) = if (referenceIndex) references else fixture.reader
        })
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        click("project-home", "project-extend")
        click("setup-source", "setup-source-source.pack")
        click("setup-profile", "setup-profile-1")
        click("project-targets", "setup-project-style")
        click("project-style-1", "project-style-sample-pack")
        compose.waitUntil(10000) {
            compose.onAllNodes(hasTestTag("project-style-sample-pack") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        click("project-style-1", "project-style-select-candidate-0")
        click("project-style-1", "project-style-select-candidate-1")
        val originalOrder = runBlocking { localReferences!!.sample(1, "source.pack", projectSourceStamp(fixture.index))
            .samples.samples.take(2).map { it.hash } }
        click("project-style-1", "project-style-up-1")
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.onNodeWithTag("project-style-description").performTextReplacement("Owner reviewed reference icons; keep target identity")
        restoration.emulateSavedInstanceStateRestore()
        // Candidate bitmaps intentionally are not restored. The shorter lazy list
        // remeasures while the independent Room read enables its real Save control.
        compose.waitUntil(10000) {
            try {
                compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-save"))
                compose.onAllNodes(hasTestTag("project-style-save") and isEnabled()).fetchSemanticsNodes().size == 1
            } catch (_: AssertionError) { false }
        }
        click("project-style-1", "project-style-save")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-target-app/Main").fetchSemanticsNodes().size == 1 }
        val saved = runBlocking { fixture.repo.load(1) }
        assertEquals(originalOrder.reversed(), saved.references.map { it.hash })
        assertEquals("Owner reviewed reference icons; keep target identity", saved.revision!!.specification)
        assertNull(saved.revision!!.analysisProfileJson)
        assertFalse(hasKey); assertEquals(0, providerCalls)
        // Preserve the existing Trial UI's key-presence gate. This boolean is
        // synthetic; free Save above used no key and no provider is submitted.
        hasKey = true
        click("project-targets", "setup-target-app/Main")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) { !trials.state(1).value.busy && trials.state(1).value.preparation != null &&
            compose.onAllNodesWithTag("trial-style").fetchSemanticsNodes().size == 1 }
        click("project-trials-1", "trial-prepare")
        compose.waitUntil(10000) { trials.state(1).value.plan != null && !trials.state(1).value.busy }
        val plan = checkNotNull(trials.state(1).value.plan)
        assertTrue(trials.state(1).value.hasKey)
        assertEquals("New free Save-to-Trial plans must freeze the new contract", 2,
            org.json.JSONObject(plan.encode()).optInt("promptVersion", 1))
        assertEquals(saved.references.map { it.hash }, plan.referenceHashes)
        val original = fixture.originals.getValue("app/Main")
        assertEquals(original.hash, plan.inputs.single().originalHash)
        assertTrue(original.hash !in plan.referenceHashes)
        assertArrayEquals(original.png, runBlocking { trials.preview(1, original.hash) })
        saved.references.forEach { assertArrayEquals(runBlocking { fixture.repo.referenceBytes(1, it.hash) },
            runBlocking { trials.preview(1, it.hash) }) }
        val request = dev.renkinProject.renkin.ai.buildImageEditRequest("synthetic-not-a-key",
            AiSettings(style = plan.specification, model = plan.context.model, quality = plan.context.quality,
                referenceNames = plan.referenceHashes.indices.map { "img_0_$it.png" }),
            listOf(original.png) + plan.referenceHashes.map { checkNotNull(runBlocking { trials.preview(1, it) }) },
            plan.appearance(plan.inputs.single()), plan.promptVersion)
        val body = request.body as okhttp3.MultipartBody
        val prompt = okio.Buffer().also { buffer -> body.parts.single {
            it.headers?.get("Content-Disposition") == "form-data; name=\"prompt\""
        }.body.writeTo(buffer) }.readUtf8()
        assertEquals(plan.promptHashes.getValue("app/Main"), sha256(prompt.toByteArray()))
        assertTrue(prompt.contains(saved.revision!!.specification))
        assertEquals(3, body.parts.count { it.headers?.get("Content-Disposition")?.contains("name=\"image[]\"") == true })
        assertEquals(0, providerCalls)
        assertTrue(runBlocking { fixture.journal.records(1) }.isEmpty())
        // Target selection legitimately updates metadata/version; the saved style
        // revision and its ordered assets must remain byte-for-byte unchanged.
        val afterTrial = runBlocking { fixture.repo.load(1) }
        assertEquals(saved.revision, afterTrial.revision)
        assertEquals(saved.references, afterTrial.references)
        compose.onNodeWithTag("trial-decline").assertIsEnabled().performClick()
        click("project-trials-1", "trial-options-toggle")
        click("project-trials-1", "trial-refine")
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("project-style-description"))
        compose.waitUntil(10000) { compose.onAllNodes(hasTestTag("project-style-description") and isEnabled()).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("project-style-description").assertTextContains("Owner reviewed reference icons; keep target identity")
        val reopened = runBlocking { fixture.repo.load(1) }
        assertEquals(saved.revision, reopened.revision)
        assertEquals(saved.references, reopened.references)
        assertEquals(1, styleSaves); assertEquals(0, providerCalls)
    }

    @Test fun firstQuickScanWaitsForVerifiedSourceAndRecreationNeverSubmits() {
        val artwork = FixtureReferenceReader((0 until 40).associate { "draw$it" to analysisFixturePng(2, it) })
        val references = object : CoverageSourceReader by artwork {
            override val index = artwork.index.copy(stamp = fixture.index.stamp)
        }
        analysis = OwnedStyleAnalysis(scope, fixture.repo, StyleAnalysisJournal(fixture.context), ReferenceSampler(),
            StyleAnalysisCoordinator(StyleAnalysisJournal(fixture.context), StyleAnalysisGateway {
                providerCalls++; error("Legacy sampling must not submit")
            }), { false }, object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T) =
                    this@GuidedProjectRoomJourneyComposeTest.owned(id, operation)
                override suspend fun catalogStamp() = projectCatalogStamp(fixture.targets)
                override suspend fun reader(source: String, referenceIndex: Boolean) =
                    if (referenceIndex) references else fixture.reader
            }, StyleDnaCoordinator(StyleDnaJournal(fixture.context), StyleDnaGateway { _, _ ->
                providerCalls++; error("Free Quick scan must not submit")
            }))
        coverageLoaded = false
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        click("project-home", "project-extend")
        click("setup-source", "setup-source-source.pack")
        click("setup-profile", "setup-profile-1")
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-project-style"))
        compose.onNodeWithTag("setup-project-style").assertIsNotEnabled()
        assertEquals(0, providerCalls)
        compose.runOnIdle { coverageLoaded = true }
        click("project-targets", "setup-project-style")
        compose.onNodeWithTag("workflow-step").assertTextContains("2 of 4", substring = true)
        click("project-style-1", "project-style-analysis-options")
        click("project-style-1", "project-style-method-QUICK")
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("style-dna-mode-QUICK"))
        compose.onNodeWithTag("style-dna-mode-QUICK").assertIsSelected()
        fun scan() {
            if (compose.onAllNodesWithTag("style-dna-panel").fetchSemanticsNodes().isEmpty()) {
                click("project-style-1", "project-style-analysis-options")
                click("project-style-1", "project-style-method-QUICK")
            }
            click("project-style-1", "style-dna-prepare")
            compose.waitUntil(10000) { analysis!!.state(1).value.let { !it.busy && (it.dnaPlan != null || it.error != null) } }
            val current = analysis!!.state(1).value
            assertNull(current.error)
            val plan = checkNotNull(current.dnaPlan)
            plan.validate()
            assertEquals(projectSourceStamp(fixture.index), plan.context.setupStamp)
            assertEquals(projectSourceStamp(references.index), plan.context.referenceStamp)
            assertNull(plan.context.baseRevision)
            assertEquals(0L, plan.context.metadataVersion)
            assertEquals(sha256(ByteArray(0)), plan.context.selectionHash)
            assertTrue(plan.samples.size in 1..12)
            assertEquals(1, plan.endpointMaximum)
            compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("style-dna-start"))
            compose.onNodeWithTag("style-dna-start").assertIsNotEnabled()
            compose.onNodeWithTag("style-dna-no-key").assertExists()
            assertNull(runBlocking { fixture.repo.load(1).revision })
            assertEquals(0, providerCalls)
        }
        scan()
        val frozenBeforeRecreation = checkNotNull(analysis!!.state(1).value.dnaPlan)
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals("Recreating the real panel must retain the unsubmitted current frozen plan",
            frozenBeforeRecreation, analysis!!.state(1).value.dnaPlan)
        assertEquals(0, providerCalls)
        scan()
        compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag("setup-back"))
        compose.onNodeWithTag("setup-back").performClick()
        click("project-targets", "setup-project-style")
        scan()
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
        enterManualStyle()
        click("project-style-1", "project-style-save")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-target-app/Main").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        assertEquals(1L, runBlocking { fixture.repo.load(1).revision!!.revision })
        assertEquals(1, styleSaves); assertEquals(0, providerCalls)
        click("project-targets", "setup-target-app/Main")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(10000) {
            val current = trials.state(1).value
            // Room/controller readiness can precede collectAsState's UI delivery.
            // This visible header is emitted only by the prepared Trial content;
            // wait for that composition before searching its lazy Prepare item.
            current.preparation != null && !current.busy &&
                compose.onAllNodesWithTag("trial-style").fetchSemanticsNodes().size == 1
        }
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
        enterManualStyle()
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
