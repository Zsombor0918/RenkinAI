package dev.renkinProject.renkin.ui.project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import dev.renkinProject.renkin.MainViewModel
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.apk.CoverageSnapshot
import dev.renkinProject.renkin.apk.CoverageTarget
import dev.renkinProject.renkin.apk.IconCoverageAnalyzer
import dev.renkinProject.renkin.data.IconPack
import dev.renkinProject.renkin.data.project.ProjectAiMetadata
import dev.renkinProject.renkin.data.project.ProjectSelection
import dev.renkinProject.renkin.packages.PackageResourceResolver
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.WavyLoadingBar
import kotlinx.coroutines.CancellationException

internal enum class ProjectSetupMode { CREATE, EXTEND }
internal enum class ProjectSetupPage { SOURCE, PROFILE, TARGETS }
internal sealed interface CoverageUiState {
    data class Loading(val done: Int = 0, val total: Int = 0) : CoverageUiState
    data class Ready(val snapshot: CoverageSnapshot) : CoverageUiState
    data object Failed : CoverageUiState
}

/** Stateless input; production callbacks only read the existing provider/repository. */
internal data class ProjectSetupData(
    val targets: List<CoverageTarget> = emptyList(),
    val packs: List<IconPack> = emptyList(),
    val catalogReady: Boolean = true,
    val catalogFailed: Boolean = false,
    val coverage: CoverageUiState = CoverageUiState.Loading(),
    val onRescan: () -> Unit = {},
    val onReloadPacks: () -> Unit = {},
    val appIcon: @Composable (String) -> Unit = {},
    val savedMetadata: ProjectAiMetadata? = null,
    val metadataLoading: Boolean = false,
    val metadataFailed: Boolean = false,
    val onSaveSelection: suspend (Long, ProjectSelection) -> ProjectAiMetadata = { _, _ -> error("Preparation persistence unavailable") },
    val launcherInventoryChanged: Boolean = false
)

/** Optional instrumentation sink. Production has no logger; values are fixed codes only. */
internal val LocalProjectInvalidationTrace = staticCompositionLocalOf<((String, String) -> Unit)?> { null }

/** Screen-scoped invalidation only, never a background watcher or icon refresh. */
@Composable
internal fun ObserveCoverageInvalidation(source: String?, observeLauncherChanges: Boolean = false,
    onPackageInvalidate: ((String) -> Unit)? = null, onInvalidate: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val invalidate by rememberUpdatedState(onInvalidate)
    val invalidatePackage by rememberUpdatedState(onPackageInvalidate)
    val trace by rememberUpdatedState(LocalProjectInvalidationTrace.current)
    DisposableEffect(lifecycle, context, source, observeLauncherChanges) {
        trace?.invoke("invalidation.attach", "observer")
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            trace?.invoke("invalidation.resume", "ON_RESUME")
            invalidate()
        } }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val pack = intent?.data?.schemeSpecificPart
                if (pack != null && (observeLauncherChanges || source != null && pack == source)) {
                    val action = when (intent?.action) {
                        Intent.ACTION_PACKAGE_ADDED -> "added"
                        Intent.ACTION_PACKAGE_REMOVED -> "removed"
                        Intent.ACTION_PACKAGE_CHANGED -> "changed"
                        Intent.ACTION_PACKAGE_REPLACED -> "replaced"
                        else -> "other"
                    }
                    trace?.invoke("invalidation.package", "$action:source_match=${pack == source}")
                    invalidatePackage?.invoke(pack) ?: invalidate()
                }
            }
        }
        lifecycle.addObserver(observer)
        if (source != null || observeLauncherChanges) ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            trace?.invoke("invalidation.detach", "observer")
            lifecycle.removeObserver(observer)
            if (source != null || observeLauncherChanges) context.unregisterReceiver(receiver)
        }
    }
}

