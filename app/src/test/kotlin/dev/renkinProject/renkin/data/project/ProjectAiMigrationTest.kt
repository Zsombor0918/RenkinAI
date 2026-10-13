package dev.renkinProject.renkin.data.project

import android.app.Application
import android.content.Context
import dev.renkinProject.renkin.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectAiMigrationTest {
    @Test fun physicalV18FilePreservesLegacyRowsAndReopensWithValidatedCurrentIdentity() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val name = "project-ai-v18-migration"
        val freshName = "project-ai-v19-fresh"
        val legacyProfile = Profile(4, "Same", "keep description", "Keep pack", "{\"global\":true}", true, true)
        val expected = DbApplication("pkg", "Main", true, false, "ai2-quick-applied-pixels",
            true, "day_", "locked.pack", "locked.pack", 4, "mapped_drawable", true, false,
            "immutable-original", false, true, "https://example.test/attribution", true)
        createDatabaseFromExportedSchema(context, checkNotNull(RenkinPackDatabase::class.java.canonicalName), 18, name) { old ->
            old.execSQL("INSERT INTO Profile (id, name, description, packLabel, prefsSnapshot, hasUnbuiltChanges, hideMissingPackWarning) VALUES (1, 'Same', '', '', '', 0, 0)")
            old.execSQL("INSERT INTO Profile (id, name, description, packLabel, prefsSnapshot, hasUnbuiltChanges, hideMissingPackWarning) VALUES (4, 'Same', 'keep description', 'Keep pack', '{\"global\":true}', 1, 1)")
            old.execSQL("INSERT INTO DbApplication (packageName, activityName, isAdaptiveIcon, isXml, drawable, calendarEnabled, calendarPrefix, calendarPackName, sourcePackName, profileId, sourceDrawableName, isCustomIcon, isLegacyIcon, baseDrawable, baseIsAdaptiveIcon, baseIsXml, sourceUrl, isFallbackIcon) VALUES ('pkg', 'Main', 1, 0, 'ai2-quick-applied-pixels', 1, 'day_', 'locked.pack', 'locked.pack', 4, 'mapped_drawable', 1, 0, 'immutable-original', 0, 1, 'https://example.test/attribution', 1)")
            old.execSQL("INSERT INTO BuiltIcon VALUES (4, 'pkg', 'Main', 'last-built-hash')")
            old.execSQL("INSERT INTO ColorPreset VALUES (8, 'Colour', 'gradient', 11)")
            old.execSQL("INSERT INTO ModifierPreset VALUES (9, 'Modifier', 'v=1', 1, 12, 13)")
            old.execSQL("INSERT INTO PackVerdict VALUES ('locked.pack', 'paid', 'Keep label', 0, 14)")
        }
        try {
            var identity = ""
            RenkinPackDatabase.open(context, name).useDatabase { db ->
                val packs = RenkinPackRepository(db)
                assertEquals(21, db.openHelper.writableDatabase.version)
                assertEquals(legacyProfile, packs.profile(4))
                assertEquals(listOf(expected), packs.getAll(4))
                assertEquals("last-built-hash", packs.builtIcons(4).single().iconHash)
                assertEquals(ColorPreset(8, "Colour", "gradient", 11), packs.allColorPresets().single())
                assertEquals(ModifierPreset(9, "Modifier", "v=1", 1, 12, 13), packs.allModifierPresets().single())
                assertFalse(packs.verdicts(listOf("locked.pack")).getValue("locked.pack").seenInstalled)
                assertEquals("paid", packs.verdicts(listOf("locked.pack")).getValue("locked.pack").verdict)
                val repository = ProjectAiRepository(db, ProjectAssetStore(context))
                assertTrue(repository.snapshot().revisions.isEmpty())
                val reference = repository.importReference(4, ByteArrayInputStream(projectTestPng()))
                repository.saveStyle(4, null, "new project style", "", listOf(reference.hash))
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
                db.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
                    assertTrue(it.moveToFirst()); identity = it.getString(0)
                }
            }
            RenkinPackDatabase.open(context, name).useDatabase { db ->
                val repository = ProjectAiRepository(db, ProjectAssetStore(context))
                assertEquals("new project style", repository.load(4).revision!!.specification)
                assertEquals(1, repository.load(4).references.size)
                assertEquals(listOf(expected), RenkinPackRepository(db).getAll(4))
                assertNull(repository.load(1).revision)
            }
            RenkinPackDatabase.open(context, freshName).useDatabase { db ->
                assertNotNull(RenkinPackRepository(db).ensureDefaultProfile())
                db.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
                    assertTrue(it.moveToFirst()); assertEquals(identity, it.getString(0))
                }
            }
        } finally {
            context.deleteDatabase(name); context.deleteDatabase(freshName)
            File(context.filesDir, "project_ai_assets").deleteRecursively()
        }
    }
}
