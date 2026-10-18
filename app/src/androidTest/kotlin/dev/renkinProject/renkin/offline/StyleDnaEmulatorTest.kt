package dev.renkinProject.renkin.offline

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.IconGenerationGateway
import dev.renkinProject.renkin.ai.ClientIconGenerationGateway
import dev.renkinProject.renkin.ai.OpenAiImageClient
import dev.renkinProject.renkin.ai.StabilityImageClient
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
import org.json.JSONObject
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import java.net.InetAddress
import java.net.Proxy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/** Real Android resources, Compose routes, HTTP, SQLite and AtomicFile journals.
 * Every byte is synthetic. This suite refuses physical devices and never reads credentials.
 * The runner force-stops the process between the two active write journeys and restartReadOnly. */
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
        val trialKeyAvailable = AtomicBoolean(false)
        val gatewayCalls = AtomicInteger()
        val server = if (name == "montage") MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 0) } else null
        val analysisClient = OkHttpClient.Builder().proxy(Proxy.NO_PROXY).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).addNetworkInterceptor { chain ->
                check(chain.request().url.host == "127.0.0.1" && chain.request().url.port == server!!.port)
                check(chain.connection()!!.route().socketAddress.address.isLoopbackAddress)
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
        private val imageGateway = server?.let { loopback ->
            // MockWebServer may expose localhost; the debug policy permits only literal loopback.
            val endpoint = loopback.url("/v1/images/edits").newBuilder().host("127.0.0.1").build()
            ClientIconGenerationGateway({ "offline-emulator-image-not-a-real-key" }, OpenAiImageClient(analysisClient, endpoint),
                StabilityImageClient(analysisClient, endpoint, endpoint))
        }
        private val actualTrials = OwnedStyleTrials(scope, repo, trialJournal,
            StyleTrialCoordinator(trialJournal, IconGenerationGateway { request ->
                gatewayCalls.incrementAndGet()
                checkNotNull(imageGateway) { "Trial submission is forbidden for manual fixtures" }.generate(request)
            }, repo),
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
        val style = object : ProjectStyleBackend {
            override fun sourceLabel(source: String) = packInfo.applicationName.also { check(source == pack) }
            private val owner = object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T) = this@Fixture.owned(id, operation)
                override suspend fun catalogStamp() = error("No target inventory for pack analysis")
                override suspend fun reader(source: String, referenceIndex: Boolean) = reader.also { check(source == pack) }
            }
            override val montages = if (name == "montage") LocalPackMontages(repo, ReferenceSampler(), owner) else null
            override val packAnalysis = montages?.let { local -> PackStyleAnalysis(repo, local, legacyJournal,
                PackStyleGateway({ "offline-emulator-never-a-real-key" }, analysisClient,
                    server!!.url("/v1/responses").newBuilder().host("127.0.0.1").build())::analyze, { true }, owner) }
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
            assertEquals(0, expected)
            assertEquals("No generation endpoint was authorized", 0, gatewayCalls.get())
            assertNull(runBlocking { legacyJournal.latest(1) })
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
            server?.shutdown(); analysisClient.connectionPool.evictAll(); analysisClient.dispatcher.executorService.shutdownNow()
            db.close()
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
                        coverage = CoverageUiState.Ready(fixture.coverage), automaticTrialSuggestions = fixture.name == "montage",
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
            check(root.parentFile == parent && name in setOf("references", "decline", "montage"))
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
    @Test fun referenceFirstJourney() = referenceJourney("references", false)
    @Test fun decliningTrialCannotRestoreTheFrozenPlan() = referenceJourney("decline", true)

    @Test fun montageJourney() {
        open("montage")
        compose.waitUntil(15000) { runBlocking { fixture.repo.load(1).revision } != null }
        val initial = runBlocking { fixture.repo.load(1) }
        val originalMontage = PackMontage.optional(initial.revision!!.analysisProfileJson)!!
        assertTrue(originalMontage.members.size in 20..30)
        assertEquals(2, originalMontage.pages.size)
        compose.waitUntil(15000) { compose.onAllNodesWithTag("pack-sheet-0").fetchSemanticsNodes().isNotEmpty() }
        screenshot("automatic-montage")
        click("pack-montage", "pack-browse")
        // Resource scanning is asynchronous; Compose idleness is not its completion.
        compose.waitUntil(15000) {
            compose.onAllNodes(hasTestTag("pack-browse") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        val removed = originalMontage.members.first().hash
        click("pack-montage", "pack-toggle-$removed")
        compose.waitUntil(15000) { runBlocking { fixture.repo.load(1).revision!!.revision } > initial.revision!!.revision }
        val edited = runBlocking { fixture.repo.load(1) }
        val montage = PackMontage.optional(edited.revision!!.analysisProfileJson)!!
        assertEquals(originalMontage.members.size - 1, montage.members.size)
        assertFalse(removed in montage.members.map { it.hash }); assertNull(montage.result)
        assertEquals(0, fixture.server!!.requestCount)
        assertNull(runBlocking { fixture.legacyJournal.latest(1) })
        val stylePrompt = "Redraw recognizable target symbols with the shared geometry, optical fill, edges and color roles observed in the contact sheets. Preserve required wordmarks and monograms, simplify complex emblems, and use standalone artwork unless an intentional tile is evidenced. Do not copy example brands or invent a fixed palette."
        fixture.server!!.enqueue(MockResponse().setBody(json("id" to "resp_emulator_montage", "status" to "completed",
            "error" to null, "incomplete_details" to null, "output" to JSONArray().put(json("type" to "message", "status" to "completed",
                "role" to "assistant", "content" to JSONArray().put(json("type" to "output_text", "text" to
                    encodePackStyleText(stylePrompt to "Shared construction with preserved target identity; no invented tile or palette.")))))).toString()))
        click("pack-montage", "pack-analyze")
        assertEquals(0, fixture.server!!.requestCount)
        compose.waitUntil(15000) {
            compose.onAllNodes(hasTestTag("pack-analysis-confirm") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("pack-analysis-confirm").assertIsEnabled().performClick()
        compose.waitUntil(15000) { runBlocking { fixture.repo.load(1).revision?.specification } == stylePrompt }
        val saved = runBlocking { fixture.repo.load(1) }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("pack-summary").fetchSemanticsNodes().isNotEmpty() }
        screenshot("saved-style")
        assertEquals(1, fixture.server!!.requestCount)
        val wire = fixture.server!!.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
        val content = JSONObject(wire.body.readUtf8()).getJSONArray("input").getJSONObject(0).getJSONArray("content")
        assertEquals(montage.pages.size + 1, content.length())
        montage.pages.forEachIndexed { i, page ->
            assertArrayEquals(runBlocking { fixture.repo.referenceBytes(1, page.hash) }, android.util.Base64.decode(
                content.getJSONObject(i + 1).getString("image_url").substringAfter(','), android.util.Base64.NO_WRAP))
        }
        fixture.trialKeyAvailable.set(true)
        click("pack-montage", "pack-continue")
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("setup-selection-saved").fetchSemanticsNodes().isNotEmpty()
        }
        val selectedBeforeBack = runBlocking { fixture.repo.metadata(1)!!.selectionJson }
        click("project-targets", "setup-back")
        compose.waitUntil(15000) {
            if (compose.onAllNodesWithTag("pack-montage").fetchSemanticsNodes().isEmpty()) false else try {
                compose.onNodeWithTag("pack-montage").performScrollToNode(hasTestTag("pack-continue"))
                compose.onAllNodes(hasTestTag("pack-continue") and isEnabled()).fetchSemanticsNodes().size == 1
            } catch (_: AssertionError) { false }
        }
        click("pack-montage", "setup-back")
        compose.onNodeWithTag("workflow-step").assertTextContains("1 of 4", substring = true)
        click("project-targets", "setup-project-style")
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        assertEquals(selectedBeforeBack, runBlocking { fixture.repo.metadata(1)!!.selectionJson })
        assertEquals(1, fixture.server!!.requestCount)
        click("project-targets", "setup-save-selection")
        compose.waitUntil(15000) { fixture.trials.state(1).value.let { !it.busy && it.preparation != null } }
        prepareVisibleTrial()
        val plan = fixture.trials.state(1).value.plan!!
        assertEquals(3, plan.promptVersion); assertEquals(saved.references.map { it.hash }, plan.referenceHashes)
        assertEquals("gpt-image-2.5-sunburst", plan.context.model); assertEquals("high", plan.context.quality)
        assertEquals(stylePrompt, plan.specification)
        assertEquals(runBlocking { currentTrialOriginal(app, target.key) }.hash, plan.inputs.single().originalHash)
        assertTrue(plan.effectivePrompt(plan.inputs.single()).contains("contact-sheet montages"))
        assertEquals(1, fixture.server!!.requestCount)
        compose.onNodeWithTag("trial-decline").performClick()
        assertEquals(0, fixture.gatewayCalls.get()); assertTrue(fixture.trialJournal.records(1).isEmpty())
        prepareVisibleTrial()
        val confirmedPlan = fixture.trials.state(1).value.plan!!
        val bitmap = android.graphics.Bitmap.createBitmap(1024, 1024, android.graphics.Bitmap.Config.ARGB_8888)
        val output = try {
            android.graphics.Canvas(bitmap).drawCircle(512f, 512f, 280f, android.graphics.Paint().apply { color = android.graphics.Color.CYAN })
            java.io.ByteArrayOutputStream().use { bytes -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bytes); bytes.toByteArray() }
        } finally { bitmap.recycle() }
        fixture.server!!.enqueue(MockResponse().setBody(json("data" to JSONArray().put(json("b64_json" to
            android.util.Base64.encodeToString(output, android.util.Base64.NO_WRAP)))).toString()))
        compose.onNodeWithTag("trial-verify-originals").performScrollTo().performClick()
        compose.onNodeWithTag("trial-confirm-paid").assertIsEnabled().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("project-trials-1").fetchSemanticsNodes().isNotEmpty() &&
                fixture.trials.state(1).value.let { !it.busy && it.variants.size == 1 }
        }
        val generated = fixture.trials.state(1).value.variants.single()
        compose.onNodeWithTag("trial-count").assertDoesNotExist()
        compose.onNodeWithTag("trial-target-${target.key}").assertDoesNotExist()
        val imageWire = fixture.server!!.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
        assertEquals("/v1/images/edits", imageWire.path)
        val imageBody = String(imageWire.body.readByteArray(), Charsets.ISO_8859_1)
        fun field(name: String) = imageBody.substringAfter("name=\"$name\"").substringAfter("\r\n\r\n").substringBefore("\r\n--")
        assertEquals("gpt-image-2.5-sunburst", field("model")); assertEquals("high", field("quality"))
        assertEquals(confirmedPlan.effectivePrompt(confirmedPlan.inputs.single()), field("prompt"))
        val original = runBlocking { currentTrialOriginal(app, target.key) }
        var position = -1
        (listOf(original.png) + saved.references.map { runBlocking { fixture.repo.referenceBytes(1, it.hash) } }).forEach { bytes ->
            val at = imageBody.indexOf(String(bytes, Charsets.ISO_8859_1)); assertTrue(at > position); position = at
        }
        assertTrue(imageBody.contains(confirmedPlan.effectivePrompt(confirmedPlan.inputs.single())))
        click("project-trials-1", "trial-accept-${generated.id}")
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("project-trials-1").fetchSemanticsNodes().isNotEmpty() &&
                fixture.trials.state(1).value.let { !it.busy && it.variants.single().reviewStatus == "ACCEPTED" }
        }
        click("project-trials-1", "trial-approve-style")
        compose.waitUntil(15000) {
            try {
                compose.onNodeWithTag("project-trials-1").performScrollToNode(hasTestTag("trial-ready"))
                compose.onAllNodesWithTag("trial-ready").fetchSemanticsNodes().size == 1
            } catch (_: AssertionError) { false }
        }
        compose.onNodeWithTag("trial-ready").assertIsDisplayed()
        screenshot("trial-review")
        assertEquals(2, fixture.server!!.requestCount); assertEquals(1, fixture.gatewayCalls.get())
        assertEquals(1, fixture.trialJournal.records(1).size)
        evidence("montage", json("loopback_posts" to 2, "external_posts" to 0, "selected_icons" to montage.members.size,
            "montage_pages" to montage.pages.size, "trial_prepared" to true, "prompt_version" to 3,
            "saved_revision" to saved.revision!!.revision, "generation_calls" to 1, "automatic_recomposition" to true,
            "variant_reviewed" to true, "style_approved" to true))
    }
    private fun screenshot(stage: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try { File(app.filesDir, "offline-$stage.png").outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
    }

    private fun referenceJourney(name: String, decline: Boolean) {
        open(name)
        click("project-style-1", "project-style-sample-pack")
        compose.waitUntil(15000) {
            compose.onAllNodes(hasTestTag("project-style-sample-pack") and isEnabled()).fetchSemanticsNodes().size == 1
        }
        click("project-style-1", "project-style-select-candidate-0")
        click("project-style-1", "project-style-select-candidate-1")
        click("project-style-1", "project-style-up-1")
        click("project-style-1", "project-style-save")
        // Save's IO transaction outlives Compose idleness. The parent route is
        // entered only after the durable Save callback; observe that boundary.
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("project-targets").fetchSemanticsNodes().isNotEmpty()
        }
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
        if (decline) {
            val beforeDecline = runBlocking { fixture.repo.load(1) }
            compose.onNodeWithTag("trial-decline").assertIsEnabled().performClick()
            assertNull(fixture.trials.state(1).value.plan)
            assertNull(runBlocking { fixture.trials.preview(1, original.hash) })
            fixture.trials.load(1, ProjectSetupMode.EXTEND, pack)
            compose.waitUntil(15000) { !fixture.trials.state(1).value.busy }
            assertNull(fixture.trials.state(1).value.plan)
            assertEquals(beforeDecline, runBlocking { fixture.repo.load(1) })
            assertTrue(runBlocking { fixture.trialJournal.records(1) }.isEmpty())
        }
        evidence(name, json("loopback_posts" to 0, "external_posts" to 0, "selected_references" to 2,
            "trial_prepared" to true, "saved_revision" to saved.revision!!.revision,
            "declined" to decline, "generation_calls" to fixture.gatewayCalls.get()))
    }

    @Test fun restartReadOnly() {
        val previous = JSONObject(File(app.filesDir, "offline-dna-evidence.json").readText())
        for (name in listOf("references", "decline", "montage")) {
            assertTrue(previous.has(name))
            assertNotEquals("Runner must terminate the earlier application process", android.os.Process.myPid(),
                previous.getJSONObject(name).getInt("process_id"))
        }
        open("references", create = false)
        val saved = runBlocking { fixture.repo.load(1) }
        assertEquals(previous.getJSONObject("references").getLong("saved_revision"), saved.revision!!.revision)
        assertEquals(2, saved.references.size)
        assertNull(saved.revision!!.analysisProfileJson)
        click("project-style-1", "style-trials")
        fixture.trialKeyAvailable.set(true)
        click("project-targets", "setup-save-selection")
        compose.waitUntil(15000) { fixture.trials.state(1).value.let { !it.busy && it.preparation != null } }
        prepareVisibleTrial()
        val plan = checkNotNull(fixture.trials.state(1).value.plan)
        assertEquals(saved.references.map { it.hash }, plan.referenceHashes)
        assertEquals(runBlocking { currentTrialOriginal(app, target.key) }.hash, plan.inputs.single().originalHash)
        fixture.assertPosts(0)
        Fixture("decline").use { other -> runBlocking {
            other.initialize(false)
            assertEquals(previous.getJSONObject("decline").getLong("saved_revision"), other.repo.load(1).revision!!.revision)
            assertEquals(2, other.repo.load(1).references.size)
            assertTrue(other.trialJournal.records(1).isEmpty())
            assertTrue(other.dnaJournal.records(1).isEmpty())
            other.assertPosts(0)
        } }
        Fixture("montage").use { other ->
            val restored = runBlocking {
                other.initialize(false)
                other.repo.load(1)
            }
            val montage = PackMontage.optional(restored.revision!!.analysisProfileJson)!!
            assertEquals(previous.getJSONObject("montage").getLong("saved_revision"), restored.revision!!.revision)
            assertNotNull(montage.result); assertEquals(montage.pages.map { it.hash }, restored.references.map { it.hash })
            montage.pages.forEach { page -> assertEquals(page.hash, sha256(runBlocking { other.repo.referenceBytes(1, page.hash) })) }
            other.trialKeyAvailable.set(true)
            other.trials.load(1, ProjectSetupMode.EXTEND, pack)
            compose.waitUntil(15000) { other.trials.state(1).value.let { !it.busy && it.preparation != null } }
            other.trials.prepare(1, listOf(target.key), false)
            compose.waitUntil(15000) { other.trials.state(1).value.let { !it.busy && it.plan != null } }
            assertEquals(3, other.trials.state(1).value.plan!!.promptVersion)
            assertEquals(0, other.server!!.requestCount); assertEquals(0, other.gatewayCalls.get())
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
}
