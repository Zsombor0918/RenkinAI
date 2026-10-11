package dev.renkinProject.renkin.ui.project

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.ui.ProfileDetailsDialog
import dev.renkinProject.renkin.ui.theme.RenkinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Actual shared form, native SDK 33 and normal idling. Match Robolectric's default
 * window width: custom larger widths loop in WRAP_CONTENT dialog measurement
 * (robolectric/robolectric#8460). This does not constrain production device sizes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33], qualifiers = "w320dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileDetailsDialogComposeTest {
    @get:Rule val compose = createComposeRule()
    private var open by mutableStateOf(true)
    private val confirmations = mutableListOf<Triple<String, String, String>>()
    private var dismissals = 0

    @androidx.compose.runtime.Composable
    private fun Content() {
        RenkinTheme(false) {
            if (open) ProfileDetailsDialog(
                title = "Create new icon pack", confirmLabel = "Create",
                onConfirm = { name, description, label ->
                    confirmations += Triple(name, description, label)
                    open = false
                },
                onDismiss = { dismissals++; open = false }
            )
        }
    }

    @Test fun requiredFieldsEnableConfirmationAndReturnTrimmedPayload() {
        compose.setContent { Content() }
        compose.onNodeWithTag("profile-create-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("profile-name").performTextInput("  Night  ")
        compose.onNodeWithTag("profile-create-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("profile-description").performTextInput("Evening icons")
        compose.onNodeWithTag("profile-pack-label").performTextInput("  Night pack  ")
        compose.onNodeWithTag("profile-create-confirm").assertIsEnabled().performClick()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        assertEquals(listOf(Triple("Night", "Evening icons", "Night pack")), confirmations)
        assertEquals(0, dismissals)
    }

    @Test fun restoredDraftCancelsWithoutConfirmingOrAllocating() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag("profile-name").performTextInput("Night")
        compose.onNodeWithTag("profile-description").performTextInput("Evening icons")
        compose.onNodeWithTag("profile-pack-label").performTextInput("Night pack")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("profile-name").assertTextContains("Night")
        compose.onNodeWithTag("profile-description").assertTextContains("Evening icons")
        compose.onNodeWithTag("profile-pack-label").assertTextContains("Night pack")
        val dismiss = RuntimeEnvironment.getApplication().getString(R.string.dismiss)
        compose.onNodeWithText(dismiss).performClick()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        assertEquals(0, confirmations.size)
        assertEquals(1, dismissals)
    }

    @Test fun cappedFieldsRemainUsableAndSubmitOnce() {
        compose.setContent { Content() }
        compose.onNodeWithTag("profile-name").performTextInput("n".repeat(40))
        compose.onNodeWithTag("profile-description").performTextInput("d".repeat(80))
        compose.onNodeWithTag("profile-pack-label").performTextInput("p".repeat(50))
        compose.onNodeWithTag("profile-name").assertTextContains("n".repeat(24))
        compose.onNodeWithTag("profile-description").assertTextContains("d".repeat(60))
        compose.onNodeWithTag("profile-pack-label").assertTextContains("p".repeat(30))
        compose.onNodeWithTag("profile-create-confirm").performClick()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        assertEquals(listOf(Triple("n".repeat(24), "d".repeat(60), "p".repeat(30))), confirmations)
    }
}
