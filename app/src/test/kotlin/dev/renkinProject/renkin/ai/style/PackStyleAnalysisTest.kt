package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import androidx.room.Room
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.project.*
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.CoverageTarget
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PackStyleAnalysisTest {
    @get:Rule val temp = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var db: RenkinPackDatabase
    private lateinit var repo: ProjectAiRepository
    private lateinit var journal: StyleAnalysisJournal
    private lateinit var local: LocalPackMontages
    private lateinit var draft: MontageDraft
    private lateinit var reader: FixtureReferenceReader
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var gateway: PackStyleGateway
    private var available = true
    private val mutex = Mutex()
    private val owner = object : AnalysisSessionOwner {
        override suspend fun <T> owned(id: Long, operation: suspend () -> T): T = mutex.withLock { check(available && id == 1L); operation() }
        override suspend fun catalogStamp(): String = error("Pack analysis does not depend on target inventory")
        override suspend fun reader(source: String, referenceIndex: Boolean) = reader.also { check(source == "fixture.pack") }
    }
    private fun service(call: suspend (StyleAnalysisRequest) -> StyleAnalysisOutcome = gateway::analyze) =
        PackStyleAnalysis(repo, local, StyleAnalysisJournal(context), call, { true }, owner)
    private val prompt = "Redraw each recognizable target as compact geometric artwork. Preserve essential wordmarks or monograms, simplify complex emblems, balance optical fill and negative space, and follow the observed variable brand-accent roles. Use a separate tile only where supported; dark standalone forms do not establish a tile."
    private fun response() = MockResponse().setBody(json("id" to "resp_montage_fixture", "status" to "completed",
        "error" to null, "incomplete_details" to null, "usage" to json("input_tokens" to 3200, "output_tokens" to 160),
        "output" to JSONArray().put(json("type" to "message", "status" to "completed", "role" to "assistant",
            "content" to JSONArray().put(json("type" to "output_text", "text" to encodePackStyleText(prompt to "Compact geometry with identity-preserving typography and mixed brand accents.")))))).toString())
    @Before fun setup() = runBlocking<Unit> {
        context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getFilesDir() = File(temp.root, "files").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(temp.root, "noBackup").apply { mkdirs() }
        }
        db = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).build()
        repo = ProjectAiRepository(db, ProjectAssetStore(context)); journal = StyleAnalysisJournal(context)
        RenkinPackRepository(db).replaceEverything(listOf(Profile(1, "Pack")), emptyList())
        reader = FixtureReferenceReader((0 until 36).associate { "art$it" to analysisFixturePng(it % 3, it) })
        local = LocalPackMontages(repo, ReferenceSampler(), owner)
        draft = local.prepare(1, "fixture.pack", projectSourceStamp(reader.index))
        local.save(1, null, draft)
        server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 0) }
        client = OkHttpClient.Builder().proxy(Proxy.NO_PROXY).retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false).addNetworkInterceptor { chain ->
                check(chain.request().url.host == "127.0.0.1" && chain.request().url.port == server.port)
                check(chain.connection()!!.route().socketAddress.address.isLoopbackAddress)
                val record = checkNotNull(journal.latest(1))
                assertEquals(AnalysisStatus.REQUESTING, record.status)
                assertEquals("UNKNOWN", record.billingCertainty)
                chain.proceed(chain.request())
            }.build()
        gateway = PackStyleGateway({ "offline-montage-not-a-real-key" }, client,
            server.url("/v1/responses").newBuilder().host("127.0.0.1").build())
    }
    @After fun close() {
        client.connectionPool.evictAll(); client.dispatcher.executorService.shutdownNow(); server.shutdown(); db.close()
    }
    /** Inspect actual multipart bytes, never request headers, credentials or image contents.
     * Test-only: the exported prompt contains synthetic fixture text, never owner data. */
    private fun imageEditDiagnostic(wire: RecordedRequest): JSONObject {
        val boundary = checkNotNull(wire.getHeader("Content-Type")).substringAfter("boundary=")
        val payload = String(wire.body.clone().readByteArray(), Charsets.ISO_8859_1)
        val fields = JSONObject()
        val images = JSONArray()
        payload.split("--$boundary").filter { it.startsWith("\r\nContent-Disposition:") }.forEach { part ->
            val headers = part.substringBefore("\r\n\r\n")
            val name = Regex("name=\"([^\"]+)\"").find(headers)!!.groupValues[1]
            val bytes = part.substringAfter("\r\n\r\n").removeSuffix("\r\n").toByteArray(Charsets.ISO_8859_1)
            if (name == "image[]") {
                val dimensions = inspectPng(bytes)
                images.put(json("position" to images.length() + 1,
                    "role" to if (images.length() == 0) "target_original_identity" else "pack_style_contact_sheet",
                    "sha256" to sha256(bytes), "bytes" to bytes.size,
                    "width" to dimensions.first, "height" to dimensions.second))
            } else {
                require(name in setOf("model", "prompt", "n", "size", "quality", "background", "output_format"))
                require(!fields.has(name)) { "Duplicate request field" }
                fields.put(name, String(bytes, Charsets.UTF_8))
            }
        }
        return json("endpoint" to "/v1/images/edits", "fields" to fields, "images" to images,
            "input_fidelity" to "provider default; not sent", "system_developer_messages" to "none; Image API multipart")
    }
    @Test fun oneActualMultimodalPostUsesSavedSheetsAfterConsentAndPersistsAReusableBrief() = runBlocking<Unit> {
        val analysis = service(); val request = analysis.prepare(1, draft, "Keep essential monograms")
        assertNull(journal.latest(1)); assertEquals(0, server.requestCount)
        assertNull(analysis.submit(request, draft, false)); assertNull(journal.latest(1)); assertEquals(0, server.requestCount)
        server.enqueue(response())
        val saved = checkNotNull(analysis.submit(request, draft, true))
        val wire = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("POST", wire.method); assertEquals("/v1/responses", wire.path)
        assertEquals(request.requestId, wire.getHeader("X-Client-Request-Id"))
        val bodyText = wire.body.readUtf8(); val body = JSONObject(bodyText)
        assertEquals(DEFAULT_ANALYSIS_MODEL, body.getString("model")); assertEquals(4096, body.getInt("max_output_tokens"))
        assertFalse(body.getBoolean("store")); assertFalse(body.getBoolean("stream"))
        assertEquals("none", body.getJSONObject("reasoning").getString("effort"))
        assertTrue(body.getString("instructions").contains("contact-sheet montages"))
        assertTrue(body.getString("instructions").contains("wordmarks, monograms and complex emblems"))
        val content = body.getJSONArray("input").getJSONObject(0).getJSONArray("content")
        assertTrue(content.getJSONObject(0).getString("text").contains("Keep essential monograms"))
        assertEquals(draft.images.size + 1, content.length())
        draft.images.forEachIndexed { index, bytes ->
            val image = content.getJSONObject(index + 1)
            assertEquals("input_image", image.getString("type")); assertEquals("high", image.getString("detail"))
            assertArrayEquals(bytes, Base64.decode(image.getString("image_url").substringAfter(','), Base64.NO_WRAP))
        }
        assertFalse(bodyText.contains("offline-montage-not-a-real-key")); assertFalse(bodyText.contains("fixture.pack"))
        assertEquals(prompt, saved.revision!!.specification)
        assertEquals(draft.montage.pages.map { it.hash }, saved.references.map { it.hash })
        assertEquals(prompt, PackMontage.optional(saved.revision!!.analysisProfileJson)!!.result!!.prompt)
        assertEquals(AnalysisStatus.REVIEWED, journal.latest(1)!!.status)
        assertEquals(journal.latest(1), AnalysisRecord.parse(journal.latest(1)!!.encode()))
        assertEquals(saved, repo.load(1)); assertEquals(1, server.requestCount)
        assertTrue(runCatching { service().submit(request, draft, true) }.isFailure)
        assertEquals(1, server.requestCount)
    }
    @Test fun incompleteAndServerFailuresNeverSaveOrReplayEvenAfterReconstruction() = runBlocking<Unit> {
        for ((reply, reason) in listOf(MockResponse().setBody(json("status" to "incomplete", "output" to JSONArray()).toString()) to "Incomplete",
            MockResponse().setResponseCode(503).setBody("private provider text") to "Unavailable")) {
            val analysis = service(); val request = analysis.prepare(1, draft, "")
            val before = repo.snapshot(); val count = server.requestCount
            server.enqueue(reply); assertNull(analysis.submit(request, draft, true))
            assertEquals(reason, journal.latest(1)!!.reason); assertEquals(before, repo.snapshot())
            assertNull(service().submit(request, draft, true)); assertEquals(count + 1, server.requestCount)
            assertNull(PackMontage.optional(repo.load(1).revision!!.analysisProfileJson)!!.result)
        }
    }
    @Test fun cancellationAndStaleSourceKeepConsumedConsentWithoutAutomaticRetry() = runBlocking<Unit> {
        val analysis = service(); val request = analysis.prepare(1, draft, "")
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val job = launch(Dispatchers.Default) { analysis.submit(request, draft, true) }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
        job.cancelAndJoin()
        assertEquals(AnalysisStatus.UNCERTAIN, journal.latest(1)!!.status)
        assertNull(service().submit(request, draft, true)); assertEquals(1, server.requestCount)
        val fresh = analysis.prepare(1, draft, "")
        reader.current = false
        assertTrue(runCatching { analysis.submit(fresh, draft, true) }.isFailure)
        assertEquals(1, server.requestCount)
        assertEquals(request.requestId, journal.latest(1)!!.requestId)
    }
    @Test fun receivedResultCanRecoverOnlyItsLocalSaveAfterOwnerReturnsWithoutAnotherPost() = runBlocking<Unit> {
        val analysis = service { request -> gateway.analyze(request).also { available = false } }
        val request = analysis.prepare(1, draft, "")
        server.enqueue(response())
        assertTrue(runCatching { analysis.submit(request, draft, true) }.isFailure)
        assertEquals(AnalysisStatus.RESULT_SAVED, journal.latest(1)!!.status)
        assertEquals(1L, repo.load(1).revision!!.revision)
        available = true
        val saved = checkNotNull(service().recover(1, draft))
        assertEquals(prompt, saved.revision!!.specification); assertEquals(1, server.requestCount)
        assertEquals(AnalysisStatus.REVIEWED, journal.latest(1)!!.status)
        assertNull(service().recover(1, draft)); assertEquals(1, server.requestCount)
    }
    @Test fun savedMontageAnalysisReachesFrozenTrialAndActualMultipartOnlyAfterSeparateConsent() = runBlocking<Unit> {
        val analysis = service(); val request = analysis.prepare(1, draft, "")
        server.enqueue(response()); analysis.submit(request, draft, true)
        server.takeRequest(5, TimeUnit.SECONDS)
        val target = CoverageTarget("target.app/Main", "Target application")
        val original = TrialOriginal(projectTestPng(android.graphics.Color.MAGENTA, 1024), "target-component-resource:1")
        val saved = repo.load(1)
        repo.saveSelection(1, saved.metadata!!.version, ProjectSelection("EXTEND", "fixture.pack", projectSourceStamp(reader.index),
            projectCatalogStamp(listOf(target)), listOf(target.key)))
        val trialsJournal = StyleTrialJournal(context)
        val imageClient = client.newBuilder().apply { networkInterceptors().clear() }.addNetworkInterceptor { chain ->
            check(chain.request().url.host == "127.0.0.1" && chain.request().url.port == server.port)
            check(chain.connection()!!.route().socketAddress.address.isLoopbackAddress)
            val item = trialsJournal.records(1).single().items.single()
            assertEquals(TrialStatus.REQUESTING, item.status); assertEquals(1, item.endpointCalls)
            chain.proceed(chain.request())
        }.build()
        val endpoint = server.url("/v1/images/edits").newBuilder().host("127.0.0.1").build()
        assertEquals("127.0.0.1", endpoint.host)
        val native = ClientIconGenerationGateway({ "offline-image-not-a-key" }, OpenAiImageClient(imageClient, endpoint),
            StabilityImageClient(imageClient, endpoint, endpoint))
        val trialOwner = object : TrialSessionOwner {
            override suspend fun <T> owned(id: Long, action: suspend () -> T) = mutex.withLock { check(id == 1L && available); action() }
            override suspend fun targets() = listOf(target)
            override suspend fun reader(source: String) = reader
            override suspend fun original(key: String) = original.also { check(key == target.key) }
            override suspend fun settings() = AiSettings()
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            fun controller() = OwnedStyleTrials(scope, repo, trialsJournal, StyleTrialCoordinator(trialsJournal, native, repo), { true }, trialOwner)
            suspend fun idle(c: OwnedStyleTrials) = withTimeout(10000) { c.state(1).first { !it.busy } }.also { assertNull(it.error) }
            val c = controller()
            c.load(1, ProjectSetupMode.EXTEND, "fixture.pack"); idle(c)
            c.prepare(1, listOf(target.key), false)
            val declined = checkNotNull(idle(c).plan)
            assertEquals(3, declined.promptVersion); assertEquals(draft.images.map(::sha256), declined.referenceHashes)
            assertEquals(original.hash, declined.inputs.single().originalHash)
            assertEquals(1, server.requestCount); assertTrue(trialsJournal.records(1).isEmpty())
            c.cancelPreparation(1); assertNull(c.state(1).value.plan)
            c.load(1, ProjectSetupMode.EXTEND, "fixture.pack"); assertNull(idle(c).plan)
            c.prepare(1, listOf(target.key), false); val plan = checkNotNull(idle(c).plan)
            assertNotEquals(declined.id, plan.id)
            val output = projectTestPng(size = 1024)
            server.enqueue(MockResponse().setBody(json("data" to JSONArray().put(json("b64_json" to Base64.encodeToString(output, Base64.NO_WRAP)))).toString()))
            c.submit(1, plan.id); assertEquals(1, idle(c).variants.size)
            val wire = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS)); assertEquals("/v1/images/edits", wire.path)
            val diagnostic = imageEditDiagnostic(wire)
            File("build/montage-evidence/v3-image-edit-request.json").also {
                checkNotNull(it.parentFile).mkdirs(); it.writeText(diagnostic.toString(2))
            }
            val payload = String(wire.body.readByteArray(), Charsets.ISO_8859_1)
            val pngs = listOf(original.png) + draft.images
            var last = -1
            pngs.forEachIndexed { index, png ->
                val at = payload.indexOf(String(png, Charsets.ISO_8859_1)); assertTrue(at > last); last = at
                assertTrue(payload.contains("filename=\"input_$index.png\""))
            }
            assertEquals(pngs.size, Regex("filename=\"input_").findAll(payload).count())
            val effective = plan.effectivePrompt(plan.inputs.single())
            val fields = diagnostic.getJSONObject("fields")
            assertEquals(effective, fields.getString("prompt"))
            assertEquals(plan.promptHashes.getValue(target.key), sha256(fields.getString("prompt").toByteArray(Charsets.UTF_8)))
            assertEquals(1, Regex("ICON GENERATION CONTRACT V3").findAll(effective).count())
            assertTrue(effective.contains("contact-sheet montages containing multiple independent icons"))
            assertTrue(effective.contains("Do not render the montage, a grid, multiple icons, or copy the example logos"))
            assertTrue(effective.contains("Redraw the target geometry"))
            assertTrue(effective.contains("Dark silhouettes, image bounds and launcher preview backgrounds do not establish a tile"))
            for (legacy in listOf(AiSettings.DEFAULT_STYLE, "STYLE SPECIFICATION:", "TARGET / EDIT / OUTPUT CONTRACT",
                "ICON GENERATION CONTRACT V2", "Additional target drawing guidance:", "Reviewed Style DNA family"))
                assertFalse(legacy, effective.contains(legacy))
            assertEquals(1, Regex(Regex.escape(prompt)).findAll(effective).count())
            for ((name, value) in listOf("n" to "1", "size" to "1024x1024", "background" to "transparent", "output_format" to "png"))
                assertEquals(value, fields.getString(name))
            val diagnosticImages = diagnostic.getJSONArray("images")
            assertEquals(pngs.size, diagnosticImages.length())
            pngs.forEachIndexed { index, bytes ->
                assertEquals(sha256(bytes), diagnosticImages.getJSONObject(index).getString("sha256"))
                assertEquals(if (index == 0) "target_original_identity" else "pack_style_contact_sheet",
                    diagnosticImages.getJSONObject(index).getString("role"))
            }
            assertFalse(diagnostic.toString().contains("offline-image-not-a-key"))
            assertEquals("gpt-image-2.5-sunburst", fields.getString("model"))
            assertEquals("high", fields.getString("quality"))
            assertEquals(plan.context.model, fields.getString("model")); assertEquals(plan.context.quality, fields.getString("quality"))
            // Changing future defaults cannot reinterpret an already frozen V3 request.
            val historical = plan.copy(context = plan.context.copy(model = "gpt-image-2.5-flare", quality = "medium"))
            val encoded = historical.encode()
            assertEquals(encoded, StyleTrialPlan.parse(encoded).encode())
            val oldBody = buildImageEditRequest("synthetic-only", AiSettings(model = historical.context.model,
                quality = historical.context.quality, referenceNames = draft.images.indices.map { "img_0_$it.png" }),
                pngs, historical.appearance(historical.inputs.single()), historical.promptVersion).body as MultipartBody
            fun oldField(name: String) = okio.Buffer().also { buffer ->
                oldBody.parts.single { it.headers?.get("Content-Disposition") == "form-data; name=\"$name\"" }.body.writeTo(buffer)
            }.readUtf8()
            assertEquals("gpt-image-2.5-flare", oldField("model")); assertEquals("medium", oldField("quality"))
            assertEquals(effective, oldField("prompt"))
            val restored = controller(); restored.load(1, ProjectSetupMode.EXTEND, "fixture.pack"); idle(restored)
            assertNull(restored.state(1).value.plan)
            StyleTrialCoordinator(StyleTrialJournal(context), native, repo).submit(plan, pngs.associateBy(::sha256), true, { true })
            assertEquals(2, server.requestCount)
            // Existing explicit options remain available; only the montage default changes.
            restored.options(1, AiSettings.OPENAI, "gpt-image-1-mini", "low"); idle(restored)
            restored.prepare(1, listOf(target.key), false)
            val explicit = checkNotNull(idle(restored).plan)
            assertEquals("gpt-image-1-mini", explicit.context.model); assertEquals("low", explicit.context.quality)
            restored.cancelPreparation(1); assertEquals(2, server.requestCount)
            reader.current = false
            restored.load(1, ProjectSetupMode.EXTEND, "fixture.pack")
            withTimeout(10000) { restored.state(1).first { !it.busy } }
            assertNull(restored.state(1).value.preparation)
        } finally { scope.cancel(); imageClient.connectionPool.evictAll(); imageClient.dispatcher.executorService.shutdownNow() }
    }
}
