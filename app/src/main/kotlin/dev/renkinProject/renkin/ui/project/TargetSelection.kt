package dev.renkinProject.renkin.ui.project

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.apk.CoverageEntry
import dev.renkinProject.renkin.apk.CoverageReason
import dev.renkinProject.renkin.apk.LocalIconState
import dev.renkinProject.renkin.apk.SourceCoverage
import dev.renkinProject.renkin.apk.filterCoverageTargets
import dev.renkinProject.renkin.apk.uniqueCoverageTargets
import dev.renkinProject.renkin.drawable.toSafeBitmapOrNull
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.WavyLoadingBar

@Composable
internal fun ProjectOriginalIcon(drawable: Drawable?) {
    val bitmap = remember(drawable) { drawable?.toSafeBitmapOrNull(48, 48)?.asImageBitmap() }
    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(48.dp))
}

@Composable
internal fun TargetSelection(
    mode: ProjectSetupMode, profileId: Long, profileName: String, source: String?,
    data: ProjectSetupData, ready: Boolean, onBack: () -> Unit,
    onChangeSource: () -> Unit, onEditor: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var missingOnly by rememberSaveable { mutableStateOf(mode == ProjectSetupMode.EXTEND) }
    var selectedStamp by rememberSaveable { mutableStateOf("") }
    val targets = uniqueCoverageTargets(data.targets)
    val snapshot = (data.coverage as? CoverageUiState.Ready)?.snapshot?.takeIf {
        it.profileId == profileId && it.index.stamp.packageName == source && it.entries.map { e -> e.target } == targets
    }
    val stamp = snapshot?.index?.stamp?.toString()
    LaunchedEffect(stamp) {
        if (stamp != null) {
            if (selectedStamp.isNotEmpty() && selectedStamp != stamp) selected = arrayListOf()
            selectedStamp = stamp
        }
    }
    val byKey = snapshot?.entries.orEmpty().associateBy { it.target.key }
    val filtered = filterCoverageTargets(targets, query).filter {
        mode == ProjectSetupMode.CREATE || !missingOnly || byKey[it.key]?.source in setOf(SourceCoverage.UNMAPPED, SourceCoverage.DECLARED_BROKEN)
    }
    val validSelected = selected.filter { key -> targets.any { it.key == key && it.selectable } }.toSet()
    val coverageReady = mode == ProjectSetupMode.CREATE || snapshot != null
    CenteredFullscreenContent {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("project-targets")) {
            item {
                SetupHeading(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.setupCreateTargets else R.string.setupExtendTargets), onBack)
                Text(profileName, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.projectIdentity, profileId))
                Text(stringResource(R.string.setupPreparationOnly))
                Text(stringResource(R.string.setupSelectionTransient))
                OutlinedButton(onClick = onEditor, enabled = ready,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-editor")) {
                    Text(stringResource(R.string.projectClassicEditor))
                }
                if (mode == ProjectSetupMode.EXTEND) {
                    Text(source.orEmpty())
                    Text(stringResource(R.string.setupReadOnly))
                    TextButton(onClick = onChangeSource, enabled = ready,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-change-source")) {
                        Text(stringResource(R.string.setupChangeSource))
                    }
                    OutlinedButton(onClick = data.onRescan, enabled = ready,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-rescan")) {
                        Text(stringResource(R.string.setupRescan))
                    }
                } else Text(stringResource(R.string.setupCreateNoSource, targets.size))
                if (!ready || !coverageReady) {
                    Text(stringResource(if (data.coverage is CoverageUiState.Failed || data.catalogFailed)
                        R.string.setupUnavailable else R.string.projectInitializing), Modifier.testTag("setup-loading"))
                    if (data.coverage !is CoverageUiState.Failed && !data.catalogFailed) WavyLoadingBar(Modifier.fillMaxWidth())
                    val progress = data.coverage as? CoverageUiState.Loading
                    if (progress != null && progress.total > 0) Text(stringResource(R.string.setupScanProgress, progress.done, progress.total))
                }
                if (snapshot != null) {
                    val counts = snapshot.counts
                    Text(stringResource(R.string.setupSourceSummary), Modifier.semantics { heading() })
                    Text(stringResource(R.string.setupCoverageCounts, counts.total, counts.declared, counts.covered,
                        counts.unmapped, counts.broken, counts.unknown, counts.unavailable), Modifier.testTag("coverage-counts"))
                    val version = snapshot.index.stamp.versionCode
                    Text(if (version != null) stringResource(R.string.setupSourceVersion, version)
                        else stringResource(R.string.setupSourceVersionUnknown))
                    Text(stringResource(R.string.setupPreviewLimit))
                    if (counts.unknown + counts.unavailable > 0) Text(stringResource(R.string.setupUnknownWarning), Modifier.testTag("coverage-warning"))
                    if (counts.covered == counts.total && counts.total > 0) Text(stringResource(R.string.setupFullyCovered))
                    Text(stringResource(R.string.setupLocalSummary), Modifier.semantics { heading() })
                    Text(stringResource(R.string.setupLocalCounts, counts.locallyHandled, counts.locked, counts.actionable))
                }
                if (targets.any { it.ambiguousUser }) Text(stringResource(R.string.setupUserWarning), Modifier.testTag("setup-user-warning"))
            }
            if (ready && coverageReady) {
                item {
                    OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.setupSearch)) },
                        modifier = Modifier.fillMaxWidth().testTag("setup-search"), singleLine = true)
                    TextButton(onClick = {
                        selected = ArrayList(targets.filter {
                            if (mode == ProjectSetupMode.CREATE) it.selectable else byKey[it.key]?.actionable == true
                        }.map { it.key })
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-select-all")) {
                        Text(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.setupSelectAll else R.string.setupSelectMissing))
                    }
                    TextButton(onClick = { selected = arrayListOf() }, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-clear")) {
                        Text(stringResource(R.string.setupClear))
                    }
                    Text(stringResource(R.string.setupSelectedCounts, validSelected.size, targets.size), Modifier.testTag("setup-selection-count"))
                    if (mode == ProjectSetupMode.EXTEND) TextButton(onClick = { missingOnly = !missingOnly },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-filter")) {
                        Text(stringResource(if (missingOnly) R.string.setupShowAll else R.string.setupShowMissing))
                    }
                    if (filtered.isEmpty()) Text(stringResource(R.string.setupNoTargets), Modifier.testTag("setup-no-targets"))
                }
                items(filtered, key = { it.key }) { target ->
                    val entry = byKey[target.key]
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(value = target.key in validSelected, enabled = target.selectable, role = Role.Checkbox) {
                            selected = ArrayList(if (it) validSelected + target.key else validSelected - target.key)
                        }.testTag("setup-target-${target.key}"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.appIcon(target.key)
                            Checkbox(target.key in validSelected, onCheckedChange = null, enabled = target.selectable)
                            Text(target.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(target.key, style = MaterialTheme.typography.bodySmall)
                        if (entry != null) SourceEntryText(entry)
                        Text(stringResource(when (target.local) {
                            LocalIconState.NO_LOCAL_ICON -> R.string.setupLocalNone
                            LocalIconState.CUSTOM_OR_MANUAL -> R.string.setupLocalManual
                            LocalIconState.FALLBACK -> R.string.setupLocalFallback
                            LocalIconState.OTHER_APPLIED -> R.string.setupLocalApplied
                            LocalIconState.UNKNOWN -> R.string.setupLocalUnknown
                        }))
                        if (target.locked) Text(stringResource(R.string.setupLocked))
                        if (target.ambiguousUser) Text(stringResource(R.string.setupUserWarning))
                        if (entry != null && !entry.actionable && target.selectable) Text(stringResource(R.string.setupManualOverride))
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceEntryText(entry: CoverageEntry) {
    Text(stringResource(when (entry.source) {
        SourceCoverage.RESOLVABLE -> R.string.setupResolvable
        SourceCoverage.DECLARED_BROKEN -> R.string.setupBroken
        SourceCoverage.UNMAPPED -> R.string.setupUnmapped
        SourceCoverage.SOURCE_UNAVAILABLE -> R.string.setupSourceUnavailable
        SourceCoverage.UNKNOWN -> if (entry.reason == CoverageReason.DYNAMIC_UNPROVEN) R.string.setupDynamicUnknown else R.string.setupSourceUnknown
    }))
    entry.drawableName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
