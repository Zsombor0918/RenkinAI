package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.apk.CoverageIndex
import dev.renkinProject.renkin.apk.CoverageTarget
import dev.renkinProject.renkin.apk.LocalIconState
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.apk.CoverageIndexState
import dev.renkinProject.renkin.data.project.ProjectSelection
import dev.renkinProject.renkin.apk.uniqueCoverageTargets
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Evaluate ownership AFTER acquiring the same mutex as icon saves/profile switches. */
internal suspend fun <T> withProjectOwner(mutex: Mutex, id: Long, active: () -> Long,
    ready: () -> Boolean, action: suspend () -> T): T = mutex.withLock {
    check(active() == id && ready()) { "Active project changed. Reopen its preparation." }
    action()
}

/** Exact conservative fingerprints, not progress/coverage guesses or Android-user migrations. */
internal fun projectCatalogStamp(targets: List<CoverageTarget>): String = sha256(JSONArray(
    uniqueCoverageTargets(targets).sortedBy { it.key }.map { target ->
        JSONArray(listOf(target.key, target.local.name, target.locked, target.ambiguousUser))
    }).toString().toByteArray())

/** Removed components disappear; newly installed components await a real icon/local-state
 * load. Never infer missing/eligible from an identity-only scan or invent an Android-user key. */
internal fun revalidateLauncherTargets(loaded: List<CoverageTarget>, current: Map<String, Boolean>): List<CoverageTarget> {
    val byKey = uniqueCoverageTargets(loaded).associateBy { it.key }
    return current.map { (key, uncertain) ->
        byKey[key]?.let { it.copy(ambiguousUser = it.ambiguousUser || uncertain) }
            ?: CoverageTarget(key, key, LocalIconState.UNKNOWN, ambiguousUser = uncertain)
    }
}

/** The user explicitly saved preparation, so verify a fresh read-only source index. Using
 * the old cached reader would reject a valid selection after a new/updated-pack rescan. */
internal suspend fun verifyProjectSelectionContext(selection: ProjectSelection, targets: List<CoverageTarget>,
    sourceReader: suspend (String, Boolean) -> CoverageSourceReader) {
    check(selection.catalogStamp == projectCatalogStamp(targets) && selection.componentKeys.all { key ->
        targets.any { it.key == key && it.selectable }
    }) { "Application catalog changed. Review the targets again." }
    if (selection.mode == "EXTEND") {
        val reader = sourceReader(checkNotNull(selection.sourcePackage), true)
        check(reader.isCurrent() && reader.index.state == CoverageIndexState.READY &&
            reader.index.stamp.versionCode != null && selection.sourceStamp == projectSourceStamp(reader.index) &&
            selection.componentKeys.none { it in reader.index.dynamicKeys }) {
            "Source pack changed. Rescan and review the targets."
        }
    }
}

internal fun projectSourceStamp(index: CoverageIndex): String = sha256(JSONObject()
    .put("package", index.stamp.packageName).put("version", index.stamp.versionCode)
    .put("updated", index.stamp.lastUpdateTime).put("revision", index.stamp.indexRevision)
    .put("theme", index.stamp.theme).put("state", index.state.name)
    .put("mappings", JSONArray(index.mappings.toSortedMap().map { JSONArray(listOf(it.key, it.value)) }))
    .put("dynamic", JSONArray(index.dynamicKeys.sorted())).toString().toByteArray())
