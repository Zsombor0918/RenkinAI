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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import dev.renkinProject.renkin.data.project.ProjectAiMetadata

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
        setupData = { mode, page, id, source -> rememberProjectSetupData(viewModel, mode, page, id, source) },
        readSetupContext = viewModel::projectMetadata,
        stylePanel = { id, mode, source, stamp, back, trials ->
            ProjectStylePanel(id, mode, source, stamp, home.actionsEnabled && id == home.activeProfileId,
                viewModel.projectStyleBackend, back, trials, onStyleSaved = { trials() }, guided = true)
        },
        bindTrialContext = viewModel.projectTrials::bindContext,
        trialPanel = { id, mode, source, back, refine, editor ->
            ProjectTrialPanel(id, home.profiles.find { it.id == id }?.name.orEmpty(), mode, source,
                home.actionsEnabled && id == home.activeProfileId, viewModel.projectTrials, back, refine, editor,
                appIcon = { key -> ProjectOriginalIcon(viewModel.applicationList.find { it.key == key }?.icon) })
        },
        guidedWorkflow = true,
        guidedTrialPanel = { id, mode, source, back, refine, editor, complete ->
            ProjectTrialPanel(id, home.profiles.find { it.id == id }?.name.orEmpty(), mode, source,
                home.actionsEnabled && id == home.activeProfileId, viewModel.projectTrials, back, refine, editor,
                appIcon = { key -> ProjectOriginalIcon(viewModel.applicationList.find { it.key == key }?.icon) },
                onComplete = complete,
                appName = { key -> viewModel.applicationList.find { it.key == key }?.appName }, guided = true)
        },
        completionPanel = { id, mode, source, back, refine, targets, editor, homeRoute ->
            ProjectCompletionPanel(id, home.profiles.find { it.id == id }?.name.orEmpty(), mode, source,
                viewModel.iconPacks.find { it.packageName == source }?.applicationName,
                home.actionsEnabled && id == home.activeProfileId, viewModel.projectStyleBackend,
                viewModel.projectTrials, back, refine, targets, editor, homeRoute)
        },
        editor = { navigateHome ->
            MainColumn(viewModel.iconPacks, onNavigateHome = navigateHome,
                viewModel = viewModel, collectToastEvents = false)
        }
    )
}

