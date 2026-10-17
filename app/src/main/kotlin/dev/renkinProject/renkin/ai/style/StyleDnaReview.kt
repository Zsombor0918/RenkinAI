package dev.renkinProject.renkin.ai.style

/** Reserved selection, not a persisted extra family or a rewrite of provider evidence. */
internal const val DNA_PACK_STYLE = "__pack__"

/** Per-image silhouettes can describe a source logo's identity. Only these closed,
 * abstract construction phrases may enter a locally derived generation recipe.
 * Unrecognized prose stays in raw evidence; Standard can supply abstract drawing rules. */
internal fun abstractDnaSilhouette(text: String): String? =
    text.lowercase().trim().replace(Regex("\\s+"), " ").takeIf { it in setOf(
        "compact geometric masses with soft external contours",
        "rounded geometric masses with soft external contours",
        "angular geometric masses with crisp external contours",
        "bold connected silhouette with open internal counters",
        "open outline contours with clear internal gaps",
        "standalone dark silhouette forms") }

/** Small, conservative synonym vocabulary. Conflicting/negated prose stays unnormalized.
 * Raw observations always remain in SavedStyleDna and the journal. */
internal fun normalizedDnaObservation(trait: String, text: String): String {
    val value = text.lowercase().trim().replace(Regex("\\s+"), " ")
    return when {
        trait == "stroke_fill" && value in setOf("solid fill with smooth edges, no outline", "solid filled shapes with no visible stroke",
            "solid fills with no outline", "solid fills without an outline", "solid fill, no outline") -> "solid filled shapes without an outline"
        trait == "corners" && value in setOf("fully rounded ends", "rounded terminals and softened corners", "rounded terminals", "rounded ends", "softened corners") ->
            "rounded terminals and softened corners"
        else -> value
    }
}

private fun packDrawingRule(saved: SavedStyleDna, trait: String, drawing: String? = null): DnaRule {
    val reports = saved.reports
    val threshold = maxOf(2, (reports.size * 3 + 4) / 5)
    val candidates = reports.filter { it.traits.getValue(trait).provenance == DnaProvenance.OBSERVED }
        .mapNotNull { report ->
            val raw = report.traits.getValue(trait).value
            val value = if(trait == "logo_silhouette") abstractDnaSilhouette(raw) else normalizedDnaObservation(trait, raw)
            value?.let { it to report }
        }.groupBy({ it.first }, { it.second })
    val recurring = candidates.entries.sortedWith(compareByDescending<Map.Entry<String,List<DnaIconReport>>> { it.value.size }.thenBy { it.key }).firstOrNull()
    if (recurring != null && recurring.value.size >= threshold)
        return DnaRule(recurring.key, DnaProvenance.INFERRED, recurring.value.map { it.sampleId })
    // Standard's validated grammar can summarize differently worded reports. Require
    // actual cited support across the pack, never promote a small optional family.
    val synthesis = if (drawing == null) emptyMap() else saved.dna.families.map { it.drawing.getValue(drawing) }
        .filter { it.provenance != DnaProvenance.UNKNOWN && it.evidenceIds.size >= 2 }
        .groupBy { normalizedDnaObservation(trait, it.value) }
        .mapValues { (_, rules) -> rules.flatMap { it.evidenceIds }.distinct() }
    val supported = synthesis.filterValues { it.size >= threshold }.entries.singleOrNull()
    return if (supported != null) DnaRule(supported.key, DnaProvenance.INFERRED, supported.value)
        else DnaRule("Unestablished from recurring sample evidence", DnaProvenance.UNKNOWN, emptyList())
}

/** Derived pack view. Existing V1 partition, raw prose and sample evidence are untouched. */
internal fun packDnaFamily(saved: SavedStyleDna): DnaFamily {
    val reports = saved.reports
    val ids = reports.map { it.sampleId }
    val mapping = mapOf("background_tile" to "tile_geometry", "geometry" to "logo_silhouette", "stroke_fill" to "stroke_fill",
        "rounding" to "corners", "simplification" to "simplification", "contour" to "logo_silhouette", "scale_padding" to "optical_scale",
        "optical_alignment" to "optical_scale", "negative_space" to "negative_space", "light_shadow_depth" to "shadow_gradient",
        "gradient_use" to "shadow_gradient", "wordmarks" to "wordmark_handling")
    val drawing = DNA_DRAWING.associateWith { key -> mapping[key]?.let { packDrawingRule(saved,it,key) }
        ?: DnaRule("Unknown; preserve target identity",DnaProvenance.UNKNOWN,emptyList()) }
    val policies = saved.dna.families.map { it.colorPolicy }.distinct()
    val policy = policies.singleOrNull()?.takeUnless { candidate ->
        candidate in setOf(PackColorPolicy.FIXED_PALETTE,PackColorPolicy.MONOCHROME,PackColorPolicy.DUOTONE) &&
            saved.dna.families.map { it.colorRoles.flatMap { role -> role.colors }.toSet() }.distinct().size > 1
    } ?: PackColorPolicy.MIXED
    val roles = saved.dna.families.flatMap { it.colorRoles }.groupBy { it.role }.map { (role, observations) ->
        DnaColorRole(role,observations.flatMap { it.colors }.distinct().take(6),observations.minOf { it.observedCount },
            if(role != "neutral" && policy == PackColorPolicy.ADAPT_BRAND_COLORS) "VARIABLE" else "TENTATIVE")
    }
    return DnaFamily("pack_overall",ids,DnaSymbolType.entries.toList(),
        if(reports.size < 2) DnaTile.UNKNOWN else reports.map { it.tileVote }.distinct().singleOrNull() ?: DnaTile.MIXED,
        if(reports.size < 2) DnaDepth.UNKNOWN else reports.map { it.depthVote }.distinct().singleOrNull() ?: DnaDepth.MIXED,policy,roles,drawing,
        emptyList(),listOf(packDrawingRule(saved,"color_roles")).filter { it.provenance != DnaProvenance.UNKNOWN },
        listOf(DnaRule("Never copy any reference logo or symbol",DnaProvenance.INFERRED,ids)))
}

/** Stable diversity across observed construction/type categories, then original sample order.
 * No unrelated family is used to fill a small family's remaining reference slots. */
internal fun dnaReferenceHashes(saved: SavedStyleDna, family: String, available: List<String>): List<String> {
    val members = if(family == DNA_PACK_STYLE) saved.reports else {
        val ids = saved.dna.families.single { it.id == family }.evidenceIds.toSet()
        saved.reports.filter { it.sampleId in ids }
    }
    val ordered = available.distinct().mapNotNull { hash -> members.singleOrNull { it.sampleHash == hash } }
        .filter { it.outlierReason.isBlank() }
    val groups = ordered.groupBy { listOf(it.familyVote.name,it.tileVote.name,it.depthVote.name,it.symbolType.name) }.values.map { it.toMutableList() }
    val selected = mutableListOf<String>()
    while(selected.size < 5 && groups.any { it.isNotEmpty() }) {
        for(group in groups) if(group.isNotEmpty() && selected.size < 5) selected += group.removeAt(0).sampleHash
    }
    return selected
}

internal fun dnaReferencesMatch(saved: SavedStyleDna, family: String, hashes: List<String>): Boolean {
    val members = if(family == DNA_PACK_STYLE) saved.reports else {
        val ids = saved.dna.families.singleOrNull { it.id == family }?.evidenceIds ?: return false
        saved.reports.filter { it.sampleId in ids }
    }
    return hashes.size <= 5 && hashes.distinct().size == hashes.size && hashes.all { hash -> members.any { it.sampleHash == hash } }
}
