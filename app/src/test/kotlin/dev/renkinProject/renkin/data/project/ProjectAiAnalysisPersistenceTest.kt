package dev.renkinProject.renkin.data.project

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.transfer.*
import dev.renkinProject.renkin.data.watch.WatchDatabase
import dev.renkinProject.renkin.data.watch.WatchRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectAiAnalysisPersistenceTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var context: Context
    private lateinit var source: RenkinPackDatabase
    private lateinit var target: RenkinPackDatabase
    private lateinit var sourceWatch: WatchDatabase
    private lateinit var targetWatch: WatchDatabase
    private lateinit var src: ProjectAiRepository
    private lateinit var dst: ProjectAiRepository
    @Before fun setup() = runBlocking {
        context = analysisFixtureContext(temporary.root)
        fun db() = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).allowMainThreadQueries().build()
        fun watch() = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).allowMainThreadQueries().build()
        source = db(); target = db(); sourceWatch = watch(); targetWatch = watch()
        src = ProjectAiRepository(source, ProjectAssetStore(context)); dst = ProjectAiRepository(target, ProjectAssetStore(context))
        RenkinPackRepository(source).replaceEverything(listOf(Profile(1, "Default"), Profile(4, "Same")), emptyList())
        RenkinPackRepository(target).replaceEverything(listOf(Profile(1, "Keep")), emptyList())
    }
    @After fun close() { source.close(); target.close(); sourceWatch.close(); targetWatch.close()
        File(context.filesDir, "project_ai_assets").deleteRecursively() }
    private fun manager(db: RenkinPackDatabase, watch: WatchDatabase) = BackupManager(context, RenkinPackRepository(db), WatchRepository(watch))
    private suspend fun save(edited: Boolean = false): SavedProjectStyle {
        val request = analysisFixtureRequest(); val png = request.images.single().png
        val asset = src.importReference(4, ByteArrayInputStream(png)); val style = analysisFixtureStyle()
        val evidence = ReviewedAnalysisEvidence(request.requestId, request.fingerprint, request.promptHash, request.context,
            request.model, listOf(asset.hash), listOf(asset.hash), "resp_fixture", sha256(style.encode().toByteArray()),
            System.currentTimeMillis(), edited, false)
        return src.saveAnalyzedStyle(4, null, 0, if (edited) "corrected charcoal fill" else PromptComposer.specification(style),
            "manual instruction preserved", listOf(asset.hash), style.encode(), evidence)
    }
    private suspend fun export(share: Boolean = false): ByteArray = ByteArrayOutputStream().let {
        if (share) manager(source, sourceWatch).exportProfile(4) { it } else manager(source, sourceWatch).exportBackup { it }; it.toByteArray()
    }
    private fun entries(bytes: ByteArray): Map<String, ByteArray> = linkedMapOf<String, ByteArray>().apply {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip -> var entry = zip.nextEntry
            while (entry != null) { put(entry.name, zip.readBytes()); entry = zip.nextEntry } }
    }
    private fun archive(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().let { output ->
        ZipOutputStream(output).use { zip -> entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() } }; output.toByteArray()
    }
    @Test fun typedAnalysisRevisionIsImmutableIdempotentAndHistoryPinsRealPng() = runBlocking {
        val saved = save(); val revision = saved.revision!!
        val evidence = ReviewedAnalysisEvidence.parse(revision.analysisEvidenceJson!!)
        assertEquals("AUTO_ANALYSIS", revision.origin)
        assertEquals(analysisFixtureStyle(), StyleProfileV1.parse(revision.analysisProfileJson!!))
        val same = src.saveAnalyzedStyle(4, null, 0, revision.specification, revision.userInstruction,
            saved.references.map { it.hash }, revision.analysisProfileJson!!, evidence)
        assertEquals(saved, same)
        src.saveStyle(4, 1, "later manual style", "", emptyList())
        src.discardDraftReferences(4, saved.references.map { it.hash }); src.collectUnreferencedFiles()
        assertEquals(revision, src.history(4).first()); assertArrayEquals(analysisFixturePng(), src.referenceBytes(4, saved.references.single().hash))
        assertEquals("USER", src.load(4).revision!!.origin)
    }
    @Test fun manualOverridesCannotClaimAutoOriginAndStaleMetadataCannotWrite() = runBlocking {
        val saved = save(edited = true); val evidence = ReviewedAnalysisEvidence.parse(saved.revision!!.analysisEvidenceJson!!)
        assertEquals("USER", saved.revision!!.origin); assertTrue(evidence.userEdited)
        assertTrue(runCatching { src.saveAnalyzedStyle(4, 1, 0, "changed again", "", saved.references.map { it.hash },
            saved.revision!!.analysisProfileJson!!, evidence) }.isFailure)
        assertEquals(saved, src.load(4)); assertEquals(1, src.history(4).size)
    }
    @Test fun foreignOwnerMissingRefAlteredResponseAndFalseProvenanceRejectBeforeMutation() = runBlocking {
        val saved = save(); val revision = saved.revision!!; val evidence = ReviewedAnalysisEvidence.parse(revision.analysisEvidenceJson!!)
        val before = src.snapshot()
        assertTrue(runCatching { src.saveAnalyzedStyle(1, null, 0, revision.specification, "", emptyList(), revision.analysisProfileJson!!, evidence) }.isFailure)
        assertTrue(runCatching { src.saveAnalyzedStyle(4, 1, 1, "fake auto text", "", saved.references.map { it.hash }, revision.analysisProfileJson!!, evidence) }.isFailure)
        assertTrue(runCatching { src.saveAnalyzedStyle(4, 1, 1, revision.specification, "", emptyList(), revision.analysisProfileJson!!, evidence) }.isFailure)
        assertTrue(runCatching { src.saveAnalyzedStyle(4, 1, 1, revision.specification, "", saved.references.map { it.hash },
            revision.analysisProfileJson!!, evidence.copy(responseHash = "a".repeat(64))) }.isFailure)
        assertEquals(before, src.snapshot())
    }
    @Test fun fullFormatFiveBackupRestoresStructuredStyleRefsAndRequiresFreshValidation() = runBlocking {
        val saved = save(); val journal = StyleAnalysisJournal(context); val candidate = analysisFixtureRequest()
        check(journal.claim(candidate))
        journal.finish(candidate.context.profileId, candidate.requestId, StyleAnalysisOutcome.Suggested(analysisFixtureStyle(), "resp_unreviewed", null))
        journal.release(candidate.context.profileId, candidate.requestId)
        val bytes = export(); val zipped = entries(bytes)
        assertEquals(6, JSONObject(String(zipped.getValue("manifest.json"))).getInt("format"))
        assertFalse(zipped.keys.any { it.contains("style_analysis") })
        assertFalse(zipped.values.any { String(it).contains("resp_unreviewed") })
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        val restored = dst.load(4)
        assertEquals(saved.revision!!.specification, restored.revision!!.specification)
        assertEquals(saved.revision!!.analysisProfileJson, restored.revision!!.analysisProfileJson)
        val evidence = ReviewedAnalysisEvidence.parse(restored.revision!!.analysisEvidenceJson!!)
        assertTrue(evidence.needsRevalidation); assertEquals(4L, evidence.context.profileId)
        assertArrayEquals(candidate.images.single().png, dst.files.read(restored.references.single()))
        assertEquals(AnalysisStatus.FAILED, journal.latest(4)!!.status)
        assertNotEquals(candidate.context.storageEpoch, journal.epoch(4))
    }
    @Test fun profileShareRekeysProvenanceAndKeepsEarlierDestinationStyle() = runBlocking {
        val saved = save(); dst.saveStyle(1, null, "keep", "", emptyList())
        val bytes = export(share = true); val zipped = entries(bytes)
        assertFalse(zipped.keys.any { it.startsWith("keystore/") || it.contains("style_analysis") })
        val id = checkNotNull(manager(target, targetWatch).importFile { ByteArrayInputStream(bytes) }.importedProfileId)
        val restored = dst.load(id); val evidence = ReviewedAnalysisEvidence.parse(restored.revision!!.analysisEvidenceJson!!)
        assertNotEquals(4L, id); assertEquals(id, evidence.context.profileId); assertTrue(evidence.needsRevalidation)
        assertEquals(saved.revision!!.analysisProfileJson, restored.revision!!.analysisProfileJson)
        assertEquals(ProjectAssetStore.path(id, saved.references.single().hash), restored.references.single().relativePath)
        assertEquals("keep", dst.load(1).revision!!.specification)
        assertTrue(runCatching { dst.saveAnalyzedStyle(id, 1, 1, restored.revision!!.specification, "", restored.references.map { it.hash },
            restored.revision!!.analysisProfileJson!!, evidence) }.isFailure)
    }
    @Test fun formatsThreeAndFourRemainReadableAndDoNotInferAnalysisEvidence() = runBlocking {
        src.saveStyle(4, null, "legacy manual style", "", emptyList())
        val current = entries(export())
        for (format in listOf(3, 4)) {
            val data = JSONObject(String(current.getValue("data.json")))
            if (format == 3) data.remove("projectAi") else {
                val project = data.getJSONObject("projectAi").put("schema", 1)
                val revisions = project.getJSONArray("revisions")
                for (i in 0 until revisions.length()) { revisions.getJSONObject(i).remove("analysisProfileJson"); revisions.getJSONObject(i).remove("analysisEvidenceJson") }
            }
            val bytes = archive(current + ("manifest.json" to "{\"format\":$format,\"kind\":\"backup\"}".toByteArray()) + ("data.json" to data.toString().toByteArray()))
            manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
            if (format == 3) assertNull(dst.load(4).revision) else {
                assertEquals("legacy manual style", dst.load(4).revision!!.specification)
                assertNull(dst.load(4).revision!!.analysisEvidenceJson); assertEquals("USER", dst.load(4).revision!!.origin)
            }
        }
    }
    @Test fun invalidPortableEvidenceAndForeignSampleHashRejectBeforeRestoreChangesData() = runBlocking {
        save(); dst.saveStyle(1, null, "keep", "", emptyList()); val before = dst.snapshot()
        val original = entries(export()); val data = JSONObject(String(original.getValue("data.json")))
        val revision = data.getJSONObject("projectAi").getJSONArray("revisions").getJSONObject(0)
        val evidence = JSONObject(revision.getString("analysisEvidenceJson")).put("responseHash", "f".repeat(64))
        revision.put("analysisEvidenceJson", evidence.toString())
        val corrupt = archive(original + ("data.json" to data.toString().toByteArray()))
        assertTrue(runCatching { manager(target, targetWatch).importBackup { ByteArrayInputStream(corrupt) } }.isFailure)
        assertEquals(before, dst.snapshot()); assertEquals("Keep", RenkinPackRepository(target).profile(1)!!.name)
    }
    @Test fun profileDeletionCascadesAcceptedHistoryWithoutLeakingToReusedIdentity() = runBlocking {
        save(); val hash = src.load(4).references.single().hash
        RenkinPackRepository(source).deleteProfile(4); src.collectUnreferencedFiles()
        assertTrue(src.snapshot().revisions.isEmpty()); assertTrue(src.snapshot().assets.isEmpty())
        assertFalse(File(context.filesDir, "project_ai_assets/4/$hash.png").exists())
        RenkinPackRepository(source).createProfile(Profile(4, "Replacement")); assertNull(src.load(4).revision)
    }
    @Test fun genuineV19FileMigratesToV20WithNullEvidenceAndIdenticalFreshRoomIdentity() = runBlocking {
        val name = "analysis-v19-${System.nanoTime()}"; val fresh = "analysis-v20-${System.nanoTime()}"
        val asset = src.files.install(4, analysisFixturePng())
        val className = checkNotNull(RenkinPackDatabase::class.java.canonicalName)
        createDatabaseFromExportedSchema(context, className, 19, name) { old ->
            old.execSQL("INSERT INTO Profile VALUES (1, 'Default', '', '', '', 0, 0)")
            old.execSQL("INSERT INTO Profile VALUES (4, 'Same', 'preserve description', 'pack label', 'prefs', 1, 1)")
            old.execSQL("INSERT INTO ProjectAiMetadata VALUES (4, 'CREATE', NULL, NULL, 3, '', 9, 55, 66)")
            old.execSQL("INSERT INTO StyleRevision VALUES (4, 3, 1, 'v19 immutable manual', 'keep instruction', 'USER', 100)")
            old.execSQL("INSERT INTO ReferenceAsset VALUES (?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(asset.profileId, asset.hash, asset.relativePath,
                asset.byteCount, asset.width, asset.height, asset.source, asset.createdAt))
            old.execSQL("INSERT INTO StyleReference VALUES (4, 3, 0, ?)", arrayOf(asset.hash))
            old.execSQL("INSERT INTO IconAssetVariant VALUES ('legacy-variant', 4, 'pkg/Main', 3, ?, ?, NULL, 'GENERATED', 'COMPLETED', 'UNREVIEWED', 'NOT_APPLIED', 101, 102)", arrayOf(asset.hash, asset.hash))
            old.execSQL("INSERT INTO BuiltIcon VALUES (4, 'pkg', 'Main', 'build-hash')")
            old.query("PRAGMA table_info(StyleRevision)").use { assertEquals(7, it.count) }
        }
        // SQLiteOpenHelper sets user_version after onCreate/population returns.
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            assertEquals(19, old.version)
            old.rawQuery("PRAGMA index_list(StyleReference)", null).use { indexes ->
                var found = false
                while (indexes.moveToNext()) {
                    if (indexes.getString(indexes.getColumnIndexOrThrow("name")) == "index_StyleReference_profileId_assetHash") found = true
                }
                assertTrue("The v19 fixture must contain its actual exported index", found)
            }
        }
        try {
            var identity = ""
            RenkinPackDatabase.open(context, name).useDatabase { migrated ->
                assertEquals(21, migrated.openHelper.writableDatabase.version)
                val repo = ProjectAiRepository(migrated, ProjectAssetStore(context)); val style = repo.load(4)
                assertEquals(StyleRevision(4, 3, 1, "v19 immutable manual", "keep instruction", "USER", 100), style.revision)
                assertNull(style.revision!!.analysisProfileJson); assertNull(style.revision!!.analysisEvidenceJson)
                assertEquals(9L, style.metadata!!.version); assertEquals(asset, style.references.single())
                assertEquals("legacy-variant", repo.variants(4, "pkg/Main").single().id)
                assertEquals("build-hash", RenkinPackRepository(migrated).builtIcons(4).single().iconHash)
                migrated.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
                migrated.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id=42").use { assertTrue(it.moveToFirst()); identity = it.getString(0) }
            }
            RenkinPackDatabase.open(context, name).useDatabase { assertEquals("v19 immutable manual", ProjectAiRepository(it, ProjectAssetStore(context)).load(4).revision!!.specification) }
            RenkinPackDatabase.open(context, fresh).useDatabase { db ->
                RenkinPackRepository(db).ensureDefaultProfile()
                db.openHelper.writableDatabase.query("SELECT identity_hash FROM room_master_table WHERE id=42").use { assertTrue(it.moveToFirst()); assertEquals(identity, it.getString(0)) }
            }
            val schema = listOf(File("app/schemas"), File("schemas")).map { File(it, "$className/21.json") }.first(File::isFile)
            assertEquals(identity, JSONObject(schema.readText()).getJSONObject("database").getString("identityHash"))
        } finally { context.deleteDatabase(name); context.deleteDatabase(fresh) }
    }
}
