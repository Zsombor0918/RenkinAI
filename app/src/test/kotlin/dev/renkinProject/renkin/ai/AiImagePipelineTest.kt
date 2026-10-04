package dev.renkinProject.renkin.ai

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import dev.renkinProject.renkin.BuildConfig
import dev.renkinProject.renkin.apk.IconPackBuilder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiImagePipelineTest {
    @Test fun editSendsOriginalFirstAndSeparateReferencesWithoutAndroidIdentifiers() {
        val settings = AiSettings(style = "White outline", referenceNames = listOf("img_1_1.png", "img_2_2.png"))
        val request = buildImageEditRequest("test-key-only", settings,
            listOf("ORIGINAL".toByteArray(), "REFERENCE_ONE".toByteArray(), "REFERENCE_TWO".toByteArray()))
        assertEquals("https://api.openai.com/v1/images/edits", request.url.toString())
        assertEquals("POST", request.method)
        val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
        assertTrue(body.indexOf("ORIGINAL") < body.indexOf("REFERENCE_ONE"))
        assertTrue(body.indexOf("REFERENCE_ONE") < body.indexOf("REFERENCE_TWO"))
        assertEquals(3, Regex("name=\"image\\[\\]\"").findAll(body).count())
        val multipart = request.body as okhttp3.MultipartBody
        fun field(name: String): String {
            val part = multipart.parts.single { it.headers?.get("Content-Disposition")?.contains("name=\"$name\"") == true }
            return Buffer().also { part.body.writeTo(it) }.readUtf8()
        }
        assertEquals("transparent", field("background"))
        assertEquals("1", field("n"))
        assertFalse(body.contains("test-key-only"))
        assertFalse(body.contains("packageName"))
        assertFalse(body.contains("store"))
    }

    @Test fun malformedAndUnboundedSettingsAreRejectedBeforeNetworking() {
        assertThrows(IllegalArgumentException::class.java) { AiSettings(style = "").validate() }
        assertThrows(IllegalArgumentException::class.java) { AiSettings(referenceNames = listOf("../secret")).validate() }
        assertThrows(IllegalArgumentException::class.java) { AiSettings(quality = "auto").validate() }
        assertThrows(IllegalArgumentException::class.java) { AiSettings(referenceNames = List(6) { "img_1_1.png" }).validate() }
        assertThrows(IllegalArgumentException::class.java) { buildImageEditRequest("test", AiSettings(), emptyList()) }
    }

    @Test fun stabilityStyleTransferUsesTargetAndOneReferenceWithoutLeakingKey() {
        val settings = AiSettings(provider = AiSettings.STABILITY, style = "white monoline")
        val request = buildStabilityStyleRequest("private-key", settings,
            "target-icon".toByteArray(), "reference-icon".toByteArray())
        assertEquals("https://api.stability.ai/v2beta/stable-image/control/style-transfer", request.url.toString())
        assertEquals("Bearer private-key", request.header("Authorization"))
        assertEquals("image/*", request.header("Accept"))
        val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
        assertTrue(body.contains("name=\"init_image\""))
        assertTrue(body.contains("target-icon"))
        assertTrue(body.contains("name=\"style_image\""))
        assertTrue(body.contains("reference-icon"))
        assertTrue(body.contains("white monoline"))
        assertFalse(body.contains("private-key"))
        assertFalse(body.contains("packageName"))
    }

    @Test fun stabilityRemoveBackgroundIsSeparateAndReturnsPngBytes() {
        val request = buildStabilityRemoveBackgroundRequest("private-key", byteArrayOf(1, 2, 3))
        assertEquals("https://api.stability.ai/v2beta/stable-image/edit/remove-background", request.url.toString())
        assertEquals("image/*", request.header("Accept"))
        val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
        assertTrue(body.contains("name=\"image\""))
        assertTrue(body.contains("name=\"output_format\""))
        assertTrue(body.contains("png"))
        assertFalse(body.contains("private-key"))
        assertTrue(stabilityErrorMessage(402).contains("credits"))
        assertTrue(stabilityErrorMessage(429).contains("No automatic retry"))
    }

    @Test fun pngMustBeSquareVisibleAndActuallyTransparent() {
        val image = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        try {
            assertThrows(IllegalStateException::class.java) { validateAiPng(image.pngBytes()) }
            image.eraseColor(Color.WHITE)
            assertThrows(IllegalStateException::class.java) { validateAiPng(image.pngBytes()) }
            image.setPixel(0, 0, Color.TRANSPARENT)
            val decoded = validateAiPng(image.pngBytes())
            assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
            assertEquals(255, Color.alpha(decoded.getPixel(512, 512)))
            decoded.recycle()
        } finally { image.recycle() }
        assertThrows(IllegalArgumentException::class.java) { validateAiPng(ByteArray(32)) }
    }

    @Test fun rejectsWrongDimensionsBeforeAllocatingFullResult() {
        val image = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try { assertThrows(IllegalArgumentException::class.java) { validateAiPng(image.pngBytes()) } }
        finally { image.recycle() }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun largerSquareTransparentProviderPngIsNormalizedForRenkin() {
        val image = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(Color.TRANSPARENT)
            android.graphics.Canvas(image).drawRect(900f, 900f, 1150f, 1150f,
                android.graphics.Paint().apply { color = Color.WHITE })
            val normalized = validateAiPng(image.pngBytes())
            assertEquals(1024, normalized.width)
            assertEquals(1024, normalized.height)
            assertEquals(0, Color.alpha(normalized.getPixel(0, 0)))
            assertTrue(Color.alpha(normalized.getPixel(512, 512)) > 0)
            normalized.recycle()
        } finally { image.recycle() }
    }

    @Test fun responseRequiresBase64AndNeverDownloadsAnArbitraryUrl() {
        val bytes = byteArrayOf(1, 2, 3)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        assertArrayEquals(bytes, decodeImageResponse("{\"data\":[{\"b64_json\":\"$encoded\"}]}"))
        assertThrows(java.io.IOException::class.java) { decodeImageResponse("{\"data\":[{\"url\":\"http://localhost/private\"}]}") }
    }

    @Test fun quotaAndRateLimitsDoNotEchoServerContent() {
        val secret = "DO_NOT_SHOW_THIS"
        assertTrue(apiErrorMessage(429, "{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"$secret\"}}", null).contains("credit"))
        assertTrue(apiErrorMessage(429, "", "20").contains("20 seconds"))
        assertFalse(apiErrorMessage(500, secret, secret).contains(secret))
    }

    @Test fun httpErrorsAreReportedWithoutRetrying() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "12").setBody("{}"))
            val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
            val outcome = runCatching { execute(client.newCall(Request.Builder().url(server.url("/edit")).build())) }
            assertTrue(outcome.exceptionOrNull()?.message.orEmpty().contains("12 seconds"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun cancellingCoroutineCancelsUnderlyingHttpCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val client = OkHttpClient()
            val call = client.newCall(Request.Builder().url(server.url("/edit")).build())
            val job = launch(start = CoroutineStart.UNDISPATCHED) { execute(call) }
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            job.cancelAndJoin()
            assertTrue(call.isCanceled())
        }
    }

    @Test fun forkDoesNotClaimUpstreamPacksOrOtherBuildVariants() {
        assertEquals(BuildConfig.APPLICATION_ID + ".pack", IconPackBuilder.PACKAGE_NAME)
        assertTrue(IconPackBuilder.isOwnPack(IconPackBuilder.PACKAGE_NAME + ".p3"))
        assertFalse(IconPackBuilder.isOwnPack("dev.renkinProject.renkinpack"))
        assertFalse(IconPackBuilder.isOwnPack(IconPackBuilder.PACKAGE_NAME + "unrelated"))
    }
}
