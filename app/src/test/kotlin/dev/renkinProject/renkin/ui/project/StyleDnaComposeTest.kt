package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33],qualifiers="w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaComposeTest {
    @get:Rule val compose=createComposeRule()
    private val flow=MutableStateFlow(ProjectAnalysisState(hasKey=true))
    private var prepares=0;private var consents=0;private var saves=0
    private var source:String? by mutableStateOf(null)
    private var mode:ProjectSetupMode by mutableStateOf(ProjectSetupMode.CREATE)
    private var dark by mutableStateOf(false)
    private var saved:SavedProjectStyle?=null
    private val draft=ProjectStyleDraft("","separate manual instruction",emptyList())
    private val backend=object:ProjectAnalysisBackend {
        override val supportsDna=true
        override fun state(id:Long)=flow
        override fun prepare(id:Long,mode:ProjectSetupMode,source:String?,setupStamp:String?,expected:Long?,draft:ProjectStyleDraft) {error("Legacy is independent")}
        override fun submit(id:Long,requestId:String){error("Legacy is independent")}
        override fun cancel(id:Long){}
        override suspend fun refresh(id:Long){}
        override suspend fun preview(id:Long,requestId:String,hash:String)=error("Legacy preview")
        override suspend fun use(id:Long,requestId:String,hashes:List<String>,allowStaleSource:Boolean)=error("Legacy use")
        override suspend fun discardCandidate(id:Long,requestId:String){}
        override fun prepareDna(id:Long,mode:ProjectSetupMode,source:String?,setupStamp:String?,expected:Long?,draft:ProjectStyleDraft,
            analysisMode:DnaAnalysisMode,model:String,detail:String) {
            prepares++;val (initial,images)=dnaFixturePlan(analysisMode,if(analysisMode==DnaAnalysisMode.QUICK)12 else 24)
            val p=initial.copy(description=draft.specification,instruction=draft.instruction,draftHash=analysisDraftHash(draft),model=model,detail=detail)
            flow.value=flow.value.copy(dnaPlan=p,samples=ReferenceSamples(null,null,p.samples.map {s->SampledReference(s.id,"fixture",s.hash,images.getValue(s.hash),referenceFeatures(images.getValue(s.hash)))},p.report))
        }
        override fun submitDna(id:Long,planId:String) {
            val p=flow.value.dnaPlan!!;assertEquals(planId,p.id);consents++
            val reports=dnaFixtureReports(p);val images=flow.value.samples!!.samples.associate {it.hash to it.png};val dna=synthesizeDnaLocally(reports,images)
            flow.value=flow.value.copy(dnaPlan=null,dnaRecord=StyleDnaRecord(p,DnaStatus.RESULT_SAVED,p.steps.map {DnaStepState(it.id,"DONE","resp_ui")},reports,
                SavedStyleDna(dna,reports,p.model,p.detail,p.algorithm,dna.recommendedFamily)))
        }
        override suspend fun dnaPreview(id:Long,hash:String)=flow.value.samples!!.samples.single {it.hash==hash}.png
        override suspend fun saveDna(id:Long,planId:String?,grammar:String,selectedFamily:String,hashes:List<String>,expected:Long?,draft:ProjectStyleDraft):SavedProjectStyle {
            saves++;val r=flow.value.dnaRecord!!;val dna=r.result!!.copy(dna=StyleDnaV1.parse(grammar),selectedFamily=selectedFamily)
            return SavedProjectStyle(ProjectAiMetadata(id,activeRevision=1),StyleRevision(id,1,specification=StyleDnaCompiler.specification(dna),
                userInstruction=draft.instruction),emptyList()).also {flow.value=flow.value.copy(savedDna=dna,dnaRecord=r.copy(status=DnaStatus.REVIEWED))}
        }
    }
    @Composable private fun Content(){RenkinTheme(dark){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ProjectStyleDnaPanel(1,mode,source,null,true,backend,null,draft,{saved=it})
    }}}
    private fun prepare(choice:String="STANDARD") {
        compose.onNodeWithTag("style-dna-mode-$choice").performClick()
        compose.onNodeWithTag("style-dna-prepare").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-start").performScrollTo().assertIsEnabled().performClick()
    }
    @Test fun freshPlanIsReadyWhileFailedOrUncertainHistoryStaysUnderDetails() {
        val (historic, _) = dnaFixturePlan(DnaAnalysisMode.QUICK, 2)
        val old = StyleDnaRecord(historic, DnaStatus.FAILED, historic.steps.map { DnaStepState(it.id) }, reason = "UNAUTHORIZED")
        flow.value = flow.value.copy(dnaRecord = old)
        compose.setContent { Content() }
        prepare("QUICK")
        compose.onNodeWithTag("style-dna-decline").performClick()
        compose.onNodeWithTag("style-dna-current-status").performScrollTo()
            .assertTextContains("ready", substring = true)
        compose.onNodeWithText("This analysis did not complete.", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("style-dna-start").performScrollTo().assertIsEnabled()
        assertEquals(old, flow.value.dnaRecord)
        compose.onNodeWithTag("style-dna-advanced").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-history").performScrollTo().assertTextContains("Previous", substring = true)
        compose.onNodeWithTag("style-dna-status").performScrollTo().assertTextContains("FAILED", substring = true)
        compose.runOnIdle { flow.value = flow.value.copy(dnaRecord = old.copy(status = DnaStatus.UNCERTAIN, reason = "INTERRUPTED_NO_REPLAY")) }
        compose.onNodeWithTag("style-dna-status").performScrollTo().assertTextContains("UNCERTAIN", substring = true)
        compose.onNodeWithTag("style-dna-current-status").performScrollTo().assertTextContains("ready", substring = true)
        compose.onNodeWithTag("style-dna-start").performScrollTo().assertIsEnabled()
        assertEquals(0, consents); assertEquals(0, saves)
    }

    @Test fun changedOptionsExplainWhyFrozenPlanNeedsFreshPreparation() {
        compose.setContent { Content() }; prepare("QUICK")
        compose.onNodeWithTag("style-dna-decline").performClick()
        compose.onNodeWithTag("style-dna-advanced").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-detail-high").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-start").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("style-dna-current-status").performScrollTo()
            .assertTextContains("changed", substring = true)
        assertEquals(0, consents); assertEquals(0, saves)
    }
    @Test fun historyFailurePreservesRequestGateAndDisplaysOnlyBoundedDiagnostic() {
        flow.value = ProjectAnalysisState(error = "history_unreadable",
            dnaLocalDiagnostic = DnaLocalDiagnostic(DnaLocalStage.HISTORY_READ, DnaLocalCategory.INVALID_DATA))
        compose.setContent { Content() }
        compose.onNodeWithTag("style-dna-error").performScrollTo()
            .assertTextContains("history could not be verified", substring = true)
            .assertTextContains("do not clear app data", substring = true)
        compose.onNodeWithTag("style-dna-local-diagnostic").performScrollTo()
            .assertTextContains("history_read/invalid_data", substring = true)
        compose.onNodeWithTag("style-dna-start").assertDoesNotExist()
        compose.onNodeWithTag("style-dna-prepare").performScrollTo().assertIsEnabled()
        assertEquals(0, consents); assertEquals(0, prepares); assertEquals(0, saves)
    }
    @Test fun standardConsentListsFiveStepsAndSavingRequiresSeparateHumanReview() {
        compose.setContent {Content()};assertEquals(0,consents)
        compose.onNodeWithTag("style-dna-mode-DEEP").assertIsNotEnabled()
        prepare();assertEquals(1,prepares);assertEquals(0,consents)
        compose.onNodeWithText("maximum 5 charged endpoint submissions",substring=true).assertExists()
        compose.onNodeWithTag("style-dna-decline").performClick();assertEquals(0,consents)
        compose.onNodeWithTag("style-dna-start").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-confirm-paid").performClick();assertEquals(1,consents);assertEquals(0,saves)
        compose.onNodeWithTag("style-dna-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("style-dna-reviewed").performScrollTo().performClick()
        compose.onNodeWithTag("style-dna-save").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10000){saves==1};assertEquals(1,consents);assertEquals("separate manual instruction",saved!!.revision!!.userInstruction)
    }
    @Test fun sourceChangeWhileConfirmationOpenDisablesOldFrozenConsent() {
        compose.setContent {Content()};prepare("QUICK")
        compose.runOnIdle {mode=ProjectSetupMode.EXTEND;source="different.pack"}
        compose.onNodeWithTag("style-dna-confirm-paid").assertIsNotEnabled();assertEquals(0,consents)
    }
    @Test fun missingExtendStampExplainsDisabledScanAndManualPathStaysAvailable() {
        mode = ProjectSetupMode.EXTEND; source = "fixture.pack"
        compose.setContent {Content()}
        compose.onNodeWithTag("style-dna-prepare").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("style-dna-source-required").assertTextContains("Return to Project/source", substring=true)
        compose.onNodeWithTag("style-dna-manual").performScrollTo().assertIsEnabled()
        assertEquals(0,prepares);assertEquals(0,consents);assertEquals(0,saves)
    }
    @Test fun restorationAndDarkNarrowUiNeverSubmitAndDeepRemainsDisabled() {
        val restoration=StateRestorationTester(compose);restoration.setContent {Content()};prepare("QUICK")
        restoration.emulateSavedInstanceStateRestore();assertEquals(0,consents)
        compose.runOnIdle {dark=true};compose.onNodeWithTag("style-dna-confirm-paid").assertIsEnabled();assertEquals(0,consents)
        compose.onNodeWithTag("style-dna-decline").performClick();compose.onNodeWithTag("style-dna-mode-DEEP").performScrollTo().assertIsNotEnabled()
    }
}
