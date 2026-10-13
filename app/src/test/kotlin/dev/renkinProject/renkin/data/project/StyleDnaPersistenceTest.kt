package dev.renkinProject.renkin.data.project

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.transfer.BackupManager
import dev.renkinProject.renkin.data.watch.*
import dev.renkinProject.renkin.ui.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaPersistenceTest {
    @get:Rule val temp=TemporaryFolder()
    private lateinit var context: Context
    private lateinit var source: RenkinPackDatabase
    private lateinit var target: RenkinPackDatabase
    private lateinit var sourceWatch: WatchDatabase
    private lateinit var targetWatch: WatchDatabase
    private lateinit var repo: ProjectAiRepository
    private lateinit var restored: ProjectAiRepository
    @Before fun setup()=runBlocking {
        context=analysisFixtureContext(temp.root)
        source=Room.inMemoryDatabaseBuilder(context,RenkinPackDatabase::class.java).build()
        target=Room.inMemoryDatabaseBuilder(context,RenkinPackDatabase::class.java).build()
        sourceWatch=Room.inMemoryDatabaseBuilder(context,WatchDatabase::class.java).build()
        targetWatch=Room.inMemoryDatabaseBuilder(context,WatchDatabase::class.java).build()
        repo=ProjectAiRepository(source,ProjectAssetStore(context));restored=ProjectAiRepository(target,ProjectAssetStore(context))
        RenkinPackRepository(source).replaceEverything(listOf(Profile(1,"Default")),emptyList())
        RenkinPackRepository(target).replaceEverything(listOf(Profile(1,"Keep")),emptyList())
    }
    @After fun close(){source.close();target.close();sourceWatch.close();targetWatch.close();File(context.filesDir,"project_ai_assets").deleteRecursively()}
    private fun manager(db: RenkinPackDatabase,w: WatchDatabase)=BackupManager(context,RenkinPackRepository(db),WatchRepository(w))
    private suspend fun save(): SavedProjectStyle {
        val (p,images)=dnaFixturePlan(DnaAnalysisMode.QUICK,12);val reports=dnaFixtureReports(p)
        val saved=SavedStyleDna(synthesizeDnaLocally(reports,images),reports,p.model,p.detail,p.algorithm,"family1")
        val assets=repo.importReferences(1,p.samples.take(5).map { s->{ByteArrayInputStream(images.getValue(s.hash))} })
        val payload=saved.encode()
        val evidence=ReviewedAnalysisEvidence(p.id,p.fingerprint,p.promptHash,p.context,p.model,reports.map {it.sampleHash},assets.map {it.hash},
            "resp_dna",sha256(payload.toByteArray()),System.currentTimeMillis(),false,false)
        return repo.saveAnalyzedStyle(1,null,0,StyleDnaCompiler.specification(saved),"manual instruction preserved",assets.map {it.hash},payload,evidence)
    }
    @Test fun taggedGrammarHistoryAndLegacyV1RemainImmutableAndFullBackupPreservesBoth()=runBlocking {
        val first=save();val payload=first.revision!!.analysisProfileJson!!
        assertEquals(21,source.openHelper.readableDatabase.version)
        assertEquals(payload,SavedStyleDna.parse(payload).encode());assertEquals(12,SavedStyleDna.parse(payload).reports.size)
        repo.saveStyle(1,1,"legacy manual text", "",emptyList());assertEquals(payload,repo.history(1).first().analysisProfileJson)
        val bytes=ByteArrayOutputStream().also { output->manager(source,sourceWatch).exportBackup {output} }.toByteArray()
        manager(target,targetWatch).importBackup {ByteArrayInputStream(bytes)}
        assertEquals("legacy manual text",restored.load(1).revision!!.specification)
        val dna=restored.history(1).first();assertEquals(payload,dna.analysisProfileJson)
        assertTrue(ReviewedAnalysisEvidence.parse(dna.analysisEvidenceJson!!).needsRevalidation)
        assertArrayEquals(repo.referenceBytes(1,first.references.first().hash),restored.referenceBytes(1,first.references.first().hash))
        assertNull(SavedStyleDna.optional(analysisFixtureStyle().encode()))
    }
    @Test fun shareRekeysTypedEvidenceWithoutTransferringPaidRightsAndRejectsCorruptGrammar()=runBlocking {
        val saved=save();val bytes=ByteArrayOutputStream().also {output->manager(source,sourceWatch).exportProfile(1){output}}.toByteArray()
        val id=manager(target,targetWatch).importFile {ByteArrayInputStream(bytes)}.importedProfileId!!
        assertNotEquals(1L,id);val r=restored.load(id);assertEquals(saved.revision!!.analysisProfileJson,r.revision!!.analysisProfileJson)
        val e=ReviewedAnalysisEvidence.parse(r.revision!!.analysisEvidenceJson!!);assertEquals(id,e.context.profileId);assertTrue(e.needsRevalidation)
        val corrupt=org.json.JSONObject(saved.revision!!.analysisProfileJson!!).put("schema",2).toString()
        val before=repo.snapshot();assertTrue(runCatching {repo.saveAnalyzedStyle(1,1,1,"bad","",saved.references.map {it.hash},corrupt,
            ReviewedAnalysisEvidence.parse(saved.revision!!.analysisEvidenceJson!!))}.isFailure);assertEquals(before,repo.snapshot())
    }
    @Test fun actualOwnedStandardFlowNeedsConsentAndExplicitLocalSaveAndStopsOnSourceChange()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        try {
            val pngs=(0 until 50).associate {"draw$it" to analysisFixturePng(2,it)};val reader=FixtureReferenceReader(pngs)
            var calls=0;val j=StyleAnalysisJournal(context);val dj=StyleDnaJournal(context)
            val gateway=StyleDnaGateway { call,before->before();calls++
                val images=call.plan.samples.associate {it.hash to dj.image(1,it.hash)}
                DnaCallResult.Received(if(call.step.synthesis)synthesizeDnaLocally(call.reports,images).encode()
                    else dnaReportsBody(dnaFixtureReports(call.plan,call.step.samples)),"resp_$calls",null) }
            val owner=object:AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long,operation:suspend()->T):T {check(id==1L);return operation()}
                override suspend fun catalogStamp()=sha256("catalog".toByteArray())
                override suspend fun reader(source: String,referenceIndex: Boolean)=reader
            }
            val controller=OwnedStyleAnalysis(scope,repo,j,ReferenceSampler(),StyleAnalysisCoordinator(j,StyleAnalysisGateway {error("legacy must not submit")}),{true},owner,StyleDnaCoordinator(dj,gateway))
            val draft=ProjectStyleDraft("","manual instruction",emptyList())
            controller.prepareDna(1,ProjectSetupMode.EXTEND,"fixture.pack",projectSourceStamp(reader.index),null,draft,DnaAnalysisMode.STANDARD,DEFAULT_ANALYSIS_MODEL,"low")
            controller.state(1).first {!it.busy};val plan=controller.state(1).value.dnaPlan!!
            assertEquals(0,calls);assertNull(repo.load(1).revision);assertTrue(plan.samples.size in 12..24)
            controller.submitDna(1,plan.id);controller.state(1).first {!it.busy};val r=controller.state(1).value.dnaRecord!!
            assertEquals(DnaStatus.RESULT_SAVED,r.status);assertEquals(plan.endpointMaximum,calls);assertNull(repo.load(1).revision)
            val saved=controller.saveDna(1,plan.id,r.result!!.dna.encode(),r.result!!.selectedFamily,plan.samples.take(5).map {it.hash},null,draft)
            assertEquals("manual instruction",saved.revision!!.userInstruction);assertNotNull(SavedStyleDna.optional(saved.revision!!.analysisProfileJson));assertEquals(plan.endpointMaximum,calls)
            assertEquals(DnaStatus.REVIEWED,dj.latest(1)!!.status)
        } finally {scope.cancel()}
    }
}
