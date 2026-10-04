package dev.renkinProject.renkin.ai

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Local task journal, not a backup format; no credentials or pixel data. */
@Singleton
class AiBatchStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun file(profile: Long) = AtomicFile(File(context.noBackupFilesDir, "ai-batch-$profile.json"))
    suspend fun clear(profile: Long) = withContext(Dispatchers.IO) { file(profile).delete() }
    suspend fun load(profile: Long): AiBatchSession? = withContext(Dispatchers.IO) {
        if (!file(profile).baseFile.exists()) return@withContext null
        require(file(profile).baseFile.length() <= 4 * 1024 * 1024) { "Batch journal is too large." }
        decodeBatch(JSONObject(file(profile).readFully().toString(Charsets.UTF_8))).also { require(it.profileId == profile) }
    }
    suspend fun save(value: AiBatchSession) = withContext(Dispatchers.IO) {
        val target = file(value.profileId)
        val output = target.startWrite()
        try { output.write(encodeBatch(value).toString().toByteArray()); target.finishWrite(output) }
        catch (e: Exception) { target.failWrite(output); throw e }
    }
}
internal fun encodeBatch(value: AiBatchSession): JSONObject = JSONObject().put("version", 1)
    .put("profile", value.profileId).put("message", value.message).put("settings", JSONObject()
        .put("provider", value.settings.provider).put("model", value.settings.model).put("quality", value.settings.quality)
        .put("style", value.settings.style).put("references", JSONArray(value.settings.referenceNames)))
    .put("items", JSONArray(value.items.map { JSONObject().put("key", it.key).put("name", it.name)
        .put("instruction", it.instruction).put("status", it.status.name).put("file", it.fileName).put("error", it.error) }))
internal fun decodeBatch(json: JSONObject): AiBatchSession {
    require(json.getInt("version") == 1)
    val settings = json.getJSONObject("settings")
    val refs = settings.getJSONArray("references")
    val config = AiSettings(provider = settings.getString("provider"), model = settings.getString("model"),
        quality = settings.getString("quality"), style = settings.getString("style"),
        referenceNames = (0 until refs.length()).map { refs.getString(it) }).also { it.validate() }
    val items = json.getJSONArray("items")
    require(items.length() in 1..1000)
    fun JSONObject.optional(key: String): String? = if (isNull(key)) null else getString(key)
    return AiBatchSession(json.getLong("profile"), config, (0 until items.length()).map {
        val row = items.getJSONObject(it)
        AiBatchItem(row.getString("key"), row.getString("name"), row.getString("instruction"),
            AiBatchStatus.valueOf(row.getString("status")), row.optional("file"), row.optional("error"))
    }, json.optional("message"))
}
