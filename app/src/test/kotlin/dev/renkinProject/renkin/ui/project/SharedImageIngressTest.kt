package dev.renkinProject.renkin.ui.project

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedImageIngressTest {
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
