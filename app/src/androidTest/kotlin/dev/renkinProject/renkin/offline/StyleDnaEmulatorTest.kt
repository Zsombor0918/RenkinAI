package dev.renkinProject.renkin.offline

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.IconGenerationGateway
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.packages.IconPackCatalog
import dev.renkinProject.renkin.ui.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/** Real Android resources, Compose routes, HTTP, SQLite and AtomicFile journals.
 * Every byte is synthetic. This suite refuses physical devices and never reads credentials.
 * The runner force-stops the process between the first four methods and restartReadOnly. */
@RunWith(AndroidJUnit4::class)
class StyleDnaEmulatorTest {
    @get:Rule val compose = createComposeRule()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val pack = "dev.renkinai.offlinefixture"
    private val target = CoverageTarget("$pack/android.app.Activity", "Synthetic fixture application")
    private lateinit var fixture: Fixture
    private var mounted by mutableStateOf(true)
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Offline project")),
        availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var metadata by mutableStateOf<ProjectAiMetadata?>(null)

    @Before fun emulatorOnly() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) { "Synthetic E2E requires an Android emulator" }
        check(app.packageName.endsWith(".dev")) { "Synthetic E2E requires the development application" }
    }
    @After fun close() { if (::fixture.isInitialized) fixture.close() }

    private inner class Fixture(val name: String) : AutoCloseable {
        private val root = File(app.filesDir, "offline-dna-fixtures/$name")
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "noBackup").apply { mkdirs() }
            override fun getDatabasePath(name: String) = File(root, "database/$name").apply { parentFile!!.mkdirs() }
        }
        val saveReturnedOnMain = AtomicBoolean()
        val trace = OfflineDnaTrace(name)
        private val journalTrace = trace.journal(context.noBackupFilesDir)
        val db = Room.databaseBuilder(context, RenkinPackDatabase::class.java, "offline.db").build()
        val repo = ProjectAiRepository(db, ProjectAssetStore(context))
        val dnaJournal = StyleDnaJournal(context)
        val legacyJournal = StyleAnalysisJournal(context)
        val trialJournal = StyleTrialJournal(context)
        // MainViewModel owns these controllers on viewModelScope (Main.immediate).
        // Match that ownership instead of letting the test frame clock resume on IO.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val mutex = Mutex()
        val server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 0) }
        // MockWebServer.url uses InetAddress.canonicalHostName, which Android's
        // hosts file maps back to localhost. Keep the literal address required by
        // both our deny-by-default DNS guard and debug-only cleartext policy.
        val defaultServerHost = server.url("/v1/responses").host
        val loopbackUrl = server.url("/v1/responses").newBuilder().host("127.0.0.1").build().also {
            check(it.scheme == "http" && it.host == "127.0.0.1" && it.port == server.port)
        }
        val posts = AtomicInteger()
        val trialKeyAvailable = AtomicBoolean(name != "references")
        val client = OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .callTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    if (hostname != "127.0.0.1") throw UnknownHostException("Offline fixture denies external DNS")
                    return listOf(InetAddress.getByName("127.0.0.1"))
                }
            }).addInterceptor { chain ->
                val request = chain.request()
                check(request.url == "https://api.openai.com/v1/responses".toHttpUrl())
                check(request.method == "POST" && request.header("Authorization") == "Bearer $TOKEN")
                chain.proceed(request.newBuilder().url(loopbackUrl).build())
            }.addNetworkInterceptor { chain ->
                check(chain.request().url.host == "127.0.0.1" && chain.request().url.port == server.port)
                check(chain.connection()!!.route().socketAddress.address.isLoopbackAddress)
                posts.incrementAndGet()
                chain.proceed(chain.request())
            }.build()
        lateinit var reader: CoverageSourceReader
        lateinit var packInfo: IconPack
        lateinit var coverage: CoverageSnapshot
        suspend fun <T> owned(id: Long, block: suspend () -> T): T = withContext(Dispatchers.Main.immediate) {
            mutex.withLock {
                check(id == 1L)
                val job = System.identityHashCode(currentCoroutineContext()[Job])
                trace.event("owner.enter", "job" to job)
                try { block() } finally { trace.event("owner.finally", "job" to job) }
            }
        }
        val analysis = OwnedStyleAnalysis(scope, repo, legacyJournal, ReferenceSampler(),
            StyleAnalysisCoordinator(legacyJournal, StyleAnalysisGateway { error("Legacy submission is forbidden") }),
            { name != "references" }, object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T) = this@Fixture.owned(id, operation)
                override suspend fun catalogStamp() = projectCatalogStamp(listOf(target))
                override suspend fun reader(source: String, referenceIndex: Boolean): CoverageSourceReader {
                    check(source == pack); return reader
                }
            }, StyleDnaCoordinator(dnaJournal, OpenAiStyleDnaGateway({ TOKEN }, client,
                "https://api.openai.com/v1/responses".toHttpUrl())))
        private val actualTrials = OwnedStyleTrials(scope, repo, trialJournal,
            StyleTrialCoordinator(trialJournal, IconGenerationGateway { error("Trial submission is forbidden") }, repo),
            { trialKeyAvailable.get() }, object : TrialSessionOwner {
                override suspend fun <T> owned(id: Long, action: suspend () -> T) = this@Fixture.owned(id, action)
                override suspend fun targets() = listOf(target)
                override suspend fun reader(source: String): CoverageSourceReader { check(source == pack); return reader }
                override suspend fun original(key: String): TrialOriginal {
                    check(key == target.key)
                    return currentTrialOriginal(app, key)
                }
                override suspend fun settings() = AiSettings()
            })
        val trials = trace.trials(actualTrials, scope)
        private val observedAnalysis = object : ProjectAnalysisBackend by analysis {
            override suspend fun saveDna(id: Long, planId: String?, grammar: String, selectedFamily: String,
                hashes: List<String>, expected: Long?, draft: ProjectStyleDraft): SavedProjectStyle {
                trace.event("dna.save.enter", "references" to hashes.size, "expected_revision" to expected)
                return try { analysis.saveDna(id, planId, grammar, selectedFamily, hashes, expected, draft).also {
                    saveReturnedOnMain.set(android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
                    trace.event("dna.save.persisted", "revision" to it.revision?.revision, "references" to it.references.size)
                } } finally { trace.event("dna.save.finally") }
            }
        }
        private val analysisObserver = scope.launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            analysis.state(1).collect { trace.event("dna.state.observed", "busy" to it.busy, "network" to it.network,
                "plan" to it.dnaPlan?.id, "status" to it.dnaRecord?.status?.name,
                "submitted" to it.dnaRecord?.submitted, "reports" to it.dnaRecord?.reports?.size, "error" to it.error) }
        }
        val style = object : ProjectStyleBackend {
            override val analysis get() = observedAnalysis
            override val localReferences = LocalStyleReferences(repo, ReferenceSampler(), object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T) = this@Fixture.owned(id, operation)
                override suspend fun catalogStamp() = error("Free style does not read targets")
                override suspend fun reader(source: String, referenceIndex: Boolean): CoverageSourceReader {
                    check(source == pack); return reader
                }
            })
            override suspend fun load(id: Long) = owned(id) {
                trace.event("style.load.enter")
                repo.load(id).also { trace.event("style.load.complete", "revision" to it.revision?.revision,
                    "references" to it.references.size) }
            }
            override suspend fun referencePreview(id: Long, hash: String) = owned(id) { repo.referenceBytes(id, hash) }
            override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset = error("No external input")
            override suspend fun copyGlobal(id: Long): ProjectStyleDraft = error("No credential/settings input")
            override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode,
                source: String?, stamp: String?): SavedProjectStyle = localReferences.save(id, expected, draft, mode, source, stamp)
        }
        suspend fun initialize(create: Boolean) {
            val catalog = IconPackCatalog(app)
            packInfo = catalog.installedIconPacks().single { it.packageName == pack }
            val packs = IconPackRepository(app, catalog)
            packs.load(emptyList())
            reader = packs.coverageReader(pack, rescan = true, allMappings = true)
            assertEquals(CoverageIndexState.READY, reader.index.state)
            assertEquals(60, reader.index.mappings.size)
            assertTrue(reader.isCurrent())
            coverage = IconCoverageAnalyzer().analyze(1, listOf(target), reader)
            assertEquals(SourceCoverage.UNMAPPED, coverage.entries.single().source)
            if (create) RenkinPackRepository(db).replaceEverything(listOf(Profile(1, "Offline project")), emptyList())
        }
        fun assertPosts(expected: Int) {
            assertEquals("Actual loopback POST count", expected, server.requestCount)
            assertEquals("Only verified loopback connections", expected, posts.get())
        }
        override fun close() {
            trace.trial("fixture.close", trials.state(1).value)
            runBlocking {
                val saved = repo.load(1)
                trace.event("disk.reopen.snapshot", "revision" to saved.revision?.revision, "references" to saved.references.size,
                    "dna_statuses" to dnaJournal.records(1).joinToString { it.status.name },
                    "dna_submitted" to dnaJournal.records(1).sumOf { it.submitted }, "trial_records" to trialJournal.records(1).size)
                scope.coroutineContext.job.cancelAndJoin()
            }
            journalTrace.close()
            db.close(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown(); server.shutdown()
        }
    }

    @Composable private fun Content() {
        RenkinTheme(false) {
            ProjectShell(home, operation,
                onOpen = { home = home.copy(activeProfileId = it); operation = ProjectSessionState.Opened(it) },
                onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {},
                onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                readSetupContext = { fixture.repo.metadata(it) }, bindTrialContext = fixture.trials::bindContext,
                setupData = { _, _, id, _ ->
                    ProjectSetupData(targets = listOf(target), packs = listOf(fixture.packInfo), savedMetadata = metadata,
                        coverage = CoverageUiState.Ready(fixture.coverage),
                        onSaveSelection = { version, selection -> fixture.owned(1) {
                            verifyProjectSelectionContext(selection, listOf(target)) { _, _ -> fixture.reader }
                            fixture.trace.event("selection.save.enter", "expected_version" to version)
                            fixture.repo.saveSelection(1, version, selection).also {
                                metadata = it; fixture.trace.event("selection.save.persisted", "version" to it.version)
                            }
                        } })
                },
                stylePanel = { id, mode, source, stamp, back, next ->
                    ProjectStylePanel(id, mode, source, stamp, true, fixture.style, back, next,
                        onStyleSaved = { metadata = it.metadata; fixture.trace.event("style.parent.saved"); next() }, guided = true)
                }, guidedWorkflow = true,
                guidedTrialPanel = { id, mode, source, back, refine, editor, complete ->
                    val traceSink = remember(fixture) { { event: String, reason: String ->
                        fixture.trace.event(event, "reason" to reason)
                    } }
                    CompositionLocalProvider(LocalProjectInvalidationTrace provides traceSink) {
                    ProjectTrialPanel(id, "Offline project", mode, source, true, fixture.trials, back, refine, editor,
                        onComplete = complete, appName = { target.name }, guided = true)
                    }
                }) { Text("Preserved classic editor") }
        }
    }

    private fun open(name: String, create: Boolean = true) {
        // Only this emulator's dedicated synthetic fixture subtree may be reset.
        if (create) {
            val parent = File(app.filesDir, "offline-dna-fixtures").canonicalFile
            val root = File(parent, name).canonicalFile
            check(root.parentFile == parent && name in setOf("quick", "standard", "failure", "references"))
            root.deleteRecursively()
        }
        fixture = Fixture(name)
        runBlocking { fixture.initialize(create); metadata = fixture.repo.metadata(1) }
        compose.setContent { if (mounted) Content() }
        enterStyle()
    }
    private fun enterStyle() {
        click("project-home", "project-extend")
        click("setup-source", "setup-source-$pack")
        click("setup-profile", "setup-profile-1")
        click("project-targets", if (metadata?.activeRevision != null) "setup-edit-style" else "setup-project-style")
    }
    private fun click(list: String, tag: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag(list).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        compose.waitUntil(15000) { compose.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().size == 1 }
        compose.waitForIdle()
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        fixture.trace.event("ui.click.before", "tag" to tag)
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
        fixture.trace.event("ui.click.return", "tag" to tag)
        compose.waitForIdle()
    }
    private fun idleAnalysis() {
        compose.waitUntil(30000) { !fixture.analysis.state(1).value.busy }
        compose.waitForIdle()
        assertNull(fixture.analysis.state(1).value.error)
    }
    private fun prepare(mode: DnaAnalysisMode): StyleDnaPlan {
        if (compose.onAllNodesWithTag("style-dna-panel").fetchSemanticsNodes().isEmpty()) {
            click("project-style-1", "project-style-analysis-options")
            click("project-style-1", "project-style-method-${mode.name}")
        }
        click("project-style-1", "style-dna-mode-${mode.name}")
        click("project-style-1", "style-dna-prepare")
        idleAnalysis()
        val plan = checkNotNull(fixture.analysis.state(1).value.dnaPlan)
        assertEquals(if (mode == DnaAnalysisMode.QUICK) 12 else 24, plan.samples.size)
        return plan
    }
    private fun confirm() {
        click("project-style-1", "style-dna-start")
        compose.onNodeWithTag("style-dna-confirm-paid").assertIsEnabled().performClick()
    }
    private fun reports(plan: StyleDnaPlan) = plan.samples.map { sample ->
        DnaIconReport(sample.id, sample.hash, DnaFamilyVote.SOLID, DnaSymbolType.GEOMETRIC_PICTOGRAM,
            DNA_TRAITS.associateWith { trait -> DnaObservation(when (trait) {
                "tile_geometry" -> "No tile; transparent canvas"
                "stroke_fill" -> "Solid filled shapes with no visible stroke"
                "shadow_gradient" -> "No depth or gradient observed"
                else -> "Bounded planar contours and controlled transparent spacing"
            }, DnaProvenance.OBSERVED) }, PackColorPolicy.ADAPT_BRAND_COLORS, DnaTile.NONE, DnaDepth.NONE)
    }
    private fun response(plan: StyleDnaPlan, step: DnaStep): MockResponse {
        val images = checkNotNull(fixture.analysis.state(1).value.samples).samples.associate { it.hash to it.png }
        val allReports = reports(plan)
        val text = if (step.synthesis) synthesizeDnaLocally(allReports, images).encode() else
            canonicalJson(json("schema" to 1, "reports" to JSONArray(allReports.filter { it.sampleId in step.samples }.map { it.json() })))
        val call = DnaCall(plan, step, step.samples.associate { plan.bindings.getValue(it) to images.getValue(plan.bindings.getValue(it)) },
            if (step.synthesis) allReports else emptyList())
        return MockResponse().setBody(json("id" to "resp_emulator_fixture", "object" to "response", "status" to "completed",
            "error" to null, "incomplete_details" to null, "model" to plan.model, "store" to false,
            "max_output_tokens" to 12288, "reasoning" to json("effort" to "none", "summary" to null),
            "text" to dnaBody(call).getJSONObject("text"),
            "usage" to json("input_tokens" to 1800, "output_tokens" to 4096, "total_tokens" to 5896,
                "output_tokens_details" to json("reasoning_tokens" to 0)),
            "output" to JSONArray().put(json("type" to "reasoning", "id" to "rs_fixture", "summary" to JSONArray()))
                .put(json("type" to "message", "id" to "msg_fixture", "status" to "completed", "role" to "assistant",
                    "content" to JSONArray().put(json("type" to "output_text", "text" to text,
                        "annotations" to JSONArray(), "logprobs" to JSONArray()))))).toString())
    }
    private fun awaitTrialPlan() {
        // Idle alone also describes an initial or intervening local load. Await
        // the requested result; a persistently missing plan must still fail.
        compose.waitUntil(15000) {
            fixture.trials.state(1).value.let { !it.busy && (it.plan != null || it.error != null) }
        }
    }
    private fun prepareVisibleTrial() {
        // A completed controller read can precede Compose's consumption, and a
        // queued local reload can briefly replace the list with loading content.
        // Observe/materialize the real control; never retry its click or request.
        compose.waitUntil(15000) {
            val state = fixture.trials.state(1).value
            if (state.busy || state.preparation == null) false else try {
                compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-prepare"))
                compose.onAllNodes(hasTestTag("trial-prepare") and isEnabled()).fetchSemanticsNodes().size == 1
            } catch (_: AssertionError) { false }
        }
        fixture.trace.trial("ui.prepare.before", fixture.trials.state(1).value)
        compose.onNodeWithTag("trial-prepare").assertIsDisplayed().assertIsEnabled().performClick()
        fixture.trace.trial("ui.prepare.input.return", fixture.trials.state(1).value)
        try { awaitTrialPlan() } finally {
            fixture.trace.trial("ui.prepare.wait.finally", fixture.trials.state(1).value)
        }
    }
    private fun trialPreparation() {
        click("project-targets", "setup-target-${target.key}")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(15000) { fixture.trials.state(1).value.let { !it.busy && it.preparation != null } }
        prepareVisibleTrial()
        assertNull(fixture.trials.state(1).value.error)
        val plan = checkNotNull(fixture.trials.state(1).value.plan)
        assertEquals(listOf(target.key), plan.inputs.map { it.key })
        val saved = runBlocking { fixture.repo.load(1) }
        assertEquals(saved.references.map { it.hash }, plan.referenceHashes)
        assertTrue(plan.drawingRecipes.getValue(target.key).isNotBlank())
        assertTrue(plan.inputs.single().origin.startsWith("launcher:${target.key}:"))
        assertNotNull(plan.inputs.single().originalEvidence)
        assertTrue(runBlocking { fixture.trialJournal.records(1) }.isEmpty())
        // Opening/reviewing this plan grants no image-generation permission.
        compose.waitForIdle()
        compose.onNodeWithTag("trial-decline").assertIsEnabled().performClick()
    }
    private fun journey(name: String, mode: DnaAnalysisMode) {
        open(name)
        val plan = prepare(mode)
        fixture.assertPosts(0)
        plan.steps.forEach { fixture.server.enqueue(response(plan, it)) }
        confirm(); idleAnalysis()
        val record = checkNotNull(fixture.analysis.state(1).value.dnaRecord)
        assertEquals(DnaStatus.RESULT_SAVED, record.status)
        assertEquals(plan.samples.size, record.reports.size)
        assertEquals(plan.endpointMaximum, record.submitted)
        assertTrue(record.reports.all { plan.bindings[it.sampleId] == it.sampleHash })
        fixture.assertPosts(plan.endpointMaximum)
        click("project-style-1", "style-dna-reviewed")
        click("project-style-1", "style-dna-save")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("project-targets").fetchSemanticsNodes().isNotEmpty() }
        val saved = runBlocking { fixture.repo.load(1) }
        assertNotNull(SavedStyleDna.optional(saved.revision!!.analysisProfileJson))
        assertEquals(DnaStatus.REVIEWED, runBlocking { fixture.dnaJournal.latest(1) }!!.status)
        assertTrue("Fixture UI Save must return on Android main like production", fixture.saveReturnedOnMain.get())
        trialPreparation()
        fixture.assertPosts(plan.endpointMaximum)
        evidence(name, json("samples" to plan.samples.size, "reports" to record.reports.size,
            "loopback_posts" to fixture.server.requestCount, "external_posts" to 0, "saved_revision" to saved.revision!!.revision,
            "trial_prepared" to true, "mock_server_default_host" to fixture.defaultServerHost))
    }
    @Test fun quickJourney() = journey("quick", DnaAnalysisMode.QUICK)
    @Test fun standardJourney() = journey("standard", DnaAnalysisMode.STANDARD)

    @Test fun referenceFirstJourney() {
        open("references")
        assertFalse(fixture.analysis.state(1).value.hasKey)
        click("project-style-1", "project-style-sample-pack")
        compose.waitUntil(15000) {
            compose.onAllNodes(hasTestTag("project-style-sample-pack") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        click("project-style-1", "project-style-select-candidate-0")
        click("project-style-1", "project-style-select-candidate-1")
        click("project-style-1", "project-style-up-1")
        click("project-style-1", "project-style-save")
        val saved = runBlocking { fixture.repo.load(1) }
        assertEquals(2, saved.references.size)
        assertNull(saved.revision!!.analysisProfileJson)
        assertTrue(saved.revision!!.specification.contains("reference icons"))
        assertFalse(fixture.trialKeyAvailable.get())
        fixture.assertPosts(0)
        // Only synthetic presence for the unchanged Trial preparation gate.
        // There is no actual image-generation credential or submission.
        fixture.trialKeyAvailable.set(true)
        click("project-targets", "setup-target-${target.key}")
        click("project-targets", "setup-save-selection")
        compose.waitUntil(15000) { fixture.trials.state(1).value.let { !it.busy && it.preparation != null } }
        prepareVisibleTrial()
        val plan = checkNotNull(fixture.trials.state(1).value.plan)
        assertTrue(fixture.trials.state(1).value.hasKey)
        assertEquals(2, plan.promptVersion)
        assertEquals(plan, StyleTrialPlan.parse(plan.encode()))
        assertEquals(sha256(plan.effectivePrompt(plan.inputs.single()).toByteArray()), plan.promptHashes.getValue(target.key))
        assertEquals(saved.references.map { it.hash }, plan.referenceHashes)
        val original = runBlocking { currentTrialOriginal(app, target.key) }
        assertEquals(original.hash, plan.inputs.single().originalHash)
        assertArrayEquals(original.png, runBlocking { fixture.trials.preview(1, original.hash) })
        saved.references.forEach { assertArrayEquals(runBlocking { fixture.repo.referenceBytes(1, it.hash) },
            runBlocking { fixture.trials.preview(1, it.hash) }) }
        assertTrue(runBlocking { fixture.dnaJournal.records(1) }.isEmpty())
        assertTrue(runBlocking { fixture.trialJournal.records(1) }.isEmpty())
        fixture.assertPosts(0)
        evidence("references", json("loopback_posts" to 0, "external_posts" to 0, "selected_references" to 2,
            "trial_prepared" to true, "saved_revision" to saved.revision!!.revision))
    }

    @Test fun incompleteAndCancel() {
        open("failure")
        val first = prepare(DnaAnalysisMode.QUICK)
        fixture.server.enqueue(MockResponse().setBody(json("id" to "resp_incomplete", "status" to "incomplete",
            "incomplete_details" to json("reason" to "max_output_tokens"), "output" to JSONArray(),
            "usage" to json("input_tokens" to 1800, "output_tokens" to 12288)).toString()))
        confirm(); idleAnalysis()
        val failed = checkNotNull(fixture.analysis.state(1).value.dnaRecord)
        assertEquals("RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS", failed.reason)
        assertNull(failed.result); assertNull(runBlocking { fixture.repo.load(1) }.revision)
        fixture.assertPosts(1)
        prepare(DnaAnalysisMode.QUICK)
        fixture.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        confirm()
        compose.waitUntil(15000) { fixture.server.requestCount == 2 }
        click("project-style-1", "style-dna-cancel")
        idleAnalysis()
        val cancelled = checkNotNull(runBlocking { fixture.dnaJournal.latest(1) })
        assertEquals("CANCELLED_NO_REPLAY", cancelled.reason)
        assertNull(cancelled.result)
        runBlocking { fixture.analysis.refresh(1) }
        assertNull(fixture.analysis.state(1).value.dnaPlan)
        assertNull(runBlocking { fixture.repo.load(1) }.revision)
        assertEquals(2, runBlocking { fixture.dnaJournal.records(1) }.size)
        assertEquals(1, runBlocking { fixture.dnaJournal.record(1, first.id) }!!.submitted)
        fixture.assertPosts(2)
        evidence("failure", json("loopback_posts" to 2, "external_posts" to 0, "consumed_attempts" to 2,
            "accepted_styles" to 0, "incomplete_reason" to failed.reason, "cancel_reason" to cancelled.reason))
    }

    @Test fun restartReadOnly() {
        val evidenceFile = File(app.filesDir, "offline-dna-evidence.json")
        val previous = JSONObject(evidenceFile.readText())
        assertTrue(previous.has("quick") && previous.has("standard") && previous.has("failure") && previous.has("references"))
        for (name in listOf("quick", "standard", "failure", "references"))
            assertNotEquals("Runner must terminate the earlier application process", android.os.Process.myPid(),
                previous.getJSONObject(name).getInt("process_id"))
        // A new instrumentation process reconstructs Room/controllers from disk, with an empty server.
        open("quick", create = false)
        val saved = runBlocking { fixture.repo.load(1) }
        assertEquals(previous.getJSONObject("quick").getLong("saved_revision"), saved.revision!!.revision)
        assertNotNull(SavedStyleDna.optional(saved.revision!!.analysisProfileJson))
        runBlocking { fixture.analysis.refresh(1) }
        assertNull(fixture.analysis.state(1).value.dnaPlan)
        click("project-style-1", "style-trials")
        // Targets were already persisted; continue is a read-only selection operation.
        click("project-targets", "setup-save-selection")
        compose.waitUntil(15000) { fixture.trials.state(1).value.let { !it.busy && it.preparation != null } }
        prepareVisibleTrial()
        assertNull(fixture.trials.state(1).value.error)
        assertNotNull(fixture.trials.state(1).value.plan)
        fixture.assertPosts(0)
        // Independently open Standard and failed journals after the same real process restart.
        for (name in listOf("standard", "failure", "references")) {
            Fixture(name).use { other -> runBlocking {
                other.initialize(false)
                if (name == "standard") assertNotNull(SavedStyleDna.optional(other.repo.load(1).revision!!.analysisProfileJson))
                else if (name == "references") {
                    val local = other.repo.load(1)
                    assertEquals(2, local.references.size)
                    assertNull(local.revision!!.analysisProfileJson)
                    assertEquals(previous.getJSONObject("references").getLong("saved_revision"), local.revision!!.revision)
                    assertTrue(other.dnaJournal.records(1).isEmpty())
                } else {
                    assertNull(other.repo.load(1).revision)
                    other.analysis.bindContext(1, ProjectSetupMode.EXTEND, pack); other.analysis.refresh(1)
                    assertNull(other.analysis.state(1).value.dnaPlan)
                    assertEquals(2, other.dnaJournal.records(1).size)
                    assertEquals(2, other.dnaJournal.records(1).sumOf { it.submitted })
                }
                other.assertPosts(0)
            } }
        }
        evidence("restart", json("process_restart" to true, "loopback_posts" to 0, "external_posts" to 0,
            "saved_projects_reopened" to 3, "trial_prepared" to true, "uncertain_attempts_replayed" to 0))
    }
    private fun evidence(name: String, value: JSONObject) {
        val file = File(app.filesDir, "offline-dna-evidence.json")
        val root = if (file.exists()) JSONObject(file.readText()) else JSONObject()
        value.put("process_id", android.os.Process.myPid()).put("android_api", Build.VERSION.SDK_INT)
        root.put(name, value)
        fixture.trace.event("evidence.save.enter", "journey" to name)
        file.writeText(root.toString(2))
        fixture.trace.event("evidence.save.complete", "journey" to name)
    }
    companion object { private const val TOKEN = "offline-emulator-synthetic-never-a-paid-key" }
}
