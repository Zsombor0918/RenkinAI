package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

internal interface TrialSessionOwner {
    suspend fun <T> owned(id: Long, action: suspend () -> T): T
    suspend fun targets(): List<CoverageTarget>
    suspend fun reader(source: String): CoverageSourceReader
    suspend fun original(key: String): TrialOriginal
    suspend fun settings(): AiSettings
}

/** Local prerequisite codes only: never expose exception, source or provider prose. */
private class TrialPrerequisite(val reason: String) : IllegalStateException()
private fun trialRequire(condition: Boolean, reason: String) {
    if (!condition) throw TrialPrerequisite(reason)
}

/** One controller in the existing VM. Paid jobs are never composition/restoration effects. */
internal class OwnedStyleTrials(private val scope: CoroutineScope, private val repository: ProjectAiRepository,
    private val journal: StyleTrialJournal, private val coordinator: StyleTrialCoordinator,
    private val hasKey: (String) -> Boolean, private val owner: TrialSessionOwner) : ProjectTrialBackend {
    private val states = ConcurrentHashMap<Long, MutableStateFlow<ProjectTrialState>>()
    private val routes = ConcurrentHashMap<Long, Pair<ProjectSetupMode, String?>>()
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val dnaChoices = ConcurrentHashMap<Long, Map<String, Pair<String, DnaSymbolType>>>()
    private val chosenOptions = ConcurrentHashMap<Long, Triple<String, String, String>>()
    private val images = ConcurrentHashMap<Long, Map<String, ByteArray>>()
    private val verifiedOriginalPlans = ConcurrentHashMap<Long, String>()
    // Coalesce local reads blocked by another profile's already-confirmed job.
    // These entries can only call load/recovery, never prepare or submit a request.
    private val pendingLoads = ConcurrentHashMap<Long, Pair<ProjectSetupMode, String?>>()
    private fun matchesRoute(c: TrialContext) = routes[c.profileId]?.let { it.first.name == c.mode && it.second == c.sourcePackage } == true
    private fun flow(id: Long) = states.getOrPut(id) { MutableStateFlow(ProjectTrialState()) }
    override fun state(id: Long): StateFlow<ProjectTrialState> = flow(id)
    override fun bindContext(id: Long, mode: ProjectSetupMode, source: String?) {
        val next = mode to source
        val previous = routes.put(id, next)
        if (previous != null && previous != next && !flow(id).value.network) {
            images.remove(id); flow(id).value = flow(id).value.copy(plan = null, preparation = null, ready = false)
        }
    }
    private fun start(id: Long, network: Boolean = false, action: suspend () -> Unit) {
        if (states.values.any { it.value.busy }) return
        flow(id).value = flow(id).value.copy(busy = true, network = network, error = null)
        jobs[id] = scope.launch {
            try { action() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: TrialPrerequisite) { flow(id).value = flow(id).value.copy(error = failure.reason) }
            catch (_: UnverifiedTrialOriginal) { flow(id).value = flow(id).value.copy(error = "unverified_original") }
            catch (_: Exception) { flow(id).value = flow(id).value.copy(error = "trial_context_or_storage") }
            finally {
                jobs.remove(id)
                flow(id).value = flow(id).value.copy(busy = false, network = false)
                val waiting = pendingLoads.toMap()
                waiting.forEach { (profile, route) ->
                    if (pendingLoads.remove(profile, route) && routes[profile] == route) {
                        load(profile, route.first, route.second)
                    }
                }
            }
        }
    }
    private suspend fun preparation(id: Long): TrialPreparation = owner.owned(id) {
        val route = routes[id] ?: throw TrialPrerequisite("changed_context")
        val style = repository.load(id)
        val revision = style.revision ?: throw TrialPrerequisite("missing_style")
        val metadata = checkNotNull(style.metadata)
        val selection = try { ProjectSelection.decode(metadata.selectionJson) }
            catch (_: Exception) { throw TrialPrerequisite("stale_targets") }
            ?: throw TrialPrerequisite("missing_targets")
        trialRequire(selection.mode == route.first.name && selection.sourcePackage == route.second &&
            metadata.mode == route.first.name && metadata.sourcePackage == route.second, "changed_context")
        trialRequire(selection.componentKeys.isNotEmpty(), "missing_targets")
        val targets = owner.targets()
        val reader = if (route.first == ProjectSetupMode.EXTEND) {
            val source = route.second ?: throw TrialPrerequisite("stale_source")
            try { owner.reader(source) } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { throw TrialPrerequisite("stale_source") }
        } else null
        val stamp = reader?.index?.let(::projectSourceStamp)
        trialRequire(selection.isCurrent(route.first.name, route.second, stamp, projectCatalogStamp(targets),
            targets.filter { it.selectable }.map { it.key }.toSet()), "stale_targets")
        trialRequire(metadata.sourceStamp == stamp, "stale_source")
        if (reader != null) trialRequire(reader.isCurrent() && reader.index.state == CoverageIndexState.READY && reader.index.stamp.versionCode != null, "stale_source")
        val coverage = reader?.let { IconCoverageAnalyzer().analyze(id, targets.filter { it.key in selection.componentKeys }, it) }
        val defaults = chosenOptions[id] ?: owner.settings().let { Triple(it.provider, it.model, it.quality) }
        val context = TrialContext(id, revision.revision, metadata.version, route.first.name, route.second, stamp,
            projectCatalogStamp(targets), sha256(metadata.selectionJson.toByteArray()),
            referenceFingerprint(style.references.map { it.hash }), withContext(Dispatchers.IO) { journal.epoch(id) },
            defaults.first, defaults.second, defaults.third, styleFingerprint(revision.specification, revision.userInstruction),
            SavedStyleDna.optional(revision.analysisProfileJson)?.let { dna -> sha256(canonicalJson(json("reviewedDna" to dna.encode(),
                "choices" to json(*(dnaChoices[id].orEmpty().filterKeys { key -> key in selection.componentKeys }.map { (key,value) ->
                    key to json("family" to value.first,"type" to value.second.name) }).toTypedArray()))).toByteArray()) }.orEmpty()).also { it.validate() }
        TrialPreparation(context, style, StyleTrialTargetSelector.candidates(route.first.name, targets, selection.componentKeys, coverage), dnaChoices[id].orEmpty())
    }
    private suspend fun history(id: Long) {
        val records = withContext(Dispatchers.IO) { journal.records(id) }
        val variants = repository.trialVariants(id)
        flow(id).value = flow(id).value.copy(records = records, variants = variants)
    }
    override fun load(id: Long, mode: ProjectSetupMode, source: String?) {
        bindContext(id, mode, source)
        if (flow(id).value.network) return
        if (states.values.any { it.value.busy }) {
            pendingLoads[id] = mode to source
            return
        }
        pendingLoads.remove(id)
        start(id) {
            // A read queued during Prepare must not erase its completed plan.
            // Keep the dialog while busy (paid confirmation stays disabled), then
            // retain the exact plan/bytes only after all current-input checks.
            val frozen = flow(id).value.plan
            flow(id).value = flow(id).value.copy(preparation = null, ready = false)
            try {
                coordinator.recover(id); history(id)
                val p = preparation(id)
                val keyPresent = withContext(Dispatchers.IO) { hasKey(p.context.provider) }
                val accepted = accepted(id)
                val retained = frozen?.takeIf { plan ->
                    p.context == plan.context && flow(id).value.plan?.id == plan.id && images[id] != null &&
                        current(plan) && flow(id).value.plan?.id == plan.id && images[id] != null
                }
                if (retained == null) { images.remove(id); verifiedOriginalPlans.remove(id) }
                flow(id).value = flow(id).value.copy(preparation = p, hasKey = keyPresent,
                    ready = accepted.isNotEmpty(), plan = retained)
            } catch (failure: Exception) {
                cancelPreparation(id)
                throw failure
            }
        }
    }
    override fun options(id: Long, provider: String, model: String, quality: String) {
        if (flow(id).value.busy) return
        require(provider in AiSettings.PROVIDERS && model in AiSettings.MODELS && quality in AiSettings.QUALITIES)
        chosenOptions[id] = Triple(provider, model, quality)
        routes[id]?.let { load(id, it.first, it.second) }
    }
    override fun dnaChoice(id: Long,key: String,family: String,type: DnaSymbolType) {
        if(flow(id).value.busy) return
        val p=flow(id).value.preparation ?: return
        require(p.candidates.any { it.target.key==key && it.eligible })
        val dna=checkNotNull(SavedStyleDna.optional(p.style.revision?.analysisProfileJson))
        require(family == DNA_PACK_STYLE || dna.dna.families.any { it.id==family })
        val choices=dnaChoices[id].orEmpty()+ (key to (family to type));require(choices.size<=1000)
        dnaChoices[id]=choices;routes[id]?.let {load(id,it.first,it.second)}
    }
    override fun prepare(id: Long, keys: List<String>, allowCovered: Boolean) = start(id) {
        verifiedOriginalPlans.remove(id); images.remove(id)
        flow(id).value = flow(id).value.copy(plan = null)
        val p = preparation(id)
        StyleTrialTargetSelector.validateChoice(p.candidates, keys, allowCovered)
        trialRequire(p.context.provider != AiSettings.STABILITY || p.style.references.size == 1, "stability_references")
        SavedStyleDna.optional(p.style.revision?.analysisProfileJson)?.let { dna ->
            keys.forEach { key -> trialRequire(dnaReferencesMatch(dna,p.dnaChoices[key]?.first ?: dna.selectedFamily,
                p.style.references.map { it.hash }),"dna_reference_mismatch") }
        }
        val originals = owner.owned(id) { keys.map { key -> key to owner.original(key) } }
        val inputs = originals.map { (key, original) -> TrialInput(key, p.candidates.single { it.target.key == key }.target.name,
            original.hash, original.origin, originalEvidence = original.evidence) }
        val bytes = owner.owned(id) { p.style.references.associate { it.hash to repository.referenceBytes(id, it.hash) } } +
            originals.associate { it.second.hash to it.second.png }
        val plan = StyleTrialPlan(p.context, checkNotNull(p.style.revision).specification,
            p.style.revision.userInstruction, p.style.references.map { it.hash }, inputs,
            drawingRecipes = SavedStyleDna.optional(p.style.revision.analysisProfileJson)?.let { dna -> keys.associateWith { key ->
                val choice=p.dnaChoices[key] ?: (dna.selectedFamily to DnaSymbolType.UNKNOWN)
                StyleDnaCompiler.recipe(dna,choice.second,choice.first)
            } }.orEmpty())
        check(current(plan))
        images[id] = bytes
        flow(id).value = flow(id).value.copy(preparation = p, plan = plan,
            hasKey = withContext(Dispatchers.IO) { hasKey(p.context.provider) }, ready = false)
    }
    private suspend fun current(plan: StyleTrialPlan): Boolean = try {
        val p=preparation(plan.context.profileId)
        val dna=SavedStyleDna.optional(p.style.revision?.analysisProfileJson)
        val expectedRecipes=dna?.let { saved -> plan.inputs.associate { input ->
            val choice=p.dnaChoices[input.key] ?: (saved.selectedFamily to DnaSymbolType.UNKNOWN)
            input.key to StyleDnaCompiler.recipe(saved,choice.second,choice.first) } }.orEmpty()
        if (p.context != plan.context || plan.drawingRecipes != expectedRecipes) false else owner.owned(plan.context.profileId) {
            plan.inputs.all { input -> owner.original(input.key).let { it.hash == input.originalHash && it.origin == input.origin &&
                (input.originalEvidence == null || it.evidence == input.originalEvidence) } }
        }
    } catch (cancel: CancellationException) { throw cancel }
    catch (_: Exception) { false }
    override fun submit(id: Long, planId: String) {
        val state = flow(id).value; val plan = state.plan ?: return
        if (plan.id != planId || !state.hasKey || state.busy) return
        if (plan.inputs.any { it.originalEvidence != null } && verifiedOriginalPlans[id] != plan.id) return
        val bytes = images[id] ?: return
        start(id, network = true) {
            flow(id).value = flow(id).value.copy(plan = null)
            try { check(current(plan)); coordinator.submit(plan, bytes, true, { current(plan) }, { history(id) }) }
            finally { withContext(NonCancellable) { images.remove(id); verifiedOriginalPlans.remove(id); flow(id).value = flow(id).value.copy(plan = null); history(id) } }
            coordinator.recover(id); history(id)
        }
    }
    override fun verifyOriginals(id: Long, planId: String, verified: Boolean) {
        if (flow(id).value.plan?.id != planId || flow(id).value.busy) return
        if (verified) verifiedOriginalPlans[id] = planId else verifiedOriginalPlans.remove(id)
    }
    override fun cancelPreparation(id: Long) { if (!flow(id).value.network) { images.remove(id); verifiedOriginalPlans.remove(id); flow(id).value = flow(id).value.copy(plan = null) } }
    override fun stop(id: Long) { jobs[id]?.cancel() }
    override fun release(id: Long) {
        pendingLoads.remove(id)
        if (!flow(id).value.network) { jobs[id]?.cancel(); cancelPreparation(id) }
    }
    private suspend fun currentOriginals(p: TrialPreparation, keys: List<String>): Map<String, Pair<String, String>> = owner.owned(p.context.profileId) {
        check(keys.all { key -> p.candidates.any { it.target.key == key && it.eligible } }) { "A trial target is no longer eligible." }
        keys.distinct().associateWith { key -> owner.original(key).let { it.hash to it.origin } }
    }
    override fun review(id: Long, variantId: String, accepted: Boolean) = start(id) {
        val p = preparation(id); val row = repository.trialVariants(id).single { it.id == variantId }
        val original = currentOriginals(p, listOf(row.componentKey)).getValue(row.componentKey)
        owner.owned(id) { check(matchesRoute(p.context)); repository.reviewTrial(p.context, variantId, accepted, original, { journal.epoch(id) == p.context.epoch }) }
        history(id); flow(id).value = flow(id).value.copy(ready = this@OwnedStyleTrials.accepted(id).isNotEmpty())
    }
    override fun approve(id: Long) = start(id) {
        val p = preparation(id)
        val rows = repository.trialVariants(id).filter { it.reviewStatus == "ACCEPTED" &&
            it.trialContextJson?.let { text -> TrialVariantEvidence.parse(text).reviewContext == p.context } == true }
        val originals = currentOriginals(p, rows.map { it.componentKey })
        owner.owned(id) { check(matchesRoute(p.context)); repository.approveTrials(p.context, originals, { journal.epoch(id) == p.context.epoch }) }
        flow(id).value = flow(id).value.copy(ready = accepted(id).isNotEmpty()); history(id)
    }
    /** Read-only M6 consumption seam: fresh owner/source/style/references/originals, zero POST. */
    suspend fun accepted(id: Long): List<AcceptedTrialVariant> {
        val p = preparation(id)
        val approval = repository.database.projectAiDao().approval(id, p.context.revision) ?: return emptyList()
        if (approval.needsRevalidation || approval.contextJson != p.context.encode()) return emptyList()
        val ids = trialApprovalIds(approval)
        val rows = repository.trialVariants(id).filter { it.id in ids }
        val originals = currentOriginals(p, rows.map { it.componentKey })
        return owner.owned(id) { check(matchesRoute(p.context)); repository.acceptedTrials(p.context, originals, { journal.epoch(id) == p.context.epoch }) }
    }
    override suspend fun preview(id: Long, hash: String): ByteArray? {
        images[id]?.get(hash)?.let { return it }
        return try { repository.referenceBytes(id, hash) }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { null }
    }
}
