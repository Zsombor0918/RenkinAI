package dev.renkinProject.renkin.data.project

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import dev.renkinProject.renkin.data.*
import kotlinx.coroutines.*
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

internal fun projectTestPng(color: Int = Color.WHITE, size: Int = 32): ByteArray {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(color)
    bitmap.setPixel(0, 0, Color.TRANSPARENT)
    return try { ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() } }
    finally { bitmap.recycle() }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectAiPersistenceTest {
    private lateinit var context: Context
    private lateinit var db: RenkinPackDatabase
    private lateinit var packs: RenkinPackRepository
    private lateinit var repo: ProjectAiRepository
    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).build()
        packs = RenkinPackRepository(db)
        repo = ProjectAiRepository(db, ProjectAssetStore(context))
        packs.replaceEverything(listOf(Profile(1, "Same"), Profile(2, "Same")), emptyList())
    }
    @After fun close() { db.close(); File(context.filesDir, "project_ai_assets").deleteRecursively() }
    private suspend fun reference(id: Long = 1, color: Int = Color.WHITE) =
        repo.importReference(id, ByteArrayInputStream(projectTestPng(color)))

    @Test fun freshProfilesAreUnsetAndEqualNamesHaveIsolatedStyles() = runBlocking {
        assertNull(repo.load(1).revision)
        assertNull(repo.load(2).metadata)
        repo.saveStyle(1, null, "White outline", "round caps", listOf(reference().hash))
        repo.saveStyle(2, null, "Black filled", "", listOf(reference(2, Color.BLACK).hash))
        assertEquals("White outline", repo.load(1).revision!!.specification)
        assertEquals("Black filled", repo.load(2).revision!!.specification)
        assertNotEquals(repo.load(1).references.single().hash, repo.load(2).references.single().hash)
    }

    @Test fun repeatedSaveIsIdempotentAndChangedSavePreservesOrderedHistory() = runBlocking {
        val first = reference()
        val second = reference(color = Color.RED)
        val one = repo.saveStyle(1, null, "outline", "", listOf(first.hash, second.hash))
        val again = repo.saveStyle(1, null, "outline", "", listOf(first.hash, second.hash))
        assertEquals(one.revision, again.revision)
        val two = repo.saveStyle(1, 1, "outline", "", listOf(second.hash, first.hash))
        assertEquals(2L, two.revision!!.revision)
        assertEquals(listOf(1L, 2L), repo.history(1).map { it.revision })
        assertEquals(listOf(first.hash, second.hash), repo.references(1, 1).map { it.hash })
        assertEquals(listOf(second.hash, first.hash), repo.load(1).references.map { it.hash })
    }

    @Test fun twoConcurrentDifferentWritersCannotOverwriteEachOther() = runBlocking {
        val outcomes = (1..8).map { index -> async(Dispatchers.Default) {
            runCatching { repo.saveStyle(1, null, "style $index", "", emptyList()) }
        } }.awaitAll()
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(7, outcomes.count { it.isFailure })
        assertEquals(1, repo.history(1).size)
        assertEquals(repo.history(1).single(), repo.load(1).revision)
    }

    @Test fun concurrentEqualWritersProduceOneRevision() = runBlocking {
        val rows = (1..8).map { async(Dispatchers.Default) { repo.saveStyle(1, null, "same", "", emptyList()) } }.awaitAll()
        assertEquals(setOf(1L), rows.map { it.revision!!.revision }.toSet())
        assertEquals(1, repo.history(1).size)
    }

    @Test fun canceledAndStaleSaveKeepLastValidRevision() = runBlocking {
        repo.saveStyle(1, null, "saved", "", emptyList())
        val canceled = Job().also { it.cancel() }
        assertTrue(runCatching { withContext(canceled) { repo.saveStyle(1, 1, "canceled", "", emptyList()) } }.isFailure)
        assertTrue(runCatching { repo.saveStyle(1, null, "stale", "", emptyList()) }.isFailure)
        assertEquals("saved", repo.load(1).revision!!.specification)
        assertEquals(1, repo.history(1).size)
    }

    @Test fun foreignAndMissingAssetCannotBecomeASavedStyle() = runBlocking {
        val owned = reference(2)
        assertTrue(runCatching { repo.saveStyle(1, null, "style", "", listOf(owned.hash)) }.isFailure)
        assertTrue(runCatching { repo.saveStyle(1, null, "style", "", listOf("a".repeat(64))) }.isFailure)
        assertNull(repo.load(1).revision)
    }

    @Test fun galleryDeletionAndHistoricalPinsDoNotRemoveReferences() = runBlocking {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val upload = try { UploadedImageStore.save(context, bitmap) } finally { bitmap.recycle() }
        val originalBytes = upload.readBytes()
        val asset = repo.importReference(1, upload.inputStream())
        repo.saveStyle(1, null, "first", "", listOf(asset.hash))
        upload.delete()
        repo.saveStyle(1, 1, "second", "", emptyList())
        repo.discardDraftReferences(1, listOf(asset.hash))
        repo.collectUnreferencedFiles()
        assertEquals(asset.hash, repo.references(1, 1).single().hash)
        assertArrayEquals(originalBytes, repo.files.read(asset))
    }

    @Test fun failedFileImportAndUnknownTempAreCollectibleWithoutTouchingHistory() = runBlocking {
        val asset = reference()
        repo.saveStyle(1, null, "saved", "", listOf(asset.hash))
        val directory = File(context.filesDir, "project_ai_assets/1")
        File(directory, ".write-interrupted.tmp").writeBytes(byteArrayOf(1))
        val unrecorded = repo.files.install(1, projectTestPng(Color.GREEN))
        assertTrue(runCatching { repo.importReference(1, ByteArrayInputStream(ByteArray(128))) }.isFailure)
        assertEquals(2, repo.collectUnreferencedFiles())
        assertFalse(repo.files.resolve(unrecorded.relativePath).exists())
        assertEquals("saved", repo.load(1).revision!!.specification)
    }

    @Test fun archiveAssetLeaseKeepsGcBehindFileAndRoomFinalization() = runBlocking {
        val finalized = CompletableDeferred<ReferenceAsset>()
        val release = CompletableDeferred<Unit>()
        val writer = async {
            repo.withAssetLease {
                val asset = repo.files.install(1, projectTestPng())
                finalized.complete(asset)
                release.await()
                db.projectAiDao().insertAssets(listOf(asset))
            }
        }
        val asset = finalized.await()
        val collector = async(start = CoroutineStart.UNDISPATCHED) { repo.collectUnreferencedFiles() }
        assertFalse(collector.isCompleted)
        release.complete(Unit)
        writer.await()
        assertEquals(0, collector.await())
        assertArrayEquals(projectTestPng(), repo.referenceBytes(1, asset.hash))
    }

    @Test fun failedGroupedImportKeepsPreviouslyOwnedDraftAndCleansOnlyItsNewFiles() = runBlocking {
        val existing = reference()
        val unrelated = repo.files.install(1, projectTestPng(Color.GREEN))
        val failed = runCatching { repo.importReferences(1, listOf(
            { ByteArrayInputStream(projectTestPng()) },
            { ByteArrayInputStream(projectTestPng(Color.RED)) },
            { ByteArrayInputStream(byteArrayOf(1, 2, 3)) })) }
        assertTrue(failed.isFailure)
        assertEquals(listOf(existing.hash), repo.snapshot().assets.map { it.hash })
        assertEquals(listOf(existing.hash + ".png", unrelated.hash + ".png").sorted(),
            File(context.filesDir, "project_ai_assets/1").list()!!.sorted())
        assertArrayEquals(projectTestPng(), repo.referenceBytes(1, existing.hash))
        assertTrue(repo.files.resolve(unrelated.relativePath).isFile)
        assertEquals(1, repo.collectUnreferencedFiles())
        assertFalse(repo.files.resolve(unrelated.relativePath).exists())
    }

    @Test fun deletionCascadesAndReusedIdCannotInheritAnotherStyle() = runBlocking {
        val asset = reference(2)
        repo.saveStyle(2, null, "private", "", listOf(asset.hash))
        repo.insertVariant(IconAssetVariant("variant", 2, "pkg/Main", 1, asset.hash, asset.hash))
        packs.deleteProfile(2)
        assertTrue(repo.snapshot().assets.isEmpty())
        assertTrue(repo.snapshot().variants.isEmpty())
        assertEquals(1, repo.collectUnreferencedFiles())
        packs.createProfile(Profile(2, "Replacement"))
        assertNull(repo.load(2).revision)
        packs.deleteProfile(1)
        assertNotNull(packs.profile(1))
    }

    @Test fun variantRelationsAndStatusesAreFunctionalAndCrossOwnerUpdatesFail() = runBlocking {
        val asset = reference()
        repo.saveStyle(1, null, "style", "", emptyList())
        val variant = IconAssetVariant("candidate", 1, "pkg/Main", 1, asset.hash, asset.hash)
        repo.insertVariant(variant)
        assertEquals(listOf(variant), repo.variants(1, "pkg/Main"))
        assertEquals("UNREVIEWED", repo.variants(1, "pkg/Main").single().reviewStatus)
        assertEquals("NOT_APPLIED", repo.variants(1, "pkg/Main").single().applyStatus)
        assertTrue(runCatching { repo.insertVariant(variant.copy(id = "foreign", profileId = 2)) }.isFailure)
        repo.discardDraftReferences(1, listOf(asset.hash))
        assertTrue(repo.files.read(asset).isNotEmpty())
    }

    @Test fun selectionSurvivesReopeningButAlwaysRequiresRevalidation() = runBlocking {
        val stamp = "a".repeat(64)
        val selection = ProjectSelection("CREATE", null, null, stamp, listOf("pkg/Main"), needsRevalidation = false)
        val meta = repo.saveSelection(1, 0, selection)
        val decoded = ProjectSelection.decode(repo.metadata(1)!!.selectionJson)!!
        assertTrue(decoded.needsRevalidation)
        assertTrue(decoded.isCurrent("CREATE", null, null, stamp, setOf("pkg/Main")))
        assertFalse(decoded.isCurrent("CREATE", null, null, "b".repeat(64), setOf("pkg/Main")))
        assertFalse(decoded.isCurrent("CREATE", null, null, stamp, emptySet()))
        assertTrue(runCatching { repo.saveSelection(1, 0, selection) }.isFailure)
        assertEquals(meta, repo.metadata(1))
    }

    @Test fun brokenPngSignatureCrcEmptyArtworkSizeAndPathsAreRejected() = runBlocking {
        val good = projectTestPng()
        val corrupted = good.clone().also { it[30] = (it[30].toInt() xor 1).toByte() }
        listOf(ByteArray(128), good.copyOf(good.size - 5), corrupted,
            ByteArray(ProjectAssetStore.MAX_BYTES + 1), projectTestPng(Color.TRANSPARENT)).forEach {
            assertTrue(runCatching { repo.importReference(1, ByteArrayInputStream(it)) }.isFailure)
        }
        assertTrue(runCatching { repo.files.resolve("../secret") }.isFailure)
        assertTrue(runCatching { repo.files.resolve("1/../${"a".repeat(64)}.png") }.isFailure)
        assertTrue(repo.snapshot().assets.isEmpty())
    }

    @Test fun hashMismatchMissingImageAndSymlinkCannotReadOrReplaceSavedData() = runBlocking {
        val asset = reference()
        repo.saveStyle(1, null, "saved", "", listOf(asset.hash))
        val file = repo.files.resolve(asset.relativePath)
        val saved = file.readBytes()
        file.writeBytes(projectTestPng(Color.RED))
        assertTrue(runCatching { repo.load(1) }.isFailure)
        file.delete()
        assertTrue(runCatching { repo.load(1) }.isFailure)
        val outside = File(context.cacheDir, "outside.png").apply { writeBytes(saved) }
        java.nio.file.Files.createSymbolicLink(file.toPath(), outside.toPath())
        assertTrue(runCatching { repo.files.read(asset) }.isFailure)
        file.delete()
        file.writeBytes(saved)
        assertEquals("saved", repo.load(1).revision!!.specification)
    }

    @Test fun unsupportedRevisionAndSelectionSchemaCannotBeImported() {
        assertThrows(IllegalArgumentException::class.java) { ProjectAiRepository.validateSnapshot(
            ProjectAiSnapshot(revisions = listOf(StyleRevision(1, 1, schemaVersion = 2, specification = "style"))), setOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { ProjectSelection("CREATE", null, null,
            "a".repeat(64), emptyList(), schemaVersion = 2).encode() }
    }
}
