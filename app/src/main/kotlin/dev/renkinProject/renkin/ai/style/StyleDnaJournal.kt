package dev.renkinProject.renkin.ai.style

import android.content.Context
import android.util.AtomicFile
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal data class HistoricalDnaReceipt(val responseId: String, val usage: AnalysisUsage? = null)

internal enum class DnaStatus { PREPARED, REQUESTING, RESULT_SAVED, REVIEWED, FAILED, UNCERTAIN }
internal data class DnaStepState(val id: String,val status: String = "PENDING",val responseId: String? = null,val usage: AnalysisUsage? = null) {
    fun json()=json("id" to id,"status" to status,"responseId" to responseId,"usage" to usage?.let { json("input" to it.inputTokens,"output" to it.outputTokens) })
}
internal data class StyleDnaRecord(val plan: StyleDnaPlan,val status: DnaStatus,val steps: List<DnaStepState>,
    val reports: List<DnaIconReport> = emptyList(),val result: SavedStyleDna? = null,val reason: String? = null,val createdAt: Long = System.currentTimeMillis()) {
    val submitted get()=steps.count { it.status in setOf("SUBMITTED","DONE") }
    fun encode(): String {
        plan.validate();require(steps.map { it.id }==plan.steps.map { it.id } && steps.all { it.status in setOf("PENDING","SUBMITTED","DONE") })
        require(reports.map { it.sampleId }.distinct().size==reports.size && reports.all { plan.bindings[it.sampleId]==it.sampleHash })
        require(createdAt>0 && (reason==null || reason.matches(Regex("[A-Z0-9_]{1,64}"))))
        if(result!=null) { result.encode();require(result.reports==reports && status in setOf(DnaStatus.RESULT_SAVED,DnaStatus.REVIEWED)) }
        return canonicalJson(json("schema" to 1,"plan" to plan.json(),"status" to status.name,"steps" to JSONArray(steps.map { it.json() }),
            "reports" to JSONArray(reports.map { it.json() }),"result" to result?.dna?.encode(),"reason" to reason,"createdAt" to createdAt))
            .also { require(it.toByteArray().size<=256*1024) }
    }
    companion object {
        fun parse(text: String): StyleDnaRecord {
            val r=strictObject(text,256*1024,14);r.exactKeys(setOf("schema","plan","status","steps","reports","result","reason","createdAt"));require(r.longValue("schema")==1L)
            val steps=r.getJSONArray("steps");require(steps.length() in 1..5);val reports=r.getJSONArray("reports");require(reports.length()<=24)
            val plan=StyleDnaPlan.parse(r.getJSONObject("plan"))
            val parsedReports=(0 until reports.length()).map { DnaIconReport.parse(reports.getJSONObject(it)) }
            val dna=if(r.isNull("result"))null else StyleDnaV1.parse(r.stringValue("result"))
            return StyleDnaRecord(plan,DnaStatus.valueOf(r.stringValue("status")),
                (0 until steps.length()).map { steps.getJSONObject(it).let { s->s.exactKeys(setOf("id","status","responseId","usage"));
                    DnaStepState(s.stringValue("id"),s.stringValue("status"),if(s.isNull("responseId"))null else s.stringValue("responseId"),
                        if(s.isNull("usage"))null else s.getJSONObject("usage").let { u->AnalysisUsage(u.longValue("input"),u.longValue("output")) }) } },
                parsedReports,
                dna?.let { SavedStyleDna(it,parsedReports,plan.model,plan.detail,plan.algorithm,it.recommendedFamily,plan.promptVersion) },if(r.isNull("reason"))null else r.stringValue("reason"),r.longValue("createdAt")).also { it.encode() }
        }
    }
}
/** Independent bounded AtomicFile journal; no credentials and no automatic resume capability. */
internal class StyleDnaJournal(context: Context,private val fault: (String)->Unit = {}) {
    // Context's private directory may have a platform alias. Trust only that parent;
    // never canonicalize away a link in style_dna, a profile, or a record companion.
    private val root=File(context.noBackupFilesDir.canonicalFile,"style_dna").absoluteFile
    private val shared=locks.getOrPut(root.path) { Shared() }
    private class Shared { val lock=Any();val active=mutableSetOf<String>();val invalidated=mutableSetOf<Long>() }
    companion object { private val locks=ConcurrentHashMap<String,Shared>() }
    private fun directory(id: Long): File { require(id>0);return safe(File(root,id.toString())) }
    private fun safe(file: File): File { require(file.canonicalPath==file.absolutePath && file.absolutePath.startsWith(root.parentFile!!.canonicalPath+File.separator));return file }
    private fun recordFile(p: StyleDnaPlan)=safe(File(directory(p.context.profileId),"plan-${p.id}.json"))
    private fun readable(f: File)=safe(f).isFile || safe(File(f.path+".bak")).isFile
    private fun reserved(f: File)=readable(f) || safe(File(f.path+".new")).exists()
    private fun write(f: File,bytes: ByteArray) {
        require(bytes.size<=256*1024);safe(f);check(f.parentFile!!.mkdirs() || f.parentFile!!.isDirectory)
        safe(File(f.path+".bak"));safe(File(f.path+".new"));val atomic=AtomicFile(f);val out=atomic.startWrite()
        try {out.write(bytes);out.flush();out.fd.sync();fault("BEFORE_FINISH");atomic.finishWrite(out) }
        catch(t: Throwable) { atomic.failWrite(out);throw t }
    }
    private fun read(f: File): StyleDnaRecord {
        safe(f);safe(File(f.path+".bak"));safe(File(f.path+".new"));return AtomicFile(f).openRead().use { input->
            val bytes=input.readBytesBounded(256*1024);StyleDnaRecord.parse(Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()) }
    }
    fun record(id: Long,uuid: String): StyleDnaRecord?=synchronized(shared.lock) {
        require(java.util.UUID.fromString(uuid).toString()==uuid);val f=safe(File(directory(id),"plan-$uuid.json"));if(readable(f))read(f) else null
    }
    fun records(id: Long): List<StyleDnaRecord> = synchronized(shared.lock) {
        val files=directory(id).listFiles().orEmpty().map { if(it.name.endsWith(".bak")) File(it.path.removeSuffix(".bak")) else it }
            .filter { it.name.matches(Regex("plan-[a-f0-9-]{36}\\.json")) && readable(it) }.distinctBy { it.path }
        require(files.size<=256)
        files.map { file->val r=read(file)
            require(r.plan.context.profileId==id && file.name=="plan-${r.plan.id}.json")
            if(r.plan.id !in shared.active && r.status in setOf(DnaStatus.PREPARED,DnaStatus.REQUESTING))
                r.copy(status=DnaStatus.UNCERTAIN,reason="INTERRUPTED_NO_REPLAY").also { write(file,it.encode().toByteArray()) } else r }
    }
    fun latest(id: Long)=records(id).maxByOrNull { it.createdAt }
    fun claim(plan: StyleDnaPlan,images: Map<String,ByteArray>): Boolean=synchronized(shared.lock) {
        validateDnaImages(plan,images); val f=recordFile(plan);if(reserved(f))return@synchronized false
        val ledger=safe(File(root,"consumed")); val ids=listOf(plan.id)+plan.steps.map { it.id }
        if(ids.any { reserved(safe(File(ledger,it))) })return@synchronized false
        require(ledger.listFiles().orEmpty().map { it.name.removeSuffix(".bak").removeSuffix(".new") }.distinct().size+ids.size<=8192)
        val owner=directory(plan.context.profileId)
        require(plan.context.profileId !in shared.invalidated)
        val imageDir=File(owner,"images")
        require(imageDir.listFiles().orEmpty().sumOf { it.length() }+images.filter { !File(imageDir,it.key+".png").isFile }.values.sumOf { it.size.toLong() }<=16L*1024*1024)
        require(owner.listFiles().orEmpty().filter { it.name.startsWith("plan-") }.map { it.name.removeSuffix(".bak").removeSuffix(".new") }.distinct().size<256)
        require(owner.walkTopDown().filter { it.isFile }.sumOf { it.length() }+images.values.sumOf { it.size.toLong() }+256*1024<=72L*1024*1024)
        ids.forEach { write(safe(File(ledger,it)),"consumed".toByteArray()) }
        val r=StyleDnaRecord(plan,DnaStatus.PREPARED,plan.steps.map { DnaStepState(it.id) })
        write(f,r.encode().toByteArray());shared.active+=plan.id
        try {
            for((hash,png) in images) {val image=safe(File(owner,"images/$hash.png"));if(image.isFile)require(image.length()==png.size.toLong() && sha256(image.readBytes())==hash)
                else writeImage(image,png) }
            true
        } catch(t: Throwable) {shared.active-=plan.id;throw t}
    }
    private fun writeImage(f: File,bytes: ByteArray) {
        validateAnalysisPng(bytes);safe(f);check(f.parentFile!!.mkdirs() || f.parentFile!!.isDirectory)
        safe(File(f.path+".bak"));safe(File(f.path+".new"));val atomic=AtomicFile(f);val out=atomic.startWrite()
        try {out.write(bytes);out.flush();out.fd.sync();atomic.finishWrite(out)} catch(t:Throwable){atomic.failWrite(out);throw t}
    }
    fun image(id: Long,hash: String): ByteArray=synchronized(shared.lock) {
        dnaHash(hash);val f=safe(File(directory(id),"images/$hash.png"));require(f.length() in 33..MAX_ANALYSIS_PNG.toLong())
        AtomicFile(f).openRead().use { it.readBytesBounded(MAX_ANALYSIS_PNG) }.also { validateAnalysisPng(it);require(sha256(it)==hash) }
    }
    fun submitted(p: StyleDnaPlan,s: DnaStep)=mutate(p) { r->
        check(p.id in shared.active && r.status in setOf(DnaStatus.PREPARED,DnaStatus.REQUESTING))
        require(r.steps.single { it.id==s.id }.status=="PENDING")
        r.copy(status=DnaStatus.REQUESTING,steps=r.steps.map { if(it.id==s.id)it.copy(status="SUBMITTED") else it }) }
    fun completed(p: StyleDnaPlan,s: DnaStep,response: HistoricalDnaReceipt,newReports: List<DnaIconReport>)=mutate(p) { r->
        require(r.steps.single { it.id==s.id }.status=="SUBMITTED")
        r.copy(steps=r.steps.map { if(it.id==s.id)it.copy(status="DONE",responseId=response.responseId,usage=response.usage) else it },reports=r.reports+newReports) }
    fun result(p: StyleDnaPlan,result: SavedStyleDna)=mutate(p) { r->require(r.steps.all { it.status=="DONE" } && r.reports==result.reports)
        r.copy(status=DnaStatus.RESULT_SAVED,result=result) }
    fun failed(p: StyleDnaPlan,reason: String,unknown: Boolean)=mutate(p) { if(it.result!=null)it else it.copy(status=if(unknown)DnaStatus.UNCERTAIN else DnaStatus.FAILED,reason=reason) }
    fun reviewed(p: StyleDnaPlan)=mutate(p) { check(it.result!=null);it.copy(status=DnaStatus.REVIEWED) }
    private fun mutate(p: StyleDnaPlan,action: (StyleDnaRecord)->StyleDnaRecord)=synchronized(shared.lock) {
        val f=recordFile(p);val r=read(f);require(r.plan==p);write(f,action(r).encode().toByteArray()) }
    fun release(p: StyleDnaPlan)=synchronized(shared.lock) {shared.active-=p.id
        if(p.context.profileId in shared.invalidated) { directory(p.context.profileId).deleteRecursively();shared.invalidated-=p.context.profileId };Unit }
    fun discard(p: StyleDnaPlan) {mutate(p) { it.copy(status=DnaStatus.FAILED,result=null,reason="DISCARDED") };collect(p.context.profileId) }
    fun collect(id: Long)=synchronized(shared.lock) {
        val pinned=records(id).filter { it.status==DnaStatus.RESULT_SAVED || it.plan.id in shared.active }.flatMap { it.plan.samples.map { s->s.hash+".png" } }.toSet()
        File(directory(id),"images").listFiles().orEmpty().filter { it.name !in pinned }.forEach { safe(it).delete() };Unit
    }
    fun invalidateProfile(id: Long)=synchronized(shared.lock) {
        if(records(id).any { it.plan.id in shared.active }) shared.invalidated+=id else directory(id).deleteRecursively();Unit }
    fun invalidateAll()=synchronized(shared.lock) { root.listFiles().orEmpty().filter { it.name.matches(Regex("[1-9][0-9]*")) }.forEach { invalidateProfile(it.name.toLong()) };Unit }
}
private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) {val n=read(buffer);if(n<0)break;require(out.size()+n<=limit);out.write(buffer,0,n)};return out.toByteArray()
}
