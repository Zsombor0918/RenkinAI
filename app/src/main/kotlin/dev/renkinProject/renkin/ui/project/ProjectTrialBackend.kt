package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.flow.StateFlow

internal data class TrialPreparation(val context: TrialContext, val style: SavedProjectStyle,
    val candidates: List<TrialCandidate>)
internal data class ProjectTrialState(val preparation: TrialPreparation? = null, val plan: StyleTrialPlan? = null,
    val records: List<StyleTrialRecord> = emptyList(), val variants: List<IconAssetVariant> = emptyList(),
    val busy: Boolean = false, val network: Boolean = false, val hasKey: Boolean = false,
    val ready: Boolean = false, val error: String? = null)

internal interface ProjectTrialBackend {
    fun bindContext(id: Long, mode: ProjectSetupMode, source: String?) {}
    fun state(id: Long): StateFlow<ProjectTrialState>
    fun load(id: Long, mode: ProjectSetupMode, source: String?)
    fun options(id: Long, provider: String, model: String, quality: String)
    fun prepare(id: Long, keys: List<String>, allowCovered: Boolean)
    fun submit(id: Long, planId: String)
    fun verifyOriginals(id: Long, planId: String, verified: Boolean) {}
    fun cancelPreparation(id: Long)
    fun stop(id: Long)
    fun release(id: Long)
    fun review(id: Long, variantId: String, accepted: Boolean)
    fun approve(id: Long)
    suspend fun preview(id: Long, hash: String): ByteArray?
}
