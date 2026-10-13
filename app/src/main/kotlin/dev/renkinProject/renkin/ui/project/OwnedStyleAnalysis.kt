package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.data.project.ProjectAiRepository
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap

/** Existing VM supplies its mutex and provider reads; this object owns no session/database. */
internal interface AnalysisSessionOwner {
    suspend fun <T> owned(id: Long, operation: suspend () -> T): T
    suspend fun catalogStamp(): String
    suspend fun reader(source: String, referenceIndex: Boolean): CoverageSourceReader
}

internal class OwnedStyleAnalysis(private val scope: CoroutineScope, private val repository: ProjectAiRepository,
    private val journal: StyleAnalysisJournal, private val sampler: ReferenceSampler,
    private val coordinator: StyleAnalysisCoordinator, private val hasKey: () -> Boolean,
    private val owner: AnalysisSessionOwner, private val dnaCoordinator: StyleDnaCoordinator? = null) : ProjectAnalysisBackend {
    private val states = mutableMapOf<Long, MutableStateFlow<ProjectAnalysisState>>()
    private val jobs = mutableMapOf<Long, Job>()
    private val routes = ConcurrentHashMap<Long, Pair<ProjectSetupMode, String?>>()
    override fun state(id: Long): StateFlow<ProjectAnalysisState> = flow(id)
    override fun bindContext(id: Long, mode: ProjectSetupMode, source: String?) { routes[id] = mode to source }
    private fun matchesRoute(context: AnalysisContext) = routes[context.profileId]?.let {
        context.matchesRoute(it.first, it.second)
    } ?: true
    private fun flow(id: Long) = states.getOrPut(id) { MutableStateFlow(ProjectAnalysisState()) }
    private fun start(id: Long, network: Boolean = false, action: suspend () -> Unit) {
        if (flow(id).value.busy) return
        if (states.values.any { it.value.busy }) { flow(id).value = flow(id).value.copy(error = "busy_other"); return }
        flow(id).value = flow(id).value.copy(busy = true, network = network, error = null)
        jobs[id] = scope.launch {
            try { action() }
            catch (cancel: CancellationException) {
                if (network) withContext(NonCancellable + Dispatchers.IO) {
                    flow(id).value = flow(id).value.copy(record = journal.latest(id), preparation = null, dnaPlan = null)
                }
                throw cancel
            }
            catch (_: Exception) { flow(id).value = flow(id).value.copy(error = "operation") }
            finally { jobs.remove(id); flow(id).value = flow(id).value.copy(busy = false, network = false) }
        }
    }

    override val supportsDna get() = dnaCoordinator != null
    override fun prepareDna(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?,
        draft: ProjectStyleDraft, analysisMode: DnaAnalysisMode, model: String, detail: String) {
        if (dnaCoordinator == null) return
        bindContext(id, mode, source)
        start(id) {
            flow(id).value = flow(id).value.copy(preparation = null, dnaPlan = null)
            val samples = if (mode == ProjectSetupMode.EXTEND) {
                require(source != null && setupStamp != null)
                sampler.sample(owner.reader(source,true), candidateLimit = 600, sampleLimit = if (analysisMode == DnaAnalysisMode.STANDARD) 24 else 12)
            } else sampler.manual(owner.owned(id) { draft.hashes.map { it to repository.referenceBytes(id,it) } })
            require(samples.samples.isNotEmpty())
            val context = owner.owned(id) {
                val saved = repository.load(id); check(saved.revision?.revision == expected)
                if (mode == ProjectSetupMode.EXTEND) {
                    val setup = owner.reader(checkNotNull(source),false); val refs = owner.reader(source,true)
                    check(setup.isCurrent() && projectSourceStamp(setup.index) == setupStamp && refs.isCurrent() && projectSourceStamp(refs.index) == samples.sourceStamp)
                }
                AnalysisContext(id,mode.name,source,setupStamp,samples.sourceStamp,saved.metadata?.version ?: 0L,
                    sha256(saved.metadata?.selectionJson.orEmpty().toByteArray()),owner.catalogStamp(),expected,
                    withContext(Dispatchers.IO) { journal.epoch(id) },samples.sourceIdentity)
            }
            val plan = StyleDnaPlan(context,analysisMode,samples.samples.mapIndexed { i,s -> DnaSampleBinding("s%02d".format(i+1),s.hash) },
                samples.report,draft.specification,draft.instruction,analysisDraftHash(draft),model,detail).also { it.validate() }
            val images = samples.samples.associate { it.hash to it.png }
            validateDnaImages(plan,images)
            plan.steps.filter { !it.synthesis }.forEach { step ->
                require(dnaBody(DnaCall(plan,step,step.samples.associate { plan.bindings.getValue(it) to images.getValue(plan.bindings.getValue(it)) },emptyList()))
                    .toString().toByteArray().size <= 5*1024*1024)
            }
            val cached = withContext(Dispatchers.IO) { dnaCoordinator.journal.records(id).lastOrNull { it.plan.fingerprint == plan.fingerprint && it.result != null } }
            check(validity(context) == AnalysisValidity.CURRENT)
            flow(id).value = flow(id).value.copy(samples = samples, dnaPlan = if (cached == null) plan else null,
                dnaRecord = cached, preparedDraftHash = analysisDraftHash(draft), hasKey = withContext(Dispatchers.IO) { hasKey() }, validity = AnalysisValidity.CURRENT)
        }
    }
    override fun submitDna(id: Long, planId: String) {
        val coordinator = dnaCoordinator ?: return; val state = flow(id).value; val plan = state.dnaPlan ?: return
        if (plan.id != planId || !state.hasKey || !matchesRoute(plan.context)) return
        val images = state.samples?.samples?.associate { it.hash to it.png } ?: return
        start(id, network = true) {
            flow(id).value = flow(id).value.copy(dnaPlan = null)
            try {
                val record = coordinator.submit(plan,images,true,{ validity(plan.context) == AnalysisValidity.CURRENT }) { record ->
                    flow(id).value = flow(id).value.copy(dnaRecord = record) }
                flow(id).value = flow(id).value.copy(dnaRecord = record, validity = validity(plan.context))
            } finally { withContext(NonCancellable + Dispatchers.IO) {
                flow(id).value = flow(id).value.copy(dnaRecord = coordinator.journal.latest(id),dnaPlan = null) } }
        }
    }
    override suspend fun dnaPreview(id: Long, hash: String): ByteArray = withContext(Dispatchers.IO) {
        checkNotNull(dnaCoordinator).journal.image(id,hash)
    }
    override suspend fun discardDna(id: Long, planId: String) {
        withContext(Dispatchers.IO) { val j = checkNotNull(dnaCoordinator).journal; val r = checkNotNull(j.record(id,planId)); j.discard(r.plan) }; refresh(id)
    }
    /** Human Save only. Never executes a gateway; edited JSON becomes a new immutable revision. */
    override suspend fun saveDna(id: Long, planId: String?, grammar: String, selectedFamily: String, hashes: List<String>,
        expected: Long?, draft: ProjectStyleDraft): SavedProjectStyle = owner.owned(id) {
        val coordinator = checkNotNull(dnaCoordinator)
        val current = repository.load(id); check(current.revision?.revision == expected)
        val record = if (planId == null) null else withContext(Dispatchers.IO) { checkNotNull(coordinator.journal.record(id,planId)) }
        val sourceDna = record?.result ?: checkNotNull(SavedStyleDna.optional(current.revision?.analysisProfileJson))
        val originalEvidence = current.revision?.analysisEvidenceJson?.let(ReviewedAnalysisEvidence::parse)
        val context = if (record != null) {
            check(record.status in setOf(DnaStatus.RESULT_SAVED,DnaStatus.REVIEWED) && matchesRoute(record.plan.context))
            check(validityWithoutOwner(record.plan.context,current) == AnalysisValidity.CURRENT)
            record.plan.context
        } else {
            val route = checkNotNull(routes[id]); val setup = if (route.first == ProjectSetupMode.EXTEND) owner.reader(checkNotNull(route.second),false) else null
            val refs = if (route.first == ProjectSetupMode.EXTEND) owner.reader(checkNotNull(route.second),true) else null
            check(setup == null || setup.isCurrent() && refs!!.isCurrent() && projectSourceStamp(setup.index) == current.metadata?.sourceStamp)
            AnalysisContext(id,route.first.name,route.second,setup?.index?.let(::projectSourceStamp),refs?.index?.let(::projectSourceStamp),current.metadata?.version ?: 0L,
                sha256(current.metadata?.selectionJson.orEmpty().toByteArray()),owner.catalogStamp(),expected,
                withContext(Dispatchers.IO) { journal.epoch(id) },refs?.index?.stamp)
        }
        val reviewed = sourceDna.copy(dna = StyleDnaV1.parse(grammar), selectedFamily = selectedFamily)
        val encoded = reviewed.encode(); val specification = StyleDnaCompiler.specification(reviewed)
        require(hashes.size <= 5 && hashes.distinct().size == hashes.size)
        val selected = if (record != null) {
            require(hashes.all { hash -> record.plan.samples.any { it.hash == hash } })
            repository.importReferences(id,hashes.map { hash -> { ByteArrayInputStream(coordinator.journal.image(id,hash)) } }).map { it.hash }
        } else { require(hashes == current.references.map { it.hash }); hashes }
        val evidence = ReviewedAnalysisEvidence(record?.plan?.id ?: checkNotNull(originalEvidence).requestId,
            record?.plan?.fingerprint ?: originalEvidence!!.fingerprint,record?.plan?.promptHash ?: originalEvidence!!.promptHash,context,reviewed.model,
            reviewed.reports.map { it.sampleHash },selected,record?.steps?.lastOrNull { it.responseId != null }?.responseId ?: originalEvidence!!.responseId,
            sha256(encoded.toByteArray()),System.currentTimeMillis(), encoded != sourceDna.encode() || record == null,false)
        val saved = repository.saveAnalyzedStyle(id,expected,current.metadata?.version ?: 0L,specification,draft.instruction,selected,encoded,evidence)
        if (record != null) withContext(NonCancellable + Dispatchers.IO) { coordinator.journal.reviewed(record.plan); coordinator.journal.collect(id) }
        flow(id).value = flow(id).value.copy(savedDna = reviewed, dnaPlan = null, dnaRecord = record?.copy(status = DnaStatus.REVIEWED))
        saved
    }
    private suspend fun validityWithoutOwner(c: AnalysisContext,saved: SavedProjectStyle): AnalysisValidity {
        if (!matchesRoute(c) || (saved.metadata?.version ?: 0L) != c.metadataVersion || saved.revision?.revision != c.baseRevision ||
            sha256(saved.metadata?.selectionJson.orEmpty().toByteArray()) != c.selectionHash || owner.catalogStamp() != c.catalogStamp) return AnalysisValidity.PREPARATION_CHANGED
        if (withContext(Dispatchers.IO) { journal.epoch(c.profileId) } != c.storageEpoch) return AnalysisValidity.RESTORED
        if (c.mode == "EXTEND") {
            val setup = owner.reader(checkNotNull(c.sourcePackage),false); val refs = owner.reader(c.sourcePackage,true)
            if (!setup.isCurrent() || !refs.isCurrent() || projectSourceStamp(setup.index) != c.setupStamp || projectSourceStamp(refs.index) != c.referenceStamp) return AnalysisValidity.SOURCE_STALE
        }
        return AnalysisValidity.CURRENT
    }

    override fun prepare(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?, draft: ProjectStyleDraft) {
        bindContext(id, mode, source)
        start(id) {
            val samples = if (mode == ProjectSetupMode.EXTEND) {
                require(!source.isNullOrBlank() && setupStamp != null)
                sampler.sample(owner.reader(source, true), candidateLimit = 300)
            } else {
                val images = owner.owned(id) { draft.hashes.map { it to repository.referenceBytes(id, it) } }
                sampler.manual(images)
            }
            val context = owner.owned(id) {
                val current = repository.load(id)
                check(current.revision?.revision == expected) { "Saved style changed." }
                if (mode == ProjectSetupMode.EXTEND) {
                    val setup = owner.reader(checkNotNull(source), false)
                    check(setup.isCurrent() && projectSourceStamp(setup.index) == setupStamp)
                    val refs = owner.reader(source, true)
                    check(refs.isCurrent() && projectSourceStamp(refs.index) == samples.sourceStamp)
                }
                AnalysisContext(id, mode.name, source, setupStamp, samples.sourceStamp, current.metadata?.version ?: 0L,
                    sha256(current.metadata?.selectionJson.orEmpty().toByteArray()), owner.catalogStamp(), expected,
                    withContext(Dispatchers.IO) { journal.epoch(id) }, samples.sourceIdentity)
            }
            context.validate()
            val request = if (samples.samples.isEmpty()) null else StyleAnalysisRequest(context,
                samples.samples.map { AnalysisImage(it.hash, it.png) }, draft.specification, draft.instruction, localReport = samples.report).also { it.validate() }
            flow(id).value = flow(id).value.copy(samples = samples, preparation = request,
                dnaPlan = null, hasKey = withContext(Dispatchers.IO) { hasKey() }, validity = AnalysisValidity.CURRENT,
                error = if (request == null) "empty" else null, preparedDraftHash = analysisDraftHash(draft))
        }
    }
    /** Called only by the confirmation button with the frozen request ID that was displayed. */
    override fun submit(id: Long, requestId: String) {
        val request = flow(id).value.preparation ?: return
        if (request.requestId != requestId || request.context.profileId != id || !flow(id).value.hasKey) return
        start(id, network = true) {
            val saved = coordinator.submit(request, confirmed = true) { validity(it) == AnalysisValidity.CURRENT }
            flow(id).value = flow(id).value.copy(record = saved, preparation = null,
                validity = saved?.context?.let { validity(it) } ?: AnalysisValidity.PREPARATION_CHANGED,
                error = if (saved == null) "stale" else null)
        }
    }
    override fun cancel(id: Long) { jobs[id]?.cancel() }
    override fun releasePreview(id: Long) {
        if (!flow(id).value.network) jobs[id]?.cancel()
        flow(id).value = flow(id).value.copy(samples = null, preparation = null, dnaPlan = null)
    }
    override suspend fun refresh(id: Long) {
        val record = withContext(Dispatchers.IO) { journal.latest(id) }
        val dnaRecord = withContext(Dispatchers.IO) { dnaCoordinator?.journal?.latest(id) }
        val savedDna = if (dnaCoordinator == null) null else try { owner.owned(id) { SavedStyleDna.optional(repository.load(id).revision?.analysisProfileJson) } }
            catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { null }
        val v = (dnaRecord?.plan?.context ?: record?.context)?.let { validity(it) } ?: AnalysisValidity.CURRENT
        // Existing VM jobs survive Activity recreation. Never replace a running preparation.
        flow(id).value = flow(id).value.copy(record = record, dnaRecord = dnaRecord, savedDna = savedDna, validity = v,
            hasKey = withContext(Dispatchers.IO) { hasKey() })
    }
    private suspend fun validity(context: AnalysisContext): AnalysisValidity = try {
        owner.owned(context.profileId) {
            if (!matchesRoute(context)) AnalysisValidity.PREPARATION_CHANGED
            else if (withContext(Dispatchers.IO) { journal.epoch(context.profileId) } != context.storageEpoch) AnalysisValidity.RESTORED
            else {
                val current = repository.load(context.profileId)
                if ((current.metadata?.version ?: 0L) != context.metadataVersion || current.revision?.revision != context.baseRevision ||
                    sha256(current.metadata?.selectionJson.orEmpty().toByteArray()) != context.selectionHash || owner.catalogStamp() != context.catalogStamp) {
                    AnalysisValidity.PREPARATION_CHANGED
                } else if (context.mode == "EXTEND") {
                    val setup = owner.reader(checkNotNull(context.sourcePackage), false)
                    val refs = owner.reader(context.sourcePackage, true)
                    if (!setup.isCurrent() || !refs.isCurrent() || projectSourceStamp(setup.index) != context.setupStamp ||
                        projectSourceStamp(refs.index) != context.referenceStamp) AnalysisValidity.SOURCE_STALE else AnalysisValidity.CURRENT
                } else AnalysisValidity.CURRENT
            }
        }
    } catch (cancel: CancellationException) { throw cancel }
    catch (_: Exception) { AnalysisValidity.WRONG_PROFILE }

    override suspend fun preview(id: Long, requestId: String, hash: String): ByteArray = withContext(Dispatchers.IO) {
        val record = checkNotNull(journal.read(id, requestId)); require(hash in record.hashes)
        journal.image(id, hash)
    }
    override suspend fun use(id: Long, requestId: String, hashes: List<String>, allowStaleSource: Boolean): AnalyzedDraft {
        val record = withContext(Dispatchers.IO) { checkNotNull(journal.read(id, requestId)) }
        check(record.status == AnalysisStatus.RESULT_SAVED && record.context != null)
        val v = validity(record.context)
        check(v == AnalysisValidity.CURRENT || (allowStaleSource && v == AnalysisValidity.SOURCE_STALE))
        require(hashes.size <= 5 && hashes.distinct().size == hashes.size && hashes.all { it in record.hashes })
        val reviewedStamp = if (record.context.mode == "EXTEND") projectSourceStamp(owner.reader(checkNotNull(record.context.sourcePackage), true).index) else null
        val imported = owner.owned(id) {
            check(matchesRoute(record.context)) { "Style setup changed; prepare a new suggestion." }
            check(withContext(Dispatchers.IO) { journal.epoch(id) } == record.context.storageEpoch)
            val sources = hashes.map { hash -> { ByteArrayInputStream(journal.image(id, hash)) } }
            repository.importReferences(id, sources)
        }
        return AnalyzedDraft(PromptComposer.specification(checkNotNull(record.style)), imported.map { it.hash }, requestId,
            System.currentTimeMillis(), reviewedStamp)
    }
    override suspend fun discardCandidate(id: Long, requestId: String) {
        withContext(Dispatchers.IO) { journal.discard(id, requestId) }; refresh(id)
    }

    /** Style Save still enters the existing session mutex; no network or icon operation. */
    suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft,
        mode: ProjectSetupMode? = null, source: String? = null): SavedProjectStyle = owner.owned(id) {
        val record = withContext(Dispatchers.IO) { checkNotNull(journal.read(id, checkNotNull(draft.analysisRequestId))) }
        check(record.context != null && record.context.profileId == id && record.status in setOf(AnalysisStatus.RESULT_SAVED, AnalysisStatus.REVIEWED))
        check(matchesRoute(record.context) && (mode == null || record.context.matchesRoute(mode, source))) {
            "Style setup changed; prepare a new suggestion."
        }
        check(withContext(Dispatchers.IO) { journal.epoch(id) } == record.context.storageEpoch)
        val current = repository.load(id)
        val revision = current.revision
        val existing = revision?.analysisEvidenceJson?.let(ReviewedAnalysisEvidence::parse)
        if (existing?.requestId == record.requestId && revision != null && revision.specification == draft.specification.trim() &&
            revision.userInstruction == draft.instruction.trim() && current.references.map { it.hash } == draft.hashes) {
            withContext(NonCancellable + Dispatchers.IO) { journal.reviewed(id, record.requestId, revision.revision) }
            return@owned current
        }
        // We already hold the owner mutex: compare the same DB snapshot and live source here.
        check((current.metadata?.version ?: 0L) == record.context.metadataVersion && current.revision?.revision == expected &&
            expected == record.context.baseRevision && sha256(current.metadata?.selectionJson.orEmpty().toByteArray()) == record.context.selectionHash &&
            owner.catalogStamp() == record.context.catalogStamp) { "Preparation changed; reopen before saving." }
        var stale = false
        if (record.context.mode == "EXTEND") {
            val setup = owner.reader(checkNotNull(record.context.sourcePackage), false)
            val refs = owner.reader(record.context.sourcePackage, true)
            stale = !setup.isCurrent() || !refs.isCurrent() || projectSourceStamp(setup.index) != record.context.setupStamp ||
                projectSourceStamp(refs.index) != record.context.referenceStamp
            check(!stale || (draft.allowStaleSource && projectSourceStamp(refs.index) == draft.analysisReviewedSourceStamp)) {
                "Source changed; explicitly review the older suggestion again."
            }
        }
        val styleJson = checkNotNull(record.styleJson)
        val edited = draft.specification.trim() != PromptComposer.specification(checkNotNull(record.style)) || draft.hashes.any { it !in record.hashes }
        val evidence = ReviewedAnalysisEvidence(record.requestId, record.fingerprint, record.promptHash, record.context, record.model,
            record.hashes, draft.hashes, checkNotNull(record.responseId), checkNotNull(record.responseHash),
            checkNotNull(draft.analysisReviewedAt), edited, stale)
        val saved = repository.saveAnalyzedStyle(id, expected, record.context.metadataVersion, draft.specification,
            draft.instruction, draft.hashes, styleJson, evidence)
        withContext(NonCancellable + Dispatchers.IO) { journal.reviewed(id, record.requestId, checkNotNull(saved.revision).revision) }
        flow(id).value = flow(id).value.copy(record = withContext(Dispatchers.IO) { journal.read(id, record.requestId) })
        saved
    }
}
