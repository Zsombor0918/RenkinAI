package dev.renkinProject.renkin.ai.style

import android.graphics.BitmapFactory
import android.graphics.drawable.*
import dev.renkinProject.renkin.data.project.inspectPng
import kotlinx.coroutines.CancellationException

/** Allowlisted reasons only; never propagate resource paths or provider/exception prose. */
internal enum class ReferenceFailure { MISSING_DRAWABLE, DECLARED_UNRENDERABLE, UNSUPPORTED_DYNAMIC,
    UNSUPPORTED_STATEFUL, RENDER_EXCEPTION, EMPTY_ALPHA, NEAR_EMPTY_ALPHA, SOURCE_RESOURCE_FAILURE, STALE_SOURCE }
internal sealed interface ReferenceRead {
    data class Rendered(val png: ByteArray) : ReferenceRead
    data class Failed(val reason: ReferenceFailure) : ReferenceRead
}
internal fun diagnoseReference(source: Drawable): ReferenceRead {
    if (source is Animatable) return ReferenceRead.Failed(ReferenceFailure.UNSUPPORTED_DYNAMIC)
    if (source is StateListDrawable || source is LevelListDrawable || source is LayerDrawable)
        return ReferenceRead.Failed(ReferenceFailure.UNSUPPORTED_STATEFUL)
    return try {
        val png = renderStaticReference(source) ?: return ReferenceRead.Failed(ReferenceFailure.DECLARED_UNRENDERABLE)
        validateReferencePixels(png); ReferenceRead.Rendered(png)
    } catch (cancel: CancellationException) { throw cancel }
    catch (failure: EmptyReference) { ReferenceRead.Failed(failure.reason) }
    catch (_: OutOfMemoryError) { ReferenceRead.Failed(ReferenceFailure.SOURCE_RESOURCE_FAILURE) }
    catch (_: Exception) { ReferenceRead.Failed(ReferenceFailure.RENDER_EXCEPTION) }
}
internal class EmptyReference(val reason: ReferenceFailure) : IllegalArgumentException("Insufficient visible reference artwork.")
internal fun validateReferencePixels(png: ByteArray) {
    val (w,h) = inspectPng(png, MAX_ANALYSIS_PNG, requireVisible = false)
    require(w in 1..256 && h in 1..256)
    val bitmap = checkNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))
    try {
        val pixels = IntArray(w*h); bitmap.getPixels(pixels,0,w,0,0,w,h)
        if (pixels.none { it ushr 24 != 0 }) throw EmptyReference(ReferenceFailure.EMPTY_ALPHA)
        if (pixels.count { it ushr 24 >= 32 } < maxOf(20, w*h/205)) throw EmptyReference(ReferenceFailure.NEAR_EMPTY_ALPHA)
    } finally { bitmap.recycle() }
}
