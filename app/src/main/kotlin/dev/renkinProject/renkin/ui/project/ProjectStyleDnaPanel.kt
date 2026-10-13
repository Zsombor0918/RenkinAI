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
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.SavedProjectStyle
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import kotlinx.coroutines.*

/** Optional new analysis path. Save is an explicit local reviewed revision, never a paid action. */
@Composable
internal fun ProjectStyleDnaPanel(id: Long, mode: ProjectSetupMode, source: String?, stamp: String?, enabled: Boolean,
    backend: ProjectAnalysisBackend, expected: Long?, draft: ProjectStyleDraft, onSaved: (SavedProjectStyle)->Unit) {
    val state by backend.state(id).collectAsState()
    var selectedMode by rememberSaveable(id) { mutableStateOf("LEGACY") }
    var model by rememberSaveable(id) { mutableStateOf(DEFAULT_ANALYSIS_MODEL) }
    var detail by rememberSaveable(id) { mutableStateOf("low") }
    var confirmation by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var review by rememberSaveable(id) { mutableStateOf(false) }
    var edit by rememberSaveable(id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val record=state.dnaRecord
    val suggestion=record?.takeIf { it.status==DnaStatus.RESULT_SAVED }?.result ?: state.savedDna
    var grammar by rememberSaveable(id,record?.plan?.id,state.savedDna?.encode()) { mutableStateOf(suggestion?.dna?.encode().orEmpty()) }
    var family by rememberSaveable(id,record?.plan?.id,state.savedDna?.encode()) { mutableStateOf(suggestion?.selectedFamily.orEmpty()) }
    var refs by rememberSaveable(id,record?.plan?.id) { mutableStateOf(ArrayList(record?.plan?.samples?.take(5)?.map { it.hash } ?: draft.hashes)) }
    Column(Modifier.fillMaxWidth().testTag("style-dna-panel"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.styleDnaTitle))
        for(choice in listOf("LEGACY","QUICK","STANDARD","DEEP")) TextButton(onClick={selectedMode=choice;review=false},
            enabled=enabled && !state.busy && !saving && choice!="DEEP",modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-mode-$choice")) {
            Text(if(choice=="LEGACY") stringResource(R.string.styleDnaLegacy) else if(choice=="DEEP") stringResource(R.string.styleDnaDeep) else "$choice ${if(selectedMode==choice)"✓" else ""}") }
        if(selectedMode!="LEGACY") {
            Text(stringResource(if(selectedMode=="STANDARD")R.string.styleDnaStandard else R.string.styleDnaQuick))
            for(value in ANALYSIS_MODELS) TextButton(onClick={model=value},enabled=enabled && !state.busy,modifier=Modifier.heightIn(min=48.dp)) {Text("$value ${if(model==value)"✓" else ""}")}
            Row {for(value in listOf("low","high")) TextButton(onClick={detail=value},enabled=enabled && !state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-detail-$value")) {Text("detail=$value ${if(detail==value)"✓" else ""}")}}
            Text(stringResource(R.string.styleDnaPrice))
            OutlinedButton(onClick={review=false;backend.prepareDna(id,mode,source,stamp,expected,draft,DnaAnalysisMode.valueOf(selectedMode),model,detail)},
                enabled=enabled && !state.busy && !saving,modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-prepare")) {Text(stringResource(R.string.styleDnaPrepare))}
            state.samples?.let { samples->
                Text(stringResource(R.string.styleDnaCounts,samples.report.examined,samples.report.valid,samples.report.duplicates,samples.report.unsupported,samples.samples.size),Modifier.testTag("style-dna-counts"))
                samples.report.reasonCounts?.forEach { (reason,count)->Text("${reason.name}: $count",Modifier.testTag("style-dna-reason-${reason.name}")) }
                if(samples.report.candidates!=null && samples.report.examined<samples.report.candidates) Text(stringResource(R.string.styleDnaTimeBound))
                samples.samples.forEachIndexed { n,s->Row {DnaThumbnail(s.hash,{s.png});Text("s%02d · %s".format(n+1,s.hash.take(8))) } }
            }
            val plan=state.dnaPlan
            val matches=plan!=null && plan.mode.name==selectedMode && plan.model==model && plan.detail==detail &&
                plan.context.matchesRoute(mode,source) && plan.draftHash==analysisDraftHash(draft)
            OutlinedButton(onClick={confirmation=plan?.id},enabled=enabled && matches && state.hasKey && !state.busy,
                modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-start")) {Text(stringResource(R.string.styleDnaAnalyze))}
            if(record!=null) {
                val uploaded=record.plan.steps.filter { step->record.steps.single { it.id==step.id }.status in setOf("SUBMITTED","DONE") }.sumOf { it.samples.size }
                Text(stringResource(R.string.styleDnaProgress,record.reports.size,record.plan.samples.size,uploaded,record.submitted,record.plan.endpointMaximum),Modifier.testTag("style-dna-progress"))
                Text("${record.status.name}${record.reason?.let { ": $it" }.orEmpty()}",Modifier.testTag("style-dna-status"))
                if(record.status in setOf(DnaStatus.UNCERTAIN,DnaStatus.FAILED)) Text(stringResource(R.string.styleDnaNoReplay))
                var reportsOpen by rememberSaveable(id,record.plan.id) {mutableStateOf(false)}
                TextButton(onClick={reportsOpen=!reportsOpen},modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-reports")) {Text(stringResource(R.string.styleDnaReports))}
                if(reportsOpen) record.reports.forEach { r->Column(Modifier.testTag("style-dna-report-${r.sampleId}")) {
                    Text("${r.sampleId} · ${r.familyVote} · ${r.colorPolicy}")
                    DnaThumbnail(r.sampleHash,{backend.dnaPreview(id,r.sampleHash)})
                    r.traits.forEach { (trait,observation)->Text("$trait · ${observation.provenance}: ${observation.value}") }
                    Text(stringResource(R.string.styleDnaDoNotCopy))
                } }
                if(record.status==DnaStatus.RESULT_SAVED) TextButton(onClick={scope.launch {try{backend.discardDna(id,record.plan.id)}catch(_:Exception){error=true}}},
                    enabled=enabled && !saving && !state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-discard")) {Text(stringResource(R.string.styleAnalysisDiscardCandidate))}
            }
        }
        if(suggestion!=null) {
            Text(stringResource(R.string.styleDnaReviewHelp))
            suggestion.dna.families.forEach { f->TextButton(onClick={family=f.id;review=false},enabled=enabled && !saving && !state.busy,
                modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-family-${f.id}")) {Text("${f.id} · ${f.observedCount}/${suggestion.reports.size} · ${f.colorPolicy} ${if(family==f.id)"✓" else ""}")} }
            TextButton(onClick={edit=!edit},modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-edit")) {Text(stringResource(R.string.styleDnaEdit))}
            if(edit) OutlinedTextField(grammar,{if(it.length<=65536){grammar=it;review=false}},enabled=enabled && !saving && !state.busy,
                label={Text(stringResource(R.string.styleDnaGrammar))},modifier=Modifier.fillMaxWidth().testTag("style-dna-grammar"),minLines=4,maxLines=12)
            val candidate=runCatching {suggestion.copy(dna=StyleDnaV1.parse(grammar),selectedFamily=family).also {it.encode();StyleDnaCompiler.specification(it)}}.getOrNull()
            candidate?.dna?.families?.firstOrNull {it.id==family}?.let { f->
                Text("Tile: ${f.tile} · depth: ${f.depth}")
                f.drawing.forEach { (key,rule)->Text("$key · ${rule.provenance}: ${rule.value}") }
                candidate.dna.uncertainty.forEach {Text(it)}
            }
            if(candidate==null) Text(stringResource(R.string.styleDnaInvalid))
            if(record?.status==DnaStatus.RESULT_SAVED) record.plan.samples.forEach { sample->Row {
                Checkbox(sample.hash in refs,{checked->review=false;refs=ArrayList(if(checked){if(refs.size<5)refs+sample.hash else refs}else refs.filter{it!=sample.hash})},
                    enabled=enabled && !saving,modifier=Modifier.size(48.dp).testTag("style-dna-ref-${sample.id}"));Text(sample.id) } }
            Row {Checkbox(review,{review=it},enabled=enabled && !saving && !state.busy,modifier=Modifier.size(48.dp).testTag("style-dna-reviewed"));Text(stringResource(R.string.styleDnaReviewed))}
            OutlinedButton(onClick={if(candidate!=null && !saving){saving=true;scope.launch {
                try {onSaved(backend.saveDna(id,record?.takeIf {it.status==DnaStatus.RESULT_SAVED}?.plan?.id,grammar,family,
                    if(record?.status==DnaStatus.RESULT_SAVED)refs else draft.hashes,expected,draft));error=false;review=false}
                catch(cancel:CancellationException){throw cancel}catch(_:Exception){error=true}finally{saving=false}
            }}},enabled=enabled && !saving && !state.busy && review && candidate!=null &&
                (record?.status!=DnaStatus.RESULT_SAVED || state.validity==AnalysisValidity.CURRENT),
                modifier=Modifier.heightIn(min=48.dp).testTag("style-dna-save")) {Text(stringResource(R.string.styleDnaSave))}
        }
        if(error || state.error!=null) Text(stringResource(R.string.styleDnaLocalError),Modifier.testTag("style-dna-error"))
    }
    val plan=state.dnaPlan
    if(confirmation!=null) RenkinAlertDialog(onDismissRequest={confirmation=null},title={Text(stringResource(R.string.styleAnalysisConsentTitle))},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.styleDnaConsent,plan?.samples?.size ?: 0,plan?.model.orEmpty(),plan?.detail.orEmpty(),plan?.endpointMaximum ?: 0))
            plan?.steps?.forEachIndexed { index,step->Text("${index+1}. ${if(step.synthesis)"Synthesis (text only)" else "${step.samples.size} individual labeled images"} · ${step.id.take(8)}") }
            Text(stringResource(R.string.styleDnaPrice));Text(stringResource(R.string.styleDnaNoReplay))
        }},confirmButton={TextButton(onClick={val selected=confirmation;confirmation=null
            if(plan!=null && plan.context.matchesRoute(mode,source) && selected==plan.id && plan.mode.name==selectedMode && plan.model==model && plan.detail==detail && plan.draftHash==analysisDraftHash(draft)) backend.submitDna(id,plan.id)},
            enabled=enabled && !state.busy && state.hasKey && plan?.context?.matchesRoute(mode,source)==true && plan?.id==confirmation && plan?.mode?.name==selectedMode && plan?.model==model && plan?.detail==detail && plan?.draftHash==analysisDraftHash(draft),
            modifier=Modifier.testTag("style-dna-confirm-paid")) {Text(stringResource(R.string.styleDnaConfirm))}},
        dismissButton={TextButton(onClick={confirmation=null},modifier=Modifier.testTag("style-dna-decline")){Text(stringResource(R.string.colorizeCancel))}})
}
@Composable
private fun DnaThumbnail(hash: String,read: suspend ()->ByteArray) {
    val latest by rememberUpdatedState(read)
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,hash) {
        var bitmap:android.graphics.Bitmap?=null
        try {withContext(Dispatchers.IO) {val png=latest();bitmap=android.graphics.BitmapFactory.decodeByteArray(png,0,png.size,
            android.graphics.BitmapFactory.Options().apply {inSampleSize=4})};value=bitmap?.asImageBitmap();awaitCancellation()}
        catch(cancel:CancellationException){throw cancel}catch(_:Exception){value=null}finally{bitmap?.recycle()}
    }
    image?.let {Image(it,"Style evidence only",Modifier.size(48.dp))}
}
