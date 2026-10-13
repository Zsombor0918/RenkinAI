package dev.renkinProject.renkin.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Stability's style-transfer endpoint takes one target and one style image, then a separate
 * remove-background call supplies actual PNG alpha. Each endpoint charges credits on success. */
@Singleton
class StabilityImageClient internal constructor(private val client: OkHttpClient,
    private val styleEndpoint: HttpUrl, private val backgroundEndpoint: HttpUrl) {
    @Inject constructor() : this(defaultClient(),
        "https://api.stability.ai/v2beta/stable-image/control/style-transfer".toHttpUrl(),
        "https://api.stability.ai/v2beta/stable-image/edit/remove-background".toHttpUrl())
    companion object {
    private fun defaultClient() = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
    }

    suspend fun edit(key: String, settings: AiSettings, source: ByteArray, styleReference: ByteArray,
        promptOverride: String? = null, beforeEndpoint: suspend (Int) -> Unit = {}): ByteArray =
        withContext(Dispatchers.IO) {
            require(source.size in 1..4 * 1024 * 1024 && styleReference.size in 1..4 * 1024 * 1024)
            val first = buildStabilityStyleRequest(key, settings, source, styleReference, promptOverride).newBuilder().url(styleEndpoint).build().withOneShotBody()
            beforeEndpoint(1)
            val stylized = requestBytes(client.newCall(first))
            dev.renkinProject.renkin.data.project.inspectPng(stylized, MAX_AI_RESPONSE_BYTES)
            val second = buildStabilityRemoveBackgroundRequest(key, stylized).newBuilder().url(backgroundEndpoint).build().withOneShotBody()
            beforeEndpoint(2)
            requestBytes(client.newCall(second))
        }
}

internal fun buildStabilityStyleRequest(
    key: String, settings: AiSettings, source: ByteArray, reference: ByteArray, promptOverride: String? = null
): Request {
    settings.validate()
    require(settings.provider == AiSettings.STABILITY)
    require(promptOverride == null || promptOverride.isNotBlank() && promptOverride.length <= MAX_COMPOSED_ICON_PROMPT)
    val prompt = """
        Redraw the recognizable symbol from the input app icon as one minimal Android launcher icon.
        Preserve its distinctive app identity and composition. Apply this shared icon-pack style:
        ${settings.style}
        App-specific instruction (keep the shared style and output rules): ${settings.appInstruction}
        Square canvas, centered artwork, balanced padding, simple silhouette, no lettering, no watermark,
        no tile or scene. The next API step removes the background and makes real transparency.
    """.trimIndent()
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("init_image", "app-icon.png", source.toRequestBody("image/png".toMediaType()))
        .addFormDataPart("style_image", "style-reference.png", reference.toRequestBody("image/png".toMediaType()))
        .addFormDataPart("prompt", promptOverride ?: prompt)
        .addFormDataPart("composition_fidelity", "0.9")
        .addFormDataPart("change_strength", "0.7")
        .addFormDataPart("output_format", "png")
        .build()
    return Request.Builder().url("https://api.stability.ai/v2beta/stable-image/control/style-transfer")
        .header("Authorization", "Bearer $key").header("Accept", "image/*")
        .post(body).build()
}

internal fun buildStabilityRemoveBackgroundRequest(key: String, stylized: ByteArray): Request {
    require(stylized.isNotEmpty() && stylized.size <= MAX_AI_RESPONSE_BYTES)
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("image", "stylized.png", stylized.toRequestBody("image/png".toMediaType()))
        .addFormDataPart("output_format", "png")
        .build()
    return Request.Builder().url("https://api.stability.ai/v2beta/stable-image/edit/remove-background")
        .header("Authorization", "Bearer $key").header("Accept", "image/*")
        .post(body).build()
}

private suspend fun requestBytes(call: Call): ByteArray = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(IOException(
                "Connection interrupted or timed out. One request may already have used credits. " +
                    "No automatic retry was made; check your Stability balance before trying again."
            ))
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val bytes = response.use {
                    val body = it.body ?: throw IOException("The image service returned an empty response.")
                    val limit = if (it.isSuccessful) MAX_AI_RESPONSE_BYTES else 64 * 1024
                    val stream = body.byteStream()
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (call.isCanceled()) throw IOException("Cancelled")
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > limit) throw IOException("The image service response exceeds the safety limit.")
                        output.write(buffer, 0, count)
                    }
                    if (!it.isSuccessful) throw ImageApiException(it.code, "http_${it.code}", stabilityErrorMessage(it.code))
                    output.toByteArray()
                }
                if (continuation.isActive) continuation.resume(bytes)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(
                    if (error is ImageApiException) error else IOException(error.message?.take(350) ?: "The image service response was invalid.")
                )
            }
        }
    })
}

internal fun stabilityErrorMessage(status: Int): String = when (status) {
    401 -> "Stability API key rejected. Replace it in AI settings."
    402 -> "Stability trial credits are exhausted. Check your account balance."
    403 -> "Stability denied the request. Check API access and trial credits."
    429 -> "Stability rate limit reached. No automatic retry was made."
    in 400..499 -> "Stability rejected the request (HTTP $status). Check the input icon and account access."
    else -> "Stability image service failed (HTTP $status). No automatic retry was made."
}
