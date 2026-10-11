package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.*
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
class ProjectSelectionComposeTest {
    @get:Rule val compose = createComposeRule()
    private var metadata by mutableStateOf<ProjectAiMetadata?>(null)
    private var targets by mutableStateOf(listOf(CoverageTarget("pkg/Main", "App"), CoverageTarget("locked/Main", "Locked", locked = true)))
    private var mode by mutableStateOf(ProjectSetupMode.CREATE)
    private var coverage: CoverageUiState by mutableStateOf(CoverageUiState.Loading())
    private var saves = 0
    private var styleEntries = 0
    @Composable private fun Content() {
        RenkinTheme(false) {
            TargetSelection(mode, 1, "Project", if (mode == ProjectSetupMode.CREATE) null else "pack.source",
                ProjectSetupData(targets = targets, coverage = coverage, savedMetadata = metadata,
                    onSaveSelection = { version, selection ->
                        check(version == (metadata?.version ?: 0))
                        saves++
                        ProjectAiMetadata(1, selection.mode, selection.sourcePackage, selection.sourceStamp,
                            selectionJson = selection.encode(), version = version + 1).also { metadata = it }
                    }), true, {}, {}, {}, { styleEntries++ })
        }
    }
    private fun scroll(tag: String) { compose.onNodeWithTag("project-targets").performScrollToNode(hasTestTag(tag)) }
    private fun index(version: Long, state: CoverageIndexState = CoverageIndexState.READY) = CoverageIndex(
        CoverageStamp("pack.source", version, 1, 1, false), state)
    private fun snapshot(index: CoverageIndex) = CoverageUiState.Ready(CoverageSnapshot(1, index, targets.map {
        CoverageEntry(it, if (index.state == CoverageIndexState.READY) SourceCoverage.UNMAPPED else SourceCoverage.UNKNOWN, null, CoverageReason.NO_MAPPING)
    }))

    @Test fun explicitSavePersistsExactKeysAndRestorationNeverResubmits() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }
        scroll("setup-select-all"); compose.onNodeWithTag("setup-select-all").performClick()
        scroll("setup-save-selection"); compose.onNodeWithTag("setup-save-selection").performClick()
        assertEquals(listOf("pkg/Main"), ProjectSelection.decode(metadata!!.selectionJson)!!.componentKeys)
        assertEquals(1, saves)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(1, saves)
        scroll("setup-saved-selection"); compose.onNodeWithTag("setup-saved-selection").assertExists()
    }

    @Test fun staleCreateCatalogRequiresExplicitReviewAndCannotRestoreAmbiguousTargets() {
        val saved = ProjectSelection("CREATE", null, null, projectCatalogStamp(targets), listOf("pkg/Main"))
        metadata = ProjectAiMetadata(1, selectionJson = saved.encode())
        targets = targets.map { if (it.key == "pkg/Main") it.copy(ambiguousUser = true) else it }
        compose.setContent { Content() }
        scroll("setup-saved-selection"); compose.onNodeWithTag("setup-saved-selection").assertTextContains("changed")
        assertEquals(0, saves)
        scroll("setup-revalidate-selection"); compose.onNodeWithTag("setup-revalidate-selection").performClick()
        scroll("setup-selection-count"); compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 0 / 2 known launcher entries")
    }

    @Test fun extendSourceUpdateRequiresReviewAndUnknownIndexCannotBecomeSavedTargets() {
        mode = ProjectSetupMode.EXTEND
        val original = index(1)
        metadata = ProjectAiMetadata(1, "EXTEND", "pack.source", projectSourceStamp(original),
            selectionJson = ProjectSelection("EXTEND", "pack.source", projectSourceStamp(original), projectCatalogStamp(targets), listOf("pkg/Main")).encode())
        coverage = snapshot(index(2))
        compose.setContent { Content() }
        scroll("setup-saved-selection"); compose.onNodeWithTag("setup-saved-selection").assertTextContains("changed")
        scroll("setup-revalidate-selection"); compose.onNodeWithTag("setup-revalidate-selection").performClick()
        scroll("setup-selection-count"); compose.onNodeWithTag("setup-selection-count").assertTextEquals("Selected: 1 / 2 known launcher entries")
        compose.runOnIdle { coverage = snapshot(index(2, CoverageIndexState.UNKNOWN)) }
        scroll("setup-save-selection"); compose.onNodeWithTag("setup-save-selection").assertIsNotEnabled()
        assertEquals(0, saves)
    }

    @Test fun styleEntryRemainsAvailableForCreateAndUnavailableExtendSource() {
        compose.setContent { Content() }
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        compose.runOnIdle { mode = ProjectSetupMode.EXTEND; coverage = CoverageUiState.Failed }
        compose.onNodeWithTag("setup-project-style").performScrollTo().assertIsEnabled().performClick()
        assertEquals(2, styleEntries)
        assertEquals(0, saves)
    }
}
