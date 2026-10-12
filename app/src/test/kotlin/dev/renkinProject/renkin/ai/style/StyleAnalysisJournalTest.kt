package dev.renkinProject.renkin.ai.style

import android.app.Application
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleAnalysisJournalTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun journal() = StyleAnalysisJournal(analysisFixtureContext(temporary.root))
    private fun suggested() = StyleAnalysisOutcome.Suggested(analysisFixtureStyle(), "resp_fixture", AnalysisUsage(40, 60))

    @Test fun noConsentAndStaleContextWriteNoFilesAndCallNoGateway() = runBlocking {
        val journal = journal(); val calls = AtomicInteger()
        val coordinator = StyleAnalysisCoordinator(journal, StyleAnalysisGateway { calls.incrementAndGet(); suggested() })
        assertNull(coordinator.submit(analysisFixtureRequest(), false) { true })
        assertNull(coordinator.submit(analysisFixtureRequest(), true) { false })
        assertEquals(0, calls.get()); assertFalse(File(temporary.root, "style_analysis").exists())
    }
    @Test fun durableRequestingPrecedesCallAndResultReloadDoesNotRebillSameConsent() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest(); var calls = 0
        val gateway = StyleAnalysisGateway { input ->
            calls++; val marker = checkNotNull(journal.read(input.context.profileId, input.requestId))
            assertEquals(AnalysisStatus.REQUESTING, marker.status); assertEquals("UNKNOWN", marker.billingCertainty)
            assertArrayEquals(input.images[0].png, journal.image(input.context.profileId, input.images[0].hash)); suggested()
        }
        val result = checkNotNull(StyleAnalysisCoordinator(journal, gateway).submit(request, true) { true })
        assertEquals(AnalysisStatus.RESULT_SAVED, result.status); assertEquals("RESPONSE_RECEIVED", result.billingCertainty)
        assertEquals(request.fingerprint, result.fingerprint); assertEquals(request.promptHash, result.promptHash)
        assertEquals(result, journal().latest(4))
        assertEquals(result, StyleAnalysisCoordinator(journal(), gateway).submit(request, true) { true })
        assertEquals(1, calls)
    }
    @Test fun concurrentDoubleTapAndReconstructedCoordinatorSubmitExactlyOnce() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest(); val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger(); val gateway = StyleAnalysisGateway { calls.incrementAndGet(); entered.complete(Unit); release.await(); suggested() }
        val one = async { StyleAnalysisCoordinator(journal, gateway).submit(request, true) { true } }
        entered.await()
        val duplicate = StyleAnalysisCoordinator(journal(), gateway).submit(request, true) { true }
        assertEquals(AnalysisStatus.REQUESTING, duplicate!!.status)
        val another = request.copy(requestId = UUID.randomUUID().toString())
        try { StyleAnalysisCoordinator(journal(), gateway).submit(another, true) { true }; fail("Concurrent analysis must be rejected") }
        catch (_: IllegalStateException) { }
        release.complete(Unit); one.await(); assertEquals(1, calls.get())
    }
    @Test fun cancellationAndColdRecoveryAreUncertainAndNeverAutomaticallyResend() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest(); val entered = CompletableDeferred<Unit>(); var calls = 0
        val gateway = StyleAnalysisGateway { calls++; entered.complete(Unit); awaitCancellation() }
        val job = launch { StyleAnalysisCoordinator(journal, gateway).submit(request, true) { true } }
        entered.await(); job.cancelAndJoin()
        assertEquals(AnalysisStatus.UNCERTAIN, journal().latest(4)!!.status)
        StyleAnalysisCoordinator(journal(), StyleAnalysisGateway { calls++; suggested() }).submit(request, true) { true }
        assertEquals(1, calls)
        // A true process restart loses the live registry; releasing models that exact boundary.
        val crashed = request.copy(requestId = UUID.randomUUID().toString())
        assertTrue(journal.claim(crashed)); journal.release(4, crashed.requestId)
        assertEquals(AnalysisStatus.UNCERTAIN, journal().latest(4)!!.status)
        assertFalse(journal.claim(crashed))
    }
    @Test fun explicitRetryNeedsFreshRequestIdentityAndFreshConfirmation() = runBlocking {
        val journal = journal(); var calls = 0
        val coordinator = StyleAnalysisCoordinator(journal, StyleAnalysisGateway { calls++; StyleAnalysisOutcome.Uncertain })
        val request = analysisFixtureRequest(); coordinator.submit(request, true) { true }
        coordinator.submit(request, true) { true }; assertEquals(1, calls)
        val retry = request.copy(requestId = UUID.randomUUID().toString())
        assertEquals(request.fingerprint, retry.fingerprint)
        coordinator.submit(retry, false) { true }; assertEquals(1, calls)
        coordinator.submit(retry, true) { true }; assertEquals(2, calls)
    }
    @Test fun markerFailureAndCorruptedRecordFailClosedBeforeAnyPost() = runBlocking {
        val blocker = File(temporary.root, "style_analysis").apply { writeText("a file, not a directory") }
        var calls = 0; val request = analysisFixtureRequest()
        try { StyleAnalysisCoordinator(journal(), StyleAnalysisGateway { calls++; suggested() }).submit(request, true) { true }; fail("Marker write must fail") }
        catch (_: Exception) { assertEquals(0, calls) }
        assertTrue(blocker.delete())
        val journal = journal(); assertTrue(journal.claim(request)); journal.release(4, request.requestId)
        File(temporary.root, "style_analysis/4/request-${request.requestId}.json").writeText("corrupted")
        try { StyleAnalysisCoordinator(journal(), StyleAnalysisGateway { calls++; suggested() }).submit(request, true) { true }; fail("Corruption must not replay") }
        catch (_: Exception) { assertEquals(0, calls) }
    }
    @Test fun explicitCleanupRetainsConsentMarkersAndReleasesOnlyUnpinnedImages() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest()
        StyleAnalysisCoordinator(journal, StyleAnalysisGateway { suggested() }).submit(request, true) { true }
        journal.reviewed(4, request.requestId, 2)
        assertEquals(AnalysisStatus.REVIEWED, journal.read(4, request.requestId)!!.status)
        assertFalse(File(temporary.root, "style_analysis/4/images/${request.images[0].hash}.png").exists())
        assertFalse(journal().claim(request))
        val second = request.copy(requestId = UUID.randomUUID().toString())
        StyleAnalysisCoordinator(journal, StyleAnalysisGateway { suggested() }).submit(second, true) { true }
        journal.discard(4, second.requestId)
        assertEquals(AnalysisStatus.FAILED, journal.read(4, second.requestId)!!.status); assertFalse(journal.claim(second))
    }
    @Test fun fullRestoreAndProfileDeleteRotateEpochAndBlockOldPreparation() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest()
        StyleAnalysisCoordinator(journal, StyleAnalysisGateway { suggested() }).submit(request, true) { true }
        journal().invalidateAll()
        assertNotEquals(request.context.storageEpoch, journal.epoch(4))
        assertEquals(AnalysisStatus.FAILED, journal.read(4, request.requestId)!!.status)
        assertThrows(IllegalArgumentException::class.java) { journal.claim(request.copy(requestId = UUID.randomUUID().toString())) }
        val fresh = request.copy(context = request.context.copy(storageEpoch = journal.epoch(4)), requestId = UUID.randomUUID().toString())
        StyleAnalysisCoordinator(journal, StyleAnalysisGateway { suggested() }).submit(fresh, true) { true }
        journal().invalidateProfile(4)
        val tombstone = journal.read(4, fresh.requestId)!!
        assertNull(tombstone.context); assertNull(tombstone.styleJson); assertTrue(tombstone.hashes.isEmpty()); assertEquals("", tombstone.description)
        assertFalse(journal.claim(fresh)); assertNotEquals(fresh.context.storageEpoch, journal.epoch(4))
    }
    @Test fun pngTamperingAndStagingQuotaPreventUnverifiedUpload() {
        val journal = journal(); val request = analysisFixtureRequest(); assertTrue(journal.claim(request)); journal.release(4, request.requestId)
        val png = File(temporary.root, "style_analysis/4/images/${request.images[0].hash}.png")
        png.writeBytes(analysisFixturePng(2))
        assertThrows(Exception::class.java) { journal.image(4, request.images[0].hash) }
        val huge = File(png.parentFile, "quota.bin"); java.io.RandomAccessFile(huge, "rw").use { it.setLength(8L * 1024 * 1024) }
        assertThrows(IllegalArgumentException::class.java) { journal.claim(analysisFixtureRequest(images = 2)) }
    }
    @Test fun atomicBackupRecoveryAndInterruptedNewMarkerStayConsumed() {
        val journal = journal(); val request = analysisFixtureRequest(); assertTrue(journal.claim(request)); journal.release(4, request.requestId)
        val file = File(temporary.root, "style_analysis/4/request-${request.requestId}.json")
        assertTrue(file.renameTo(File(file.path + ".bak")))
        assertFalse(journal().claim(request)); assertEquals(request.requestId, journal().read(4, request.requestId)!!.requestId)
        val second = request.copy(requestId = UUID.randomUUID().toString())
        File(file.parentFile, "request-${second.requestId}.json.new").writeText("interrupted")
        assertFalse(journal().claim(second))
    }
}
