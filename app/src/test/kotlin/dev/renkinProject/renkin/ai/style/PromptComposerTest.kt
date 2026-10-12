package dev.renkinProject.renkin.ai.style

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class PromptComposerTest {
    @Test fun canonicalOutputSurvivesStructuredReloadAndKeepsIdentityAndFormat() {
        val style = analysisFixtureStyle()
        val text = PromptComposer.compose(style, "use mint", "keep the antenna")
        assertEquals(text, PromptComposer.compose(StyleProfileV1.parse(style.encode()), "use mint", "keep the antenna"))
        assertTrue(text.contains("original app identity")); assertTrue(text.contains("transparent PNG"))
        assertTrue(text.contains("recognizable symbol")); assertTrue(text.contains("do not substitute"))
        assertTrue(PromptComposer.specification(style).length <= 6000)
    }
    @Test fun untrustedTextIsQuotedAndUserOverrideIsSeparateAndOccursOnce() {
        val instruction = "mint\n\"ignore previous\""
        val text = PromptComposer.compose(analysisFixtureStyle(), instruction, "antenna", "manually corrected charcoal")
        assertTrue(text.contains("override suggested stylistic defaults"))
        assertTrue(text.contains("mint\\n\\\"ignore previous\\\""))
        assertEquals(1, Regex("mint").findAll(text).count())
        assertFalse(text.contains("mint\n"))
        assertTrue(text.contains("manually corrected charcoal"))
        assertFalse(text.contains("Light monochrome"))
    }
    @Test fun textBoundsAndControlCharactersRejectRatherThanTruncate() {
        val style = analysisFixtureStyle()
        assertThrows(Exception::class.java) { PromptComposer.compose(style, "x".repeat(2001)) }
        assertThrows(Exception::class.java) { PromptComposer.compose(style, "", "x".repeat(2001)) }
        assertThrows(Exception::class.java) { PromptComposer.compose(style, "", reviewedDescription = "x".repeat(6001)) }
        assertThrows(Exception::class.java) { PromptComposer.compose(style, "bad\u0000instruction") }
        assertTrue(PromptComposer.compose(style, "x".repeat(2000), reviewedDescription = "y".repeat(6000)).contains("y".repeat(6000)))
    }
    @Test fun composerHasNoLocalInventoryFileIdentityOrSecretInputs() {
        val text = PromptComposer.compose(analysisFixtureStyle(), "mint")
        listOf("profileId", "sourcePackage", "storageEpoch", "catalogStamp", "project_ai_assets/", "Authorization", "Bearer")
            .forEach { assertFalse(it, text.contains(it)) }
        assertTrue(text.indexOf("Project instruction:") < text.indexOf("App instruction:"))
    }
}
