package dev.renkinProject.renkin.ui.project

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
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
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.packages.PackageResourceResolver
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.WavyLoadingBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Suggestions are reviewable, never active on arrival. Use changes only the protected draft;
 * the existing explicit style Save performs durable acceptance. Small previews are disposed. */
@Composable
internal fun ProjectStyleAnalysisPanel(id: Long, mode: ProjectSetupMode, source: String?, stamp: String?,
    enabled: Boolean, backend: ProjectAnalysisBackend, expected: Long?, draft: ProjectStyleDraft,
    dirty: Boolean, advanced: Boolean, onAdvanced: (Boolean) -> Unit,
    onBusy: (Boolean) -> Unit, onUse: (AnalyzedDraft, Boolean) -> Unit) {
    val state by backend.state(id).collectAsState()
    var invalidation by remember { mutableIntStateOf(0) }
    val theme = PackageResourceResolver.displayedNightMode
    var readError by remember(id) { mutableStateOf(false) }
    ObserveCoverageInvalidation(source) { invalidation++ }
    LaunchedEffect(id, mode, source, invalidation, theme) {
        backend.bindContext(id, mode, source)
        try { backend.refresh(id); readError = false } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { readError = true }
    }
    DisposableEffect(id, backend) { onDispose { backend.releasePreview(id) } }
    var confirm by rememberSaveable(id) { mutableStateOf(false) }
    var confirmedId by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var useConfirm by rememberSaveable(id) { mutableStateOf(false) }
    var using by remember { mutableStateOf(false) }
    var useError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().testTag("style-analysis-$id"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.styleAnalysisTitle))
        Text(stringResource(if (mode == ProjectSetupMode.CREATE) R.string.styleAnalysisCreate else R.string.styleAnalysisExtend))
        Text(stringResource(R.string.styleAnalysisManualFallback))
        OutlinedButton(onClick = { backend.prepare(id, mode, source, stamp, expected, draft) },
            enabled = enabled && !state.busy && !using,
            modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-prepare")) {
            Text(stringResource(if (mode == ProjectSetupMode.EXTEND) R.string.styleAnalysisSamplePack else R.string.styleAnalysisPrepareOwn))
        }
        if (state.busy) {
            WavyLoadingBar()
            Text(stringResource(if (state.network) R.string.styleAnalysisRunning else R.string.styleAnalysisPreparing), Modifier.testTag("style-analysis-progress"))
            TextButton(onClick = { backend.cancel(id) }, modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-cancel")) {
                Text(stringResource(R.string.styleAnalysisCancel))
            }
        }
        if (!state.hasKey) Text(stringResource(R.string.styleAnalysisNoKey), Modifier.testTag("style-analysis-no-key"))
        if (state.error != null || useError || readError) Text(stringResource(
            if (state.error == "empty") R.string.styleAnalysisEmpty else R.string.styleAnalysisLocalError), Modifier.testTag("style-analysis-error"))
        state.samples?.let { samples ->
            Text(stringResource(R.string.styleAnalysisSamples, samples.samples.size, samples.report.examined, samples.report.duplicates,
                samples.report.unsupported, samples.report.broken), Modifier.testTag("style-analysis-sample-count"))
            if (samples.sourceIdentity != null) Text(stringResource(R.string.styleAnalysisSourceIdentity,
                samples.sourceIdentity.packageName, samples.sourceIdentity.versionCode.toString(), samples.sourceIdentity.theme.toString()))
            if (samples.report.lowSample) Text(stringResource(R.string.styleAnalysisLowSamples))
            if (samples.report.mixedStyleSuspected) Text(stringResource(R.string.styleAnalysisMixed), Modifier.testTag("style-analysis-mixed"))
            samples.samples.chunked(4).forEachIndexed { row, images ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    images.forEachIndexed { column, sample -> AnalysisThumbnail(sample.hash, { sample.png }, row * 4 + column) }
                }
            }
        }
        val prepared = state.preparation
        val matchesDraft = prepared != null && prepared.context.matchesRoute(mode, source) &&
            state.preparedDraftHash == analysisDraftHash(draft)
        if (prepared != null && !matchesDraft) Text(stringResource(R.string.styleAnalysisDraftChanged))
        OutlinedButton(onClick = { if (prepared != null) { confirmedId = prepared.requestId; confirm = true } },
            enabled = enabled && !state.busy && matchesDraft && state.hasKey,
            modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-start")) { Text(stringResource(R.string.styleAnalysisStart)) }
        val record = state.record
        if (record != null) {
            val validity = if (record.context?.matchesRoute(mode, source) == true) state.validity else AnalysisValidity.PREPARATION_CHANGED
            Text(stringResource(R.string.styleAnalysisRequestStatus, record.status.name), Modifier.testTag("style-analysis-request-status"))
            if (record.status == AnalysisStatus.UNCERTAIN || record.status == AnalysisStatus.PREPARED || record.status == AnalysisStatus.REQUESTING) {
                Text(stringResource(R.string.styleAnalysisUncertain), Modifier.testTag("style-analysis-uncertain"))
            }
            if (record.status == AnalysisStatus.FAILED) Text(stringResource(when (record.reason) {
                "Unauthorized" -> R.string.styleAnalysisUnauthorized
                "RateLimited" -> R.string.styleAnalysisRateLimited
                "Refused" -> R.string.styleAnalysisRefused
                else -> R.string.styleAnalysisFailed
            }), Modifier.testTag("style-analysis-failed"))
            if (validity != AnalysisValidity.CURRENT) Text(stringResource(R.string.styleAnalysisStale), Modifier.testTag("style-analysis-stale"))
            record.style?.let { style ->
                Text(style.shortStyleDescription, Modifier.testTag("style-analysis-suggestion"))
                Text(stringResource(R.string.styleAnalysisSummary, style.archetype.name, style.backgroundMode.name,
                    style.stroke.name, style.cornerTreatment.name, style.opticalPadding.name, style.detailLevel.name))
                Text(stringResource(R.string.styleAnalysisPalette, style.paletteArgb.joinToString(", ").ifEmpty { "unknown" }))
                Text(stringResource(R.string.styleAnalysisAssessment, style.selfAssessment.name))
                if (style.uncertaintyNotes.isNotEmpty()) Text(style.uncertaintyNotes.joinToString("\n"))
                if (style.mixedStyleWarning || record.localReport?.mixedStyleSuspected == true) Text(stringResource(R.string.styleAnalysisMixed), Modifier.testTag("style-analysis-mixed-result"))
                if (record.hashes.size < 8) Text(stringResource(R.string.styleAnalysisLowSamples))
                record.usage?.let { Text(stringResource(R.string.styleAnalysisUsage, it.inputTokens, it.outputTokens)) }
            }
            if (record.status == AnalysisStatus.RESULT_SAVED) {
                var selected by rememberSaveable(id, record.requestId) { mutableStateOf(ArrayList(record.hashes.take(5))) }
                TextButton(onClick = { onAdvanced(!advanced) }, modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-advanced")) {
                    Text(stringResource(R.string.styleAnalysisAdvanced))
                }
                if (advanced) {
                    Text(stringResource(R.string.styleAnalysisAdvancedHelp))
                    Text(stringResource(R.string.styleAnalysisSelectedRefs, selected.size))
                    record.hashes.forEachIndexed { index, hash ->
                        Row {
                            Checkbox(hash in selected, { checked ->
                                selected = ArrayList(if (checked) {
                                    if (hash in selected || selected.size >= 5) selected else selected + hash
                                } else selected.filter { it != hash })
                            }, enabled = !using, modifier = Modifier.size(48.dp).testTag("style-analysis-reference-$index"))
                            AnalysisThumbnail(hash, { backend.preview(id, record.requestId, hash) }, index)
                        }
                        TextButton(onClick = { selected = ArrayList(selected).apply { remove(hash); add(0, hash) } },
                            enabled = hash in selected && selected.firstOrNull() != hash && !using,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-first-$index")) {
                            Text(stringResource(R.string.styleAnalysisMakeFirst))
                        }
                    }
                }
                OutlinedButton(onClick = { useConfirm = true }, enabled = enabled && !using &&
                    validity in setOf(AnalysisValidity.CURRENT, AnalysisValidity.SOURCE_STALE),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-use")) { Text(stringResource(R.string.styleAnalysisUseDraft)) }
                Text(stringResource(R.string.styleAnalysisSaveSeparately))
                TextButton(onClick = {
                    if (!using) { using = true; onBusy(true); scope.launch {
                        try { backend.discardCandidate(id, record.requestId); useError = false }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { useError = true }
                        finally { using = false; onBusy(false) }
                    } }
                }, enabled = enabled && !using, modifier = Modifier.heightIn(min = 48.dp).testTag("style-analysis-discard")) {
                    Text(stringResource(R.string.styleAnalysisDiscardCandidate))
                }
                if (useConfirm) RenkinAlertDialog(onDismissRequest = { useConfirm = false },
                    title = { Text(stringResource(R.string.styleAnalysisReviewTitle)) },
                    text = { Text(stringResource(when {
                        validity == AnalysisValidity.SOURCE_STALE -> R.string.styleAnalysisUseStaleText
                        dirty -> R.string.styleAnalysisReplaceDirtyText
                        else -> R.string.styleAnalysisUseText
                    })) },
                    confirmButton = { TextButton(onClick = {
                        if (enabled && !using && validity in setOf(AnalysisValidity.CURRENT, AnalysisValidity.SOURCE_STALE)) {
                            val staleAccepted = validity == AnalysisValidity.SOURCE_STALE
                            using = true; onBusy(true); useConfirm = false
                            scope.launch {
                                try { onUse(backend.use(id, record.requestId, selected, staleAccepted), staleAccepted); useError = false }
                                catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { useError = true }
                                finally { using = false; onBusy(false) }
                            }
                        }
                    }, enabled = enabled && !using && validity in setOf(AnalysisValidity.CURRENT, AnalysisValidity.SOURCE_STALE),
                        modifier = Modifier.testTag("style-analysis-confirm-use")) { Text(stringResource(R.string.styleAnalysisUseDraft)) } },
                    dismissButton = { TextButton(onClick = { useConfirm = false }, modifier = Modifier.testTag("style-analysis-keep-draft")) {
                        Text(stringResource(R.string.colorizeCancel))
                    } })
            }
        }
    }
    if (confirm) {
        val request = state.preparation
        val validConfirmation = request != null && request.requestId == confirmedId &&
            request.context.matchesRoute(mode, source) &&
            state.preparedDraftHash == analysisDraftHash(draft) && !state.busy && enabled && state.hasKey
        RenkinAlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.styleAnalysisConsentTitle)) },
            text = { Text(stringResource(R.string.styleAnalysisConsentText, source ?: stringResource(R.string.styleAnalysisOwnSource),
                request?.images?.size ?: 0, request?.model ?: DEFAULT_ANALYSIS_MODEL)) },
            confirmButton = { TextButton(onClick = {
                confirm = false
                if (validConfirmation) backend.submit(id, checkNotNull(request).requestId)
            }, enabled = validConfirmation,
                modifier = Modifier.testTag("style-analysis-confirm-paid")) { Text(stringResource(R.string.styleAnalysisConfirm)) } },
            dismissButton = { TextButton(onClick = { confirm = false }, modifier = Modifier.testTag("style-analysis-decline")) {
                Text(stringResource(R.string.colorizeCancel))
            } })
    }
}

@Composable
private fun AnalysisThumbnail(hash: String, read: suspend () -> ByteArray, index: Int) {
    val latest by rememberUpdatedState(read)
    val image by produceState<ImageBitmap?>(null, hash) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                val bytes = latest(); val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
            value = bitmap?.asImageBitmap()
            kotlinx.coroutines.awaitCancellation()
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null }
        finally { bitmap?.recycle() }
    }
    if (image != null) Image(checkNotNull(image), stringResource(R.string.styleAnalysisPreview, index + 1), Modifier.size(64.dp))
    else Text(stringResource(R.string.styleAnalysisPreview, index + 1), Modifier.testTag("style-analysis-preview-$index"))
}
