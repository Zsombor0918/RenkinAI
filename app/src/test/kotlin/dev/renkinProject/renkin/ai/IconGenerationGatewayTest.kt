package dev.renkinProject.renkin.ai

import android.app.Application
import android.graphics.Color
import android.util.Base64
import dev.renkinProject.renkin.data.project.projectTestPng
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconGenerationGatewayTest {
    private val offlineCredential = UUID.randomUUID().toString()
    private fun adapter(server: MockWebServer, timeout: Long = 3000) = ClientIconGenerationGateway(
        { offlineCredential },
        OpenAiImageClient(http(timeout), server.url("/openai")),
        StabilityImageClient(http(timeout), server.url("/style"), server.url("/background")))
    private fun http(timeout: Long) = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).callTimeout(timeout, TimeUnit.MILLISECONDS).build()
    private fun request(references: Int = 0) = IconGenerationRequest(projectTestPng(Color.WHITE),
        (0 until references).map { projectTestPng(Color.rgb(50 + it * 20, 20, 100)) }, "profile-specific monoline",
        "app instruction", profileId = 4, styleRevision = 2)
    private fun response(): MockResponse {
        val encoded = Base64.encodeToString(projectTestPng(Color.WHITE, 512), Base64.NO_WRAP)
        return MockResponse().setBody("{\"data\":[{\"b64_json\":\"$encoded\"}]}")
    }

    @Test fun realOpenAiAdapterSendsOriginalThenZeroThroughFiveReferencesExactlyOnce() = runBlocking {
        MockWebServer().use { server ->
            val gateway = adapter(server)
            for (count in 0..5) {
                server.enqueue(response())
                val input = request(count)
                val result = gateway.generate(input) as IconGenerationOutcome.Generated
                val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
                val body = recorded.body.readByteArray()
                val text = String(body, Charsets.ISO_8859_1)
                assertEquals("POST", recorded.method)
                assertEquals("/openai", recorded.path)
                assertEquals(count + 1, Regex("name=\"image\\[\\]\"").findAll(text).count())
                var previous = -1
                for (png in listOf(input.originalPng) + input.referencePngs) {
                    val position = text.indexOf(String(png, Charsets.ISO_8859_1))
                    assertTrue(position > previous); previous = position
                }
                assertTrue(text.contains(input.specification))
                assertTrue(text.contains(input.appInstruction))
                assertTrue(text.contains("transparent"))
                assertFalse(text.contains(offlineCredential))
                assertFalse(text.contains("profileId"))
                assertFalse(text.contains(input.requestId))
                assertEquals(input.requestId, result.requestId)
                assertEquals(1, result.endpointCalls)
                val bitmap = validateAiPng(result.png)
                assertEquals(1024, bitmap.width); bitmap.recycle()
                assertEquals(count + 1, server.requestCount)
            }
        }
    }

    @Test fun stabilityUsesOneStyleReferenceAndTwoExistingClientRequests() = runBlocking {
        MockWebServer().use { server ->
            val intermediate = projectTestPng(Color.RED, 512)
            server.enqueue(MockResponse().setBody(Buffer().write(intermediate)))
            server.enqueue(MockResponse().setBody(Buffer().write(projectTestPng(Color.WHITE, 512))))
            val result = adapter(server).generate(request(1).copy(provider = AiSettings.STABILITY)) as IconGenerationOutcome.Generated
            val first = server.takeRequest(5, TimeUnit.SECONDS)!!
            val second = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/style", first.path)
            assertEquals("/background", second.path)
            assertTrue(first.body.readUtf8().contains("name=\"style_image\""))
            assertTrue(String(second.body.readByteArray(), Charsets.ISO_8859_1).contains(String(intermediate, Charsets.ISO_8859_1)))
            assertEquals(2, server.requestCount)
            assertEquals(2, result.endpointCalls)
        }
    }

    @Test fun invalidImagesLimitsProviderAndCredentialsRejectBeforeAnyPost() = runBlocking {
        MockWebServer().use { server ->
            val gateway = adapter(server)
            for (input in listOf(request().copy(originalPng = ByteArray(128)), request(6), request().copy(specification = ""),
                request().copy(provider = "unknown"), request().copy(provider = AiSettings.STABILITY),
                request(2).copy(provider = AiSettings.STABILITY))) {
                assertTrue(gateway.generate(input) is IconGenerationOutcome.InvalidInput)
            }
            val noKey = ClientIconGenerationGateway({ error("secret exception") },
                OpenAiImageClient(http(3000), server.url("/openai")), StabilityImageClient(http(3000), server.url("/style"), server.url("/background")))
            assertTrue(noKey.generate(request()) is IconGenerationOutcome.CredentialsUnavailable)
            assertEquals(0, server.requestCount)
        }
    }

    @Test fun authenticationRateLimitInvalidResponseAndTimeoutNeverRetry() = runBlocking {
        MockWebServer().use { server ->
            val gateway = adapter(server)
            for (status in listOf(401, 403, 429, 500)) {
                server.enqueue(MockResponse().setResponseCode(status).setBody("private-server-message"))
                assertTrue(gateway.generate(request()) is IconGenerationOutcome.Uncertain)
            }
            server.enqueue(MockResponse().setBody("{\"data\":[{\"b64_json\":\"aW52YWxpZA==\"}]}"))
            assertTrue(gateway.generate(request()) is IconGenerationOutcome.Uncertain)
            assertEquals(5, server.requestCount)
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            assertTrue(adapter(server, 100).generate(request()) is IconGenerationOutcome.Uncertain)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun cancellationCancelsRealPostAndPreservesTypedBillingUncertainty() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            var cancellation: GenerationCancelled? = null
            val job = launch { try { adapter(server).generate(request()) }
                catch (error: GenerationCancelled) { cancellation = error; throw error } }
            withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(5, TimeUnit.SECONDS)) }
            job.cancelAndJoin()
            assertNotNull(cancellation)
            assertTrue(cancellation!!.billingUncertain)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun fakePortCountsCallsAndKeepsTwoProfileStylesIndependent() = runBlocking {
        val observed = mutableListOf<IconGenerationRequest>()
        val fake = IconGenerationGateway { input ->
            observed += input
            IconGenerationOutcome.Generated(projectTestPng(Color.WHITE, 512), input.requestId, input.provider, 512, 512, 0)
        }
        val first = request().copy(profileId = 1, specification = "white outline")
        val second = request().copy(profileId = 2, specification = "filled red")
        fake.generate(first); fake.generate(second)
        assertEquals(2, observed.size)
        assertEquals(listOf("white outline", "filled red"), observed.map { it.specification })
        assertEquals(listOf(1L, 2L), observed.map { it.profileId })
        assertNotEquals(observed[0].requestId, observed[1].requestId)
    }
}
