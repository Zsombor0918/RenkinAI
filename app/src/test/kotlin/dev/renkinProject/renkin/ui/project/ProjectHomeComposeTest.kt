package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.AdaptiveLayoutInfo
import dev.renkinProject.renkin.ui.LocalAdaptiveLayoutInfo
import dev.renkinProject.renkin.ui.VerticalHingeBounds
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w450dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectHomeComposeTest {
    @get:Rule val compose = createComposeRule()

    private fun ready(vararg profiles: Profile) = ProjectHomeState(
        profiles = profiles.toList(), availability = ProjectHomeAvailability.READY
    )

    private fun show(state: ProjectHomeState, dark: Boolean = false, onOpen: (Long) -> Unit = {}) {
        compose.setContent {
            RenkinTheme(dark) {
                ProjectHome(state, onOpen, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun defaultProfileAndHonestEqualEntryActionsAreAvailable() {
        show(ready(Profile(1, "Default")))
        compose.onNodeWithTag("project-default-only").assertIsDisplayed()
        compose.onNodeWithTag("project-open-1").assertIsDisplayed()
        compose.onNodeWithTag("project-create").assertIsDisplayed()
        compose.onNodeWithTag("project-extend").assertIsDisplayed()
        compose.onNodeWithTag("project-classic").assertIsDisplayed()
        compose.onNodeWithText("Automatic AI", substring = true).assertExists()
    }

    @Test
    fun emptyProfilesAreDescribedWithoutInventingProjects() {
        show(ready())
        compose.onNodeWithTag("project-empty").assertIsDisplayed()
        compose.onNodeWithTag("project-1").assertDoesNotExist()
    }

    @Test
    fun duplicateNamesKeepDistinctIdsAndAccessibleContinueActions() {
        var opened = 0L
        show(ready(Profile(1, "Default"), Profile(7, "Night"), Profile(8, "Night")), onOpen = { opened = it })
        compose.onNodeWithTag("project-open-7").assertContentDescriptionEquals("Continue editing Night, project 7")
        compose.onNodeWithTag("project-open-8").assertContentDescriptionEquals("Continue editing Night, project 8")
        compose.onNodeWithTag("project-open-8").performClick()
        assertEquals(8L, opened)
    }

    @Test
    fun longImportedNameAndDescriptionRemainReadableInDarkTheme() {
        val name = "A very long imported profile name that is not a unique identifier"
        show(ready(Profile(1, name, "Optional project description", hasUnbuiltChanges = true)), dark = true)
        compose.onNodeWithText(name).assertExists()
        compose.onNodeWithText("Optional project description").assertIsDisplayed()
        compose.onNodeWithTag("project-open-1").assertContentDescriptionEquals("Continue editing $name, project 1")
    }

    @Test
    fun initializationAndUnloadedCatalogueDisableAllProfileActions() {
        show(ProjectHomeState())
        compose.onNodeWithTag("project-loading").assertIsDisplayed()
        compose.onNodeWithTag("project-create").assertIsNotEnabled()
        compose.onNodeWithTag("project-extend").assertIsNotEnabled()
        compose.onNodeWithTag("project-classic").assertIsNotEnabled()
    }

    @Test
    fun profileSwitchShowsBusyStateAndDisablesReentry() {
        show(ready(Profile(1, "Default")).copy(busy = true, hasUnsavedChanges = true))
        compose.onNodeWithTag("project-loading").assertIsDisplayed()
        compose.onNodeWithTag("project-open-1").assertIsNotEnabled()
        compose.onNodeWithTag("project-create").assertIsNotEnabled()
        compose.onNodeWithText("Unsaved edits in this session").assertExists()
    }

    @Test
    fun unavailableProfileListOffersAnActualRetry() {
        var retried = false
        compose.setContent {
            ProjectHome(ProjectHomeState(availability = ProjectHomeAvailability.UNAVAILABLE),
                {}, {}, {}, {}, { retried = true })
        }
        compose.onNodeWithTag("project-error").assertIsDisplayed()
        compose.onNodeWithTag("project-retry").performClick()
        assertTrue(retried)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1000dp")
    fun wideLayoutUsesEqualWidthActionsWithAtLeast48dpTargets() {
        show(ready(Profile(1, "Default")))
        val create = compose.onNodeWithTag("project-create").getUnclippedBoundsInRoot()
        val extend = compose.onNodeWithTag("project-extend").getUnclippedBoundsInRoot()
        assertEquals(create.width.value, extend.width.value, 1f)
        assertTrue(extend.left >= create.right)
        assertTrue(create.height >= 48.dp && extend.height >= 48.dp)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1400dp")
    fun separatingFoldKeepsControlsInTheLargerPane() {
        compose.setContent {
            CompositionLocalProvider(LocalAdaptiveLayoutInfo provides
                AdaptiveLayoutInfo(1000.dp, VerticalHingeBounds(400.dp, 430.dp))) {
                RenkinTheme(false) { ProjectHome(ready(Profile(1, "Default")), {}, {}, {}, {}, {}) }
            }
        }
        val bounds = compose.onNodeWithTag("project-create").getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 430.dp)
        assertTrue(bounds.right <= 1000.dp)
    }
}
