package dev.renkinProject.renkin.ai.style

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.LevelListDrawable
import android.graphics.drawable.StateListDrawable
import dev.renkinProject.renkin.data.project.inspectPng
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.roundToInt

internal const val REFERENCE_SIDE = 256
internal const val MAX_ANALYSIS_PNG = 384 * 1024
internal const val LEGACY_REFERENCE_ALGORITHM = "static256-features64-v1"
internal const val PREVIOUS_REFERENCE_ALGORITHM = "static256-oklab64-families-v2"
internal const val REFERENCE_ALGORITHM = "static256-oklab64-diagnostics-v3"

/** Adaptive/vector/bitmap draw is independent of profile rendering/modifiers.
 * Unstable animated/state/layer resources are deliberately not certified static. */
internal fun renderStaticReference(source: Drawable): ByteArray? = renderStaticReference(source) {
    Bitmap.createBitmap(REFERENCE_SIDE, REFERENCE_SIDE, Bitmap.Config.ARGB_8888)
}

/** Allocation seam verifies owned render-bitmap cleanup on both success and draw failure. */
internal fun renderStaticReference(source: Drawable, allocate: () -> Bitmap): ByteArray? = renderReference(source, allocate, false)
internal fun renderMontageReference(source: Drawable): ByteArray? = renderReference(source,
    { Bitmap.createBitmap(REFERENCE_SIDE, REFERENCE_SIDE, Bitmap.Config.ARGB_8888) }, true)

private fun renderReference(source: Drawable, allocate: () -> Bitmap, fit: Boolean): ByteArray? {
    if (source is Animatable || source is StateListDrawable || source is LevelListDrawable || source is LayerDrawable) return null
    if (source is BitmapDrawable && (source.bitmap.isRecycled || source.bitmap.allocationByteCount > 16 * 1024 * 1024)) return null
    val drawable = source.constantState?.newDrawable()?.mutate() ?: source
    val old = Rect(drawable.bounds)
    val bitmap = allocate()
    return try {
        require(bitmap.width == REFERENCE_SIDE && bitmap.height == REFERENCE_SIDE && bitmap.config != Bitmap.Config.HARDWARE)
        if (fit && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            val scale = REFERENCE_SIDE.toDouble() / max(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val w = (drawable.intrinsicWidth * scale).roundToInt().coerceAtLeast(1)
            val h = (drawable.intrinsicHeight * scale).roundToInt().coerceAtLeast(1)
            val x = (REFERENCE_SIDE - w) / 2; val y = (REFERENCE_SIDE - h) / 2
            drawable.setBounds(x, y, x + w, y + h)
        } else drawable.setBounds(0, 0, REFERENCE_SIDE, REFERENCE_SIDE)
        drawable.draw(Canvas(bitmap))
        png(bitmap).also { inspectPng(it, MAX_ANALYSIS_PNG, requireVisible = false) }
    } finally { drawable.bounds = old; bitmap.recycle() }
}

internal fun thumbnailReference(bytes: ByteArray): ByteArray {
    val (width, height) = inspectPng(bytes)
    val options = BitmapFactory.Options().apply { inSampleSize = Integer.highestOneBit((max(width, height) / REFERENCE_SIDE).coerceAtLeast(1)) }
    val decoded = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
    try {
        val scale = min(1.0, REFERENCE_SIDE.toDouble() / max(decoded.width, decoded.height))
        val scaled = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1), true)
        return try { png(scaled).also { validateAnalysisPng(it) } } finally { if (scaled !== decoded) scaled.recycle() }
    } finally { decoded.recycle() }
}

internal fun validateAnalysisPng(bytes: ByteArray) {
    val (w, h) = inspectPng(bytes, MAX_ANALYSIS_PNG)
    require(w in 1..REFERENCE_SIDE && h in 1..REFERENCE_SIDE) { "Analysis thumbnails exceed the dimension limit." }
}

private fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use {
    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.toByteArray()
}

