package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import androidx.room.Room
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
}
