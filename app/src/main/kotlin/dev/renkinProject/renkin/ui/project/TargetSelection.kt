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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.renkinProject.renkin.ai.trial.StyleTrialTargetSelector
import dev.renkinProject.renkin.data.project.ProjectSelection
import dev.renkinProject.renkin.apk.CoverageEntry
import dev.renkinProject.renkin.apk.CoverageReason
import dev.renkinProject.renkin.apk.LocalIconState
import dev.renkinProject.renkin.apk.SourceCoverage
import dev.renkinProject.renkin.apk.filterCoverageTargets
import dev.renkinProject.renkin.apk.uniqueCoverageTargets
import dev.renkinProject.renkin.drawable.toSafeBitmapOrNull
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.WavyLoadingBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ProjectOriginalIcon(drawable: Drawable?) {
    val bitmap = remember(drawable) { drawable?.toSafeBitmapOrNull(48, 48)?.asImageBitmap() }
    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(48.dp))
}

@Composable
internal fun TargetSelection(
    mode: ProjectSetupMode, profileId: Long, profileName: String, source: String?,
    data: ProjectSetupData, ready: Boolean, onBack: () -> Unit,
    onChangeSource: () -> Unit, onEditor: () -> Unit, onStyle: () -> Unit = {}, onTrials: () -> Unit = {},
    guidedStep: Int? = null, onContinue: () -> Unit = onStyle
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var missingOnly by rememberSaveable { mutableStateOf(mode == ProjectSetupMode.EXTEND) }
    var selectedStamp by rememberSaveable { mutableStateOf("") }
    var selectionBusy by remember { mutableStateOf(false) }
    var selectionError by remember { mutableStateOf(false) }
    var selectionSaved by remember { mutableStateOf(false) }
    var selectionEdited by rememberSaveable { mutableStateOf(false) }
    var invalidated by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val targets = uniqueCoverageTargets(data.targets)
    val snapshot = (data.coverage as? CoverageUiState.Ready)?.snapshot?.takeIf {
        it.profileId == profileId && it.index.stamp.packageName == source && it.entries.map { e -> e.target } == targets
    }
    val catalogStamp = projectCatalogStamp(targets)
    val sourceStamp = snapshot?.index?.let { projectSourceStamp(it) }
    val stamp = if (mode == ProjectSetupMode.CREATE) catalogStamp else sourceStamp?.let { "$it:$catalogStamp" }
    LaunchedEffect(stamp) {
        if (stamp != null) {
            if (selectedStamp.isNotEmpty() && selectedStamp != stamp) { selected = arrayListOf(); invalidated = true; selectionSaved = false }
            selectedStamp = stamp
        }
    }
    val byKey = snapshot?.entries.orEmpty().associateBy { it.target.key }
    val filtered = filterCoverageTargets(targets, query).filter {
        mode == ProjectSetupMode.CREATE || !missingOnly || byKey[it.key]?.source in setOf(SourceCoverage.UNMAPPED, SourceCoverage.DECLARED_BROKEN)
    }
    val trialCandidates = if (guidedStep == 3)
        StyleTrialTargetSelector.candidates(mode.name, targets, targets.map { it.key }, snapshot) else emptyList()
    val eligibleTrialKeys = trialCandidates.filter { it.eligible }.map { it.target.key }.toSet()
    val validSelected = selected.filter { key -> targets.any { it.key == key && it.selectable } &&
        (guidedStep != 3 || key in eligibleTrialKeys) }.toSet()
    val coverageReady = mode == ProjectSetupMode.CREATE || snapshot != null
    val savedSelection = remember(data.savedMetadata?.selectionJson) {
        runCatching { ProjectSelection.decode(data.savedMetadata?.selectionJson.orEmpty()) }.getOrNull()
    }
    val safeKeys = targets.filter { target -> target.selectable && (mode == ProjectSetupMode.CREATE ||
        byKey[target.key]?.source in setOf(SourceCoverage.RESOLVABLE, SourceCoverage.UNMAPPED, SourceCoverage.DECLARED_BROKEN)) &&
        (guidedStep != 3 || target.key in eligibleTrialKeys) }.map { it.key }.toSet()
    LaunchedEffect(profileId, mode, source, stamp, guidedStep, data.savedMetadata?.selectionJson, data.metadataLoading) {
        if (!data.metadataLoading && !selectionEdited && !invalidated && stamp != null &&
            savedSelection?.isCurrent(mode.name, source, sourceStamp, catalogStamp, safeKeys) == true) {
            val restored = if (guidedStep == 3 && savedSelection.componentKeys.size > 3)
                StyleTrialTargetSelector.recommend(trialCandidates.filter { it.target.key in savedSelection.componentKeys })
                else savedSelection.componentKeys
            selected = ArrayList(restored)
            if (restored != savedSelection.componentKeys) selectionEdited = true
            selectedStamp = stamp
        }
    }
    val persistReady = ready && !data.metadataLoading && !data.metadataFailed && !selectionBusy && coverageReady &&
        (mode == ProjectSetupMode.CREATE || (snapshot?.index?.state == dev.renkinProject.renkin.apk.CoverageIndexState.READY && snapshot.index.stamp.versionCode != null))
    var suggestedThisVisit by rememberSaveable(profileId, mode, source) { mutableStateOf(false) }
    // The operation's own busy flag must not cancel its suspended durable Save.
    // Real source/catalog/route changes still cancel and require fresh validation.
    LaunchedEffect(ready, coverageReady, stamp, data.metadataLoading, data.metadataFailed,
        data.savedMetadata?.version, guidedStep, data.automaticTrialSuggestions) {
        if (guidedStep == 3 && data.automaticTrialSuggestions && persistReady && savedSelection == null &&
            !selectionEdited && !invalidated && !suggestedThisVisit && data.savedMetadata?.activeRevision != null) {
            val suggested = StyleTrialTargetSelector.recommend(trialCandidates)
            if (suggested.isNotEmpty()) {
                suggestedThisVisit = true; selectionBusy = true
                try {
                    data.onSaveSelection(data.savedMetadata.version,
                        ProjectSelection(mode.name, source, sourceStamp, catalogStamp, suggested))
                    selected = ArrayList(suggested); selectedStamp = checkNotNull(stamp)
                    selectionSaved = true; selectionError = false
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { selectionError = true }
                finally { selectionBusy = false }
            }
        }
    }
    val trialReason = when {
        !persistReady -> if (data.metadataFailed) R.string.setupSelectionSaveError else R.string.projectInitializing
        data.savedMetadata?.activeRevision == null -> R.string.trialSaveStyle
        savedSelection == null || savedSelection.componentKeys.isEmpty() || selectionEdited -> R.string.trialSaveTargets
        !savedSelection.isCurrent(mode.name, source, sourceStamp, catalogStamp, safeKeys) || invalidated -> R.string.trialStaleTargets
        else -> null
    }
    if (guidedStep == 1) {
        ProjectSourceSummary(mode, profileId, profileName, source, data, ready, onBack, onChangeSource,
            onContinue, onStyle, onEditor)
        return
    }
    if (guidedStep == 3) {
        var targetDetails by rememberSaveable(profileId, mode, source) { mutableStateOf<String?>(null) }
        var details by rememberSaveable(profileId, mode, source) { mutableStateOf(false) }
        val savedCurrent = savedSelection?.isCurrent(mode.name, source, sourceStamp, catalogStamp, safeKeys) == true &&
            savedSelection.componentKeys.toSet() == validSelected && !invalidated
        val selectionReady = persistReady && validSelected.size in 1..3 && validSelected.all { it in safeKeys } &&
            data.savedMetadata?.activeRevision != null
        CenteredFullscreenContent {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("project-targets")) {
                item {
                    SetupHeading(stringResource(R.string.workflowChooseApps), onBack)
                    Text(profileName, style = MaterialTheme.typography.titleLarge)
                    data.savedMetadata?.activeRevision?.let { Text(stringResource(R.string.workflowSavedRevision, it)) }
                    if (!persistReady) {
                        Text(stringResource(if (data.metadataFailed || data.catalogFailed || data.coverage is CoverageUiState.Failed)
                            R.string.setupUnavailable else R.string.projectInitializing), Modifier.testTag("setup-loading"))
                    }
                    if (invalidated || savedSelection != null &&
                        !savedSelection.isCurrent(mode.name, source, sourceStamp, catalogStamp, safeKeys)) {
                        Text(stringResource(R.string.setupSavedSelectionStale), Modifier.testTag("setup-saved-selection"))
                    }
                    if (selectionError) Text(stringResource(R.string.setupSelectionSaveError), Modifier.testTag("setup-selection-error"))
                    if (selectionSaved) Text(stringResource(R.string.setupSelectionSaved), Modifier.testTag("setup-selection-saved"))
                    Text(stringResource(R.string.setupSelectedCounts, validSelected.size, targets.size),
                        Modifier.testTag("setup-selection-count"))
                    Button(onClick = {
                        if (selectionReady) {
                            if (savedCurrent) {
                                selectionEdited = false
                                onTrials()
                            } else {
                                selectionBusy = true
                                val version = data.savedMetadata?.version ?: 0
                                val selection = ProjectSelection(mode.name, source, sourceStamp, catalogStamp, validSelected.sorted())
                                scope.launch {
                                    try {
                                        data.onSaveSelection(version, selection)
                                        selectionSaved = true; selectionError = false
                                        invalidated = false; selectionEdited = false
                                        onTrials()
                                    } catch (cancel: CancellationException) { throw cancel }
                                    catch (_: Exception) { selectionError = true; selectionSaved = false }
                                    finally { selectionBusy = false }
                                }
                            }
                        }
                    }, enabled = selectionReady,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-save-selection")) {
                        Text(stringResource(if (savedCurrent) R.string.workflowSavedTargetsContinue else R.string.workflowSaveTargets))
                    }
                    if (!selectionReady && persistReady) Text(stringResource(if (data.savedMetadata?.activeRevision == null)
                        R.string.trialSaveStyle else R.string.workflowChooseReason), Modifier.testTag("setup-trials-reason"))
                    OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.setupSearch)) },
                        modifier = Modifier.fillMaxWidth().testTag("setup-search"), singleLine = true)
                    TextButton(onClick = {
                        val suggestion = StyleTrialTargetSelector.recommend(trialCandidates).toMutableList()
                        val alternate = trialCandidates.firstOrNull { it.recommended && it.target.key !in suggestion }?.target?.key
                        if (suggestion.toSet() == validSelected && alternate != null) suggestion[suggestion.lastIndex] = alternate
                        selected = ArrayList(suggestion)
                        selectionEdited = true; selectionSaved = false
                        if (data.automaticTrialSuggestions && suggestion.isNotEmpty()) {
                            selectionBusy = true
                            scope.launch {
                                try {
                                    data.onSaveSelection(data.savedMetadata?.version ?: 0,
                                        ProjectSelection(mode.name, source, sourceStamp, catalogStamp, suggestion.sorted()))
                                    selectionEdited = false; selectionSaved = true; selectionError = false; invalidated = false
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { selectionError = true }
                                finally { selectionBusy = false }
                            }
                        }
                    }, enabled = persistReady, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-select-all")) {
                        Text(stringResource(if (data.automaticTrialSuggestions) R.string.packStyleOtherApps else R.string.workflowRecommendApps))
                    }
                    if (mode == ProjectSetupMode.EXTEND) TextButton(onClick = { missingOnly = !missingOnly },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-filter")) {
                        Text(stringResource(if (missingOnly) R.string.setupShowAll else R.string.setupShowMissing))
                    }
                    if (filtered.isEmpty()) Text(stringResource(R.string.setupNoTargets))
                }
                if (ready && coverageReady) items(filtered, key = { it.key }) { target ->
                    val key = target.key
                    val eligible = key in eligibleTrialKeys && !selectionBusy &&
                        (key in validSelected || validSelected.size < 3)
                    val entry = byKey[key]
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = key in validSelected, enabled = eligible, role = Role.Checkbox) {
                            selected = ArrayList(if (it) validSelected + key else validSelected - key)
                            selectionEdited = true; selectionSaved = false
                        }.testTag("setup-target-$key"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.appIcon(key)
                            Checkbox(key in validSelected, onCheckedChange = null, enabled = eligible)
                            Text(target.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        }
                        if (key !in eligibleTrialKeys) Text(stringResource(when {
                            target.locked -> R.string.setupLocked
                            target.ambiguousUser -> R.string.setupUserWarning
                            target.local == LocalIconState.CUSTOM_OR_MANUAL -> R.string.setupLocalManual
                            target.local == LocalIconState.OTHER_APPLIED -> R.string.setupLocalApplied
                            else -> R.string.workflowAppUnavailable
                        }))
                        else if (!eligible && key !in validSelected && validSelected.size >= 3)
                            Text(stringResource(R.string.workflowMaximumApps))
                        if (entry?.source == SourceCoverage.RESOLVABLE) Text(stringResource(R.string.workflowCoveredReason))
                        TextButton(onClick = { targetDetails = if (targetDetails == key) null else key },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("setup-target-details-$key")) {
                            Text(stringResource(R.string.workflowDetails))
                        }
                        if (targetDetails == key) {
                            Text(key, style = MaterialTheme.typography.bodySmall)
                            entry?.let { SourceEntryText(it) }
                        }
                    }
                }
                item {
                    TextButton(onClick = onStyle, enabled = ready && !selectionBusy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-project-style")) {
                        Text(stringResource(R.string.workflowReviewStyle))
                    }
                    TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-details")) {
                        Text(stringResource(R.string.workflowDetails))
                    }
                    if (details) {
                        source?.let { Text(it) }
                        TextButton(onClick = { selected = arrayListOf(); selectionEdited = true; selectionSaved = false },
                            enabled = !selectionBusy, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-clear")) {
                            Text(stringResource(R.string.setupClear))
                        }
                        TextButton(onClick = data.onRescan, enabled = ready) { Text(stringResource(R.string.setupRescan)) }
                        TextButton(onClick = onChangeSource, enabled = ready) { Text(stringResource(R.string.setupChangeSource)) }
                        TextButton(onClick = onEditor, enabled = ready) { Text(stringResource(R.string.projectClassicEditor)) }
                    }
                }
            }
        }
        return
    }
    CenteredFullscreenContent {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("project-targets")) {
            item {
                SetupHeading(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.setupCreateTargets else R.string.setupExtendTargets), onBack)
                Text(profileName, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.projectIdentity, profileId))
                Text(stringResource(R.string.setupPreparationOnly))
                Text(stringResource(R.string.setupSelectionDurable))
                if (data.launcherInventoryChanged) Text(stringResource(R.string.setupLauncherChanged),
                    Modifier.testTag("setup-launcher-changed"))
                OutlinedButton(onClick = onStyle, enabled = ready && !selectionBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-project-style")) {
                    Text(stringResource(R.string.projectStyleTitle))
                }
                if (savedSelection != null) {
                    val current = savedSelection.isCurrent(mode.name, source, sourceStamp, catalogStamp, safeKeys)
                    Text(stringResource(if (current) R.string.setupSavedSelectionReview else R.string.setupSavedSelectionStale),
                        Modifier.testTag("setup-saved-selection"))
                    OutlinedButton(onClick = {
                        selected = ArrayList(savedSelection.componentKeys.filter { it in safeKeys })
                        invalidated = false
                        selectionSaved = false
                        selectionEdited = true
                    }, enabled = persistReady, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-revalidate-selection")) {
                        Text(stringResource(R.string.setupReviewSavedTargets))
                    }
                }
                OutlinedButton(onClick = onTrials, enabled = trialReason == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-trials")) {
                    Text(stringResource(R.string.trialTitle))
                }
                trialReason?.let { Text(stringResource(it), Modifier.testTag("setup-trials-reason")) }
                if (invalidated) Text(stringResource(R.string.setupSavedSelectionStale))
                if (selectionError || data.metadataFailed) Text(stringResource(R.string.setupSelectionSaveError), Modifier.testTag("setup-selection-error"))
                if (selectionSaved) Text(stringResource(R.string.setupSelectionSaved), Modifier.testTag("setup-selection-saved"))
                OutlinedButton(onClick = {
                    if (!persistReady) return@OutlinedButton
                    selectionBusy = true
                    val version = data.savedMetadata?.version ?: 0
                    val selection = ProjectSelection(mode.name, source, sourceStamp, catalogStamp, validSelected.sorted())
                    scope.launch {
                        try { data.onSaveSelection(version, selection); selectionSaved = true; selectionError = false; invalidated = false; selectionEdited = false }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { selectionError = true; selectionSaved = false }
                        finally { selectionBusy = false }
                    }
                }, enabled = persistReady && validSelected.all { it in safeKeys },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-save-selection")) {
                    Text(stringResource(R.string.setupSaveSelection))
                }
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
                        selectionSaved = false
                        selectionEdited = true
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-select-all")) {
                        Text(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.setupSelectAll else R.string.setupSelectMissing))
                    }
                    TextButton(onClick = { selected = arrayListOf(); selectionSaved = false; selectionEdited = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-clear")) {
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
                            selectionSaved = false
                            selectionEdited = true
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
