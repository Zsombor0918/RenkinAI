package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.RenkinAlertDialog
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w450dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectShellComposeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var activity: ComponentActivity
    private var home by mutableStateOf(ProjectHomeState(
        profiles = listOf(Profile(1, "Default"), Profile(2, "Night")),
        availability = ProjectHomeAvailability.READY
    ))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var ingress by mutableLongStateOf(0)
    private var attention by mutableStateOf(false)
    private var childOpen by mutableStateOf(false)
    private var searchOpen by mutableStateOf(false)
    private val opens = mutableListOf<Long>()
    private var creates = 0
    private var saves = 0
    private var cancelled = 0
    private var exits = 0
    private var legacyExits = 0

    private fun open(id: Long) {
        opens += id
        if (home.hasUnsavedChanges && id != home.activeProfileId) {
            operation = ProjectSessionState.ConfirmSave(ProjectRequest.Open(id, home.activeProfileId))
        } else {
            home = home.copy(activeProfileId = id)
            operation = ProjectSessionState.Opened(id)
        }
    }

    @Composable
    private fun Content() {
        activity = LocalContext.current as ComponentActivity
        RenkinTheme(false) {
            ProjectShell(home, operation, ingress, attention,
                onOpen = ::open,
                onCreate = { _, _, _ ->
                    creates++
                    operation = ProjectSessionState.Loading
                    home = home.copy(busy = true)
                    true
                },
                onSave = {
                    saves++
                    val target = (operation as ProjectSessionState.ConfirmSave).request as ProjectRequest.Open
                    home = home.copy(activeProfileId = target.profileId, hasUnsavedChanges = false)
                    operation = ProjectSessionState.Opened(target.profileId)
                },
                onCancel = { cancelled++; if (operation is ProjectSessionState.ConfirmSave) operation = ProjectSessionState.Idle },
                onAcknowledge = { operation = ProjectSessionState.Idle },
                onRetry = {}, onExit = { exits++ }
            ) { goHome ->
                // Use the same handler as MainColumn, before search/dialog children.
                ProjectEditorBackHandler(goHome) { legacyExits++ }
                var draft by rememberSaveable { mutableStateOf("") }
                Column(Modifier.testTag("test-editor")) {
                    Text("Editor profile ${home.activeProfileId}")
                    OutlinedTextField(draft, { draft = it }, modifier = Modifier.testTag("editor-draft"))
                    TextButton(onClick = goHome, modifier = Modifier.testTag("editor-home")) { Text("Projects") }
                }
                BackHandler(enabled = searchOpen) { searchOpen = false }
                if (childOpen || attention) {
                    RenkinAlertDialog(
                        onDismissRequest = { childOpen = false; attention = false },
                        text = { Text("Pending watch/editor dialog", Modifier.testTag("child-dialog")) }
                    )
                }
            }
        }
    }

    private fun start() { compose.setContent { Content() } }
    private fun back() { compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() } }

    @Test
    fun activeEditorHomeRoundTripPreservesDraftAndNeverUsesLegacyExit() {
        start()
        compose.onNodeWithTag("project-classic").performClick()
        compose.onNodeWithTag("editor-draft").performTextInput("Unsaved preview")
        back()
        compose.onNodeWithTag("project-home").assertIsDisplayed()
        compose.onNodeWithTag("project-open-1").performClick()
        compose.onNodeWithTag("editor-draft").assertExists()
        assertEquals("Unsaved preview", compose.onNodeWithTag("editor-draft").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        assertEquals(listOf(1L, 1L), opens)
        assertEquals(0, saves)
        assertEquals(0, legacyExits)
        assertEquals(1L, home.activeProfileId)
    }

    @Test
    fun searchChildBackRunsBeforeRootThenOneBackReturnsHome() {
        start()
        compose.onNodeWithTag("project-classic").performClick()
        compose.runOnIdle { searchOpen = true }
        back()
        assertFalse(searchOpen)
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        back()
        compose.onNodeWithTag("project-home").assertIsDisplayed()
        assertEquals(0, exits)
        assertEquals(0, legacyExits)
    }

    @Test
    @Suppress("DEPRECATION")
    fun childDialogBackDismissesItWithoutNavigatingHome() {
        start()
        compose.onNodeWithTag("project-classic").performClick()
        compose.runOnIdle { childOpen = true }
        compose.onNodeWithTag("child-dialog").assertIsDisplayed()
        compose.runOnIdle { ShadowDialog.getLatestDialog().onBackPressed() }
        compose.onNodeWithTag("child-dialog").assertDoesNotExist()
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        back()
        compose.onNodeWithTag("project-home").assertIsDisplayed()
    }

    @Test
    fun savePromptCancelKeepsOriginThenSaveOpensTheChosenStableId() {
        home = home.copy(hasUnsavedChanges = true)
        start()
        compose.onNodeWithTag("project-open-2").performClick()
        compose.onNodeWithTag("project-save-cancel").performClick()
        compose.onNodeWithTag("project-home").assertIsDisplayed()
        assertEquals(1L, home.activeProfileId)
        assertEquals(0, saves)
        compose.onNodeWithTag("project-open-2").performClick()
        compose.onNodeWithTag("project-save-continue").performClick()
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        assertEquals(2L, home.activeProfileId)
        assertEquals(1, saves)
        assertTrue(cancelled > 0)
    }

    @Test
    fun confirmedCreationRecompositionAndRestorationDoNotResubmitOrOpenEarly() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag("project-create").performClick()
        compose.onNodeWithTag("profile-name").performTextInput("Night")
        compose.onNodeWithTag("profile-pack-label").performTextInput("Night pack")
        compose.onNodeWithTag("profile-create-confirm").performClick()
        compose.onNodeWithTag("project-create").assertIsNotEnabled()
        compose.onNodeWithTag("test-editor").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(1, creates)
        compose.runOnIdle {
            home = home.copy(activeProfileId = 3, busy = false, profiles = home.profiles + Profile(3, "Night"))
            operation = ProjectSessionState.Opened(3)
        }
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        assertEquals(1, creates)
    }

    @Test
    fun extendExplanationActuallyOpensTheClassicActiveEditor() {
        start()
        compose.onNodeWithTag("project-extend").performClick()
        compose.onNodeWithTag("project-extend-editor").performClick()
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        assertEquals(listOf(1L), opens)
        assertEquals(0, creates)
    }

    @Test
    fun externalImageEntryAndPendingWatchTakePriorityOverHomeAndCreateForms() {
        start()
        compose.onNodeWithTag("project-create").performClick()
        compose.runOnIdle { ingress++ }
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        compose.onNodeWithTag("editor-home").performClick()
        compose.runOnIdle { home = home.copy(activeProfileId = 2); attention = true }
        compose.onNodeWithTag("child-dialog").assertIsDisplayed()
        assertEquals(2L, home.activeProfileId)
        assertEquals(0, creates)
    }

    @Test
    fun editorRouteRestoresWithoutAnotherProfileOperationOrApiRequest() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag("project-classic").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        assertEquals(listOf(1L), opens)
        assertEquals(0, creates)
        assertEquals(0, saves)
    }

    @Test
    fun editorDoesNotDisappearDuringClassicRefresh() {
        start()
        compose.onNodeWithTag("project-classic").performClick()
        compose.runOnIdle { home = home.copy(busy = true) }
        compose.onNodeWithTag("test-editor").assertIsDisplayed()
        compose.onNodeWithTag("project-editor-loading").assertDoesNotExist()
    }

    @Test
    fun homeBackExitsCleanlyButWarnsBeforeLosingSessionEdits() {
        start()
        back()
        assertEquals(1, exits)
        compose.runOnIdle { home = home.copy(hasUnsavedChanges = true) }
        back()
        compose.onNodeWithTag("project-exit-discard").assertIsDisplayed()
        assertEquals(1, exits)
        compose.onNodeWithTag("project-exit-discard").performClick()
        assertEquals(2, exits)
        assertEquals(0, saves)
    }

    @Test
    fun standaloneEditorHandlerStillCallsTheLegacyBackPath() {
        compose.setContent {
            activity = LocalContext.current as ComponentActivity
            ProjectEditorBackHandler(null) { legacyExits++ }
            Text("Classic editor")
        }
        back()
        assertEquals(1, legacyExits)
    }
}
