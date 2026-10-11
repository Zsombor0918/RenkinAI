package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.apk.CoverageIndex
import dev.renkinProject.renkin.apk.CoverageTarget
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

internal fun projectSourceStamp(index: CoverageIndex): String = sha256(JSONObject()
    .put("package", index.stamp.packageName).put("version", index.stamp.versionCode)
    .put("updated", index.stamp.lastUpdateTime).put("revision", index.stamp.indexRevision)
    .put("theme", index.stamp.theme).put("state", index.state.name)
    .put("mappings", JSONArray(index.mappings.toSortedMap().map { JSONArray(listOf(it.key, it.value)) }))
    .put("dynamic", JSONArray(index.dynamicKeys.sorted())).toString().toByteArray())
