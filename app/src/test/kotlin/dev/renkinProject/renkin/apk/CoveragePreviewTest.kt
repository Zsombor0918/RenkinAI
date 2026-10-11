package dev.renkinProject.renkin.apk

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.util.Xml
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoveragePreviewTest {
    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources
    @Test fun bitmapDrawActuallyProducesPixelsAtBoundedSize() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try { assertTrue(drawCoveragePreview(BitmapDrawable(resources, source))); assertFalse(source.isRecycled) }
        finally { source.recycle() }
    }
    @Test fun vectorDrawAndAdaptiveLayersActuallyProducePixels() {
        val parser = Xml.newPullParser().apply { setInput(StringReader("""
            <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
              <path android:fillColor="#ff0000" android:pathData="M0,0 L24,0 L24,24 L0,24 Z"/>
            </vector>
        """.trimIndent())) }
        val vector = Drawable.createFromXml(resources, parser)
        assertTrue(vector is VectorDrawable)
        assertTrue(drawCoveragePreview(vector))
        assertTrue(drawCoveragePreview(AdaptiveIconDrawable(ColorDrawable(Color.BLACK), ColorDrawable(Color.GREEN))))
    }
    @Test fun nonNullButTransparentDrawableDoesNotCountAsRenderedCoverage() {
        assertFalse(drawCoveragePreview(ColorDrawable(Color.TRANSPARENT)))
    }
    @Test fun faultyDrawRestoresBoundsAndPropagatesToThePerEntryErrorBoundary() {
        val drawable = object : Drawable() {
            override fun draw(canvas: Canvas) { throw IllegalStateException("fixture draw") }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSLUCENT
        }.apply { setBounds(7, 8, 9, 10) }
        val before = android.graphics.Rect(drawable.bounds)
        try { drawCoveragePreview(drawable); fail("Draw failure cannot count as success") }
        catch (_: IllegalStateException) { assertEquals(before, drawable.bounds) }
    }
    @Test fun oversizedBitmapIsRejectedWithoutCreatingAnotherLargePreview() {
        val bitmap = Bitmap.createBitmap(2100, 2100, Bitmap.Config.ARGB_8888)
        try { assertFalse(drawCoveragePreview(BitmapDrawable(resources, bitmap))) }
        finally { bitmap.recycle() }
    }
}
