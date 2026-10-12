package dev.renkinProject.renkin.ai

import okhttp3.RequestBody
import okio.BufferedSink

/** Keep multipart bytes/type/length intact but prohibit OkHttp's HTTP follow-up replay. */
internal fun RequestBody.oneShot(): RequestBody = object : RequestBody() {
    override fun contentType() = this@oneShot.contentType()
    override fun contentLength() = this@oneShot.contentLength()
    override fun isOneShot() = true
    override fun writeTo(sink: BufferedSink) = this@oneShot.writeTo(sink)
}
