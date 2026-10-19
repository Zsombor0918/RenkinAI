package dev.renkinProject.renkin.ai.style

import android.graphics.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.*

internal const val MONTAGE_CELL = 288 // 256 source pixels, 16 transparent pixels on each side.
internal const val MONTAGE_PAGE_CAPACITY = 15 // At most three rows and five columns; no source downsampling.
internal const val MONTAGE_MAX_MEMBERS = 60 // Four pages within the existing ordered reference/input boundary.
internal const val MONTAGE_STARTING_BRIEF = "Match the visual language of the pack shown in the contact sheets while preserving the target application's recognizable identity."

internal data class MontageMember(val component: String, val drawable: String, val hash: String) {
    fun json() = json("component" to component, "drawable" to drawable, "hash" to hash)
}
internal data class MontagePage(val hash: String, val width: Int, val height: Int, val count: Int) {
    fun json() = json("hash" to hash, "width" to width, "height" to height, "count" to count)
}
internal data class PackStyleResult(val prompt: String, val summary: String, val requestId: String,
    val responseId: String, val model: String) {
    fun json() = json("prompt" to prompt, "summary" to summary, "requestId" to requestId, "responseId" to responseId, "model" to model)
}

/** Local source/byte provenance, not a visual classification schema. Stored in existing revision JSON. */
internal data class PackMontage(val source: String, val setupStamp: String, val referenceStamp: String,
    val members: List<MontageMember>, val pages: List<MontagePage>, val result: PackStyleResult? = null) {
    fun encode(): String {
        require(source.matches(Regex("[a-zA-Z0-9_.]{1,255}")))
        listOf(setupStamp, referenceStamp).forEach(::dnaHash)
        require(members.size in 1..MONTAGE_MAX_MEMBERS && members.map { it.hash }.distinct().size == members.size)
        members.forEach { require(it.component.length in 1..1000 && it.drawable.length in 1..255); dnaHash(it.hash) }
        require(pages.size == (members.size + MONTAGE_PAGE_CAPACITY - 1) / MONTAGE_PAGE_CAPACITY)
        require(pages.sumOf { it.count } == members.size && pages.map { it.hash }.distinct().size == pages.size)
        pages.forEach { page ->
            dnaHash(page.hash); require(page.count in 1..MONTAGE_PAGE_CAPACITY)
            val layout = montageLayout(page.count)
            require(page.width == layout.first * MONTAGE_CELL && page.height == layout.second * MONTAGE_CELL)
        }
        result?.let { r ->
            require(r.prompt.isNotBlank()); cleanText(r.prompt, 6000, true); cleanText(r.summary, 240, true)
            require(java.util.UUID.fromString(r.requestId).toString() == r.requestId)
            require(r.responseId.matches(Regex("[A-Za-z0-9_-]{1,128}")) && r.model in ANALYSIS_MODELS)
        }
        return canonicalJson(json("type" to "pack_montage", "schema" to 1, "layout" to 1, "source" to source,
            "setupStamp" to setupStamp, "referenceStamp" to referenceStamp, "members" to JSONArray(members.map { it.json() }),
            "pages" to JSONArray(pages.map { it.json() }), "result" to result?.json())).also { require(it.toByteArray().size <= 32768) }
    }
    val fingerprint get() = sha256(copy(result = null).encode().toByteArray())
    companion object {
        fun optional(text: String?): PackMontage? {
            if (text == null || strictObject(text, 128 * 1024, 12).optString("type") != "pack_montage") return null
            return parse(text)
        }
        fun parse(text: String): PackMontage {
            val r = strictObject(text, 32768, 8)
            r.exactKeys(setOf("type", "schema", "layout", "source", "setupStamp", "referenceStamp", "members", "pages", "result"))
            require(r.stringValue("type") == "pack_montage" && r.longValue("schema") == 1L && r.longValue("layout") == 1L)
            val members = r.getJSONArray("members"); require(members.length() in 1..MONTAGE_MAX_MEMBERS)
            val pages = r.getJSONArray("pages"); require(pages.length() in 1..4)
            return PackMontage(r.stringValue("source"), r.stringValue("setupStamp"), r.stringValue("referenceStamp"),
                (0 until members.length()).map { members.getJSONObject(it).let { m ->
                    m.exactKeys(setOf("component", "drawable", "hash")); MontageMember(m.stringValue("component"), m.stringValue("drawable"), m.stringValue("hash")) } },
                (0 until pages.length()).map { pages.getJSONObject(it).let { p ->
                    p.exactKeys(setOf("hash", "width", "height", "count")); MontagePage(p.stringValue("hash"), p.longValue("width").toInt(), p.longValue("height").toInt(), p.longValue("count").toInt()) } },
                if (r.isNull("result")) null else r.getJSONObject("result").let { v ->
                    v.exactKeys(setOf("prompt", "summary", "requestId", "responseId", "model"))
                    PackStyleResult(v.stringValue("prompt"), v.stringValue("summary"), v.stringValue("requestId"), v.stringValue("responseId"), v.stringValue("model")) }).also { require(it.encode() == canonicalJson(r)) }
        }
    }
}

