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
    val preparedDraftHash: String? = null,
    val dnaPlan: dev.renkinProject.renkin.ai.style.StyleDnaPlan? = null,
    val dnaRecord: dev.renkinProject.renkin.ai.style.StyleDnaRecord? = null,
    val savedDna: dev.renkinProject.renkin.ai.style.SavedStyleDna? = null,
    val legacyValidity: AnalysisValidity? = null,
    val dnaValidity: AnalysisValidity? = null)

internal fun analysisDraftHash(draft: ProjectStyleDraft) = dev.renkinProject.renkin.data.project.sha256(
    dev.renkinProject.renkin.ai.style.canonicalJson(dev.renkinProject.renkin.ai.style.json("description" to draft.specification,
        "instruction" to draft.instruction, "references" to org.json.JSONArray(draft.hashes))).toByteArray(Charsets.UTF_8))
internal data class AnalyzedDraft(val specification: String, val hashes: List<String>, val requestId: String,
    val reviewedAt: Long, val reviewedSourceStamp: String?)

/** UI declares consent through submit; prepare/refresh/reopen never submit. VM owns jobs. */
internal interface ProjectAnalysisBackend {
    fun state(id: Long): StateFlow<ProjectAnalysisState>
    val supportsDna: Boolean get() = false
    fun prepareDna(id: Long, mode: ProjectSetupMode, source: String?, setupStamp: String?, expected: Long?,
        draft: ProjectStyleDraft, analysisMode: dev.renkinProject.renkin.ai.style.DnaAnalysisMode, model: String, detail: String) {}
    fun submitDna(id: Long, planId: String) {}
    suspend fun saveDna(id: Long, planId: String?, grammar: String, selectedFamily: String, hashes: List<String>,
        expected: Long?, draft: ProjectStyleDraft): dev.renkinProject.renkin.data.project.SavedProjectStyle = error("Style DNA unavailable")
    suspend fun discardDna(id: Long, planId: String) {}
    suspend fun dnaPreview(id: Long, hash: String): ByteArray = error("Style DNA unavailable")
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
