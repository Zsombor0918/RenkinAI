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
}
