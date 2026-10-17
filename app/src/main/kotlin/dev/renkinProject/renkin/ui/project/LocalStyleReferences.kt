package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.data.project.*
import java.io.ByteArrayInputStream
import org.json.JSONObject

/** Draft provenance only, never a second reference store or permission to submit a request. */
internal data class LocalReferenceOrigin(val profileId: Long, val source: String,
    val setupStamp: String, val referenceStamp: String, val drawables: Map<String, String>) {
    fun encode(): String = JSONObject().put("id", profileId).put("source", source)
        .put("setup", setupStamp).put("references", referenceStamp)
        .put("drawables", JSONObject(drawables)).toString()
    companion object {
        fun parse(value: String): LocalReferenceOrigin = JSONObject(value).let { json ->
            val names = json.getJSONObject("drawables")
            LocalReferenceOrigin(json.getLong("id"), json.getString("source"), json.getString("setup"),
                json.getString("references"), names.keys().asSequence().associateWith { names.getString(it) })
        }
    }
    fun merge(other: LocalReferenceOrigin): LocalReferenceOrigin {
        check(copy(drawables = emptyMap()) == other.copy(drawables = emptyMap()))
        return copy(drawables = drawables + other.drawables)
    }
}

internal data class LocalReferenceCandidates(val origin: LocalReferenceOrigin, val samples: ReferenceSamples)

/** Uses the existing owner lock, real pack renderer, deduplicating sampler and private assets.
 * There is deliberately no provider, credential reader, journal or request coordinator here. */
internal class LocalStyleReferences(private val repository: ProjectAiRepository,
    private val sampler: ReferenceSampler, private val owner: AnalysisSessionOwner) {
    suspend fun sample(id: Long, source: String, stamp: String): LocalReferenceCandidates = owner.owned(id) {
        checkSetup(source, stamp)
        val samples = sampler.sample(owner.reader(source, true), candidateLimit = 120, sampleLimit = 12)
        check(samples.sourcePackage == source && samples.samples.isNotEmpty())
        checkSetup(source, stamp)
        LocalReferenceCandidates(LocalReferenceOrigin(id, source, stamp, checkNotNull(samples.sourceStamp),
            samples.samples.associate { it.hash to it.drawable }), samples)
    }

    suspend fun select(id: Long, candidates: LocalReferenceCandidates, hash: String): ReferenceAsset = owner.owned(id) {
        val origin = candidates.origin
        check(origin.profileId == id)
        checkSetup(origin.source, origin.setupStamp)
        val sample = candidates.samples.samples.single { it.hash == hash }
        check(sha256(sample.png) == hash && origin.drawables[hash] == sample.drawable)
        checkReferences(origin, listOf(hash))
        repository.importReference(id, ByteArrayInputStream(sample.png))
    }

    suspend fun save(id: Long, expected: Long?, draft: ProjectStyleDraft, mode: ProjectSetupMode,
        source: String?, stamp: String?): SavedProjectStyle = owner.owned(id) {
        if (mode == ProjectSetupMode.EXTEND) checkSetup(checkNotNull(source), checkNotNull(stamp))
        draft.localReferenceOrigin?.let { origin ->
            check(mode == ProjectSetupMode.EXTEND && origin.profileId == id && origin.source == source && origin.setupStamp == stamp)
            checkReferences(origin, draft.hashes.filter { it in origin.drawables })
        }
        repository.saveStyle(id, expected, draft.specification, draft.instruction, draft.hashes, mode.name, source, stamp)
    }

    private suspend fun checkSetup(source: String, stamp: String) {
        val current = owner.reader(source, false)
        check(current.isCurrent() && current.index.state == dev.renkinProject.renkin.apk.CoverageIndexState.READY &&
            current.index.stamp.versionCode != null && current.index.stamp.packageName == source && projectSourceStamp(current.index) == stamp)
    }

    private suspend fun checkReferences(origin: LocalReferenceOrigin, hashes: List<String>) {
        val current = owner.reader(origin.source, true)
        check(current.isCurrent() && current.index.stamp.packageName == origin.source &&
            projectSourceStamp(current.index) == origin.referenceStamp)
        for (hash in hashes) {
            val name = origin.drawables.getValue(hash)
            check(name in current.index.mappings.values)
            val rendered = current.inspectReference(name) as? ReferenceRead.Rendered
            check(rendered != null && sha256(rendered.png) == hash)
        }
        check(current.isCurrent())
    }
}
