package dev.renkinProject.renkin.ui.project

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.data.project.ReferenceAsset
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class ProjectStyleDraft(val specification: String, val instruction: String, val hashes: List<String>,
    val analysisRequestId: String? = null, val analysisReviewedAt: Long? = null, val allowStaleSource: Boolean = false,
    val analysisReviewedSourceStamp: String? = null)
internal interface ProjectStyleBackend {
    val analysis: ProjectAnalysisBackend? get() = null
    suspend fun load(id: Long): SavedProjectStyle
    suspend fun importReference(id: Long, uri: Uri): ReferenceAsset
    suspend fun referencePreview(id: Long, hash: String): ByteArray? = null
    suspend fun copyGlobal(id: Long): ProjectStyleDraft
    suspend fun discard(id: Long, hashes: List<String>) {}
    suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft,
        mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle
}

/** Primitives only in saved state; bytes and file ownership stay in the existing VM/repository.
 * Save/import start exclusively in callbacks. A restored form never resubmits a write. */
@Composable
internal fun ProjectStylePanel(id: Long, mode: ProjectSetupMode, source: String?, stamp: String?,
    available: Boolean, backend: ProjectStyleBackend, onBack: () -> Unit, onTrials: (() -> Unit)? = null) {
    var initialized by rememberSaveable(id) { mutableStateOf(false) }
    var specification by rememberSaveable(id) { mutableStateOf("") }
    var instruction by rememberSaveable(id) { mutableStateOf("") }
    var hashes by rememberSaveable(id) { mutableStateOf(arrayListOf<String>()) }
    var expectedRevision by rememberSaveable(id) { mutableStateOf<Long?>(null) }
    var savedSpecification by rememberSaveable(id) { mutableStateOf("") }
    var savedInstruction by rememberSaveable(id) { mutableStateOf("") }
    var savedHashes by rememberSaveable(id) { mutableStateOf(arrayListOf<String>()) }
    // Keep removed/replaced draft imports too, so explicit discard can release them.
    var importedHashes by rememberSaveable(id) { mutableStateOf(arrayListOf<String>()) }
    var analysisRequestId by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var analysisReviewedAt by rememberSaveable(id) { mutableStateOf<Long?>(null) }
    var analysisStaleAccepted by rememberSaveable(id) { mutableStateOf(false) }
    var analysisReviewedSourceStamp by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var loaded by remember(id) { mutableStateOf(false) }
    var busy by remember(id) { mutableStateOf(false) }
    var advanced by rememberSaveable(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var reload by remember(id) { mutableIntStateOf(0) }
    var discardOpen by rememberSaveable(id) { mutableStateOf(false) }
    var reloadAfterDiscard by rememberSaveable(id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val analysis = backend.analysis
    val analysisState = if (analysis != null) analysis.state(id).collectAsState().value else ProjectAnalysisState()
    val dirty = specification != savedSpecification || instruction != savedInstruction || hashes != savedHashes
    val enabled = available && loaded && !busy && !analysisState.busy
    fun applySaved(saved: SavedProjectStyle) {
        specification = saved.revision?.specification.orEmpty()
        instruction = saved.revision?.userInstruction.orEmpty()
        hashes = ArrayList(saved.references.map { it.hash })
        savedSpecification = specification
        savedInstruction = instruction
        savedHashes = ArrayList(hashes)
        expectedRevision = saved.revision?.revision
        analysisRequestId = null; analysisReviewedAt = null; analysisStaleAccepted = false; analysisReviewedSourceStamp = null
        initialized = true
    }
    LaunchedEffect(id, reload) {
        loaded = false
        error = null
        try {
            val saved = backend.load(id)
            if (!initialized) applySaved(saved)
            loaded = true
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { error = "load" }
    }
    val goBack = { if (!busy) { reloadAfterDiscard = false; if (dirty) discardOpen = true else onBack() } }
    BackHandler(onBack = goBack)
    val import = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && enabled && hashes.size < 5) {
            busy = true
            scope.launch {
                try {
                    val asset = backend.importReference(id, uri)
                    importedHashes = ArrayList((importedHashes + asset.hash).distinct())
                    if (asset.hash !in hashes) hashes = ArrayList(hashes + asset.hash)
                    error = null
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { error = "import" }
                finally { busy = false }
            }
        }
    }
    CenteredFullscreenContent {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("project-style-$id")) {
            item {
                SetupHeading(stringResource(R.string.projectStyleTitle), goBack)
                Text(stringResource(R.string.projectIdentity, id))
                Text(stringResource(R.string.projectStyleLocalOnly))
                if (mode == ProjectSetupMode.EXTEND) Text(stringResource(R.string.projectStyleSource, source.orEmpty()))
                Text(stringResource(when {
                    analysisState.network -> R.string.styleAnalysisRunning
                    analysisState.busy -> R.string.styleAnalysisPreparing
                    busy -> R.string.projectStyleSaving
                    !loaded -> R.string.projectInitializing
                    dirty -> R.string.projectStyleDirty
                    expectedRevision == null -> R.string.projectStyleUnset
                    else -> R.string.projectStyleSaved
                }), Modifier.testTag("project-style-status"))
                if (onTrials != null) {
                    OutlinedButton(onClick = onTrials, enabled = enabled && !dirty && expectedRevision != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-trials")) {
                        Text(stringResource(R.string.trialTitle))
                    }
                    Text(stringResource(when {
                        !loaded -> R.string.projectInitializing
                        dirty -> R.string.trialSaveStyleChanges
                        expectedRevision == null -> R.string.trialSaveStyle
                        else -> R.string.trialStyleNext
                    }), Modifier.testTag("style-trials-reason"))
                }
                if (error != null) {
                    Text(stringResource(if (error == "load") R.string.projectStyleLoadError else R.string.projectStyleWriteError),
                        Modifier.testTag("project-style-error"))
                    OutlinedButton(onClick = {
                        if (dirty) { reloadAfterDiscard = true; discardOpen = true }
                        else { initialized = false; reload++ }
                    }, enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-reload")) {
                        Text(stringResource(R.string.projectReload))
                    }
                }
            }
            if (analysis != null) item {
                ProjectStyleAnalysisPanel(id, mode, source, stamp, enabled, analysis,
                    expectedRevision, ProjectStyleDraft(specification, instruction, hashes.toList()), dirty,
                    advanced, { advanced = it }, { busy = it },
                    onUse = { draft, stale ->
                        specification = draft.specification
                        hashes = ArrayList(draft.hashes)
                        importedHashes = ArrayList((importedHashes + draft.hashes).distinct())
                        analysisRequestId = draft.requestId; analysisReviewedAt = draft.reviewedAt
                        analysisStaleAccepted = stale; analysisReviewedSourceStamp = draft.reviewedSourceStamp
                        // Separate userInstruction is intentionally never overwritten by analysis.
                    })
            }
            if (analysis != null && analysisState.record?.status != dev.renkinProject.renkin.ai.style.AnalysisStatus.RESULT_SAVED) item {
                OutlinedButton(onClick = { advanced = !advanced }, enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-advanced")) {
                    Text(stringResource(R.string.styleAnalysisAdvanced))
                }
            }
            if (analysis == null || advanced) {
            item {
                OutlinedTextField(specification, { specification = it }, enabled = enabled,
                    label = { Text(stringResource(R.string.projectStyleDescription)) },
                    modifier = Modifier.fillMaxWidth().testTag("project-style-description"), minLines = 3)
                OutlinedTextField(instruction, { instruction = it }, enabled = enabled,
                    label = { Text(stringResource(R.string.projectStyleInstruction)) },
                    modifier = Modifier.fillMaxWidth().testTag("project-style-instruction"))
                TextButton(onClick = {
                    if (!enabled) return@TextButton
                    busy = true
                    scope.launch {
                        try {
                            val draft = backend.copyGlobal(id)
                            importedHashes = ArrayList((importedHashes + draft.hashes).distinct())
                            specification = draft.specification
                            instruction = draft.instruction
                            hashes = ArrayList(draft.hashes)
                            analysisRequestId = null; analysisReviewedAt = null; analysisStaleAccepted = false
                            error = null
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { error = "import" }
                        finally { busy = false }
                    }
                }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-copy-global")) {
                    Text(stringResource(R.string.projectStyleCopyGlobal))
                }
                Text(stringResource(R.string.projectStyleReferences, hashes.size))
                OutlinedButton(onClick = { import.launch("image/png") }, enabled = enabled && hashes.size < 5,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-add-reference")) {
                    Text(stringResource(R.string.projectStyleAddReference))
                }
            }
            items(hashes.size, key = { hashes[it] }) { position ->
                val hash = hashes[position]
                Column(Modifier.fillMaxWidth().testTag("project-style-reference-$position")) {
                    ProjectReferencePreview(id, hash, backend)
                    Text(stringResource(R.string.projectStyleReferenceNumber, position + 1))
                    Text(hash.take(12))
                    TextButton(onClick = {
                        if (enabled) {
                            busy = true
                            scope.launch {
                                try {
                                    // An explicit removal can release a pending row; saved history
                                    // and variants remain pinned by the repository's SQL predicate.
                                    backend.discard(id, listOf(hash))
                                    hashes = ArrayList(hashes.filter { it != hash })
                                    importedHashes = ArrayList(importedHashes.filter { it != hash })
                                    error = null
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { error = "discard" }
                                finally { busy = false }
                            }
                        }
                    }, enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-remove-$position")) {
                        Text(stringResource(R.string.projectStyleRemove))
                    }
                    TextButton(onClick = {
                        hashes = ArrayList(hashes).apply { removeAt(position); add(position - 1, hash) }
                    }, enabled = enabled && position > 0,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-up-$position")) {
                        Text(stringResource(R.string.projectStyleMoveEarlier))
                    }
                }
            }
            }
            item {
                OutlinedButton(onClick = {
                    if (!enabled) return@OutlinedButton
                    busy = true
                    val draft = ProjectStyleDraft(specification, instruction, hashes.toList(), analysisRequestId,
                        analysisReviewedAt, analysisStaleAccepted, analysisReviewedSourceStamp)
                    val revision = expectedRevision
                    scope.launch {
                        try {
                            applySaved(backend.save(id, revision, draft, mode, source, stamp))
                            error = null
                        }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { error = "save" }
                        finally { busy = false }
                    }
                }, enabled = enabled && specification.isNotBlank() && specification.length <= 6000 && instruction.length <= 2000,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-style-save")) {
                    Text(stringResource(R.string.saveAction))
                }
                Text(stringResource(R.string.projectStyleLater))
            }
        }
    }
    if (discardOpen) RenkinAlertDialog(onDismissRequest = { discardOpen = false },
        title = { Text(stringResource(R.string.projectStyleDiscardTitle)) },
        text = { Text(stringResource(R.string.projectStyleDiscardText)) },
        confirmButton = { TextButton(onClick = {
            if (!busy) {
                busy = true
                scope.launch {
                    try {
                        backend.discard(id, (hashes + importedHashes).distinct())
                        importedHashes = arrayListOf(); initialized = false; discardOpen = false
                        if (reloadAfterDiscard) reload++ else onBack()
                    }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { error = "discard"; discardOpen = false }
                    finally { busy = false }
                }
            }
        }, enabled = !busy,
            modifier = Modifier.testTag("project-style-discard")) { Text(stringResource(R.string.projectStyleDiscard)) } },
        dismissButton = { TextButton(onClick = { discardOpen = false }, modifier = Modifier.testTag("project-style-keep")) {
            Text(stringResource(R.string.colorizeCancel))
        } })
}

@Composable
private fun ProjectReferencePreview(id: Long, hash: String, backend: ProjectStyleBackend) {
    val image by produceState<ImageBitmap?>(null, id, hash, backend) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            val bytes = backend.referencePreview(id, hash)
            if (bytes != null) {
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }
                bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                value = bitmap?.asImageBitmap()
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null }
        awaitDispose { bitmap?.recycle() }
    }
    image?.let { Image(it, contentDescription = stringResource(R.string.projectStyleReferencePreview), Modifier.size(72.dp)) }
}
