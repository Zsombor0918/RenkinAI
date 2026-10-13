package dev.renkinProject.renkin.data.project

import androidx.room.withTransaction
import dev.renkinProject.renkin.ai.trial.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun trialApprovalIds(row: StyleTrialApproval): List<String> {
    require(row.variantIdsJson.length <= 32768)
    val a = JSONArray(row.variantIdsJson); require(a.length() in 1..256)
    return (0 until a.length()).map { a.getString(it).also(::trialId) }.also { require(it.distinct().size == it.size) }
}

/** Called by backup validation too; imported approval history never becomes current by inference. */
internal fun validateTrialSnapshot(data: ProjectAiSnapshot) {
    val assets = data.assets.associateBy { it.profileId to it.hash }
    val variants = data.variants.associateBy { it.id }
    data.variants.forEach { row -> row.trialContextJson?.let { text ->
        val e = TrialVariantEvidence.parse(text)
        require(e.context.profileId == row.profileId && e.context.revision == row.styleRevision && e.requestId == row.id)
        require(row.requestStatus == "COMPLETED" && assets.containsKey(row.profileId to row.originalHash))
        val original = assets.getValue(row.profileId to row.originalHash)
        val raw = assets.getValue(row.profileId to row.rawAssetHash)
        require(original.width == 1024 && original.height == 1024 && raw.width == 1024 && raw.height == 1024)
        require(e.referenceHashes.all { assets.containsKey(row.profileId to it) })
    } }
    require(data.approvals.map { it.profileId to it.styleRevision }.distinct().size == data.approvals.size)
    data.approvals.forEach { row ->
        trialId(row.approvalId); require(row.approvedAt > 0 && row.contextJson.length <= 16384)
        val context = TrialContext.parse(JSONObject(row.contextJson))
        require(context.profileId == row.profileId && context.revision == row.styleRevision)
        trialApprovalIds(row).forEach { id ->
            val v = checkNotNull(variants[id]); require(v.profileId == row.profileId && v.styleRevision == row.styleRevision)
            require(v.requestStatus == "COMPLETED" && v.trialContextJson != null)
            val evidence = TrialVariantEvidence.parse(v.trialContextJson)
            require(evidence.context.sameArtworkInputs(context))
            require(row.needsRevalidation || evidence.reviewContext == context)
            require(row.needsRevalidation || v.reviewStatus == "ACCEPTED")
        }
    }
}

internal suspend fun ProjectAiRepository.trialVariants(id: Long): List<IconAssetVariant> =
    database.projectAiDao().allVariants().filter { it.profileId == id && it.trialContextJson != null }

