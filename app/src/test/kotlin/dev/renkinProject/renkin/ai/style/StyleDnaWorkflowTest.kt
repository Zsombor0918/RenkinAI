package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.graphics.*
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

internal fun dnaFixturePlan(mode: DnaAnalysisMode = DnaAnalysisMode.STANDARD,count: Int = 24): Pair<StyleDnaPlan,Map<String,ByteArray>> {
    val images=(0 until count).map { analysisFixturePng(2,it) }.associateBy(::sha256)
    require(images.size==count)
    val old=analysisFixtureRequest(1).context
    val plan=StyleDnaPlan(old,mode,images.keys.mapIndexed { i,h->DnaSampleBinding("s%02d".format(i+1),h) },
        ReferenceSampleReport(count,count,0,0,0,false,count<8),"drawing grammar","keep original identity",sha256("draft".toByteArray()))
    return plan to images
}
internal fun dnaFixtureReports(plan: StyleDnaPlan,ids: List<String> = plan.samples.map { it.id })=ids.map { id->
    DnaIconReport.parse(dnaReportFixture(id,plan.bindings.getValue(id)))
}
internal fun dnaReportsBody(reports: List<DnaIconReport>)=canonicalJson(json("schema" to 1,"reports" to JSONArray(reports.map { it.json() })))

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaWorkflowTest {
    @Test fun thirdStandardBatchCountFailureRetainsTwelveReportsAndSpecificReasonWithoutReplay() = runBlocking {
        val (plan, images) = dnaFixturePlan()
        var calls = 0
        val j = journal()
        val coordinator = StyleDnaCoordinator(j, StyleDnaGateway { request, before ->
            before(); calls++
            val reports = dnaFixtureReports(plan, request.step.samples)
            DnaCallResult.Received(dnaReportsBody(if (calls == 3) reports.dropLast(1) else reports), "resp_fixture_$calls", null)
        })
        val record = coordinator.submit(plan, images, true, { true })!!
        assertEquals("REPORT_COUNT", record.reason)
        assertEquals(DnaStatus.UNCERTAIN, record.status)
        assertEquals(12, record.reports.size); assertEquals(3, record.submitted)
        assertEquals(dnaFixtureReports(plan, plan.steps.take(2).flatMap { it.samples }), record.reports)
        assertNull(record.result)
        val durable = record.encode()
        assertEquals(record, journal().latest(1))
        assertEquals(record, coordinator.submit(plan, images, true, { true }))
        assertEquals(durable, j.record(1, plan.id)!!.encode()); assertEquals(3, calls)
    }
    @get:Rule val temporary=TemporaryFolder()
    private fun journal()=StyleDnaJournal(analysisFixtureContext(temporary.root))

    @Test fun trustedParentAliasKeepsAtomicRecoveryAndConsumedReservations() {
        val canonical = journal()
        assertTrue(canonical.records(1).isEmpty())
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 2)
        assertTrue(canonical.claim(plan, images)); canonical.submitted(plan, plan.steps.first()); canonical.release(plan)
        val file = File(temporary.root, "style_dna/1/plan-${plan.id}.json")
        val durable = file.readBytes()
        assertTrue(file.renameTo(File(file.path + ".bak")))
        File(file.path + ".new").writeText("unfinished private bytes")
        val aliased = StyleDnaJournal(analysisFixtureContext(File(temporary.root, ".")))
        val recovered = aliased.records(1).single()
        assertEquals(DnaStatus.UNCERTAIN, recovered.status)
        assertEquals(1, recovered.submitted)
        assertEquals(plan, recovered.plan)
        assertFalse(aliased.claim(plan, images))
        assertFalse(canonical.claim(plan, images))
        assertNotNull(StyleDnaRecord.parse(String(durable)))
        assertEquals(recovered, canonical.latest(1))
        val orphan = plan.copy(id = java.util.UUID.randomUUID().toString(),
            samples = plan.samples) // New plan ID, still consumed step IDs.
        File(file.parentFile, "plan-${orphan.id}.json.new").writeText("interrupted")
        assertFalse(aliased.claim(orphan, images))
        assertEquals(recovered, aliased.latest(1))
    }

    @Test fun canonicalContextStillRejectsLinkedJournalAndProfileDirectories() {
        val outside = temporary.newFolder("outside")
        val root = File(temporary.root, "style_dna")
        java.nio.file.Files.createSymbolicLink(root.toPath(), outside.toPath())
        assertTrue(runCatching { journal().records(1) }.isFailure)
        java.nio.file.Files.delete(root.toPath()) // Only remove the fixture link, never its target.
        assertTrue(root.mkdir())
        java.nio.file.Files.createSymbolicLink(File(root, "1").toPath(), outside.toPath())
        assertTrue(runCatching { journal().records(1) }.isFailure)
        assertTrue(outside.listFiles()!!.isEmpty())
    }

    @Test fun safeRendererSeparatesDeliberateStatefulExclusionsFromDarkVisibleAndBrokenArt() {
        val resources=org.robolectric.RuntimeEnvironment.getApplication().resources
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(20,20,22));val paint=Paint().apply {color=Color.rgb(25,25,28)}
            Canvas(bitmap).drawCircle(128f,128f,65f,paint)
            val result=diagnoseReference(android.graphics.drawable.BitmapDrawable(resources,bitmap))
            assertTrue(result is ReferenceRead.Rendered);assertTrue(referenceFeatures((result as ReferenceRead.Rendered).png).occupancy>.9)
            assertEquals(ReferenceFailure.UNSUPPORTED_STATEFUL,(diagnoseReference(android.graphics.drawable.StateListDrawable()) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.UNSUPPORTED_DYNAMIC,(diagnoseReference(android.graphics.drawable.AnimationDrawable()) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.EMPTY_ALPHA,(diagnoseReference(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.NEAR_EMPTY_ALPHA,(diagnoseReference(android.graphics.drawable.ColorDrawable(Color.argb(10,255,255,255))) as ReferenceRead.Failed).reason)
            val broken=object:android.graphics.drawable.Drawable(){override fun draw(canvas:Canvas){throw IllegalStateException("private resource path")}
                override fun setAlpha(alpha:Int){};override fun setColorFilter(filter:ColorFilter?){};@Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT}
            assertEquals(ReferenceFailure.RENDER_EXCEPTION,(diagnoseReference(broken) as ReferenceRead.Failed).reason)
        } finally {bitmap.recycle()}
    }
    @Test fun boundedSixHundredCandidateScanRetainsTwentyFourSeparateThumbnailsAndMinorityChecks()=runBlocking {
        val art=(0 until 54).associate {"art$it" to analysisFixturePng(it%3,it)}
        for(size in listOf(50,150,500,1500)) for(cap in listOf(300,600)) {
            val mapping=(0 until size).associate {"entry%04d/Main".format(it) to "art${it%54}"}
            val before=System.nanoTime();val runtime=Runtime.getRuntime();val heap=runtime.totalMemory()-runtime.freeMemory()
            val r=ReferenceSampler().sample(FixtureReferenceReader(art,mapping),cap,24)
            assertEquals(minOf(size,cap),r.report.examined);r.report.validate();assertTrue(r.samples.size<=24)
            assertTrue(r.samples.sumOf {it.png.size.toLong()}<=8L*1024*1024)
            println("DNA_SCAN mappings=$size cap=$cap examined=${r.report.examined} distinct=${r.report.valid} selected=${r.samples.size} elapsedMs=${(System.nanoTime()-before)/1000000} heapDeltaBytes=${runtime.totalMemory()-runtime.freeMemory()-heap} nativePeak=NOT_MEASURED")
        }
    }

    @Test fun standardHasFourSixImageReadsThenOneIndependentSynthesisWithDurableSteps()=runBlocking {
        val (plan,images)=dnaFixturePlan();val j=journal();var calls=0
        val c=StyleDnaCoordinator(j,StyleDnaGateway { request,before->
            assertEquals(calls, j.record(1,plan.id)!!.submitted);before();calls++
            assertEquals(calls,j.record(1,plan.id)!!.submitted)
            if(request.step.synthesis) {
                assertEquals(24,request.reports.size);assertTrue(request.images.isEmpty())
                DnaCallResult.Received(synthesizeDnaLocally(request.reports,images).encode(),"resp_synthesis",AnalysisUsage(20,30))
            } else {
                assertEquals(6,request.images.size);assertTrue(request.reports.isEmpty())
                DnaCallResult.Received(dnaReportsBody(dnaFixtureReports(plan,request.step.samples)),"resp_group$calls",AnalysisUsage(10,15))
            }
        })
        assertNull(c.submit(plan,images,false,{true}));assertEquals(0,calls);assertFalse(File(temporary.root,"style_dna").exists())
        val result=c.submit(plan,images,true,{true})!!
        assertEquals(5,calls);assertEquals(24,result.reports.size);assertEquals(DnaStatus.RESULT_SAVED,result.status)
        assertEquals(5,result.submitted);assertEquals(5,result.steps.count {it.status=="DONE"})
        assertEquals(result,journal().latest(1));c.submit(plan,images,true,{true});assertEquals(5,calls)
        assertEquals(StyleDnaPlan.parse(plan.json()),plan);assertEquals(result,StyleDnaRecord.parse(result.encode()))
    }
    @Test fun quickHasOneSubmissionAndLocalSynthesisWithoutSilentSecondPass()=runBlocking {
        val (plan,images)=dnaFixturePlan(DnaAnalysisMode.QUICK,12);var calls=0
        val c=StyleDnaCoordinator(journal(),StyleDnaGateway { request,before->before();calls++;assertFalse(request.step.synthesis)
            DnaCallResult.Received(dnaReportsBody(dnaFixtureReports(plan)),"resp_quick",null) })
        val r=c.submit(plan,images,true,{true})!!
        assertEquals(1,calls);assertEquals(12,r.reports.size);assertEquals(DnaStatus.RESULT_SAVED,r.status)
    }
    @Test fun completedHttpReportSchemaAndSynthesisFailuresKeepDistinctStagesAndConsumedRights() = runBlocking {
        for (fault in listOf("missing", "hash", "extra_field", "extra_report", "duplicate", "wrong_type",
            "quick_local_synthesis", "standard_schema", "standard_evidence")) {
            val standard = fault.startsWith("standard")
            val (plan, images) = dnaFixturePlan(if (standard) DnaAnalysisMode.STANDARD else DnaAnalysisMode.QUICK,
                if (standard) 24 else 12)
            okhttp3.mockwebserver.MockWebServer().use { server ->
                for (step in plan.steps) {
                    val reports = dnaFixtureReports(plan, step.samples)
                    val text = when {
                        step.synthesis && fault == "standard_schema" -> "{}"
                        step.synthesis -> {
                            val valid = synthesizeDnaLocally(dnaFixtureReports(plan), images)
                            valid.copy(families = valid.families.map { it.copy(tile = DnaTile.ROUND) }).encode()
                        }
                        fault == "quick_local_synthesis" -> dnaReportsBody(reports.mapIndexed { i, r ->
                            r.copy(familyVote = DnaFamilyVote.entries[i % DnaFamilyVote.entries.size],
                                tileVote = DnaTile.entries[i / DnaFamilyVote.entries.size])
                        }) // Schema-valid reports with >8 distinct groups exceed the local synthesis bound.
                        standard -> dnaReportsBody(reports.reversed())
                        else -> org.json.JSONObject(dnaReportsBody(reports)).also { root ->
                            val a = root.getJSONArray("reports")
                            when (fault) {
                                "missing" -> a.remove(a.length() - 1)
                                "hash" -> a.getJSONObject(0).put("sample_hash", "f".repeat(64))
                                "extra_field" -> a.getJSONObject(0).put("untrusted", "private provider prose")
                                "extra_report" -> a.put(a.getJSONObject(0))
                                "duplicate" -> a.put(1, a.getJSONObject(0))
                                "wrong_type" -> a.getJSONObject(0).put("schema", "1")
                            }
                        }.toString()
                    }
                    server.enqueue(okhttp3.mockwebserver.MockResponse().setBody(json("id" to "resp_simulated_${step.id}",
                        "status" to "completed", "output" to JSONArray().put(json("type" to "message", "status" to "completed",
                            "content" to JSONArray().put(json("type" to "output_text", "text" to text))))).toString()))
                }
                val gateway = OpenAiStyleDnaGateway({ "offline_fixture_no_real_credential" }, okhttp3.OkHttpClient(), server.url("/v1/responses"))
                val coordinator = StyleDnaCoordinator(journal(), gateway)
                val result = coordinator.submit(plan, images, true, { true })!!
                val synthesis = standard || fault == "quick_local_synthesis"
                assertEquals(fault, if (synthesis) "SYNTHESIS_INVALID" else "REPORT_SCHEMA_INVALID", result.reason)
                assertEquals(fault, if (fault == "quick_local_synthesis") DnaStatus.FAILED else DnaStatus.UNCERTAIN, result.status)
                assertNull(result.result)
                assertEquals(if (standard) 24 else if (fault == "quick_local_synthesis") 12 else 0, result.reports.size)
                assertEquals(if (standard) 5 else 1, result.submitted)
                assertEquals(result.submitted, server.requestCount)
                val durable = result.encode()
                assertFalse(durable.contains("private provider prose"))
                val recreated = StyleDnaCoordinator(journal(), gateway)
                assertEquals(result, recreated.submit(plan, images, true, { true }))
                assertEquals(durable, journal().record(1, plan.id)!!.encode())
                assertEquals(result.submitted, server.requestCount)
            }
        }
    }

    @Test fun partialSixImageFailureRetainsOnlyCompletedReportsAndCannotResumeOrReplay()=runBlocking {
        val (plan,images)=dnaFixturePlan();var calls=0
        val c=StyleDnaCoordinator(journal(),StyleDnaGateway { request,before->before();calls++
            DnaCallResult.Received(dnaReportsBody(dnaFixtureReports(plan,if(calls==2)request.step.samples.dropLast(1) else request.step.samples)),"resp_$calls",null) })
        val r=c.submit(plan,images,true,{true})!!
        assertEquals(2,calls);assertEquals(6,r.reports.size);assertNull(r.result);assertEquals(DnaStatus.UNCERTAIN,r.status)
        c.submit(plan,images,true,{true});assertEquals(2,calls)
        assertEquals(r,journal().latest(1))
    }
    @Test fun cancellationAndColdRecoveryKeepUniqueRightsConsumedAcrossOwners()=runBlocking {
        val (plan,images)=dnaFixturePlan();val j=journal();assertTrue(j.claim(plan,images));j.submitted(plan,plan.steps.first());j.release(plan)
        val recovered=journal().latest(1)!!;assertEquals(DnaStatus.UNCERTAIN,recovered.status);assertEquals(1,recovered.submitted)
        assertFalse(journal().claim(plan,images))
        val foreign=plan.copy(context=plan.context.copy(profileId=2),id=java.util.UUID.randomUUID().toString())
        assertFalse(journal().claim(foreign,images))
        j.invalidateProfile(1);assertNull(journal().latest(1));assertFalse(journal().claim(plan,images))
    }
    @Test fun staleContextAndMarkerFailureSendNothingAndFreshConsentIsRequired()=runBlocking {
        val (plan,images)=dnaFixturePlan();var calls=0
        val gateway=StyleDnaGateway { _,before->before();calls++;DnaCallResult.Failed("RATE_LIMITED") }
        assertNull(StyleDnaCoordinator(journal(),gateway).submit(plan,images,true,{false}));assertEquals(0,calls)
        val fault=StyleDnaJournal(analysisFixtureContext(temporary.root)) {throw java.io.IOException("private path must not be logged")}
        assertTrue(runCatching {StyleDnaCoordinator(fault,gateway).submit(plan,images,true,{true})}.isFailure);assertEquals(0,calls)
        assertEquals(1,plan.steps.count {it.synthesis});assertEquals(5,plan.endpointMaximum)
        assertNotEquals(plan.fingerprint,plan.copy(detail="high").fingerprint)
        assertNotEquals(plan.fingerprint,plan.copy(description="changed").fingerprint)
    }
    @Test fun strictPerImageBindingAndEnumsRejectFabricatedUnknownPartialOrOverflowingObservations() {
        val (plan,_)=dnaFixturePlan(DnaAnalysisMode.QUICK,2);val original=dnaReportsBody(dnaFixtureReports(plan))
        val r=org.json.JSONObject(original);r.getJSONArray("reports").getJSONObject(0).put("sample_hash","f".repeat(64))
        assertTrue(runCatching {StyleDnaCodec.parseReports(r.toString(),plan.bindings)}.isFailure)
        for(change in listOf("BAD_FAMILY","null")) {val p=org.json.JSONObject(original);p.getJSONArray("reports").getJSONObject(0).put("family_vote",change)
            assertTrue(runCatching {StyleDnaCodec.parseReports(p.toString(),plan.bindings)}.isFailure) }
        val p=org.json.JSONObject(original);p.getJSONArray("reports").getJSONObject(0).getJSONObject("traits").getJSONObject("corners").put("value","x".repeat(241))
        assertTrue(runCatching {StyleDnaCodec.parseReports(p.toString(),plan.bindings)}.isFailure)
    }
    @Test fun uncertainPaletteCannotForceDarkTileOrGradientAndReferenceIdentityNeverEntersRecipe() {
        val (plan,images)=dnaFixturePlan(DnaAnalysisMode.QUICK,12)
        val reports=dnaFixtureReports(plan).map {it.copy(traits=it.traits+ ("logo_silhouette" to DnaObservation("FOREIGN_SOURCE_LOGO",DnaProvenance.OBSERVED))) }
        val dna=synthesizeDnaLocally(reports,images)
        val f=dna.families.first();assertEquals(PackColorPolicy.ADAPT_BRAND_COLORS,f.colorPolicy)
        assertEquals(DnaTile.NONE,f.tile);assertEquals(DnaDepth.NONE,f.depth)
        val recipe=StyleDnaCompiler.recipe(dna,DnaSymbolType.WORDMARK,f.id)
        assertFalse(recipe.contains("FOREIGN_SOURCE_LOGO"));assertTrue(recipe.contains("Preserve original target hues"));assertTrue(recipe.contains("wordmark") || recipe.contains("Target-type"))
        assertTrue(runCatching {validateDnaSynthesis(dna.copy(families=listOf(f.copy(colorPolicy=PackColorPolicy.MONOCHROME))),reports,images)}.isFailure)
        assertTrue(runCatching {validateDnaSynthesis(dna.copy(families=listOf(f.copy(tile=DnaTile.ROUND,depth=DnaDepth.GRADIENT))),reports,images)}.isFailure)
    }
    @Test fun sixOfflineGoldenPackCasesKeepPoliciesFamiliesAndHeldOutSourceSymbolsSeparate() {
        for(case in listOf("lux_mixed","basit_outline","monochrome","duotone","wordmark","variable_brand")) {
            val reports=mutableListOf<DnaIconReport>();val images=linkedMapOf<String,ByteArray>()
            repeat(12) { i->
                val b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
                val canvas=Canvas(b);val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                val branded=case in setOf("lux_mixed","variable_brand")
                if(branded) {paint.color=Color.rgb(25,25,28);canvas.drawRoundRect(12f,12f,244f,244f,20f,20f,paint)}
                paint.color=if(branded)Color.HSVToColor(floatArrayOf(i*30f,.8f,.9f)) else Color.WHITE
                paint.style=if(case=="basit_outline")Paint.Style.STROKE else Paint.Style.FILL;paint.strokeWidth=8f
                if(case=="wordmark") {paint.textSize=52f;canvas.drawText("ABC$i",28f,140f,paint)}
                else canvas.drawCircle(128f,128f,(40+i).toFloat(),paint)
                if(case=="duotone") {paint.color=Color.CYAN;canvas.drawRect(60f,90f,110f,170f,paint)}
                val png=java.io.ByteArrayOutputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it);it.toByteArray()};b.recycle()
                val h=sha256(png);images[h]=png;val id="s%02d".format(i+1)
                val r=DnaIconReport.parse(dnaReportFixture(id,h,if(case=="lux_mixed" && i>=9)"OUTLINE" else "TILE"))
                reports+=r.copy(symbolType=if(case=="wordmark")DnaSymbolType.WORDMARK else DnaSymbolType.GEOMETRIC_PICTOGRAM,
                    tileVote=if(branded)DnaTile.ROUND else DnaTile.NONE)
            }
            val start=System.nanoTime();val runtime=Runtime.getRuntime();val heap=runtime.totalMemory()-runtime.freeMemory()
            val dna=synthesizeDnaLocally(reports,images)
            val policy=dna.families.first().colorPolicy
            assertEquals(case,when(case){"duotone"->PackColorPolicy.DUOTONE;"lux_mixed","variable_brand"->PackColorPolicy.ADAPT_BRAND_COLORS;else->PackColorPolicy.MONOCHROME},policy)
            assertEquals(case,if(case=="lux_mixed")2 else 1,dna.families.size)
            val heldOut="held_out_source_identity";assertFalse(StyleDnaCompiler.recipe(dna,DnaSymbolType.UNKNOWN).contains(heldOut))
            println("DNA_GOLDEN case=$case analyzed=${reports.size} families=${dna.families.size} policy=$policy elapsedMs=${(System.nanoTime()-start)/1000000} heapDeltaBytes=${runtime.totalMemory()-runtime.freeMemory()-heap} visualQuality=NOT_TESTED")
        }
    }
}
