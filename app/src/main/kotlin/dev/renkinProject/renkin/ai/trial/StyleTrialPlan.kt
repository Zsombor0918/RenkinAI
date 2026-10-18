package dev.renkinProject.renkin.ai.trial

import dev.renkinProject.renkin.ai.AiSettings
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.project.sha256
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun trialHash(value: String) = require(value.matches(Regex("[a-f0-9]{64}")))
internal fun trialId(value: String) = require(UUID.fromString(value).toString() == value)
internal fun referenceFingerprint(hashes: List<String>): String {
    require(hashes.size <= 5 && hashes.distinct().size == hashes.size)
    hashes.forEach(::trialHash)
    return sha256(JSONArray(hashes).toString().toByteArray())
}
internal fun styleFingerprint(specification: String, instruction: String) =
    sha256(canonicalJson(json("specification" to specification, "instruction" to instruction)).toByteArray())

/** All values remain local. Equality is deliberately conservative; none is cost permission. */
internal data class TrialContext(val profileId: Long, val revision: Long, val metadataVersion: Long,
    val mode: String, val sourcePackage: String?, val sourceStamp: String?, val catalogStamp: String,
    val selectionHash: String, val referenceFingerprint: String, val epoch: String,
    val provider: String, val model: String, val quality: String, val styleFingerprint: String = "", val dnaFingerprint: String = "") {
    fun validate() {
        require(profileId > 0 && revision > 0 && metadataVersion >= 0)
        require(mode in setOf("CREATE", "EXTEND"))
        require(if (mode == "CREATE") sourcePackage == null && sourceStamp == null else !sourcePackage.isNullOrBlank() && sourceStamp != null)
        require(sourcePackage == null || sourcePackage.length <= 500)
        sourceStamp?.let(::trialHash); trialHash(catalogStamp); trialHash(selectionHash); trialHash(referenceFingerprint)
        require(epoch.matches(Regex("[a-zA-Z0-9:-]{1,140}")))
        require(provider in AiSettings.PROVIDERS && model in AiSettings.MODELS && quality in AiSettings.QUALITIES)
        if (styleFingerprint.isNotEmpty()) trialHash(styleFingerprint)
        if (dnaFingerprint.isNotEmpty()) trialHash(dnaFingerprint)
    }
    fun json(): JSONObject { validate(); return json("profileId" to profileId, "revision" to revision,
        "metadataVersion" to metadataVersion, "mode" to mode, "sourcePackage" to sourcePackage,
        "sourceStamp" to sourceStamp, "catalogStamp" to catalogStamp, "selectionHash" to selectionHash,
        "referenceFingerprint" to referenceFingerprint, "epoch" to epoch, "provider" to provider,
        "model" to model, "quality" to quality).also {
            if (styleFingerprint.isNotEmpty()) it.put("styleFingerprint", styleFingerprint)
            if (dnaFingerprint.isNotEmpty()) it.put("dnaFingerprint", dnaFingerprint)
        } }
    fun encode() = canonicalJson(json())
    /** Source/selection/epoch changes need human re-review, while actual artwork inputs stay fixed. */
    fun sameArtworkInputs(other: TrialContext) = profileId == other.profileId && revision == other.revision &&
        styleFingerprint.isNotEmpty() && styleFingerprint == other.styleFingerprint && dnaFingerprint == other.dnaFingerprint &&
        referenceFingerprint == other.referenceFingerprint && provider == other.provider && model == other.model && quality == other.quality
    fun sameStyleKeys(other: TrialContext) = profileId == other.profileId && revision == other.revision &&
        referenceFingerprint == other.referenceFingerprint && provider == other.provider && model == other.model && quality == other.quality && dnaFingerprint == other.dnaFingerprint
    companion object {
        fun parse(r: JSONObject): TrialContext {
            r.exactKeys(setOf("profileId", "revision", "metadataVersion", "mode", "sourcePackage", "sourceStamp",
                "catalogStamp", "selectionHash", "referenceFingerprint", "epoch", "provider", "model", "quality") +
                (if (r.has("styleFingerprint")) setOf("styleFingerprint") else emptySet()) + (if (r.has("dnaFingerprint")) setOf("dnaFingerprint") else emptySet()))
            return TrialContext(r.longValue("profileId"), r.longValue("revision"), r.longValue("metadataVersion"),
                r.stringValue("mode"), if (r.isNull("sourcePackage")) null else r.stringValue("sourcePackage"),
                if (r.isNull("sourceStamp")) null else r.stringValue("sourceStamp"), r.stringValue("catalogStamp"),
                r.stringValue("selectionHash"), r.stringValue("referenceFingerprint"), r.stringValue("epoch"),
                r.stringValue("provider"), r.stringValue("model"), r.stringValue("quality"),
                if (r.has("styleFingerprint")) r.stringValue("styleFingerprint") else "",
                if (r.has("dnaFingerprint")) r.stringValue("dnaFingerprint") else "").also { it.validate() }
        }
    }
}

