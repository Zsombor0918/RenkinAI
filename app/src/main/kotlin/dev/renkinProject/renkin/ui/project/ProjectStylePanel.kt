package dev.renkinProject.renkin.ui.project

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
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
    val localReferenceOrigin: LocalReferenceOrigin? = null)
internal interface ProjectStyleBackend {
    val montages: LocalPackMontages? get() = null
    val packAnalysis: PackStyleAnalysis? get() = null
    fun sourceLabel(source: String): String? = null
    val localReferences: LocalStyleReferences? get() = null
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
    available: Boolean, backend: ProjectStyleBackend, onBack: () -> Unit, onTrials: (() -> Unit)? = null,
    onStyleSaved: ((SavedProjectStyle) -> Unit)? = null, guided: Boolean = false) {
    if (mode == ProjectSetupMode.EXTEND && backend.montages != null) {
        var montagePath by remember(id) { mutableStateOf<Boolean?>(null) }
        var failed by remember(id) { mutableStateOf(false) }
        var reload by remember(id) { mutableIntStateOf(0) }
        LaunchedEffect(id, reload) {
            failed = false
            try { montagePath = backend.load(id).revision.let { it == null ||
                dev.renkinProject.renkin.ai.style.PackMontage.optional(it.analysisProfileJson) != null } }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { failed = true }
        }
        if (montagePath == true) {
            PackMontagePanel(id, source, stamp, available, backend, onBack, onTrials, onStyleSaved)
            return
        }
        if (montagePath == null) {
            Column {
                Text(stringResource(if (failed) R.string.setupUnavailable else R.string.projectInitializing))
                if (failed) TextButton(onClick = { reload++ }) { Text(stringResource(R.string.setupRescan)) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.setupBack)) }
            }
            return
        }
    }
    ManualProjectStylePanel(id, mode, source, stamp, available, backend, onBack, onTrials, onStyleSaved, guided)
}