private enum class ProjectRoute { HOME, SETUP, STYLE, TRIAL, COMPLETE, EDITOR }

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
    setupData: @Composable (ProjectSetupMode, ProjectSetupPage, Long?, String?) -> ProjectSetupData = { _, _, _, _ -> ProjectSetupData() },
    readSetupContext: suspend (Long) -> ProjectAiMetadata? = { null },
    stylePanel: @Composable (Long, ProjectSetupMode, String?, String?, () -> Unit, () -> Unit) -> Unit = { _, _, _, _, back, _ ->
        TextButton(onClick = back) { Text(stringResource(R.string.projectBackHome)) }
    },
    bindTrialContext: (Long, ProjectSetupMode, String?) -> Unit = { _, _, _ -> },
    trialPanel: @Composable (Long, ProjectSetupMode, String?, () -> Unit, () -> Unit, () -> Unit) -> Unit = { _, _, _, back, _, _ ->
        TextButton(onClick = back) { Text(stringResource(R.string.setupBack)) }
    },
    guidedWorkflow: Boolean = false,
    guidedTrialPanel: (@Composable (Long, ProjectSetupMode, String?, () -> Unit, () -> Unit, () -> Unit, () -> Unit) -> Unit)? = null,
    completionPanel: @Composable (Long, ProjectSetupMode, String?, () -> Unit, () -> Unit, () -> Unit, () -> Unit, () -> Unit) -> Unit =
        { _, _, _, back, _, _, _, _ -> TextButton(onClick = back) { Text(stringResource(R.string.setupBack)) } },
    editor: @Composable (onNavigateHome: () -> Unit) -> Unit
) {
    var route by rememberSaveable { mutableStateOf(ProjectRoute.HOME) }
    var consumedEntry by rememberSaveable { mutableLongStateOf(0) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var setupMode by rememberSaveable { mutableStateOf(ProjectSetupMode.CREATE) }
    var setupPage by rememberSaveable { mutableStateOf(ProjectSetupPage.SOURCE) }
    var setupProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sourcePackage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSetup by rememberSaveable { mutableStateOf(false) }
    var setupContexts by rememberSaveable { mutableStateOf(hashMapOf<String, String>()) }
    var exitOpen by rememberSaveable { mutableStateOf(false) }
    var styleStamp by rememberSaveable { mutableStateOf<String?>(null) }
    var guidedStage by rememberSaveable { mutableIntStateOf(1) }
    var contextBusy by remember { mutableStateOf(false) }
    var contextFailed by remember { mutableStateOf(false) }
    var contextRequest by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val editorState = rememberSaveableStateHolder()
    androidx.compose.runtime.SideEffect { bindTrialContext(setupProfileId ?: state.activeProfileId, setupMode, sourcePackage) }

    LaunchedEffect(operation, state.availability, state.activeProfileId, state.busy) {
        if (operation is ProjectSessionState.Opened) {
            if (state.activeProfileId == operation.profileId && state.actionsEnabled) {
                if (pendingSetup) {
                    setupProfileId = operation.profileId
                    setupContexts = HashMap(setupContexts).apply { put(operation.profileId.toString(), sourcePackage.orEmpty()) }
                    setupPage = ProjectSetupPage.TARGETS
                    route = ProjectRoute.SETUP
                } else route = ProjectRoute.EDITOR
                pendingSetup = false
                onAcknowledge()
            } else if (!state.busy && state.activeProfileId != operation.profileId) onAcknowledge()
        }
        if (operation is ProjectSessionState.Failed || operation is ProjectSessionState.Interrupted) pendingSetup = false
    }
    LaunchedEffect(editorEntryVersion, editorAttention) {
        if ((editorEntryVersion > 0 && editorEntryVersion != consumedEntry) || editorAttention) {
            contextRequest++
            contextBusy = false
            consumedEntry = editorEntryVersion
            onCancel()
            createOpen = false
            pendingSetup = false
            exitOpen = false
            route = ProjectRoute.EDITOR
        }
    }

    val navigateHome = { route = ProjectRoute.HOME }
    val cancelOperation = { pendingSetup = false; onCancel() }
    val setupBack = {
        if (!state.busy) {
            if (guidedWorkflow && setupPage == ProjectSetupPage.TARGETS && guidedStage == 3) {
                route = ProjectRoute.STYLE
            } else when (setupPage) {
                ProjectSetupPage.SOURCE -> route = ProjectRoute.HOME
                ProjectSetupPage.PROFILE -> setupPage = ProjectSetupPage.SOURCE
                ProjectSetupPage.TARGETS -> if (setupMode == ProjectSetupMode.EXTEND) setupPage = ProjectSetupPage.PROFILE
                    else route = ProjectRoute.HOME
            }
        }
    }
    // A loading session cannot be mistaken for the previous editor. Dialog/search handlers
    // within MainColumn own Editor Back; no later root handler overrides them.
    BackHandler(enabled = route == ProjectRoute.HOME) {
        when {
            state.busy || contextBusy -> Unit
            operation is ProjectSessionState.ConfirmSave -> cancelOperation()
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
    } else if (route == ProjectRoute.COMPLETE) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (guidedWorkflow) ProjectWorkflowHeading(4)
            completionPanel(setupProfileId ?: state.activeProfileId, setupMode, sourcePackage,
                { route = ProjectRoute.TRIAL }, { route = ProjectRoute.STYLE },
                { guidedStage = 3; route = ProjectRoute.SETUP },
                { pendingSetup = false; onOpen(setupProfileId ?: state.activeProfileId) }, navigateHome)
        }
    } else if (route == ProjectRoute.TRIAL) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (guidedWorkflow) ProjectWorkflowHeading(3)
            editorState.SaveableStateProvider("trials-$setupProfileId") {
                val back = { guidedStage = 3; route = ProjectRoute.SETUP }
                val refine = { route = ProjectRoute.STYLE }
                val openEditor = { pendingSetup = false; onOpen(setupProfileId ?: state.activeProfileId) }
                val guidedPanel = guidedTrialPanel
                if (guidedWorkflow && guidedPanel != null) {
                    guidedPanel(setupProfileId ?: state.activeProfileId, setupMode, sourcePackage,
                        back, refine, openEditor, { route = ProjectRoute.COMPLETE })
                } else trialPanel(setupProfileId ?: state.activeProfileId, setupMode, sourcePackage,
                    back, refine, openEditor)
            }
        }
    } else if (route == ProjectRoute.STYLE) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (guidedWorkflow) ProjectWorkflowHeading(2)
            editorState.SaveableStateProvider("style-$setupProfileId") {
                stylePanel(setupProfileId ?: state.activeProfileId, setupMode, sourcePackage, styleStamp,
                    { route = ProjectRoute.SETUP }, {
                        if (guidedWorkflow) { guidedStage = 3; route = ProjectRoute.SETUP }
                        else route = ProjectRoute.TRIAL
                    })
            }
        }
    } else if (route == ProjectRoute.SETUP) {
        BackHandler { if (operation is ProjectSessionState.ConfirmSave) cancelOperation() else setupBack() }
        val data = setupData(setupMode, setupPage, setupProfileId, sourcePackage)
        val currentSourceStamp = if (setupMode == ProjectSetupMode.EXTEND) {
            (data.coverage as? CoverageUiState.Ready)?.snapshot?.takeIf {
                it.profileId == setupProfileId && it.index.stamp.packageName == sourcePackage &&
                    it.index.state == dev.renkinProject.renkin.apk.CoverageIndexState.READY && it.index.stamp.versionCode != null
            }?.let { projectSourceStamp(it.index) }
        } else null
        val openStyle = { styleStamp = currentSourceStamp; route = ProjectRoute.STYLE }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (guidedWorkflow) ProjectWorkflowHeading(if (setupPage == ProjectSetupPage.TARGETS) guidedStage else 1)
            editorState.SaveableStateProvider("setup-$setupProfileId-$setupMode-$sourcePackage") {
                ProjectSetup(setupMode, setupPage, setupProfileId, sourcePackage, state, data,
                    onSource = { sourcePackage = it; setupPage = ProjectSetupPage.PROFILE },
                    onProfile = { pendingSetup = true; onOpen(it) },
                    onNewProfile = { createOpen = true },
                    onBack = setupBack,
                    onChangeSource = { setupPage = ProjectSetupPage.SOURCE },
                    onStyle = openStyle,
                    onTrials = { route = ProjectRoute.TRIAL },
                    onEditor = {
                        pendingSetup = false
                        onOpen(if (setupPage == ProjectSetupPage.TARGETS) setupProfileId ?: state.activeProfileId else state.activeProfileId)
                    }, guidedStep = if (guidedWorkflow) guidedStage else null,
                    onContinue = {
                        if (savedStyleMatchesSetup(data.savedMetadata, setupProfileId ?: state.activeProfileId,
                                setupMode, sourcePackage, currentSourceStamp)) {
                            guidedStage = 3
                        } else openStyle()
                    })
            }
        }
    } else {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ProjectHome(state.copy(busy = state.busy || contextBusy, operationFailed = state.operationFailed || contextFailed),
                onOpen = { pendingSetup = false; onOpen(it) },
                onCreate = { guidedStage = 1; setupMode = ProjectSetupMode.CREATE; sourcePackage = null; createOpen = true },
                onExtend = { guidedStage = 1; setupMode = ProjectSetupMode.EXTEND; setupPage = ProjectSetupPage.SOURCE; sourcePackage = null; route = ProjectRoute.SETUP },
                onClassicEditor = { pendingSetup = false; onOpen(state.activeProfileId) },
                onRetry = onRetry,
                onSetup = { id ->
                    if (!contextBusy) {
                        contextBusy = true
                        contextFailed = false
                        val request = ++contextRequest
                        scope.launch {
                            try {
                                val saved = readSetupContext(id)
                                if (request == contextRequest && route == ProjectRoute.HOME) {
                                    sourcePackage = saved?.sourcePackage ?: setupContexts[id.toString()]?.takeIf { it.isNotEmpty() }
                                    setupMode = if (saved != null) ProjectSetupMode.valueOf(saved.mode)
                                        else if (sourcePackage == null) ProjectSetupMode.CREATE else ProjectSetupMode.EXTEND
                                    if (setupMode == ProjectSetupMode.CREATE) sourcePackage = null
                                    guidedStage = 1
                                    pendingSetup = true
                                    onOpen(id)
                                }
                            } catch (cancel: CancellationException) { throw cancel }
                            catch (_: Exception) { if (request == contextRequest) contextFailed = true }
                            finally { if (request == contextRequest) contextBusy = false }
                        }
                    }
                })
        }
        homeDialogs()
    }

    if (createOpen) {
        ProfileDetailsDialog(
            title = stringResource(R.string.projectCreate),
            confirmLabel = stringResource(R.string.createProfileAction),
            onConfirm = { name, description, label ->
                if (onCreate(name, description, label)) { createOpen = false; pendingSetup = true }
            },
            onDismiss = { createOpen = false }
        )
    }
    if (operation is ProjectSessionState.ConfirmSave) {
        RenkinAlertDialog(
            onDismissRequest = cancelOperation,
            title = { Text(stringResource(R.string.saveBeforeSwitchTitle)) },
            text = { Text(stringResource(R.string.projectSaveBeforeSwitch)) },
            confirmButton = {
                TextButton(onClick = onSave, modifier = Modifier.testTag("project-save-continue")) {
                    Text(stringResource(R.string.saveAction))
                }
            },
            dismissButton = {
                TextButton(onClick = cancelOperation, modifier = Modifier.testTag("project-save-cancel")) {
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
