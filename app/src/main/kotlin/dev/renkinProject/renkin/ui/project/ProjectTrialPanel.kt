package dev.renkinProject.renkin.ui.project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.data.project.IconAssetVariant
import dev.renkinProject.renkin.packages.PackageResourceResolver
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.WavyLoadingBar
import dev.renkinProject.renkin.ui.drawAlphaCheckerboard
import kotlinx.coroutines.CancellationException
import dev.renkinProject.renkin.ui.theme.CardShape

internal data class TrialReviewSummary(val current: List<IconAssetVariant>,
    val accepted: List<IconAssetVariant>, val unreviewed: List<IconAssetVariant>,
    val canApprove: Boolean, val ready: Boolean)

private fun trialEvidence(variant: IconAssetVariant): TrialVariantEvidence? = variant.trialContextJson?.let {
    runCatching { TrialVariantEvidence.parse(it) }.getOrNull()
}

/** Presentation of repository-owned proof, including older successful current variants. */
internal fun trialReviewSummary(state: ProjectTrialState): TrialReviewSummary {
    val context = state.preparation?.context
    val current = if (context == null) emptyList() else state.variants.filter { variant ->
        variant.requestStatus == "COMPLETED" && variant.profileId == context.profileId &&
            variant.styleRevision == context.revision && trialEvidence(variant)?.let {
                it.requestId == variant.id && (it.context == context || it.reviewContext == context)
            } == true
    }
    val accepted = current.filter { it.reviewStatus == "ACCEPTED" && trialEvidence(it)?.reviewContext == context }
    val unreviewed = current.filter { it.reviewStatus == "UNREVIEWED" }
    val canApprove = context != null && !state.busy && state.error == null && accepted.isNotEmpty() && unreviewed.isEmpty()
    return TrialReviewSummary(current, accepted, unreviewed, canApprove, state.ready && canApprove)
}

/** Style/options can invalidate a paid plan without replacing the chosen target identities. */
private fun TrialContext.sameTargetSelection(other: TrialContext) = profileId == other.profileId &&
    mode == other.mode && sourcePackage == other.sourcePackage && sourceStamp == other.sourceStamp &&
    catalogStamp == other.catalogStamp && selectionHash == other.selectionHash && epoch == other.epoch

