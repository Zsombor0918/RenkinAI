package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.util.Base64
import dev.renkinProject.renkin.apk.CoverageStamp
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
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
class StyleAnalysisGatewayTest {
    @Test fun analysisDescribesColorUsageAndGeometryWithoutCopyingSampleIdentities() {
        val body = analysisBody(analysisFixtureRequest())
        val rules = body.getString("instructions")
        for (term in listOf("FIXED_PALETTE", "ADAPT_BRAND_COLORS", "MONOCHROME", "DUOTONE", "UNKNOWN",
            "negative space", "shadow", "outlier", "never enumerate")) assertTrue(term, rules.contains(term))
        val prompt = PromptComposer.openAiEditPrompt("mixed brand-colored outlines", 5)
        assertTrue(prompt.contains("meaningful original colors"))
        assertTrue(prompt.contains("sample swatches are observations"))
        assertTrue(prompt.contains("Produce exactly ONE"))
        assertEquals("low", body.getJSONArray("input").getJSONObject(0).getJSONArray("content").getJSONObject(1).getString("detail"))
        assertEquals(canonicalJson(StyleProfileV1.jsonSchema()), canonicalJson(body.getJSONObject("text").getJSONObject("format").getJSONObject("schema")))
    }
    private val offlineCredential = UUID.randomUUID().toString()
    private fun gateway(server: MockWebServer, timeout: Long = 3000, key: () -> String = { offlineCredential }) =
        OpenAiStyleAnalysisGateway(key, OkHttpClient.Builder().callTimeout(timeout, TimeUnit.MILLISECONDS).build(), server.url("/v1/responses"))