/** Durable RAW+original files precede one idempotent Room variant. Never writes profile icons. */
internal suspend fun ProjectAiRepository.installTrialVariant(plan: StyleTrialPlan, input: TrialInput,
    original: ByteArray, png: ByteArray, epochCurrent: () -> Boolean,
    fault: (String) -> Unit = {}): IconAssetVariant = withAssetLease {
    plan.validate(); require(input in plan.inputs && sha256(original) == input.originalHash && inspectPng(original) == (1024 to 1024))
    require(epochCurrent()) { "Project data was restored; old request rights are invalid." }
    validateTrialPng(png)
    val dao = database.projectAiDao()
    val savedStyle = checkNotNull(dao.history(plan.context.profileId).find { it.revision == plan.context.revision })
    check(savedStyle.specification == plan.specification && savedStyle.userInstruction == plan.instruction &&
        dao.references(plan.context.profileId).filter { it.revision == plan.context.revision }.map { it.assetHash } == plan.referenceHashes) {
        "Restored style history does not match the original generation input."
    }
    val evidence = TrialVariantEvidence(plan.context, plan.id, input.requestId, input.origin, plan.referenceHashes).encode()
    val existing = dao.variant(input.requestId)
    if (existing != null) {
        check(existing.profileId == plan.context.profileId && existing.componentKey == input.key &&
            existing.styleRevision == plan.context.revision && existing.originalHash == input.originalHash &&
            existing.rawAssetHash == sha256(png) && existing.trialContextJson?.let { TrialVariantEvidence.parse(it)
                .copy(reviewContext = null, reviewOriginalOrigin = null).encode() } == evidence)
        referenceBytesUnlocked(existing.profileId, existing.originalHash)
        referenceBytesUnlocked(existing.profileId, existing.rawAssetHash)
        return@withAssetLease existing
    }
    val originalAsset = files.install(plan.context.profileId, original, "TRIAL_ORIGINAL")
    val raw = files.install(plan.context.profileId, png, "TRIAL_RAW")
    fault("FILES_INSTALLED")
    val row = IconAssetVariant(input.requestId, plan.context.profileId, input.key, plan.context.revision,
        input.originalHash, raw.hash, trialContextJson = evidence)
    database.withTransaction {
        require(epochCurrent())
        check(database.profileDao().get(row.profileId) != null && dao.history(row.profileId).any { it.revision == row.styleRevision })
        val prior = dao.variant(row.id)
        if (prior == null) {
            dao.insertAssets(listOf(originalAsset, raw)); dao.insertVariants(listOf(row))
            dao.approval(row.profileId, row.styleRevision)?.let {
                dao.putApprovals(listOf(it.copy(needsRevalidation = true)))
            }
        }
        else check(prior.originalHash == row.originalHash && prior.rawAssetHash == row.rawAssetHash && prior.trialContextJson == evidence)
    }
    fault("DB_COMMITTED")
    checkNotNull(dao.variant(row.id))
}

// Caller already holds the asset lease; do not reenter its non-reentrant mutex.
private suspend fun ProjectAiRepository.referenceBytesUnlocked(id: Long, hash: String): ByteArray =
    files.read(checkNotNull(database.projectAiDao().assets(id).find { it.hash == hash }))

private suspend fun ProjectAiRepository.checkTrialContext(context: TrialContext) {
    context.validate()
    val m = checkNotNull(database.projectAiDao().metadata(context.profileId))
    check(m.activeRevision == context.revision && m.version == context.metadataVersion && m.mode == context.mode &&
        m.sourcePackage == context.sourcePackage && m.sourceStamp == context.sourceStamp &&
        sha256(m.selectionJson.toByteArray()) == context.selectionHash) { "Trial preparation changed. Review the current project." }
    val revision = checkNotNull(database.projectAiDao().history(context.profileId).find { it.revision == context.revision })
    check(context.styleFingerprint == styleFingerprint(revision.specification, revision.userInstruction))
    val refs = database.projectAiDao().references(context.profileId).filter { it.revision == context.revision }.map { it.assetHash }
    check(referenceFingerprint(refs) == context.referenceFingerprint)
}

internal suspend fun ProjectAiRepository.reviewTrial(context: TrialContext, id: String, accepted: Boolean,
    original: Pair<String, String>, epochCurrent: () -> Boolean) = withAssetLease {
    require(epochCurrent()); checkTrialContext(context)
    val dao = database.projectAiDao(); val row = checkNotNull(dao.variant(id))
    val evidence = TrialVariantEvidence.parse(checkNotNull(row.trialContextJson))
    check(row.profileId == context.profileId && evidence.context.sameArtworkInputs(context) && row.originalHash == original.first &&
        row.requestStatus == "COMPLETED")
    validateTrialPng(referenceBytesUnlocked(row.profileId, row.rawAssetHash))
    database.withTransaction {
        require(epochCurrent()); checkTrialContext(context)
        check(dao.reviewVariant(context.profileId, id, if (accepted) "ACCEPTED" else "REJECTED",
            evidence.copy(reviewContext = context, reviewOriginalOrigin = original.second).encode(), System.currentTimeMillis()) == 1)
        dao.approval(context.profileId, context.revision)?.let { approval ->
            if (!accepted && id in trialApprovalIds(approval)) dao.putApprovals(listOf(approval.copy(needsRevalidation = true)))
        }
    }
}

