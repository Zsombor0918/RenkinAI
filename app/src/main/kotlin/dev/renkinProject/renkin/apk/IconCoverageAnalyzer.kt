package dev.renkinProject.renkin.apk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

internal enum class SourceCoverage { RESOLVABLE, DECLARED_BROKEN, UNMAPPED, SOURCE_UNAVAILABLE, UNKNOWN }
internal enum class LocalIconState { NO_LOCAL_ICON, CUSTOM_OR_MANUAL, FALLBACK, OTHER_APPLIED, UNKNOWN }
internal enum class CoverageIndexState { READY, LOADING, UNKNOWN, UNAVAILABLE }
internal enum class CoverageReason { DRAWN, NO_MAPPING, DRAW_FAILED, INDEX_UNPROVEN, SOURCE_CHANGED, USER_IDENTITY, DYNAMIC_UNPROVEN }

/** Primitives only: the profile's live icon data remains in ApplicationProvider. */
internal data class CoverageTarget(
    val key: String,
    val name: String,
    val local: LocalIconState = LocalIconState.NO_LOCAL_ICON,
    val locked: Boolean = false,
    val ambiguousUser: Boolean = false
) {
    val selectable: Boolean get() = !locked && !ambiguousUser && local != LocalIconState.UNKNOWN
}

internal data class CoverageStamp(
    val packageName: String,
    val versionCode: Long?,
    val lastUpdateTime: Long?,
    val indexRevision: Long,
    val theme: Boolean?
)

internal data class CoverageIndex(
    val stamp: CoverageStamp,
    val state: CoverageIndexState,
    val mappings: Map<String, String> = emptyMap(),
    val dynamicKeys: Set<String> = emptySet()
)

/** The adapter reads one frozen index and probes only its explicitly selected pack. */
internal interface CoverageSourceReader {
    val index: CoverageIndex
    fun isCurrent(): Boolean
    suspend fun canRender(drawableName: String): Boolean
    val stableReferences: Boolean get() = true
    suspend fun referencePng(drawableName: String): ByteArray? = null
    suspend fun inspectMontageReference(drawableName: String): dev.renkinProject.renkin.ai.style.ReferenceRead = inspectReference(drawableName)
    /** Compatibility adapters may lack resource-ID diagnostics. Real pack readers override this. */
    suspend fun inspectReference(drawableName: String): dev.renkinProject.renkin.ai.style.ReferenceRead {
        if (!canRender(drawableName)) return dev.renkinProject.renkin.ai.style.ReferenceRead.Failed(
            dev.renkinProject.renkin.ai.style.ReferenceFailure.MISSING_DRAWABLE)
        val png = referencePng(drawableName) ?: return dev.renkinProject.renkin.ai.style.ReferenceRead.Failed(
            dev.renkinProject.renkin.ai.style.ReferenceFailure.UNSUPPORTED_STATEFUL)
        return dev.renkinProject.renkin.ai.style.ReferenceRead.Rendered(png)
    }
}

internal data class CoverageEntry(
    val target: CoverageTarget,
    val source: SourceCoverage,
    val drawableName: String?,
    val reason: CoverageReason
) {
    val actionable: Boolean get() = target.selectable &&
        (source == SourceCoverage.UNMAPPED || source == SourceCoverage.DECLARED_BROKEN) &&
        (target.local == LocalIconState.NO_LOCAL_ICON || target.local == LocalIconState.FALLBACK)
}

internal data class CoverageCounts(
    val total: Int, val declared: Int, val covered: Int, val broken: Int,
    val unmapped: Int, val unknown: Int, val unavailable: Int,
    val locallyHandled: Int, val locked: Int, val actionable: Int
)

internal data class CoverageSnapshot(
    val profileId: Long,
    val index: CoverageIndex,
    val entries: List<CoverageEntry>
) {
    val counts: CoverageCounts get() = CoverageCounts(
        entries.size, entries.count { it.drawableName != null },
        entries.count { it.source == SourceCoverage.RESOLVABLE },
        entries.count { it.source == SourceCoverage.DECLARED_BROKEN },
        entries.count { it.source == SourceCoverage.UNMAPPED },
        entries.count { it.source == SourceCoverage.UNKNOWN },
        entries.count { it.source == SourceCoverage.SOURCE_UNAVAILABLE },
        entries.count { it.target.local == LocalIconState.CUSTOM_OR_MANUAL || it.target.local == LocalIconState.OTHER_APPLIED },
        entries.count { it.target.locked }, entries.count { it.actionable }
    )
}

/** A collision is one unsupported row, never two targets that share a Room/component key. */
internal fun uniqueCoverageTargets(targets: List<CoverageTarget>): List<CoverageTarget> =
    targets.groupBy { it.key }.values.map { group ->
        group.first().copy(ambiguousUser = group.size > 1 || group.any { it.ambiguousUser })
    }

internal fun filterCoverageTargets(targets: List<CoverageTarget>, query: String): List<CoverageTarget> =
    targets.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) || it.key.contains(query.trim(), ignoreCase = true) }

internal class IconCoverageAnalyzer {
    suspend fun analyze(
        profileId: Long,
        targets: List<CoverageTarget>,
        reader: CoverageSourceReader,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): CoverageSnapshot = withContext(Dispatchers.Default) {
        val index = reader.index
        val unique = uniqueCoverageTargets(targets)
        // Only booleans are cached per scan. At most one 96 px preview is alive in the adapter.
        val drawn = mutableMapOf<String, Boolean>()
        val entries = unique.mapIndexed { position, target ->
            currentCoroutineContext().ensureActive()
            val name = index.mappings[target.key]
            val (status, reason) = when {
                index.state == CoverageIndexState.UNAVAILABLE || !reader.isCurrent() -> SourceCoverage.SOURCE_UNAVAILABLE to CoverageReason.SOURCE_CHANGED
                target.ambiguousUser -> SourceCoverage.UNKNOWN to CoverageReason.USER_IDENTITY
                index.state != CoverageIndexState.READY -> SourceCoverage.UNKNOWN to CoverageReason.INDEX_UNPROVEN
                target.key in index.dynamicKeys -> SourceCoverage.UNKNOWN to CoverageReason.DYNAMIC_UNPROVEN
                name == null -> SourceCoverage.UNMAPPED to CoverageReason.NO_MAPPING
                else -> {
                    val success = drawn[name] ?: try {
                        reader.canRender(name).also { drawn[name] = it }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        false.also { drawn[name] = it }
                    }
                    if (success) SourceCoverage.RESOLVABLE to CoverageReason.DRAWN
                    else SourceCoverage.DECLARED_BROKEN to CoverageReason.DRAW_FAILED
                }
            }
            onProgress(position + 1, unique.size)
            yield()
            CoverageEntry(target, status, name, reason)
        }
        // A package/index/theme change at any point invalidates the entire earlier result.
        val finalEntries = if (reader.isCurrent()) entries else entries.map {
            it.copy(source = SourceCoverage.SOURCE_UNAVAILABLE, reason = CoverageReason.SOURCE_CHANGED)
        }
        CoverageSnapshot(profileId, index, finalEntries)
    }
}
