package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.apk.CoverageTarget
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.data.project.ProjectAiMetadata
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectGuidedNavigationComposeTest {
    @get:Rule val compose = createComposeRule()
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Project")),
        availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var metadata by mutableStateOf<ProjectAiMetadata?>(null)
    private var saves = 0
    private var styleSaves = 0
    private var automaticSuggestions = false
    private var saveRelease: CompletableDeferred<Unit>? = null
    private val saveEntered = CompletableDeferred<Unit>()
    private val saveCancelled = CompletableDeferred<Unit>()
    @Composable private fun Content() {
        RenkinTheme {
            ProjectShell(home, operation, onOpen = { operation = ProjectSessionState.Opened(it) },
                onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {},
                onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                setupData = { _, _, _, _ -> ProjectSetupData(targets = if (automaticSuggestions)
                    (0..9).map { CoverageTarget("app$it/Main", "App $it") } + CoverageTarget("protected/Main", "Protected", locked = true)
                    else listOf(CoverageTarget("app/Main", "App")), automaticTrialSuggestions = automaticSuggestions,
                    savedMetadata = metadata, onSaveSelection = { version, selection ->
                        saves++
                        saveEntered.complete(Unit)
                        try { saveRelease?.await() } catch (cancel: CancellationException) { saveCancelled.complete(Unit); throw cancel }
                        checkNotNull(metadata).copy(selectionJson = selection.encode(), version = version + 1)
                            .also { metadata = it }
                    }) },
                stylePanel = { _, _, _, _, back, next ->
                    TextButton(onClick = back, modifier = Modifier.testTag("fixture-style-back")) { Text("Back") }
                    TextButton(onClick = {
                        styleSaves++; metadata = ProjectAiMetadata(1, activeRevision = 1); next()
                    }, modifier = Modifier.testTag("fixture-save-style")) { Text("Save style") }
                },
                trialPanel = { _, _, _, _, _, _ -> Text("Trial review", Modifier.testTag("fixture-trials")) },
                guidedWorkflow = true) {
                Text("Classic editor")
            }
        }
    }
    @Test fun backFromSavedStyleReturnsToSourceSummaryInsteadOfBouncingToTargets() {
        compose.setContent { Content() }
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1"))
        compose.onNodeWithTag("project-setup-1").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-project-style").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        compose.onNodeWithTag("fixture-save-style").performClick()
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-project-style"))
        compose.onNodeWithTag("setup-project-style").performClick()
        compose.onNodeWithTag("fixture-style-back").performClick()
        compose.onNodeWithTag("workflow-step").assertTextContains("1 of 4", substring = true)
        assertEquals(1L, metadata!!.activeRevision)
    }
    @Test fun initialRandomSuggestionIsSavedOnceAndSurvivesRestorationWithoutGenerating() {
        automaticSuggestions = true
        saveRelease = CompletableDeferred()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1"))
        compose.onNodeWithTag("project-setup-1").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-project-style").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        compose.onNodeWithTag("fixture-save-style").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("project-targets").fetchSemanticsNodes().isNotEmpty() && saveEntered.isCompleted
        }
        compose.waitForIdle()
        org.junit.Assert.assertFalse("A busy-state recomposition must not cancel durable suggestion Save", saveCancelled.isCompleted)
        compose.runOnIdle { saveRelease!!.complete(Unit) }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-selection-saved").fetchSemanticsNodes().isNotEmpty() }
        var chosen = emptyList<String>()
        compose.runOnIdle {
            assertEquals(1, saves)
            chosen = dev.renkinProject.renkin.data.project.ProjectSelection.decode(metadata!!.selectionJson)!!.componentKeys
        }
        assertEquals(3, chosen.size); org.junit.Assert.assertFalse("protected/Main" in chosen)
        compose.onNodeWithTag("fixture-trials").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(chosen, dev.renkinProject.renkin.data.project.ProjectSelection.decode(metadata!!.selectionJson)!!.componentKeys)
        }
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-select-all"))
        compose.onNodeWithTag("setup-select-all").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(2, saves)
            org.junit.Assert.assertNotEquals(chosen, dev.renkinProject.renkin.data.project.ProjectSelection.decode(metadata!!.selectionJson)!!.componentKeys)
        }
        compose.onNodeWithTag("fixture-trials").assertDoesNotExist()
    }
    @Test fun guidedStyleCompletionReturnsToTargetsAndTheirSaveContinuesWithoutRoundTrip() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-1"))
        compose.onNodeWithTag("project-setup-1").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("setup-project-style").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("workflow-step").assertTextContains("1 of 4", substring = true)
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        compose.onNodeWithTag("fixture-save-style").performClick()
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("workflow-step").assertTextContains("3 of 4", substring = true)
        assertEquals(1, styleSaves)
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-target-app/Main"))
        compose.onNodeWithTag("setup-target-app/Main").performClick()
        compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag("setup-save-selection"))
        compose.onNodeWithTag("setup-save-selection").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("fixture-trials").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, saves)
        assertEquals(listOf("app/Main"), dev.renkinProject.renkin.data.project.ProjectSelection.decode(metadata!!.selectionJson)!!.componentKeys)
    }
}
