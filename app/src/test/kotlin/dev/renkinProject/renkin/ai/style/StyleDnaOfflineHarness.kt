package dev.renkinProject.renkin.ai.style

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.CoverageTarget
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.*
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.TimeUnit

private val offlineArtwork by lazy { (0 until 50).associate { "draw$it" to analysisFixturePng(2, it) } }

/** Reusable real HTTP/controller/disk-Room fixture. Only this test source set owns
 * routing. Requests retain the release URL until the first application interceptor;
 * a deny-by-default DNS and network interceptor independently forbid egress. */
internal class StyleDnaOfflineHarness(root: File, timeoutMillis: Long = 5000) : AutoCloseable {
    companion object { const val TOKEN = "offline-synthetic-token-never-a-paid-key" }
    val databaseFile = File(root, "database/renkin.db").apply { parentFile!!.mkdirs() }
    val context: Context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
        override fun getFilesDir() = File(root, "files").apply { mkdirs() }
        override fun getNoBackupFilesDir() = File(root, "noBackup").apply { mkdirs() }
        override fun getDatabasePath(name: String) = databaseFile.also { check(name == it.name) }
        override fun getApplicationContext(): Context = this
    }
    val server = MockWebServer().apply { start(InetAddress.getByName("127.0.0.1"), 0) }
    val reader = FixtureReferenceReader(offlineArtwork)
    val targets = listOf(CoverageTarget("fixture.target/.Main", "Synthetic target"))
    val original = TrialOriginal(projectTestPng(size = 1024), "launcher:fixture.target/.Main:fixture-v1")
    var activeProfile = 1L
    var keyPresent = true
    var catalog = projectCatalogStamp(targets)
    var beforeNetwork: (Request) -> Unit = {}
    val logicalDestinations = Collections.synchronizedList(mutableListOf<HttpUrl>())
    val networkDestinations = Collections.synchronizedList(mutableListOf<HttpUrl>())
    val connectedAddresses = Collections.synchronizedList(mutableListOf<InetSocketAddress>())
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    var database = openDatabase()
        private set
    var repository = ProjectAiRepository(database, ProjectAssetStore(context))
        private set
    var journal = StyleDnaJournal(context)
        private set
    private var legacyJournal = StyleAnalysisJournal(context)
    val client = OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
        .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (hostname != "127.0.0.1") throw UnknownHostException("offline fixture forbids external DNS")
                return listOf(InetAddress.getByName("127.0.0.1"))
            }
        })
        .addInterceptor { chain ->
            val request = chain.request()
            check(request.url.scheme == "https" && request.url.host == "api.openai.com")
            check(request.url.encodedPath in setOf("/v1/responses", "/v1/images/edits"))
            check(request.method == "POST" && request.header("Authorization") == "Bearer $TOKEN")
            logicalDestinations += request.url
            beforeNetwork(request)
            chain.proceed(request.newBuilder().url(server.url(request.url.encodedPath)).build())
        }
        .addNetworkInterceptor { chain ->
            val url = chain.request().url
            check(url.host == "127.0.0.1" && url.port == server.port && url.scheme == "http")
            networkDestinations += url
            chain.proceed(chain.request())
        }
        .eventListener(object : EventListener() {
            override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
                check(inetSocketAddress.address.isLoopbackAddress && proxy.type() == Proxy.Type.DIRECT)
                connectedAddresses += inetSocketAddress
            }
        }).build()
    val gateway = OpenAiStyleDnaGateway({ TOKEN }, client, "https://api.openai.com/v1/responses".toHttpUrl())
    var controller = newController()
        private set
    val draft = ProjectStyleDraft("bounded drawing rules", "preserve the actual target identity", emptyList())

    private fun openDatabase() = Room.databaseBuilder(context, RenkinPackDatabase::class.java, databaseFile.name).build()
    private fun newController() = OwnedStyleAnalysis(scope, repository, legacyJournal, ReferenceSampler(),
        StyleAnalysisCoordinator(legacyJournal, StyleAnalysisGateway { error("Legacy must not be submitted") }),
        { keyPresent }, object : AnalysisSessionOwner {
            override suspend fun <T> owned(id: Long, operation: suspend () -> T): T {
                check(id == activeProfile); return operation()
            }
            override suspend fun catalogStamp() = catalog
            override suspend fun reader(source: String, referenceIndex: Boolean) = reader
        }, StyleDnaCoordinator(journal, gateway))

    suspend fun initialize() {
        RenkinPackRepository(database).replaceEverything(listOf(Profile(1, "Offline project"), Profile(2, "Untouched project")), emptyList())
        repository.saveStyle(1, null, "prior manual style", "prior instruction", emptyList(), "EXTEND", "fixture.pack", projectSourceStamp(reader.index))
        repository.saveStyle(2, null, "other project's immutable style", "", emptyList())
        val metadata = checkNotNull(repository.metadata(1))
        repository.saveSelection(1, metadata.version, ProjectSelection("EXTEND", "fixture.pack", projectSourceStamp(reader.index), catalog, targets.map { it.key }))
    }

    suspend fun idle(): ProjectAnalysisState = withTimeout(10000) { controller.state(1).first { !it.busy } }
    suspend fun prepare(mode: DnaAnalysisMode): StyleDnaPlan {
        controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index),
            repository.load(1).revision?.revision, draft, mode, DEFAULT_ANALYSIS_MODEL, "low")
        val state = idle()
        assertNull("Local preparation: ${state.dnaLocalDiagnostic}", state.error)
        return checkNotNull(state.dnaPlan)
    }
    fun images(plan: StyleDnaPlan) = checkNotNull(controller.state(1).value.samples).samples
        .filter { image -> plan.samples.any { it.hash == image.hash } }.associate { it.hash to it.png }
    fun call(plan: StyleDnaPlan, step: DnaStep, images: Map<String, ByteArray>): DnaCall = DnaCall(plan, step,
        step.samples.associate { plan.bindings.getValue(it) to images.getValue(plan.bindings.getValue(it)) },
        if (step.synthesis) dnaFixtureReports(plan) else emptyList())
    fun completed(plan: StyleDnaPlan, step: DnaStep, images: Map<String, ByteArray>, verbose: Boolean = false): MockResponse {
        val reports = verboseDnaReports(plan, verbose)
        val text = if (step.synthesis) synthesizeDnaLocally(reports, images).encode()
            else dnaReportsBody(reports.filter { it.sampleId in step.samples })
        return MockResponse().setBody(dnaOfflineEnvelope(call(plan, step, images), text))
    }
    suspend fun submit(plan: StyleDnaPlan): StyleDnaRecord {
        controller.submitDna(1, plan.id)
        val state = idle()
        assertNull(state.error)
        return checkNotNull(state.dnaRecord)
    }
    suspend fun save(record: StyleDnaRecord): SavedProjectStyle {
        val result = checkNotNull(record.result)
        val references = dnaReferenceHashes(result, DNA_PACK_STYLE, record.plan.samples.map { it.hash })
        return controller.saveDna(1, record.plan.id, result.dna.encode(), DNA_PACK_STYLE, references,
            repository.load(1).revision?.revision, draft)
    }
    /** A new repository, SQLite connection, controller and journal reader; no retained VM state. */
    suspend fun reopen() {
        scope.coroutineContext.job.cancelAndJoin()
        database.close()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        database = openDatabase()
        repository = ProjectAiRepository(database, ProjectAssetStore(context))
        journal = StyleDnaJournal(context)
        legacyJournal = StyleAnalysisJournal(context)
        controller = newController()
        controller.bindContext(1, ProjectSetupMode.EXTEND, "fixture.pack")
        controller.refresh(1)
        assertNull(controller.state(1).value.dnaPlan)
    }
    fun trialController(): OwnedStyleTrials {
        val trialJournal = StyleTrialJournal(context)
        val imageGateway = ClientIconGenerationGateway({ TOKEN },
            OpenAiImageClient(client, "https://api.openai.com/v1/images/edits".toHttpUrl()),
            StabilityImageClient(client, server.url("/never-stability"), server.url("/never-background")))
        return OwnedStyleTrials(scope, repository, trialJournal, StyleTrialCoordinator(trialJournal, imageGateway, repository),
            { keyPresent }, object : TrialSessionOwner {
                override suspend fun <T> owned(id: Long, action: suspend () -> T): T { check(id == activeProfile); return action() }
                override suspend fun targets() = targets
                override suspend fun reader(source: String) = reader
                override suspend fun original(key: String) = original.also { check(key == targets.single().key) }
                override suspend fun settings() = AiSettings()
            })
    }
    suspend fun trialIdle(trials: OwnedStyleTrials) = withTimeout(10000) { trials.state(1).first { !it.busy } }
    fun takeRequest(): RecordedRequest = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
    fun assertOffline(expectedPosts: Int) {
        assertEquals(expectedPosts, server.requestCount)
        assertEquals(expectedPosts, logicalDestinations.size)
        assertEquals(expectedPosts, networkDestinations.size)
        assertTrue(logicalDestinations.all { it.host == "api.openai.com" })
        assertTrue(networkDestinations.all { it.host == "127.0.0.1" && it.port == server.port })
        assertTrue(connectedAddresses.isNotEmpty())
        assertTrue(connectedAddresses.all { it.address.isLoopbackAddress })
    }
    override fun close() {
        scope.cancel(); database.close(); client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown()
    }
}

