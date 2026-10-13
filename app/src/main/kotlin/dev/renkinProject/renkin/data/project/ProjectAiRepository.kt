package dev.renkinProject.renkin.data.project

import androidx.room.withTransaction
import dev.renkinProject.renkin.data.RenkinPackDatabase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.WeakHashMap
import dev.renkinProject.renkin.ai.style.ReviewedAnalysisEvidence

data class SavedProjectStyle(val metadata: ProjectAiMetadata?, val revision: StyleRevision?, val references: List<ReferenceAsset>)

/** Shares the existing DB, never owns a profile session, provider, API key or network call. */
class ProjectAiRepository(val database: RenkinPackDatabase, val files: ProjectAssetStore) {
    private val dao = database.projectAiDao()
    private val storage = synchronized(locks) { locks.getOrPut(database) { Mutex() } }

    /** Files and synchronous legacy DAOs must never run on a UI caller's dispatcher.
     * Archive restore shares this lease so explicit GC cannot collect staged assets. */
    internal suspend fun <T> withAssetLease(action: suspend () -> T): T = withContext(Dispatchers.IO) {
        storage.withLock { action() }
    }

    suspend fun load(profileId: Long): SavedProjectStyle = withAssetLease {
        val result = database.withTransaction {
            check(database.profileDao().get(profileId) != null) { "Project no longer exists." }
            val meta = dao.metadata(profileId)
            val revision = dao.history(profileId).find { it.revision == meta?.activeRevision }
            val byHash = dao.assets(profileId).associateBy { it.hash }
            val references = dao.references(profileId).filter { it.revision == meta?.activeRevision }
                .map { checkNotNull(byHash[it.assetHash]) }
            SavedProjectStyle(meta, revision, references)
        }
        result.revision?.let { validateStyle(it, result.references.map { a -> a.hash }) }
        result.references.forEach { files.read(it) }
        result
    }

    suspend fun history(id: Long): List<StyleRevision> = dao.history(id)
    suspend fun metadata(id: Long): ProjectAiMetadata? = dao.metadata(id)
    suspend fun referenceBytes(id: Long, hash: String): ByteArray = withAssetLease {
        files.read(checkNotNull(dao.assets(id).find { it.hash == hash }) { "Project reference is missing." })
    }
    suspend fun references(id: Long, revision: Long): List<ReferenceAsset> {
        val assets = dao.assets(id).associateBy { it.hash }
        return dao.references(id).filter { it.revision == revision }.map { checkNotNull(assets[it.assetHash]) }
    }

    suspend fun importReference(id: Long, input: InputStream, mime: String = "image/png"): ReferenceAsset = input.use {
        importReferences(id, listOf({ input }), mime).single()
    }

    /** Explicit global-copy imports succeed as a group. Failed/cancelled copies drop only
     * rows created by this operation and collect unrecorded files under the same storage lease. */
    internal suspend fun importReferences(id: Long, sources: List<() -> InputStream>, mime: String = "image/png"): List<ReferenceAsset> {
        require(sources.size <= 5)
        val imported = mutableListOf<ReferenceAsset>()
        var previous = emptySet<String>()
        try {
            return withAssetLease {
                check(database.profileDao().get(id) != null) { "Project no longer exists." }
                previous = dao.assets(id).map { it.hash }.toSet()
                for (source in sources) source().use { imported += files.copy(id, it, mime) }
                currentCoroutineContext().ensureActive()
                database.withTransaction {
                    check(database.profileDao().get(id) != null)
                    dao.insertAssets(imported)
                }
                imported.toList()
            }
        } catch (failure: Throwable) {
            try {
                withContext(NonCancellable) { withAssetLease {
                    val newHashes = imported.map { it.hash }.filter { it !in previous }
                    database.withTransaction { dao.discardUnlinked(id, newHashes) }
                    // Import failure is cleanup of this operation, not permission for global GC.
                    files.collectUnreferenced(dao.allAssets(), newHashes.map { ProjectAssetStore.path(id, it) }.toSet())
                } }
            } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
            throw failure
        }
    }

    /** The caller holds the existing session mutex and checks active ID before entering.
     * The DB transaction supplies optimistic revision protection for independent writers. */
    suspend fun saveStyle(id: Long, expectedRevision: Long?, specification: String,
        userInstruction: String, orderedHashes: List<String>, mode: String? = null,
        source: String? = null, sourceStamp: String? = null): SavedProjectStyle =
        saveRevision(id, expectedRevision, specification, userInstruction, orderedHashes, mode, source, sourceStamp)

    internal suspend fun saveAnalyzedStyle(id: Long, expectedRevision: Long?, expectedVersion: Long,
        specification: String, userInstruction: String, hashes: List<String>, profileJson: String,
        evidence: ReviewedAnalysisEvidence): SavedProjectStyle {
        require(!evidence.needsRevalidation && evidence.context.profileId == id)
        return saveRevision(id, expectedRevision, specification, userInstruction, hashes, evidence.context.mode,
            evidence.context.sourcePackage, evidence.context.setupStamp, profileJson, evidence.encode(), expectedVersion,
            if (evidence.userEdited) "USER" else "AUTO_ANALYSIS")
    }

