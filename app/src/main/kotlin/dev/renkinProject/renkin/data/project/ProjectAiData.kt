package dev.renkinProject.renkin.data.project

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import dev.renkinProject.renkin.data.Profile

/** One preparation context per real Renkin profile. Selection is never permission to bill. */
@Entity(foreignKeys = [ForeignKey(entity = Profile::class, parentColumns = ["id"],
    childColumns = ["profileId"], onDelete = ForeignKey.CASCADE)])
data class ProjectAiMetadata(
    @PrimaryKey val profileId: Long,
    val mode: String = "CREATE",
    val sourcePackage: String? = null,
    val sourceStamp: String? = null,
    val activeRevision: Long? = null,
    val selectionJson: String = "",
    val version: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

/** Immutable manual/imported history; no approval or automatic analysis is inferred. */
@Entity(primaryKeys = ["profileId", "revision"], foreignKeys = [ForeignKey(
    entity = Profile::class, parentColumns = ["id"], childColumns = ["profileId"],
    onDelete = ForeignKey.CASCADE)])
data class StyleRevision(
    val profileId: Long,
    val revision: Long,
    val schemaVersion: Int = 1,
    val specification: String,
    val userInstruction: String = "",
    val origin: String = "USER",
    val createdAt: Long = System.currentTimeMillis(),
    val analysisProfileJson: String? = null,
    val analysisEvidenceJson: String? = null
)

/** Files are immutable, addressed by content, and owned by one profile (no shared deletion). */
@Entity(primaryKeys = ["profileId", "hash"], foreignKeys = [ForeignKey(
    entity = Profile::class, parentColumns = ["id"], childColumns = ["profileId"],
    onDelete = ForeignKey.CASCADE)])
data class ReferenceAsset(
    val profileId: Long,
    val hash: String,
    val relativePath: String,
    val byteCount: Long,
    val width: Int,
    val height: Int,
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(primaryKeys = ["profileId", "revision", "position"], foreignKeys = [
    ForeignKey(entity = StyleRevision::class, parentColumns = ["profileId", "revision"],
        childColumns = ["profileId", "revision"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = ReferenceAsset::class, parentColumns = ["profileId", "hash"],
        childColumns = ["profileId", "assetHash"], deferred = true)
], indices = [Index(value = ["profileId", "assetHash"])])
data class StyleReference(val profileId: Long, val revision: Long, val position: Int, val assetHash: String)

/** A usable persistence contract, not a staging gallery or a reinterpretation of AI2 journals. */
@Entity(foreignKeys = [
    ForeignKey(entity = StyleRevision::class, parentColumns = ["profileId", "revision"],
        childColumns = ["profileId", "styleRevision"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = ReferenceAsset::class, parentColumns = ["profileId", "hash"],
        childColumns = ["profileId", "rawAssetHash"], deferred = true),
    ForeignKey(entity = ReferenceAsset::class, parentColumns = ["profileId", "hash"],
        childColumns = ["profileId", "editedAssetHash"], deferred = true)
], indices = [Index(value = ["profileId", "styleRevision"]),
    Index(value = ["profileId", "rawAssetHash"]), Index(value = ["profileId", "editedAssetHash"])])
data class IconAssetVariant(
    @PrimaryKey val id: String,
    val profileId: Long,
    val componentKey: String,
    val styleRevision: Long,
    val originalHash: String,
    val rawAssetHash: String,
    val editedAssetHash: String? = null,
    val origin: String = "GENERATED",
    val requestStatus: String = "COMPLETED",
    val reviewStatus: String = "UNREVIEWED",
    val applyStatus: String = "NOT_APPLIED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val trialContextJson: String? = null
)

data class ProjectAiSnapshot(
    val metadata: List<ProjectAiMetadata> = emptyList(),
    val revisions: List<StyleRevision> = emptyList(),
    val assets: List<ReferenceAsset> = emptyList(),
    val references: List<StyleReference> = emptyList(),
    val variants: List<IconAssetVariant> = emptyList(),
    val approvals: List<StyleTrialApproval> = emptyList()
) {
    fun forProfile(id: Long) = ProjectAiSnapshot(metadata.filter { it.profileId == id },
        revisions.filter { it.profileId == id }, assets.filter { it.profileId == id },
        references.filter { it.profileId == id }, variants.filter { it.profileId == id }, approvals.filter { it.profileId == id })

    fun rekey(oldId: Long, newId: Long) = forProfile(oldId).let { old ->
        val ids = old.variants.associate { it.id to java.util.UUID.randomUUID().toString() }
        ProjectAiSnapshot(
        old.metadata.map { it.copy(profileId = newId, sourceStamp = null,
            selectionJson = ProjectSelection.decode(it.selectionJson)?.copy(sourceStamp = null, needsRevalidation = true)?.encode().orEmpty()) },
        old.revisions.map { it.copy(profileId = newId,
            analysisEvidenceJson = it.analysisEvidenceJson?.let { evidence ->
                dev.renkinProject.renkin.ai.style.ReviewedAnalysisEvidence.parse(evidence).rekey(newId).encode()
            }) },
        old.assets.map { it.copy(profileId = newId, relativePath = ProjectAssetStore.path(newId, it.hash)) },
        old.references.map { it.copy(profileId = newId) },
        old.variants.map { row -> row.copy(id = ids.getValue(row.id), profileId = newId,
            trialContextJson = row.trialContextJson?.let { text ->
                dev.renkinProject.renkin.ai.trial.TrialVariantEvidence.parse(text).let {
                    it.copy(context = it.context.copy(profileId = newId), requestId = ids.getValue(row.id),
                        reviewContext = it.reviewContext?.copy(profileId = newId)).encode()
                }
            }) },
        old.approvals.map { row -> row.copy(profileId = newId, approvalId = java.util.UUID.randomUUID().toString(),
            contextJson = dev.renkinProject.renkin.ai.trial.TrialContext.parse(org.json.JSONObject(row.contextJson)).copy(profileId = newId).encode(),
            variantIdsJson = org.json.JSONArray(trialApprovalIds(row).map { ids.getValue(it) }).toString(), needsRevalidation = true) }
    ) }
}

@Dao
interface ProjectAiDao {
    @Query("SELECT * FROM ProjectAiMetadata WHERE profileId = :id")
    suspend fun metadata(id: Long): ProjectAiMetadata?
    @Query("SELECT * FROM StyleRevision WHERE profileId = :id ORDER BY revision")
    suspend fun history(id: Long): List<StyleRevision>
    @Query("SELECT * FROM ReferenceAsset WHERE profileId = :id")
    suspend fun assets(id: Long): List<ReferenceAsset>
    @Query("SELECT * FROM StyleReference WHERE profileId = :id ORDER BY revision, position")
    suspend fun references(id: Long): List<StyleReference>
    @Query("SELECT * FROM IconAssetVariant WHERE profileId = :id AND componentKey = :key ORDER BY createdAt, id")
    suspend fun variants(id: Long, key: String): List<IconAssetVariant>
    @Query("SELECT * FROM ProjectAiMetadata") suspend fun allMetadata(): List<ProjectAiMetadata>
    @Query("SELECT * FROM StyleRevision") suspend fun allRevisions(): List<StyleRevision>
    @Query("SELECT * FROM ReferenceAsset") suspend fun allAssets(): List<ReferenceAsset>
    @Query("SELECT * FROM StyleReference") suspend fun allReferences(): List<StyleReference>
    @Query("SELECT * FROM IconAssetVariant") suspend fun allVariants(): List<IconAssetVariant>
    @Query("SELECT * FROM StyleTrialApproval") suspend fun allApprovals(): List<StyleTrialApproval>
    @Query("SELECT * FROM StyleTrialApproval WHERE profileId = :id AND styleRevision = :revision")
    suspend fun approval(id: Long, revision: Long): StyleTrialApproval?
    @Query("SELECT * FROM IconAssetVariant WHERE id = :id") suspend fun variant(id: String): IconAssetVariant?
    @Query("UPDATE IconAssetVariant SET reviewStatus = :status, trialContextJson = :evidence, updatedAt = :time WHERE id = :id AND profileId = :profileId")
    suspend fun reviewVariant(profileId: Long, id: String, status: String, evidence: String, time: Long): Int
    @Upsert suspend fun putApprovals(rows: List<StyleTrialApproval>)
    @Upsert suspend fun putMetadata(row: ProjectAiMetadata)
    @Insert suspend fun insertRevisions(rows: List<StyleRevision>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAssets(rows: List<ReferenceAsset>)
    @Insert suspend fun insertReferences(rows: List<StyleReference>)
    @Insert suspend fun insertVariants(rows: List<IconAssetVariant>)
    @Query("DELETE FROM IconAssetVariant") suspend fun clearVariants()
    @Query("DELETE FROM StyleReference") suspend fun clearReferences()
    @Query("DELETE FROM StyleRevision") suspend fun clearRevisions()
    @Query("DELETE FROM ReferenceAsset") suspend fun clearAssets()
    @Query("DELETE FROM ProjectAiMetadata") suspend fun clearMetadata()
    @Query("DELETE FROM ReferenceAsset WHERE profileId = :id AND hash IN (:hashes) " +
        "AND NOT EXISTS (SELECT 1 FROM StyleReference WHERE StyleReference.profileId = ReferenceAsset.profileId AND StyleReference.assetHash = ReferenceAsset.hash) " +
        "AND NOT EXISTS (SELECT 1 FROM IconAssetVariant WHERE IconAssetVariant.profileId = ReferenceAsset.profileId AND (IconAssetVariant.rawAssetHash = ReferenceAsset.hash OR IconAssetVariant.editedAssetHash = ReferenceAsset.hash OR (IconAssetVariant.trialContextJson IS NOT NULL AND IconAssetVariant.originalHash = ReferenceAsset.hash)))")
    suspend fun discardUnlinked(id: Long, hashes: List<String>)
}
