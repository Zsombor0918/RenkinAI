package dev.renkinProject.renkin.ui.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.data.project.sha256
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import kotlinx.coroutines.*

/** One visible path. View, mode changes and local Save never authorize an endpoint. */
@Composable
internal fun ProjectStyleDnaPanel(id: Long, mode: ProjectSetupMode, source: String?, stamp: String?, enabled: Boolean,
    backend: ProjectAnalysisBackend, expected: Long?, draft: ProjectStyleDraft, onSaved: (SavedProjectStyle) -> Unit,
    selectedPath: String? = null, onPathChanged: (String) -> Unit = {}, onBusy: (Boolean) -> Unit = {},
    onDirty: (Boolean) -> Unit = {}, guided: Boolean = false) {
    val state by backend.state(id).collectAsState()
    var localPath by rememberSaveable(id) { mutableStateOf<String?>(null) }
    val selectedMode = selectedPath ?: localPath ?: initialStyleAnalysisPath(expected, state)
    var model by rememberSaveable(id) { mutableStateOf(DEFAULT_ANALYSIS_MODEL) }
    var detail by rememberSaveable(id) { mutableStateOf("low") }
    var confirmation by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var review by rememberSaveable(id) { mutableStateOf(false) }
    var edit by rememberSaveable(id) { mutableStateOf(false) }
    var details by rememberSaveable(id) { mutableStateOf(false) }
    var saving by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val record = state.dnaRecord
    val needsReview = record?.status == DnaStatus.RESULT_SAVED && state.dnaPlan == null
    val suggestion = record?.takeIf { needsReview }?.result ?: state.savedDna
    val sourceKey = if (needsReview) record?.plan?.id else suggestion?.let { sha256(it.encode().toByteArray()) }
    val initialGrammar = suggestion?.dna?.encode().orEmpty()
    val initialFamily = suggestion?.selectedFamily.orEmpty()
    val initialRefs = if (needsReview && suggestion != null) dnaReferenceHashes(suggestion,suggestion.selectedFamily,
        record?.plan?.samples?.map { it.hash }.orEmpty()) else draft.hashes
    // Stable primitives survive an initially empty controller and subsequent journal loading.
    // A hash avoids saving a second copy of the potentially 64 KiB grammar in the Bundle.
    var grammar by rememberSaveable(id) { mutableStateOf(initialGrammar) }
    var family by rememberSaveable(id) { mutableStateOf(initialFamily) }
    var refs by rememberSaveable(id) { mutableStateOf(ArrayList(initialRefs)) }
    var baselineKey by rememberSaveable(id) { mutableStateOf(sourceKey) }
    var baselineGrammar by rememberSaveable(id) { mutableStateOf(sha256(initialGrammar.toByteArray())) }
    var baselineFamily by rememberSaveable(id) { mutableStateOf(initialFamily) }
    var baselineRefs by rememberSaveable(id) { mutableStateOf(ArrayList(initialRefs)) }
    val dirty = sha256(grammar.toByteArray()) != baselineGrammar || family != baselineFamily || refs != baselineRefs
    val sourceChanged = dirty && sourceKey != null && baselineKey != sourceKey
    LaunchedEffect(sourceKey) {
        if (sourceKey != null && (baselineKey == null || (!dirty && baselineKey != sourceKey))) {
            grammar = initialGrammar; family = initialFamily; refs = ArrayList(initialRefs)
            baselineKey = sourceKey; baselineGrammar = sha256(initialGrammar.toByteArray())
            baselineFamily = initialFamily; baselineRefs = ArrayList(initialRefs); review = false
        }
    }
    LaunchedEffect(dirty) { onDirty(dirty) }
    fun choose(choice: String) { localPath = choice; onPathChanged(choice); review = false }
    fun finishDraft() {
        baselineGrammar = sha256(grammar.toByteArray()); baselineFamily = family; baselineRefs = ArrayList(refs)
        baselineKey = null; edit = false; review = false; onDirty(false)
    }
    val editable = needsReview || edit
    val sourcePrepared = mode != ProjectSetupMode.EXTEND || !source.isNullOrBlank() && stamp != null
    val plan = state.dnaPlan
    val matches = plan != null && plan.mode.name == selectedMode && plan.model == model && plan.detail == detail &&
        plan.context.matchesRoute(mode, source) && plan.context.baseRevision == expected &&
        (mode != ProjectSetupMode.EXTEND || plan.context.setupStamp == stamp) &&
        plan.draftHash == analysisDraftHash(draft) && plan.samples.map { it.hash } == state.samples?.samples?.map { it.hash }
    val candidate = suggestion?.let { saved -> runCatching {
        saved.copy(dna = StyleDnaV1.parse(grammar), selectedFamily = family).also {
            it.encode(); StyleDnaCompiler.specification(it)
        }
    }.getOrNull() }?.takeUnless { sourceChanged }
    Column(Modifier.fillMaxWidth().testTag("style-dna-panel"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.styleDnaTitle))
        for (choice in listOf("QUICK", "STANDARD", "DEEP")) TextButton(onClick = { choose(choice) },
            enabled = enabled && !state.busy && !saving && choice != "DEEP",
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = selectedMode == choice }
                .testTag("style-dna-mode-$choice")) {
            Text((if (selectedMode == choice) "✓ " else "") + stringResource(when (choice) {
                "QUICK" -> R.string.uxStyleQuickChoice
                "STANDARD" -> R.string.uxStyleStandardChoice
                else -> R.string.uxStyleDeepChoice
            }))
        }
        TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-advanced")) {
            Text(stringResource(R.string.uxStyleDetails))
        }
        if (details) {
            TextButton(onClick = { choose("LEGACY") }, enabled = enabled && !state.busy && !saving,
                modifier = Modifier.heightIn(min = 48.dp).semantics { selected = selectedMode == "LEGACY" }.testTag("style-dna-mode-LEGACY")) {
                Text((if (selectedMode == "LEGACY") "✓ " else "") + stringResource(R.string.styleDnaLegacy))
            }
            if (selectedMode != "LEGACY") {
                for (value in ANALYSIS_MODELS) TextButton(onClick = { model = value; review = false },
                    enabled = enabled && !state.busy && !saving,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = model == value }) {
                    Text((if (model == value) "✓ " else "") + value)
                }
                Row { for (value in listOf("low", "high")) TextButton(onClick = { detail = value; review = false },
                    enabled = enabled && !state.busy && !saving,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = detail == value }.testTag("style-dna-detail-$value")) {
                    Text((if (detail == value) "✓ " else "") + "detail=$value")
                } }
                Text(stringResource(R.string.styleDnaPrice))
            }
        }
        if (selectedMode != "LEGACY") {
            Text(stringResource(if (selectedMode == "STANDARD") R.string.styleDnaStandard else R.string.styleDnaQuick))
            Text(stringResource(R.string.uxStyleCostUnknown))
            if (!state.hasKey) Text(stringResource(R.string.styleAnalysisNoKey), Modifier.testTag("style-dna-no-key"))
            if (!sourcePrepared) Text(stringResource(R.string.styleDnaSourceRequired), Modifier.testTag("style-dna-source-required"))
            TextButton(onClick = { choose("LEGACY") }, enabled = enabled && !state.busy && !saving,
                modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-manual")) { Text(stringResource(R.string.uxStyleManual)) }
            val prepare = {
                review = false
                backend.prepareDna(id, mode, source, stamp, expected, draft, DnaAnalysisMode.valueOf(selectedMode), model, detail)
            }
            if (state.dnaPlan == null && suggestion == null) Button(onClick = prepare,
                enabled = enabled && sourcePrepared && !state.busy && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-dna-prepare")) {
                Text(stringResource(R.string.styleDnaPrepare))
            }
            else OutlinedButton(onClick = prepare, enabled = enabled && sourcePrepared && !state.busy && !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-dna-prepare")) { Text(stringResource(R.string.styleDnaPrepare)) }
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(if (state.network) R.string.styleAnalysisRunning else R.string.styleAnalysisPreparing),
                    Modifier.testTag("style-dna-current-status"))
                TextButton(onClick = { backend.cancel(id) }, modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-cancel")) {
                    Text(stringResource(R.string.styleAnalysisCancel))
                }
            } else Text(stringResource(when {
                plan != null && !matches -> R.string.uxStylePlanChanged
                plan != null && !state.hasKey -> R.string.uxStylePlanNeedsKey
                plan != null -> R.string.uxStylePlanReady
                needsReview -> R.string.uxStyleReady
                suggestion != null -> R.string.uxStylePreviousUsed
                state.samples != null -> R.string.uxStylePreviewOnly
                else -> R.string.uxStylePrepareNext
            }), Modifier.testTag("style-dna-current-status"))
            // Keep the next action above the thumbnail grid, independently of older history.
            if (plan != null) {
                Text(stringResource(R.string.uxStylePlan, plan.samples.size, plan.endpointMaximum, plan.model))
                Button(onClick = { confirmation = plan.id },
                    enabled = enabled && matches && state.hasKey && !state.busy && !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-dna-start")) { Text(stringResource(R.string.styleDnaAnalyze)) }
            }
            state.samples?.takeIf { suggestion == null || details }?.let { samples ->
                Text(stringResource(R.string.uxStyleSamples, samples.samples.size))
                samples.samples.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { sample -> DnaThumbnail(sample.hash, { sample.png }) }
                    }
                }
                if (details) {
                    Text(stringResource(R.string.styleDnaCounts, samples.report.examined, samples.report.valid,
                        samples.report.duplicates, samples.report.unsupported, samples.samples.size), Modifier.testTag("style-dna-counts"))
                    samples.report.reasonCounts?.forEach { (reason, count) ->
                        Text(reason.name + ": " + count, Modifier.testTag("style-dna-reason-" + reason.name))
                    }
                    if (samples.report.candidates != null && samples.report.examined < samples.report.candidates)
                        Text(stringResource(R.string.styleDnaTimeBound))
                    samples.report.evidence?.let { evidence ->
                        Text(stringResource(R.string.styleAnalysisMeasuredOnly)); Text(evidence.metrics)
                        Text(stringResource(R.string.styleAnalysisLocalPolicy, evidence.colorPolicy.name))
                    }
                }
            }
            if (record != null) {
                if (details) {
                    Text(stringResource(R.string.uxStyleHistory), Modifier.testTag("style-dna-history"))
                    if (!state.busy && record.status in setOf(DnaStatus.FAILED, DnaStatus.UNCERTAIN)) {
                        Text(stringResource(R.string.uxStylePreviousFailed), Modifier.testTag("style-dna-last-outcome"))
                        Text(stringResource(when (record.reason) {
                            "UNAUTHORIZED" -> R.string.uxStyleKeyUnavailableOrRejected
                            "RATE_LIMITED" -> R.string.styleAnalysisRateLimited
                            "REQUEST_REJECTED_402" -> R.string.styleAnalysisQuotaExceeded
                            "REQUEST_REJECTED_400", "REQUEST_REJECTED_422" -> R.string.styleAnalysisRequestRejected
                            "REFUSED" -> R.string.styleAnalysisRefused
                            "RESPONSE_INCOMPLETE" -> R.string.uxStyleResponseIncomplete
                            "RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS" -> R.string.uxStyleResponseIncompleteTokens
                            "RESPONSE_INCOMPLETE_CONTENT_FILTER" -> R.string.uxStyleResponseIncompleteFilter
                            "RESPONSE_INCOMPLETE_UNKNOWN" -> R.string.uxStyleResponseIncompleteUnknown
                            "RESPONSE_FAILED" -> R.string.uxStyleResponseFailed
                            "ENVELOPE_INVALID" -> R.string.uxStyleEnvelopeInvalid
                            in DNA_ENVELOPE_FAILURE_CODES -> R.string.uxStyleEnvelopeInvalid
                            "REPORT_SCHEMA_INVALID" -> R.string.uxStyleReportsInvalid
                            in DNA_REPORT_FAILURE_CODES -> R.string.uxStyleReportsInvalid
                            "SYNTHESIS_INVALID" -> R.string.uxStyleSynthesisInvalid
                            "INVALID_RESPONSE", "INVALID_OR_INTERRUPTED" -> R.string.uxStyleHistoricalCauseUnknown
                            "STALE_SOURCE" -> R.string.styleAnalysisStale
                            else -> R.string.styleDnaNoReplay
                        }), Modifier.testTag("style-dna-outcome-reason"))
                    }
                    Text(stringResource(when (record.status) {
                        DnaStatus.RESULT_SAVED -> R.string.uxStyleReady
                        DnaStatus.REVIEWED -> R.string.uxStylePreviousUsed
                        DnaStatus.FAILED -> R.string.uxStylePreviousFailed
                        DnaStatus.UNCERTAIN -> R.string.uxStyleUncertain
                        else -> R.string.styleAnalysisRunning
                    }))
                    Text(stringResource(R.string.styleDnaNoReplay))
                    val uploaded = record.plan.steps.filter { step ->
                        record.steps.single { it.id == step.id }.status in setOf("SUBMITTED", "DONE")
                    }.sumOf { it.samples.size }
                    Text(stringResource(R.string.styleDnaProgress, record.reports.size, record.plan.samples.size,
                        uploaded, record.submitted, record.plan.endpointMaximum), Modifier.testTag("style-dna-progress"))
                    Text(record.status.name + record.reason?.let { ": " + it }.orEmpty(), Modifier.testTag("style-dna-status"))
                    var reportsOpen by rememberSaveable(id, record.plan.id) { mutableStateOf(false) }
                    TextButton(onClick = { reportsOpen = !reportsOpen },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-reports")) { Text(stringResource(R.string.styleDnaReports)) }
                    if (reportsOpen) record.reports.forEach { report ->
                        Column(Modifier.testTag("style-dna-report-" + report.sampleId)) {
                            Text(report.sampleId + " · " + report.familyVote + " · " + report.colorPolicy)
                            DnaThumbnail(report.sampleHash, { backend.dnaPreview(id, report.sampleHash) })
                            report.traits.forEach { (trait, observation) ->
                                Text(trait + " · " + observation.provenance + ": " + observation.value)
                            }
                            Text(stringResource(R.string.styleDnaDoNotCopy))
                        }
                    }
                    if (needsReview) TextButton(onClick = {
                        if (!saving) {
                            saving = true; onBusy(true)
                            scope.launch {
                                try { backend.discardDna(id, record.plan.id); finishDraft(); error = false }
                                catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { error = true }
                                finally { saving = false; onBusy(false) }
                            }
                        }
                    }, enabled = enabled && !saving && !state.busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-discard")) { Text(stringResource(R.string.styleAnalysisDiscardCandidate)) }
                }
            }
            if (suggestion != null) {
                Text(stringResource(R.string.uxStyleSummary)); Text(stringResource(R.string.uxStyleInferredHelp))
                fun selectFamily(next: String) {
                    family = next; review = false
                    if(needsReview) refs = ArrayList(dnaReferenceHashes(suggestion,next,record?.plan?.samples?.map { it.hash }.orEmpty()))
                }
                if(editable) TextButton(onClick = { selectFamily(DNA_PACK_STYLE) },enabled = enabled && !saving && !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min=48.dp).semantics { selected = family == DNA_PACK_STYLE }
                        .testTag("style-dna-pack")) { Text(stringResource(R.string.styleDnaOverall)) }
                suggestion.dna.families.forEachIndexed { index, f ->
                    if (editable && details) TextButton(onClick = { selectFamily(f.id) },
                        enabled = enabled && !saving && !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { selected = family == f.id }
                            .testTag("style-dna-family-" + f.id)) {
                        Text((if (family == f.id) "✓ " else "") +
                            stringResource(R.string.uxStyleFamily, index + 1, f.observedCount, suggestion.reports.size))
                    }
                    if(details && f.observedCount < 3) Text(stringResource(R.string.styleDnaProvisionalFamily))
                }
                val displayedFamily = runCatching { StyleDnaCompiler.family(candidate ?: suggestion) }.getOrNull()
                displayedFamily?.let { f ->
                    listOf("stroke_fill","rounding","simplification","scale_padding","negative_space").mapNotNull { key ->
                        f.drawing.getValue(key).takeUnless { it.provenance == DnaProvenance.UNKNOWN }?.value
                    }.distinct().take(4).forEach { Text(it,maxLines=3,overflow=TextOverflow.Ellipsis) }
                    Text(stringResource(R.string.styleDnaUncertainSummary))
                    Text(stringResource(R.string.uxStyleSurface, friendlyStyleValue(f.tile.name), friendlyStyleValue(f.depth.name)))
                    Text(stringResource(when (f.colorPolicy) {
                        PackColorPolicy.ADAPT_BRAND_COLORS -> R.string.uxStyleBrandColors
                        PackColorPolicy.MONOCHROME -> R.string.uxStyleMonochrome
                        PackColorPolicy.DUOTONE -> R.string.uxStyleDuotone
                        PackColorPolicy.FIXED_PALETTE -> R.string.uxStyleFixedPalette
                        else -> R.string.uxStyleMixedColors
                    }))
                    Text(stringResource(R.string.uxStyleReferences,if(needsReview) refs.size else draft.hashes.size))
                    (if(needsReview) refs else draft.hashes).chunked(5).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            row.forEach { hash -> DnaThumbnail(hash,{ backend.dnaPreview(id,hash) }) }
                        }
                    }
                    if (details) {
                        f.drawing.forEach { (key, drawing) ->
                            Text(friendlyStyleValue(key) + " · " + stringResource(when (drawing.provenance) {
                                DnaProvenance.OBSERVED -> R.string.uxStyleObserved
                                DnaProvenance.INFERRED -> R.string.uxStyleInferred
                                DnaProvenance.UNKNOWN -> R.string.uxStyleNotEstablished
                            }) + ": " + drawing.value)
                        }
                        f.colorRoles.forEach { role -> Text(role.role + " · " + role.strength + ": " + role.colors.joinToString()) }
                        suggestion.dna.uncertainty.forEach { Text(it) }
                    }
                }
                TextButton(onClick = { edit = !edit; if (edit) details = true },
                    enabled = enabled && !saving && !state.busy,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-edit")) { Text(stringResource(R.string.uxStyleEdit)) }
                if (edit) OutlinedTextField(grammar, { if (it.length <= 65536) { grammar = it; review = false } },
                    enabled = enabled && !saving && !state.busy, label = { Text(stringResource(R.string.styleDnaGrammar)) },
                    modifier = Modifier.fillMaxWidth().testTag("style-dna-grammar"), minLines = 4, maxLines = 12)
                if (sourceChanged) Text(stringResource(R.string.uxStyleReloadKept), Modifier.testTag("style-dna-draft-kept"))
                else if (candidate == null) Text(stringResource(R.string.styleDnaInvalid))
                if (editable && details) {
                    Text(stringResource(R.string.uxStyleReferences, if (needsReview) refs.size else draft.hashes.size))
                    if (needsReview) record?.plan?.samples?.forEach { sample ->
                        Row {
                            Checkbox(sample.hash in refs, { checked ->
                                review = false
                                refs = ArrayList(if (checked) { if (refs.size < 5) refs + sample.hash else refs }
                                    else refs.filter { it != sample.hash })
                            }, enabled = enabled && !saving && candidate?.let { dnaReferencesMatch(it,family,listOf(sample.hash)) } == true,
                                modifier = Modifier.size(48.dp).testTag("style-dna-ref-" + sample.id))
                            DnaThumbnail(sample.hash, { backend.dnaPreview(id, sample.hash) }); Text(sample.id)
                        }
                        val position = refs.indexOf(sample.hash)
                        if (position >= 0) TextButton(onClick = {
                            refs = ArrayList(refs).apply { removeAt(position); add(position - 1, sample.hash) }; review = false
                        }, enabled = enabled && !saving && position > 0,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("style-dna-ref-up-" + sample.id)) {
                            Text(stringResource(R.string.projectStyleMoveEarlier))
                        }
                    }
                }
                if (editable) {
                    Text(stringResource(R.string.uxStyleReviewHelp))
                    Row {
                        Checkbox(review, { review = it }, enabled = enabled && !saving && !state.busy,
                            modifier = Modifier.size(48.dp).testTag("style-dna-reviewed"))
                        Text(stringResource(R.string.styleDnaReviewed))
                    }
                    Button(onClick = {
                        if (candidate != null && !saving) {
                            saving = true; onBusy(true)
                            scope.launch {
                                try {
                                    val saved = backend.saveDna(id, record?.takeIf { needsReview }?.plan?.id, grammar, family,
                                        if (needsReview) refs else draft.hashes, expected, draft)
                                    finishDraft(); error = false; onSaved(saved)
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { error = true }
                                finally { saving = false; onBusy(false) }
                            }
                        }
                    }, enabled = enabled && !saving && !state.busy && review && candidate != null &&
                        (!needsReview || (state.dnaValidity ?: state.validity) == AnalysisValidity.CURRENT),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("style-dna-save")) {
                        Text(stringResource(if (guided) R.string.uxStyleSaveContinue else R.string.styleDnaSave))
                    }
                }
            }
            if (error || state.error != null) Text(stringResource(when (state.error) {
                "source_required" -> R.string.styleDnaSourceRequired
                "source_changed" -> R.string.styleDnaSourceChanged
                "references_empty" -> R.string.styleAnalysisEmpty
                "samples_unavailable" -> R.string.styleDnaSamplesUnavailable
                "history_unreadable" -> R.string.styleDnaHistoryError
                "storage" -> if (state.dnaLocalDiagnostic?.stage in setOf(DnaLocalStage.SOURCE_OPEN, DnaLocalStage.SAMPLE_RENDER,
                    DnaLocalStage.SOURCE_RECHECK)) R.string.styleDnaSamplesUnavailable else R.string.styleDnaStorageError
                "inputs_changed" -> R.string.styleDnaInputsChanged
                "invalid_plan", "payload_size" -> R.string.styleDnaInvalidPlan
                else -> if (suggestion != null) R.string.uxStyleCurrentAttemptFailed else R.string.styleDnaLocalError
            }), Modifier.testTag("style-dna-error"))
            if (state.error != null) state.dnaLocalDiagnostic?.let { diagnostic ->
                Text(stringResource(R.string.styleDnaLocalDiagnostic, diagnostic.code), Modifier.testTag("style-dna-local-diagnostic"))
            }
        }
    }
    if (confirmation != null) RenkinAlertDialog(onDismissRequest = { confirmation = null },
        title = { Text(stringResource(R.string.styleAnalysisConsentTitle)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.styleDnaConsent, plan?.samples?.size ?: 0, plan?.model.orEmpty(),
                plan?.detail.orEmpty(), plan?.endpointMaximum ?: 0))
            plan?.steps?.forEachIndexed { index, step ->
                Text((index + 1).toString() + ". " + (if (step.synthesis) "Synthesis (text only)" else step.samples.size.toString() + " individual labeled images"))
            }
            Text(stringResource(R.string.styleDnaPrice)); Text(stringResource(R.string.styleDnaNoReplay))
        } },
        confirmButton = { TextButton(onClick = {
            val selected = confirmation; confirmation = null
            if (enabled && !state.busy && !saving && state.hasKey && matches && plan != null && selected == plan.id)
                backend.submitDna(id, plan.id)
        }, enabled = enabled && !state.busy && !saving && state.hasKey &&
            matches && plan?.id == confirmation,
            modifier = Modifier.testTag("style-dna-confirm-paid")) { Text(stringResource(R.string.styleDnaConfirm)) } },
        dismissButton = { TextButton(onClick = { confirmation = null }, modifier = Modifier.testTag("style-dna-decline")) {
            Text(stringResource(R.string.colorizeCancel))
        } })
}

private fun friendlyStyleValue(value: String) = value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun DnaThumbnail(hash: String, read: suspend () -> ByteArray) {
    val latest by rememberUpdatedState(read)
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, hash) {
        var bitmap: android.graphics.Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                val png = latest()
                bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size,
                    android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 })
            }
            value = bitmap?.asImageBitmap(); awaitCancellation()
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null }
        finally { bitmap?.recycle() }
    }
    image?.let { Image(it, stringResource(R.string.projectStyleReferencePreview), Modifier.size(48.dp)) }
}