internal data class TrialInput(val key: String, val name: String, val originalHash: String,
    val origin: String, val requestId: String = UUID.randomUUID().toString(), val originalEvidence: OriginalIconEvidence? = null) {
    fun validate() {
        require(key.isNotBlank() && key.length <= 1000 && '/' in key && '\u0000' !in key)
        require(name.length <= 1000 && origin.isNotBlank() && origin.length <= 1000)
        trialHash(originalHash); trialId(requestId)
        originalEvidence?.validate()
    }
    fun json(): JSONObject { validate(); return json("key" to key, "name" to name, "originalHash" to originalHash,
        "origin" to origin, "requestId" to requestId).also { if (originalEvidence != null) it.put("originalEvidence", originalEvidence.json()) } }
    companion object {
        fun parse(r: JSONObject): TrialInput {
            r.exactKeys(setOf("key", "name", "originalHash", "origin", "requestId") + if (r.has("originalEvidence")) setOf("originalEvidence") else emptySet())
            return TrialInput(r.stringValue("key"), r.stringValue("name"), r.stringValue("originalHash"),
                r.stringValue("origin"), r.stringValue("requestId"),
                if (r.has("originalEvidence")) OriginalIconEvidence.parse(r.getJSONObject("originalEvidence")) else null).also { it.validate() }
        }
    }
}

