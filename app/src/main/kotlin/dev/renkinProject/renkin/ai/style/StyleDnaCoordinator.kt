package dev.renkinProject.renkin.ai.style

import kotlinx.coroutines.*

internal class StyleDnaCoordinator(val journal: StyleDnaJournal,private val gateway: StyleDnaGateway) {
    suspend fun submit(plan: StyleDnaPlan,images: Map<String,ByteArray>,confirmed: Boolean,current: suspend ()->Boolean,
        progress: suspend (StyleDnaRecord)->Unit = {}): StyleDnaRecord?=withContext(Dispatchers.IO) {
        if(!confirmed || !current())return@withContext null
        validateDnaImages(plan,images)
        // Preflight every image body before reserving consent. Synthesis has its own bounded validation.
        plan.steps.filter { !it.synthesis }.forEach { s->require(dnaBody(DnaCall(plan,s,s.samples.associate { plan.bindings.getValue(it) to images.getValue(plan.bindings.getValue(it)) },emptyList())).toString().toByteArray().size<=5*1024*1024) }
        if(!journal.claim(plan,images))return@withContext journal.record(plan.context.profileId,plan.id)
        try {
            for(step in plan.steps) {
                currentCoroutineContext().ensureActive()
                if(!current()) {journal.failed(plan,"STALE_SOURCE",false);break}
                val prior=checkNotNull(journal.record(plan.context.profileId,plan.id))
                val stepImages=step.samples.associate { plan.bindings.getValue(it) to journal.image(plan.context.profileId,plan.bindings.getValue(it)) }
                val result=gateway.call(DnaCall(plan,step,stepImages,if(step.synthesis)prior.reports else emptyList())) {
                    check(current());journal.submitted(plan,step);progress(checkNotNull(journal.record(plan.context.profileId,plan.id))) }
                if(result is DnaCallResult.Failed) {journal.failed(plan,result.reason,result.billingUnknown);break}
                result as DnaCallResult.Received
                if(step.synthesis) {
                    val dna=StyleDnaV1.parse(result.text);validateDnaSynthesis(dna,prior.reports,images)
                    journal.completed(plan,step,result,emptyList())
                    journal.result(plan,SavedStyleDna(dna,prior.reports,plan.model,plan.detail,plan.algorithm,dna.recommendedFamily))
                } else {
                    val reports=StyleDnaCodec.parseReports(result.text,step.samples.associateWith { plan.bindings.getValue(it) })
                    journal.completed(plan,step,result,reports)
                    if(plan.mode==DnaAnalysisMode.QUICK) {
                        val dna=synthesizeDnaLocally(reports,images);validateDnaSynthesis(dna,reports,images)
                        journal.result(plan,SavedStyleDna(dna,reports,plan.model,plan.detail,plan.algorithm,dna.recommendedFamily))
                    }
                }
                progress(checkNotNull(journal.record(plan.context.profileId,plan.id)))
            }
        } catch(cancel: CancellationException) {
            withContext(NonCancellable) { journal.failed(plan,"CANCELLED_NO_REPLAY",journal.record(plan.context.profileId,plan.id)?.steps?.any { it.status=="SUBMITTED" }==true) };throw cancel
        } catch(_: Exception) { withContext(NonCancellable) { journal.failed(plan,"INVALID_OR_INTERRUPTED",journal.record(plan.context.profileId,plan.id)?.steps?.any { it.status=="SUBMITTED" }==true) } }
        finally {journal.release(plan);journal.collect(plan.context.profileId)}
        journal.record(plan.context.profileId,plan.id)
    }
}