internal data class MontageEvidence(val profileId: Long, val payloadHash: String, val briefHash: String,
    val needsRevalidation: Boolean = false) {
    fun encode() = canonicalJson(json("type" to "pack_montage", "schema" to 1, "profileId" to profileId,
        "payloadHash" to payloadHash, "briefHash" to briefHash, "needsRevalidation" to needsRevalidation))
    fun validate(id: Long, payload: String, brief: String, hashes: List<String>) {
        require(profileId == id && id > 0); dnaHash(payloadHash); dnaHash(briefHash)
        require(payloadHash == sha256(payload.toByteArray()) && briefHash == sha256(brief.toByteArray()))
        require(PackMontage.parse(payload).pages.map { it.hash } == hashes)
    }
    companion object {
        fun parse(text: String): MontageEvidence {
            val r = strictObject(text, 2048, 3); r.exactKeys(setOf("type", "schema", "profileId", "payloadHash", "briefHash", "needsRevalidation"))
            require(r.stringValue("type") == "pack_montage" && r.longValue("schema") == 1L)
            return MontageEvidence(r.longValue("profileId"), r.stringValue("payloadHash"), r.stringValue("briefHash"),
                r.get("needsRevalidation") as? Boolean ?: error("Invalid revalidation flag"))
        }
    }
}

internal fun rekeyStyleEvidence(text: String, id: Long? = null): String = if (strictObject(text, 16384, 6).optString("type") == "pack_montage")
    MontageEvidence.parse(text).let { it.copy(profileId = id ?: it.profileId, needsRevalidation = true).encode() }
else ReviewedAnalysisEvidence.parse(text).let { if (id != null) it.rekey(id).encode() else it.copy(needsRevalidation = true).encode() }

/** Prefer filled, moderately wide grids; partial final rows are centered, never drawn as placeholders. */
internal fun montageLayout(count: Int): Pair<Int, Int> {
    require(count in 1..MONTAGE_PAGE_CAPACITY)
    return (1..5).map { it to (count + it - 1) / it }.filter { it.second <= 3 }
        .minBy { (c, r) -> 2.0 * (c * r - count) / count + abs(ln(c.toDouble() / r / 1.5)) }
}

internal suspend fun renderMontages(samples: List<SampledReference>): List<ByteArray> = withContext(Dispatchers.Default) {
    require(samples.size in 1..MONTAGE_MAX_MEMBERS && samples.map { it.hash }.distinct().size == samples.size)
    val pageCount = (samples.size + MONTAGE_PAGE_CAPACITY - 1) / MONTAGE_PAGE_CAPACITY
    val sizes = List(pageCount) { samples.size / pageCount + if (it < samples.size % pageCount) 1 else 0 }
    var position = 0
    sizes.map { size ->
        currentCoroutineContext().ensureActive()
        val (columns, rows) = montageLayout(size)
        val bitmap = Bitmap.createBitmap(columns * MONTAGE_CELL, rows * MONTAGE_CELL, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap); val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            repeat(size) { at ->
                currentCoroutineContext().ensureActive()
                val sample = samples[position++]; require(sha256(sample.png) == sample.hash); validateAnalysisPng(sample.png)
                val icon = checkNotNull(BitmapFactory.decodeByteArray(sample.png, 0, sample.png.size))
                try {
                    val row = at / columns; val inRow = minOf(columns, size - row * columns)
                    val x = (columns - inRow) * MONTAGE_CELL / 2 + (at % columns) * MONTAGE_CELL
                    val scale = minOf(1f, 256f / maxOf(icon.width, icon.height))
                    val w = icon.width * scale; val h = icon.height * scale
                    canvas.drawBitmap(icon, null, RectF(x + (MONTAGE_CELL - w) / 2, row * MONTAGE_CELL + (MONTAGE_CELL - h) / 2,
                        x + (MONTAGE_CELL + w) / 2, row * MONTAGE_CELL + (MONTAGE_CELL + h) / 2), paint)
                } finally { icon.recycle() }
            }
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray().also { inspectPng(it) }
            }
        } finally { bitmap.recycle() }
    }
}
