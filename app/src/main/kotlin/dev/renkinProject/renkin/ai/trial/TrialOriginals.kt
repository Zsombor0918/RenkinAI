package dev.renkinProject.renkin.ai.trial

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Process
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.pngBytes
import dev.renkinProject.renkin.data.project.inspectPng
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class OriginalIconEvidence(val source: String, val drawableType: String, val user: Int,
    val density: Int, val resourceId: Int, val platformAgreement: Boolean) {
    fun validate() {
        require(source in setOf("APP_COMPONENT_RESOURCE", "APP_RESOURCE_FALLBACK"))
        require(drawableType in setOf("adaptive", "bitmap", "vector", "other") && user >= 0 && density in 1..10000 && resourceId > 0)
    }
    fun json() = json("schema" to 1, "source" to source, "drawableType" to drawableType, "user" to user,
        "density" to density, "resourceId" to resourceId, "platformAgreement" to platformAgreement,
        "verification" to "UNVERIFIED_ORIGINAL").also { validate() }
    companion object {
        fun parse(r: org.json.JSONObject): OriginalIconEvidence {
            r.exactKeys(setOf("schema", "source", "drawableType", "user", "density", "resourceId", "platformAgreement", "verification"))
            require(r.longValue("schema") == 1L && r.stringValue("verification") == "UNVERIFIED_ORIGINAL")
            fun integer(key: String) = r.longValue(key).also { require(it in 0..Int.MAX_VALUE.toLong()) }.toInt()
            return OriginalIconEvidence(r.stringValue("source"), r.stringValue("drawableType"), integer("user"), integer("density"), integer("resourceId"),
                r.get("platformAgreement") as? Boolean ?: error("Invalid original evidence.")).also { it.validate() }
        }
    }
}
internal class UnverifiedTrialOriginal : IllegalStateException("UNVERIFIED_ORIGINAL")
internal data class TrialOriginal(val png: ByteArray, val origin: String, val evidence: OriginalIconEvidence? = null) { val hash get() = sha256(png) }

internal fun validateTrialPng(png: ByteArray) {
    require(inspectPng(png, 8 * 1024 * 1024) == (1024 to 1024))
    dev.renkinProject.renkin.ai.validateAiPng(png).recycle()
}

/** Render the real current component, not a 48px thumbnail or a synthetic fallback. */
internal suspend fun currentTrialOriginal(context: Context, key: String): TrialOriginal = withContext(Dispatchers.Default) {
    val component = checkNotNull(ComponentName.unflattenFromString(key))
    val manager = context.packageManager
    val before = manager.getPackageInfo(component.packageName, 0)
    val info = manager.getActivityInfo(component, 0) // A removed component never becomes an app fallback.
    val user = Process.myUid() / 100000
    if (!info.enabled || !info.applicationInfo.enabled || info.applicationInfo.uid / 100000 != user) throw UnverifiedTrialOriginal()
    val density = context.resources.displayMetrics.densityDpi
    val night = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    val resourceId = info.iconResource
    if (resourceId == 0) throw UnverifiedTrialOriginal()
    // Resolve the exact component's declared full icon from its owning package, without
    // selecting adaptive monochrome or consulting launcher, EXTEND or profile artwork.
    // Resources can still be OEM-overlaid: this is evidence, never a pristine-store guarantee.
    val drawable = try { manager.getResourcesForApplication(info.applicationInfo).getDrawableForDensity(resourceId, density, null) }
        catch (_: Exception) { throw UnverifiedTrialOriginal() }
    val bytes = renderTrialOriginal(drawable)
    val platformAgreement = runCatching { bytes.contentEquals(renderTrialOriginal(manager.getActivityIcon(component))) }.getOrDefault(false)
    val after = manager.getPackageInfo(component.packageName, 0)
    check(before.lastUpdateTime == after.lastUpdateTime && androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(before) ==
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(after))
    check(night == (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) && density == context.resources.displayMetrics.densityDpi)
    val suffix = "${component.flattenToString()}:${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(after)}:${after.lastUpdateTime}:$night:render1"
    // Equal original bytes retain the historical origin, so a refactor alone does not stale approvals.
    val origin = if (platformAgreement) "launcher:$suffix" else "component-resource-v2:$suffix:user=$user:resource=$resourceId"
    val type = when (drawable) { is AdaptiveIconDrawable -> "adaptive"; is android.graphics.drawable.BitmapDrawable -> "bitmap"
        is android.graphics.drawable.VectorDrawable -> "vector"; else -> "other" }
    TrialOriginal(bytes, origin, OriginalIconEvidence(if (info.icon != 0) "APP_COMPONENT_RESOURCE" else "APP_RESOURCE_FALLBACK",
        type, user, density, resourceId, platformAgreement))
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
