package dev.renkinProject.renkin.ai

import okhttp3.RequestBody
import okhttp3.Request
import okio.BufferedSink

/** Keep multipart bytes/type/length intact but prohibit OkHttp's HTTP follow-up replay. */
internal fun RequestBody.oneShot(): RequestBody = object : RequestBody() {
    override fun contentType() = this@oneShot.contentType()
    override fun contentLength() = this@oneShot.contentLength()
    override fun isOneShot() = true
    override fun writeTo(sink: BufferedSink) = this@oneShot.writeTo(sink)
}

/** Preserve the existing native multipart factories. Pinned 4.12 MultipartBody does
 * not propagate one-shot flags from its children; the actual transport must wrap it. */
internal fun Request.withOneShotBody(): Request {
    require(method == "POST")
    return newBuilder().post(checkNotNull(body).oneShot()).build()
}