@Composable
internal fun ProjectTrialPanel(id: Long, name: String, mode: ProjectSetupMode, source: String?, available: Boolean,
    backend: ProjectTrialBackend, onBack: () -> Unit, onRefine: () -> Unit, onEditor: () -> Unit,
    appIcon: @Composable (String) -> Unit = {}, onTargets: () -> Unit = onBack,
    onComplete: (() -> Unit)? = null, appName: (String) -> String? = { null }, guided: Boolean = false) {
    val state by backend.state(id).collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable(id) { mutableStateOf(arrayListOf<String>()) }
    var selectionContext by rememberSaveable(id) { mutableStateOf("") }
    var allowCovered by rememberSaveable(id) { mutableStateOf(false) }
    var dark by rememberSaveable(id) { mutableStateOf(false) }
    var checker by rememberSaveable(id) { mutableStateOf(true) }
    var optionsExpanded by rememberSaveable(id) { mutableStateOf(false) }
    var historyExpanded by rememberSaveable(id) { mutableStateOf(false) }
    var requestHistoryExpanded by rememberSaveable(id) { mutableStateOf(false) }
    var previewOptionsExpanded by rememberSaveable(id) { mutableStateOf(false) }
    var recheckId by rememberSaveable(id) { mutableStateOf<String?>(null) }
    val theme = PackageResourceResolver.displayedNightMode
    val trace = LocalProjectInvalidationTrace.current
    val traceKeys = remember { arrayOfNulls<List<Any?>>(1) }
    ObserveCoverageInvalidation(source, observeLauncherChanges = true, onPackageInvalidate = { changed ->
        if (available && !state.network) backend.packageChanged(id, mode, source, changed)
    }) { reload++ }
    // An invalidation while a paid job owns the controller is rechecked locally when it finishes.
    LaunchedEffect(id, mode, source, reload, theme, available, state.network) {
        if (trace != null) {
            val keys = listOf(id, mode, source, reload, theme, available, state.network)
            val labels = listOf("id", "mode", "source", "reload", "theme", "available", "network")
            val previous = traceKeys[0]
            trace("trial.effect", if (previous == null) "initial" else
                labels.filterIndexed { index, _ -> keys[index] != previous[index] }.joinToString(","))
            traceKeys[0] = keys
        }
        if (available && !state.network) backend.load(id, mode, source)
    }
    DisposableEffect(id, backend) {
        trace?.invoke("trial.panel.attach", "composition")
        onDispose { trace?.invoke("trial.panel.detach", "composition"); backend.release(id) }
    }
    BackHandler(onBack = onBack)
    val p = state.preparation
    val context = p?.context?.encode().orEmpty()
    LaunchedEffect(context) {
        if (context.isNotEmpty() && selectionContext != context) {
            val previous = runCatching { TrialContext.parse(org.json.JSONObject(selectionContext)) }.getOrNull()
            if (previous == null || p == null || !previous.sameTargetSelection(p.context)) {
                selected = ArrayList(StyleTrialTargetSelector.recommend(p!!.candidates))
                allowCovered = false
            } else selected = ArrayList(selected.filter { key -> p.candidates.any {
                it.target.key == key && it.eligible && (!it.overrideRequired || allowCovered)
            } })
            selectionContext = context
        }
    }
    val enabled = available && !state.busy
    val validChoice = p != null && runCatching { StyleTrialTargetSelector.validateChoice(p.candidates, selected, allowCovered) }.isSuccess
    val refsValid = p != null && (p.context.provider != AiSettings.STABILITY || p.style.references.size == 1)
    val review = trialReviewSummary(state)
    val ordered = state.variants.sortedWith(compareByDescending<IconAssetVariant> { it.createdAt }.thenByDescending { it.id })
    val currentIds = review.current.map { it.id }.toSet()
    val latest = ordered.filter { it.id in currentIds }.distinctBy { it.componentKey }
    val compact = guided && p?.context?.montageFingerprint?.isNotEmpty() == true
    val latestIds = latest.map { it.id }.toSet()
    val previous = ordered.filter { it.id !in latestIds }
    val frozenNames = state.records.sortedBy { it.confirmedAt }.flatMap { it.plan.inputs }.associate { it.key to it.name }
    val names = frozenNames + p?.candidates.orEmpty().associate { it.target.key to it.target.name }
    val fallbackName = stringResource(R.string.guidedTrialSavedApp)
    fun label(key: String) = appName(key)?.takeIf { it.isNotBlank() && it != key }
        ?: names[key]?.takeIf { it.isNotBlank() && it != key } ?: fallbackName
    val lastRecord = state.records.maxByOrNull { it.confirmedAt }
    CenteredFullscreenContent {
        LazyColumn(Modifier.testTag("project-trials-$id"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SetupHeading(stringResource(R.string.trialTitle), onBack)
                if (!compact) Text(name, style = MaterialTheme.typography.titleLarge)
                if (p != null) Text(stringResource(R.string.guidedTrialSummary, p.context.revision,
                    review.current.size, review.current.count { it.reviewStatus != "UNREVIEWED" }), Modifier.testTag("trial-style"))
                if (state.busy) WavyLoadingBar(Modifier.fillMaxWidth())
                if (state.error != null || p == null && !state.busy) Text(stringResource(when (state.error) {
                    "missing_style" -> R.string.trialSaveStyle
                    "missing_targets" -> R.string.trialSaveTargets
                    "stale_targets" -> R.string.trialStaleTargets
                    "stale_source" -> R.string.trialStaleSource
                    "changed_context" -> R.string.trialChangedContext
                    "stability_references" -> R.string.trialStabilityRefs
                    "dna_reference_mismatch" -> R.string.styleDnaReferenceMismatch
                    "unverified_original" -> R.string.guidedTrialUnverifiedUnavailable
                    else -> R.string.guidedTrialContextError
                }), Modifier.testTag("trial-error"), color = MaterialTheme.colorScheme.error)
                if (p == null && state.error == "missing_style") Button(onClick = onRefine, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-refine")) {
                    Text(stringResource(R.string.guidedTrialReturnStyle))
                }
                if (p == null && state.error != "missing_style") Button(onClick = onTargets, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-targets")) {
                    Text(stringResource(R.string.trialReviewTargets))
                } else TextButton(onClick = onTargets, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-targets")) {
                    Text(stringResource(if (guided) R.string.guidedTrialChangeTargets else R.string.trialReviewTargets))
                }
                if (state.network) Button(onClick = { backend.stop(id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-stop")) { Text(stringResource(R.string.trialStop)) }
                if (review.ready) {
                    Text(stringResource(R.string.guidedTrialApproved), Modifier.testTag("trial-ready"))
                    onComplete?.let { complete -> Button(onClick = complete, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-complete")) {
                        Text(stringResource(R.string.guidedTrialContinue))
                    } }
                } else onComplete?.let { complete -> TextButton(onClick = complete, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-overview")) {
                    Text(stringResource(R.string.guidedTrialOverview))
                } }
                if (lastRecord?.items?.any { it.status == TrialStatus.UNCERTAIN } == true)
                    Text(stringResource(R.string.guidedTrialUncertain), Modifier.testTag("trial-latest-uncertain"))
                if (lastRecord?.items?.any { it.status == TrialStatus.PNG_SAVED } == true)
                    Text(stringResource(R.string.guidedTrialLocalPersistence), Modifier.testTag("trial-latest-persistence"))
                lastRecord?.items?.firstOrNull { it.status == TrialStatus.FAILED }?.let { failed ->
                    Text(stringResource(when (failed.reason) {
                        "model_unavailable", "http_404" -> R.string.guidedTrialModelUnavailable
                        "http_401", "http_402", "http_403", "http_429" -> R.string.guidedTrialAccountError
                        else -> R.string.guidedTrialFailure
                    }), Modifier.testTag("trial-latest-failure"), color = MaterialTheme.colorScheme.error)
                }
                TrialExpandButton(optionsExpanded, { optionsExpanded = !optionsExpanded },
                    stringResource(if (compact) R.string.guidedTrialDetails else R.string.guidedTrialOptions), "trial-options-toggle")
                if (optionsExpanded) {
                    Text(stringResource(R.string.guidedTrialBoundary))
                    if (compact) Text(stringResource(R.string.guidedTrialApprovalBoundary))
                    Text(stringResource(R.string.projectIdentity, id)); Text(mode.name); source?.let { Text(it) }
                    p?.let { preparation ->
                        Text(stringResource(R.string.trialRevision, preparation.context.revision, preparation.style.references.size))
                        Text(checkNotNull(preparation.style.revision).specification)
                        if (preparation.style.revision.userInstruction.isNotBlank()) Text(preparation.style.revision.userInstruction)
                        TrialOptions(preparation.context, enabled) { provider, model, quality -> backend.options(id, provider, model, quality) }

                        Text(stringResource(R.string.trialSuggestion))
                    }
                    OutlinedButton(onClick = { trace?.invoke("trial.reload.user", "button"); backend.load(id, mode, source) }, enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("trial-reload")) { Text(stringResource(R.string.guidedTrialReload)) }
                    if (p != null || state.error != "missing_style") OutlinedButton(onClick = onRefine, enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("trial-refine")) { Text(stringResource(R.string.trialRefine)) }
                    OutlinedButton(onClick = onEditor, enabled = available,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("trial-editor")) { Text(stringResource(R.string.projectClassicEditor)) }
                }
            }
            if (p != null && (!compact || latest.isEmpty() || optionsExpanded)) {
                item {
                    if (!compact || optionsExpanded) Text(stringResource(if (guided) R.string.guidedTrialChosenApps else R.string.guidedTrialChooseApps),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    if (!compact || optionsExpanded) Text(stringResource(R.string.trialCount, selected.size, p.candidates.count { it.recommended }), Modifier.testTag("trial-count"))
                    if (!state.hasKey) Text(stringResource(R.string.trialNoKey), Modifier.testTag("trial-no-key"))
                    if (!refsValid) Text(stringResource(R.string.trialStabilityRefs), Modifier.testTag("trial-reference-error"))
                    if (p.candidates.none { it.recommended }) Text(stringResource(R.string.trialNoEligible), Modifier.testTag("trial-empty"))
                    if (p.candidates.any { it.overrideRequired && it.eligible }) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(allowCovered, enabled = enabled, role = Role.Checkbox) {
                            allowCovered = it
                            selected = if (it && guided) ArrayList(p.candidates.filter { c -> c.eligible }.map { c -> c.target.key }.take(3))
                                else ArrayList(selected.filter { key -> it || p.candidates.none { c -> c.target.key == key && c.overrideRequired } })
                            backend.cancelPreparation(id)
                        }.testTag("trial-covered-override")) {
                        Checkbox(allowCovered, onCheckedChange = null); Text(stringResource(R.string.trialCoveredOverride))
                    }
                }
                items(p.candidates, key = { "target-${it.target.key}" }) { candidate ->
                    val key = candidate.target.key
                    val selectable = enabled && candidate.eligible && (!candidate.overrideRequired || allowCovered) && (key in selected || selected.size < 3)
                    var details by rememberSaveable(id, key) { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val selectionModifier = if (guided) Modifier else Modifier.toggleable(key in selected, selectable, Role.Checkbox) {
                            selected = ArrayList(if (it) selected + key else selected - key); backend.cancelPreparation(id)
                        }
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(selectionModifier).testTag("trial-target-$key"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            appIcon(key)
                            if (!guided) Checkbox(key in selected, onCheckedChange = null, enabled = selectable)
                            Text(label(key), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        }
                        if (!candidate.eligible) Text(stringResource(if (candidate.reason == "protected_local") R.string.trialProtected else R.string.trialUnproven))
                        if (candidate.overrideRequired) Text(stringResource(R.string.trialCovered))
                        if (!compact || optionsExpanded) TrialExpandButton(details, { details = !details }, stringResource(R.string.guidedTrialDetails), "trial-target-details-$key")
                        if (details) Text(key, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    val canPrepare = enabled && validChoice && refsValid && state.hasKey && state.error == null
                    if (review.current.isEmpty() && !review.ready) Button(onClick = { backend.prepare(id, selected.toList(), allowCovered) },
                        enabled = canPrepare, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-prepare")) {
                        Text(stringResource(R.string.guidedTrialPrepare))
                    } else OutlinedButton(onClick = { backend.prepare(id, selected.toList(), allowCovered) }, enabled = canPrepare,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-prepare")) { Text(stringResource(R.string.guidedTrialPrepare)) }
                    if (!canPrepare) Text(stringResource(when {
                        state.busy -> R.string.guidedTrialWait
                        !state.hasKey -> R.string.guidedTrialKeyReason
                        !refsValid -> R.string.guidedTrialReferenceReason
                        state.error != null -> R.string.guidedTrialVerifyReason
                        else -> R.string.guidedTrialChoiceReason
                    }), Modifier.testTag("trial-prepare-reason"))
                }
            }
            if (latest.isNotEmpty()) item {
                if (!compact) Text(stringResource(R.string.guidedTrialCurrentResults), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("trial-current-results").semantics { heading() })
                if (!compact) Text(stringResource(R.string.guidedTrialReviewHelp))
                if (!compact || optionsExpanded) TrialExpandButton(previewOptionsExpanded, { previewOptionsExpanded = !previewOptionsExpanded },
                    stringResource(R.string.guidedTrialPreviewOptions), "trial-preview-options-toggle")
                if (previewOptionsExpanded) Row {
                    TextButton(onClick = { dark = !dark }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("trial-preview-theme")) {
                        Text(stringResource(if (dark) R.string.trialLight else R.string.trialDark))
                    }
                    TextButton(onClick = { checker = !checker }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("trial-preview-alpha")) {
                        Text(stringResource(R.string.trialChecker))
                    }
                }
            }
            items(latest, key = { "variant-${it.id}" }) { variant ->
                TrialResult(id, variant, label(variant.componentKey), state, p, backend, enabled, allowCovered,
                    refsValid, dark, checker, appIcon) { recheckId = it }
            }
            if (!compact || latest.isNotEmpty()) item {
                val canApprove = enabled && review.canApprove && !review.ready
                if (!review.ready) Button(onClick = { backend.approve(id) }, enabled = canApprove,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-approve-style")) {
                    Text(stringResource(R.string.guidedTrialApprove))
                } else OutlinedButton(onClick = {}, enabled = false,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-approve-style")) {
                    Text(stringResource(R.string.guidedTrialApproved))
                }
                if (!review.ready && !canApprove) Text(stringResource(when {
                    state.busy -> R.string.guidedTrialWait
                    state.error != null || p == null -> R.string.guidedTrialVerifyReason
                    review.unreviewed.isNotEmpty() -> R.string.guidedTrialReviewReason
                    else -> R.string.guidedTrialAcceptReason
                }), Modifier.testTag("trial-approval-reason"))
                if (!compact) Text(stringResource(R.string.guidedTrialApprovalBoundary))
            }
            if (previous.isNotEmpty()) item {
                TrialExpandButton(historyExpanded, { historyExpanded = !historyExpanded },
                    stringResource(R.string.guidedTrialHistory, previous.size), "trial-history-toggle")
            }
            if (historyExpanded) items(previous, key = { "variant-${it.id}" }) { variant ->
                TrialResult(id, variant, label(variant.componentKey), state, p, backend, enabled, allowCovered,
                    refsValid, dark, checker, appIcon) { recheckId = it }
            }
            if (state.records.isNotEmpty()) item {
                TrialExpandButton(requestHistoryExpanded, { requestHistoryExpanded = !requestHistoryExpanded },
                    stringResource(R.string.guidedTrialRequestHistory), "trial-request-history-toggle")
            }
            if (requestHistoryExpanded) items(state.records.sortedByDescending { it.confirmedAt }, key = { "record-${it.plan.id}" }) { record ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.trialProgress, record.items.count { it.status == TrialStatus.REVIEWABLE }, record.items.size,
                        record.items.sumOf { it.endpointCalls }), Modifier.testTag("trial-progress-${record.plan.id}"))
                    record.items.forEach { row ->
                        val input = record.plan.inputs.single { it.requestId == row.requestId }
                        Text("${input.name}: ${stringResource(trialStatusLabel(row.status))}", Modifier.testTag("trial-status-${row.requestId}"))
                        if (row.status == TrialStatus.UNCERTAIN) Text(stringResource(R.string.trialUncertain))
                        if (row.status == TrialStatus.PNG_SAVED) Text(stringResource(R.string.trialLocalPersistence))
                        if (row.reason == "model_unavailable" || row.reason == "http_404") Text(stringResource(R.string.trialModelUnavailable))
                        if (row.reason in setOf("http_401", "http_402", "http_403", "http_429")) Text(stringResource(R.string.trialAccountError))
                    }
                }
            }
        }
    }
    val recheck = recheckId
    if (recheck != null) RenkinAlertDialog(onDismissRequest = { recheckId = null },
        title = { Text(stringResource(R.string.trialRecheckTitle)) },
        text = { Text(stringResource(R.string.trialRecheckText)) },
        confirmButton = { TextButton(onClick = { recheckId = null; backend.review(id, recheck, true) }, enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp).testTag("trial-recheck-confirm")) { Text(stringResource(R.string.trialAccept)) } },
        dismissButton = { TextButton(onClick = { recheckId = null }, Modifier.heightIn(min = 48.dp).testTag("trial-recheck-cancel")) {
            Text(stringResource(R.string.colorizeCancel))
        } })
    val plan = state.plan
    var originalsVerified by remember(plan?.id) { mutableStateOf(false) }
    var consentDetails by remember(plan?.id) { mutableStateOf(false) }
    val declinePreparation = {
        if (!state.network) {
            backend.cancelPreparation(id)
            // Re-read the unchanged durable approval; cancellation grants no request consent.
            trace?.invoke("trial.reload.user", "decline")
            reload++
        }
    }
    if (plan != null) RenkinAlertDialog(onDismissRequest = declinePreparation,
        title = { Text(stringResource(R.string.trialConsentTitle)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("trial-frozen-confirmation"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name)
            Text(stringResource(R.string.guidedTrialConsent, plan.inputs.size, plan.endpointMaximum, plan.context.provider,
                if (plan.context.provider == AiSettings.STABILITY) stringResource(R.string.trialStabilityRoute) else plan.context.model,
                if (plan.context.provider == AiSettings.STABILITY) stringResource(R.string.trialProviderDefault) else plan.context.quality,
                plan.context.revision))
            plan.inputs.forEach { input ->
                Text(input.name, style = MaterialTheme.typography.titleMedium)

                Text(stringResource(R.string.trialOriginalApp), Modifier.testTag("trial-original-label-${input.requestId}"))
                TrialPreview(id, input.originalHash, backend, "trial-frozen-${input.requestId}", 72, false, true,
                    stringResource(R.string.trialOriginalApp))
            }
            if (plan.referenceHashes.isNotEmpty()) Text(stringResource(if (plan.promptVersion == 3) R.string.packStyleTrialSheets else R.string.guidedTrialStyleOnlyReferences))
            if (plan.promptVersion != 3) plan.referenceHashes.forEachIndexed { index, hash ->
                Text(stringResource(R.string.projectStyleReferenceNumber, index + 1))
                TrialPreview(id, hash, backend, "trial-frozen-reference-$index", 72, false, true,
                    stringResource(R.string.projectStyleReferencePreview))
            }
            if (plan.inputs.any { it.originalEvidence != null }) {
                Text(stringResource(R.string.guidedTrialCheckOriginals))
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(originalsVerified, enabled, Role.Checkbox) {
                    originalsVerified = it; backend.verifyOriginals(id, plan.id, it)
                }.testTag("trial-verify-originals")) {
                    Checkbox(originalsVerified, onCheckedChange = null); Text(stringResource(R.string.trialVerifyOriginals))
                }
            }
            TrialExpandButton(consentDetails, { consentDetails = !consentDetails }, stringResource(R.string.guidedTrialDetails), "trial-consent-details")
            if (consentDetails) {
                if (plan.promptVersion == 3) {
                    plan.referenceHashes.forEachIndexed { index, hash ->
                        TrialPreview(id, hash, backend, "trial-frozen-reference-$index", 200, false, true,
                            stringResource(R.string.packStylePreview))
                    }
                    Text(plan.effectivePrompt(plan.inputs.first()), Modifier.testTag("trial-effective-prompt"))
                }
                Text(plan.specification); if (plan.instruction.isNotBlank()) Text(plan.instruction); Text(plan.fingerprint)
                Text(stringResource(R.string.trialStylePack, plan.context.sourcePackage ?: stringResource(R.string.styleAnalysisOwnSource)),
                    Modifier.testTag("trial-style-source"))
                plan.drawingRecipes.forEach { (key, recipe) -> Text(label(key)); Text(recipe) }
                plan.inputs.forEach { input ->
                    Text(input.key); Text(input.originalHash)
                    Text(input.origin, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("trial-original-origin-${input.requestId}"))
                    input.originalEvidence?.let { evidence -> Text(stringResource(R.string.trialOriginalEvidence, evidence.source,
                        evidence.drawableType, evidence.user, if (evidence.platformAgreement) stringResource(R.string.trialOriginalAgreement)
                        else stringResource(R.string.trialOriginalDifference))) }
                }
                plan.referenceHashes.forEachIndexed { index, hash -> Text(stringResource(R.string.projectStyleReferenceNumber, index + 1)); Text(hash) }
                if (plan.inputs.any { it.originalEvidence != null }) Text(stringResource(R.string.trialOriginalUnverified), Modifier.testTag("trial-original-unverified"))
            }
        } },
        confirmButton = { TextButton(onClick = { backend.submit(id, plan.id) }, enabled = enabled && state.hasKey && p?.context == plan.context &&
            state.error == null && (plan.inputs.none { it.originalEvidence != null } || originalsVerified),
            modifier = Modifier.heightIn(min = 48.dp).testTag("trial-confirm-paid")) { Text(stringResource(R.string.trialConfirmPaid)) } },
        dismissButton = { TextButton(onClick = declinePreparation, enabled = !state.network,
            modifier = Modifier.heightIn(min = 48.dp).testTag("trial-decline")) { Text(stringResource(R.string.colorizeCancel)) } })
}

@Composable
private fun TrialExpandButton(expanded: Boolean, onClick: () -> Unit, label: String, tag: String) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) {
        Text(stringResource(if (expanded) R.string.guidedTrialCollapse else R.string.guidedTrialExpand, label))
    }
}

