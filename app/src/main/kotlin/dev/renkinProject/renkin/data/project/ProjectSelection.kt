package dev.renkinProject.renkin.data.project

import org.json.JSONArray
import org.json.JSONObject

/** Primitive preparation only; revalidation is mandatory on reopening, never cost consent. */
data class ProjectSelection(
    val mode: String,
    val sourcePackage: String?,
    val sourceStamp: String?,
    val catalogStamp: String,
    val componentKeys: List<String>,
    val updatedAt: Long = System.currentTimeMillis(),
    val needsRevalidation: Boolean = true,
    val schemaVersion: Int = 1
) {
    fun validate() {
        require(schemaVersion == 1 && mode in setOf("CREATE", "EXTEND")) { "Unsupported project selection." }
        require(if (mode == "CREATE") sourcePackage == null && sourceStamp == null else !sourcePackage.isNullOrBlank())
        require(componentKeys.size <= 10000 && componentKeys.distinct().size == componentKeys.size)
        require(componentKeys.all { it.isNotBlank() && it.length <= 1000 && !it.contains('\u0000') })
        require(catalogStamp.matches(Regex("[a-f0-9]{64}")))
        require(sourceStamp == null || sourceStamp.length <= 2000)
    }

    fun encode(): String {
        validate()
        return JSONObject().put("schema", schemaVersion).put("mode", mode)
            .put("sourcePackage", sourcePackage).put("sourceStamp", sourceStamp)
            .put("catalogStamp", catalogStamp).put("keys", JSONArray(componentKeys))
            .put("updatedAt", updatedAt).put("needsRevalidation", needsRevalidation).toString()
    }

    fun isCurrent(mode: String, source: String?, stamp: String?, catalog: String, selectableKeys: Set<String>): Boolean =
        this.mode == mode && sourcePackage == source && sourceStamp == stamp &&
            catalogStamp == catalog && componentKeys.all { it in selectableKeys }

    companion object {
        fun decode(json: String): ProjectSelection? {
            if (json.isEmpty()) return null
            require(json.length <= 12 * 1024 * 1024)
            val root = JSONObject(json)
            val keys = root.getJSONArray("keys")
            require(keys.length() <= 10000)
            return ProjectSelection(root.getString("mode"),
                root.optString("sourcePackage").takeIf { !root.isNull("sourcePackage") },
                root.optString("sourceStamp").takeIf { !root.isNull("sourceStamp") },
                root.getString("catalogStamp"), (0 until keys.length()).map { keys.getString(it) },
                root.getLong("updatedAt"), true, root.getInt("schema")).also { it.validate() }
        }
    }
}
