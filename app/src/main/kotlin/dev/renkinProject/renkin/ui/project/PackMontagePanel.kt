package dev.renkinProject.renkin.ui.project

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.drawAlphaCheckerboard
import kotlinx.coroutines.*

/** The user chooses artwork, never grid dimensions. Only explicit consent can call the provider. */
@Composable
internal fun PackMontagePanel(id: Long, source: String?, stamp: String?, available: Boolean,
    backend: ProjectStyleBackend, onBack: () -> Unit, onTrials: (() -> Unit)?,
    onStyleSaved: ((SavedProjectStyle) -> Unit)?) {
    val local = checkNotNull(backend.montages)
    val analysis = checkNotNull(backend.packAnalysis)
    val scope = rememberCoroutineScope()
    var saved by remember(id) { mutableStateOf<SavedProjectStyle?>(null) }
    var draft by remember(id) { mutableStateOf<MontageDraft?>(null) }
    var busy by remember(id) { mutableStateOf(false) }
    var job by remember(id) { mutableStateOf<Job?>(null) }
    var error by remember(id) { mutableStateOf(false) }
    var record by remember(id) { mutableStateOf<AnalysisRecord?>(null) }
    var refresh by remember(id) { mutableIntStateOf(0) }
    var browse by rememberSaveable(id) { mutableStateOf(false) }
    var advanced by rememberSaveable(id) { mutableStateOf(false) }
    var brief by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var instruction by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var preferences by rememberSaveable(id) { mutableStateOf("") }
    var confirmation by remember(id) { mutableStateOf<StyleAnalysisRequest?>(null) }
    var discard by rememberSaveable(id) { mutableStateOf(false) }
    val hasKey by produceState(false, analysis, refresh) { value = withContext(Dispatchers.IO) { analysis.hasKey() } }
    val dirty = saved != null && (brief != saved?.revision?.specification || instruction != saved?.revision?.userInstruction)
    val enabled = available && !busy && draft != null && saved != null
    suspend fun accept(next: SavedProjectStyle) {
        saved = next
        draft = local.reopen(id)
        brief = next.revision!!.specification; instruction = next.revision!!.userInstruction
        record = analysis.latest(id)
    }
    fun action(block: suspend () -> Unit) {
        if (busy || !available) return
        busy = true; error = false
        job = scope.launch {
            try { block() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { error = true }
            finally { busy = false; job = null; withContext(NonCancellable) { record = analysis.latest(id) } }
        }
    }
    LaunchedEffect(id, source, stamp, available, refresh) {
        if (!available) return@LaunchedEffect
        busy = true; error = false
        try {
            val current = backend.load(id)
            if (current.revision == null) {
                val initial = local.prepare(id, checkNotNull(source), checkNotNull(stamp))
                accept(local.save(id, null, initial))
            } else {
                val previousBrief = brief; val previousInstruction = instruction
                accept(current)
                // Restored unsaved text is a draft, never silently submitted or overwritten.
                if (previousBrief != null) brief = previousBrief
                if (previousInstruction != null) instruction = previousInstruction
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { error = true }
        finally { busy = false }
    }
    val back = { if (!busy) { if (dirty) discard = true else onBack() } }
    BackHandler(onBack = back)
    fun change(hashes: List<String>) = action {
        val next = local.recompose(id, checkNotNull(draft), hashes)
        val previous = checkNotNull(saved)
        val unchanged = next.montage.fingerprint == draft!!.montage.fingerprint
        val nextSaved = local.save(id, previous.revision!!.revision, next,
            if (unchanged) previous.revision.specification else MONTAGE_STARTING_BRIEF,
            previous.revision.userInstruction, previous.metadata!!.version)
        val candidates = next.candidates
        accept(nextSaved); draft = draft!!.copy(candidates = candidates)
    }
    CenteredFullscreenContent {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("pack-montage")) {
            item {
                SetupHeading(stringResource(R.string.packStyleTitle), back)
                Text(source?.let(backend::sourceLabel) ?: stringResource(R.string.workflowSelectedSource), style = MaterialTheme.typography.titleMedium)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error) {
                    Text(stringResource(R.string.packStyleError), Modifier.testTag("pack-error"), color = MaterialTheme.colorScheme.error)
                    if (draft == null) TextButton(onClick = { refresh++ }, enabled = !busy) { Text(stringResource(R.string.setupRescan)) }
                }
            }
            draft?.let { d ->
                itemsIndexed(d.images) { index, png ->
                    MontagePng(png, "pack-sheet-$index", Modifier.fillMaxWidth().heightIn(max = 350.dp))
                }
                item {
                    Text(stringResource(R.string.packStyleCount, d.montage.members.size, d.images.size))
                    TextButton(onClick = {
                        if (browse) browse = false else action { draft = local.browse(id, d); browse = true }
                    }, enabled = enabled && !dirty, modifier = Modifier.testTag("pack-browse")) {
                        Text(stringResource(R.string.packStyleBrowse))
                    }
                    val result = d.montage.result
                    if (result != null) Text(result.summary, Modifier.testTag("pack-summary"))
                    if (record?.status in setOf(AnalysisStatus.UNCERTAIN, AnalysisStatus.FAILED))
                        Text(stringResource(R.string.packStyleFailedAnalysis), Modifier.testTag("pack-analysis-status"))
                    if (record?.status == AnalysisStatus.RESULT_SAVED) TextButton(onClick = {
                        action { accept(checkNotNull(analysis.recover(id, d))) }
                    }, enabled = enabled && !dirty, modifier = Modifier.testTag("pack-recover")) { Text(stringResource(R.string.packStyleRecover)) }
                    Button(onClick = { action { confirmation = analysis.prepare(id, d, preferences) } },
                        enabled = enabled && !dirty && hasKey, modifier = Modifier.fillMaxWidth().testTag("pack-analyze")) {
                        Text(stringResource(R.string.packStyleAnalyze))
                    }
                    if (!hasKey) Text(stringResource(R.string.packStyleKey))
                    Button(onClick = { action {
                        val previous = checkNotNull(saved)
                        // Fresh validation, also clears an imported evidence revalidation flag.
                        val checked = local.save(id, previous.revision!!.revision, d, checkNotNull(brief),
                            checkNotNull(instruction), previous.metadata!!.version)
                        accept(checked)
                        if (onStyleSaved != null) onStyleSaved(checked) else onTrials?.invoke()
                    } }, enabled = enabled && !dirty && result != null,
                        modifier = Modifier.fillMaxWidth().testTag("pack-continue")) { Text(stringResource(R.string.packStyleContinue)) }
                    TextButton(onClick = { advanced = !advanced }, modifier = Modifier.testTag("pack-advanced")) {
                        Text(stringResource(R.string.packStyleEdit))
                    }
                    if (advanced) {
                        source?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { action {
                            val current = checkNotNull(saved)
                            val fresh = local.prepare(id, checkNotNull(source), checkNotNull(stamp))
                            accept(local.save(id, current.revision!!.revision, fresh, MONTAGE_STARTING_BRIEF,
                                current.revision.userInstruction, current.metadata!!.version))
                            browse = false
                        } }, enabled = enabled && !dirty, modifier = Modifier.testTag("pack-refresh")) {
                            Text(stringResource(R.string.packStyleRefresh))
                        }
                        OutlinedTextField(brief.orEmpty(), { if (it.length <= 6000) brief = it },
                            label = { Text(stringResource(R.string.packStyleBrief)) }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("pack-brief"))
                        OutlinedTextField(instruction.orEmpty(), { if (it.length <= 2000) instruction = it },
                            label = { Text(stringResource(R.string.packStyleInstruction)) }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("pack-instruction"))
                        OutlinedTextField(preferences, { if (it.length <= 2000) preferences = it },
                            label = { Text(stringResource(R.string.packStylePreferences)) }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("pack-preferences"))
                        TextButton(onClick = { action { accept(local.save(id, saved!!.revision!!.revision,
                            d, checkNotNull(brief), checkNotNull(instruction), saved!!.metadata!!.version)) } },
                            enabled = enabled && !brief.isNullOrBlank(), modifier = Modifier.testTag("pack-save")) {
                            Text(stringResource(R.string.packStyleSave))
                        }
                    }
                    if (busy) TextButton(onClick = { job?.cancel() }, enabled = job != null,
                        modifier = Modifier.testTag("pack-stop")) { Text(stringResource(R.string.colorizeCancel)) }
                }
                if (browse) {
                    val hashes = d.montage.members.map { it.hash }
                    itemsIndexed(d.candidates.sortedBy { candidate -> hashes.indexOf(candidate.hash).let { if (it < 0) Int.MAX_VALUE else it } },
                        key = { _, candidate -> candidate.hash }) { _, candidate ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MontagePng(candidate.png, "pack-candidate-${candidate.hash}", Modifier.size(64.dp))
                            Column(Modifier.weight(1f)) {
                                Text(candidate.drawable, maxLines = 1)
                                val at = hashes.indexOf(candidate.hash)
                                Row {
                                    TextButton(onClick = { change(if (at < 0) hashes + candidate.hash else hashes - candidate.hash) },
                                        enabled = enabled && !dirty && if (at < 0) hashes.size < MONTAGE_MAX_MEMBERS else hashes.size > 1,
                                        modifier = Modifier.testTag("pack-toggle-${candidate.hash}")) {
                                        Text(stringResource(if (at < 0) R.string.packStyleAdd else R.string.packStyleRemove))
                                    }
                                    if (at > 0) TextButton(onClick = { change(hashes.toMutableList().apply {
                                        val value = removeAt(at); add(at - 1, value)
                                    }) }, enabled = enabled && !dirty, modifier = Modifier.testTag("pack-up-${candidate.hash}")) {
                                        Text(stringResource(R.string.packStyleUp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmation?.let { request -> RenkinAlertDialog(onDismissRequest = { confirmation = null },
        title = { Text(stringResource(R.string.packStyleAnalyze)) },
        text = { Text(stringResource(R.string.packStyleConsent, request.images.size)) },
        confirmButton = { TextButton(onClick = {
            confirmation = null
            action {
                val result = analysis.submit(request, checkNotNull(draft), true)
                if (result != null) accept(result) else error = true
            }
        }, enabled = enabled, modifier = Modifier.testTag("pack-analysis-confirm")) { Text(stringResource(R.string.packStyleConfirm)) } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text(stringResource(R.string.colorizeCancel)) } }) }
    if (discard) RenkinAlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.projectStyleDiscardTitle)) },
        text = { Text(stringResource(R.string.projectStyleDiscardText)) },
        confirmButton = { TextButton(onClick = {
            brief = saved?.revision?.specification; instruction = saved?.revision?.userInstruction
            discard = false; onBack()
        }) { Text(stringResource(R.string.projectStyleDiscard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.colorizeCancel)) } })
}

@Composable
private fun MontagePng(bytes: ByteArray, tag: String, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val image by produceState<ImageBitmap?>(null, bytes) {
        value = null
        val bitmap = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        value = bitmap?.asImageBitmap()
        // Published images may still be referenced by Compose's retained draw nodes.
        // Let Bitmap/Compose release them after those references disappear; recycling
        // on a byte-array key change can invalidate a frame during a saved-style reload.
    }
    val checker = Modifier.drawBehind { drawAlphaCheckerboard(Color.White, Color(0xffdddddd)) }
    image?.let { bitmap ->
        Image(bitmap, contentDescription = stringResource(R.string.packStylePreview),
            modifier.then(checker).clickable { expanded = true }.testTag(tag))
        if (expanded) RenkinAlertDialog(onDismissRequest = { expanded = false },
            title = { Text(stringResource(R.string.packStylePreview)) },
            text = { Box(Modifier.heightIn(max = 450.dp).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
                Image(bitmap, contentDescription = stringResource(R.string.packStylePreview),
                    Modifier.requiredSize(bitmap.width.dp, bitmap.height.dp).then(checker))
            } }, confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.setupBack)) } })
    }
}
