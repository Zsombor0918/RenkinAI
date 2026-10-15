package dev.renkinProject.renkin.ai.style

import kotlinx.coroutines.*

/** Distinguish validation stages without retaining provider or exception prose. */
private class DnaResultValidation(val reason: String) : IllegalArgumentException()
private inline fun <T> validateDnaResult(reason: String, action: () -> T): T = try { action() }
catch (cancel: CancellationException) { throw cancel }
catch (rejected: DnaReportRejected) { throw DnaResultValidation(rejected.failure.code) }
catch (_: Exception) { throw DnaResultValidation(reason) }

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
                    val dna=validateDnaResult("SYNTHESIS_INVALID") {
                        StyleDnaV1.parse(result.text).also { validateDnaSynthesis(it,prior.reports,images) }
                    }
                    journal.completed(plan,step,result,emptyList())
                    journal.result(plan,SavedStyleDna(dna,prior.reports,plan.model,plan.detail,plan.algorithm,DNA_PACK_STYLE))
                } else {
                    val reports=validateDnaResult("REPORT_SCHEMA_INVALID") {
                        StyleDnaCodec.parseReports(result.text,step.samples.associateWith { plan.bindings.getValue(it) })
                    }
                    journal.completed(plan,step,result,reports)
                    if(plan.mode==DnaAnalysisMode.QUICK) {
                        val dna=validateDnaResult("SYNTHESIS_INVALID") {
                            synthesizeDnaLocally(reports,images).also { validateDnaSynthesis(it,reports,images) }
                        }
                        journal.result(plan,SavedStyleDna(dna,reports,plan.model,plan.detail,plan.algorithm,DNA_PACK_STYLE))
                    }
                }
                progress(checkNotNull(journal.record(plan.context.profileId,plan.id)))
            }
        } catch(cancel: CancellationException) {
            withContext(NonCancellable) { journal.failed(plan,"CANCELLED_NO_REPLAY",journal.record(plan.context.profileId,plan.id)?.steps?.any { it.status=="SUBMITTED" }==true) };throw cancel
        } catch(failure: DnaResultValidation) {
            withContext(NonCancellable) { journal.failed(plan,failure.reason,journal.record(plan.context.profileId,plan.id)?.steps?.any { it.status=="SUBMITTED" }==true) }
        } catch(_: Exception) { withContext(NonCancellable) { journal.failed(plan,"INVALID_OR_INTERRUPTED",journal.record(plan.context.profileId,plan.id)?.steps?.any { it.status=="SUBMITTED" }==true) } }
        finally {journal.release(plan);journal.collect(plan.context.profileId)}
        journal.record(plan.context.profileId,plan.id)
    }
}
