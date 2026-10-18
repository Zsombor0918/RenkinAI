package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.room.Room
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.project.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PackMontageTest {
    @get:Rule val temp = TemporaryFolder()
    private fun samples(count: Int) = (0 until count).map { i ->
        var png = analysisFixturePng(i % 3, i)
        if (i % 5 == 0) {
            val decoded = BitmapFactory.decodeByteArray(png, 0, png.size)
            val image = decoded.copy(Bitmap.Config.ARGB_8888, true).also { decoded.recycle() }
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30 + i * 3, 130, 230); textSize = 76f }
                Canvas(image).drawText(if (i % 10 == 0) "AB" else "Go", 32f, 160f, paint)
                png = java.io.ByteArrayOutputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
            } finally { image.recycle() }
        }
        SampledReference("fixture.$i/Main", "icon$i", sha256(png), png, referenceFeatures(png))
    }
    @Test fun automaticRedistributionPreservesOrderedPixelsAlphaAndDetailAcrossSelectionSizes() = runBlocking<Unit> {
        val input = samples(60)
        for (n in listOf(1, 2, 7, 14, 15, 16, 29, 30, 31, 60)) {
            val selected = input.take(n); val pages = renderMontages(selected)
            assertEquals((n + 14) / 15, pages.size)
            assertTrue(pages.sumOf { it.size.toLong() } <= 16L * 1024 * 1024)
            var position = 0
            pages.forEachIndexed { page, png ->
                val count = n / pages.size + if (page < n % pages.size) 1 else 0
                val (columns, rows) = montageLayout(count)
                val (w, h) = inspectPng(png)
                assertEquals(columns * MONTAGE_CELL to rows * MONTAGE_CELL, w to h)
                // Maximum page bitmap plus a single decoded icon is bounded below6 MiB.
                assertTrue(w.toLong() * h * 4 + 256 * 256 * 4 < 6L * 1024 * 1024)
                assertTrue(((w + 31) / 32) * ((h + 31) / 32) <= 2500)
                val image = BitmapFactory.decodeByteArray(png, 0, png.size)
                try {
                    assertEquals(0, Color.alpha(image.getPixel(0, 0)))
                    repeat(count) { at ->
                        val original = selected[position++].png
                        val decoded = BitmapFactory.decodeByteArray(original, 0, original.size)
                        try {
                            val row = at / columns; val inRow = minOf(columns, count - row * columns)
                            val x = (columns - inRow) * MONTAGE_CELL / 2 + at % columns * MONTAGE_CELL + 16
                            val y = row * MONTAGE_CELL + 16
                            val expected = IntArray(256 * 256); val actual = IntArray(expected.size)
                            decoded.getPixels(expected, 0, 256, 0, 0, 256, 256)
                            image.getPixels(actual, 0, 256, x, y, 256, 256)
                            assertArrayEquals("n=$n page=$page at=$at source pixels must not be rescaled/cropped", expected, actual)
                        } finally { decoded.recycle() }
                    }
                } finally { image.recycle() }
            }
            assertEquals(n, position)
        }
        val pages = renderMontages(input.take(30))
        assertFalse(pages.first().contentEquals(renderMontages(input.take(30).reversed()).first()))
        val edited = renderMontages(input.take(16).drop(1))
        assertEquals(1, edited.size) // Removal redistributes the prior two-page selection automatically.
        val output = File("build/montage-evidence").apply { mkdirs() }
        pages.forEachIndexed { index, bytes -> File(output, "sheet-${index + 1}.png").writeBytes(bytes) }
        val beforeEdit = renderMontages(input.take(16))
        beforeEdit.forEachIndexed { index, bytes -> File(output, "before-16-sheet-${index + 1}.png").writeBytes(bytes) }
        File(output, "after-15-sheet-1.png").writeBytes(edited.single())
        File(output, "metadata.json").writeText(JSONObject().put("icons", 30).put("pages", pages.size)
            .put("dimensions", org.json.JSONArray(pages.map { inspectPng(it).let { (w, h) -> "${w}x$h" } }))
            .put("png_bytes", org.json.JSONArray(pages.map { it.size })).put("source_pixels_per_icon", 256)
            .put("automatic_edit", JSONObject().put("before_icons", 16).put("after_icons", 15)
                .put("before_pages", beforeEdit.size).put("after_pages", edited.size)
                .put("before_hashes", org.json.JSONArray(beforeEdit.map(::sha256)))
                .put("after_hashes", org.json.JSONArray(edited.map(::sha256)))).toString(2))
        // Poorly compressible source art must still fit the real private-asset and HTTP limits.
        val noisy = (0 until 15).map { index ->
            val image = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val random = kotlin.random.Random(index)
            try {
                image.setPixels(IntArray(256 * 256) { random.nextInt() or (0xff shl 24) }, 0, 256, 0, 0, 256, 256)
                val png = java.io.ByteArrayOutputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
                SampledReference("noise.$index/Main", "noise$index", sha256(png), png, referenceFeatures(png))
            } finally { image.recycle() }
        }
        val stress = renderMontages(noisy).single()
        assertTrue(stress.size > 2 * 1024 * 1024)
        assertTrue(stress.size < ProjectAssetStore.MAX_BYTES)
        inspectPng(stress)
    }
    @Test fun montageRenderingFitsWordmarksWithoutChangingHistoricalSquareRendering() {
        val bitmap = Bitmap.createBitmap(200, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try {
            val drawable = BitmapDrawable(RuntimeEnvironment.getApplication().resources, bitmap)
            val original = checkNotNull(renderStaticReference(drawable))
            val fitted = checkNotNull(renderMontageReference(drawable))
            fun bounds(bytes: ByteArray): Pair<Int, Int> {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                try {
                    val xs = mutableListOf<Int>(); val ys = mutableListOf<Int>()
                    for (y in 0 until image.height) for (x in 0 until image.width)
                        if (Color.alpha(image.getPixel(x, y)) > 128) { xs += x; ys += y }
                    return xs.max() - xs.min() + 1 to ys.max() - ys.min() + 1
                } finally { image.recycle() }
            }
            assertEquals(256 to 256, bounds(original))
            assertEquals(256 to 64, bounds(fitted))
        } finally { bitmap.recycle() }
    }
    @Test fun localMontagesPersistReopenEditAndRekeyWithoutAnalysisOrCredentials() = runBlocking<Unit> {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, RenkinPackDatabase::class.java).build()
        try {
            val repo = ProjectAiRepository(db, ProjectAssetStore(analysisFixtureContext(temp.root)))
            RenkinPackRepository(db).replaceEverything(listOf(Profile(1, "Pack"), Profile(2, "Other")), emptyList())
            val art = samples(42).associate { it.drawable to it.png }
            val reader = FixtureReferenceReader(art)
            val mutex = Mutex()
            val owner = object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T): T = mutex.withLock { check(id == 1L); operation() }
                override suspend fun catalogStamp(): String = error("No target or provider input during local sampling")
                override suspend fun reader(source: String, referenceIndex: Boolean) = reader.also { check(source == "fixture.pack") }
            }
            val local = LocalPackMontages(repo, ReferenceSampler(), owner)
            val draft = local.prepare(1, "fixture.pack", projectSourceStamp(reader.index))
            assertTrue(draft.montage.members.size in 20..30)
            val saved = local.save(1, null, draft)
            assertEquals(draft.montage.pages.map { it.hash }, saved.references.map { it.hash })
            val reopened = local.reopen(1)
            assertEquals(draft.montage, reopened.montage)
            draft.images.forEachIndexed { index, bytes -> assertArrayEquals(bytes, reopened.images[index]) }
            val browse = local.browse(1, reopened)
            val edited = local.recompose(1, browse, browse.montage.members.take(15).reversed().map { it.hash })
            assertEquals(1, edited.images.size); assertNull(edited.montage.result)
            local.save(1, saved.revision!!.revision, edited)
            assertEquals(2, repo.history(1).size)
            assertEquals(draft.montage.encode(), repo.history(1).first().analysisProfileJson)
            val snapshot = repo.snapshot()
            assertEquals(snapshot, ProjectAiCodec.decode(ProjectAiCodec.encode(snapshot)))
            val rekeyed = snapshot.rekey(1, 2)
            ProjectAiRepository.validateSnapshot(rekeyed, setOf(2))
            assertTrue(MontageEvidence.parse(rekeyed.revisions.last().analysisEvidenceJson!!).needsRevalidation)
            reader.current = false
            assertTrue(runCatching { local.save(1, 2, edited) }.isFailure)
            assertEquals(snapshot, repo.snapshot())
            assertTrue(runCatching { local.save(2, null, edited) }.isFailure)
            assertNull(StyleAnalysisJournal(analysisFixtureContext(temp.root)).latest(1))
        } finally { db.close() }
    }
}
