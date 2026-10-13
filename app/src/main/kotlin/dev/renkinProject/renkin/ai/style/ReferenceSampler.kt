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
    val unsupported: Int, val broken: Int, val mixedStyleSuspected: Boolean, val lowSample: Boolean,
    val evidence: PackStyleEvidence? = null) {
    fun encode() = json("examined" to examined, "valid" to valid, "duplicates" to duplicates,
        "unsupported" to unsupported, "broken" to broken, "mixed" to mixedStyleSuspected, "lowSample" to lowSample)
        .also { if (evidence != null) it.put("evidence", evidence.encode()) }
    fun validate() { require(examined in 0..300 && listOf(valid, duplicates, unsupported, broken).all { it >= 0 } &&
        valid + duplicates + unsupported + broken == examined); evidence?.let { it.validate(); require(it.artworks == valid) } }
    companion object {
        fun parse(r: org.json.JSONObject): ReferenceSampleReport {
            r.exactKeys(setOf("examined", "valid", "duplicates", "unsupported", "broken", "mixed", "lowSample") +
                if (r.has("evidence")) setOf("evidence") else emptySet())
            fun count(key: String) = r.longValue(key).also { require(it in 0L..300L) }.toInt()
            return ReferenceSampleReport(count("examined"), count("valid"), count("duplicates"), count("unsupported"), count("broken"),
                r.get("mixed") as? Boolean ?: error("Invalid report."), r.get("lowSample") as? Boolean ?: error("Invalid report."),
                if (r.has("evidence")) PackStyleEvidence.parse(r.getJSONObject("evidence")) else null).also { it.validate() }
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
            require(candidateLimit in 1..300 && sampleLimit in 1..12)
            val index = reader.index
            check(index.state == CoverageIndexState.READY && index.stamp.versionCode != null) { "A verified source index is required." }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val sorted = index.mappings.toSortedMap().entries.toList()
            val candidates = if (sorted.size <= candidateLimit) sorted else (0 until candidateLimit).map {
                sorted[(it.toLong() * (sorted.size - 1) / (candidateLimit - 1).coerceAtLeast(1)).toInt()]
            }
            var duplicate = 0; var unsupported = 0; var broken = 0
            val valid = mutableListOf<SampledReference>()
            var retainedBytes = 0L
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
                    else {
                        // Keep a bounded sketch, not 300 maximum-size PNGs. Selected evicted bytes
                        // are rendered once more and must match the original hash/source stamp.
                        val retain = retainedBytes + png.size <= 8L * 1024 * 1024
                        if (retain) retainedBytes += png.size
                        valid += SampledReference(component, drawable, hash, if (retain) png else ByteArray(0), features)
                    }
                } catch (cancel: CancellationException) { throw cancel }
                catch (stale: StaleReferenceSource) { throw stale }
                catch (_: OutOfMemoryError) { broken++ }
                catch (_: Exception) { broken++ }
                if (!reader.isCurrent()) throw StaleReferenceSource()
                yield()
            }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val chosen = mutableListOf<SampledReference>(); var uploadBytes = 0L
            for (sample in diverseReferences(valid, sampleLimit)) {
                currentCoroutineContext().ensureActive()
                if (!reader.isCurrent()) throw StaleReferenceSource()
                val png = if (sample.png.isNotEmpty()) sample.png else checkNotNull(reader.referencePng(sample.drawable))
                validateAnalysisPng(png)
                if (sha256(png) != sample.hash || !reader.isCurrent()) throw StaleReferenceSource()
                if (uploadBytes + png.size <= 4L * 1024 * 1024) { chosen += sample.copy(png = png); uploadBytes += png.size }
            }
            val evidence = packStyleEvidence(valid)
            val mixed = evidence?.colorPolicy == PackColorPolicy.MIXED
            ReferenceSamples(index.stamp.packageName, projectSourceStamp(index), chosen,
                ReferenceSampleReport(candidates.size, valid.size, duplicate, unsupported, broken, mixed, chosen.size < 8, evidence),
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
        val evidence = packStyleEvidence(unique)
        ReferenceSamples(null, null, unique, ReferenceSampleReport(all.size, unique.size, all.size - unique.size,
            0, 0, evidence?.colorPolicy == PackColorPolicy.MIXED, unique.size < 8, evidence))
    }
}

/** Proportional geometry families, then medoids/diversity within each family. A minority
 * check remains visible without letting a 10% family dominate twelve uploaded samples. */
internal fun diverseReferences(input: List<SampledReference>, limit: Int): List<SampledReference> {
    require(limit in 1..12)
    val families = geometryFamilies(input)
    if (families.isEmpty()) return emptyList()
    val size = minOf(limit, input.size)
    val allocations = IntArray(families.size)
    // Reserve one minority check only when there is room; the rest follow population share.
    if (families.size > 1 && size > 1) allocations[1] = 1
    while (allocations.sum() < size) {
        val next = families.indices.filter { allocations[it] < families[it].size }
            .maxBy { families[it].size.toDouble() / (allocations[it] + 1) }
        allocations[next]++
    }
    return families.flatMapIndexed { i, family -> if (allocations[i] == 0) emptyList() else familyMedoids(family, allocations[i]) }
}
private fun familyMedoids(input: List<SampledReference>, limit: Int): List<SampledReference> {
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