@Composable
internal fun rememberProjectSetupData(
    viewModel: MainViewModel, mode: ProjectSetupMode, page: ProjectSetupPage,
    profileId: Long?, source: String?
): ProjectSetupData {
    val loadedTargets = viewModel.coverageTargets()
    var metadata by remember(profileId) { mutableStateOf<ProjectAiMetadata?>(null) }
    var metadataLoading by remember(profileId) { mutableStateOf(true) }
    var metadataFailed by remember(profileId) { mutableStateOf(false) }
    LaunchedEffect(profileId, page) {
        if (page == ProjectSetupPage.TARGETS && profileId != null) {
            metadataLoading = true
            metadataFailed = false
            try { metadata = viewModel.projectMetadata(profileId) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { metadataFailed = true }
            finally { metadataLoading = false }
        }
    }
    var rescan by remember { mutableIntStateOf(0) }
    var resumeCheck by remember { mutableIntStateOf(0) }
    ObserveCoverageInvalidation(source, observeLauncherChanges = true) { resumeCheck++ }
    var launcherIdentity by remember(profileId, page, resumeCheck) { mutableStateOf<Map<String, Boolean>?>(null) }
    var launcherLoading by remember(profileId, page, resumeCheck) { mutableStateOf(page == ProjectSetupPage.TARGETS) }
    var launcherFailed by remember(profileId, page, resumeCheck) { mutableStateOf(false) }
    LaunchedEffect(profileId, page, resumeCheck, viewModel.applicationsLoaded) {
        if (page == ProjectSetupPage.TARGETS && viewModel.applicationsLoaded) {
            try { launcherIdentity = viewModel.coverageLauncherIdentity() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { launcherFailed = true }
            finally { launcherLoading = false }
        }
    }
    val targets = launcherIdentity?.let { revalidateLauncherTargets(loadedTargets, it) } ?: loadedTargets
    val launcherChanged = launcherIdentity?.keys?.let { it != loadedTargets.map { target -> target.key }.toSet() } ?: false
    val theme = PackageResourceResolver.displayedNightMode
    var packReload by remember { mutableIntStateOf(0) }
    var reloadedPacks by remember(viewModel.iconPacks) { mutableStateOf<List<IconPack>?>(null) }
    var packFailed by remember { mutableStateOf(false) }
    var packLoading by remember { mutableStateOf(false) }
    LaunchedEffect(packReload) {
        if (packReload > 0) {
            val request = packReload
            packLoading = true
            packFailed = false
            try { reloadedPacks = viewModel.coverageSources() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { packFailed = true; reloadedPacks = null }
            finally { if (request == packReload) packLoading = false }
        }
    }
    // A new profile/component/local snapshot starts with no result; old rows never leak.
    var coverage by remember(profileId, source, targets, rescan, resumeCheck, theme) { mutableStateOf<CoverageUiState>(CoverageUiState.Loading()) }
    LaunchedEffect(profileId, source, targets, rescan, resumeCheck, theme, page, mode, viewModel.activeProfileId,
        viewModel.profileSummaryReady, viewModel.iconPackLoaded, launcherLoading, launcherFailed) {
        if (mode == ProjectSetupMode.EXTEND && page == ProjectSetupPage.TARGETS &&
            source != null && profileId == viewModel.activeProfileId && viewModel.profileSummaryReady && viewModel.iconPackLoaded &&
            !launcherLoading && !launcherFailed) {
            coverage = CoverageUiState.Loading()
            try {
                val reader = viewModel.coverageReader(source, rescan > 0 || viewModel.iconPacks.none { it.packageName == source })
                val snapshot = IconCoverageAnalyzer().analyze(profileId!!, targets, reader) { done, total ->
                    coverage = CoverageUiState.Loading(done, total)
                }
                if (profileId == viewModel.activeProfileId && viewModel.profileSummaryReady) coverage = CoverageUiState.Ready(snapshot)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { coverage = CoverageUiState.Failed }
        }
    }
    return ProjectSetupData(targets, reloadedPacks ?: viewModel.iconPacks,
        viewModel.applicationsLoaded && viewModel.iconPackLoaded && !packLoading && !launcherLoading && !launcherFailed,
        packFailed || launcherFailed || viewModel.startupFailed, coverage,
        onRescan = { rescan++ }, onReloadPacks = { packReload++ },
        appIcon = { key -> ProjectOriginalIcon(viewModel.applicationList.find { it.key == key }?.icon) },
        savedMetadata = metadata, metadataLoading = metadataLoading, metadataFailed = metadataFailed,
        onSaveSelection = { version, selection -> viewModel.saveProjectSelection(checkNotNull(profileId), version, selection).also { metadata = it } },
        launcherInventoryChanged = launcherChanged)
}

@Composable
internal fun ProjectSetup(
    mode: ProjectSetupMode, page: ProjectSetupPage, profileId: Long?, source: String?,
    state: ProjectHomeState, data: ProjectSetupData,
    onSource: (String) -> Unit, onProfile: (Long) -> Unit, onNewProfile: () -> Unit,
    onBack: () -> Unit, onChangeSource: () -> Unit, onEditor: () -> Unit, onStyle: () -> Unit = {}, onTrials: () -> Unit = {},
    guidedStep: Int? = null, onContinue: () -> Unit = onStyle
) {
    if (page == ProjectSetupPage.TARGETS) {
        val ready = state.actionsEnabled && profileId == state.activeProfileId && data.catalogReady
        TargetSelection(mode, profileId ?: state.activeProfileId,
            state.profiles.find { it.id == profileId }?.name.orEmpty(), source,
            data, ready, onBack, onChangeSource, onEditor, onStyle, onTrials, guidedStep, onContinue)
        return
    }
    CenteredFullscreenContent {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag(if (page == ProjectSetupPage.SOURCE) "setup-source" else "setup-profile")) {
            item {
                SetupHeading(stringResource(if (page == ProjectSetupPage.SOURCE) R.string.setupSourceTitle else R.string.setupProfileTitle), onBack)
                if (guidedStep == null) {
                    Text(stringResource(R.string.setupReadOnly))
                    Text(stringResource(R.string.setupPreparationOnly))
                    if (source != null) Text(source)
                } else Text(stringResource(R.string.workflowSourceLocal))
                if (!data.catalogReady || state.busy) WavyLoadingBar(Modifier.fillMaxWidth())
                if (data.catalogFailed || state.operationFailed || state.operationInterrupted) {
                    Text(stringResource(R.string.setupUnavailable))
                }
            }
            if (page == ProjectSetupPage.SOURCE) {
                item {
                    OutlinedButton(onClick = data.onReloadPacks, enabled = !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("setup-reload-packs")) {
                        Text(stringResource(R.string.setupReloadPacks))
                    }
                    if (data.catalogReady && data.packs.isEmpty() && !data.catalogFailed) {
                        Text(stringResource(R.string.setupNoPacks), Modifier.testTag("setup-no-packs"))
                    }
                    TextButton(onClick = onEditor, enabled = state.actionsEnabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("project-extend-editor")) {
                        Text(stringResource(R.string.projectClassicEditor))
                    }
                }
                items(data.packs.distinctBy { it.packageName }, key = { it.packageName }) { pack ->
                    OutlinedButton(onClick = { onSource(pack.packageName) }, enabled = state.actionsEnabled && data.catalogReady,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-source-${pack.packageName}")) {
                        Column {
                            Text(pack.applicationName)
                            if (guidedStep == null) {
                                Text(pack.packageName)
                                Text(stringResource(R.string.setupSourceVersion, pack.versionCode))
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(stringResource(R.string.setupChooseProfile))
                    OutlinedButton(onClick = onNewProfile, enabled = state.actionsEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-extend-create")) {
                        Text(stringResource(R.string.projectExtensionCreate))
                    }
                }
                items(state.profiles, key = { it.id }) { profile ->
                    OutlinedButton(onClick = { onProfile(profile.id) }, enabled = state.actionsEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setup-profile-${profile.id}")) {
                        Column { Text(profile.name); if (guidedStep == null) Text(stringResource(R.string.projectIdentity, profile.id)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SetupHeading(title: String, onBack: () -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp).testTag("setup-back")) {
            Text(stringResource(R.string.setupBack))
        }
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
    }
}