    private suspend fun saveRevision(id: Long, expectedRevision: Long?, specification: String,
        userInstruction: String, orderedHashes: List<String>, mode: String?, source: String?, sourceStamp: String?,
        analysisProfileJson: String? = null, analysisEvidenceJson: String? = null,
        expectedVersion: Long? = null, origin: String = "USER"): SavedProjectStyle = withAssetLease {
        require(mode == null || mode in setOf("CREATE", "EXTEND"))
        require(mode != "EXTEND" || !source.isNullOrBlank())
        val candidate = StyleRevision(id, 1, specification = specification.trim(), userInstruction = userInstruction.trim(),
            origin = origin, analysisProfileJson = analysisProfileJson, analysisEvidenceJson = analysisEvidenceJson)
        validateStyle(candidate, orderedHashes)
        val assets = dao.assets(id).associateBy { it.hash }
        val references = orderedHashes.map { checkNotNull(assets[it]) { "Reference image is not owned by this project." } }
        references.forEach { files.read(it) }
        currentCoroutineContext().ensureActive()
        database.withTransaction {
            check(database.profileDao().get(id) != null) { "Project no longer exists." }
            val meta = dao.metadata(id) ?: ProjectAiMetadata(id)
            val history = dao.history(id)
            val current = history.find { it.revision == meta.activeRevision }
            val oldRefs = dao.references(id).filter { it.revision == meta.activeRevision }.map { it.assetHash }
            val contextChanged = mode != null && (mode != meta.mode || source != meta.sourcePackage || sourceStamp != meta.sourceStamp)
            val sameText = current?.specification == candidate.specification && current.userInstruction == candidate.userInstruction && oldRefs == orderedHashes
            val sameAnalysis = analysisProfileJson == null || (current?.origin == origin &&
                current.analysisProfileJson == analysisProfileJson && current.analysisEvidenceJson == analysisEvidenceJson)
            if (!contextChanged && sameText && sameAnalysis) {
                return@withTransaction SavedProjectStyle(meta, current, references)
            }
            check(expectedVersion == null || meta.version == expectedVersion) { "Project preparation changed. Review it again." }
            check(meta.activeRevision == expectedRevision) { "The saved style changed. Reopen it before saving." }
            val sameStyle = sameText && sameAnalysis
            val revision = if (sameStyle) checkNotNull(current) else candidate.copy(revision = (history.maxOfOrNull { it.revision } ?: 0) + 1)
            if (!sameStyle) {
                dao.insertRevisions(listOf(revision))
                dao.insertReferences(orderedHashes.mapIndexed { position, hash -> StyleReference(id, revision.revision, position, hash) })
            }
            val updated = meta.copy(mode = mode ?: meta.mode, sourcePackage = if (mode != null) source else meta.sourcePackage,
                sourceStamp = if (mode != null) sourceStamp else meta.sourceStamp,
                selectionJson = if (contextChanged) "" else meta.selectionJson,
                activeRevision = revision.revision, version = meta.version + 1, updatedAt = System.currentTimeMillis())
            dao.putMetadata(updated)
            SavedProjectStyle(updated, revision, references)
        }
    }

    suspend fun saveSelection(id: Long, expectedVersion: Long, selection: ProjectSelection): ProjectAiMetadata = withAssetLease {
        selection.validate()
        database.withTransaction {
            check(database.profileDao().get(id) != null) { "Project no longer exists." }
            val old = dao.metadata(id) ?: ProjectAiMetadata(id)
            check(old.version == expectedVersion) { "Project preparation changed. Reopen it before saving." }
            old.copy(mode = selection.mode, sourcePackage = selection.sourcePackage, sourceStamp = selection.sourceStamp,
                selectionJson = selection.copy(needsRevalidation = true).encode(), version = old.version + 1,
                updatedAt = System.currentTimeMillis()).also { dao.putMetadata(it) }
        }
    }

    suspend fun insertVariant(row: IconAssetVariant) = withAssetLease {
        val snapshot = snapshotUnlocked()
        validateSnapshot(snapshot.copy(variants = snapshot.variants + row), database.profileDao().getAll().map { it.id }.toSet())
        snapshot.assets.filter { it.profileId == row.profileId && it.hash in listOfNotNull(row.rawAssetHash, row.editedAssetHash) }
            .forEach { files.read(it) }
        database.withTransaction { dao.insertVariants(listOf(row)) }
    }

    suspend fun variants(id: Long, key: String): List<IconAssetVariant> = dao.variants(id, key)

    suspend fun snapshot(): ProjectAiSnapshot = withAssetLease { database.withTransaction { snapshotUnlocked() } }
    private suspend fun snapshotUnlocked() = ProjectAiSnapshot(dao.allMetadata(), dao.allRevisions(),
        dao.allAssets(), dao.allReferences(), dao.allVariants(), dao.allApprovals())

