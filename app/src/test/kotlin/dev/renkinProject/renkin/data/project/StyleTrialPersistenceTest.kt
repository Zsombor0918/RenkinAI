package dev.renkinProject.renkin.data.project

import android.app.Application
import android.graphics.Color
import dev.renkinProject.renkin.ai.trial.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleTrialPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var f: StyleTrialFixture
    @Before fun setup() = runBlocking { f = StyleTrialFixture(temporary.root); f.initialize() }
    @After fun close() { f.close() }
    private suspend fun saved(): Pair<StyleTrialPlan, IconAssetVariant> {
        val p = f.plan(listOf("a/Main"))
        StyleTrialCoordinator(f.journal, f.successful(), f.repo).submit(p, f.images(p), true, { true })
        return p to f.repo.trialVariants(1).single { it.id == p.inputs.single().requestId }
    }
    private fun originals() = f.originals.mapValues { it.value.hash to it.value.origin }
    private suspend fun accept(p: StyleTrialPlan, row: IconAssetVariant) = f.repo.reviewTrial(p.context, row.id, true,
        originals().getValue(row.componentKey), { f.journal.current(p) })
    @Test fun rawAndOriginalAreRealOwnedPngsAndAnotherAttemptPreservesBothVariants() = runBlocking {
        val (first, a) = saved(); val (_, b) = saved()
        assertNotEquals(a.id, b.id); assertEquals(2, f.repo.variants(1, "a/Main").size)
        assertEquals(first.context.revision, a.styleRevision)
        assertArrayEquals(f.originals.getValue("a/Main").png, f.repo.referenceBytes(1, a.originalHash))
        validateTrialPng(f.repo.referenceBytes(1, a.rawAssetHash))
        assertEquals("UNREVIEWED", a.reviewStatus); assertEquals("NOT_APPLIED", a.applyStatus)
        assertTrue(f.repo.trialVariants(2).isEmpty())
    }
    @Test fun acceptanceAloneDoesNotApproveAndExplicitApprovalYieldsReadOnlyReuseWithoutPost() = runBlocking {
        val (p, row) = saved()
        assertTrue(f.repo.acceptedTrials(p.context, originals(), { true }).isEmpty())
        assertTrue(runCatching { f.repo.approveTrials(p.context, originals(), { true }) }.isFailure)
        accept(p, row); assertTrue(f.repo.acceptedTrials(p.context, originals(), { true }).isEmpty())
        val approval = f.repo.approveTrials(p.context, originals(), { true })
        val accepted = f.repo.acceptedTrials(p.context, originals(), { true }).single()
        assertEquals(row.id, accepted.variantId); assertEquals(row.rawAssetHash, accepted.assetHash)
        assertEquals(approval.approvedAt, accepted.approvedAt)
        assertEquals("NOT_APPLIED", f.repo.trialVariants(1).single().applyStatus)
    }
    @Test fun allCurrentSuccessesNeedReviewAndRejectingApprovedImageInvalidatesReadiness() = runBlocking {
        val (p, a) = saved(); val (_, b) = saved(); accept(p, a)
        assertTrue(runCatching { f.repo.approveTrials(p.context, originals(), { true }) }.isFailure)
        f.repo.reviewTrial(p.context, b.id, false, originals().getValue(b.componentKey), { true })
        f.repo.approveTrials(p.context, originals(), { true })
        f.repo.reviewTrial(p.context, a.id, false, originals().getValue(a.componentKey), { true })
        assertTrue(f.repo.acceptedTrials(p.context, originals(), { true }).isEmpty())
        assertTrue(f.db.projectAiDao().approval(1, 1)!!.needsRevalidation)
        ProjectAiRepository.validateSnapshot(f.repo.snapshot(), setOf(1, 2))
    }
    @Test fun newRevisionEpochOriginalAndReferenceFingerprintsCannotInheritApproval() = runBlocking {
        val (p, row) = saved(); accept(p, row); f.repo.approveTrials(p.context, originals(), { true })
        assertTrue(f.repo.acceptedTrials(p.context, originals() + ("a/Main" to ("a".repeat(64) to "changed")), { true }).isEmpty())
        assertTrue(f.repo.acceptedTrials(p.context, originals(), { false }).isEmpty())
        f.repo.saveStyle(1, 1, "refined solid", "preserved", emptyList())
        val newer = f.plan(listOf("a/Main"))
        assertEquals(2L, newer.context.revision); assertTrue(f.repo.acceptedTrials(newer.context, originals(), { true }).isEmpty())
        assertArrayEquals(projectTestPng(size = 1024), f.repo.referenceBytes(1, row.rawAssetHash))
        assertEquals(2, f.repo.history(1).size)
    }
    @Test fun staleReviewWrongOwnerAndCorruptRawAreRejectedWithoutChangingReview() = runBlocking {
        val (p, row) = saved()
        assertTrue(runCatching { f.repo.reviewTrial(p.context.copy(profileId = 2), row.id, true, originals().getValue("a/Main"), { true }) }.isFailure)
        assertTrue(runCatching { f.repo.reviewTrial(p.context, row.id, true, "b".repeat(64) to "wrong", { true }) }.isFailure)
        File(f.context.filesDir, "project_ai_assets/${ProjectAssetStore.path(1, row.rawAssetHash)}").writeBytes(byteArrayOf(0))
        assertTrue(runCatching { accept(p, row) }.isFailure)
        assertEquals("UNREVIEWED", f.repo.trialVariants(1).single().reviewStatus)
    }
    @Test fun historyAndVariantPinsProtectOriginalAndRawFromDraftCleanupAndGc() = runBlocking {
        val (_, row) = saved(); f.repo.discardDraftReferences(1, listOf(row.originalHash, row.rawAssetHash))
        f.repo.collectUnreferencedFiles()
        assertTrue(f.repo.referenceBytes(1, row.originalHash).isNotEmpty()); assertTrue(f.repo.referenceBytes(1, row.rawAssetHash).isNotEmpty())
        assertEquals(2, f.repo.snapshot().assets.size)
    }
    @Test fun idempotentRecoveryNeverOverwritesExplicitAcceptedReview() = runBlocking {
        val (p, row) = saved(); accept(p, row)
        val returned = f.repo.installTrialVariant(p, p.inputs.single(), f.originals.getValue("a/Main").png,
            f.repo.referenceBytes(1, row.rawAssetHash), { true })
        assertEquals("ACCEPTED", returned.reviewStatus); assertEquals(1, f.repo.trialVariants(1).size)
    }
    @Test fun restoredEpochRequiresNewLocalReviewAndApprovalButNeverNewGeneration() = runBlocking {
        val (p, row) = saved(); accept(p, row); f.repo.approveTrials(p.context, originals(), { true })
        f.journal.invalidateAll()
        val current = p.context.copy(epoch = f.journal.epoch(1))
        assertTrue(f.repo.acceptedTrials(current, originals(), { true }).isEmpty())
        val origin = "launcher:a/Main:rechecked-version"
        val checked = originals() + ("a/Main" to (row.originalHash to origin))
        f.repo.reviewTrial(current, row.id, true, checked.getValue("a/Main"), { true })
        assertTrue(f.repo.acceptedTrials(current, checked, { true }).isEmpty())
        f.repo.approveTrials(current, checked, { true })
        assertEquals(row.id, f.repo.acceptedTrials(current, checked, { true }).single().variantId)
        val evidence = TrialVariantEvidence.parse(f.repo.trialVariants(1).single().trialContextJson!!)
        assertEquals(p.context, evidence.context); assertEquals(p.inputs.single().origin, evidence.originalOrigin)
        assertEquals(current, evidence.reviewContext); assertEquals(origin, evidence.reviewOriginalOrigin)
        assertEquals(1, f.journal.records(1).size)
    }
    @Test fun competingRevisionSaveCannotApproveAnOldInput() = runBlocking {
        val (p, row) = saved(); accept(p, row)
        f.repo.saveSelection(1, p.context.metadataVersion, ProjectSelection("CREATE", null, null,
            p.context.catalogStamp, listOf("b/Main")))
        assertTrue(runCatching { f.repo.approveTrials(p.context, originals(), { true }) }.isFailure)
        assertNull(f.db.projectAiDao().approval(1, 1)); assertEquals("ACCEPTED", f.repo.trialVariants(1).single().reviewStatus)
    }
}
