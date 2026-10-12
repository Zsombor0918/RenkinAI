package dev.renkinProject.renkin.data.transfer

import android.content.Context
import dev.renkinProject.renkin.apk.IconPackBuilder
import dev.renkinProject.renkin.apk.PackKeystore
import dev.renkinProject.renkin.data.project.ProjectAiRepository
import dev.renkinProject.renkin.data.project.ProjectAssetStore
import dev.renkinProject.renkin.data.project.inspectPng
import dev.renkinProject.renkin.data.project.readBounded
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Stage one bounded immutable archive before changing ANY store. Restore never reopens a URI
 * whose content could have changed between the validation and installation passes. */
internal class ValidatedBackupArchive private constructor(
    val manifest: JSONObject,
    val data: BackupData,
    val entries: Map<String, File>,
    private val directory: File
) : Closeable {
    val uploadedImages: Int get() = entries.keys.count { it.startsWith("uploads/") }
    override fun close() { directory.deleteRecursively() }

    companion object {
        // Legacy raster import can retain a side just below 2048 px, including transparent
        // artwork. Do not impose the new project-reference 4 MiB/nonempty rule on its gallery.
        internal const val MAX_UPLOAD_BYTES = 20 * 1024 * 1024
        private const val MAX_TOTAL = 512L * 1024 * 1024
        private const val MAX_ENTRIES = 10000
        fun read(context: Context, open: () -> InputStream): ValidatedBackupArchive {
            val temporary = File.createTempFile("renkin-restore-", ".stage", context.cacheDir)
            check(temporary.delete() && temporary.mkdir())
            try {
                val entries = linkedMapOf<String, File>()
                var total = 0L
                ZipInputStream(open().buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        require(!entry.isDirectory && !entries.containsKey(name) && entries.size < MAX_ENTRIES) {
                            "Duplicate or excessive backup entries."
                        }
                        val limit = when {
                            name == "manifest.json" -> 64 * 1024
                            name == "data.json" -> 128 * 1024 * 1024
                            name == "keystore/${IconPackBuilder.KEYSTORE_FILE_NAME}" -> 1024 * 1024
                            name == "keystore/${PackKeystore.PASSWORD_FILE_NAME}" -> 4096
                            name.matches(Regex("uploads/[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}\\.png")) && !name.contains("..") -> MAX_UPLOAD_BYTES
                            name.matches(Regex("project_ai_assets/[1-9][0-9]*/[a-f0-9]{64}\\.png")) -> ProjectAssetStore.MAX_BYTES
                            else -> error("Unsupported or unsafe backup entry.")
                        }
                        val file = File(temporary, entries.size.toString())
                        var size = 0L
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                size += count
                                total += count
                                require(size <= limit && total <= MAX_TOTAL) { "Backup exceeds the safety limit." }
                                output.write(buffer, 0, count)
                            }
                        }
                        entries[name] = file
                        entry = zip.nextEntry
                    }
                }
                val manifest = JSONObject(checkNotNull(entries["manifest.json"]) { "Not a Renkin file (no manifest)." }.readText())
                require(manifest.getInt("format") in 1..BackupCodec.FORMAT_VERSION) { "File was made by a newer app version." }
                require(manifest.getString("kind") in setOf("backup", "profile")) { "Unknown file kind." }
                val data = BackupCodec.decode(checkNotNull(entries["data.json"]) { "File has no data entry." }.readText())
                val ids = data.profiles.map { it.profile.id }
                require(ids.isNotEmpty() && ids.all { it > 0 } && ids.distinct().size == ids.size) { "Invalid backup profile identities." }
                if (manifest.getString("kind") == "profile") require(ids.size == 1 && entries.keys.none {
                    it.startsWith("uploads/") || it.startsWith("keystore/")
                }) { "A shared profile cannot carry device files." }
                ProjectAiRepository.validateSnapshot(data.projectAi, ids.toSet())
                val expected = data.projectAi.assets.map { "project_ai_assets/${it.relativePath}" }.toSet()
                require(expected == entries.keys.filter { it.startsWith("project_ai_assets/") }.toSet()) {
                    "Project backup has missing or unlisted reference images."
                }
                data.projectAi.assets.groupBy { it.profileId }.values.forEach {
                    require(it.sumOf { asset -> asset.byteCount } <= ProjectAssetStore.PROJECT_QUOTA)
                }
                data.projectAi.assets.forEach { asset ->
                    val file = entries.getValue("project_ai_assets/${asset.relativePath}")
                    require(file.length() == asset.byteCount)
                    val bytes = file.inputStream().use { it.readBounded(ProjectAssetStore.MAX_BYTES) }
                    require(sha256(bytes) == asset.hash && inspectPng(bytes) == (asset.width to asset.height)) {
                        "Project backup image checksum or dimensions are invalid."
                    }
                    if (data.projectAi.variants.any { it.trialContextJson != null && it.profileId == asset.profileId && it.rawAssetHash == asset.hash }) {
                        dev.renkinProject.renkin.ai.trial.validateTrialPng(bytes)
                    }
                }
                entries.filterKeys { it.startsWith("uploads/") }.values.forEach {
                    inspectPng(it.readBytes(), MAX_UPLOAD_BYTES, requireVisible = false)
                }
                return ValidatedBackupArchive(manifest, data, entries, temporary)
            } catch (error: Throwable) {
                temporary.deleteRecursively()
                throw error
            }
        }
    }
}
