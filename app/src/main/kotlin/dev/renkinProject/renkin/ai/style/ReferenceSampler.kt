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
    val evidence: PackStyleEvidence? = null, val reasonCounts: Map<ReferenceFailure, Int>? = null,
    val candidates: Int? = null) {
    fun encode() = json("examined" to examined, "valid" to valid, "duplicates" to duplicates,
        "unsupported" to unsupported, "broken" to broken, "mixed" to mixedStyleSuspected, "lowSample" to lowSample)
        .also { if (evidence != null) it.put("evidence", evidence.encode())
            if (reasonCounts != null) it.put("reasonCounts", json(*reasonCounts.map { it.key.name to it.value }.toTypedArray()))
            if (candidates != null) it.put("candidates", candidates) }
    fun validate() { require(examined in 0..600 && listOf(valid, duplicates, unsupported, broken).all { it >= 0 } &&
        valid + duplicates + unsupported + broken == examined); evidence?.let { it.validate(); require(it.artworks == valid) }
        reasonCounts?.let { require(it.values.all { n -> n > 0 } && it.values.sum() == unsupported + broken) }
        candidates?.let { require(it in examined..600) } }
    companion object {
        fun parse(r: org.json.JSONObject): ReferenceSampleReport {
            r.exactKeys(setOf("examined", "valid", "duplicates", "unsupported", "broken", "mixed", "lowSample") +
                (if (r.has("evidence")) setOf("evidence") else emptySet()) +
                (if (r.has("reasonCounts")) setOf("reasonCounts") else emptySet()) + (if (r.has("candidates")) setOf("candidates") else emptySet()))
            fun count(key: String) = r.longValue(key).also { require(it in 0L..600L) }.toInt()
            return ReferenceSampleReport(count("examined"), count("valid"), count("duplicates"), count("unsupported"), count("broken"),
                r.get("mixed") as? Boolean ?: error("Invalid report."), r.get("lowSample") as? Boolean ?: error("Invalid report."),
                if (r.has("evidence")) PackStyleEvidence.parse(r.getJSONObject("evidence")) else null,
                if (r.has("reasonCounts")) r.getJSONObject("reasonCounts").let { reasons -> reasons.keys().asSequence().associate { key ->
                    ReferenceFailure.valueOf(key) to reasons.longValue(key).also { require(it in 1..600) }.toInt() } } else null,
                if (r.has("candidates")) count("candidates") else null).also { it.validate() }
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
        sampleInternal(reader, candidateLimit, sampleLimit.also { require(it in 1..24) }, false)

    suspend fun montageCandidates(reader: CoverageSourceReader): ReferenceSamples = sampleInternal(reader, 300, MONTAGE_MAX_MEMBERS, true)

    private suspend fun sampleInternal(reader: CoverageSourceReader, candidateLimit: Int, sampleLimit: Int,
        montage: Boolean): ReferenceSamples = withContext(Dispatchers.Default) {
            require(candidateLimit in 1..600 && sampleLimit in 1..MONTAGE_MAX_MEMBERS)
            val index = reader.index
            check(index.state == CoverageIndexState.READY && index.stamp.versionCode != null) { "A verified source index is required." }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val sorted = index.mappings.toSortedMap().entries.toList()
            val candidates = if (sorted.size <= candidateLimit) sorted else (0 until candidateLimit).map {
                sorted[(it.toLong() * (sorted.size - 1) / (candidateLimit - 1).coerceAtLeast(1)).toInt()]
            }
            var duplicate = 0; var unsupported = 0; var broken = 0; var examined = 0
            val reasons = mutableMapOf<ReferenceFailure, Int>()
            fun failed(reason: ReferenceFailure) {
                reasons[reason] = (reasons[reason] ?: 0) + 1
                if (reason in setOf(ReferenceFailure.UNSUPPORTED_DYNAMIC, ReferenceFailure.UNSUPPORTED_STATEFUL)) unsupported++ else broken++
            }
            val deadline = System.nanoTime() + 20_000_000_000L
            val valid = mutableListOf<SampledReference>()
            var retainedBytes = 0L
            for ((component, drawable) in candidates) {
                currentCoroutineContext().ensureActive()
                if (!reader.isCurrent()) throw StaleReferenceSource()
                if (System.nanoTime() >= deadline) break
                examined++
                if (!reader.stableReferences || component in index.dynamicKeys) { failed(ReferenceFailure.UNSUPPORTED_DYNAMIC); continue }
                try {
                    if (drawable.isBlank()) { failed(ReferenceFailure.MISSING_DRAWABLE); continue }
                    val read = if (montage) reader.inspectMontageReference(drawable) else reader.inspectReference(drawable)
                    if (read is ReferenceRead.Failed) { failed(read.reason); continue }
                    val png = (read as ReferenceRead.Rendered).png
                    validateReferencePixels(png)
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
                catch (empty: EmptyReference) { failed(empty.reason) }
                catch (_: OutOfMemoryError) { failed(ReferenceFailure.SOURCE_RESOURCE_FAILURE) }
                catch (_: Exception) { failed(ReferenceFailure.RENDER_EXCEPTION) }
                if (!reader.isCurrent()) throw StaleReferenceSource()
                yield()
            }
            if (!reader.isCurrent()) throw StaleReferenceSource()
            val chosen = mutableListOf<SampledReference>(); var uploadBytes = 0L
            for (sample in diverseReferences(valid, sampleLimit)) {
                currentCoroutineContext().ensureActive()
                if (!reader.isCurrent()) throw StaleReferenceSource()
                val png = if (sample.png.isNotEmpty()) sample.png else if (montage)
                    (reader.inspectMontageReference(sample.drawable) as? ReferenceRead.Rendered)?.png ?: error("Montage resource changed")
                    else checkNotNull(reader.referencePng(sample.drawable))
                validateAnalysisPng(png)
                if (sha256(png) != sample.hash || !reader.isCurrent()) throw StaleReferenceSource()
                if (uploadBytes + png.size <= (if (montage) 16L else if (sampleLimit <= 12) 4L else 8L) * 1024 * 1024) { chosen += sample.copy(png = png); uploadBytes += png.size }
            }
            val evidence = packStyleEvidence(valid)
            val mixed = evidence?.colorPolicy == PackColorPolicy.MIXED
            ReferenceSamples(index.stamp.packageName, projectSourceStamp(index), chosen,
                ReferenceSampleReport(examined, valid.size, duplicate, unsupported, broken, mixed, chosen.size < 8, evidence, reasons, candidates.size),
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
    require(limit in 1..MONTAGE_MAX_MEMBERS)
    val families = geometryFamilies(input)
    if (families.isEmpty()) return emptyList()
    val size = minOf(limit, input.size)
    val allocations = IntArray(families.size)
    // Reserve one minority check only when there is room; the rest follow population share.
    if (families.size > 1 && size > 1) {
        if (limit <= 12) allocations[1] = 1
        else families.indices.drop(1).take(2).forEach { allocations[it] = minOf(2,families[it].size) }
    }
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
