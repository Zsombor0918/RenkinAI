package dev.renkinProject.renkin.data.project

import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive v18 -> v19 migration. Existing Renkin tables and pixels are never rebuilt here. */
internal object ProjectAiSchema {
    fun create(db: SupportSQLiteDatabase) {
        val statements = listOf(
            """CREATE TABLE IF NOT EXISTS `ProjectAiMetadata` (`profileId` INTEGER NOT NULL,
                `mode` TEXT NOT NULL, `sourcePackage` TEXT, `sourceStamp` TEXT, `activeRevision` INTEGER,
                `selectionJson` TEXT NOT NULL, `version` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`profileId`),
                FOREIGN KEY(`profileId`) REFERENCES `Profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            """CREATE TABLE IF NOT EXISTS `StyleRevision` (`profileId` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL, `schemaVersion` INTEGER NOT NULL, `specification` TEXT NOT NULL,
                `userInstruction` TEXT NOT NULL, `origin` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `revision`),
                FOREIGN KEY(`profileId`) REFERENCES `Profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            """CREATE TABLE IF NOT EXISTS `ReferenceAsset` (`profileId` INTEGER NOT NULL,
                `hash` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `byteCount` INTEGER NOT NULL,
                `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `hash`),
                FOREIGN KEY(`profileId`) REFERENCES `Profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
            """CREATE TABLE IF NOT EXISTS `StyleReference` (`profileId` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL, `position` INTEGER NOT NULL, `assetHash` TEXT NOT NULL,
                PRIMARY KEY(`profileId`, `revision`, `position`),
                FOREIGN KEY(`profileId`, `revision`) REFERENCES `StyleRevision`(`profileId`, `revision`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`profileId`, `assetHash`) REFERENCES `ReferenceAsset`(`profileId`, `hash`) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)""",
            "CREATE INDEX IF NOT EXISTS `index_StyleReference_profileId_assetHash` ON `StyleReference` (`profileId`, `assetHash`)",
            """CREATE TABLE IF NOT EXISTS `IconAssetVariant` (`id` TEXT NOT NULL,
                `profileId` INTEGER NOT NULL, `componentKey` TEXT NOT NULL, `styleRevision` INTEGER NOT NULL,
                `originalHash` TEXT NOT NULL, `rawAssetHash` TEXT NOT NULL, `editedAssetHash` TEXT,
                `origin` TEXT NOT NULL, `requestStatus` TEXT NOT NULL, `reviewStatus` TEXT NOT NULL,
                `applyStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`profileId`, `styleRevision`) REFERENCES `StyleRevision`(`profileId`, `revision`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`profileId`, `rawAssetHash`) REFERENCES `ReferenceAsset`(`profileId`, `hash`) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED,
                FOREIGN KEY(`profileId`, `editedAssetHash`) REFERENCES `ReferenceAsset`(`profileId`, `hash`) ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)""",
            "CREATE INDEX IF NOT EXISTS `index_IconAssetVariant_profileId_styleRevision` ON `IconAssetVariant` (`profileId`, `styleRevision`)",
            "CREATE INDEX IF NOT EXISTS `index_IconAssetVariant_profileId_rawAssetHash` ON `IconAssetVariant` (`profileId`, `rawAssetHash`)",
            "CREATE INDEX IF NOT EXISTS `index_IconAssetVariant_profileId_editedAssetHash` ON `IconAssetVariant` (`profileId`, `editedAssetHash`)"
        )
        statements.forEach(db::execSQL)
    }
}
