package dev.renkinProject.renkin.ai

import android.util.Base64
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
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val MAX_AI_RESPONSE_BYTES = 16 * 1024 * 1024

@Singleton
class OpenAiImageClient internal constructor(private val client: OkHttpClient, private val endpoint: HttpUrl) {
    @Inject constructor() : this(defaultClient(), "https://api.openai.com/v1/images/edits".toHttpUrl())
    // No logging interceptor, redirects or automatic retries of a potentially billable POST.
    companion object {
    private fun defaultClient() = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS).readTimeout(240, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
    }

    suspend fun edit(key: String, settings: AiSettings, images: List<ByteArray>,
        promptOverride: String? = null, beforeEndpoint: suspend (Int) -> Unit = {}): ByteArray =
        withContext(Dispatchers.IO) {
            val request = buildImageEditRequest(key, settings, images, promptOverride).newBuilder().url(endpoint).build()
            beforeEndpoint(1)
            decodeImageResponse(execute(client.newCall(request)))
        }
}

internal fun buildImageEditRequest(key: String, settings: AiSettings, images: List<ByteArray>, promptOverride: String? = null): Request {
    settings.validate()
    require(promptOverride == null || promptOverride.isNotBlank() && promptOverride.length <= 16000)
    require(images.size == settings.referenceNames.size + 1 && images.size in 1..6)
    require(images.all { it.isNotEmpty() && it.size <= 4 * 1024 * 1024 }) { "An input icon is too large." }
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("model", settings.model)
        .addFormDataPart("prompt", promptOverride ?: iconPrompt(settings.style, images.size - 1, settings.appInstruction))
        .addFormDataPart("n", "1")
        .addFormDataPart("size", "1024x1024")
        .addFormDataPart("quality", settings.quality)
        .addFormDataPart("background", "transparent")
        .addFormDataPart("output_format", "png")
    images.forEachIndexed { index, png ->
        body.addFormDataPart("image[]", "input_$index.png", png.toRequestBody("image/png".toMediaType()))
    }
    return Request.Builder().url("https://api.openai.com/v1/images/edits")
        .header("Authorization", "Bearer $key").post(body.build().oneShot()).build()
}

internal suspend fun execute(call: Call): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            // Do not expose raw exceptions: URLs, headers or server messages may contain secrets.
            if (continuation.isActive) continuation.resumeWithException(IOException(
                "Connection interrupted or timed out. The request may already have been billed. " +
                    "It was not retried automatically; check your API usage before regenerating."
            ))
        }
        override fun onResponse(call: Call, response: Response) {
            try {
                val payload = response.use {
                    val body = it.body ?: throw IOException("The API returned an empty response.")
                    val limit = if (it.isSuccessful) MAX_AI_RESPONSE_BYTES else 64 * 1024
                    val stream = body.byteStream()
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (call.isCanceled()) throw IOException("Cancelled")
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > limit) throw IOException("The API response exceeds the safety limit.")
                        bytes.write(buffer, 0, count)
                    }
                    val text = bytes.toString("UTF-8")
                    if (!it.isSuccessful) throw IOException(apiErrorMessage(it.code, text, it.header("Retry-After")))
                    text
                }
                if (continuation.isActive) continuation.resume(payload)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(
                    IOException(if (error is IOException) error.message else "Invalid API response.")
                )
            }
        }
    })
}

internal fun apiErrorMessage(status: Int, body: String, retryAfter: String?): String {
    val code = runCatching { JSONObject(body).optJSONObject("error")?.optString("code") }.getOrNull()
    return when {
        code == "insufficient_quota" || code == "billing_hard_limit_reached" -> "API credit or spending limit reached. Check API billing; ChatGPT Plus does not pay for these requests."
        status == 401 -> "API key rejected (HTTP 401). Replace it in AI settings."
        status == 403 -> "Access denied (HTTP 403). Check the project's image permissions and model access."
        status == 429 -> "API rate limit reached (HTTP 429). " +
            (retryAfter?.toLongOrNull()?.coerceIn(1, 3600)?.let { "Wait at least $it seconds. " } ?: "Wait before trying again. ") +
            "No automatic retry was made."
        code in setOf("model_not_found", "invalid_model", "unsupported_model") -> "The configured image model is unavailable (HTTP $status). Select another supported model in AI settings."
        status in 400..499 -> "API request rejected (HTTP $status). Check model access and input images."
        else -> "API service failed (HTTP $status). No automatic retry was made."
    }
}

internal fun decodeImageResponse(payload: String): ByteArray {
    try {
        val encoded = JSONObject(payload).getJSONArray("data").getJSONObject(0).getString("b64_json")
        require(encoded.length in 1..(12 * 1024 * 1024))
        return Base64.decode(encoded, Base64.DEFAULT).also { require(it.size <= 8 * 1024 * 1024) }
    } catch (_: Exception) {
        throw IOException("The API did not return a valid, bounded PNG response.")
    }
}
