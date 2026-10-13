package dev.renkinProject.renkin.data.project

import org.json.JSONArray
import org.json.JSONObject

/** Explicit versioned, credential-free backup payload. File bytes travel as separate ZIP entries. */
object ProjectAiCodec {
    fun encode(data: ProjectAiSnapshot): JSONObject = obj("schema" to 3,
        "metadata" to array(data.metadata.map { obj("profileId" to it.profileId, "mode" to it.mode,
            "sourcePackage" to it.sourcePackage, "sourceStamp" to it.sourceStamp, "activeRevision" to it.activeRevision,
            "selectionJson" to it.selectionJson, "version" to it.version, "createdAt" to it.createdAt, "updatedAt" to it.updatedAt) }),
        "revisions" to array(data.revisions.map { obj("profileId" to it.profileId, "revision" to it.revision,
            "schemaVersion" to it.schemaVersion, "specification" to it.specification, "userInstruction" to it.userInstruction,
            "origin" to it.origin, "createdAt" to it.createdAt, "analysisProfileJson" to it.analysisProfileJson,
            "analysisEvidenceJson" to it.analysisEvidenceJson) }),
        "assets" to array(data.assets.map { obj("profileId" to it.profileId, "hash" to it.hash,
            "relativePath" to it.relativePath, "byteCount" to it.byteCount, "width" to it.width, "height" to it.height,
            "source" to it.source, "createdAt" to it.createdAt) }),
        "references" to array(data.references.map { obj("profileId" to it.profileId, "revision" to it.revision,
            "position" to it.position, "assetHash" to it.assetHash) }),
        "variants" to array(data.variants.map { obj("id" to it.id, "profileId" to it.profileId,
            "componentKey" to it.componentKey, "styleRevision" to it.styleRevision, "originalHash" to it.originalHash,
            "rawAssetHash" to it.rawAssetHash, "editedAssetHash" to it.editedAssetHash, "origin" to it.origin,
            "requestStatus" to it.requestStatus, "reviewStatus" to it.reviewStatus, "applyStatus" to it.applyStatus,
            "createdAt" to it.createdAt, "updatedAt" to it.updatedAt, "trialContextJson" to it.trialContextJson) }),
        "approvals" to array(data.approvals.map { obj("profileId" to it.profileId, "styleRevision" to it.styleRevision,
            "approvalId" to it.approvalId, "contextJson" to it.contextJson, "variantIdsJson" to it.variantIdsJson,
            "approvedAt" to it.approvedAt, "needsRevalidation" to it.needsRevalidation) }))

    fun decode(root: JSONObject?): ProjectAiSnapshot {
        if (root == null) return ProjectAiSnapshot()
        require(root.getInt("schema") in 1..3) { "Unsupported project AI backup version." }
        return ProjectAiSnapshot(
            root.rows("metadata").map { ProjectAiMetadata(it.getLong("profileId"), it.getString("mode"),
                it.nullString("sourcePackage"), it.nullString("sourceStamp"),
                if (it.isNull("activeRevision")) null else it.getLong("activeRevision"),
                it.getString("selectionJson"), it.getLong("version"), it.getLong("createdAt"), it.getLong("updatedAt")) },
            root.rows("revisions").map { StyleRevision(it.getLong("profileId"), it.getLong("revision"),
                it.getInt("schemaVersion"), it.getString("specification"), it.getString("userInstruction"),
                it.getString("origin"), it.getLong("createdAt"), it.nullString("analysisProfileJson"), it.nullString("analysisEvidenceJson")) },
            root.rows("assets").map { ReferenceAsset(it.getLong("profileId"), it.getString("hash"),
                it.getString("relativePath"), it.getLong("byteCount"), it.getInt("width"), it.getInt("height"),
                it.getString("source"), it.getLong("createdAt")) },
            root.rows("references").map { StyleReference(it.getLong("profileId"), it.getLong("revision"),
                it.getInt("position"), it.getString("assetHash")) },
            root.rows("variants").map { IconAssetVariant(it.getString("id"), it.getLong("profileId"),
                it.getString("componentKey"), it.getLong("styleRevision"), it.getString("originalHash"),
                it.getString("rawAssetHash"), it.nullString("editedAssetHash"), it.getString("origin"),
                it.getString("requestStatus"), it.getString("reviewStatus"), it.getString("applyStatus"),
                it.getLong("createdAt"), it.getLong("updatedAt"), it.nullString("trialContextJson")) },
            if (root.getInt("schema") < 3) emptyList() else root.rows("approvals").map {
                StyleTrialApproval(it.getLong("profileId"), it.getLong("styleRevision"), it.getString("approvalId"),
                    it.getString("contextJson"), it.getString("variantIdsJson"), it.getLong("approvedAt"), it.getBoolean("needsRevalidation")) })
    }

    private fun obj(vararg fields: Pair<String, Any?>) = JSONObject().apply {
        fields.forEach { (name, value) -> put(name, value ?: JSONObject.NULL) }
    }
    private fun array(rows: List<JSONObject>) = JSONArray(rows)
    private fun JSONObject.rows(name: String): List<JSONObject> {
        val array = getJSONArray(name)
        require(array.length() <= 50000) { "Too many project backup records." }
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
    private fun JSONObject.nullString(name: String): String? = if (isNull(name)) null else getString(name)
}
