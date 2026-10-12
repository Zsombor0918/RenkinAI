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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.data.project.IconAssetVariant
import dev.renkinProject.renkin.packages.PackageResourceResolver
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.WavyLoadingBar
import dev.renkinProject.renkin.ui.drawAlphaCheckerboard
import kotlinx.coroutines.CancellationException

@Composable
internal fun ProjectTrialPanel(id: Long, name: String, mode: ProjectSetupMode, source: String?, available: Boolean,
    backend: ProjectTrialBackend, onBack: () -> Unit, onRefine: () -> Unit, onEditor: () -> Unit,
    appIcon: @Composable (String) -> Unit = {}) {
    val state by backend.state(id).collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable(id) { mutableStateOf(arrayListOf<String>()) }
    var selectionContext by rememberSaveable(id) { mutableStateOf("") }
    var allowCovered by rememberSaveable(id) { mutableStateOf(false) }
    var dark by rememberSaveable(id) { mutableStateOf(false) }
    var checker by rememberSaveable(id) { mutableStateOf(true) }
    val theme = PackageResourceResolver.displayedNightMode
    ObserveCoverageInvalidation(source, observeLauncherChanges = true) { reload++ }
    LaunchedEffect(id, mode, source, reload, theme, available) {
        if (available) backend.load(id, mode, source)
    }
    DisposableEffect(id, backend) { onDispose { backend.release(id) } }
    BackHandler(onBack = onBack)
    val p = state.preparation
    val context = p?.context?.encode().orEmpty()
    LaunchedEffect(context) {
        if (context.isNotEmpty() && selectionContext != context) {
            selected = ArrayList(StyleTrialTargetSelector.recommend(p!!.candidates))
            selectionContext = context; allowCovered = false
        }
    }
    val enabled = available && !state.busy
    val validChoice = p != null && runCatching { StyleTrialTargetSelector.validateChoice(p.candidates, selected, allowCovered) }.isSuccess
    val refsValid = p != null && (p.context.provider != AiSettings.STABILITY || p.style.references.size == 1)
    CenteredFullscreenContent {
        LazyColumn(Modifier.testTag("project-trials-$id"), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SetupHeading(stringResource(R.string.trialTitle), onBack)
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.projectIdentity, id)); Text(mode.name); source?.let { Text(it) }
                Text(stringResource(R.string.trialBoundary))
                if (state.busy) WavyLoadingBar(Modifier.fillMaxWidth())
                if (state.error != null || p == null && !state.busy) Text(stringResource(R.string.trialContextError), Modifier.testTag("trial-error"))
                OutlinedButton(onClick = { backend.load(id, mode, source) }, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-reload")) { Text(stringResource(R.string.projectReload)) }
                OutlinedButton(onClick = onRefine, enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-refine")) { Text(stringResource(R.string.trialRefine)) }
                OutlinedButton(onClick = onEditor, enabled = available,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-editor")) { Text(stringResource(R.string.projectClassicEditor)) }
                if (state.network) OutlinedButton(onClick = { backend.stop(id) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("trial-stop")) { Text(stringResource(R.string.trialStop)) }
                if (state.ready) Text(stringResource(R.string.trialReady), Modifier.testTag("trial-ready"))
            }
            if (p != null) {
                item {
                    Text(stringResource(R.string.trialRevision, p.context.revision, p.style.references.size), Modifier.testTag("trial-style"))
                    Text(checkNotNull(p.style.revision).specification)
                    if (p.style.revision.userInstruction.isNotBlank()) Text(p.style.revision.userInstruction)
                    TrialOptions(p.context, enabled) { provider, model, quality -> backend.options(id, provider, model, quality) }
                    if (!state.hasKey) Text(stringResource(R.string.trialNoKey), Modifier.testTag("trial-no-key"))
                    if (!refsValid) Text(stringResource(R.string.trialStabilityRefs), Modifier.testTag("trial-reference-error"))
                    Text(stringResource(R.string.trialSuggestion))
                    Text(stringResource(R.string.trialCount, selected.size, p.candidates.count { it.recommended }), Modifier.testTag("trial-count"))
                    if (p.candidates.none { it.recommended }) Text(stringResource(R.string.trialNoEligible), Modifier.testTag("trial-empty"))
                    if (p.candidates.any { it.overrideRequired && it.eligible }) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(allowCovered, enabled = enabled, role = Role.Checkbox) {
                            allowCovered = it
                            if (!it) selected = ArrayList(selected.filter { key -> p.candidates.none { c -> c.target.key == key && c.overrideRequired } })
                            backend.cancelPreparation(id)
                        }.testTag("trial-covered-override")) {
                        Checkbox(allowCovered, onCheckedChange = null); Text(stringResource(R.string.trialCoveredOverride))
                    }
                }
                items(p.candidates, key = { "target-${it.target.key}" }) { candidate ->
                    val key = candidate.target.key
                    val selectable = enabled && candidate.eligible && (!candidate.overrideRequired || allowCovered) && (key in selected || selected.size < 3)
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(key in selected, selectable, Role.Checkbox) {
                        selected = ArrayList(if (it) selected + key else selected - key); backend.cancelPreparation(id)
                    }.testTag("trial-target-$key")) {
                        Row { appIcon(key); Checkbox(key in selected, onCheckedChange = null, enabled = selectable); Text(candidate.target.name) }
                        Text(key, style = MaterialTheme.typography.bodySmall)
                        if (!candidate.eligible) Text(stringResource(if (candidate.reason == "protected_local")
                            R.string.trialProtected else R.string.trialUnproven))
                        if (candidate.overrideRequired) Text(stringResource(R.string.trialCovered))
                    }
                }
                item {
                    OutlinedButton(onClick = { backend.prepare(id, selected.toList(), allowCovered) },
                        enabled = enabled && validChoice && refsValid && state.hasKey,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-prepare")) {
                        Text(stringResource(R.string.trialPrepare))
                    }
                }
            }
            if (state.records.isNotEmpty()) item {
                Text(stringResource(R.string.trialProgressTitle))
                state.records.forEach { record ->
                    Text(stringResource(R.string.trialProgress, record.items.count { it.status == TrialStatus.REVIEWABLE }, record.items.size,
                        record.items.sumOf { it.endpointCalls }), Modifier.testTag("trial-progress-${record.plan.id}"))
                    record.items.forEach { item ->
                        val input = record.plan.inputs.single { it.requestId == item.requestId }
                        Text("${input.name}: ${item.status.name}", Modifier.testTag("trial-status-${item.requestId}"))
                        if (item.status == TrialStatus.UNCERTAIN) Text(stringResource(R.string.trialUncertain))
                        if (item.status == TrialStatus.PNG_SAVED) Text(stringResource(R.string.trialLocalPersistence))
                    }
                }
            }
            if (state.variants.isNotEmpty()) item {
                Text(stringResource(R.string.trialReviewTitle), style = MaterialTheme.typography.titleLarge)
                Row {
                    TextButton(onClick = { dark = !dark }, Modifier.heightIn(min = 48.dp).testTag("trial-preview-theme")) {
                        Text(stringResource(if (dark) R.string.trialLight else R.string.trialDark))
                    }
                    TextButton(onClick = { checker = !checker }, Modifier.heightIn(min = 48.dp).testTag("trial-preview-alpha")) {
                        Text(stringResource(R.string.trialChecker))
                    }
                }
            }
            items(state.variants.sortedWith(compareByDescending<IconAssetVariant> { it.createdAt }.thenBy { it.id }), key = { "variant-${it.id}" }) { v ->
                val evidence = v.trialContextJson?.let { runCatching { TrialVariantEvidence.parse(it) }.getOrNull() }
                val current = evidence != null && p != null && evidence.context == p.context
                Column(Modifier.fillMaxWidth().testTag("trial-variant-${v.id}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(v.componentKey); Text(stringResource(R.string.trialVariantRevision, v.styleRevision, v.reviewStatus))
                    if (!current) Text(stringResource(R.string.trialHistorical), Modifier.testTag("trial-historical-${v.id}"))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrialComparison(id, v.originalHash, backend, false, dark, checker, v.id)
                        TrialComparison(id, v.rawAssetHash, backend, true, dark, checker, v.id)
                    }
                    Row {
                        TextButton(onClick = { backend.review(id, v.id, true) }, enabled = enabled && current,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("trial-accept-${v.id}")) { Text(stringResource(R.string.trialAccept)) }
                        TextButton(onClick = { backend.review(id, v.id, false) }, enabled = enabled && current,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("trial-reject-${v.id}")) { Text(stringResource(R.string.trialReject)) }
                    }
                    OutlinedButton(onClick = { backend.prepare(id, listOf(v.componentKey), allowCovered) },
                        enabled = enabled && current && state.hasKey && refsValid && p?.candidates?.any {
                            it.target.key == v.componentKey && it.eligible && (!it.overrideRequired || allowCovered)
                        } == true, modifier = Modifier.heightIn(min = 48.dp).testTag("trial-regenerate-${v.id}")) {
                        Text(stringResource(R.string.trialRegenerate))
                    }
                }
            }
            item {
                OutlinedButton(onClick = { backend.approve(id) }, enabled = enabled && p != null && state.variants.any {
                    it.reviewStatus == "ACCEPTED" && it.trialContextJson?.let { text -> TrialVariantEvidence.parse(text).context == p.context } == true
                } && state.variants.none { it.reviewStatus == "UNREVIEWED" && it.trialContextJson?.let { text ->
                    TrialVariantEvidence.parse(text).context == p.context } == true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trial-approve-style")) {
                    Text(stringResource(R.string.trialApproveStyle))
                }
                Text(stringResource(R.string.trialApprovalBoundary))
            }
        }
    }
    val plan = state.plan
    if (plan != null) RenkinAlertDialog(onDismissRequest = { if (!state.network) backend.cancelPreparation(id) },
        title = { Text(stringResource(R.string.trialConsentTitle)) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("trial-frozen-confirmation"),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name); Text(stringResource(R.string.trialConsentText, plan.inputs.size, plan.endpointMaximum,
                plan.context.provider, if (plan.context.provider == AiSettings.STABILITY) stringResource(R.string.trialStabilityRoute) else plan.context.model,
                if (plan.context.provider == AiSettings.STABILITY) stringResource(R.string.trialProviderDefault) else plan.context.quality, plan.context.revision))
            Text(plan.specification); Text(plan.instruction); Text(plan.fingerprint)
            plan.inputs.forEach { input ->
                Text(input.name); Text(input.key); Text(input.originalHash)
                TrialPreview(id, input.originalHash, backend, "trial-frozen-${input.requestId}", 72, false, true,
                    stringResource(R.string.trialOriginal))
            }
            plan.referenceHashes.forEachIndexed { index, hash ->
                Text(stringResource(R.string.projectStyleReferenceNumber, index + 1)); Text(hash)
                TrialPreview(id, hash, backend, "trial-frozen-reference-$index", 72, false, true,
                    stringResource(R.string.projectStyleReferencePreview))
            }
        } },
        confirmButton = { TextButton(onClick = { backend.submit(id, plan.id) }, enabled = enabled && state.hasKey && p?.context == plan.context,
            modifier = Modifier.testTag("trial-confirm-paid")) { Text(stringResource(R.string.trialConfirmPaid)) } },
        dismissButton = { TextButton(onClick = { backend.cancelPreparation(id) }, enabled = !state.network,
            modifier = Modifier.testTag("trial-decline")) { Text(stringResource(R.string.colorizeCancel)) } })
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
    dark: Boolean, checker: Boolean, variant: String) {
    val label = stringResource(if (generated) R.string.trialGenerated else R.string.trialOriginal)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label)
        TrialPreview(id, hash, backend, "trial-large-$generated-$variant", 128, dark, checker, label)
        TrialPreview(id, hash, backend, "trial-48-$generated-$variant", 48, dark, checker, stringResource(R.string.trialLauncherPreview, label))
    }
}

@Composable
private fun TrialPreview(id: Long, hash: String, backend: ProjectTrialBackend, tag: String, size: Int,
    dark: Boolean, checker: Boolean, label: String) {
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
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { value = null }
        awaitDispose { decoded?.recycle() }
    }
    val background = if (dark) Color(0xff202020) else Color.White
    Box(Modifier.size(size.dp).background(background).drawBehind {
        if (checker) drawAlphaCheckerboard(if (dark) Color(0xff343434) else Color.White,
            if (dark) Color(0xff202020) else Color(0xffdddddd))
    }.testTag(tag)) {
        bitmap?.let { Image(it.asImageBitmap(), label, Modifier.fillMaxSize()) }
    }
}
