package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.graphics.*
import android.graphics.drawable.*
import android.util.Xml
import dev.renkinProject.renkin.apk.CoverageIndexState
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.packages.ApplicationManager
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSamplerTest {
    private val sampler = ReferenceSampler()
    private fun artwork(style: Int) = (0 until 18).associate { "style${style}_$it" to analysisFixturePng(style, it) }

    @Test fun threeDistinctPackFixturesYieldStableDiverseBytesAndMeasuredFeatures() = runBlocking {
        val features = mutableListOf<ReferenceFeatures>()
        for (style in 0..2) {
            val pngs = artwork(style); val started = System.nanoTime()
            val one = sampler.sample(FixtureReferenceReader(pngs))
            val two = sampler.sample(FixtureReferenceReader(pngs.entries.reversed().associate { it.toPair() }))
            assertTrue("fixture $style: ${one.report}", one.samples.size in 8..12)
            assertEquals(one.samples.map { it.component }, two.samples.map { it.component })
            assertEquals(one.samples.map { it.hash }, two.samples.map { it.hash })
            one.samples.zip(two.samples).forEach { assertArrayEquals(it.first.png, it.second.png) }
            assertTrue(one.samples.map { it.hash }.distinct().size == one.samples.size)
            assertEquals("fixture.pack", one.sourcePackage); assertEquals(7L, one.sourceIdentity!!.versionCode)
            assertEquals(REFERENCE_ALGORITHM, one.algorithm); one.report.validate()
            assertFalse(one.report.lowSample)
            features += one.samples.first().features
            println("REFERENCE_FIXTURE style=$style examined=${one.report.examined} valid=${one.report.valid} chosen=${one.samples.size} duplicates=${one.report.duplicates} mixed=${one.report.mixedStyleSuspected} elapsedMs=${(System.nanoTime() - started) / 1000000}")
        }
        assertTrue(features[0].occupancy < features[1].occupancy)
        assertTrue(features[0].brightness > features[1].brightness)
        assertTrue(features[2].saturation > features[0].saturation)
        assertEquals(features[0].distance(features[2]), features[2].distance(features[0]), 1e-12)
    }
    @Test fun mixedPackReportsObservedDiversityRatherThanHomogeneousConfidence() = runBlocking {
        val result = sampler.sample(FixtureReferenceReader(artwork(0) + artwork(1) + artwork(2)))
        val families = result.samples.map { it.drawable.substringBefore('_') }.toSet()
        assertEquals(setOf("style0", "style1", "style2"), families)
        assertTrue("report=${result.report}; distances=${result.samples.flatMap { a -> result.samples.map { a.features.distance(it.features) } }}",
            result.report.mixedStyleSuspected)
    }
    @Test fun duplicateEmptyBrokenLowAlphaAndDynamicEntriesAreAccountedFor() = runBlocking {
        val good = analysisFixturePng()
        val pngs = mapOf("good" to good, "duplicate" to good, "empty" to analysisFixturePng(alpha = 0),
            "low" to analysisFixturePng(alpha = 10), "corrupt" to ByteArray(16), "calendar" to analysisFixturePng(1))
        val mappings = pngs.keys.associate { "uninstalled.$it/.Main" to it } + ("missing/.Main" to "missing")
        val result = sampler.sample(FixtureReferenceReader(pngs, mappings, setOf("uninstalled.calendar/.Main")))
        assertEquals(7, result.report.examined); assertEquals(1, result.report.valid)
        assertEquals(1, result.report.duplicates); assertEquals(1, result.report.unsupported); assertEquals(4, result.report.broken)
        assertEquals(1, result.samples.size); assertTrue(result.report.lowSample)
        assertEquals(sha256(good), result.samples.single().hash)
    }
    @Test fun zeroOneTwoAndThemedSourcesNeverInventSampleCounts() = runBlocking {
        for (count in 0..2) {
            val result = sampler.sample(FixtureReferenceReader((0 until count).associate { "icon$it" to analysisFixturePng(it, it) }))
            assertEquals(count, result.samples.size); assertTrue(result.report.lowSample)
        }
        val reader = FixtureReferenceReader(artwork(2), stableReferences = false)
        val themed = sampler.sample(reader)
        assertTrue(themed.samples.isEmpty()); assertEquals(18, themed.report.unsupported); assertEquals(0, reader.renderCount)
    }
    @Test fun candidateBoundCoversEndOfLargeSortedIndexInsteadOfFirstRows() = runBlocking {
        val pngs = artwork(2)
        val mapping = (0 until 2000).associate { "component.%04d/.Main".format(it) to "style2_${it % 18}" }
        val reader = FixtureReferenceReader(pngs, mapping)
        val result = sampler.sample(reader)
        assertEquals(120, result.report.examined); assertEquals(120, reader.renderCount)
        val singleton = sampler.sample(FixtureReferenceReader(pngs, mapping), candidateLimit = 1)
        assertEquals(1, singleton.report.examined)
        assertTrue(result.samples.any { it.component.substringAfter("component.").substringBefore('/').toInt() > 120 })
    }
    @Test fun sourceChangeAfterRenderingAndUnknownIndexesFailClosed() = runBlocking {
        val reader = FixtureReferenceReader(artwork(1)).apply { afterRender = { current = false } }
        try { sampler.sample(reader); fail("A changed source must not be current") } catch (_: StaleReferenceSource) { }
        assertEquals(1, reader.renderCount)
        val unknown = object : CoverageSourceReader by FixtureReferenceReader(artwork(1)) {
            override val index = FixtureReferenceReader(artwork(1)).index.copy(state = CoverageIndexState.UNKNOWN)
        }
        try { sampler.sample(unknown); fail("Unproven mapping must not produce samples") } catch (_: IllegalStateException) { }
    }
    @Test fun cancellationStopsBoundedRenderImmediatelyWithoutPublishingSamples() = runBlocking {
        val reader = FixtureReferenceReader(artwork(2))
        lateinit var job: Deferred<ReferenceSamples>
        reader.afterRender = { job.cancel() }
        job = async(start = CoroutineStart.LAZY) { sampler.sample(reader) }
        job.start()
        try { job.await(); fail("Cancelled sampling must not complete") } catch (_: CancellationException) { }
        assertEquals(1, reader.renderCount)
    }
    @Test fun staticBitmapVectorAdaptiveRenderAndOwnedBitmapCleanupAreReal() {
        val resources = RuntimeEnvironment.getApplication<Application>().resources
        val input = BitmapFactory.decodeByteArray(analysisFixturePng(2), 0, analysisFixturePng(2).size)
        try {
            val bitmapDrawable = BitmapDrawable(resources, input)
            val parser = Xml.newPullParser().apply { setInput(StringReader("""<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="64dp" android:height="64dp" android:viewportWidth="64" android:viewportHeight="64"><path android:fillColor="#ff00aa" android:pathData="M8,8 L56,8 L32,56 Z"/></vector>""")) }
            val vector = Drawable.createFromXml(resources, parser)
            val adaptive = AdaptiveIconDrawable(ColorDrawable(Color.DKGRAY), bitmapDrawable)
            for (drawable in listOf(bitmapDrawable, vector, adaptive)) {
                var owned: Bitmap? = null
                val bytes = checkNotNull(renderStaticReference(drawable) {
                    Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also { owned = it }
                })
                assertTrue(checkNotNull(owned).isRecycled); validateAnalysisPng(bytes)
                assertTrue(referenceFeatures(bytes).occupancy > 0)
            }
            assertFalse(input.isRecycled)
        } finally { input.recycle() }
    }
    @Test fun drawFailureRestoresBoundsAndRecyclesOnlyOwnedOutput() {
        val drawable = object : Drawable() {
            override fun draw(canvas: Canvas) { error("broken resource") }
            override fun setAlpha(alpha: Int) { }
            override fun setColorFilter(filter: ColorFilter?) { }
            @Deprecated("Required drawable override") override fun getOpacity() = PixelFormat.TRANSLUCENT
        }
        drawable.setBounds(2, 3, 7, 9); var output: Bitmap? = null
        assertThrows(IllegalStateException::class.java) { renderStaticReference(drawable) {
            Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also { output = it }
        } }
        assertTrue(checkNotNull(output).isRecycled); assertEquals(Rect(2, 3, 7, 9), drawable.bounds)
        assertNull(renderStaticReference(LayerDrawable(arrayOf(ColorDrawable(Color.RED)))))
        assertNull(renderStaticReference(StateListDrawable()))
        assertNull(renderStaticReference(AnimationDrawable()))
    }
    @Test fun ownReferenceNormalizationDeduplicatesAndCapsGenerationInputs() = runBlocking {
        val png = analysisFixturePng(1)
        val own = sampler.manual(listOf("first" to png, "second" to png))
        assertEquals(1, own.samples.size); assertEquals(1, own.report.duplicates)
        assertNull(own.sourceIdentity); assertNull(own.sourcePackage)
        try { sampler.manual((0..5).map { "ref$it" to png }); fail("Manual generation refs remain capped at five") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun allMappingOptInUsesExistingParserAndKeepsLastValidDuplicate() {
        val parser = Xml.newPullParser().apply { setInput(StringReader("""<resources><item component="ComponentInfo{outside/Main}" drawable="first"/><item component="ComponentInfo{outside/Main}" drawable="last"/><item component="ComponentInfo{outside/Main}"/><item component="ComponentInfo{broken/Main}"/><calendar component="ComponentInfo{calendar/Main}" prefix="day_"/></resources>""")) }
        val manager = ApplicationManager(RuntimeEnvironment.getApplication())
        val all = manager.readAppFilterForCoverage(parser, emptyList(), allMappings = true)
        assertEquals("last", all.mappings["ComponentInfo{outside/Main}"])
        assertEquals("", all.mappings["ComponentInfo{broken/Main}"])
        assertTrue(all.calendarComponents.contains("ComponentInfo{calendar/Main}"))
        assertTrue("legacy installed-app list must remain unchanged", all.elements.isEmpty())
    }
}
