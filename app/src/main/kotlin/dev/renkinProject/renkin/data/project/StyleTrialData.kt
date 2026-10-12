package dev.renkinProject.renkin.data.project

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.sqlite.db.SupportSQLiteDatabase

/** Readiness is an explicit decision about exact reviewed images, never future cost consent. */
@Entity(primaryKeys = ["profileId", "styleRevision"], foreignKeys = [ForeignKey(
    entity = StyleRevision::class, parentColumns = ["profileId", "revision"],
    childColumns = ["profileId", "styleRevision"], onDelete = ForeignKey.CASCADE)])
data class StyleTrialApproval(val profileId: Long, val styleRevision: Long, val approvalId: String,
    val contextJson: String, val variantIdsJson: String, val approvedAt: Long,
    val needsRevalidation: Boolean = false)

data class AcceptedTrialVariant(val profileId: Long, val componentKey: String, val styleRevision: Long,
    val originalHash: String, val referenceFingerprint: String, val assetHash: String,
    val approvedAt: Long, val variantId: String)

internal object StyleTrialSchema {
    fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `IconAssetVariant` ADD COLUMN `trialContextJson` TEXT")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `StyleTrialApproval` (`profileId` INTEGER NOT NULL,
            `styleRevision` INTEGER NOT NULL, `approvalId` TEXT NOT NULL, `contextJson` TEXT NOT NULL,
            `variantIdsJson` TEXT NOT NULL, `approvedAt` INTEGER NOT NULL, `needsRevalidation` INTEGER NOT NULL,
            PRIMARY KEY(`profileId`, `styleRevision`),
            FOREIGN KEY(`profileId`, `styleRevision`) REFERENCES `StyleRevision`(`profileId`, `revision`)
            ON UPDATE NO ACTION ON DELETE CASCADE)""")
    }
}
