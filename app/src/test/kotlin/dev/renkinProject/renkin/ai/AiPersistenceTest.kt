package dev.renkinProject.renkin.ai

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class AiPersistenceTest {
    @Test fun promptStoreSeparatesComponentsAndProfilesAndResetInheritsGlobal() = runBlocking {
        val store = AiPromptStore(RuntimeEnvironment.getApplication())
        store.save("7:example/AppA", "Keep a leaf")
        store.save("7:example/AppB", "Use rounded waves")
        store.save("8:example/AppA", "Use a star")
        val reopened = AiPromptStore(RuntimeEnvironment.getApplication())
        assertEquals("Keep a leaf", reopened.load("7:example/AppA"))
        assertEquals("Use rounded waves", reopened.load("7:example/AppB"))
        assertEquals("Use a star", reopened.load("8:example/AppA"))
        reopened.save("7:example/AppA", "")
        assertEquals("", reopened.load("7:example/AppA"))
        assertEquals("Use a star", reopened.load("8:example/AppA"))
    }
    @Test fun journalSurvivesNewStoreAndClearDoesNotDeleteOtherProfiles() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val store = AiBatchStore(context)
        val first = AiBatchSession(7, AiSettings(), listOf(AiBatchItem("key", "App", "leaf", AiBatchStatus.GENERATED, "img_1_1.png")))
        val second = first.copy(profileId = 8)
        store.save(first); store.save(second)
        assertEquals(first, AiBatchStore(context).load(7))
        store.clear(7)
        assertNull(store.load(7)); assertEquals(second, store.load(8))
    }
    @Test fun cacheFingerprintChangesWithSourceStyleModelQualityInstructionAndReferenceContent() {
        val context = RuntimeEnvironment.getApplication()
        val service = AiIconGenerationService(context, AiCredentialStore(context), OpenAiImageClient(), StabilityImageClient())
        val source = "original".toByteArray()
        val config = AiSettings()
        val hash = service.requestFingerprint(config, source)
        assertEquals(hash, service.requestFingerprint(config, source))
        assertNotEquals(hash, service.requestFingerprint(config, "new-icon".toByteArray()))
        assertNotEquals(hash, service.requestFingerprint(config.copy(style = "different style"), source))
        assertNotEquals(hash, service.requestFingerprint(config.copy(model = AiSettings.MODELS.last()), source))
        assertNotEquals(hash, service.requestFingerprint(config.copy(quality = "high"), source))
        assertNotEquals(hash, service.requestFingerprint(config.copy(appInstruction = "leaf"), source))
        val reference = java.io.File(dev.renkinProject.renkin.data.UploadedImageStore.directory(context), "img_91_91.png")
        reference.writeBytes("one".toByteArray())
        val withRef = config.copy(referenceNames = listOf(reference.name))
        val before = service.requestFingerprint(withRef, source)
        reference.writeBytes("two".toByteArray())
        assertNotEquals(before, service.requestFingerprint(withRef, source))
    }
}
