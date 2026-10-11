package dev.renkinProject.renkin.apk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/** One small software draw, no full-size previews in state/cache; discard after validation. */
internal fun drawCoveragePreview(source: Drawable): Boolean {
    if (source is BitmapDrawable && source.bitmap.allocationByteCount > 16 * 1024 * 1024) return false
    val drawable = source.constantState?.newDrawable()?.mutate() ?: source
    val oldBounds = Rect(drawable.bounds)
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    return try {
        drawable.setBounds(0, 0, 96, 96)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(96 * 96)
        bitmap.getPixels(pixels, 0, 96, 0, 0, 96, 96)
        pixels.any { (it ushr 24) != 0 }
    } finally {
        drawable.bounds = oldBounds
        bitmap.recycle()
    }
}
