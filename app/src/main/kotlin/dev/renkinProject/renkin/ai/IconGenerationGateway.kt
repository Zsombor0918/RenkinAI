package dev.renkinProject.renkin.ai

import dev.renkinProject.renkin.data.project.inspectPng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class IconGenerationRequest(
    val originalPng: ByteArray,
    val referencePngs: List<ByteArray>,
    val specification: String,
    val appInstruction: String = "",
    val provider: String = AiSettings.OPENAI,
    val model: String = AiSettings.MODELS.first(),
    val quality: String = "medium",
    val requestId: String = UUID.randomUUID().toString(),
    val profileId: Long? = null,
    val styleRevision: Long? = null,
    val composedPrompt: String? = null,
    val beforeEndpoint: suspend (Int) -> Unit = {}
)

sealed interface IconGenerationOutcome {
    data class Generated(val png: ByteArray, val requestId: String, val provider: String,
        val width: Int, val height: Int, val endpointCalls: Int) : IconGenerationOutcome
    data class InvalidInput(val requestId: String) : IconGenerationOutcome
    data class CredentialsUnavailable(val requestId: String) : IconGenerationOutcome
    /** Once submitted, rejection/timeout/invalid response can never authorize an automatic retry. */
    data class Uncertain(val requestId: String, val provider: String) : IconGenerationOutcome
}

/** Cancellation propagates as [GenerationCancelled]: the caller must preserve uncertain billing.
 * There is no retry, scheduling, pricing, persistence or apply operation in this port. */
fun interface IconGenerationGateway {
    suspend fun generate(request: IconGenerationRequest): IconGenerationOutcome
}

class GenerationCancelled(val requestId: String, val billingUncertain: Boolean,
    cause: CancellationException) : CancellationException("Generation cancelled; do not retry automatically.") {
    init { initCause(cause) }
}

/** Real future-workflow adapter. Legacy AI1/AI2 deliberately retain their existing service. */
@Singleton
class ClientIconGenerationGateway internal constructor(
    private val readKey: (String) -> String,
    private val openAi: OpenAiImageClient,
    private val stability: StabilityImageClient
) : IconGenerationGateway {
    @Inject constructor(credentials: AiCredentialStore, openAi: OpenAiImageClient, stability: StabilityImageClient) :
        this(credentials::read, openAi, stability)

    override suspend fun generate(request: IconGenerationRequest): IconGenerationOutcome {
        var submitted = false
        try {
            val settings = try {
                require(UUID.fromString(request.requestId).toString() == request.requestId)
                require(request.profileId == null || request.profileId > 0)
                require(request.styleRevision == null || request.styleRevision > 0)
                require(request.referencePngs.size <= 5)
                require(request.composedPrompt == null || request.composedPrompt.isNotBlank() && request.composedPrompt.length <= 16000)
                require(request.provider != AiSettings.STABILITY || request.referencePngs.size == 1)
                (listOf(request.originalPng) + request.referencePngs).forEach { inspectPng(it) }
                AiSettings(request.provider, request.specification, request.model, request.quality,
                    request.referencePngs.indices.map { "img_0_$it.png" }, request.appInstruction).also { it.validate() }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { return IconGenerationOutcome.InvalidInput(request.requestId) }
            val key = try { readKey(request.provider) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { return IconGenerationOutcome.CredentialsUnavailable(request.requestId) }
            currentCoroutineContext().ensureActive()
            submitted = true
            val bytes = when (settings.provider) {
                AiSettings.OPENAI -> openAi.edit(key, settings, listOf(request.originalPng) + request.referencePngs,
                    request.composedPrompt, request.beforeEndpoint)
                AiSettings.STABILITY -> stability.edit(key, settings, request.originalPng, request.referencePngs.single(),
                    request.composedPrompt, request.beforeEndpoint)
                else -> return IconGenerationOutcome.InvalidInput(request.requestId)
            }
            inspectPng(bytes, 8 * 1024 * 1024)
            val normalized = validateAiPng(bytes)
            val png = try { normalized.pngBytes() } finally { normalized.recycle() }
            return IconGenerationOutcome.Generated(png, request.requestId, request.provider, 1024, 1024,
                if (request.provider == AiSettings.STABILITY) 2 else 1)
        } catch (error: CancellationException) {
            throw GenerationCancelled(request.requestId, submitted, error)
        } catch (_: Exception) {
            return IconGenerationOutcome.Uncertain(request.requestId, request.provider)
        }
    }
}