private fun trialStatusLabel(status: TrialStatus) = when (status) {
    TrialStatus.PENDING -> R.string.guidedTrialPending
    TrialStatus.REQUESTING -> R.string.guidedTrialGenerating
    TrialStatus.PNG_SAVED -> R.string.guidedTrialPersistencePending
    TrialStatus.REVIEWABLE -> R.string.guidedTrialSavedResult
    TrialStatus.FAILED -> R.string.guidedTrialFailed
    TrialStatus.UNCERTAIN -> R.string.guidedTrialOutcomeUnknown
    TrialStatus.SKIPPED -> R.string.guidedTrialSkipped
}

@Composable
private fun TrialResult(id: Long, variant: IconAssetVariant, name: String, state: ProjectTrialState,
    preparation: TrialPreparation?, backend: ProjectTrialBackend, enabled: Boolean, allowCovered: Boolean,
    refsValid: Boolean, dark: Boolean, checker: Boolean, appIcon: @Composable (String) -> Unit, onRecheck: (String) -> Unit) {
    val evidence = trialEvidence(variant)
    val current = evidence != null && preparation != null &&
        (evidence.context == preparation.context || evidence.reviewContext == preparation.context)
    val candidate = preparation?.candidates?.find { it.target.key == variant.componentKey }
    val eligible = candidate?.eligible == true
    val generationAllowed = eligible && (candidate?.overrideRequired != true || allowCovered)
    val recheck = !current && evidence != null && preparation != null && evidence.context.sameArtworkInputs(preparation.context) && eligible
    var details by rememberSaveable(id, variant.id) { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth().testTag("trial-variant-${variant.id}"), shape = CardShape) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                appIcon(variant.componentKey)
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(when (variant.reviewStatus) {
                "ACCEPTED" -> R.string.guidedTrialAccepted
                "REJECTED" -> R.string.guidedTrialRejected
                else -> R.string.guidedTrialNeedsReview
            }))
            if (!current) Text(stringResource(R.string.guidedTrialHistorical), Modifier.testTag("trial-historical-${variant.id}"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrialComparison(id, variant.originalHash, backend, false, dark, checker, variant.id, Modifier.weight(1f))
                TrialComparison(id, variant.rawAssetHash, backend, true, dark, checker, variant.id, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { backend.review(id, variant.id, true) }, enabled = enabled && current && eligible && state.error == null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("trial-accept-${variant.id}")) { Text(stringResource(R.string.trialAccept)) }
                TextButton(onClick = { backend.review(id, variant.id, false) }, enabled = enabled && current && eligible && state.error == null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("trial-reject-${variant.id}")) { Text(stringResource(R.string.trialReject)) }
            }
            if (!current || !eligible || state.error != null) Text(stringResource(R.string.guidedTrialReviewDisabled),
                Modifier.testTag("trial-review-reason-${variant.id}"))
            if (recheck) OutlinedButton(onClick = { onRecheck(variant.id) }, enabled = enabled && state.error == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-recheck-${variant.id}")) { Text(stringResource(R.string.trialRecheck)) }
            val canRegenerate = enabled && state.hasKey && refsValid && generationAllowed && state.error == null
            OutlinedButton(onClick = { backend.prepare(id, listOf(variant.componentKey), allowCovered) }, enabled = canRegenerate,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-regenerate-${variant.id}")) { Text(stringResource(R.string.guidedTrialRegenerate)) }
            if (!canRegenerate) Text(stringResource(when {
                state.busy -> R.string.guidedTrialWait
                !state.hasKey -> R.string.guidedTrialKeyReason
                !refsValid -> R.string.guidedTrialReferenceReason
                eligible && !generationAllowed -> R.string.trialCovered
                else -> R.string.guidedTrialVerifyReason
            }), Modifier.testTag("trial-regenerate-reason-${variant.id}"))
            TrialExpandButton(details, { details = !details }, stringResource(R.string.guidedTrialDetails), "trial-variant-details-${variant.id}")
            if (details) {
                Text(variant.componentKey); Text(stringResource(R.string.trialVariantRevision, variant.styleRevision, variant.reviewStatus))
                Text(variant.originalHash); Text(variant.rawAssetHash)
                evidence?.let { e ->
                    Text(if (e.context.provider == AiSettings.STABILITY) stringResource(R.string.trialStabilityRoute)
                        else "${e.context.provider} · ${e.context.model} · ${e.context.quality}")
                    Text(e.originalOrigin)
                }
            }
        }
    }
}

