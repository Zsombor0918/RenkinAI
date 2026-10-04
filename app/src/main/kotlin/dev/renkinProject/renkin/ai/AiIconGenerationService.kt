package dev.renkinProject.renkin.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.data.UploadedImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

internal fun validateAiPng(bytes: ByteArray): Bitmap {
    require(bytes.size in 8..(8 * 1024 * 1024)) { "Invalid PNG size." }
    val signature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    require(bytes.copyOfRange(0, 8).contentEquals(signature)) { "The result is not a PNG." }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth == bounds.outHeight && bounds.outWidth in 512..4096) {
        "The result must be a square icon between 512 and 4096 pixels."
    }
    val sample = Integer.highestOneBit((bounds.outWidth / 1024).coerceAtLeast(1))
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inSampleSize = sample })
        ?: throw IOException("The PNG could not be decoded.")
    val bitmap = if (decoded.width == 1024 && decoded.height == 1024) decoded
        else Bitmap.createScaledBitmap(decoded, 1024, 1024, true).also { if (it !== decoded) decoded.recycle() }
    var visible = false
    var transparent = false
    val row = IntArray(bitmap.width)
    for (y in 0 until bitmap.height) {
        bitmap.getPixels(row, 0, row.size, 0, y, row.size, 1)
        for (pixel in row) {
            val alpha = pixel ushr 24
            if (alpha > 0) visible = true
            if (alpha == 0) transparent = true
        }
    }
    if (!visible || !transparent) {
        bitmap.recycle()
        error(if (!visible) "The generated icon is empty." else "The result has no transparent pixels. Regenerate it.")
    }
    return bitmap
}

internal fun Bitmap.pngBytes(): ByteArray = ByteArrayOutputStream().use {
    check(compress(Bitmap.CompressFormat.PNG, 100, it)) { "PNG encoding failed." }
    it.toByteArray()
}

data class AiIconResult(val file: File, val bitmap: Bitmap)

@Singleton
class AiIconGenerationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentials: AiCredentialStore,
    private val client: OpenAiImageClient,
    private val stabilityClient: StabilityImageClient
) {
    private val generation = Mutex()
    private fun draftFile(target: String): AtomicFile {
        val hash = MessageDigest.getInstance("SHA-256").digest(target.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return AtomicFile(File(context.noBackupFilesDir, "ai-draft-$hash"))
    }

    suspend fun previous(target: String): AiIconResult? = withContext(Dispatchers.IO) {
        runCatching {
            val marker = draftFile(target).readFully().toString(Charsets.UTF_8)
            val name = if (marker.startsWith("{")) JSONObject(marker).getString("file") else marker
            require(name.matches(Regex("img_[0-9]+_[0-9]+\\.png")))
            val file = File(UploadedImageStore.directory(context), name)
            require(file.length() in 1..(4 * 1024 * 1024))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            require(bounds.outWidth in 1..512 && bounds.outHeight in 1..512)
            val bitmap = BitmapFactory.decodeFile(file.path) ?: return@runCatching null
            AiIconResult(file, bitmap)
        }.getOrNull()
    }

    /** Batch reuse requires matching settings; old single-icon previews are never silently reused. */
    suspend fun matchingPrevious(target: String, settings: AiSettings, source: Bitmap): AiIconResult? = withContext(Dispatchers.IO) {
        val marker = runCatching { JSONObject(draftFile(target).readFully().toString(Charsets.UTF_8)) }.getOrNull()
        if (marker?.optString("fingerprint") == requestFingerprint(settings, source.pngBytes())) previous(target) else null
    }

    suspend fun savedResult(name: String): AiIconResult = withContext(Dispatchers.IO) {
        require(name.matches(Regex("img_[0-9]+_[0-9]+\\.png"))) { "Invalid saved icon." }
        val file = File(UploadedImageStore.directory(context), name)
        require(file.length() in 8..(4 * 1024 * 1024)) { "The saved icon is missing. Do not regenerate without checking API usage." }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth in 1..512 && bounds.outHeight in 1..512) { "Invalid saved image dimensions." }
        AiIconResult(file, BitmapFactory.decodeFile(file.path) ?: error("The saved PNG could not be decoded."))
    }

    internal fun requestFingerprint(settings: AiSettings, source: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(listOf("v3", settings.provider, settings.model, settings.quality,
            settings.style, settings.appInstruction).joinToString("\u0000").toByteArray())
        digest.update(source)
        settings.referenceNames.forEach { name ->
            digest.update(name.toByteArray())
            File(UploadedImageStore.directory(context), name).takeIf { it.isFile }?.let { require(it.length() <= 4 * 1024 * 1024) { "A reference icon is too large." }; digest.update(it.readBytes()) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun generate(target: String, source: Bitmap, settings: AiSettings): AiIconResult {
        check(generation.tryLock()) { "Another icon is already being generated." }
        try {
            return withContext(Dispatchers.IO) {
                settings.validate()
                require(settings.provider != AiSettings.STABILITY || settings.referenceNames.isNotEmpty()) {
                    "Stability style transfer needs one style reference. Add an icon in AI settings."
                }
                val images = listOf(source.pngBytes()) + settings.referenceNames.map { name ->
                    val file = File(UploadedImageStore.directory(context), name)
                    require(file.length() in 1..(4 * 1024 * 1024)) {
                        "A reference icon is missing or too large. Select it again in AI settings."
                    }
                    file.readBytes()
                }
                val key = credentials.read(settings.provider)
                val png = when (settings.provider) {
                    AiSettings.OPENAI -> client.edit(key, settings, images)
                    AiSettings.STABILITY -> stabilityClient.edit(key, settings, images.first(), images[1])
                    else -> error("Select a supported AI provider.")
                }
                ensureActive()
                val fullSize = validateAiPng(png)
                val bitmap = try { Bitmap.createScaledBitmap(fullSize, 512, 512, true) }
                    finally { fullSize.recycle() }
                // Persist BEFORE showing Preview. A process death never requires regenerating
                // a successfully saved image; gallery and per-target draft can recover it.
                val file = try { UploadedImageStore.save(context, bitmap) }
                    catch (e: Exception) { bitmap.recycle(); throw e }
                val marker = draftFile(target)
                val stream = marker.startWrite()
                try {
                    stream.write(JSONObject().put("file", file.name).put("fingerprint", requestFingerprint(settings, images.first())).toString().toByteArray())
                    marker.finishWrite(stream)
                } catch (e: Exception) {
                    marker.failWrite(stream)
                    throw IOException("The icon is saved in Upload, but its preview could not be recorded.")
                }
                AiIconResult(file, bitmap)
            }
        } finally { generation.unlock() }
    }
}
