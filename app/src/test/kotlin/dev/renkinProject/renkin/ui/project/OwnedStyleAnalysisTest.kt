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
    private fun newController(dna: Boolean = false, dnaContext: Context = analysisFixtureContext(temporary.root)) = OwnedStyleAnalysis(scope, repo, journal, ReferenceSampler(),
        StyleAnalysisCoordinator(journal, gateway), { key }, owner,
        if (dna) StyleDnaCoordinator(StyleDnaJournal(dnaContext), StyleDnaGateway { _, _ ->
            calls++; error("Local preparation must not submit")
        }) else null)
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

    @Test fun trustedContextAliasPreparesBothModesAcrossPacksWithoutChangingHistoryOrRoom() = runBlocking {
        key = false
        repo.saveStyle(1, null, "keep saved style", "keep instruction", emptyList())
        val dnaJournal = StyleDnaJournal(analysisFixtureContext(temporary.root))
        val (historic, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 2)
        assertTrue(dnaJournal.claim(historic, images))
        dnaJournal.submitted(historic, historic.steps.first())
        dnaJournal.failed(historic, "NETWORK", true)
        dnaJournal.release(historic)
        val before = repo.snapshot()
        val historicBytes = File(temporary.root, "style_dna/1/plan-${historic.id}.json").readBytes()
        // Context owns its directory. Normalize its alias, never a child journal symlink.
        val contextAlias = analysisFixtureContext(File(temporary.root, "."))
        assertNotEquals(contextAlias.noBackupFilesDir.absolutePath, contextAlias.noBackupFilesDir.canonicalPath)
        val controller = newController(dna = true, dnaContext = contextAlias)
        for (profile in listOf(1L, 2L)) for (pack in listOf("outline.pack", "color.pack")) {
            active = profile
            val fixture = FixtureReferenceReader((0 until 30).associate { "art$it" to analysisFixturePng(if (pack == "outline.pack") 0 else 2, it) })
            sourceOverride = object : CoverageSourceReader by fixture {
                override val index = fixture.index.copy(stamp = fixture.index.stamp.copy(packageName = pack, theme = pack == "color.pack"))
            }
            for (mode in listOf(DnaAnalysisMode.QUICK, DnaAnalysisMode.STANDARD)) {
                controller.prepareDna(profile, ProjectSetupMode.EXTEND, pack, projectSourceStamp(sourceOverride!!.index),
                    repo.load(profile).revision?.revision, ProjectStyleDraft("", "", emptyList()), mode, DEFAULT_ANALYSIS_MODEL, "low")
                val state = controller.state(profile).first { !it.busy }
                assertNull("$profile/$pack/$mode local failure", state.error)
                val plan = checkNotNull(state.dnaPlan)
                plan.validate()
                assertEquals(pack, plan.context.sourcePackage)
                assertEquals(mode, plan.mode)
                assertTrue(plan.samples.size in 1..if (mode == DnaAnalysisMode.QUICK) 12 else 24)
                assertFalse(state.hasKey)
                assertEquals(0, calls)
                assertEquals(before, repo.snapshot())
                assertArrayEquals(historicBytes, File(temporary.root, "style_dna/1/plan-${historic.id}.json").readBytes())
            }
        }
        assertFalse(dnaJournal.claim(historic, images))
    }

    @Test fun corruptSharedHistoryFailsClosedAcrossPacksButReportsHistoryInsteadOfDiskSpace() = runBlocking {
        val (historic, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 2)
        val dnaJournal = StyleDnaJournal(analysisFixtureContext(temporary.root))
        assertTrue(dnaJournal.claim(historic, images)); dnaJournal.release(historic)
        val file = File(temporary.root, "style_dna/1/plan-${historic.id}.json")
        val damaged = "{truncated private journal content".toByteArray()
        file.writeBytes(damaged)
        val before = repo.snapshot()
        val controller = newController(dna = true)
        for (pack in listOf("outline.pack", "color.pack")) for (mode in DnaAnalysisMode.entries) {
            sourceOverride = object : CoverageSourceReader by reader {
                override val index = reader.index.copy(stamp = reader.index.stamp.copy(packageName = pack))
            }
            controller.prepareDna(1, ProjectSetupMode.EXTEND, pack, projectSourceStamp(sourceOverride!!.index), null,
                ProjectStyleDraft("", "", emptyList()), mode, DEFAULT_ANALYSIS_MODEL, "low")
            val state = controller.state(1).first { !it.busy }
            assertNull(state.dnaPlan)
            assertEquals("history_unreadable", state.error)
            assertEquals("history_read/invalid_data", state.dnaLocalDiagnostic!!.code)
            assertEquals(0, calls); assertEquals(before, repo.snapshot()); assertArrayEquals(damaged, file.readBytes())
        }
        assertFalse(dnaJournal.claim(historic, images))
        active = 2
        controller.prepareDna(2, ProjectSetupMode.EXTEND, "color.pack", projectSourceStamp(sourceOverride!!.index), null,
            ProjectStyleDraft("", "", emptyList()), DnaAnalysisMode.QUICK, DEFAULT_ANALYSIS_MODEL, "low")
        val fresh = controller.state(2).first { !it.busy }
        assertNull(fresh.error); assertNotNull(fresh.dnaPlan); assertEquals(0, calls)
    }

    @Test fun sourceIoAndPermissionFailuresExposeOnlyFixedStageCodesAndRetryFreely() = runBlocking {
        val controller = newController(dna = true)
        key = false
        val before = repo.snapshot()
        for (failure in listOf(java.io.IOException("secret path and inventory"), SecurityException("private permission details"))) {
            sourceOverride = object : CoverageSourceReader by reader { override val index get() = throw failure }
            controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index), null,
                ProjectStyleDraft("", "", emptyList()), DnaAnalysisMode.STANDARD, DEFAULT_ANALYSIS_MODEL, "low")
            val failed = controller.state(1).first { !it.busy }
            assertEquals("storage", failed.error)
            assertEquals(DnaLocalStage.SAMPLE_RENDER, failed.dnaLocalDiagnostic!!.stage)
            assertEquals(if (failure is SecurityException) DnaLocalCategory.PERMISSION else DnaLocalCategory.IO,
                failed.dnaLocalDiagnostic.category)
            assertFalse(failed.toString().contains("secret")); assertFalse(failed.toString().contains("private"))
            assertNull(failed.dnaPlan); assertEquals(before, repo.snapshot()); assertEquals(0, calls)
        }
        sourceOverride = null
        controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index), null,
            ProjectStyleDraft("", "", emptyList()), DnaAnalysisMode.STANDARD, DEFAULT_ANALYSIS_MODEL, "low")
        val retried = controller.state(1).first { !it.busy }
        assertNull(retried.error); assertNull(retried.dnaLocalDiagnostic); assertNotNull(retried.dnaPlan)
        assertFalse(retried.hasKey); assertEquals(before, repo.snapshot()); assertEquals(0, calls)
    }

    @Test fun firstQuickScanWithoutSetupStampReportsPrerequisiteAndRetryIsLocal() = runBlocking {
        key = false
        val before = repo.snapshot()
        val controller = newController(dna = true)
        val draft = ProjectStyleDraft("", "", emptyList())
        controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", null, null, draft,
            DnaAnalysisMode.QUICK, DEFAULT_ANALYSIS_MODEL, "high")
        val failed = controller.state(1).first { !it.busy }
        assertNull(failed.dnaPlan)
        assertEquals(before, repo.snapshot())
        assertEquals(0, reader.renderCount)
        assertEquals(0, calls)
        // The old enabled guided action reaches require(setupStamp != null), then hides its cause.
        assertEquals("source_required", failed.error)

        controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index), null, draft,
            DnaAnalysisMode.QUICK, DEFAULT_ANALYSIS_MODEL, "high")
        val prepared = controller.state(1).first { !it.busy }
        assertNull(prepared.error)
        val plan = checkNotNull(prepared.dnaPlan)
        plan.validate()
        assertEquals(projectSourceStamp(reader.index), plan.context.setupStamp)
        assertNull(plan.context.baseRevision)
        assertFalse(prepared.hasKey)
        assertEquals(1, plan.endpointMaximum)
        assertEquals(before, repo.snapshot())
        assertEquals(0, calls)
    }

    @Test fun quickPreparationDiagnosesStaleEmptyAndStorageWithoutLeakingExceptionText() = runBlocking {
        val controller = newController(dna = true)
        val before = repo.snapshot()
        val draft = ProjectStyleDraft("", "", emptyList())
        suspend fun attempt(reason: String) {
            controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index), null,
                draft, DnaAnalysisMode.QUICK, DEFAULT_ANALYSIS_MODEL, "low")
            val state = controller.state(1).first { !it.busy }
            assertEquals(reason, state.error)
            assertNull(state.dnaPlan)
            assertEquals(before, repo.snapshot())
            assertEquals(0, calls)
        }
        sourceOverride = object : CoverageSourceReader by reader {
            override val index = reader.index.copy(stamp = reader.index.stamp.copy(versionCode = 8))
        }
        attempt("source_changed")
        controller.releasePreview(1)
        assertNull(controller.state(1).value.error)
        sourceOverride = FixtureReferenceReader(emptyMap())
        attempt("references_empty")
        controller.bindContext(1, ProjectSetupMode.EXTEND, "other.pack")
        assertNull(controller.state(1).value.error)
        sourceOverride = object : CoverageSourceReader by reader {
            override val index get() = throw java.io.IOException("private path /fixture/secret and private content")
        }
        attempt("storage")
        sourceOverride = null
        controller.prepareDna(1, ProjectSetupMode.EXTEND, "fixture.pack", projectSourceStamp(reader.index), null,
            draft, DnaAnalysisMode.QUICK, DEFAULT_ANALYSIS_MODEL, "low")
        val recovered = controller.state(1).first { !it.busy }
        assertNull(recovered.error)
        assertNotNull(recovered.dnaPlan)
        assertEquals(0, calls)
    }

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

    @Test fun sameProfileModeOrChosenSourceChangeBlocksConsentUseAndSave() = runBlocking {
        val controller = newController()
        val prepared = prepare(controller, extend = true)
        controller.bindContext(1, ProjectSetupMode.EXTEND, "other.pack")
        controller.submit(1, prepared.requestId)
        controller.state(1).first { !it.busy }
        assertEquals(0, calls)
        assertFalse(File(temporary.root, "style_analysis").exists())

        val record = result(controller, extend = true)
        val used = controller.use(1, record.requestId, record.hashes.take(5), false)
        controller.bindContext(1, ProjectSetupMode.CREATE, null)
        controller.refresh(1)
        assertEquals(AnalysisValidity.PREPARATION_CHANGED, controller.state(1).value.validity)
        assertTrue(runCatching { controller.use(1, record.requestId, emptyList(), true) }.isFailure)
        assertTrue(runCatching { controller.save(1, null, draft(used)) }.isFailure)

        controller.bindContext(1, ProjectSetupMode.EXTEND, "other.pack")
        assertTrue(runCatching { controller.use(1, record.requestId, emptyList(), true) }.isFailure)
        assertTrue(runCatching { controller.save(1, null, draft(used), ProjectSetupMode.EXTEND, "other.pack") }.isFailure)
        assertNull(repo.load(1).revision)
        assertEquals(AnalysisStatus.RESULT_SAVED, journal.read(1, record.requestId)!!.status)
        assertEquals(1, calls)
    }

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
    @Test fun staleDnaHistoryDoesNotInvalidateACurrentLegacyCandidate() = runBlocking {
        val dnaJournal = StyleDnaJournal(analysisFixtureContext(temporary.root))
        val dnaCoordinator = StyleDnaCoordinator(dnaJournal, StyleDnaGateway { _, _ -> error("Viewing history must not submit") })
        val controller = OwnedStyleAnalysis(scope, repo, journal, ReferenceSampler(),
            StyleAnalysisCoordinator(journal, gateway), { key }, owner, dnaCoordinator)
        val legacy = result(controller)
        val (fixture, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        val stale = fixture.copy(context = checkNotNull(legacy.context).copy(catalogStamp = sha256("old catalog".toByteArray())))
        assertTrue(dnaJournal.claim(stale, images))
        dnaJournal.failed(stale, "STALE_SOURCE", false); dnaJournal.release(stale)
        controller.refresh(1)
        val state = controller.state(1).value
        assertEquals(legacy.requestId, state.record!!.requestId)
        assertEquals(stale.id, state.dnaRecord!!.plan.id)
        assertEquals("The legacy candidate must keep its own current validity", AnalysisValidity.CURRENT, state.validity)
        assertEquals(AnalysisValidity.CURRENT, state.legacyValidity)
        assertEquals(AnalysisValidity.PREPARATION_CHANGED, state.dnaValidity)
        assertEquals(legacy.requestId, controller.use(1, legacy.requestId, legacy.hashes, false).requestId)
        assertEquals(1, calls)
        assertNull(repo.load(1).revision)
    }
    @Test fun rejectedAnalysisKeepsSafeReasonAndNeedsFreshPreparationUuidAndExplicitConfirmation() = runBlocking {
        okhttp3.mockwebserver.MockWebServer().use { server ->
            server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(400).setBody("private payload and identifiers"))
            gateway = OpenAiStyleAnalysisGateway({ java.util.UUID.randomUUID().toString() },
                okhttp3.OkHttpClient(), server.url("/v1/responses"))
            val controller = newController(); val first = prepare(controller, extend = true)
            controller.submit(1, first.requestId)
            val failed = controller.state(1).first { !it.busy }.record!!
            assertEquals(AnalysisStatus.FAILED, failed.status)
            assertEquals("REQUEST_REJECTED:400", failed.reason)
            assertFalse(failed.encode().contains("private payload"))
            assertNull(controller.state(1).value.preparation)
            controller.submit(1, first.requestId); controller.refresh(1); assertEquals(1, server.requestCount)
            val next = prepare(controller, extend = true)
            assertNotEquals(first.requestId, next.requestId); assertEquals(1, server.requestCount)
            server.enqueue(okhttp3.mockwebserver.MockResponse().setBody(analysisFixtureResponse()))
            controller.submit(1, next.requestId)
            assertEquals(AnalysisStatus.RESULT_SAVED, controller.state(1).first { !it.busy }.record!!.status)
            assertEquals(2, server.requestCount); assertNull(repo.load(1).revision)
        }
    }
}
