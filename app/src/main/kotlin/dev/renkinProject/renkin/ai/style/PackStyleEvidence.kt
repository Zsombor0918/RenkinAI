package dev.renkinProject.renkin.ai.style

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal fun colorHex(color: Int) = "#%08X".format(Locale.ROOT, color)
private fun rgb(color: Int, shift: Int) = (color ushr shift) and 255

/** W3C CSS Color 4's sRGB transfer and Oklab transform. Distances are heuristics, not confidence. */
internal fun oklab(color: Int): DoubleArray {
    fun linear(shift: Int): Double = (rgb(color, shift) / 255.0).let { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }
    val r = linear(16); val g = linear(8); val b = linear(0)
    val l = Math.cbrt(.4122214708 * r + .5363325363 * g + .0514459929 * b)
    val m = Math.cbrt(.2119034982 * r + .6806995451 * g + .1073969566 * b)
    val s = Math.cbrt(.0883024619 * r + .2817188376 * g + .6299787005 * b)
    return doubleArrayOf(.2104542553 * l + .7936177850 * m - .0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + .4505937099 * s,
        .0259040371 * l + .7827717662 * m - .8086757660 * s)
}
internal fun colorDistance(a: Int, b: Int): Double {
    val one = oklab(a); val two = oklab(b)
    return sqrt(one.indices.sumOf { (one[it] - two[it]).pow(2) })
}
private class ColorCluster(val seed: Int) {
    var weight = 0.0; var r = 0.0; var g = 0.0; var b = 0.0
    fun add(color: Int, w: Double) { weight += w; r += rgb(color, 16) * w; g += rgb(color, 8) * w; b += rgb(color, 0) * w }
    fun color() = (0xff shl 24) or ((r / weight).roundToInt() shl 16) or ((g / weight).roundToInt() shl 8) or (b / weight).roundToInt()
}
/** At most 4096 sampled pixels; 4-bit buckets retain actual weighted RGB, never bin centers.
 * Greedy Oklab merging has at most 24 seeds, deterministic weight/color ties, six returned swatches. */
internal fun measuredColors(pixels: List<Pair<Int, Double>>): List<Pair<Int, Double>> {
    require(pixels.size <= 4096)
    val buckets = linkedMapOf<Int, ColorCluster>()
    for ((color, weight) in pixels) {
        val key = (rgb(color, 16) / 16) * 256 + (rgb(color, 8) / 16) * 16 + rgb(color, 0) / 16
        buckets.getOrPut(key) { ColorCluster(color) }.add(color, weight)
    }
    val clusters = mutableListOf<ColorCluster>()
    for (bucket in buckets.values.sortedWith(compareByDescending<ColorCluster> { it.weight }.thenBy { it.seed })) {
        val color = bucket.color()
        val nearest = clusters.minByOrNull { colorDistance(it.seed, color) }
        when {
            nearest != null && colorDistance(nearest.seed, color) <= .035 -> nearest.add(color, bucket.weight)
            clusters.size < 24 -> clusters += ColorCluster(color).also { it.add(color, bucket.weight) }
            else -> Unit // Fine gradients are a bounded tendency; discarded mass is not renormalized.
        }
    }
    val total = buckets.values.sumOf { it.weight }
    return clusters.sortedWith(compareByDescending<ColorCluster> { it.weight }.thenBy { it.seed }).take(6)
        .map { it.color() to it.weight / total }
}

internal enum class PackColorPolicy { FIXED_PALETTE, ADAPT_BRAND_COLORS, MONOCHROME, DUOTONE, MIXED, UNKNOWN }
internal data class MeasuredPackSwatch(val argb: String, val role: String, val icons: Int, val sharePercent: Int) {
    fun json() = json("argb" to argb, "role" to role, "icons" to icons, "sharePercent" to sharePercent)
}
/** Optional local-only evidence v1 inside a backward-readable sample report. No Room/profile schema change. */
internal data class PackStyleEvidence(val artworks: Int, val dominantFamily: Int, val families: Int,
    val colorPolicy: PackColorPolicy, val swatches: List<MeasuredPackSwatch>, val metrics: String) {
    fun validate() {
        require(artworks in 1..300 && dominantFamily in 1..artworks && families in 1..artworks)
        require(swatches.size <= 6 && swatches.map { it.argb }.distinct().size == swatches.size)
        swatches.forEach { require(it.argb.matches(Regex("#FF[A-F0-9]{6}")) && it.role in setOf("neutral", "dominant", "secondary", "accent") &&
            it.icons in 1..artworks && it.sharePercent in 0..100) }
        cleanText(metrics, 1600)
    }
    fun encode(): JSONObject { validate(); return json("schema" to 1, "artworks" to artworks, "dominantFamily" to dominantFamily,
        "families" to families, "colorPolicy" to colorPolicy.name, "swatches" to JSONArray(swatches.map { it.json() }), "metrics" to metrics) }
    companion object {
        fun parse(r: JSONObject): PackStyleEvidence {
            r.exactKeys(setOf("schema", "artworks", "dominantFamily", "families", "colorPolicy", "swatches", "metrics")); require(r.longValue("schema") == 1L)
            fun integer(root: JSONObject, key: String) = root.longValue(key).also { require(it in 0..300) }.toInt()
            val a = r.getJSONArray("swatches"); require(a.length() <= 6)
            return PackStyleEvidence(integer(r, "artworks"), integer(r, "dominantFamily"), integer(r, "families"),
                PackColorPolicy.valueOf(r.stringValue("colorPolicy")), (0 until a.length()).map { i -> a.getJSONObject(i).let {
                    it.exactKeys(setOf("argb", "role", "icons", "sharePercent"))
                    MeasuredPackSwatch(it.stringValue("argb"), it.stringValue("role"), integer(it, "icons"), integer(it, "sharePercent"))
                } }, r.stringValue("metrics")).also { it.validate() }
        }
    }
}