internal fun verboseDnaReports(plan: StyleDnaPlan, verbose: Boolean): List<DnaIconReport> = dnaFixtureReports(plan).map { report ->
    if (!verbose) report else report.copy(traits = report.traits.mapValues { (trait, observation) ->
        // Exactly the production 240-character field bound. These are artificial
        // observations, not a claim about model token counts or visual quality.
        observation.copy(value = ("Observed $trait; bounded planar construction and controlled transparent spacing. ".repeat(5)).take(240))
    })
}

/** Full official-shaped metadata plus reasoning and message blocks, with split text
 * to exercise the real concatenation path. Never captured private provider output. */
internal fun dnaOfflineEnvelope(call: DnaCall, text: String, status: String = "completed", reason: Any? = null): String {
    val root = JSONObject(dnaFullResponseFixture(call, text))
    root.put("status", status).put("incomplete_details", if (status == "incomplete") json("reason" to reason) else JSONObject.NULL)
    root.put("usage", json("input_tokens" to 1800, "output_tokens" to if (status == "incomplete") 12288 else 4096,
        "total_tokens" to if (status == "incomplete") 14088 else 5896,
        "input_tokens_details" to json("cached_tokens" to 0), "output_tokens_details" to json("reasoning_tokens" to 0)))
    val message = root.getJSONArray("output").getJSONObject(1)
    message.put("status", status)
    val content = message.getJSONArray("content")
    val cut = text.length / 2
    content.getJSONObject(0).put("text", text.take(cut))
    content.put(json("type" to "output_text", "text" to text.drop(cut), "annotations" to org.json.JSONArray(), "logprobs" to org.json.JSONArray()))
    return root.toString()
}
