package dev.renkinProject.renkin.ai.style

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import dev.renkinProject.renkin.apk.CoverageIndex
import dev.renkinProject.renkin.apk.CoverageIndexState
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.apk.CoverageStamp
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** Actual PNG artwork, entirely local. No downloaded assets, credentials or provider calls. */
internal fun analysisFixturePng(style: Int = 0, variant: Int = 0, alpha: Int = 255): ByteArray {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (style) { 0 -> Color.WHITE; 1 -> Color.rgb(12, 18, 24); else -> Color.rgb(240, 40, 160) }
            this.alpha = alpha
            this.style = if (style == 0) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = (5 + variant % 4 * 3).toFloat()
        }
        val vertices = 3 + variant % 6
        val radius = 50.0 + variant % 5 * 10
        val path = Path()
        repeat(vertices) { i ->
            val angle = (i * 360.0 / vertices + variant * 17) * Math.PI / 180
            val x = (128 + radius * cos(angle)).toFloat(); val y = (128 + radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); canvas.drawPath(path, paint)
        if (style == 2) {
            paint.style = Paint.Style.FILL
            repeat(6 + variant % 7) { i ->
                paint.color = Color.rgb((i * 41 + variant * 11) % 256, (i * 79 + 80) % 256, (i * 17 + 30) % 256)
                paint.alpha = alpha
                val x = (35 + (i * 37 + variant * 7) % 180).toFloat()
                val y = (35 + (i * 61 + variant * 13) % 180).toFloat()
                canvas.drawCircle(x, y, (5 + i % 4 * 3).toFloat(), paint)
            }
        }
        return ByteArrayOutputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.toByteArray() }
    } finally { bitmap.recycle() }
}

internal fun analysisFixtureStyle() = StyleProfileV1(StyleArchetype.OUTLINE, listOf("#FFFFFFFF"),
    StyleBackground.TRANSPARENT, StyleStroke.THIN, StyleCorners.ROUND, StylePadding.MEDIUM,
    StyleDetail.SIMPLE, listOf("photographic textures"), listOf("retain the recognizable symbol"),
    "Light monochrome outlines with generous transparent padding.", listOf("small sample; stroke is an estimate"),
    false, StyleAssessment.TENTATIVE)

internal fun analysisFixtureRequest(profileId: Long = 4L, images: Int = 1): StyleAnalysisRequest {
    val context = AnalysisContext(profileId, "CREATE", null, null, null, 0L,
        sha256(ByteArray(0)), sha256("catalog".toByteArray()), null)
    return StyleAnalysisRequest(context, (0 until images).map { i ->
        val png = analysisFixturePng(i % 3, i); AnalysisImage(sha256(png), png)
    }, "clear outline artwork", "keep the original symbol")
}

internal fun analysisFixtureResponse(style: StyleProfileV1 = analysisFixtureStyle(), usage: Boolean = true): String =
    json("id" to "resp_fixture", "status" to "completed", "output" to JSONArray().put(json("type" to "reasoning"))
        .put(json("type" to "message", "status" to "completed", "content" to JSONArray().put(json("type" to "output_text", "text" to style.encode())))))
        .apply { if (usage) put("usage", json("input_tokens" to 43, "output_tokens" to 92)) }.toString()

internal fun analysisFixtureContext(directory: File): Context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
    override fun getNoBackupFilesDir(): File = directory
}

internal class FixtureReferenceReader(val artwork: Map<String, ByteArray>,
    mapping: Map<String, String> = artwork.keys.associate { "uninstalled.$it/.Main" to it },
    dynamic: Set<String> = emptySet(), override val stableReferences: Boolean = true) : CoverageSourceReader {
    override val index = CoverageIndex(CoverageStamp("fixture.pack", 7, 1234, 3, false),
        CoverageIndexState.READY, mapping, dynamic)
    var current = true
    var renderCount = 0
    var afterRender: (() -> Unit)? = null
    override fun isCurrent() = current
    override suspend fun canRender(drawableName: String) = artwork.containsKey(drawableName)
    override suspend fun referencePng(drawableName: String): ByteArray? {
        renderCount++; afterRender?.invoke(); return artwork[drawableName]
    }
}
