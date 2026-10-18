package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream

internal data class MontageDraft(val profileId: Long, val montage: PackMontage,
    val images: List<ByteArray>, val candidates: List<SampledReference>)

/** One local artifact path; the same immutable sheets are later analysis and generation inputs. */
internal class LocalPackMontages(private val repository: ProjectAiRepository,
    private val sampler: ReferenceSampler, private val owner: AnalysisSessionOwner) {
    private suspend fun source(source: String, stamp: String): CoverageSourceReader {
        val setup = owner.reader(source, false)
        check(setup.isCurrent() && setup.index.state == CoverageIndexState.READY && setup.index.stamp.versionCode != null &&
            setup.index.stamp.packageName == source && projectSourceStamp(setup.index) == stamp)
        return owner.reader(source, true).also {
            check(it.isCurrent() && it.stableReferences && it.index.state == CoverageIndexState.READY && it.index.stamp.packageName == source)
        }
    }

    suspend fun prepare(id: Long, source: String, stamp: String): MontageDraft = owner.owned(id) {
        val reader = source(source, stamp)
        val batch = sampler.montageCandidates(reader)
        check(batch.samples.isNotEmpty())
        val selected = diverseReferences(batch.samples, minOf(30, batch.samples.size))
        build(id, source, stamp, checkNotNull(batch.sourceStamp), selected, batch.samples).also { check(reader.isCurrent()) }
    }

    private suspend fun build(id: Long, source: String, stamp: String, referenceStamp: String,
        selected: List<SampledReference>, candidates: List<SampledReference>): MontageDraft {
        val images = renderMontages(selected)
        var at = 0
        val count = images.size
        val pages = images.mapIndexed { index, bytes ->
            val size = selected.size / count + if (index < selected.size % count) 1 else 0
            val (w, h) = inspectPng(bytes)
            at += size; MontagePage(sha256(bytes), w, h, size)
        }
        check(at == selected.size)
        val montage = PackMontage(source, stamp, referenceStamp,
            selected.map { MontageMember(it.component, it.drawable, it.hash) }, pages)
        montage.encode()
        return MontageDraft(id, montage, images, candidates)
    }

    /** Reopening never samples or submits. Existing PNG bytes remain the visual source of truth. */
    suspend fun reopen(id: Long): MontageDraft = owner.owned(id) {
        val saved = repository.load(id)
        val montage = checkNotNull(PackMontage.optional(saved.revision?.analysisProfileJson))
        MontageDraft(id, montage, montage.pages.map { repository.referenceBytes(id, it.hash) }, emptyList())
    }

    /** Only explicit Browse regenerates candidate previews; current members/order are retained. */
    suspend fun browse(id: Long, draft: MontageDraft): MontageDraft = owner.owned(id) {
        check(draft.profileId == id)
        val m = draft.montage; val reader = source(m.source, m.setupStamp)
        check(projectSourceStamp(reader.index) == m.referenceStamp)
        val selected = readMembers(reader, m.members)
        val candidates = (selected + sampler.montageCandidates(reader).samples).distinctBy { it.hash }
        check(reader.isCurrent())
        draft.copy(candidates = candidates)
    }

    suspend fun recompose(id: Long, draft: MontageDraft, orderedHashes: List<String>): MontageDraft = owner.owned(id) {
        check(id == draft.profileId)
        require(orderedHashes.size in 1..MONTAGE_MAX_MEMBERS && orderedHashes.distinct().size == orderedHashes.size)
        val m = draft.montage; val reader = source(m.source, m.setupStamp)
        check(projectSourceStamp(reader.index) == m.referenceStamp)
        val chosen = orderedHashes.map { hash -> draft.candidates.single { it.hash == hash } }
        val selected = readMembers(reader, chosen.map { MontageMember(it.component, it.drawable, it.hash) })
        build(id, m.source, m.setupStamp, m.referenceStamp, selected, draft.candidates).let { rebuilt ->
            // A reorder is a new visual input too; old paid results stay in revision history.
            if (rebuilt.montage.fingerprint == m.fingerprint) rebuilt.copy(montage = rebuilt.montage.copy(result = m.result)) else rebuilt
        }
    }

    suspend fun validate(id: Long, draft: MontageDraft) = owner.owned(id) { validateOwned(id, draft) }
    private suspend fun validateOwned(id: Long, draft: MontageDraft) {
        check(id == draft.profileId)
        val m = draft.montage; m.encode()
        val reader = source(m.source, m.setupStamp)
        check(projectSourceStamp(reader.index) == m.referenceStamp)
        val selected = readMembers(reader, m.members)
        val expected = renderMontages(selected)
        check(expected.size == draft.images.size && expected.map(::sha256) == m.pages.map { it.hash })
        draft.images.forEachIndexed { index, bytes ->
            check(sha256(bytes) == m.pages[index].hash && inspectPng(bytes) == (m.pages[index].width to m.pages[index].height))
        }
        check(reader.isCurrent())
    }

    private suspend fun readMembers(reader: CoverageSourceReader, members: List<MontageMember>): List<SampledReference> = members.map { m ->
        currentCoroutineContext().ensureActive()
        check(reader.isCurrent() && m.component !in reader.index.dynamicKeys && reader.index.mappings[m.component] == m.drawable)
        val png = (reader.inspectMontageReference(m.drawable) as? ReferenceRead.Rendered)?.png ?: error("Source icon unavailable")
        check(sha256(png) == m.hash)
        SampledReference(m.component, m.drawable, m.hash, png, referenceFeatures(png))
    }

    suspend fun save(id: Long, expected: Long?, draft: MontageDraft, brief: String = MONTAGE_STARTING_BRIEF,
        instruction: String = ""): SavedProjectStyle = owner.owned(id) {
        validateOwned(id, draft)
        repository.importReferences(id, draft.images.map { bytes -> { ByteArrayInputStream(bytes) } })
        // No new store, key, journal or paid permission. Existing optimistic revision check applies.
        repository.saveMontageStyle(id, expected, draft.montage, brief, instruction)
    }
}