    @Test fun realResponsesAdapterSendsBoundedOrderedImagesStrictTextSchemaAndNoLocalIdentity() = runBlocking {
        MockWebServer().use { server ->
            val port = gateway(server)
            for (count in listOf(1, 5, 8, 12)) {
                server.enqueue(MockResponse().setBody(analysisFixtureResponse()))
                val own = analysisFixtureRequest(images = count)
                val context = own.context.copy(mode = "EXTEND", sourcePackage = "private.source.pack",
                    setupStamp = "a".repeat(64), referenceStamp = "b".repeat(64), sourceIdentity = CoverageStamp("private.source.pack", 1, 1, 1, false))
                val request = own.copy(context = context)
                val result = port.analyze(request) as StyleAnalysisOutcome.Suggested
                val wire = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                assertEquals("POST", wire.method); assertEquals("/v1/responses", wire.path)
                val raw = wire.body.readUtf8(); val body = JSONObject(raw)
                assertFalse(raw.contains(offlineCredential)); assertFalse(raw.contains("private.source.pack"))
                listOf("profileId", "catalogStamp", "selectionHash", "fingerprint", "storageEpoch", "project_ai_assets/", request.requestId)
                    .forEach { assertFalse(it, raw.contains(it)) }
                assertTrue(wire.getHeader("Authorization") == "Bearer $offlineCredential")
                assertEquals(request.requestId, wire.getHeader("X-Client-Request-Id"))
                assertEquals(DEFAULT_ANALYSIS_MODEL, body.getString("model")); assertFalse(body.getBoolean("store")); assertFalse(body.getBoolean("stream"))
                assertEquals(4096, body.getInt("max_output_tokens")); assertFalse(body.has("response_format"))
                val format = body.getJSONObject("text").getJSONObject("format")
                assertEquals("json_schema", format.getString("type")); assertTrue(format.getBoolean("strict"))
                assertEquals(canonicalJson(StyleProfileV1.jsonSchema()), canonicalJson(format.getJSONObject("schema")))
                val parts = body.getJSONArray("input").getJSONObject(0).getJSONArray("content")
                assertEquals(count + 1, parts.length()); assertEquals("input_text", parts.getJSONObject(0).getString("type"))
                assertTrue(parts.getJSONObject(0).getString("text").contains(request.userInstruction))
                for (i in request.images.indices) {
                    val part = parts.getJSONObject(i + 1)
                    assertEquals("input_image", part.getString("type")); assertEquals("low", part.getString("detail"))
                    val data = part.getString("image_url"); assertTrue(data.startsWith("data:image/png;base64,"))
                    assertArrayEquals(request.images[i].png, Base64.decode(data.substringAfter(','), Base64.DEFAULT))
                }
                assertEquals(analysisFixtureStyle(), result.style); assertEquals(AnalysisUsage(43, 92), result.usage)
            }
            assertEquals(4, server.requestCount)
        }
    }
    @Test fun malformedInputsOversizedImagesDuplicatesUnknownModelAndNoKeyCauseZeroPosts() = runBlocking {
        MockWebServer().use { server ->
            val good = analysisFixtureRequest(); val port = gateway(server)
            val invalid = listOf(good.copy(images = emptyList()), analysisFixtureRequest(images = 13),
                good.copy(images = good.images + good.images), good.copy(model = "gpt-image-1"),
                good.copy(detail = "unbounded"), good.copy(description = "x".repeat(6001)),
                good.copy(images = listOf(AnalysisImage(good.images[0].hash, ByteArray(128)))))
            for (request in invalid) assertEquals(StyleAnalysisOutcome.InvalidInput, port.analyze(request))
            assertEquals(StyleAnalysisOutcome.Unauthorized, gateway(server, key = { "" }).analyze(good))
            assertEquals(StyleAnalysisOutcome.Unauthorized, gateway(server, key = { error("credential read failed") }).analyze(good))
            assertEquals(0, server.requestCount)
        }
    }
    @Test fun authenticationModelRateLimitsAndServerFailuresAreTypedAndNeverRetry() = runBlocking {
        MockWebServer().use { server ->
            val port = gateway(server)
            val expected = listOf(401 to StyleAnalysisOutcome.Unauthorized, 403 to StyleAnalysisOutcome.Unauthorized,
                404 to StyleAnalysisOutcome.Unavailable, 429 to StyleAnalysisOutcome.RateLimited, 500 to StyleAnalysisOutcome.Uncertain,
                503 to StyleAnalysisOutcome.Uncertain, 400 to StyleAnalysisOutcome.RequestRejected(400),
                402 to StyleAnalysisOutcome.RequestRejected(402), 422 to StyleAnalysisOutcome.RequestRejected(422))
            for ((code, outcome) in expected) {
                server.enqueue(MockResponse().setResponseCode(code).setBody("private server error"))
                assertEquals(outcome, port.analyze(analysisFixtureRequest()))
            }
            assertEquals(expected.size, server.requestCount)
        }
    }
    @Test fun quotaCodeIsAllowlistedWithoutRetainingSensitiveErrorProse() = runBlocking {
        MockWebServer().use { server ->
            val port = gateway(server)
            val secret = "private key prompt and project identifiers"
            server.enqueue(MockResponse().setResponseCode(429).setBody(json("error" to
                json("code" to "insufficient_quota", "message" to secret)).toString()))
            assertEquals(StyleAnalysisOutcome.QuotaExceeded, port.analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setResponseCode(429).setBody(json("error" to
                json("code" to secret, "message" to secret)).toString()))
            assertEquals(StyleAnalysisOutcome.RateLimited, port.analyze(analysisFixtureRequest()))
            assertEquals(2, server.requestCount)
        }
    }
    @Test fun refusalMalformedSchemaIncompleteAndMissingUsageDoNotInventSuccessOrCost() = runBlocking {
        MockWebServer().use { server ->
            val port = gateway(server)
            val refused = JSONObject(analysisFixtureResponse()).apply { getJSONArray("output").getJSONObject(1)
                .put("content", JSONArray().put(json("type" to "refusal", "refusal" to "no"))) }
            server.enqueue(MockResponse().setBody(refused.toString()))
            assertEquals(StyleAnalysisOutcome.Refused, port.analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setBody(JSONObject(analysisFixtureResponse()).put("status", "incomplete").toString()))
            assertEquals(StyleAnalysisOutcome.InvalidResponse, port.analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setBody(analysisFixtureResponse().replace("outline", "not-a-category")))
            assertEquals(StyleAnalysisOutcome.InvalidResponse, port.analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setBody(analysisFixtureResponse(usage = false)))
            assertNull((port.analyze(analysisFixtureRequest()) as StyleAnalysisOutcome.Suggested).usage)
            server.enqueue(MockResponse().setBody(JSONObject(analysisFixtureResponse()).put("usage", json("input_tokens" to "43", "output_tokens" to 2)).toString()))
            assertNull((port.analyze(analysisFixtureRequest()) as StyleAnalysisOutcome.Suggested).usage)
            assertEquals(5, server.requestCount)
        }
    }
    @Test fun invalidUtf8OversizedBodyTimeoutAndRedirectAreNotReplayed() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(Buffer().write(byteArrayOf(0xc3.toByte(), 0x28))))
            assertEquals(StyleAnalysisOutcome.Uncertain, gateway(server).analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setBody("x".repeat(128 * 1024 + 1)))
            assertEquals(StyleAnalysisOutcome.Uncertain, gateway(server).analyze(analysisFixtureRequest()))
            server.enqueue(MockResponse().setResponseCode(307).addHeader("Location", server.url("/second")))
            assertEquals(StyleAnalysisOutcome.Unavailable, gateway(server).analyze(analysisFixtureRequest()))
            assertEquals(3, server.requestCount)
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            assertEquals(StyleAnalysisOutcome.Uncertain, gateway(server, 100).analyze(analysisFixtureRequest()))
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun cancellationStopsOneRealHttpCallWithoutReconnectingOrRetrying() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val job = launch { gateway(server).analyze(analysisFixtureRequest()) }
            withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(5, TimeUnit.SECONDS)) }
            job.cancelAndJoin(); assertTrue(job.isCancelled); assertEquals(1, server.requestCount)
        }
    }
    @Test fun serviceRetryAfterZeroCannotReplayOneBillableAnalysisBody() = runBlocking {
        MockWebServer().use { server ->
            // Negative control: the pinned client's connection flag alone permits
            // a 503 HTTP follow-up. This is local mock traffic, never a provider.
            server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0").setBody("busy"))
            server.enqueue(MockResponse().setBody(analysisFixtureResponse()))
            withContext(Dispatchers.IO) {
                OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).build()
                    .newCall(Request.Builder().url(server.url("/v1/responses"))
                        .post("{}".toRequestBody("application/json".toMediaType())).build())
                    .execute().use { assertEquals(200, it.code) }
            }
            assertEquals(2, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(503).addHeader("Retry-After", "0").setBody("busy"))
            server.enqueue(MockResponse().setBody(analysisFixtureResponse()))
            val before = server.requestCount
            assertEquals(StyleAnalysisOutcome.Uncertain, gateway(server).analyze(analysisFixtureRequest()))
            assertEquals("One consent must submit at most one POST", before + 1, server.requestCount)
        }
    }
}
