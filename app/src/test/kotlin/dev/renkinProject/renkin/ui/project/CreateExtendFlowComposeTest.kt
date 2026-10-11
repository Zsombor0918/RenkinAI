package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.IconPack
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreateExtendFlowComposeTest {
    @get:Rule val compose = createComposeRule()
    private var home by mutableStateOf(ProjectHomeState(profiles = listOf(Profile(1, "Default"), Profile(2, "Same name"), Profile(7, "Same name")), availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var ingress by mutableLongStateOf(0)
    private var attention by mutableStateOf(false)
    private var dark by mutableStateOf(false)
    private var data by mutableStateOf(ProjectSetupData(targets = listOf(
        CoverageTarget("app/Main", "Original"), CoverageTarget("app/Other", "Manual", LocalIconState.CUSTOM_OR_MANUAL),
        CoverageTarget("locked/Main", "Locked", locked = true))))
    private val opens = mutableListOf<Long>()
    private var creates = 0
    private var saves = 0
    private var rescans = 0
    private var reloads = 0
    private fun pack(id: String) = IconPack(id, "Same long external pack name that does not identify the source", 5, "5", 0)
    private fun open(id: Long) {
        opens += id
        if (home.hasUnsavedChanges && id != home.activeProfileId) operation = ProjectSessionState.ConfirmSave(ProjectRequest.Open(id, home.activeProfileId))
        else operation = ProjectSessionState.Opened(id).also { home = home.copy(activeProfileId = id) }
    }
    @Composable private fun Content() {
        RenkinTheme(dark) {
            ProjectShell(home, operation, ingress, attention,
                onOpen = ::open,
                onCreate = { _, _, _ -> creates++; home = home.copy(busy = true); operation = ProjectSessionState.Loading; true },
                onSave = {
                    saves++
                    val request = (operation as ProjectSessionState.ConfirmSave).request as ProjectRequest.Open
                    home = home.copy(activeProfileId = request.profileId, hasUnsavedChanges = false)
                    operation = ProjectSessionState.Opened(request.profileId)
                }, onCancel = { if (operation is ProjectSessionState.ConfirmSave) operation = ProjectSessionState.Idle },
                onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                setupData = { _, _, _, _ -> data.copy(onRescan = { rescans++ }, onReloadPacks = { reloads++ }) }
            ) { goHome ->
                ProjectEditorBackHandler(goHome) {}
                var iconDraft by rememberSaveable { mutableStateOf("unchanged icon state") }
                Column {
                    Text(iconDraft, Modifier.testTag("flow-editor"))
                    TextButton(onClick = { iconDraft = "edited icon state"; home = home.copy(hasUnsavedChanges = true) }, Modifier.testTag("flow-edit")) { Text("Edit icon") }
                    TextButton(onClick = goHome, Modifier.testTag("flow-home")) { Text("Projects") }
                }
            }
        }
    }
    private fun start() = compose.setContent { Content() }
    private fun scroll(tag: String) {
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag(tag))
    }
    private fun setup(id: Long) {
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-$id"))
        compose.onNodeWithTag("project-setup-$id").performClick()
    }
    private fun extend(profile: Long = 1) {
        compose.onNodeWithTag("project-extend").performClick()
        compose.onNodeWithTag("setup-source-pack.two").performScrollTo().performClick()
        compose.onNodeWithTag("setup-profile-$profile").performScrollTo().performClick()
    }
    private fun readyCoverage(profile: Long = 1): CoverageUiState {
        val targets = data.targets
        val index = CoverageIndex(CoverageStamp("pack.two", 5, 40, 1, false), CoverageIndexState.READY,
            mapOf("app/Main" to "good", "app/Other" to "broken"))
        return CoverageUiState.Ready(CoverageSnapshot(profile, index, targets.map {
            CoverageEntry(it, when (it.key) { "app/Main" -> SourceCoverage.RESOLVABLE; "app/Other" -> SourceCoverage.DECLARED_BROKEN; else -> SourceCoverage.UNMAPPED },
                index.mappings[it.key], when (it.key) { "app/Main" -> CoverageReason.DRAWN; "app/Other" -> CoverageReason.DRAW_FAILED; else -> CoverageReason.NO_MAPPING })
        }))
    }
    @Test fun createUsesAllLauncherActivitiesWithoutSourceAndKeepsSelectionAndEditorState() {
        start(); setup(1)
        scroll("setup-target-app/Other")
        compose.onNodeWithTag("setup-target-app/Other").assertIsDisplayed().assertIsEnabled()
        scroll("setup-select-all"); compose.onNodeWithTag("setup-select-all").performClick()
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 2 / 3 known launcher entries")
        scroll("setup-search"); compose.onNodeWithTag("setup-search").performTextInput("manual")
        scroll("setup-target-app/Other"); compose.onNodeWithTag("setup-target-app/Other").assertIsDisplayed()
        compose.onNodeWithTag("setup-target-app/Main").assertDoesNotExist()
        scroll("setup-editor"); compose.onNodeWithTag("setup-editor").performClick()
        compose.onNodeWithTag("flow-edit").performClick(); compose.onNodeWithTag("flow-home").performClick()
        setup(1); scroll("setup-search")
        compose.onNodeWithTag("setup-search").assertTextContains("manual")
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 2 / 3 known launcher entries")
        scroll("setup-editor"); compose.onNodeWithTag("setup-editor").performClick()
        compose.onNodeWithTag("flow-editor").assertTextEquals("edited icon state")
        assertEquals(0, saves); assertEquals(0, creates); assertTrue(home.hasUnsavedChanges)
    }
    @Test fun explicitCreateConfirmLoadsOnceThenShowsTargetsBeforeEditorAndRestorationDoesNotReplay() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }
        compose.onNodeWithTag("project-create").performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Prepared")
        compose.onNodeWithTag("profile-pack-label").performTextInput("Prepared pack")
        compose.onNodeWithTag("profile-create-confirm").performClick()
        compose.onNodeWithTag("project-targets").assertDoesNotExist()
        compose.onNodeWithTag("flow-editor").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore(); assertEquals(1, creates)
        compose.runOnIdle { home = home.copy(busy = false, activeProfileId = 8, profiles = home.profiles + Profile(8, "Prepared")); operation = ProjectSessionState.Opened(8) }
        compose.onNodeWithTag("project-targets").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore(); assertEquals(1, creates)
        scroll("setup-editor"); compose.onNodeWithTag("setup-editor").performClick()
        compose.onNodeWithTag("flow-editor").assertIsDisplayed(); assertEquals(8L, home.activeProfileId)
    }
    @Test fun extendChoosesPackageThenExplicitProfileAndPreservesSaveCancel() {
        data = data.copy(packs = listOf(pack("pack.one"), pack("pack.two")), coverage = readyCoverage(7))
        home = home.copy(hasUnsavedChanges = true)
        start(); compose.onNodeWithTag("project-extend").performClick()
        assertTrue(opens.isEmpty())
        compose.onNodeWithTag("setup-source-pack.two").performScrollTo().performClick()
        assertTrue(opens.isEmpty()); assertEquals(1L, home.activeProfileId)
        compose.onNodeWithTag("setup-profile-7").performScrollTo().performClick()
        compose.onNodeWithTag("project-save-cancel").performClick()
        assertEquals(0, saves); assertEquals(1L, home.activeProfileId)
        compose.onNodeWithTag("project-targets").assertDoesNotExist()
        compose.onNodeWithTag("setup-profile-7").performScrollTo().performClick()
        compose.onNodeWithTag("project-save-continue").performClick()
        assertEquals(1, saves); assertEquals(7L, home.activeProfileId)
        scroll("coverage-counts"); compose.onNodeWithTag("coverage-counts").assertTextContains("drawn: 1")
        scroll("setup-select-all"); compose.onNodeWithTag("setup-select-all").performClick()
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 0 / 3 known launcher entries")
        compose.onNodeWithTag("setup-filter").performClick()
        scroll("setup-target-app/Main"); compose.onNodeWithTag("setup-target-app/Main").assertIsEnabled().performClick()
        scroll("setup-clear"); compose.onNodeWithTag("setup-clear").performClick()
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 0 / 3 known launcher entries")
    }
    @Test fun extendLoadingFailureAndUnknownOfferRetryWithoutClaimingMissingCoverage() {
        data = data.copy(packs = listOf(pack("pack.two")))
        start(); extend()
        scroll("setup-loading"); compose.onNodeWithTag("setup-loading").assertIsDisplayed()
        compose.onNodeWithTag("coverage-counts").assertDoesNotExist()
        compose.runOnIdle { data = data.copy(coverage = CoverageUiState.Failed) }
        scroll("setup-rescan"); compose.onNodeWithTag("setup-rescan").performClick(); assertEquals(1, rescans)
        compose.runOnIdle { data = data.copy(coverage = readyCoverage().let { state ->
            val snap = (state as CoverageUiState.Ready).snapshot
            CoverageUiState.Ready(snap.copy(entries = snap.entries.map { it.copy(source = SourceCoverage.UNKNOWN, reason = CoverageReason.INDEX_UNPROVEN) }))
        }) }
        scroll("coverage-warning"); compose.onNodeWithTag("coverage-warning").assertIsDisplayed()
        scroll("setup-select-all"); compose.onNodeWithTag("setup-select-all").performClick()
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 0 / 3 known launcher entries")
    }
    @Test fun noPacksAndCancelledFormNeverCreateOrChooseDefaultAndEditorStillWorks() {
        start(); compose.onNodeWithTag("project-create").performClick()
        compose.onNodeWithText("Cancel").performClick(); assertEquals(0, creates)
        compose.onNodeWithTag("project-extend").performClick()
        compose.onNodeWithTag("setup-no-packs").assertExists()
        compose.onNodeWithTag("setup-reload-packs").performClick(); assertEquals(1, reloads)
        compose.onNodeWithTag("project-extend-editor").performClick()
        compose.onNodeWithTag("flow-editor").assertIsDisplayed(); assertEquals(listOf(1L), opens)
    }
    @Test fun incomingImageAndWatchLeaveSetupAndNeverConsumePendingModal() {
        data = data.copy(packs = listOf(pack("pack.two")), coverage = readyCoverage())
        start(); extend()
        compose.runOnIdle { ingress++ }
        compose.onNodeWithTag("flow-editor").assertIsDisplayed(); compose.onNodeWithTag("project-targets").assertDoesNotExist()
        compose.onNodeWithTag("flow-home").performClick(); setup(1)
        compose.runOnIdle { attention = true }
        compose.onNodeWithTag("flow-editor").assertIsDisplayed(); assertTrue(attention)
        assertEquals(0, creates); assertEquals(0, saves)
    }
    @Test fun sourceVersionChangeInvalidatesSelectionAndCollisionRowsAreDisabled() {
        data = data.copy(packs = listOf(pack("pack.two")), targets = listOf(CoverageTarget("app/Main", "Personal", ambiguousUser = true), CoverageTarget("app/Other", "Missing")))
        data = data.copy(coverage = readyCoverage())
        start(); extend()
        scroll("setup-select-all"); compose.onNodeWithTag("setup-select-all").performClick()
        compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 1 / 2 known launcher entries")
        compose.onNodeWithTag("setup-filter").performClick()
        scroll("setup-target-app/Main"); compose.onNodeWithTag("setup-target-app/Main").assertIsNotEnabled()
        compose.runOnIdle {
            val snap = (data.coverage as CoverageUiState.Ready).snapshot
            data = data.copy(coverage = CoverageUiState.Ready(snap.copy(index = snap.index.copy(stamp = snap.index.stamp.copy(versionCode = 6)))))
        }
        scroll("setup-selection-count"); compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 0 / 2 known launcher entries")
    }
    @Test @Config(qualifiers = "w1000dp-h1400dp")
    fun wideDarkAndLightSetupKeepsSemanticTargetsAndMinimumTouchSize() {
        start(); setup(1)
        scroll("setup-select-all")
        assertTrue(compose.onNodeWithTag("setup-select-all").getUnclippedBoundsInRoot().height >= 48.dp)
        scroll("setup-target-app/Other"); compose.onNodeWithTag("setup-target-app/Other").assertIsEnabled().assertIsOff()
        compose.runOnIdle { dark = true }
        compose.onNodeWithTag("setup-target-app/Other").assertIsDisplayed().performClick().assertIsOn()
    }
}
