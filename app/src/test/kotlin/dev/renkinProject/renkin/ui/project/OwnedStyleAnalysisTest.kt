package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.data.RenkinPackDatabase
import dev.renkinProject.renkin.data.RenkinPackRepository
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OwnedStyleAnalysisTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var db: RenkinPackDatabase
    private lateinit var repo: ProjectAiRepository
    private lateinit var journal: StyleAnalysisJournal
    private lateinit var scope: CoroutineScope
    private var active = 1L
    private var calls = 0
    private var key = true
    private var catalog = sha256("catalog".toByteArray())
    private var reader = FixtureReferenceReader((0 until 12).associate { "icon$it" to analysisFixturePng(it % 3, it) })
    private var gateway: StyleAnalysisGateway = StyleAnalysisGateway { calls++; StyleAnalysisOutcome.Suggested(analysisFixtureStyle(), "resp_fixture", null) }
    private val lock = Mutex()
    private val owner = object : AnalysisSessionOwner {
        override suspend fun <T> owned(id: Long, operation: suspend () -> T): T = lock.withLock { check(id == active); operation() }
        override suspend fun catalogStamp() = catalog
        override suspend fun reader(source: String, referenceIndex: Boolean): CoverageSourceReader = sourceOverride ?: reader
    }
    private fun newController() = OwnedStyleAnalysis(scope, repo, journal, ReferenceSampler(), StyleAnalysisCoordinator(journal, gateway), { key }, owner)
    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).build()
        RenkinPackRepository(db).replaceEverything(listOf(Profile(1, "Same"), Profile(2, "Same")), emptyList())
        repo = ProjectAiRepository(db, ProjectAssetStore(context))
        journal = StyleAnalysisJournal(analysisFixtureContext(temporary.root))
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    @After fun close() { scope.cancel(); db.close(); File(context.filesDir, "project_ai_assets").deleteRecursively() }
    private suspend fun prepare(controller: OwnedStyleAnalysis, extend: Boolean = false): StyleAnalysisRequest {
        val refs = if (extend) emptyList() else listOf(repo.importReference(1, ByteArrayInputStream(analysisFixturePng())).hash)
        val draft = ProjectStyleDraft("outline", "do not lose my instruction", refs)
        controller.prepare(1, if (extend) ProjectSetupMode.EXTEND else ProjectSetupMode.CREATE,
            if (extend) "fixture.pack" else null, if (extend) projectSourceStamp(reader.index) else null, repo.load(1).revision?.revision, draft)
        val state = controller.state(1).first { !it.busy }
        assertNull(state.error); return checkNotNull(state.preparation)
    }
    private suspend fun result(controller: OwnedStyleAnalysis, extend: Boolean = false): AnalysisRecord {
        val request = prepare(controller, extend); controller.submit(1, request.requestId)
        val state = controller.state(1).first { !it.busy }; assertNull(state.error)
        return checkNotNull(state.record)
    }
    private fun draft(used: AnalyzedDraft, instruction: String = "do not lose my instruction", stale: Boolean = false) =
        ProjectStyleDraft(used.specification, instruction, used.hashes, used.requestId, used.reviewedAt, stale, used.reviewedSourceStamp)

    @Test fun prepareAndReopenAreLocalAndMissingKeyDoesNotBlockManualSave() = runBlocking {
        key = false; val controller = newController(); val request = prepare(controller)
        assertFalse(controller.state(1).value.hasKey); assertEquals(0, calls)
        controller.submit(1, request.requestId); controller.refresh(1); assertEquals(0, calls)
        assertFalse(File(temporary.root, "style_analysis").exists())
        repo.saveStyle(1, null, "manual", "", emptyList()); assertEquals("USER", repo.load(1).revision!!.origin)
    }
    @Test fun useChangesOnlyDraftAndExplicitSavePreservesInstructionAndImmutableHistory() = runBlocking {
        val controller = newController(); val record = result(controller)
        assertNull(repo.load(1).revision)
        val used = controller.use(1, record.requestId, record.hashes, false)
        assertNull(repo.load(1).revision)
        val saved = controller.save(1, null, draft(used))
        assertEquals("AUTO_ANALYSIS", saved.revision!!.origin)
        assertEquals("do not lose my instruction", saved.revision!!.userInstruction)
        assertEquals(used.hashes, saved.references.map { it.hash })
        assertEquals(AnalysisStatus.REVIEWED, journal.read(1, record.requestId)!!.status)
        assertEquals(saved, newController().save(1, null, draft(used)))
        assertEquals(1, repo.history(1).size); assertEquals(1, calls)
    }
    @Test fun significantManualOverrideIsUserOriginButRetainsAnalysisEvidence() = runBlocking {
        val controller = newController(); val record = result(controller)
        val used = controller.use(1, record.requestId, record.hashes, false)
        val saved = controller.save(1, null, draft(used).copy(specification = "manually corrected solid charcoal"))
        assertEquals("USER", saved.revision!!.origin)
        val evidence = ReviewedAnalysisEvidence.parse(saved.revision!!.analysisEvidenceJson!!)
        assertTrue(evidence.userEdited); assertEquals(record.requestId, evidence.requestId)
        assertNotNull(saved.revision!!.analysisProfileJson)
    }
    @Test fun profileSwitchDuringCloudResponseKeepsCandidateOwnedAndBlocksForeignUse() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        gateway = StyleAnalysisGateway { calls++; entered.complete(Unit); release.await(); StyleAnalysisOutcome.Suggested(analysisFixtureStyle(), "resp_fixture", null) }
        val controller = newController(); val request = prepare(controller); controller.submit(1, request.requestId); entered.await()
        lock.withLock { active = 2 }; release.complete(Unit)
        val state = controller.state(1).first { !it.busy }
        assertEquals(AnalysisValidity.WRONG_PROFILE, state.validity); assertEquals(AnalysisStatus.RESULT_SAVED, state.record!!.status)
        assertTrue(runCatching { controller.use(1, request.requestId, request.images.map { it.hash }, false) }.isFailure)
        assertNull(repo.load(1).revision); assertNull(repo.load(2).revision); assertEquals(1, calls)
        lock.withLock { active = 1 }; newController().refresh(1); assertEquals(1, calls)
    }
    @Test fun changedMetadataSelectionOrCatalogCannotApplyAnOlderResult() = runBlocking {
        val controller = newController(); val record = result(controller)
        repo.saveSelection(1, 0, ProjectSelection("CREATE", null, null, catalog, listOf("pkg/Main")))
        controller.refresh(1); assertEquals(AnalysisValidity.PREPARATION_CHANGED, controller.state(1).value.validity)
        assertTrue(runCatching { controller.use(1, record.requestId, emptyList(), true) }.isFailure)
        assertNull(repo.load(1).revision)
    }
    @Test fun freshCatalogIsRecheckedBetweenUseAndStyleSave() = runBlocking {
        val controller = newController(); val record = result(controller)
        val used = controller.use(1, record.requestId, record.hashes, false)
        catalog = sha256("new catalog".toByteArray())
        assertTrue(runCatching { controller.save(1, null, draft(used)) }.isFailure)
        assertNull(repo.load(1).revision); assertEquals(AnalysisStatus.RESULT_SAVED, journal.read(1, record.requestId)!!.status)
    }
    @Test fun sourceOnlyChangeRequiresExplicitReviewAndAnotherUpdateBlocksSaveAgain() = runBlocking {
        val controller = newController(); val record = result(controller, extend = true)
        sourceOverride = object : CoverageSourceReader by reader { override val index = reader.index.copy(stamp = reader.index.stamp.copy(versionCode = 8)) }
        controller.refresh(1); assertEquals(AnalysisValidity.SOURCE_STALE, controller.state(1).value.validity)
        assertTrue(runCatching { controller.use(1, record.requestId, record.hashes.take(5), false) }.isFailure)
        val used = controller.use(1, record.requestId, record.hashes.take(5), true)
        sourceOverride = object : CoverageSourceReader by reader { override val index = reader.index.copy(stamp = reader.index.stamp.copy(versionCode = 9)) }
        assertTrue(runCatching { controller.save(1, null, draft(used, stale = true)) }.isFailure)
        assertNull(repo.load(1).revision)
        val reviewedAgain = controller.use(1, record.requestId, record.hashes.take(5), true)
        val saved = controller.save(1, null, draft(reviewedAgain, stale = true))
        assertTrue(ReviewedAnalysisEvidence.parse(saved.revision!!.analysisEvidenceJson!!).staleSourceAccepted)
        assertEquals(1, calls)
    }
    private var sourceOverride: CoverageSourceReader? = null

    @Test fun restoreEpochBlocksCandidatesEvenWhenProfileAndMetadataIdsMatch() = runBlocking {
        val controller = newController(); val record = result(controller); val used = controller.use(1, record.requestId, record.hashes, false)
        journal.invalidateAll(); controller.refresh(1)
        assertEquals(AnalysisValidity.RESTORED, controller.state(1).value.validity)
        assertTrue(runCatching { controller.save(1, null, draft(used)) }.isFailure)
        assertNull(repo.load(1).revision); assertEquals(1, calls)
    }
    @Test fun staleSavedRevisionAndMoreThanFiveRefsFailWithoutOverwritingManualStyle() = runBlocking {
        val controller = newController(); val record = result(controller, extend = true)
        assertTrue(record.hashes.size > 5)
        assertTrue(runCatching { controller.use(1, record.requestId, record.hashes, false) }.isFailure)
        val used = controller.use(1, record.requestId, record.hashes.take(5), false)
        repo.saveStyle(1, null, "keep manual", "", emptyList())
        assertTrue(runCatching { controller.save(1, null, draft(used)) }.isFailure)
        assertEquals("keep manual", repo.load(1).revision!!.specification)
    }
    @Test fun controllerRecreationAndDiscardNeverResendAndKeepActiveStyle() = runBlocking {
        val controller = newController(); val record = result(controller)
        controller.releasePreview(1)
        val recreated = newController(); recreated.refresh(1)
        assertEquals(record.requestId, recreated.state(1).value.record!!.requestId)
        assertNull(recreated.state(1).value.preparation); assertEquals(1, calls)
        recreated.discardCandidate(1, record.requestId); assertNull(repo.load(1).revision); assertEquals(1, calls)
    }
}
