package dev.renkinProject.renkin.ui.project

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.MainViewModel
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.LocalToaster
import dev.renkinProject.renkin.ui.MainColumn
import dev.renkinProject.renkin.ui.ProfileDetailsDialog
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.WavyLoadingBar
import dev.renkinProject.renkin.ui.WhatsNewDialog
import dev.renkinProject.renkin.ui.getCurrentMainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

@Composable
internal fun RenkinAiRoot(viewModel: MainViewModel) {
    var listRetry by remember { mutableIntStateOf(0) }
    var listFailed by remember { mutableStateOf(false) }
    val profiles by produceState<List<Profile>?>(null, viewModel, listRetry) {
        listFailed = false
        value = null
        try {
            viewModel.profiles.collect { value = it }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            listFailed = true
        }
    }
    val session = viewModel.projectSession
    val operation = session.state
    val failed = operation is ProjectSessionState.Failed
    val interrupted = operation is ProjectSessionState.Interrupted
    val home = ProjectHomeState(
        profiles = profiles.orEmpty(),
        activeProfileId = viewModel.activeProfileId,
        availability = when {
            listFailed || viewModel.startupFailed || (failed && !viewModel.profileSummaryReady) -> ProjectHomeAvailability.UNAVAILABLE
            profiles == null || viewModel.startupLoading || !viewModel.profileSummaryReady -> ProjectHomeAvailability.INITIALIZING
            else -> ProjectHomeAvailability.READY
        },
        busy = operation is ProjectSessionState.Loading || viewModel.isProfileSwitching || viewModel.isRefreshing,
        hasUnsavedChanges = viewModel.profileSummaryReady && viewModel.hasUnsavedChanges(),
        operationFailed = failed,
        operationInterrupted = interrupted
    )
    val context = LocalContext.current
    val toaster = LocalToaster.current
    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { toaster.show(context.getString(it)) }
    }
    // VM-owned prompts stay in MainColumn. Bring their owner into the tree rather than
    // leaving a pending watch/import/installer dialog on an invisible route.
    val editorAttention = viewModel.pendingWatchSuggestionId != null || viewModel.watchProfileMissing ||
        viewModel.showMissingPacksDialog || viewModel.pendingBackupImport != null ||
        viewModel.newIconPackInstalled != null || viewModel.installerPromptReason != null ||
        viewModel.installFailure != null || viewModel.installFallbackPending || viewModel.buildOutcome != null
    val activity = getCurrentMainActivity()
    ProjectShell(
        state = home,
        operation = operation,
        editorEntryVersion = viewModel.editorEntryVersion,
        editorAttention = editorAttention,
        onOpen = { session.open(it) },
        onCreate = { name, description, label -> session.create(name, description, label) },
        onSave = session::saveAndContinue,
        onCancel = session::cancel,
        onAcknowledge = session::acknowledge,
        onRetry = {
            listRetry++
            if (viewModel.startupFailed || !viewModel.startupComplete) viewModel.retryStartup()
            else if (!viewModel.profileSummaryReady) session.open(viewModel.activeProfileId)
            else session.acknowledge()
        },
        onExit = activity::finish,
        homeDialogs = { if (viewModel.whatsNewVisible) WhatsNewDialog(viewModel::dismissWhatsNew) },
        editor = { navigateHome ->
            MainColumn(viewModel.iconPacks, onNavigateHome = navigateHome,
                viewModel = viewModel, collectToastEvents = false)
        }
    )
}

private enum class ProjectRoute { HOME, EDITOR }

