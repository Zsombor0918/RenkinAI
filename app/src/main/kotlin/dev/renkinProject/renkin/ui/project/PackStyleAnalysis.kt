package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*

/** One explicit operation, using the existing journal/epochs/consumed IDs. No background submission. */
internal class PackStyleAnalysis(private val repository: ProjectAiRepository, private val local: LocalPackMontages,
    private val journal: StyleAnalysisJournal, private val gateway: suspend (StyleAnalysisRequest) -> StyleAnalysisOutcome,
    val hasKey: () -> Boolean, private val owner: AnalysisSessionOwner) {
    suspend fun latest(id: Long) = withContext(Dispatchers.IO) { journal.latest(id)?.takeIf { it.contract == 2 } }

    suspend fun prepare(id: Long, draft: MontageDraft, preferences: String): StyleAnalysisRequest {
        require(hasKey()) { "Add an OpenAI key in AI settings" }
        local.validate(id, draft)
        return owner.owned(id) {
            val saved = repository.load(id); val metadata = checkNotNull(saved.metadata); val revision = checkNotNull(saved.revision)
            val montage = checkNotNull(PackMontage.optional(revision.analysisProfileJson))
            check(montage.fingerprint == draft.montage.fingerprint && metadata.sourcePackage == montage.source && metadata.sourceStamp == montage.setupStamp)
            val reader = owner.reader(montage.source, true)
            check(reader.isCurrent() && projectSourceStamp(reader.index) == montage.referenceStamp)
            val context = AnalysisContext(id, "EXTEND", montage.source, montage.setupStamp, montage.referenceStamp,
                metadata.version, sha256(metadata.selectionJson.toByteArray()), sha256(ByteArray(0)), revision.revision,
                withContext(Dispatchers.IO) { journal.epoch(id) }, reader.index.stamp)
            StyleAnalysisRequest(context, draft.images.map { AnalysisImage(sha256(it), it.copyOf()) }, PACK_ANALYSIS_INSTRUCTIONS,
                preferences, detail = "high", contract = 2).also { it.validate() }
        }
    }

    private suspend fun current(request: StyleAnalysisRequest): Boolean = owner.owned(request.context.profileId) {
        val c = request.context; val saved = repository.load(c.profileId); val meta = saved.metadata ?: return@owned false
        val montage = PackMontage.optional(saved.revision?.analysisProfileJson) ?: return@owned false
        val reader = owner.reader(montage.source, true)
        meta.version == c.metadataVersion && saved.revision!!.revision == c.baseRevision &&
            meta.mode == c.mode && meta.sourcePackage == c.sourcePackage && meta.sourceStamp == c.setupStamp &&
            sha256(meta.selectionJson.toByteArray()) == c.selectionHash && montage.referenceStamp == c.referenceStamp &&
            montage.pages.map { it.hash } == request.images.map { it.hash } && reader.isCurrent() &&
            reader.index.stamp == c.sourceIdentity && projectSourceStamp(reader.index) == c.referenceStamp &&
            withContext(Dispatchers.IO) { journal.epoch(c.profileId) } == c.storageEpoch
    }

    suspend fun submit(request: StyleAnalysisRequest, draft: MontageDraft, confirmed: Boolean): SavedProjectStyle? {
        if (!confirmed) return null
        request.validate(); require(request.contract == 2 && request.context.profileId == draft.profileId)
        check(current(request)) { "Pack or project changed; review again" }
        val id = request.context.profileId
        if (!withContext(Dispatchers.IO) { journal.claim(request) }) return null
        try {
            val frozen = withContext(Dispatchers.IO) { request.copy(images = request.images.map { image ->
                AnalysisImage(image.hash, journal.image(id, image.hash, 2)) }) }
            val outcome = if (current(request)) gateway(frozen) else StyleAnalysisOutcome.StaleSource
            val record = withContext(Dispatchers.IO) { journal.finish(id, request.requestId, outcome) }
            if (record.status != AnalysisStatus.RESULT_SAVED || !current(request)) return null
            return saveResult(record, request, draft)
        } catch (cancel: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) { journal.interrupted(id, request.requestId) }
            throw cancel
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { journal.release(id, request.requestId) }
        }
    }

    private suspend fun saveResult(record: AnalysisRecord, request: StyleAnalysisRequest, draft: MontageDraft): SavedProjectStyle {
        val (prompt, summary) = parsePackStyleText(checkNotNull(record.styleJson))
        val result = PackStyleResult(prompt, summary, record.requestId, checkNotNull(record.responseId), record.model)
        val saved = local.save(draft.profileId, request.context.baseRevision,
            draft.copy(montage = draft.montage.copy(result = result)), prompt,
            repository.load(draft.profileId).revision!!.userInstruction, expectedVersion = request.context.metadataVersion)
        withContext(Dispatchers.IO) { journal.reviewed(draft.profileId, record.requestId, saved.revision!!.revision) }
        return saved
    }

    /** Retry only a proven received result's local Save, never the provider request. */
    suspend fun recover(id: Long, draft: MontageDraft): SavedProjectStyle? {
        val record = latest(id)?.takeIf { it.status == AnalysisStatus.RESULT_SAVED } ?: return null
        val request = StyleAnalysisRequest(checkNotNull(record.context), draft.images.map { AnalysisImage(sha256(it), it) },
            record.description, record.userInstruction, record.model, "high", record.requestId,
            algorithm = record.algorithm, contract = 2)
        request.validate()
        if (request.fingerprint != record.fingerprint || !current(request)) return null
        return saveResult(record, request, draft)
    }
}
