package dev.renkinProject.renkin.ui.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.data.project.ProjectAiMetadata
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import kotlinx.coroutines.CancellationException

/** Navigation is presentation; saved revisions, selections and approvals stay repository-owned. */
internal fun savedStyleMatchesSetup(metadata: ProjectAiMetadata?, id: Long, mode: ProjectSetupMode,
    source: String?, stamp: String?): Boolean = metadata != null && metadata.profileId == id && metadata.activeRevision != null &&
    metadata.mode == mode.name && metadata.sourcePackage == source &&
    (mode == ProjectSetupMode.CREATE && source == null && metadata.sourceStamp == null ||
        mode == ProjectSetupMode.EXTEND && stamp != null && metadata.sourceStamp == stamp)

@Composable
internal fun ProjectWorkflowHeading(step: Int) {
    val label = stringResource(when (step) {
        1 -> R.string.workflowProjectSource
        2 -> R.string.workflowStyle
        3 -> R.string.workflowTrials
        else -> R.string.workflowCompletion
    })
    Text(stringResource(R.string.workflowProgress, step, label),
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("workflow-step").semantics { heading() },
        style = MaterialTheme.typography.titleMedium)
}

@Composable
internal fun ProjectSourceSummary(mode: ProjectSetupMode, id: Long, name: String, source: String?,
    data: ProjectSetupData, ready: Boolean, onBack: () -> Unit, onChangeSource: () -> Unit,
    onContinue: () -> Unit, onStyle: () -> Unit, onEditor: () -> Unit) {
    var details by rememberSaveable(id, mode, source) { mutableStateOf(false) }
    val snapshot = (data.coverage as? CoverageUiState.Ready)?.snapshot?.takeIf {
        it.profileId == id && it.index.stamp.packageName == source
    }
    val stamp = snapshot?.index?.let(::projectSourceStamp)
    val sourceReady = snapshot?.index?.state == dev.renkinProject.renkin.apk.CoverageIndexState.READY &&
        snapshot.index.stamp.versionCode != null
    val saved = savedStyleMatchesSetup(data.savedMetadata, id, mode, source, stamp)
    val sourceName = data.packs.find { it.packageName == source }?.applicationName
    CenteredFullscreenContent {
        LazyColumn(Modifier.testTag("project-targets"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SetupHeading(stringResource(R.string.workflowProjectSource), onBack)
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.workflowCreateSummary
                    else R.string.workflowExtendSummary, sourceName ?: stringResource(R.string.workflowSelectedSource)))
                if (mode == ProjectSetupMode.EXTEND) {
                    Text(stringResource(when {
                        sourceReady -> R.string.workflowSourceAvailable
                        data.coverage is CoverageUiState.Failed || snapshot != null -> R.string.workflowSourceUnavailable
                        else -> R.string.workflowSourceChecking
                    }))
                    if (sourceReady) snapshot?.counts?.let { Text(stringResource(R.string.workflowMissingCoverage, it.actionable)) }
                }
                data.savedMetadata?.activeRevision?.let {
                    Text(stringResource(R.string.workflowSavedRevision, it), Modifier.testTag("workflow-source-style-saved"))
                }
                if (!ready || data.metadataLoading) Text(stringResource(R.string.projectInitializing))
                if (data.metadataFailed) Text(stringResource(R.string.setupSelectionSaveError))
                Button(onClick = onContinue, enabled = ready && !data.metadataLoading && !data.metadataFailed &&
                    (mode != ProjectSetupMode.EXTEND || sourceReady),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-project-style")) {
                    Text(stringResource(if (saved) R.string.workflowContinueTargets else R.string.workflowContinueStyle))
                }
                if (data.savedMetadata?.activeRevision != null) TextButton(onClick = onStyle, enabled = ready,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("setup-edit-style")) {
                    Text(stringResource(R.string.workflowReviewStyle))
                }
                if (mode == ProjectSetupMode.EXTEND) TextButton(onClick = onChangeSource, enabled = ready,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("setup-change-source")) {
                    Text(stringResource(R.string.setupChangeSource))
                }
                TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("setup-details")) { Text(stringResource(R.string.workflowDetails)) }
                if (details) {
                    Text(stringResource(R.string.projectIdentity, id))
                    source?.let { Text(it) }
                    snapshot?.counts?.let { c -> Text(stringResource(R.string.setupCoverageCounts, c.total,
                        c.declared, c.covered, c.unmapped, c.broken, c.unknown, c.unavailable)) }
                    OutlinedButton(onClick = data.onRescan, enabled = ready) { Text(stringResource(R.string.setupRescan)) }
                    TextButton(onClick = onEditor, enabled = ready) { Text(stringResource(R.string.projectClassicEditor)) }
                }
            }
        }
    }
}