/** Primitive frozen input only. Actual PNGs are privately staged after explicit consent. */
internal data class StyleTrialPlan(val context: TrialContext, val specification: String, val instruction: String,
    val referenceHashes: List<String>, val inputs: List<TrialInput>, val id: String = UUID.randomUUID().toString(),
    val drawingRecipes: Map<String, String> = emptyMap(), val promptVersion: Int = 1,
    val promptHashes: Map<String, String> = emptyMap()) {
    val endpointMaximum get() = inputs.size * if (context.provider == AiSettings.STABILITY) 2 else 1
    val fingerprint get() = sha256(encode().toByteArray())
    fun appearance(input: TrialInput): String {
        val recipe = drawingRecipes[input.key]
        return if (context.provider != AiSettings.OPENAI) PromptComposer.composeSaved(recipe ?: specification, instruction)
        else if (promptVersion == 2) PromptComposer.referenceFirstAppearance(specification, instruction, recipe)
        else if (recipe != null) dnaTrialAppearance(recipe, specification, instruction)
        else PromptComposer.openAiTrialAppearance(specification, instruction)
    }
    fun effectivePrompt(input: TrialInput): String = if (context.provider == AiSettings.OPENAI)
        PromptComposer.versionedEditPrompt(appearance(input), referenceHashes.size, promptVersion) else appearance(input)
    /** New plans only. Never upgrade a restored/consumed plan or change its UUID's request. */
    fun freezeReferenceFirstPrompt(): StyleTrialPlan {
        if (context.provider != AiSettings.OPENAI) return this
        val next = copy(promptVersion = 2, promptHashes = emptyMap())
        return next.copy(promptHashes = inputs.associate { it.key to sha256(next.effectivePrompt(it).toByteArray(Charsets.UTF_8)) })
    }
    fun validate() {
        context.validate(); trialId(id)
        cleanText(specification, 6000, true); require(specification.isNotBlank()); cleanText(instruction, 2000, true)
        require(context.styleFingerprint.isEmpty() || context.styleFingerprint == styleFingerprint(specification, instruction))
        val prompt = if (context.provider == AiSettings.OPENAI && promptVersion == 2)
            PromptComposer.referenceFirstEditPrompt(PromptComposer.referenceFirstAppearance(specification, instruction), referenceHashes.size)
            else if (context.provider == AiSettings.OPENAI)
            PromptComposer.openAiEditPrompt(PromptComposer.openAiTrialAppearance(specification, instruction), referenceHashes.size)
            else PromptComposer.composeSaved(specification, instruction)
        require(prompt.length <= dev.renkinProject.renkin.ai.MAX_COMPOSED_ICON_PROMPT)
        require(referenceFingerprint(referenceHashes) == context.referenceFingerprint)
        require(context.provider != AiSettings.STABILITY || referenceHashes.size == 1)
        require(inputs.size in 1..3 && inputs.map { it.key }.distinct().size == inputs.size &&
            inputs.map { it.requestId }.distinct().size == inputs.size)
        inputs.forEach { it.validate() }
        require((context.dnaFingerprint.isEmpty() && drawingRecipes.isEmpty()) ||
            context.dnaFingerprint.isNotEmpty() && drawingRecipes.keys == inputs.map { it.key }.toSet())
        drawingRecipes.forEach { (_,recipe) -> cleanText(recipe,6000,true); require(recipe.isNotBlank())
            if (context.provider == AiSettings.OPENAI && promptVersion == 1) PromptComposer.openAiEditPrompt(dnaTrialAppearance(recipe,specification,instruction),referenceHashes.size)
            else if (context.provider != AiSettings.OPENAI) require(PromptComposer.composeSaved(recipe,instruction).length <= dev.renkinProject.renkin.ai.MAX_COMPOSED_ICON_PROMPT) }
        require(promptVersion in 1..2)
        if (promptVersion == 1) require(promptHashes.isEmpty()) else {
            require(context.provider == AiSettings.OPENAI && promptHashes.keys == inputs.map { it.key }.toSet())
            inputs.forEach { require(promptHashes[it.key] == sha256(effectivePrompt(it).toByteArray(Charsets.UTF_8))) }
        }
    }
    fun encode(): String { validate(); return canonicalJson(json("schema" to 1, "id" to id,
        "context" to context.json(), "specification" to specification, "instruction" to instruction,
        "referenceHashes" to JSONArray(referenceHashes), "inputs" to JSONArray(inputs.map { it.json() })).also {
            if (drawingRecipes.isNotEmpty()) it.put("drawingRecipes",json(*drawingRecipes.map { it.key to it.value }.toTypedArray()))
            if (promptVersion != 1) { it.put("promptVersion", promptVersion); it.put("promptHashes", JSONObject(promptHashes)) }
        }) }
    companion object {
        fun parse(text: String): StyleTrialPlan {
            val r = strictObject(text, 65536, 12); require(r.longValue("schema") == 1L)
            r.exactKeys(setOf("schema", "id", "context", "specification", "instruction", "referenceHashes", "inputs") +
                (if (r.has("drawingRecipes")) setOf("drawingRecipes") else emptySet()) +
                (if (r.has("promptVersion")) setOf("promptVersion", "promptHashes") else emptySet()))
            val version = if (r.has("promptVersion")) r.longValue("promptVersion").also { require(it == 2L) }.toInt() else 1
            val refs = r.getJSONArray("referenceHashes"); require(refs.length() <= 5)
            val inputs = r.getJSONArray("inputs"); require(inputs.length() in 1..3)
            return StyleTrialPlan(TrialContext.parse(r.getJSONObject("context")), r.stringValue("specification"),
                r.stringValue("instruction"), (0 until refs.length()).map { refs.getString(it) },
                (0 until inputs.length()).map { TrialInput.parse(inputs.getJSONObject(it)) }, r.stringValue("id"),
                if (r.has("drawingRecipes")) r.getJSONObject("drawingRecipes").let { recipes -> require(recipes.length()<=3);
                    recipes.keys().asSequence().associateWith { recipes.stringValue(it) } } else emptyMap(), version,
                if (version == 2) r.getJSONObject("promptHashes").let { hashes -> require(hashes.length() <= 3)
                    hashes.keys().asSequence().associateWith { hashes.stringValue(it) } } else emptyMap()).also { it.validate() }
        }
    }
}