internal data class ReferenceFeatures(
    val occupancy: Double, val padding: Double, val brightness: Double, val saturation: Double,
    val contrast: Double, val edgeDensity: Double, val outlineEstimate: Double,
    val palette: List<String>, val colors: List<Double>, val alphaHash: Long, val luminanceHash: Long,
    val paletteWeights: List<Double> = emptyList(), val width: Double = 0.0, val height: Double = 0.0,
    val softAlpha: Double = 0.0
) {
    /** Geometry family sketch deliberately excludes logo hue and theme brightness. */
    fun geometryDistance(other: ReferenceFeatures): Double =
        .35 * abs(occupancy - other.occupancy) + .15 * abs(padding - other.padding) +
        .25 * abs(outlineEstimate - other.outlineEstimate) + .15 * abs(edgeDensity - other.edgeDensity) +
        .10 * abs(contrast - other.contrast)
    fun distance(other: ReferenceFeatures): Double =
        .18 * abs(occupancy - other.occupancy) + .10 * abs(padding - other.padding) +
        .14 * abs(brightness - other.brightness) + .14 * abs(saturation - other.saturation) +
        .12 * abs(contrast - other.contrast) + .16 * abs(edgeDensity - other.edgeDensity) +
        .08 * abs(outlineEstimate - other.outlineEstimate) + .08 * colors.zip(other.colors).sumOf { abs(it.first - it.second) } / 2
    fun near(other: ReferenceFeatures): Boolean =
        java.lang.Long.bitCount(alphaHash xor other.alphaHash) <= 2 &&
        java.lang.Long.bitCount(luminanceHash xor other.luminanceHash) <= 2 && distance(other) < .025 &&
        palette.isNotEmpty() && other.palette.isNotEmpty() &&
        palette.all { a -> other.palette.any { b -> colorDistance(android.graphics.Color.parseColor(a), android.graphics.Color.parseColor(b)) <= .02 } } &&
        other.palette.all { b -> palette.any { a -> colorDistance(android.graphics.Color.parseColor(a), android.graphics.Color.parseColor(b)) <= .02 } }
}

/** Measured pixels, not recognition confidence. Stroke/outline is only a center-occupancy heuristic. */
internal fun referenceFeatures(bytes: ByteArray): ReferenceFeatures {
    validateAnalysisPng(bytes)
    val decoded = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    val bitmap = Bitmap.createScaledBitmap(decoded, 64, 64, true)
    try {
        val pixels = IntArray(4096); bitmap.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        val alpha = DoubleArray(4096); val luminance = DoubleArray(4096); val hist = IntArray(64)
        var occupied = 0; var center = 0; var left = 64; var top = 64; var right = -1; var bottom = -1
        var light = 0.0; var light2 = 0.0; var sat = 0.0; var weight = 0.0; var soft = 0
        val colorPixels = mutableListOf<Pair<Int, Double>>()
        for (i in pixels.indices) {
            val p = pixels[i]; val a = (p ushr 24) and 255; alpha[i] = a / 255.0
            if (a < 32) continue
            val x = i % 64; val y = i / 64; occupied++
            if (x in 20..43 && y in 20..43) center++
            left = min(left, x); right = max(right, x); top = min(top, y); bottom = max(bottom, y)
            val r = (p ushr 16) and 255; val g = (p ushr 8) and 255; val b = p and 255
            val l = (.2126 * r + .7152 * g + .0722 * b) / 255
            luminance[i] = l * alpha[i]
            val w = alpha[i]; weight += w; light += l * w; light2 += l * l * w
            val hi = max(r, max(g, b)); val lo = min(r, min(g, b)); sat += w * if (hi == 0) 0.0 else (hi - lo).toDouble() / hi
            hist[(r / 64) * 16 + (g / 64) * 4 + b / 64]++
            if (a < 224) soft++
            // Faint antialias/shadow RGB is not a principal palette swatch.
            if (a >= 128) colorPixels += (p or (0xff shl 24)) to w
        }
        require(occupied >= 20 && pixels.distinct().size > 1) { "Reference has too little visible artwork." }
        var edges = 0
        for (y in 0..62) for (x in 0..62) {
            val i = y * 64 + x
            if (abs(alpha[i] - alpha[i + 1]) + abs(alpha[i] - alpha[i + 64]) +
                abs(luminance[i] - luminance[i + 1]) + abs(luminance[i] - luminance[i + 64]) > .25) edges++
        }
        fun hash(values: DoubleArray): Long {
            var result = 0L
            for (y in 0..7) for (x in 0..7) if (values[(y * 8 + 4) * 64 + x * 7] > values[(y * 8 + 4) * 64 + x * 7 + 7]) {
                result = result or (1L shl (y * 8 + x))
            }
            return result
        }
        require(colorPixels.isNotEmpty()) { "Reference has no reliable visible colors." }
        val swatches = measuredColors(colorPixels)
        return ReferenceFeatures(occupied / 4096.0, min(min(left, top), min(63 - right, 63 - bottom)) / 64.0,
            light / weight, sat / weight, sqrt((light2 / weight - (light / weight) * (light / weight)).coerceAtLeast(0.0)),
            edges / 3969.0, (1 - center / 576.0).coerceIn(0.0, 1.0), swatches.map { colorHex(it.first) },
            hist.map { it.toDouble() / occupied }, hash(alpha), hash(luminance), swatches.map { it.second },
            (right - left + 1) / 64.0, (bottom - top + 1) / 64.0, soft.toDouble() / occupied)
    } finally { if (bitmap !== decoded) bitmap.recycle(); decoded.recycle() }
}
