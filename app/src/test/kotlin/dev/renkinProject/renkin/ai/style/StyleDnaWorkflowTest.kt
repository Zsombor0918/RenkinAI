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
    @get:Rule val temporary=TemporaryFolder()
    private fun journal()=StyleDnaJournal(analysisFixtureContext(temporary.root))
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
