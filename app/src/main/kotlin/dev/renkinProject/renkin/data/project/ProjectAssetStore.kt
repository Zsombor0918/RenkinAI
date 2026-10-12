package dev.renkinProject.renkin.data.project

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

/** Private immutable PNGs. A finalized file precedes its Room record; failed inserts leave an
 * unreferenced file, never a successful revision pointing at a half-written file. GC is explicit. */
class ProjectAssetStore(context: Context) {
    private val root = File(context.filesDir.canonicalFile, "project_ai_assets")

    suspend fun copy(profileId: Long, input: InputStream, mimeType: String = "image/png"): ReferenceAsset =
        withContext(Dispatchers.IO) {
            require(mimeType == "image/png") { "Select a PNG reference image." }
            input.use { install(profileId, it.readBounded(MAX_BYTES)) }
        }

    suspend fun install(profileId: Long, bytes: ByteArray, source: String = "MANUAL", restoring: Boolean = false): ReferenceAsset =
        withContext(Dispatchers.IO) {
            val dimensions = inspectPng(bytes)
            val hash = sha256(bytes)
            val target = resolve(path(profileId, hash))
            target.parentFile!!.mkdirs()
            require(target.parentFile!!.canonicalPath == target.parentFile!!.absolutePath) { "Unsafe asset directory." }
            val validExisting = target.isFile && target.length() == bytes.size.toLong() && sha256(target.readBytes()) == hash
            if (target.exists() && !restoring) {
                require(validExisting) {
                    "An existing project image is damaged. Restore it from a backup."
                }
            }
            if (!validExisting) {
                val used = target.parentFile!!.listFiles().orEmpty().filter { it.extension == "png" }.sumOf { it.length() }
                require(used + bytes.size <= PROJECT_QUOTA * if (restoring) 2 else 1) { "Project reference storage is full." }
                val temporary = File.createTempFile(".write-", ".tmp", target.parentFile)
                try {
                    FileOutputStream(temporary).use { output ->
                        currentCoroutineContext().ensureActive()
                        output.write(bytes)
                        output.flush()
                        output.fd.sync()
                    }
                    currentCoroutineContext().ensureActive()
                    check(temporary.renameTo(target)) { "Could not finalize the project image." }
                } finally { temporary.delete() }
            }
            ReferenceAsset(profileId, hash, path(profileId, hash), bytes.size.toLong(),
                dimensions.first, dimensions.second, source)
        }

    suspend fun read(asset: ReferenceAsset): ByteArray = withContext(Dispatchers.IO) {
        require(asset.relativePath == path(asset.profileId, asset.hash)) { "Invalid project asset ownership." }
        val file = resolve(asset.relativePath)
        require(file.isFile && file.length() == asset.byteCount && file.length() in 8..MAX_BYTES.toLong()) {
            "A saved project reference is missing or damaged. Restore it from a backup."
        }
        val bytes = file.inputStream().use { it.readBounded(MAX_BYTES) }
        require(sha256(bytes) == asset.hash) { "Project image checksum mismatch." }
        require(inspectPng(bytes) == (asset.width to asset.height)) { "Project image dimensions changed." }
        bytes
    }

    internal fun resolve(relative: String): File {
        require(relative.matches(Regex("[1-9][0-9]*/[a-f0-9]{64}\\.png"))) { "Invalid project image path." }
        require(!root.exists() || root.canonicalPath == root.absolutePath) { "Unsafe project storage." }
        return File(root, relative).also {
            require(it.canonicalPath == it.absolutePath && it.canonicalPath.startsWith(root.absolutePath + File.separator)) {
                "Project image path escapes private storage."
            }
        }
    }

    /** Caller holds the repository's storage lock. Active AND historical links/variants pin files.
     * Pending imported rows are retained too; no startup/background deletion of drafts or Upload. */
    internal fun collectUnreferenced(retained: List<ReferenceAsset>, candidates: Set<String>? = null): Int {
        if (!root.exists()) return 0
        require(root.canonicalPath == root.absolutePath)
        val paths = retained.map { it.relativePath }.toSet()
        var removed = 0
        root.listFiles().orEmpty().filter { it.isDirectory && it.name.matches(Regex("[1-9][0-9]*")) &&
            it.canonicalPath == it.absolutePath }.forEach { directory ->
            directory.listFiles().orEmpty().filter { it.canonicalPath == it.absolutePath }.forEach { file ->
                val relative = "${directory.name}/${file.name}"
                if ((file.name.matches(Regex("[a-f0-9]{64}\\.png")) || file.name.startsWith(".write-")) &&
                    relative !in paths && (candidates == null || relative in candidates) && file.delete()) removed++
            }
            if (directory.listFiles().isNullOrEmpty()) directory.delete()
        }
        return removed
    }

    companion object {
        const val MAX_BYTES = 4 * 1024 * 1024
        const val PROJECT_QUOTA = 64L * 1024 * 1024
        fun path(profileId: Long, hash: String): String {
            require(profileId > 0 && hash.matches(Regex("[a-f0-9]{64}")))
            return "$profileId/$hash.png"
        }
    }
}

internal fun InputStream.readBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "Image or archive entry exceeds the safety limit." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
    .joinToString("") { "%02x".format(it) }

/** Bound before allocating pixels; validate every chunk CRC and terminal IEND (a lenient decoder
 * alone can accept truncated PNGs). References may be opaque; empty artwork is rejected. */
internal fun inspectPng(bytes: ByteArray, limit: Int = ProjectAssetStore.MAX_BYTES, requireVisible: Boolean = true): Pair<Int, Int> {
    require(bytes.size in 33..limit && bytes.take(8).toByteArray().contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) {
        "Invalid or oversized PNG image."
    }
    val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
    var offset = 8
    var ended = false
    var sawData = false
    var chunks = 0
    while (offset + 12 <= bytes.size) {
        val length = buffer.getInt(offset)
        require(length >= 0 && length <= bytes.size - offset - 12) { "Truncated PNG image." }
        val type = String(bytes, offset + 4, 4, Charsets.US_ASCII)
        if (chunks++ == 0) require(type == "IHDR" && length == 13)
        val crc = CRC32().apply { update(bytes, offset + 4, length + 4) }.value
        require(crc == (buffer.getInt(offset + 8 + length).toLong() and 0xffffffffL)) { "Damaged PNG image." }
        offset += 12 + length
        if (type == "IDAT") sawData = true
        if (type == "IEND") { require(length == 0 && offset == bytes.size); ended = true; break }
    }
    require(ended && sawData) { "Incomplete PNG image." }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outMimeType == "image/png" && bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096) {
        "PNG dimensions must be between 1 and 4096 pixels."
    }
    val bitmap = checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "PNG decoding failed." }
    try {
        val row = IntArray(bitmap.width)
        var visible = false
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, row.size, 0, y, row.size, 1)
            if (row.any { it ushr 24 != 0 }) { visible = true; break }
        }
        require(!requireVisible || visible) { "Reference artwork is empty." }
    } finally { bitmap.recycle() }
    return bounds.outWidth to bounds.outHeight
}