/** Reads existing persistence and current approval proof; opening this page is never a paid action. */
@Composable
internal fun ProjectCompletionPanel(id: Long, name: String, mode: ProjectSetupMode, source: String?,
    sourceName: String?, available: Boolean, styleBackend: ProjectStyleBackend, backend: ProjectTrialBackend,
    onBack: () -> Unit, onStyle: () -> Unit, onTargets: () -> Unit, onEditor: () -> Unit, onHome: () -> Unit) {
    val state by backend.state(id).collectAsState()
    var readFailed by remember(id, available) { mutableStateOf(false) }
    var styleLoaded by remember(id, available) { mutableStateOf(false) }
    val saved by produceState<SavedProjectStyle?>(null, id, available, styleBackend) {
        readFailed = false
        styleLoaded = false
        if (available) try { value = styleBackend.load(id) }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { readFailed = true }
        finally { styleLoaded = true }
    }
    LaunchedEffect(id, mode, source, available, state.network) {
        if (available && !state.network) backend.load(id, mode, source)
    }
    DisposableEffect(id, backend) { onDispose { backend.release(id) } }
    BackHandler(onBack = onBack)
    val review = trialReviewSummary(state)
    val checking = state.busy || !styleLoaded || state.preparation == null && state.error == null
    val approved = available && review.ready
    val attention = state.records.sumOf { record -> record.items.count {
        it.status.name in setOf("PENDING", "REQUESTING", "PNG_SAVED", "UNCERTAIN", "FAILED", "SKIPPED")
    } }
    var details by rememberSaveable(id) { mutableStateOf(false) }
    CenteredFullscreenContent {
        LazyColumn(Modifier.testTag("project-completion-$id"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SetupHeading(stringResource(R.string.workflowCompletion), onBack)
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.workflowCreateSummary
                    else R.string.workflowExtendSummary, sourceName ?: stringResource(R.string.workflowSelectedSource)))
                Text(if (!styleLoaded) stringResource(R.string.projectInitializing)
                    else if (saved?.revision != null) stringResource(R.string.workflowSavedRevision, saved!!.revision!!.revision)
                    else stringResource(if (readFailed) R.string.projectStyleLoadError else R.string.trialSaveStyle),
                    Modifier.testTag("completion-style"))
                Text(stringResource(R.string.workflowStoredTrials, state.variants.size))
                Text(stringResource(R.string.workflowReviewCounts, review.accepted.size, review.unreviewed.size))
                Text(stringResource(when {
                    checking -> R.string.workflowCheckingStatus
                    approved -> R.string.workflowApproved
                    else -> R.string.workflowApprovalPending
                }),
                    Modifier.testTag("completion-approval"))
                if (attention > 0) Text(stringResource(R.string.workflowRequestsAttention, attention))
                if (state.network) Text(stringResource(R.string.workflowRequestRunning))
                else if (state.busy) Text(stringResource(R.string.workflowCheckingStatus))
                else if (state.error != null) Text(stringResource(R.string.workflowReviewContext))
                Text(stringResource(R.string.workflowEditorBoundary))
                Button(onClick = if (approved) onEditor else onBack, enabled = available && !checking,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("completion-next")) {
                    Text(stringResource(if (approved) R.string.projectClassicEditor else R.string.workflowContinueReview))
                }
                TextButton(onClick = onStyle, enabled = available && !state.busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("completion-style-edit")) {
                    Text(stringResource(R.string.workflowReviewStyle))
                }
                TextButton(onClick = onTargets, enabled = available && !state.busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("completion-targets")) {
                    Text(stringResource(R.string.workflowChooseApps))
                }
                TextButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp).testTag("completion-home")) {
                    Text(stringResource(R.string.projectBackHome))
                }
                TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.workflowDetails))
                }
                if (details) {
                    source?.let { Text(it) }
                    state.records.forEach { record ->
                        Text(record.plan.id)
                        record.items.forEach { Text(it.status.name) }
                    }
                    TextButton(onClick = onEditor, enabled = available && !state.busy) {
                        Text(stringResource(R.string.projectClassicEditor))
                    }
                }
            }
        }
    }
}
