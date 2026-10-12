package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.style.AnalysisContext
import dev.renkinProject.renkin.ai.style.AnalysisRecord
import dev.renkinProject.renkin.ai.style.ReferenceSamples
import dev.renkinProject.renkin.ai.style.StyleAnalysisRequest
import kotlinx.coroutines.flow.StateFlow

internal enum class AnalysisValidity { CURRENT, SOURCE_STALE, PREPARATION_CHANGED, RESTORED, WRONG_PROFILE }
internal fun AnalysisContext.matchesRoute(mode: ProjectSetupMode, source: String?) =
    this.mode == mode.name && sourcePackage == source
internal data class ProjectAnalysisState(val busy: Boolean = false, val network: Boolean = false, val preparation: StyleAnalysisRequest? = null,
    val samples: ReferenceSamples? = null, val record: AnalysisRecord? = null,
    val validity: AnalysisValidity = AnalysisValidity.CURRENT, val hasKey: Boolean = false, val error: String? = null,
    val preparedDraftHash: String? = null)

internal fun analysisDraftHash(draft: ProjectStyleDraft) = dev.renkinProject.renkin.data.project.sha256(
    dev.renkinProject.renkin.ai.style.canonicalJson(dev.renkinProject.renkin.ai.style.json("description" to draft.specification,
        "instruction" to draft.instruction, "references" to org.json.JSONArray(draft.hashes))).toByteArray(Charsets.UTF_8))
internal data class AnalyzedDraft(val specification: String, val hashes: List<String>, val requestId: String,
    val reviewedAt: Long, val reviewedSourceStamp: String?)

/** UI declares consent through submit; prepare/refresh/reopen never submit. VM owns jobs. */
internal interface ProjectAnalysisBackend {
    fun state(id: Long): StateFlow<ProjectAnalysisState>
    fun bindContext(id: Long, mode: ProjectSetupMode, source: String?) {}
    fun prepare(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?,
        expected: Long?, draft: ProjectStyleDraft)
    fun submit(id: Long, requestId: String)
    fun cancel(id: Long)
    fun releasePreview(id: Long) {}
    suspend fun refresh(id: Long)
    suspend fun preview(id: Long, requestId: String, hash: String): ByteArray
    suspend fun use(id: Long, requestId: String, hashes: List<String>, allowStaleSource: Boolean): AnalyzedDraft
    suspend fun discardCandidate(id: Long, requestId: String)
}
