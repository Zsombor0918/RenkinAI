package dev.renkinProject.renkin.ui.project

import android.app.Application
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
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
    private fun controller(sessionOwner: TrialSessionOwner = owner) = OwnedStyleTrials(scope, f.repo, f.journal, StyleTrialCoordinator(f.journal, gateway, f.repo), { key }, sessionOwner)
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
    @Test fun reloadQueuedDuringPrepareKeepsCurrentPlanAndStagedOriginalWithoutSubmission() = runBlocking {
        val entered=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        val produced=CompletableDeferred<StyleTrialPlan>(); val reloadStarted=CompletableDeferred<Unit>()
        val c=controller(object : TrialSessionOwner by owner {
            override suspend fun original(key: String): TrialOriginal {
                entered.complete(Unit); release.await(); return owner.original(key)
            }
        })
        loaded(c)
        val observe=launch(Dispatchers.Unconfined,start=CoroutineStart.UNDISPATCHED) {
            c.state(1).collect { state ->
                state.plan?.let { produced.complete(it) }
                if(produced.isCompleted && state.busy && state.preparation==null) reloadStarted.complete(Unit)
            }
        }
        try {
            c.prepare(1,listOf("a/Main"),false)
            withTimeout(10000) { entered.await() }
            assertTrue(c.state(1).value.busy)
            c.load(1,ProjectSetupMode.CREATE,null) // Exact hosted order: local load queues behind Prepare.
            release.complete(Unit)
            val frozen=withTimeout(10000) { produced.await() }
            withTimeout(10000) { reloadStarted.await() }
            val after=idle(c)
            assertNull(after.error)
            assertEquals("Read-only queued load must retain a still-current prepared plan",frozen,after.plan)
            assertArrayEquals(f.originals.getValue("a/Main").png,c.preview(1,frozen.inputs.single().originalHash))
            assertEquals(0,calls); assertTrue(f.journal.records(1).isEmpty())
            c.submit(1,frozen.id);assertNull(idle(c).error)
            assertEquals(1,calls);assertEquals(1,f.journal.read(1,frozen.id)!!.items.single().endpointCalls)
        } finally { release.complete(Unit); observe.cancelAndJoin() }
    }
    @Test fun readOnlyReloadInvalidatesChangedOriginalStyleEpochAndRoute() = runBlocking {
        for(change in listOf("original","style","epoch","route")) {
            val c=controller(); val frozen=prepared(c)
            when(change) {
                "original" -> f.originals["a/Main"]=f.originals.getValue("b/Main")
                "style" -> { val saved=f.repo.load(1); f.repo.saveStyle(1,saved.revision!!.revision,"changed construction","",emptyList(),"CREATE",null,null) }
                "epoch" -> f.journal.invalidateAll()
                "route" -> c.bindContext(1,ProjectSetupMode.EXTEND,"source.pack")
            }
            c.load(1,if(change=="route") ProjectSetupMode.EXTEND else ProjectSetupMode.CREATE,if(change=="route") "source.pack" else null)
            assertNull(idle(c).plan)
            assertNull(c.preview(1,frozen.inputs.single().originalHash))
            c.submit(1,frozen.id); assertEquals(0,calls);assertTrue(f.journal.records(1).isEmpty())
            c.release(1)
        }
    }
    @Test fun decliningWhileReloadRechecksOriginalCannotResurrectThePlan() = runBlocking {
        val entered=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        var pause=false
        val c=controller(object : TrialSessionOwner by owner {
            override suspend fun original(key: String): TrialOriginal {
                if(pause) { entered.complete(Unit);release.await() }
                return owner.original(key)
            }
        })
        val frozen=prepared(c);pause=true
        c.load(1,ProjectSetupMode.CREATE,null)
        withTimeout(10000) { entered.await() }
        assertTrue(c.state(1).value.busy)
        assertEquals(frozen,c.state(1).value.plan)
        c.cancelPreparation(1);release.complete(Unit)
        assertNull(idle(c).plan);assertNull(c.preview(1,frozen.inputs.single().originalHash))
        c.submit(1,frozen.id);assertEquals(0,calls);assertTrue(f.journal.records(1).isEmpty())
    }
    @Test fun dnaFamilyReferenceMismatchBlocksAndChangedChoiceInvalidatesFrozenOriginalFirstRequest() = runBlocking {
        val (analysis, art) = dnaFixturePlan(DnaAnalysisMode.QUICK,12)
        val reports = dnaFixtureReports(analysis).mapIndexed { index, r -> if(index < 6) r else r.copy(familyVote=DnaFamilyVote.SOLID) }
        val raw = synthesizeDnaLocally(reports,art)
        val dna = SavedStyleDna(raw,reports,analysis.model,analysis.detail,analysis.algorithm,"family1")
        val hashes = dnaReferenceHashes(dna,"family1",analysis.samples.map { it.hash })
        f.repo.importReferences(1,hashes.map { hash -> { java.io.ByteArrayInputStream(art.getValue(hash)) } })
        val before = f.repo.load(1)
        val meta = before.metadata!!
        val analysisContext = AnalysisContext(1,"CREATE",null,null,null,meta.version,sha256(meta.selectionJson.toByteArray()),
            projectCatalogStamp(f.targets),before.revision!!.revision)
        val payload = dna.encode()
        val evidence = ReviewedAnalysisEvidence(analysis.id,analysis.fingerprint,analysis.promptHash,analysisContext,analysis.model,
            reports.map { it.sampleHash },hashes,"resp_fixture",sha256(payload.toByteArray()),System.currentTimeMillis(),false,false)
        f.repo.saveAnalyzedStyle(1,before.revision!!.revision,meta.version,StyleDnaCompiler.specification(dna),"",hashes,payload,evidence)
        val requests = mutableListOf<IconGenerationRequest>()
        gateway = f.successful { calls++; requests += it }
        val c = controller()
        val first = prepared(c)
        assertEquals(hashes,first.referenceHashes)
        c.dnaChoice(1,"a/Main","family2",DnaSymbolType.WORDMARK); idle(c)
        c.submit(1,first.id); assertEquals(0,calls)
        c.prepare(1,listOf("a/Main"),false)
        assertEquals("dna_reference_mismatch",idle(c).error); assertNull(c.state(1).value.plan); assertEquals(0,calls)
        c.dnaChoice(1,"a/Main",DNA_PACK_STYLE,DnaSymbolType.WORDMARK); idle(c)
        c.prepare(1,listOf("a/Main"),false)
        val frozen = idle(c).plan!!
        assertNotEquals(first.context.dnaFingerprint,frozen.context.dnaFingerprint)
        assertTrue(frozen.drawingRecipes.getValue("a/Main").contains("WORDMARK"))
        c.submit(1,frozen.id); assertNull(idle(c).error)
        val request = requests.single()
        assertArrayEquals(f.originals.getValue("a/Main").png,request.originalPng)
        assertEquals(hashes,request.referencePngs.map(::sha256))
        val settings = AiSettings(request.provider,request.specification,request.model,request.quality,
            request.referencePngs.indices.map { "img_0_$it.png" },request.appInstruction)
        val body = buildImageEditRequest("offline_fixture_not_real",settings,listOf(request.originalPng)+request.referencePngs,
            request.composedPrompt).body as okhttp3.MultipartBody
        val parts = body.parts.filter { it.headers?.get("Content-Disposition")?.contains("name=\"image[]\"") == true }
        (listOf(request.originalPng)+request.referencePngs).forEachIndexed { index, bytes ->
            assertArrayEquals(bytes,okio.Buffer().also { parts[index].body.writeTo(it) }.readByteArray()) }
        assertEquals(1,calls)
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
        var sourceB = f.repo.referenceBytes(1, f.repo.load(1).references.single().hash)
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
                    val newReference = f.repo.importReference(1, java.io.ByteArrayInputStream(projectTestPng(android.graphics.Color.CYAN)))
                    f.repo.saveStyle(1, old.revision!!.revision, old.revision.specification, old.revision.userInstruction,
                        listOf(newReference.hash), "EXTEND", "source.other", projectSourceStamp(f.index))
                    sourceB = f.repo.referenceBytes(1, newReference.hash)
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

    @Test fun openingAnotherProjectDuringAConfirmedRequestEventuallyLoadsItLocally() = runBlocking {
        f.repo.saveStyle(2, null, "other manual style", "", emptyList(), "CREATE", null, null)
        f.repo.saveSelection(2, 1, ProjectSelection("CREATE", null, null,
            projectCatalogStamp(f.targets), listOf("b/Main")))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        gateway = IconGenerationGateway { request ->
            calls++; request.beforeEndpoint(1); entered.complete(Unit); release.await()
            IconGenerationOutcome.Generated(projectTestPng(size = 1024), request.requestId,
                request.provider, 1024, 1024, 1)
        }
        val c = controller()
        val plan = prepared(c)
        c.submit(1, plan.id)
        withTimeout(10000) { entered.await() }
        mutex.withLock { active = 2 }
        c.load(2, ProjectSetupMode.CREATE, null)
        assertNull(c.state(2).value.plan)
        release.complete(Unit)
        idle(c)
        val other = withTimeout(10000) { c.state(2).first { it.preparation != null && !it.busy } }
        assertNull(other.error)
        assertEquals(listOf("b/Main"), other.preparation!!.candidates.map { it.target.key })
        assertEquals(1, calls)
        assertTrue(f.journal.records(2).isEmpty())
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
