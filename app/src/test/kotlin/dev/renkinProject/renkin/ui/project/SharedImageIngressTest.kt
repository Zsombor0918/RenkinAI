package dev.renkinProject.renkin.ui.project

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedImageIngressTest {
    @Test
    fun editorRequestSurvivesRestorationBeforeShellConsumesIt() {
        val saved = SavedStateHandle()
        val requests = EditorEntryRequests(saved)
        requests.request()
        val restored = EditorEntryRequests(SavedStateHandle(saved.keys().associateWith { saved.get<Long>(it) }))
        assertEquals(1L, restored.version)
        // Observing a restored counter never generates another request or image import.
        repeat(3) { assertEquals(1L, restored.version) }
        restored.request()
        assertEquals(2L, restored.version)
    }

    @Test
    fun activityAndProcessRestorationDoNotImportTheSameImageAgain() {
        val ingress = SharedImageIngress()
        assertTrue(ingress.accept("content://image/one"))
        assertFalse(ingress.accept("content://image/one"))
        val restored = SharedImageIngress(ingress.consumedUri)
        assertFalse(restored.accept("content://image/one"))
        assertTrue(restored.accept("content://image/two"))
    }

    @Test
    fun separatelyDeliveredUserShareMayImportTheSameUri() {
        val ingress = SharedImageIngress("content://image/one")
        ingress.onNewIntent()
        assertTrue(ingress.accept("content://image/one"))
        assertFalse(ingress.accept("content://image/one"))
    }
}