@Composable
private fun ManualProjectStylePanel(id: Long, mode: ProjectSetupMode, source: String?, stamp: String?,
    available: Boolean, backend: ProjectStyleBackend, onBack: () -> Unit, onTrials: (() -> Unit)?,
    onStyleSaved: ((SavedProjectStyle) -> Unit)?, guided: Boolean) {
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
    var loaded by remember(id) { mutableStateOf(false) }
    var busy by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var reload by remember(id) { mutableIntStateOf(0) }
    var discardOpen by rememberSaveable(id) { mutableStateOf(false) }
    var reloadAfterDiscard by rememberSaveable(id) { mutableStateOf(false) }
    var localOrigin by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var candidates by remember(id, source, stamp) { mutableStateOf<LocalReferenceCandidates?>(null) }
    val startingBrief = stringResource(R.string.referenceStyleBrief)
    val scope = rememberCoroutineScope()
    val dirty = specification != savedSpecification || instruction != savedInstruction || hashes != savedHashes
    val enabled = available && loaded && !busy
    fun applySaved(saved: SavedProjectStyle) {
        specification = saved.revision?.specification ?: if (guided) startingBrief else ""
        instruction = saved.revision?.userInstruction.orEmpty()
        hashes = ArrayList(saved.references.map { it.hash })
        savedSpecification = specification
        savedInstruction = instruction
        savedHashes = ArrayList(hashes)
        expectedRevision = saved.revision?.revision
        localOrigin = null
        initialized = true
    }
    fun finishSave(saved: SavedProjectStyle) {
        applySaved(saved)
        error = null
        if (guided) onStyleSaved?.invoke(saved)
    }
    LaunchedEffect(id, reload) {
        loaded = false
        error = null
        try {
            val saved = backend.load(id)
            if (!initialized || (!dirty && saved.revision?.revision != expectedRevision)) applySaved(saved)
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
                if (!guided) Text(stringResource(R.string.projectIdentity, id))
                Text(stringResource(if (guided) R.string.uxStyleLocalSave else R.string.projectStyleLocalOnly))
                if (mode == ProjectSetupMode.EXTEND)
                    Text(stringResource(R.string.projectStyleSource, source.orEmpty()))
                Text(stringResource(when {
                    busy -> R.string.projectStyleSaving
                    !loaded -> R.string.projectInitializing
                    dirty -> R.string.projectStyleDirty
                    expectedRevision == null -> R.string.projectStyleUnset
                    else -> R.string.projectStyleSaved
                }), Modifier.testTag("project-style-status"))
                if (guided && expectedRevision != null) Text(stringResource(R.string.uxStyleRevision, expectedRevision!!),
                    Modifier.testTag("project-style-revision"))
                if (onTrials != null) {
                    val readyForTargets = enabled && !dirty && expectedRevision != null
                    if (guided && readyForTargets) Button(onClick = onTrials,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-trials")) {
                        Text(stringResource(R.string.workflowContinueTargets))
                    }
                    else OutlinedButton(onClick = onTrials, enabled = readyForTargets,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-trials")) {
                        Text(stringResource(if (guided) R.string.workflowContinueTargets else R.string.trialTitle))
                    }
                    Text(stringResource(when {
                        !loaded -> R.string.projectInitializing
                        busy -> R.string.projectStyleSaving
                        dirty -> R.string.trialSaveStyleChanges
                        expectedRevision == null -> R.string.trialSaveStyle
                        else -> R.string.trialStyleNext
                    }), Modifier.testTag("style-trials-reason"))
                }
                if (error != null) {
                    Text(stringResource(when (error) {
                        "load" -> R.string.projectStyleLoadError
                        "source" -> R.string.referenceStyleSourceError
                        else -> R.string.projectStyleWriteError
                    }),
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
            item {
                Text(stringResource(R.string.referenceStyleTitle))
                Text(stringResource(R.string.referenceStyleHelp))
                if (hashes.isEmpty()) Text(stringResource(R.string.referenceStyleNoImages))
                if (mode == ProjectSetupMode.EXTEND && backend.localReferences != null) {
                    OutlinedButton(onClick = {
                        if (enabled && source != null && stamp != null) {
                            busy = true
                            scope.launch {
                                try { candidates = backend.localReferences!!.sample(id, source, stamp); error = null }
                                catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { error = "source" }
                                finally { busy = false }
                            }
                        }
                    }, enabled = enabled && source != null && stamp != null,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-sample-pack")) {
                        Text(stringResource(R.string.referenceStyleSample))
                    }
                }
            }
            candidates?.let { batch ->
                items(batch.samples.samples.size) { position ->
                    val sample = batch.samples.samples[position]
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnalysisThumbnail(sample.hash, { sample.png }, position)
                        Column {
                            Text(stringResource(R.string.referenceStyleCandidate, position + 1))
                            TextButton(onClick = {
                                if (enabled && hashes.size < 5 && sample.hash !in hashes) {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val origin = localOrigin?.let(LocalReferenceOrigin::parse)?.merge(batch.origin) ?: batch.origin
                                            val asset = backend.localReferences!!.select(id, batch, sample.hash)
                                            importedHashes = ArrayList((importedHashes + asset.hash).distinct())
                                            hashes = ArrayList(hashes + asset.hash)
                                            localOrigin = origin.encode(); error = null
                                        } catch (cancel: CancellationException) { throw cancel }
                                        catch (_: Exception) { error = "source" }
                                        finally { busy = false }
                                    }
                                }
                            }, enabled = enabled && hashes.size < 5 && sample.hash !in hashes,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("project-style-select-candidate-$position")) {
                                Text(stringResource(if (sample.hash in hashes) R.string.referenceStyleSelected else R.string.referenceStyleSelect))
                            }
                        }
                    }
                }
            }
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
                            localOrigin = null
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
                    if (!guided) Text(hash.take(12))
                    TextButton(onClick = {
                        if (enabled) {
                            busy = true
                            scope.launch {
                                try {
                                    // An explicit removal can release a pending row; saved history
                                    // and variants remain pinned by the repository's SQL predicate.
                                    backend.discard(id, listOf(hash))
                                    hashes = ArrayList(hashes.filter { it != hash })
                                    if (localOrigin?.let(LocalReferenceOrigin::parse)?.drawables?.keys?.none { it in hashes } == true)
                                        localOrigin = null
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
            if (!guided || dirty || expectedRevision == null) item {
                Button(onClick = {
                    if (!enabled) return@Button
                    busy = true
                    val draft = ProjectStyleDraft(specification, instruction, hashes.toList(),
                        localReferenceOrigin = localOrigin?.let(LocalReferenceOrigin::parse))
                    val revision = expectedRevision
                    scope.launch {
                        try {
                            finishSave(backend.save(id, revision, draft, mode, source, stamp))
                        }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { error = "save" }
                        finally { busy = false }
                    }
                }, enabled = enabled && specification.isNotBlank() && specification.length <= 6000 && instruction.length <= 2000,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-style-save")) {
                    Text(stringResource(if (guided) R.string.uxStyleSaveContinue else R.string.saveAction))
                }
                if (!guided) Text(stringResource(R.string.projectStyleLater))
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
                        localOrigin = null; candidates = null
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