    /** Only unknown files/temp files are collected. Draft imports with Room rows remain pinned;
     * active/history/variant records cannot lose their images. No automatic startup GC. */
    suspend fun collectUnreferencedFiles(): Int = withAssetLease { files.collectUnreferenced(dao.allAssets()) }
    suspend fun discardDraftReferences(id: Long, hashes: List<String>) = withAssetLease {
        database.withTransaction { dao.discardUnlinked(id, hashes) }
        files.collectUnreferenced(dao.allAssets())
    }

    companion object {
        private val locks = WeakHashMap<RenkinPackDatabase, Mutex>()
        internal fun validateStyle(row: StyleRevision, hashes: List<String>) {
            require(row.schemaVersion == 1 && row.revision > 0 && row.profileId > 0)
            require(row.origin in setOf("USER", "IMPORTED", "AUTO_ANALYSIS"))
            require(row.specification.isNotBlank() && row.specification.length <= 6000 && row.userInstruction.length <= 2000)
            require(hashes.size <= 5 && hashes.distinct().size == hashes.size && hashes.all { it.matches(Regex("[a-f0-9]{64}")) })
            require((row.analysisProfileJson == null) == (row.analysisEvidenceJson == null)) { "Incomplete analysis evidence." }
            row.analysisEvidenceJson?.let { evidence ->
                ReviewedAnalysisEvidence.parse(evidence).validate(row.profileId, checkNotNull(row.analysisProfileJson), row.specification, row.origin, hashes)
            }
        }

        fun validateSnapshot(data: ProjectAiSnapshot, profileIds: Set<Long>) {
            validateTrialSnapshot(data)
            require(data.metadata.map { it.profileId }.distinct().size == data.metadata.size)
            require(data.revisions.map { it.profileId to it.revision }.distinct().size == data.revisions.size)
            require(data.assets.map { it.profileId to it.hash }.distinct().size == data.assets.size)
            require(data.references.map { Triple(it.profileId, it.revision, it.position) }.distinct().size == data.references.size)
            require(data.variants.map { it.id }.distinct().size == data.variants.size)
            val revisions = data.revisions.associateBy { it.profileId to it.revision }
            val assets = data.assets.associateBy { it.profileId to it.hash }
            data.metadata.forEach {
                require(it.profileId in profileIds && it.version >= 0 && it.mode in setOf("CREATE", "EXTEND"))
                require(if (it.mode == "CREATE") it.sourcePackage == null && it.sourceStamp == null else !it.sourcePackage.isNullOrBlank())
                require(it.activeRevision == null || revisions.containsKey(it.profileId to it.activeRevision))
                ProjectSelection.decode(it.selectionJson)?.let { selection ->
                    require(selection.mode == it.mode && selection.sourcePackage == it.sourcePackage)
                }
            }
            data.assets.forEach {
                require(it.profileId in profileIds && it.relativePath == ProjectAssetStore.path(it.profileId, it.hash))
                require(it.byteCount in 33..ProjectAssetStore.MAX_BYTES.toLong() && it.width in 1..4096 && it.height in 1..4096)
            }
            data.references.forEach {
                require(revisions.containsKey(it.profileId to it.revision) && assets.containsKey(it.profileId to it.assetHash))
            }
            data.revisions.forEach { row ->
                require(row.profileId in profileIds)
                val refs = data.references.filter { it.profileId == row.profileId && it.revision == row.revision }.sortedBy { it.position }
                require(refs.map { it.position } == refs.indices.toList())
                validateStyle(row, refs.map { it.assetHash })
            }
            data.variants.forEach {
                require(it.id.isNotBlank() && it.id.length <= 100 && it.componentKey.isNotBlank() && it.componentKey.length <= 1000)
                require(revisions.containsKey(it.profileId to it.styleRevision) && assets.containsKey(it.profileId to it.rawAssetHash))
                require(it.editedAssetHash == null || assets.containsKey(it.profileId to it.editedAssetHash))
                require(it.originalHash.matches(Regex("[a-f0-9]{64}")) && it.origin in setOf("GENERATED", "MANUAL", "IMPORTED"))
                require(it.requestStatus in setOf("COMPLETED", "UNCERTAIN", "FAILED"))
                require(it.reviewStatus in setOf("UNREVIEWED", "ACCEPTED", "REJECTED"))
                require(it.applyStatus in setOf("NOT_APPLIED", "APPLIED"))
            }
        }

        /** Must be inside the existing database transaction, after profile replacement. */
        internal suspend fun insertSnapshot(database: RenkinPackDatabase, data: ProjectAiSnapshot) {
            val dao = database.projectAiDao()
            dao.insertAssets(data.assets)
            dao.insertRevisions(data.revisions)
            dao.insertReferences(data.references)
            dao.insertVariants(data.variants)
            dao.putApprovals(data.approvals)
            data.metadata.forEach { dao.putMetadata(it) }
        }
    }
}
