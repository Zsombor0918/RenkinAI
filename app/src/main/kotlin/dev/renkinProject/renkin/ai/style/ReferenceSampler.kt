package dev.renkinProject.renkin.ai.style

import dev.renkinProject.renkin.apk.CoverageIndexState
import dev.renkinProject.renkin.apk.CoverageSourceReader
import dev.renkinProject.renkin.data.project.sha256
import dev.renkinProject.renkin.ui.project.projectSourceStamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

internal data class SampledReference(val component: String, val drawable: String,
    val hash: String, val png: ByteArray, val features: ReferenceFeatures)
internal data class ReferenceSampleReport(val examined: Int, val valid: Int, val duplicates: Int,
    val unsupported: Int, val broken: Int, val mixedStyleSuspected: Boolean, val lowSample: Boolean) {
    fun encode() = json("examined" to examined, "valid" to valid, "duplicates" to duplicates,
        "unsupported" to unsupported, "broken" to broken, "mixed" to mixedStyleSuspected, "lowSample" to lowSample)
    fun validate() { require(examined in 0..120 && listOf(valid, duplicates, unsupported, broken).all { it >= 0 } &&
        valid + duplicates + unsupported + broken == examined) }
    companion object {
        fun parse(r: org.json.JSONObject): ReferenceSampleReport {
            r.exactKeys(setOf("examined", "valid", "duplicates", "unsupported", "broken", "mixed", "lowSample"))
            fun count(key: String) = r.longValue(key).also { require(it in 0L..120L) }.toInt()
            return ReferenceSampleReport(count("examined"), count("valid"), count("duplicates"), count("unsupported"), count("broken"),
                r.get("mixed") as? Boolean ?: error("Invalid report."), r.get("lowSample") as? Boolean ?: error("Invalid report.")).also { it.validate() }
        }
    }
}
internal data class ReferenceSamples(val sourcePackage: String?, val sourceStamp: String?,
    val samples: List<SampledReference>, val report: ReferenceSampleReport,
    val algorithm: String = REFERENCE_ALGORITHM,
    val sourceIdentity: dev.renkinProject.renkin.apk.CoverageStamp? = null)

internal class StaleReferenceSource : IllegalStateException("Source changed during sampling; prepare fresh references.")

internal class ReferenceSampler {
    suspend fun sample(reader: CoverageSourceReader, candidateLimit: Int = 120, sampleLimit: Int = 12): ReferenceSamples =
        withContext(Dispatchers.Default) {
            require(candidateLimit in 1..120 && sampleLimit in 1..12)
            val index = reader.index
            check(index.state == CoverageIndexState.READY && index.stamp.versionCode != null) { "A verified source index is required." }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val sorted = index.mappings.toSortedMap().entries.toList()
            val candidates = if (sorted.size <= candidateLimit) sorted else (0 until candidateLimit).map {
                sorted[(it.toLong() * (sorted.size - 1) / (candidateLimit - 1).coerceAtLeast(1)).toInt()]
            }
            var duplicate = 0; var unsupported = 0; var broken = 0
            val valid = mutableListOf<SampledReference>()
            for ((component, drawable) in candidates) {
                currentCoroutineContext().ensureActive()
                if (!reader.isCurrent()) throw StaleReferenceSource()
                if (!reader.stableReferences || component in index.dynamicKeys) { unsupported++; continue }
                try {
                    if (drawable.isBlank() || !reader.canRender(drawable)) { broken++; continue }
                    val png = reader.referencePng(drawable)
                    if (png == null) { unsupported++; continue }
                    val features = referenceFeatures(png); val hash = sha256(png)
                    if (valid.any { it.hash == hash || it.features.near(features) }) duplicate++
                    else valid += SampledReference(component, drawable, hash, png, features)
                } catch (cancel: CancellationException) { throw cancel }
                catch (stale: StaleReferenceSource) { throw stale }
                catch (_: OutOfMemoryError) { broken++ }
                catch (_: Exception) { broken++ }
                if (!reader.isCurrent()) throw StaleReferenceSource()
                yield()
            }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val chosen = diverseReferences(valid, sampleLimit)
            val mixed = chosen.any { a -> chosen.any { b -> a.features.distance(b.features) > .34 } }
            ReferenceSamples(index.stamp.packageName, projectSourceStamp(index), chosen,
                ReferenceSampleReport(candidates.size, valid.size, duplicate, unsupported, broken, mixed, chosen.size < 8),
                sourceIdentity = index.stamp)
        }

    suspend fun manual(pngs: List<Pair<String, ByteArray>>): ReferenceSamples = withContext(Dispatchers.Default) {
        require(pngs.size <= 5)
        val all = pngs.map { (id, bytes) ->
            currentCoroutineContext().ensureActive()
            val png = thumbnailReference(bytes); SampledReference(id, "own reference", sha256(png), png, referenceFeatures(png))
        }
        val unique = mutableListOf<SampledReference>()
        for (sample in all) if (unique.none { it.hash == sample.hash || it.features.near(sample.features) }) unique += sample
        ReferenceSamples(null, null, unique, ReferenceSampleReport(all.size, unique.size, all.size - unique.size,
            0, 0, unique.any { a -> unique.any { a.features.distance(it.features) > .34 } }, unique.size < 8))
    }
}

/** Medoid then farthest-first; stable source/hash ties, no random seed or first-row bias. */
internal fun diverseReferences(input: List<SampledReference>, limit: Int): List<SampledReference> {
    require(limit in 1..12)
    val remaining = input.sortedWith(compareBy<SampledReference> { it.component }.thenBy { it.hash }).toMutableList()
    if (remaining.isEmpty()) return emptyList()
    val center = remaining.minBy { a -> remaining.sumOf { a.features.distance(it.features) } }
    val chosen = mutableListOf(center); remaining.remove(center)
    while (chosen.size < limit && remaining.isNotEmpty()) {
        val next = remaining.maxBy { a -> chosen.minOf { a.features.distance(it.features) } }
        chosen += next; remaining.remove(next)
    }
    return chosen
}