internal suspend fun ProjectAiRepository.approveTrials(context: TrialContext,
    originals: Map<String, Pair<String, String>>, epochCurrent: () -> Boolean): StyleTrialApproval = withAssetLease {
    require(epochCurrent()); checkTrialContext(context)
    val dao = database.projectAiDao()
    val current = dao.allVariants().filter { it.profileId == context.profileId && it.trialContextJson?.let { text ->
        TrialVariantEvidence.parse(text).let { it.context == context || it.reviewContext == context }
    } == true }
    check(current.isNotEmpty() && current.none { it.reviewStatus == "UNREVIEWED" }) { "Review every successful current trial first." }
    val accepted = current.filter { it.reviewStatus == "ACCEPTED" && it.trialContextJson?.let { text ->
        TrialVariantEvidence.parse(text).reviewContext == context } == true }.groupBy { it.componentKey }.values.map {
        it.maxWith(compareBy<IconAssetVariant> { v -> v.createdAt }.thenBy { v -> v.id })
    }.sortedBy { it.componentKey }
    check(accepted.isNotEmpty() && accepted.size <= 256) { "Accept at least one current trial." }
    accepted.forEach { row ->
        val e = TrialVariantEvidence.parse(checkNotNull(row.trialContextJson)); val original = originals[row.componentKey]
        check(original == (row.originalHash to e.reviewOriginalOrigin)) { "An original icon changed." }
        validateTrialPng(referenceBytesUnlocked(row.profileId, row.rawAssetHash))
    }
    val row = StyleTrialApproval(context.profileId, context.revision, UUID.randomUUID().toString(), context.encode(),
        JSONArray(accepted.map { it.id }).toString(), System.currentTimeMillis())
    database.withTransaction { require(epochCurrent()); checkTrialContext(context); dao.putApprovals(listOf(row)) }
    row
}

/** M6 read-only port. Identical fresh inputs are mandatory; this never submits or applies. */
internal suspend fun ProjectAiRepository.acceptedTrials(context: TrialContext,
    originals: Map<String, Pair<String, String>>, epochCurrent: () -> Boolean): List<AcceptedTrialVariant> = withAssetLease {
    if (!epochCurrent()) return@withAssetLease emptyList()
    checkTrialContext(context)
    val dao = database.projectAiDao()
    val approval = dao.approval(context.profileId, context.revision) ?: return@withAssetLease emptyList()
    if (approval.needsRevalidation || TrialContext.parse(JSONObject(approval.contextJson)) != context) return@withAssetLease emptyList()
    if (dao.allVariants().any { v -> v.profileId == context.profileId && v.reviewStatus == "UNREVIEWED" &&
        v.trialContextJson?.let { TrialVariantEvidence.parse(it).let { e -> e.context == context || e.reviewContext == context } } == true
    }) return@withAssetLease emptyList()
    val values = trialApprovalIds(approval).map { id ->
        val v = dao.variant(id) ?: return@withAssetLease emptyList()
        val e = v.trialContextJson?.let(TrialVariantEvidence::parse) ?: return@withAssetLease emptyList()
        if (v.profileId != context.profileId || v.styleRevision != context.revision || !e.context.sameArtworkInputs(context) || e.reviewContext != context ||
            v.reviewStatus != "ACCEPTED" || v.requestStatus != "COMPLETED" ||
            originals[v.componentKey] != (v.originalHash to e.reviewOriginalOrigin)) return@withAssetLease emptyList()
        validateTrialPng(referenceBytesUnlocked(v.profileId, v.rawAssetHash))
        AcceptedTrialVariant(v.profileId, v.componentKey, v.styleRevision, v.originalHash,
            context.referenceFingerprint, v.rawAssetHash, approval.approvedAt, v.id)
    }
    if (!epochCurrent()) emptyList() else values
}
