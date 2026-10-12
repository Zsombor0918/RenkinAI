package dev.renkinProject.renkin.ui.project

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.core.app.ActivityOptionsCompat
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.ai.IconGenerationGateway
import dev.renkinProject.renkin.ai.IconGenerationOutcome
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import kotlinx.coroutines.CompletableDeferred
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
class ProjectStyleComposeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var activity: ComponentActivity
    private var home by mutableStateOf(ProjectHomeState(listOf(Profile(1, "Same"), Profile(2, "Same")),
        availability = ProjectHomeAvailability.READY))
    private var operation: ProjectSessionState by mutableStateOf(ProjectSessionState.Idle)
    private var ingress by mutableLongStateOf(0)
    private var dark by mutableStateOf(false)
    private var launched = -1
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) { launched = requestCode }
    }
    private val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry: ActivityResultRegistry get() = registry }
    private var calls = 0
    private val fakeGateway = IconGenerationGateway { request -> calls++; IconGenerationOutcome.InvalidInput(request.requestId) }
    private val records = mutableMapOf<Long, SavedProjectStyle>()
    private var saves = 0
    private var imports = 0
    private val discardedHashes = mutableListOf<String>()
    private var failSave = false
    private var gate: CompletableDeferred<Unit>? = null
    private var data by mutableStateOf(ProjectSetupData(targets = listOf(CoverageTarget("pkg/Main", "App"))))
    private val backend = object : ProjectStyleBackend {
        override suspend fun load(id: Long) = records[id] ?: SavedProjectStyle(null, null, emptyList())
        override suspend fun importReference(id: Long, uri: Uri): ReferenceAsset {
            imports++
            val hash = imports.toString().repeat(64)
            return ReferenceAsset(id, hash, ProjectAssetStore.path(id, hash), 100, 32, 32)
        }
        override suspend fun copyGlobal(id: Long) = ProjectStyleDraft("copied global", "", emptyList())
        override suspend fun discard(id: Long, hashes: List<String>) { discardedHashes.addAll(hashes) }
        override suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft,
            mode: ProjectSetupMode, source: String?, stamp: String?): SavedProjectStyle {
            saves++
            gate?.await()
            check(!failSave)
            check(id == home.activeProfileId)
            val previous = records[id]
            check(previous?.revision?.revision == expected)
            val revision = StyleRevision(id, (expected ?: 0) + 1, specification = draft.specification.trim(), userInstruction = draft.instruction.trim())
            return SavedProjectStyle(ProjectAiMetadata(id, mode.name, source, stamp, revision.revision), revision,
                draft.hashes.map { ReferenceAsset(id, it, ProjectAssetStore.path(id, it), 100, 32, 32) }).also { records[id] = it }
        }
    }
    @Composable private fun Content() {
        activity = LocalContext.current as ComponentActivity
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            RenkinTheme(dark) {
                ProjectShell(home, operation, ingress,
                    onOpen = { id -> home = home.copy(activeProfileId = id); operation = ProjectSessionState.Opened(id) },
                    onCreate = { _, _, _ -> false }, onSave = {}, onCancel = {},
                    onAcknowledge = { operation = ProjectSessionState.Idle }, onRetry = {}, onExit = {},
                    setupData = { _, _, _, _ -> data }, readSetupContext = { records[it]?.metadata },
                    stylePanel = { id, mode, source, stamp, back -> ProjectStylePanel(id, mode, source, stamp,
                        id == home.activeProfileId, backend, back) }
                ) { back ->
                    ProjectEditorBackHandler(back) {}
                    Column { Text("classic editor", Modifier.testTag("style-classic-editor"))
                        TextButton(onClick = back, Modifier.testTag("style-editor-home")) { Text("Projects") } }
                }
            }
        }
    }
    private fun open(id: Long = 1) {
        compose.onNodeWithTag("project-home").performScrollToNode(hasTestTag("project-setup-$id"))
        compose.onNodeWithTag("project-setup-$id").performClick()
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
    }
    private fun scroll(tag: String, id: Long = 1) { compose.onNodeWithTag("project-style-$id").performScrollToNode(hasTestTag(tag)) }
    private fun back() = compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
    private fun addReference() {
        scroll("project-style-add-reference")
        compose.onNodeWithTag("project-style-add-reference").performClick()
        compose.runOnIdle { registry.dispatchResult(launched, android.app.Activity.RESULT_OK, Intent().setData(Uri.parse("content://test/ref.png"))) }
    }

    @Test fun manualSaveAndReopenKeepProjectStyleReferencesAndNeverCallGateway() {
        compose.setContent { Content() }; open()
        compose.onNodeWithTag("project-style-status").assertTextEquals("No AI style saved for this project")
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("outline")
        addReference(); addReference()
        scroll("project-style-up-1"); compose.onNodeWithTag("project-style-up-1").performClick()
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        assertEquals(1, saves)
        assertEquals(listOf("2".repeat(64), "1".repeat(64)), records[1]!!.references.map { it.hash })
        back(); compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("outline")
        assertEquals(0, calls)
    }

    @Test fun dirtyBackKeepsDraftOnCancelAndDiscardsOnlyAfterExplicitChoice() {
        compose.setContent { Content() }; open()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("draft")
        back(); compose.onNodeWithTag("project-style-keep").performClick()
        compose.onNodeWithTag("project-style-description").assertTextContains("draft")
        back(); compose.onNodeWithTag("project-style-discard").performClick()
        compose.onNodeWithTag("setup-project-style").performScrollTo().performClick()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        assertEquals(0, saves)
    }

    @Test fun explicitRemovalReleasesItsDraftImportAndRestoredDiscardKeepsRemovedHashesCovered() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }; open()
        addReference()
        scroll("project-style-remove-0"); compose.onNodeWithTag("project-style-remove-0").performClick()
        compose.onNodeWithTag("project-style-add-reference").assertIsEnabled()
        assertEquals(listOf("1".repeat(64)), discardedHashes)
        addReference()
        restoration.emulateSavedInstanceStateRestore()
        back(); compose.onNodeWithTag("project-style-discard").performClick()
        compose.onNodeWithTag("setup-project-style").assertExists()
        assertTrue("2".repeat(64) in discardedHashes)
        assertEquals(0, saves); assertEquals(0, calls)
    }

    @Test fun restorationDoesNotSubmitDraftAndExternalEntryPreservesItForItsOwner() {
        val restoration = StateRestorationTester(compose); restoration.setContent { Content() }; open()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("restored draft")
        restoration.emulateSavedInstanceStateRestore()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("restored draft")
        compose.runOnIdle { ingress++ }
        compose.onNodeWithTag("style-classic-editor").assertIsDisplayed()
        compose.onNodeWithTag("style-editor-home").performClick(); open()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("restored draft")
        assertEquals(0, saves); assertEquals(0, calls)
    }

    @Test fun saveWaitsForCompletionAndDoubleClickCannotSubmitAgain() {
        gate = CompletableDeferred()
        compose.setContent { Content() }; open()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("pending")
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        compose.onNodeWithTag("project-style-save").assertIsNotEnabled().performClick()
        assertEquals(1, saves); assertNull(records[1])
        compose.runOnIdle { gate!!.complete(Unit) }
        compose.onNodeWithTag("project-style-save").assertIsEnabled()
        assertEquals(1L, records[1]!!.revision!!.revision)
    }

    @Test fun failureKeepsDraftAndTwoProfilesDoNotShareStyles() {
        failSave = true
        compose.setContent { Content() }; open()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").performTextInput("first project")
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        scroll("project-style-error"); compose.onNodeWithTag("project-style-error").assertIsDisplayed()
        assertNull(records[1])
        compose.runOnIdle { failSave = false }
        scroll("project-style-save"); compose.onNodeWithTag("project-style-save").performClick()
        back(); back(); open(2)
        scroll("project-style-description", 2)
        compose.onNodeWithTag("project-style-description").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        assertEquals("first project", records[1]!!.revision!!.specification)
        assertNull(records[2]); assertEquals(0, calls)
    }

    @Test fun extendPanelUsesSavedSourceContextAndExplicitGlobalCopyWithoutGenerating() {
        records[1] = SavedProjectStyle(ProjectAiMetadata(1, "EXTEND", "pack.source", "stamp"), null, emptyList())
        compose.setContent { Content() }; open()
        compose.onNodeWithText("Source inspiration: pack.source. Optional analysis uses separately confirmed reference images.").assertIsDisplayed()
        scroll("project-style-copy-global"); compose.onNodeWithTag("project-style-copy-global").performClick()
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertTextContains("copied global")
        assertEquals(0, saves); assertEquals(0, calls)
        compose.runOnIdle { dark = true }
        scroll("project-style-description"); compose.onNodeWithTag("project-style-description").assertIsEnabled()
    }
}
