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
    private val owner: AnalysisSessionOwner) : ProjectAnalysisBackend {
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
                    flow(id).value = flow(id).value.copy(record = journal.latest(id), preparation = null)
                }
                throw cancel
            }
            catch (_: Exception) { flow(id).value = flow(id).value.copy(error = "operation") }
            finally { jobs.remove(id); flow(id).value = flow(id).value.copy(busy = false, network = false) }
        }
    }
    override fun prepare(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?, draft: ProjectStyleDraft) {
        bindContext(id, mode, source)
        start(id) {
            val samples = if (mode == ProjectSetupMode.EXTEND) {
                require(!source.isNullOrBlank() && setupStamp != null)
                sampler.sample(owner.reader(source, true))
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
                hasKey = withContext(Dispatchers.IO) { hasKey() }, validity = AnalysisValidity.CURRENT,
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
        flow(id).value = flow(id).value.copy(samples = null, preparation = null)
    }
    override suspend fun refresh(id: Long) {
        val record = withContext(Dispatchers.IO) { journal.latest(id) }
        val v = record?.context?.let { validity(it) } ?: AnalysisValidity.CURRENT
        // Existing VM jobs survive Activity recreation. Never replace a running preparation.
        flow(id).value = flow(id).value.copy(record = record, validity = v,
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