@Composable
private fun TrialOptions(context: TrialContext, enabled: Boolean, onChange: (String, String, String) -> Unit) {
    Text(if (context.provider == AiSettings.STABILITY) stringResource(R.string.trialStabilityRoute)
        else "${context.provider} · ${context.model} · ${context.quality}", Modifier.testTag("trial-model"))
    AiSettings.PROVIDERS.forEach { provider -> TextButton(onClick = { onChange(provider, context.model, context.quality) },
        enabled = enabled && provider != context.provider, modifier = Modifier.heightIn(min = 48.dp).testTag("trial-provider-$provider")) { Text(provider) } }
    if (context.provider == AiSettings.OPENAI) AiSettings.MODELS.forEach { model -> TextButton(onClick = { onChange(context.provider, model, context.quality) },
        enabled = enabled && model != context.model, modifier = Modifier.heightIn(min = 48.dp).testTag("trial-model-$model")) { Text(model) } }
    if (context.provider == AiSettings.OPENAI) AiSettings.QUALITIES.forEach { quality -> TextButton(onClick = { onChange(context.provider, context.model, quality) },
        enabled = enabled && quality != context.quality, modifier = Modifier.heightIn(min = 48.dp).testTag("trial-quality-$quality")) { Text(quality) } }
}

@Composable
private fun TrialComparison(id: Long, hash: String, backend: ProjectTrialBackend, generated: Boolean,
    dark: Boolean, checker: Boolean, variant: String, modifier: Modifier = Modifier) {
    val label = stringResource(if (generated) R.string.trialGenerated else R.string.trialOriginal)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        TrialPreview(id, hash, backend, "trial-large-$generated-$variant", 128, dark, checker, label)
        TrialPreview(id, hash, backend, "trial-48-$generated-$variant", 48, dark, checker, stringResource(R.string.trialLauncherPreview, label))
    }
}

@Composable
private fun TrialPreview(id: Long, hash: String, backend: ProjectTrialBackend, tag: String, size: Int,
    dark: Boolean, checker: Boolean, label: String) {
    var unavailable by remember(id, hash, backend, size) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(null, id, hash, backend, size) {
        var decoded: Bitmap? = null
        try {
            val bytes = backend.preview(id, hash)
            if (bytes != null) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = Integer.highestOneBit((maxOf(bounds.outWidth, bounds.outHeight) / size).coerceAtLeast(1))
                decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                value = decoded
                unavailable = decoded == null
            } else unavailable = true
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null; unavailable = true }
        awaitDispose { decoded?.recycle() }
    }
    val background = if (dark) Color(0xff202020) else Color.White
    Box(Modifier.size(size.dp).background(background).drawBehind {
        if (checker) drawAlphaCheckerboard(if (dark) Color(0xff343434) else Color.White,
            if (dark) Color(0xff202020) else Color(0xffdddddd))
    }.testTag(tag)) {
        bitmap?.let { Image(it.asImageBitmap(), label, Modifier.fillMaxSize()) }
        if (unavailable) Text(stringResource(R.string.trialImageUnavailable), style = MaterialTheme.typography.labelSmall)
    }
}
