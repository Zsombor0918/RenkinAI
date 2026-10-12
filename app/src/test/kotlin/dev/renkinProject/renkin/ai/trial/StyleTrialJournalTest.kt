package dev.renkinProject.renkin.ai.trial

import android.app.Application
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleTrialJournalTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var f: StyleTrialFixture
    @Before fun setup() = runBlocking { f = StyleTrialFixture(temporary.root); f.initialize() }
    @After fun close() { f.close() }
    @Test fun declineAndStalePlanHaveZeroFilesAndZeroGatewayCalls() = runBlocking {
        var calls = 0; val c = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo)
        val p = f.plan(); val images = f.images(p)
        c.submit(p, images, false, { true }); c.submit(p, images, true, { false })
        assertEquals(0, calls); assertFalse(File(f.context.noBackupFilesDir, "style_trials").exists())
    }
    @Test fun serialSubmissionIsDurableBeforeCallAndSameIdCannotSubmitTwice() = runBlocking {
        var calls = 0
        val c = StyleTrialCoordinator(f.journal, f.successful { r ->
            calls++; val saved = f.journal.records(1).single().items.single { it.requestId == r.requestId }
            assertEquals(TrialStatus.REQUESTING, saved.status)
            assertEquals(calls - 1, f.repo.trialVariants(1).size)
        }, f.repo)
        val p = f.plan(); c.submit(p, f.images(p), true, { true }); c.submit(p, f.images(p), true, { true })
        assertEquals(3, calls); assertEquals(3, f.repo.trialVariants(1).size)
        assertTrue(f.journal.read(1, p.id)!!.items.all { it.status == TrialStatus.REVIEWABLE && it.endpointCalls == 1 })
        assertTrue(dev.renkinProject.renkin.data.RenkinPackRepository(f.db).getAll(1).isEmpty())
    }
    @Test fun twoConcurrentConfirmationCallbacksConsumeOnlyOneFrozenUuid() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>(); var calls = 0
        val gateway = IconGenerationGateway { request ->
            calls++; request.beforeEndpoint(1); entered.complete(Unit); release.await()
            IconGenerationOutcome.Generated(projectTestPng(size = 1024), request.requestId, request.provider, 1024, 1024, 1)
        }
        val c = StyleTrialCoordinator(f.journal, gateway, f.repo); val p = f.plan(listOf("a/Main")); val images = f.images(p)
        val first = launch(Dispatchers.Default) { c.submit(p, images, true, { true }) }
        withTimeout(5000) { entered.await() }
        c.submit(p, images, true, { true }); assertEquals(1, calls)
        release.complete(Unit); first.join(); assertEquals(1, f.repo.trialVariants(1).size)
    }
    @Test fun coldRequestingBecomesUncertainAndUnstartedItemsNeverReplay() = runBlocking {
        var calls = 0; val p = f.plan(); val images = f.images(p)
        assertTrue(f.journal.claim(p, images)); f.journal.begin(p, p.inputs.first().requestId)
        f.journal.endpoint(p, p.inputs.first().requestId, 1); f.journal.release(p)
        val fresh = StyleTrialJournal(f.context)
        val c = StyleTrialCoordinator(fresh, f.successful { calls++ }, f.repo)
        c.recover(1); c.submit(p, images, true, { true }); c.recover(1)
        assertEquals(0, calls)
        assertEquals(listOf(TrialStatus.UNCERTAIN, TrialStatus.SKIPPED, TrialStatus.SKIPPED), fresh.read(1, p.id)!!.items.map { it.status })
    }
    @Test fun aNewPlanCannotReuseAnAlreadyConsumedItemUuid() = runBlocking {
        val p = f.plan(listOf("a/Main")); var calls = 0
        val c = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo)
        c.submit(p, f.images(p), true, { true })
        val repeated = p.copy(id = java.util.UUID.randomUUID().toString())
        assertTrue(runCatching { c.submit(repeated, f.images(repeated), true, { true }) }.isFailure)
        assertEquals(1, calls)
    }
    @Test fun fileBeforeMarkerAndFileBeforeRoomFaultsRecoverOnlyLocally() = runBlocking {
        for (point in listOf("RESULT_FILE_SAVED", "FILES_INSTALLED", "DB_COMMITTED")) {
            var calls = 0; var thrown = false
            val c = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo) { location ->
                if (location == point && !thrown) { thrown = true; error("injected crash") }
            }
            val p = f.plan(listOf("a/Main")); c.submit(p, f.images(p), true, { true })
            assertTrue(thrown); assertEquals(1, calls)
            val repair = StyleTrialCoordinator(StyleTrialJournal(f.context), f.successful { calls++ }, f.repo)
            repair.recover(1); repair.recover(1)
            assertEquals(1, calls); assertEquals(TrialStatus.REVIEWABLE, f.journal.read(1, p.id)!!.items.single().status)
            val row = f.repo.trialVariants(1).single { it.id == p.inputs.single().requestId }
            assertArrayEquals(projectTestPng(size = 1024), f.repo.referenceBytes(1, row.rawAssetHash))
            assertEquals("NOT_APPLIED", row.applyStatus)
        }
        assertEquals(3, f.repo.trialVariants(1).size)
    }
    @Test fun stopKeepsCompletedFirstAndMarksSentSecondUncertainSkippingThird() = runBlocking {
        var calls = 0; val entered = CompletableDeferred<Unit>()
        val gateway = IconGenerationGateway { request ->
            calls++; request.beforeEndpoint(1)
            if (calls == 2) { entered.complete(Unit); awaitCancellation() }
            IconGenerationOutcome.Generated(projectTestPng(size = 1024), request.requestId, request.provider, 1024, 1024, 1)
        }
        val c = StyleTrialCoordinator(f.journal, gateway, f.repo); val p = f.plan()
        val job = launch(Dispatchers.Default) { c.submit(p, f.images(p), true, { true }) }
        withTimeout(5000) { entered.await() }; job.cancelAndJoin()
        assertEquals(2, calls); assertEquals(1, f.repo.trialVariants(1).size)
        assertEquals(listOf(TrialStatus.REVIEWABLE, TrialStatus.UNCERTAIN, TrialStatus.SKIPPED), f.journal.read(1, p.id)!!.items.map { it.status })
        c.recover(1); assertEquals(2, calls)
    }
    @Test fun stabilitySecondEndpointMarkerCannotBeRepeatedAfterFailure() = runBlocking {
        f.initialize(refs = 1); var calls = 0
        val gateway = IconGenerationGateway { request ->
            calls++; request.beforeEndpoint(1); request.beforeEndpoint(2)
            assertTrue(runCatching { request.beforeEndpoint(2) }.isFailure)
            IconGenerationOutcome.Uncertain(request.requestId, request.provider)
        }
        val c = StyleTrialCoordinator(f.journal, gateway, f.repo); val p = f.plan(listOf("a/Main"), AiSettings.STABILITY)
        c.submit(p, f.images(p), true, { true }); c.recover(1); c.submit(p, f.images(p), true, { true })
        assertEquals(1, calls); assertEquals(2, f.journal.read(1, p.id)!!.items.single().endpointCalls)
        assertEquals(TrialStatus.UNCERTAIN, f.journal.read(1, p.id)!!.items.single().status)
    }
    @Test fun markerStorageQuotaAndCorruptRecordsFailBeforePost() = runBlocking {
        var calls = 0; val p = f.plan(listOf("a/Main")); val c = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo)
        val dir = File(f.context.noBackupFilesDir, "style_trials/1").apply { mkdirs() }
        val quota = File(dir, "quota.bin"); RandomAccessFile(quota, "rw").use { it.setLength(64L * 1024 * 1024) }
        assertTrue(runCatching { c.submit(p, f.images(p), true, { true }) }.isFailure); quota.delete()
        val marker = File(dir, "plan-${p.id}.json"); marker.writeText("{broken")
        assertTrue(runCatching { c.recover(1) }.isFailure)
        assertEquals(0, calls)
    }
    @Test fun restoreEpochInvalidatesOldStagedResultWithoutRebillingOrWrongHistoryInsert() = runBlocking {
        val p = f.plan(listOf("a/Main")); var calls = 0
        val c = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo) { if (it == "RESULT_FILE_SAVED") error("kill") }
        c.submit(p, f.images(p), true, { true }); f.journal.invalidateAll()
        val repair = StyleTrialCoordinator(f.journal, f.successful { calls++ }, f.repo)
        repair.recover(1); assertTrue(f.repo.trialVariants(1).isEmpty()); assertEquals(1, calls)
        assertFalse(f.journal.current(p)); assertFalse(f.journal.claim(p, f.images(p)))
    }
}