/** UI-only routes/forms; profile operations are user callbacks, never restoration effects. */
@Composable
internal fun ProjectShell(
    state: ProjectHomeState,
    operation: ProjectSessionState,
    editorEntryVersion: Long = 0,
    editorAttention: Boolean = false,
    onOpen: (Long) -> Unit,
    onCreate: (String, String, String) -> Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onAcknowledge: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    homeDialogs: @Composable () -> Unit = {},
    editor: @Composable (onNavigateHome: () -> Unit) -> Unit
) {
    var route by rememberSaveable { mutableStateOf(ProjectRoute.HOME) }
    var consumedEntry by rememberSaveable { mutableLongStateOf(0) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var extendOpen by rememberSaveable { mutableStateOf(false) }
    var exitOpen by rememberSaveable { mutableStateOf(false) }
    val editorState = rememberSaveableStateHolder()

    LaunchedEffect(operation, state.availability, state.activeProfileId, state.busy) {
        if (operation is ProjectSessionState.Opened) {
            if (state.activeProfileId == operation.profileId && state.actionsEnabled) {
                route = ProjectRoute.EDITOR
                onAcknowledge()
            } else if (!state.busy && state.activeProfileId != operation.profileId) onAcknowledge()
        }
    }
    LaunchedEffect(editorEntryVersion, editorAttention) {
        if ((editorEntryVersion > 0 && editorEntryVersion != consumedEntry) || editorAttention) {
            consumedEntry = editorEntryVersion
            onCancel()
            createOpen = false
            extendOpen = false
            exitOpen = false
            route = ProjectRoute.EDITOR
        }
    }

    val navigateHome = { route = ProjectRoute.HOME }
    // A loading session cannot be mistaken for the previous editor. Dialog/search handlers
    // within MainColumn own Editor Back; no later root handler overrides them.
    BackHandler(enabled = route == ProjectRoute.HOME) {
        when {
            state.busy -> Unit
            operation is ProjectSessionState.ConfirmSave -> onCancel()
            state.hasUnsavedChanges -> exitOpen = true
            else -> onExit()
        }
    }
    if (route == ProjectRoute.EDITOR) {
        val ready = state.availability == ProjectHomeAvailability.READY && operation !is ProjectSessionState.Loading
        Box(Modifier.fillMaxSize()) {
            // Keep launcher registrations and the one classic session alive during a legacy
            // switch/watch load. Do not key this composition by an early-changing profile ID.
            editorState.SaveableStateProvider("classic-editor") {
                val concealed = if (ready) Modifier else Modifier.clearAndSetSemantics {}.graphicsLayer { alpha = 0f }
                Box(Modifier.fillMaxSize().then(concealed).testTag("project-editor")) { editor(navigateHome) }
            }
            if (!ready) {
                Surface(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = {}) }) {
                    CenteredFullscreenContent(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                        Column(Modifier.fillMaxSize().padding(24.dp).testTag("project-editor-loading"),
                            verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(if (state.availability == ProjectHomeAvailability.UNAVAILABLE)
                                R.string.projectUnavailable else R.string.projectOpening))
                            if (state.availability != ProjectHomeAvailability.UNAVAILABLE) WavyLoadingBar(Modifier.fillMaxWidth())
                            else OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text(stringResource(R.string.projectReload)) }
                            OutlinedButton(onClick = navigateHome, enabled = !state.busy) { Text(stringResource(R.string.projectBackHome)) }
                        }
                    }
                }
            }
        }
        BackHandler(enabled = !ready) { if (!state.busy) navigateHome() }
    } else {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ProjectHome(state,
                onOpen = onOpen,
                onCreate = { createOpen = true },
                onExtend = { extendOpen = true },
                onClassicEditor = { onOpen(state.activeProfileId) },
                onRetry = onRetry)
        }
        homeDialogs()
    }

    if (createOpen) {
        ProfileDetailsDialog(
            title = stringResource(R.string.projectCreate),
            confirmLabel = stringResource(R.string.createProfileAction),
            onConfirm = { name, description, label ->
                if (onCreate(name, description, label)) createOpen = false
            },
            onDismiss = { createOpen = false }
        )
    }
    if (extendOpen) {
        RenkinAlertDialog(
            onDismissRequest = { extendOpen = false },
            title = { Text(stringResource(R.string.projectExtend)) },
            text = { Text(stringResource(R.string.projectExtendInstructions)) },
            confirmButton = {
                TextButton(onClick = { extendOpen = false; onOpen(state.activeProfileId) },
                    enabled = state.actionsEnabled, modifier = Modifier.testTag("project-extend-editor")) {
                    Text(stringResource(R.string.projectClassicEditor))
                }
            },
            dismissButton = {
                TextButton(onClick = { extendOpen = false; createOpen = true },
                    enabled = state.actionsEnabled, modifier = Modifier.testTag("project-extend-create")) {
                    Text(stringResource(R.string.projectExtensionCreate))
                }
            }
        )
    }
    if (operation is ProjectSessionState.ConfirmSave) {
        RenkinAlertDialog(
            onDismissRequest = onCancel,
            title = { Text(stringResource(R.string.saveBeforeSwitchTitle)) },
            text = { Text(stringResource(R.string.projectSaveBeforeSwitch)) },
            confirmButton = {
                TextButton(onClick = onSave, modifier = Modifier.testTag("project-save-continue")) {
                    Text(stringResource(R.string.saveAction))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel, modifier = Modifier.testTag("project-save-cancel")) {
                    Text(stringResource(R.string.colorizeCancel))
                }
            }
        )
    }
    if (exitOpen) {
        RenkinAlertDialog(
            onDismissRequest = { exitOpen = false },
            title = { Text(stringResource(R.string.projectExitTitle)) },
            text = { Text(stringResource(R.string.projectExitUnsaved)) },
            confirmButton = {
                TextButton(onClick = { exitOpen = false; onOpen(state.activeProfileId) }) {
                    Text(stringResource(R.string.projectReturnEditor))
                }
            },
            dismissButton = {
                TextButton(onClick = { exitOpen = false; onExit() }, modifier = Modifier.testTag("project-exit-discard")) {
                    Text(stringResource(R.string.projectExitDiscard))
                }
            }
        )
    }
}

/** Registered before editor children, retaining their search/dialog Back priority. */
@Composable
internal fun ProjectEditorBackHandler(onNavigateHome: (() -> Unit)?, onLegacyBack: () -> Unit) {
    BackHandler { if (onNavigateHome != null) onNavigateHome() else onLegacyBack() }
}
