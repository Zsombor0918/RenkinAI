package dev.renkinProject.renkin.ui.project

import android.app.Application
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OwnedStyleTrialsTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var f: StyleTrialFixture
    private lateinit var scope: CoroutineScope
    private var active = 1L
    private var key = true
    private var calls = 0
    private lateinit var gateway: IconGenerationGateway
    private val mutex = Mutex()
    private val owner = object : TrialSessionOwner {
        override suspend fun <T> owned(id: Long, action: suspend () -> T): T = mutex.withLock { check(id == active); action() }
        override suspend fun targets() = f.targets
        override suspend fun reader(source: String) = f.reader
        override suspend fun original(key: String) = f.originals.getValue(key)
        override suspend fun settings() = AiSettings()
    }
    private fun controller() = OwnedStyleTrials(scope, f.repo, f.journal, StyleTrialCoordinator(f.journal, gateway, f.repo), { key }, owner)
    @Before fun setup() = runBlocking {
        f = StyleTrialFixture(temporary.root); f.initialize()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default); gateway = f.successful { calls++ }
    }
    @After fun close() { scope.cancel(); f.close() }
    private suspend fun idle(c: OwnedStyleTrials): ProjectTrialState = withTimeout(10000) { c.state(1).first { !it.busy } }
    private suspend fun loaded(c: OwnedStyleTrials, extend: Boolean = false): ProjectTrialState {
        c.load(1, if (extend) ProjectSetupMode.EXTEND else ProjectSetupMode.CREATE, if (extend) "source.pack" else null)
        return idle(c).also { assertNull(it.error) }
    }
    private suspend fun prepared(c: OwnedStyleTrials, keys: List<String> = listOf("a/Main")): StyleTrialPlan {
        loaded(c); c.prepare(1, keys, false); return checkNotNull(idle(c).also { assertNull(it.error) }.plan)
    }
    private suspend fun generated(c: OwnedStyleTrials): StyleTrialPlan {
        val plan = prepared(c); c.submit(1, plan.id); idle(c).also { assertNull(it.error) }; return plan
    }
    @Test fun resourceOriginalVerificationIsPlanSpecificAndChangedEvidencePreventsAPost() = runBlocking {
        val value = f.originals.getValue("a/Main")
        val evidence = OriginalIconEvidence("APP_COMPONENT_RESOURCE", "adaptive", 0, 420, 123, true)
        f.originals["a/Main"] = value.copy(evidence = evidence)
        val c = controller(); val p = prepared(c)
        assertEquals(evidence, p.inputs.single().originalEvidence)
        c.submit(1, p.id); assertEquals(0, calls)
        c.verifyOriginals(1, "wrong-plan", true); c.submit(1, p.id); assertEquals(0, calls)
        c.verifyOriginals(1, p.id, true)
        f.originals["a/Main"] = value.copy(evidence = evidence.copy(density = 480))
        c.submit(1, p.id); idle(c); assertEquals(0, calls)
        val fresh = prepared(c); c.submit(1, fresh.id); assertEquals(0, calls)
        c.verifyOriginals(1, fresh.id, true); c.submit(1, fresh.id); idle(c); assertEquals(1, calls)
    }
    @Test fun appliedDisplayAAndExtendStyleBRemainSeparateFromFrozenOriginalCAndRealFirstMultipart() = runBlocking {
        f.initialize("EXTEND", refs = 1)
        val genuineC = f.originals.getValue("a/Main").png
        var appliedA = projectTestPng(android.graphics.Color.RED, 1024)
        val sourceB = f.repo.referenceBytes(1, f.repo.load(1).references.single().hash)
        assertFalse(genuineC.contentEquals(appliedA)); assertFalse(genuineC.contentEquals(sourceB))
        okhttp3.mockwebserver.MockWebServer().use { server ->
            val client = okhttp3.OkHttpClient.Builder().callTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
            gateway = ClientIconGenerationGateway({ java.util.UUID.randomUUID().toString() },
                OpenAiImageClient(client, server.url("/edits")), StabilityImageClient(client, server.url("/style"), server.url("/background")))
            val c = controller()
            for (pass in 0..1) {
                if (pass == 1) {
                    appliedA = projectTestPng(android.graphics.Color.GREEN, 1024)
                    f.index = f.index.copy(stamp = f.index.stamp.copy(packageName = "source.other", versionCode = 2, theme = true))
                    val old = f.repo.load(1)
                    f.repo.saveStyle(1, old.revision!!.revision, old.revision.specification, old.revision.userInstruction,
                        old.references.map { it.hash }, "EXTEND", "source.other", projectSourceStamp(f.index))
                    val metadata = f.repo.load(1).metadata!!
                    f.repo.saveSelection(1, metadata.version, ProjectSelection("EXTEND", "source.other", projectSourceStamp(f.index),
                        projectCatalogStamp(f.targets), f.targets.map { it.key }))
                }
                c.load(1, ProjectSetupMode.EXTEND, if (pass == 0) "source.pack" else "source.other"); assertNull(idle(c).error)
                c.prepare(1, listOf("a/Main"), false); val plan = idle(c).plan!!
                assertEquals(sha256(genuineC), plan.inputs.single().originalHash)
                assertArrayEquals(genuineC, c.preview(1, plan.inputs.single().originalHash))
                assertArrayEquals(sourceB, c.preview(1, plan.referenceHashes.single()))
                val output = projectTestPng(size = 1024)
                server.enqueue(okhttp3.mockwebserver.MockResponse().setBody("{\"data\":[{\"b64_json\":\"${android.util.Base64.encodeToString(output, android.util.Base64.NO_WRAP)}\"}]}"))
                c.submit(1, plan.id); assertNull(idle(c).error)
                val wire = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!.body.readByteArray().toString(Charsets.ISO_8859_1)
                val originalPosition = wire.indexOf(genuineC.toString(Charsets.ISO_8859_1))
                val stylePosition = wire.indexOf(sourceB.toString(Charsets.ISO_8859_1))
                assertTrue(originalPosition >= 0 && stylePosition > originalPosition)
                assertFalse(wire.contains(appliedA.toString(Charsets.ISO_8859_1)))
                assertEquals(pass + 1, server.requestCount)
            }
        }
    }
    @Test fun openPrepareCancelReloadAndMissingKeyRemainLocal() = runBlocking {
        key = false; val c = controller(); val p = prepared(c)
        assertFalse(c.state(1).value.hasKey); c.submit(1, p.id); c.cancelPreparation(1); loaded(c)
        assertEquals(0, calls); assertTrue(f.journal.records(1).isEmpty())
    }
    @Test fun doubleTapAcceptApproveAndM6ReadOnlyReuseNeedNoAdditionalGeneration() = runBlocking {
        val c = controller(); val p = prepared(c); c.submit(1, p.id); c.submit(1, p.id); idle(c)
        val row = c.state(1).value.variants.single(); c.review(1, row.id, true); idle(c)
        assertFalse(c.state(1).value.ready); assertTrue(c.accepted(1).isEmpty())
        c.approve(1); assertTrue(idle(c).ready); assertEquals(row.id, c.accepted(1).single().variantId)
        assertEquals(1, calls); assertEquals("NOT_APPLIED", f.repo.trialVariants(1).single().applyStatus)
    }
    @Test fun modelSelectionAndSameProfileRouteChangesInvalidateFrozenConfirmation() = runBlocking {
        val c = controller(); val p = prepared(c)
        c.options(1, AiSettings.OPENAI, AiSettings.MODELS.last(), "high"); idle(c); c.submit(1, p.id)
        assertEquals(0, calls); assertNull(c.state(1).value.plan)
        val second = prepared(c); c.bindContext(1, ProjectSetupMode.EXTEND, "other.pack"); c.submit(1, second.id)
        assertEquals(0, calls); assertNull(c.state(1).value.plan)
    }
    @Test fun targetLockOriginalChangeOrNewSavedStylePreventsStalePaidSubmission() = runBlocking {
        val c = controller(); val p = prepared(c)
        f.targets = f.targets.map { if (it.key == "a/Main") it.copy(locked = true) else it }; c.submit(1, p.id); idle(c)
        assertEquals(0, calls)
        f.targets = f.targets.map { it.copy(locked = false) }
        val second = prepared(c); f.originals["a/Main"] = TrialOriginal(projectTestPng(android.graphics.Color.RED, 1024), "launcher:a/Main:2")
        c.submit(1, second.id); idle(c); assertEquals(0, calls)
        val third = prepared(c); f.repo.saveStyle(1, 1, "new immutable style", "", emptyList()); c.submit(1, third.id); idle(c)
        assertEquals(0, calls); assertTrue(f.repo.trialVariants(1).isEmpty())
    }
    @Test fun profileSwitchWhileResponseArrivesKeepsOldOwnedVariantAndSkipsRemaining() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        gateway = IconGenerationGateway { r ->
            calls++; r.beforeEndpoint(1); entered.complete(Unit); release.await()
            IconGenerationOutcome.Generated(projectTestPng(size = 1024), r.requestId, r.provider, 1024, 1024, 1)
        }
        val c = controller(); val p = prepared(c, listOf("a/Main", "b/Main")); c.submit(1, p.id)
        withTimeout(5000) { entered.await() }; mutex.withLock { active = 2 }; release.complete(Unit); idle(c)
        assertEquals(1, calls); assertEquals(1, f.repo.trialVariants(1).size); assertTrue(f.repo.trialVariants(2).isEmpty())
        assertEquals(listOf(TrialStatus.REVIEWABLE, TrialStatus.SKIPPED), f.journal.read(1, p.id)!!.items.map { it.status })
        assertTrue(runCatching { c.accepted(1) }.isFailure)
    }
    @Test fun extendUsesCurrentSourceCoverageAndUpdateUninstallOrDynamicUnknownCannotSubmit() = runBlocking {
        f.initialize("EXTEND"); val c = controller(); loaded(c, true)
        c.prepare(1, listOf("a/Main"), false); val p = checkNotNull(idle(c).plan)
        f.index = f.index.copy(stamp = f.index.stamp.copy(versionCode = 2)); c.submit(1, p.id); idle(c); assertEquals(0, calls)
        c.load(1, ProjectSetupMode.EXTEND, "source.pack"); assertNotNull(idle(c).error)
        f.sourceCurrent = false; c.load(1, ProjectSetupMode.EXTEND, "source.pack"); assertNotNull(idle(c).error)
        assertEquals(0, calls)
    }
    @Test fun regenerationKeepsAcceptedHistoryAndRequiresNewExplicitConsent() = runBlocking {
        val c = controller(); val first = generated(c); val old = c.state(1).value.variants.single()
        c.review(1, old.id, true); idle(c)
        c.prepare(1, listOf("a/Main"), false); val second = checkNotNull(idle(c).plan)
        assertNotEquals(first.id, second.id); assertNotEquals(first.inputs.single().requestId, second.inputs.single().requestId)
        assertEquals(1, calls); c.cancelPreparation(1); assertEquals(1, f.repo.trialVariants(1).size)
        assertEquals("ACCEPTED", f.repo.trialVariants(1).single().reviewStatus)
    }
    @Test fun restoredCompletedTrialCanBeExplicitlyRecheckedAndApprovedWithoutAnotherPost() = runBlocking {
        val c = controller(); generated(c); val row = c.state(1).value.variants.single()
        c.review(1, row.id, true); idle(c); c.approve(1); assertTrue(idle(c).ready)
        f.journal.invalidateAll(); loaded(c); assertFalse(c.state(1).value.ready); assertTrue(c.accepted(1).isEmpty())
        c.review(1, row.id, true); assertNull(idle(c).error); assertFalse(c.state(1).value.ready)
        c.approve(1); assertTrue(idle(c).ready); assertEquals(row.id, c.accepted(1).single().variantId)
        assertEquals(1, calls); assertEquals(1, f.repo.trialVariants(1).size)
    }
}
