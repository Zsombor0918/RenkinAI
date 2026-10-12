package dev.renkinProject.renkin.ai.trial

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.data.project.projectTestPng
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleTrialGatewayTest {
    private val credential = UUID.randomUUID().toString()
    private fun http(timeout: Long = 2000) = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).callTimeout(timeout, TimeUnit.MILLISECONDS).build()
    private fun adapter(s: MockWebServer, timeout: Long = 2000) = ClientIconGenerationGateway({ credential },
        OpenAiImageClient(http(timeout), s.url("/edits")), StabilityImageClient(http(timeout), s.url("/style"), s.url("/background")))
    private fun input(refs: Int = 0) = IconGenerationRequest(projectTestPng(Color.BLUE, 1024),
        (0 until refs).map { projectTestPng(Color.rgb(30 + it * 20, 50, 80)) }, "saved project style",
        composedPrompt = dev.renkinProject.renkin.ai.style.PromptComposer.composeSaved("saved project style", "separate project instruction"))
    private fun response(png: ByteArray = projectTestPng(size = 1024)) = MockResponse().setBody(
        "{\"data\":[{\"b64_json\":\"${Base64.encodeToString(png, Base64.NO_WRAP)}\"}]}")

    @Test fun unsafe503ControlRepeatsButRealOpenAiMultipartIsOneShotAndUncertain() = runBlocking {
        MockWebServer().use { s ->
            s.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0")); s.enqueue(MockResponse().setBody("ok"))
            withContext(Dispatchers.IO) { http().newCall(Request.Builder().url(s.url("/unsafe")).post("repeatable".toRequestBody()).build())
                .execute().use { assertEquals(200, it.code) } }
            assertEquals(2, s.requestCount); repeat(2) { assertEquals("POST", s.takeRequest().method) }
            s.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0")); s.enqueue(response())
            var endpoints = 0
            assertTrue(adapter(s).generate(input().copy(beforeEndpoint = { assertEquals(1, it); endpoints++ })) is IconGenerationOutcome.Uncertain)
            assertEquals(1, endpoints); assertEquals(3, s.requestCount); assertEquals("POST", s.takeRequest().method)
            val settings = AiSettings(referenceNames = emptyList())
            assertTrue(buildImageEditRequest(credential, settings, listOf(input().originalPng)).body!!.isOneShot())
        }
    }
    @Test fun realBodyKeepsOriginalFirstOrderedRefsAndNoLocalMetadataOrSecrets() = runBlocking {
        MockWebServer().use { s ->
            for (n in 0..5) {
                s.enqueue(response()); val request = input(n).copy(profileId = 42, styleRevision = 7)
                assertTrue(adapter(s).generate(request) is IconGenerationOutcome.Generated)
                val recorded = s.takeRequest(5, TimeUnit.SECONDS)!!
                assertEquals("Bearer $credential", recorded.getHeader("Authorization"))
                val text = String(recorded.body.readByteArray(), Charsets.ISO_8859_1)
                var last = -1
                (listOf(request.originalPng) + request.referencePngs).forEach { png ->
                    val at = text.indexOf(String(png, Charsets.ISO_8859_1)); assertTrue(at > last); last = at
                }
                for (value in listOf("transparent", "1024x1024", "output_format", "png", "separate project instruction", request.model, request.quality)) assertTrue(text.contains(value))
                for (value in listOf(credential, request.requestId, "profileId", "source.pack", "componentKey")) assertFalse(text.contains(value))
                assertEquals(n + 1, Regex("name=\"image\\[\\]\"").findAll(text).count())
            }
            assertEquals(6, s.requestCount)
        }
    }
    @Test fun stabilityBothRealEndpointsAreOneShotAndFailureCannotRepeatEitherStep() = runBlocking {
        for (failedStep in 1..2) MockWebServer().use { s ->
            if (failedStep == 2) s.enqueue(MockResponse().setBody(Buffer().write(projectTestPng(Color.RED, 1024))))
            s.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0")); s.enqueue(MockResponse().setBody(Buffer().write(projectTestPng(size = 1024))))
            val steps = mutableListOf<Int>()
            assertTrue(adapter(s).generate(input(1).copy(provider = AiSettings.STABILITY, beforeEndpoint = { steps += it })) is IconGenerationOutcome.Uncertain)
            assertEquals((1..failedStep).toList(), steps); assertEquals(failedStep, s.requestCount)
            assertEquals("/style", s.takeRequest().path)
            if (failedStep == 2) assertEquals("/background", s.takeRequest().path)
            val settings = AiSettings(provider = AiSettings.STABILITY, referenceNames = listOf("img_0_0.png"))
            assertTrue(buildStabilityStyleRequest(credential, settings, input().originalPng, input(1).referencePngs.single()).body!!.isOneShot())
            assertTrue(buildStabilityRemoveBackgroundRequest(credential, projectTestPng(size = 1024)).body!!.isOneShot())
        }
    }
    @Test fun malformedFirstStabilityImageDoesNotStartSecondPaidEndpoint() = runBlocking {
        MockWebServer().use { s ->
            s.enqueue(MockResponse().setBody("not a PNG")); s.enqueue(MockResponse().setBody(Buffer().write(projectTestPng(size = 1024))))
            val steps = mutableListOf<Int>()
            assertTrue(adapter(s).generate(input(1).copy(provider = AiSettings.STABILITY, beforeEndpoint = { steps += it })) is IconGenerationOutcome.Uncertain)
            assertEquals(listOf(1), steps); assertEquals(1, s.requestCount)
        }
    }
    @Test fun redirectsAuthUnavailableRateAndServerErrorsHaveExactlyOnePostEach() = runBlocking {
        for (status in listOf(307, 308, 401, 403, 404, 429, 500, 503)) MockWebServer().use { s ->
            s.enqueue(MockResponse().setResponseCode(status).addHeader("Location", s.url("/again")).addHeader("Retry-After", "0"))
            s.enqueue(response())
            assertTrue(adapter(s).generate(input()) is IconGenerationOutcome.Uncertain)
            assertEquals(1, s.requestCount); assertEquals("POST", s.takeRequest().method)
        }
    }
    @Test fun invalidJsonBase64CrcOpaqueEmptyAndOversizeResponsesNeverSucceedOrRetry() = runBlocking {
        val opaque = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val opaquePng = try { opaque.pngBytes() } finally { opaque.recycle() }
        val broken = projectTestPng(size = 1024).also { it[it.size - 1] = (it.last().toInt() xor 1).toByte() }
        val cases = listOf(MockResponse().setBody("{broken"), MockResponse().setBody("{\"data\":[{\"b64_json\":\"???\"}]}"),
            response(broken), response(opaquePng), response(projectTestPng(Color.TRANSPARENT, 1024)), response(ByteArray(8 * 1024 * 1024 + 1)))
        MockWebServer().use { s ->
            cases.forEach { r -> s.enqueue(r); assertTrue(adapter(s).generate(input()) is IconGenerationOutcome.Uncertain) }
            assertEquals(cases.size, s.requestCount)
        }
    }
    @Test fun tcpTimeoutAndCancellationPreserveUnknownBillingAndDoNotRetry() = runBlocking {
        for (policy in listOf(SocketPolicy.DISCONNECT_AFTER_REQUEST, SocketPolicy.NO_RESPONSE)) MockWebServer().use { s ->
            s.enqueue(MockResponse().setSocketPolicy(policy))
            assertTrue(adapter(s, 150).generate(input()) is IconGenerationOutcome.Uncertain); assertEquals(1, s.requestCount)
        }
        MockWebServer().use { s ->
            s.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)); var cancelled: GenerationCancelled? = null
            val job = launch { try { adapter(s).generate(input()) } catch (e: GenerationCancelled) { cancelled = e; throw e } }
            withContext(Dispatchers.IO) { assertNotNull(s.takeRequest(5, TimeUnit.SECONDS)) }; job.cancelAndJoin()
            assertTrue(cancelled!!.billingUncertain); assertEquals(1, s.requestCount)
        }
    }
}
