package dev.renkinProject.renkin.data.project

import android.app.Application
import android.content.Context
import dev.renkinProject.renkin.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class StyleTrialMigrationTest {
    @Test fun physicalExportedV20MigratesWithoutLosingOldRowsAndMatchesGenuineFreshV21() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication(); val name = "trial-v20-migration"; val fresh = "trial-v21-fresh"
        val className = checkNotNull(RenkinPackDatabase::class.java.canonicalName)
        createDatabaseFromExportedSchema(context, className, 20, name) { db ->
            db.execSQL("INSERT INTO Profile (id,name,description,packLabel,prefsSnapshot,hasUnbuiltChanges,hideMissingPackWarning) VALUES (1,'Default','','','',0,0)")
            db.execSQL("INSERT INTO StyleRevision VALUES (1,7,1,'v20 saved style','separate instruction','USER',100,NULL,NULL)")
            db.execSQL("INSERT INTO ProjectAiMetadata VALUES (1,'CREATE',NULL,NULL,7,'',4,100,101)")
            db.execSQL("INSERT INTO DbApplication (packageName,activityName,isAdaptiveIcon,isXml,drawable,calendarEnabled,profileId,isCustomIcon,isLegacyIcon,baseIsAdaptiveIcon,baseIsXml,isFallbackIcon) VALUES ('keep','Main',0,0,'manual-pixels',0,1,1,0,0,0,0)")
        }
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { assertEquals(20, it.version) }
        try {
            var identity = ""
            RenkinPackDatabase.open(context, name).useDatabase { db ->
                assertEquals(21, db.openHelper.writableDatabase.version)
                val repo = ProjectAiRepository(db, ProjectAssetStore(context))
                assertEquals("v20 saved style", repo.load(1).revision!!.specification)
                assertEquals("separate instruction", repo.load(1).revision!!.userInstruction)
                assertEquals(4L, repo.metadata(1)!!.version); assertTrue(repo.snapshot().approvals.isEmpty())
                assertEquals("manual-pixels", RenkinPackRepository(db).getAll(1).single().drawable)
                db.openHelper.writableDatabase.query("PRAGMA table_info(IconAssetVariant)").use { assertEquals(14, it.count) }
                db.openHelper.writableDatabase.query("PRAGMA table_info(StyleTrialApproval)").use { assertEquals(7, it.count) }
                db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
                db.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id=42").use { assertTrue(it.moveToFirst()); identity = it.getString(0) }
            }
            RenkinPackDatabase.open(context, fresh).useDatabase { db ->
                RenkinPackRepository(db).ensureDefaultProfile()
                db.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id=42").use { assertTrue(it.moveToFirst()); assertEquals(identity, it.getString(0)) }
            }
            val schema = listOf(File("app/schemas"), File("schemas")).map { File(it, "$className/21.json") }.first(File::isFile)
            assertEquals(identity, JSONObject(schema.readText()).getJSONObject("database").getString("identityHash"))
        } finally { context.deleteDatabase(name); context.deleteDatabase(fresh) }
    }
}
