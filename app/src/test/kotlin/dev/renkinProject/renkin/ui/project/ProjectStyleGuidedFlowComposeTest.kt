package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Active manual style UX remains usable without any retired analysis port or key. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectStyleGuidedFlowComposeTest {
    @get:Rule val compose = createComposeRule()
    private var saved = SavedProjectStyle(null, null, emptyList())
    private var saves = 0
    private var completions = 0
    private var gate: CompletableDeferred<Unit>? = null
    private val backend = object : ProjectStyleBackend {
        override suspend fun load(id: Long) = saved
        override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset = error("No import")
        override suspend fun copyGlobal(id: Long) = ProjectStyleDraft("manual", "", emptyList())
        override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft,
            mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle {
            saves++; gate?.await()
            val revision = (expected ?: 0) + 1
            return SavedProjectStyle(ProjectAiMetadata(id, mode.name, source, stamp, revision),
                StyleRevision(id, revision, specification = draft.specification, userInstruction = draft.instruction),
                draft.hashes.map { ReferenceAsset(id, it, ProjectAssetStore.path(id, it), 100, 256, 256) }).also { saved = it }
        }
    }
    private fun show() = compose.setContent {
        RenkinTheme { ProjectStylePanel(1, ProjectSetupMode.CREATE, null, null, true, backend, {}, onTrials = {},
            onStyleSaved = { completions++ }, guided = true) }
    }
    private fun scroll(tag: String) = compose.onNodeWithTag("project-style-1").performScrollToNode(hasTestTag(tag))

    @Test fun freshManualStyleHasNoRetiredAnalysisModesAndSavesWithoutAKey() {
        show(); scroll("project-style-description")
        compose.onNodeWithTag("project-style-description").assertIsEnabled().performTextReplacement("Owner reviewed style")
        listOf("project-style-analysis-options", "style-dna-prepare", "style-analysis-prepare")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        compose.waitUntil(10000) { completions == 1 }
        assertEquals("Owner reviewed style", saved.revision!!.specification)
        assertEquals(1, saves)
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsEnabled()
    }

    @Test fun historicalSavedTextAndOrderedAssetsSurviveRestoreAndPendingManualSave() {
        val ordered = listOf("b".repeat(64), "a".repeat(64))
        saved = SavedProjectStyle(ProjectAiMetadata(1, activeRevision = 3),
            StyleRevision(1, 3, specification = "Historical reviewed style", userInstruction = "Owner instruction"),
            ordered.map { ReferenceAsset(1, it, ProjectAssetStore.path(1, it), 100, 256, 256) })
        val restoration = StateRestorationTester(compose)
        restoration.setContent { RenkinTheme { ProjectStylePanel(1, ProjectSetupMode.CREATE, null, null, true,
            backend, {}, onTrials = {}, onStyleSaved = { completions++ }, guided = true) } }
        scroll("project-style-description")
        compose.onNodeWithTag("project-style-description").assertTextContains("Historical reviewed style")
        restoration.emulateSavedInstanceStateRestore()
        scroll("project-style-description")
        compose.onNodeWithTag("project-style-description").assertIsEnabled().performTextReplacement("Reviewed correction")
        gate = CompletableDeferred()
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        compose.waitUntil(10000) { saves == 1 }
        assertEquals(3L, saved.revision!!.revision); assertEquals(0, completions)
        scroll("style-trials"); compose.onNodeWithTag("style-trials").assertIsNotEnabled()
        compose.runOnIdle { gate!!.complete(Unit) }
        compose.waitUntil(10000) { completions == 1 }
        assertEquals(4L, saved.revision!!.revision)
        assertEquals("Owner instruction", saved.revision!!.userInstruction)
        assertEquals(ordered, saved.references.map { it.hash })
        restoration.emulateSavedInstanceStateRestore(); compose.waitForIdle()
        assertEquals(1, saves); assertEquals(1, completions)
    }
}