/** Portable input evidence. Share can rekey the owner, but never transfer paid request rights. */
internal data class TrialVariantEvidence(val context: TrialContext, val planId: String, val requestId: String,
    val originalOrigin: String, val referenceHashes: List<String>, val reviewContext: TrialContext? = null,
    val reviewOriginalOrigin: String? = null) {
    fun encode(): String {
        context.validate(); trialId(planId); trialId(requestId)
        require(originalOrigin.isNotBlank() && originalOrigin.length <= 1000)
        require(referenceFingerprint(referenceHashes) == context.referenceFingerprint)
        require((reviewContext == null) == (reviewOriginalOrigin == null))
        reviewContext?.let { it.validate(); require(context.sameStyleKeys(it) && context.styleFingerprint == it.styleFingerprint) }
        reviewOriginalOrigin?.let { require(it.isNotBlank() && it.length <= 1000) }
        return canonicalJson(json("schema" to 2, "context" to context.json(), "planId" to planId,
            "requestId" to requestId, "originalOrigin" to originalOrigin, "referenceHashes" to JSONArray(referenceHashes),
            "reviewContext" to reviewContext?.json(), "reviewOriginalOrigin" to reviewOriginalOrigin))
    }
    companion object {
        fun parse(text: String): TrialVariantEvidence {
            val r = strictObject(text, 16384, 8); val schema = r.longValue("schema"); require(schema in 1L..2L)
            r.exactKeys(setOf("schema", "context", "planId", "requestId", "originalOrigin", "referenceHashes") +
                if (schema == 2L) setOf("reviewContext", "reviewOriginalOrigin") else emptySet())
            val refs = r.getJSONArray("referenceHashes"); require(refs.length() <= 5)
            return TrialVariantEvidence(TrialContext.parse(r.getJSONObject("context")), r.stringValue("planId"),
                r.stringValue("requestId"), r.stringValue("originalOrigin"),
                (0 until refs.length()).map { refs.getString(it) },
                if (schema == 1L || r.isNull("reviewContext")) null else TrialContext.parse(r.getJSONObject("reviewContext")),
                if (schema == 1L || r.isNull("reviewOriginalOrigin")) null else r.stringValue("reviewOriginalOrigin")).also { it.encode() }
        }
    }
}

internal data class TrialCandidate(val target: CoverageTarget, val eligible: Boolean,
    val recommended: Boolean, val overrideRequired: Boolean, val reason: String)

/** Deterministic spread is a selection aid, never a measured visual complexity guarantee. */
internal object StyleTrialTargetSelector {
    fun candidates(mode: String, targets: List<CoverageTarget>, selected: List<String>,
        coverage: CoverageSnapshot? = null): List<TrialCandidate> {
        require(mode in setOf("CREATE", "EXTEND"))
        val unique = uniqueCoverageTargets(targets).associateBy { it.key }
        val entries = coverage?.entries.orEmpty().associateBy { it.target.key }
        return selected.distinct().sorted().map { key ->
            val t = unique[key] ?: CoverageTarget(key, key, LocalIconState.UNKNOWN)
            val source = entries[key]?.source
            val blocked = when {
                !t.selectable -> "locked_unknown_user"
                t.local in setOf(LocalIconState.CUSTOM_OR_MANUAL, LocalIconState.OTHER_APPLIED) -> "protected_local"
                mode == "EXTEND" && source !in setOf(SourceCoverage.RESOLVABLE, SourceCoverage.UNMAPPED, SourceCoverage.DECLARED_BROKEN) -> "source_unproven"
                else -> null
            }
            val override = mode == "EXTEND" && source == SourceCoverage.RESOLVABLE
            TrialCandidate(t, blocked == null, blocked == null && !override, override,
                blocked ?: if (override) "covered_override" else "eligible")
        }
    }
    fun recommend(candidates: List<TrialCandidate>): List<String> {
        val keys = candidates.filter { it.recommended }.map { it.target.key }.distinct().sorted()
        return if (keys.size <= 3) keys else listOf(keys.first(), keys[keys.size / 2], keys.last())
    }
    fun validateChoice(candidates: List<TrialCandidate>, keys: List<String>, allowCovered: Boolean) {
        require(keys.size in 1..3 && keys.distinct().size == keys.size)
        require(keys.all { key -> candidates.any { it.target.key == key && it.eligible && (!it.overrideRequired || allowCovered) } })
    }
}

/** Fixed recipe and optional user appearance stay below the final original/output contract. */
internal fun dnaTrialAppearance(recipe: String,specification: String,instruction: String): String {
    cleanText(recipe,6000,true);cleanText(specification,6000,true);cleanText(instruction,2000,true)
    return "Reviewed target drawing recipe:\n$recipe\nSeparate user-reviewed style guidance: ${org.json.JSONObject.quote(specification)}\nProject instruction: ${org.json.JSONObject.quote(instruction)}"
}
