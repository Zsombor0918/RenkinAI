package dev.renkinProject.renkin.ai.style

/** Frozen historical recipe renderer; no analysis or provider operation. */
internal object StyleDnaCompiler {
    fun family(dna: StyleDnaV1,target: DnaSymbolType,override: String? = null): DnaFamily {
        dna.encode()
        return if(override!=null) dna.families.single { it.id==override }
            else dna.families.filter { target in it.applicability }.maxByOrNull { it.observedCount }
                ?: dna.families.single { it.id==dna.recommendedFamily }
    }
    fun recipe(dna: StyleDnaV1,target: DnaSymbolType,familyId: String? = null): String {
        val f=family(dna,target,familyId)
        return recipe(f,target)
    }
    fun family(saved: SavedStyleDna, familyId: String = saved.selectedFamily): DnaFamily {
        saved.encode()
        return if(familyId == DNA_PACK_STYLE) packDnaFamily(saved) else family(saved.dna,DnaSymbolType.UNKNOWN,familyId)
    }
    fun recipe(saved: SavedStyleDna,target: DnaSymbolType,familyId: String = saved.selectedFamily): String = recipe(family(saved,familyId),target)
    private fun recipe(f: DnaFamily,target: DnaSymbolType): String = buildString {
        appendLine("Reviewed Style DNA family ${f.id}; symbol type ${target.name}. Evidence counts are sampled observations, not confidence.")
        appendLine("Brand-preservation rule: Transform IMAGE 1 only. Preserve its recognizable silhouette, essential logotype and meaningful native colors.")
        appendLine("Abstraction rule: Transfer construction and simplification to the target's own geometry, never the reference logo. Remove incidental detail without removing identity cues.")
        val missing = mutableListOf<String>()
        fun rule(label: String, vararg keys: String) {
            val established = keys.map { f.drawing.getValue(it) }.filter { it.provenance != DnaProvenance.UNKNOWN }
                .distinctBy { it.value }
            if (established.isEmpty()) missing += label else appendLine("$label: " + established.joinToString("; ") {
                "${it.value} [${it.provenance}, ${it.evidenceIds.size}/${f.observedCount} sampled reports]" })
        }
        appendLine("Background mode: " + when(f.tile) {
            DnaTile.NONE -> "none; standalone forms, no separate background tile."
            DnaTile.ROUND -> "separate round tile where supported."
            DnaTile.SQUARE -> "separate square tile where supported."
            DnaTile.OTHER -> "other evidenced tile geometry; follow the construction rule."
            DnaTile.MIXED, DnaTile.UNKNOWN -> "variable or unresolved; do not impose a universal tile."
        })
        rule("Background construction", "background_tile")
        rule("Dominant shape / silhouette", "geometry", "contour")
        rule("Optical fill / padding", "scale_padding", "optical_alignment")
        rule("Edges / corners", "rounding")
        rule("Stroke / outline", "stroke_fill")
        rule("Simplification / complexity ceiling", "simplification")
        rule("Negative space", "negative_space")
        rule("Gradient / highlight / depth", "light_shadow_depth", "gradient_use")
        if(f.depth == DnaDepth.NONE) appendLine("Depth policy: flat; no added shadow, gradient or extrusion.")
        appendLine("Color policy: ${f.colorPolicy.name}. " + when(f.colorPolicy) {
            PackColorPolicy.ADAPT_BRAND_COLORS,PackColorPolicy.MIXED,PackColorPolicy.UNKNOWN -> "Preserve original target hues. Neutral tile colors never impose a fixed logo palette."
            else -> "Intentional recoloring is permitted only by this explicitly reviewed recurring family policy; preserve identity." })
        f.colorRoles.forEach { appendLine("Color role ${it.role}: ${it.strength}; ${it.colors.joinToString(", ")}; ${it.observedCount} sampled reports.") }
        appendLine("Text policy: Keep lettering only when required for target brand identity; no decorative text or captions.")
        rule("Observed text treatment", "wordmarks")
        val key=when(target) { DnaSymbolType.WORDMARK->"wordmarks";DnaSymbolType.MONOGRAM->"monograms";DnaSymbolType.GEOMETRIC_PICTOGRAM->"geometric_pictograms";DnaSymbolType.COMPLEX_EMBLEM->"complex_emblems";else->null }
        if(key != null && f.drawing.getValue(key).provenance != DnaProvenance.UNKNOWN)
            rule("Target-type handling",key)
        else appendLine("Target-type handling: Human review required; preserve the original identifying symbol and essential typography without guessing a brand.")
        f.must.filter { it.provenance != DnaProvenance.UNKNOWN }.forEach { appendLine("Must (${it.provenance}): ${it.value}") }
        f.should.filter { it.provenance != DnaProvenance.UNKNOWN }.forEach { appendLine("Should (${it.provenance}): ${it.value}") }
        f.mustNot.filter { it.provenance != DnaProvenance.UNKNOWN }.forEach { appendLine("Must not (${it.provenance}): ${it.value}") }
        appendLine("Pack consistency: Apply supported rules across the target; ${f.observedCount} analyzed samples, not an entire-pack measurement. Dimensions are qualitative unless explicitly measured.")
        if(missing.isNotEmpty()) appendLine("Unestablished traits: ${missing.distinct().joinToString(", ")}. Retain target geometry; do not invent a style rule.")
        appendLine("Check recognition and legibility at 48px. Produce exactly one standalone centered transparent 1024x1024 PNG.")
        append("No reference symbols, collages, generic modernization or arbitrary dark rounded tiles. Unknown/mixed depth is not permission for glow, 3D, gradients or shadows.")
    }.also { require(it.length<=6000) { "Reviewed recipe exceeds the bounded generation budget." } }
    fun specification(saved: SavedStyleDna) = recipe(saved,DnaSymbolType.UNKNOWN)
}