/** Density medoids form geometry families; hue alone never splits a multicolor pack. */
internal fun geometryFamilies(input: List<SampledReference>): List<List<SampledReference>> {
    val remaining = input.sortedWith(compareBy<SampledReference> { it.component }.thenBy { it.hash }).toMutableList()
    val groups = mutableListOf<List<SampledReference>>()
    while (remaining.isNotEmpty()) {
        val center = remaining.maxBy { a -> remaining.count { a.features.geometryDistance(it.features) <= .12 } }
        val family = remaining.filter { center.features.geometryDistance(it.features) <= .12 }
        groups += family; remaining.removeAll(family.toSet())
    }
    return groups.sortedByDescending { it.size }
}
internal fun packStyleEvidence(input: List<SampledReference>): PackStyleEvidence? {
    if (input.isEmpty()) return null
    val families = geometryFamilies(input)
    val colors = input.flatMap { s -> s.features.palette.mapIndexed { i, color ->
        android.graphics.Color.parseColor(color) to (s.features.paletteWeights.getOrNull(i) ?: (1.0 / s.features.palette.size)) } }
    // Aggregate normalized icon swatches, capped to 1800; each icon contributes equally.
    val palette = measuredColors(colors)
    fun matching(s: SampledReference, color: Int) = s.features.palette.mapIndexed { i, c ->
        if (colorDistance(android.graphics.Color.parseColor(c), color) <= .05) s.features.paletteWeights.getOrNull(i) ?: (1.0 / s.features.palette.size) else 0.0 }.sum()
    val recurring = palette.filter { (color, _) -> input.count { matching(it, color) >= .08 } >= input.size * .7 }
    // A common neutral tile must not erase a small but meaningful varying brand accent.
    val variableAccents = input.count { sample -> sample.features.palette.withIndex().any { (i, hex) ->
        val color = android.graphics.Color.parseColor(hex); val lab = oklab(color)
        val share = sample.features.paletteWeights.getOrNull(i) ?: (1.0 / sample.features.palette.size)
        share >= .05 && sqrt(lab[1].pow(2) + lab[2].pow(2)) > .025 && recurring.none { colorDistance(color, it.first) <= .05 }
    } } >= input.size * .5
    val fixed = input.size >= 4 && !variableAccents &&
        input.count { sample -> recurring.sumOf { matching(sample, it.first) } >= .88 } >= input.size * .8
    val coherent = families.first().size >= input.size * .8
    val policy = when {
        input.size < 4 -> PackColorPolicy.UNKNOWN
        fixed && recurring.size == 1 -> PackColorPolicy.MONOCHROME
        fixed && recurring.size == 2 -> PackColorPolicy.DUOTONE
        fixed -> PackColorPolicy.FIXED_PALETTE
        coherent && (variableAccents || input.count { it.features.saturation > .15 } >= input.size * .7) -> PackColorPolicy.ADAPT_BRAND_COLORS
        families.first().size < input.size * .7 -> PackColorPolicy.MIXED
        else -> PackColorPolicy.UNKNOWN
    }
    val swatches = palette.mapNotNull { (color, share) ->
        val icons = input.count { matching(it, color) >= .02 }
        if (icons == 0) return@mapNotNull null
        val lab = oklab(color); val neutral = sqrt(lab[1].pow(2) + lab[2].pow(2)) < .025
        MeasuredPackSwatch(colorHex(color), if (neutral) "neutral" else if (share >= .25) "dominant" else if (share >= .1) "secondary" else "accent",
            icons, (share * 100).roundToInt())
    }
    fun spread(label: String, read: (ReferenceFeatures) -> Double): String {
        val values = input.map { (read(it.features) * 100).roundToInt() }.sorted()
        return "$label ${values.first()}/${values[values.size / 2]}/${values.last()}% min/median/max"
    }
    val metrics = listOf(spread("occupied area", { it.occupancy }), spread("width", { it.width }), spread("height", { it.height }),
        spread("minimum optical padding", { it.padding }), spread("brightness", { it.brightness }), spread("saturation", { it.saturation }),
        spread("luminance spread", { it.contrast }), spread("edge density heuristic", { it.edgeDensity }),
        spread("empty-center heuristic", { it.outlineEstimate }), spread("soft-alpha tendency", { it.softAlpha })).joinToString("; ") +
        "; exact stroke width, corners, gradient and shadow direction are unknown locally."
    return PackStyleEvidence(input.size, families.first().size, families.size, policy, swatches, metrics).also { it.validate() }
}
