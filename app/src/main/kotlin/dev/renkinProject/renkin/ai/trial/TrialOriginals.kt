package dev.renkinProject.renkin.ai.trial

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import dev.renkinProject.renkin.ai.pngBytes
import dev.renkinProject.renkin.data.project.inspectPng
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class TrialOriginal(val png: ByteArray, val origin: String) { val hash get() = sha256(png) }

internal fun validateTrialPng(png: ByteArray) {
    require(inspectPng(png, 8 * 1024 * 1024) == (1024 to 1024))
    dev.renkinProject.renkin.ai.validateAiPng(png).recycle()
}

/** Render the real current component, not a 48px thumbnail or a synthetic fallback. */
internal suspend fun currentTrialOriginal(context: Context, key: String): TrialOriginal = withContext(Dispatchers.Default) {
    val component = checkNotNull(ComponentName.unflattenFromString(key))
    val manager = context.packageManager
    val before = manager.getPackageInfo(component.packageName, 0)
    manager.getActivityInfo(component, 0) // A removed/nonexistent component cannot become an app fallback.
    val drawable = manager.getActivityIcon(component)
    val bytes = renderTrialOriginal(drawable)
    val after = manager.getPackageInfo(component.packageName, 0)
    check(before.lastUpdateTime == after.lastUpdateTime && androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(before) ==
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(after))
    val night = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    TrialOriginal(bytes, "launcher:${component.flattenToString()}:${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(after)}:${after.lastUpdateTime}:$night:render1")
}

internal fun renderTrialOriginal(drawable: Drawable): ByteArray {
    val owned = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
    val bounds = android.graphics.Rect(owned.bounds)
    val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
    return try {
        owned.setBounds(0, 0, 1024, 1024); owned.draw(Canvas(bitmap))
        bitmap.pngBytes().also { require(inspectPng(it) == (1024 to 1024)) }
    } finally { owned.bounds = bounds; bitmap.recycle() }
}
