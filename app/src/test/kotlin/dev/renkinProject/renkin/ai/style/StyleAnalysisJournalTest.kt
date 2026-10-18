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

    private fun archive(journal: StyleAnalysisJournal, request: StyleAnalysisRequest) {
        assertTrue(journal.claim(request))
        journal.finish(request.context.profileId, request.requestId, suggested())
        journal.release(request.context.profileId, request.requestId)
    }
    @Test fun historicalAlgorithmFingerprintsRemainReadableAndNewEvidenceSurvivesReopen() = runBlocking {
        val old = analysisFixtureRequest().copy(algorithm = LEGACY_REFERENCE_ALGORITHM)
        val j = journal(); assertTrue(j.claim(old)); j.finish(4, old.requestId, suggested()); j.release(4, old.requestId)
        val record = j.read(4, old.requestId)!!
        assertFalse(org.json.JSONObject(record.encode()).has("algorithm"))
        assertEquals(old.fingerprint, AnalysisRecord.parse(record.encode()).fingerprint)
        assertEquals(LEGACY_REFERENCE_ALGORITHM, record.algorithm)
        val samples = ReferenceSampler().manual((0 until 5).map { "own$it" to analysisFixturePng(it % 3, it) })
        val current = analysisFixtureRequest(images = samples.samples.size).copy(images = samples.samples.map { AnalysisImage(it.hash, it.png) }, localReport = samples.report)
        assertTrue(j.claim(current)); j.finish(4, current.requestId, suggested()); j.release(4, current.requestId)
        val reopened = journal().latest(4)!!
        assertEquals(REFERENCE_ALGORITHM, reopened.algorithm)
        assertEquals(samples.report, reopened.localReport)
        assertEquals(current.fingerprint, reopened.fingerprint)
        assertEquals(analysisFixtureStyle(), reopened.style)
        assertFalse(journal().claim(current))
    }

    @Test fun historicalRequestMarkerAndSavedResultReloadCannotReclaimConsent() = runBlocking {
        val j = journal(); val request = analysisFixtureRequest()
        assertTrue(j.claim(request))
        val marker = checkNotNull(j.read(4, request.requestId))
        assertEquals(AnalysisStatus.REQUESTING, marker.status); assertEquals("UNKNOWN", marker.billingCertainty)
        assertArrayEquals(request.images[0].png, j.image(4, request.images[0].hash))
        val result = j.finish(4, request.requestId, suggested()); j.release(4, request.requestId)
        assertEquals(AnalysisStatus.RESULT_SAVED, result.status)
        assertEquals(request.fingerprint, result.fingerprint); assertEquals(request.promptHash, result.promptHash)
        assertEquals(result, journal().latest(4)); assertFalse(journal().claim(request))
    }

    @Test fun historicalJournalClaimsReserveAnIdentityBeforeReopening() = runBlocking {
        val j = journal(); val request = analysisFixtureRequest()
        assertTrue(j.claim(request)); assertFalse(journal().claim(request))
        val another = request.copy(requestId = UUID.randomUUID().toString())
        assertThrows(IllegalStateException::class.java) { journal().claim(another) }
        j.finish(4, request.requestId, suggested()); j.release(4, request.requestId)
        assertEquals(AnalysisStatus.RESULT_SAVED, journal().latest(4)!!.status)
        assertFalse(journal().claim(request))
    }

    @Test fun historicalInterruptedAndColdRecordsCannotBeClaimedAgain() = runBlocking {
        val j = journal(); val request = analysisFixtureRequest()
        assertTrue(j.claim(request)); j.interrupted(4, request.requestId); j.release(4, request.requestId)
        assertEquals(AnalysisStatus.UNCERTAIN, journal().latest(4)!!.status)
        assertFalse(journal().claim(request))
        val crashed = request.copy(requestId = UUID.randomUUID().toString())
        assertTrue(j.claim(crashed)); j.release(4, crashed.requestId)
        assertEquals(AnalysisStatus.UNCERTAIN, journal().latest(4)!!.status)
        assertFalse(journal().claim(crashed))
    }


    @Test fun markerFailureAndCorruptedHistoricalRecordFailClosed() = runBlocking {
        val blocker = File(temporary.root, "style_analysis").apply { writeText("not a directory") }
        val request = analysisFixtureRequest()
        assertTrue(runCatching { journal().claim(request) }.isFailure)
        assertTrue(blocker.delete())
        val j = journal(); assertTrue(j.claim(request)); j.release(4, request.requestId)
        File(temporary.root, "style_analysis/4/request-${request.requestId}.json").writeText("corrupted")
        assertTrue(runCatching { journal().read(4, request.requestId) }.isFailure)
    }

    @Test fun explicitCleanupRetainsConsentMarkersAndReleasesOnlyUnpinnedImages() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest()
        archive(journal, request)
        journal.reviewed(4, request.requestId, 2)
        assertEquals(AnalysisStatus.REVIEWED, journal.read(4, request.requestId)!!.status)
        assertFalse(File(temporary.root, "style_analysis/4/images/${request.images[0].hash}.png").exists())
        assertFalse(journal().claim(request))
        val second = request.copy(requestId = UUID.randomUUID().toString())
        archive(journal, second)
        journal.discard(4, second.requestId)
        assertEquals(AnalysisStatus.FAILED, journal.read(4, second.requestId)!!.status); assertFalse(journal.claim(second))
    }
    @Test fun fullRestoreAndProfileDeleteRotateEpochAndBlockOldPreparation() = runBlocking {
        val journal = journal(); val request = analysisFixtureRequest()
        archive(journal, request)
        journal().invalidateAll()
        assertNotEquals(request.context.storageEpoch, journal.epoch(4))
        assertEquals(AnalysisStatus.FAILED, journal.read(4, request.requestId)!!.status)
        assertThrows(IllegalArgumentException::class.java) { journal.claim(request.copy(requestId = UUID.randomUUID().toString())) }
        val fresh = request.copy(context = request.context.copy(storageEpoch = journal.epoch(4)), requestId = UUID.randomUUID().toString())
        archive(journal, fresh)
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
