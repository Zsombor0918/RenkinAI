package dev.renkinProject.renkin.data.transfer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.IconGenerationGateway
import dev.renkinProject.renkin.ai.IconGenerationOutcome
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.data.watch.WatchDatabase
import dev.renkinProject.renkin.data.watch.WatchRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
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
class ProjectAiBackupTest {
    private lateinit var context: Context
    private lateinit var source: RenkinPackDatabase
    private lateinit var target: RenkinPackDatabase
    private lateinit var sourceWatch: WatchDatabase
    private lateinit var targetWatch: WatchDatabase
    private lateinit var src: ProjectAiRepository
    private lateinit var dst: ProjectAiRepository
    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        fun db() = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).allowMainThreadQueries().build()
        fun watch() = Room.inMemoryDatabaseBuilder(context, WatchDatabase::class.java).allowMainThreadQueries().build()
        source = db(); target = db(); sourceWatch = watch(); targetWatch = watch()
        src = ProjectAiRepository(source, ProjectAssetStore(context)); dst = ProjectAiRepository(target, ProjectAssetStore(context))
        RenkinPackRepository(source).replaceEverything(listOf(Profile(1, "Default"), Profile(4, "Same")), emptyList())
        RenkinPackRepository(target).replaceEverything(listOf(Profile(1, "Keep")), emptyList())
    }
    @After fun close() {
        source.close(); target.close(); sourceWatch.close(); targetWatch.close()
        File(context.filesDir, "project_ai_assets").deleteRecursively()
    }
    private fun manager(db: RenkinPackDatabase, watch: WatchDatabase) = BackupManager(context, RenkinPackRepository(db), WatchRepository(watch))
    private suspend fun saved(id: Long = 4): ReferenceAsset {
        val asset = src.importReference(id, ByteArrayInputStream(projectTestPng()))
        src.saveStyle(id, null, "outline", "project instruction", listOf(asset.hash))
        return asset
    }
    private suspend fun savedTrial(): Pair<StyleTrialPlan, IconAssetVariant> {
        saved()
        val catalog = "a".repeat(64)
        src.saveSelection(4, 1, ProjectSelection("CREATE", null, null, catalog, listOf("pkg/Main")))
        val style = src.load(4); val meta = checkNotNull(style.metadata); val journal = StyleTrialJournal(context)
        val original = projectTestPng(Color.BLUE, 1024)
        val input = TrialInput("pkg/Main", "App", sha256(original), "launcher:pkg/Main:1")
        val ctx = TrialContext(4, 1, meta.version, "CREATE", null, null, catalog, sha256(meta.selectionJson.toByteArray()),
            referenceFingerprint(style.references.map { it.hash }), journal.epoch(4), AiSettings.OPENAI, AiSettings.MODELS.first(), "medium",
            styleFingerprint(style.revision!!.specification, style.revision.userInstruction))
        val plan = StyleTrialPlan(ctx, style.revision!!.specification, style.revision.userInstruction,
            style.references.map { it.hash }, listOf(input))
        val gateway = IconGenerationGateway { request ->
            request.beforeEndpoint(1)
            IconGenerationOutcome.Generated(projectTestPng(size = 1024), request.requestId, request.provider, 1024, 1024, 1)
        }
        StyleTrialCoordinator(journal, gateway, src).submit(plan,
            style.references.associate { it.hash to src.referenceBytes(4, it.hash) } + (input.originalHash to original), true, { true })
        val row = src.trialVariants(4).single()
        val originals = mapOf("pkg/Main" to (input.originalHash to input.origin))
        src.reviewTrial(ctx, row.id, true, originals.getValue("pkg/Main"), { true }); src.approveTrials(ctx, originals, { true })
        return plan to row
    }
    private suspend fun export(profile: Boolean = false): ByteArray = ByteArrayOutputStream().let {
        if (profile) manager(source, sourceWatch).exportProfile(4) { it } else manager(source, sourceWatch).exportBackup { it }
        it.toByteArray()
    }
    private fun entries(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { result[entry.name] = zip.readBytes(); entry = zip.nextEntry }
        }
        return result
    }
    private fun archive(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().let { output ->
        ZipOutputStream(output).use { zip -> entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() } }
        output.toByteArray()
    }
    private suspend fun expectRejected(bytes: ByteArray) {
        dst.saveStyle(1, null, "keep saved style", "", emptyList())
        val before = dst.snapshot()
        assertTrue(runCatching { manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) } }.isFailure)
        assertEquals(before, dst.snapshot())
        assertEquals("Keep", RenkinPackRepository(target).profile(1)!!.name)
        assertTrue(context.cacheDir.listFiles().orEmpty().none { it.name.startsWith("renkin-restore-") })
    }

    @Test fun trialFullBackupKeepsRawAndOriginalButInvalidatesApprovalAndLocalRequestEpoch() = runBlocking {
        val (plan, row) = savedTrial(); val before = src.snapshot(); val bytes = export()
        val zip = entries(bytes)
        assertEquals(6, JSONObject(String(zip.getValue("manifest.json"))).getInt("format"))
        assertFalse(zip.keys.any { it.contains("style_trials") || it.contains("credential") })
        assertTrue(zip.containsKey("project_ai_assets/${ProjectAssetStore.path(4, row.originalHash)}"))
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        assertEquals(before.copy(approvals = before.approvals.map { it.copy(needsRevalidation = true) }), dst.snapshot())
        assertFalse(StyleTrialJournal(context).current(plan))
        assertArrayEquals(projectTestPng(size = 1024), dst.referenceBytes(4, row.rawAssetHash))
        assertTrue(dst.acceptedTrials(plan.context, mapOf("pkg/Main" to (row.originalHash to plan.inputs.single().origin)), { true }).isEmpty())
    }

    @Test fun trialProfileShareRekeysVariantAndApprovalIdsWithoutTransferringConsent() = runBlocking {
        val (plan, row) = savedTrial(); val bytes = export(profile = true)
        val result = manager(target, targetWatch).importFile { ByteArrayInputStream(bytes) }
        val id = checkNotNull(result.importedProfileId)
        val snap = dst.snapshot().forProfile(id); val imported = snap.variants.single(); val approval = snap.approvals.single()
        assertNotEquals(row.id, imported.id); assertTrue(approval.needsRevalidation)
        assertEquals(listOf(imported.id), trialApprovalIds(approval))
        assertEquals(id, TrialVariantEvidence.parse(imported.trialContextJson!!).context.profileId)
        assertArrayEquals(projectTestPng(Color.BLUE, 1024), dst.referenceBytes(id, imported.originalHash))
        assertArrayEquals(projectTestPng(size = 1024), dst.referenceBytes(id, imported.rawAssetHash))
        assertEquals("ACCEPTED", imported.reviewStatus); assertEquals("NOT_APPLIED", imported.applyStatus)
        assertTrue(StyleTrialJournal(context).records(id).isEmpty())
        val meta = checkNotNull(dst.metadata(id))
        val checked = plan.context.copy(profileId = id, metadataVersion = meta.version,
            selectionHash = sha256(meta.selectionJson.toByteArray()), epoch = StyleTrialJournal(context).epoch(id))
        val originals = mapOf("pkg/Main" to (imported.originalHash to "launcher:pkg/Main:rechecked-on-import"))
        assertTrue(dst.acceptedTrials(checked, originals, { true }).isEmpty())
        dst.reviewTrial(checked, imported.id, true, originals.getValue("pkg/Main"), { true })
        assertTrue(dst.acceptedTrials(checked, originals, { true }).isEmpty())
        dst.approveTrials(checked, originals, { true })
        assertEquals(imported.id, dst.acceptedTrials(checked, originals, { true }).single().variantId)
        assertTrue(StyleTrialJournal(context).records(id).isEmpty())
    }

    @Test fun oldFormatFivePayloadTwoRetainsPngHistoryWithoutInventingTrialApproval() = runBlocking {
        val (_, row) = savedTrial(); val zip = entries(export())
        val root = JSONObject(String(zip.getValue("data.json")))
        val ai = root.getJSONObject("projectAi").put("schema", 2); ai.remove("approvals")
        val variants = ai.getJSONArray("variants"); for (i in 0 until variants.length()) variants.getJSONObject(i).remove("trialContextJson")
        zip["data.json"] = root.toString().toByteArray()
        zip["manifest.json"] = JSONObject(String(zip.getValue("manifest.json"))).put("format", 5).toString().toByteArray()
        manager(target, targetWatch).importBackup { ByteArrayInputStream(archive(zip)) }
        assertTrue(dst.snapshot().approvals.isEmpty()); assertNull(dst.trialVariants(4).firstOrNull())
        assertEquals("ACCEPTED", dst.variants(4, "pkg/Main").single().reviewStatus)
        assertArrayEquals(projectTestPng(size = 1024), dst.referenceBytes(4, row.rawAssetHash))
    }

    @Test fun fullBackupRestoresHistoryReferencesVariantsAndSelectionFromActualPngBytes() = runBlocking {
        val first = saved()
        val second = src.importReference(4, ByteArrayInputStream(projectTestPng(Color.RED)))
        src.saveStyle(4, 1, "changed style", "", listOf(second.hash, first.hash))
        src.insertVariant(IconAssetVariant("variant", 4, "pkg/Main", 1, first.hash, second.hash))
        src.saveSelection(4, 2, ProjectSelection("EXTEND", "pack.source", "a".repeat(64), "b".repeat(64), listOf("pkg/Main")))
        val expected = src.snapshot()
        val bytes = export()
        File(context.filesDir, "project_ai_assets").deleteRecursively()
        var opens = 0
        manager(target, targetWatch).importBackup { opens++; ByteArrayInputStream(bytes) }
        assertEquals(1, opens) // URI content is not trusted twice
        assertEquals(expected, dst.snapshot())
        assertEquals(listOf(second.hash, first.hash), dst.load(4).references.map { it.hash })
        assertArrayEquals(projectTestPng(), dst.files.read(first))
        assertTrue(ProjectSelection.decode(dst.metadata(4)!!.selectionJson)!!.needsRevalidation)
        assertEquals("UNREVIEWED", dst.variants(4, "pkg/Main").single().reviewStatus)
    }

    @Test fun oldFullArchiveClearsNewMetadataAndKeepsLegacyIcons() = runBlocking {
        dst.saveStyle(1, null, "old project style", "", emptyList())
        val old = BackupData(listOf(BackupProfile(Profile(1, "Legacy"), listOf(DbApplication("pkg", "Main", false, false, "pixels")), emptyList())), emptyMap())
        val data = JSONObject(BackupCodec.encode(old)).apply { remove("projectAi") }.toString().toByteArray()
        val bytes = archive(mapOf("manifest.json" to "{\"format\":3,\"kind\":\"backup\"}".toByteArray(), "data.json" to data))
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        assertTrue(dst.snapshot().metadata.isEmpty())
        assertTrue(dst.snapshot().revisions.isEmpty())
        assertEquals("pixels", RenkinPackRepository(target).getAll(1).single().drawable)
    }

    @Test fun legacyFullArchiveKeepsLargeAndTransparentGalleryPngsWithoutChangingProjectReferenceRules() = runBlocking {
        val random = java.util.Random(9030017)
        val bitmap = Bitmap.createBitmap(1300, 1300, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(1300 * 1300) { Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }
        bitmap.setPixels(pixels, 0, 1300, 0, 0, 1300, 1300)
        val large = try { ByteArrayOutputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.toByteArray()
        } } finally { bitmap.recycle() }
        assertTrue(large.size > ProjectAssetStore.MAX_BYTES)
        val transparent = projectTestPng(Color.TRANSPARENT)
        assertTrue(runCatching { inspectPng(transparent) }.isFailure)
        val old = BackupData(listOf(BackupProfile(Profile(1, "Legacy"), emptyList(), emptyList())), emptyMap())
        val data = JSONObject(BackupCodec.encode(old)).apply { remove("projectAi") }.toString().toByteArray()
        val bytes = archive(mapOf("manifest.json" to "{\"format\":3,\"kind\":\"backup\"}".toByteArray(),
            "data.json" to data, "uploads/img_legacy_large.png" to large, "uploads/img_legacy_transparent.png" to transparent))
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        assertArrayEquals(large, UploadedImageStore.directory(context).resolve("img_legacy_large.png").readBytes())
        assertArrayEquals(transparent, UploadedImageStore.directory(context).resolve("img_legacy_transparent.png").readBytes())
        assertTrue(dst.snapshot().metadata.isEmpty())
        assertTrue(runCatching { dst.importReference(1, ByteArrayInputStream(large)) }.isFailure)
        assertTrue(runCatching { dst.importReference(1, ByteArrayInputStream(transparent)) }.isFailure)
    }

    @Test fun profileShareEmbedsStyleAndRekeysEveryOwnerWithoutExportingDeviceFiles() = runBlocking {
        val reference = saved()
        src.saveSelection(4, 1, ProjectSelection("EXTEND", "pack.source", "a".repeat(64), "b".repeat(64), listOf("pkg/Main")))
        dst.saveStyle(1, null, "keep", "", emptyList())
        val bytes = export(true)
        assertFalse(entries(bytes).keys.any { it.startsWith("keystore/") || it.startsWith("uploads/") })
        val result = manager(target, targetWatch).importFile { ByteArrayInputStream(bytes) }
        val id = result.importedProfileId!!
        assertNotEquals(4L, id)
        assertEquals("outline", dst.load(id).revision!!.specification)
        assertEquals(id, dst.load(id).references.single().profileId)
        assertEquals(ProjectAssetStore.path(id, reference.hash), dst.load(id).references.single().relativePath)
        assertTrue(ProjectSelection.decode(dst.metadata(id)!!.selectionJson)!!.needsRevalidation)
        assertNull(dst.metadata(id)!!.sourceStamp)
        assertEquals("keep", dst.load(1).revision!!.specification)
    }

    @Test fun missingImageWrongHashAndCorruptPngRejectBeforeTouchingPreviousStyle() = runBlocking {
        val reference = saved()
        val original = entries(export())
        val name = "project_ai_assets/${reference.relativePath}"
        expectRejected(archive(original.filterKeys { it != name }))
        expectRejected(archive(LinkedHashMap(original).apply { put(name, projectTestPng(Color.RED)) }))
        expectRejected(archive(LinkedHashMap(original).apply { put(name, byteArrayOf(1, 2, 3)) }))
    }

    @Test fun unsafeUnlistedOversizedAndCredentialEntriesRejectBeforeMutation() = runBlocking {
        saved()
        val original = entries(export())
        for ((name, content) in listOf("../escape.png" to projectTestPng(),
            "project_ai_assets/1/${"a".repeat(64)}.png" to projectTestPng(),
            "noBackup/openai-key.enc" to byteArrayOf(1),
            "uploads/bomb.png" to ByteArray(ValidatedBackupArchive.MAX_UPLOAD_BYTES + 1))) {
            expectRejected(archive(LinkedHashMap(original).apply { put(name, content) }))
        }
    }

    @Test fun duplicateZipNamesAndUnsupportedSchemaFailClosed() = runBlocking {
        saved()
        val original = entries(export())
        val duplicate = archive(LinkedHashMap(original).apply { put("datb.json", getValue("data.json")) })
        val text = String(duplicate, Charsets.ISO_8859_1).replace("datb.json", "data.json").toByteArray(Charsets.ISO_8859_1)
        expectRejected(text)
        val data = JSONObject(String(original.getValue("data.json"))).apply { getJSONObject("projectAi").put("schema", 99) }
        expectRejected(archive(LinkedHashMap(original).apply { put("data.json", data.toString().toByteArray()) }))
    }

    @Test fun exportNeverIncludesNoBackupCredentialsDraftsOrBatchJournals() = runBlocking {
        saved()
        val sentinel = java.util.UUID.randomUUID().toString()
        val secret = File(context.noBackupFilesDir, "openai-key.enc").apply { writeText(sentinel) }
        val draft = File(context.noBackupFilesDir, "ai-draft-test").apply { writeText("draft-marker-secret") }
        val journal = File(context.noBackupFilesDir, "ai-batch-4.json").apply { writeText("batch-journal-secret") }
        try {
            for (bytes in listOf(export(), export(true))) {
                val decoded = entries(bytes)
                assertFalse(decoded.keys.any { it.contains("key.enc") || it.contains("draft") || it.contains("ai-batch") })
                assertFalse(decoded.values.any { String(it).contains(sentinel) || String(it).contains("batch-journal-secret") })
            }
        } finally { secret.delete(); draft.delete(); journal.delete() }
    }

    @Test fun installationFailureKeepsEarlierStyleAndBackupCanRepairDamagedReference() = runBlocking {
        val reference = saved()
        val bytes = export()
        dst.saveStyle(1, null, "keep", "", emptyList())
        val ownerDirectory = File(context.filesDir, "project_ai_assets/4")
        ownerDirectory.deleteRecursively(); ownerDirectory.writeText("not a directory")
        assertTrue(runCatching { manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) } }.isFailure)
        assertEquals("keep", dst.load(1).revision!!.specification)
        ownerDirectory.delete()
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        dst.files.resolve(reference.relativePath).writeBytes(byteArrayOf(1))
        manager(target, targetWatch).importBackup { ByteArrayInputStream(bytes) }
        assertArrayEquals(projectTestPng(), dst.files.read(reference))
    }
}
