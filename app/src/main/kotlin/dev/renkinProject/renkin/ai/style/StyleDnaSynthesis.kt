package dev.renkinProject.renkin.ai.style

/** Local synthesis is independent of the model's family/palette prose. No additional POST. */
internal fun synthesizeDnaLocally(reports: List<DnaIconReport>, images: Map<String,ByteArray>): StyleDnaV1 {
    require(reports.isNotEmpty())
    val families = reports.groupBy { Triple(it.familyVote,it.tileVote,it.depthVote) }.entries.sortedByDescending { it.value.size }.mapIndexed { index,(_,members) ->
        val ids=members.map { it.sampleId }
        fun recurring(trait: String): DnaRule {
            val groups=members.filter { it.traits.getValue(trait).provenance==DnaProvenance.OBSERVED }.groupBy { it.traits.getValue(trait).value }
            val best=groups.maxByOrNull { it.value.size }
            return if(best!=null && best.value.size>=2 && best.value.size*2>=members.size)
                DnaRule(best.key,DnaProvenance.OBSERVED,best.value.map { it.sampleId })
            else DnaRule("Unknown; per-image observations do not establish a recurring drawing rule",DnaProvenance.UNKNOWN,emptyList())
        }
        val policy=measuredFamilyPolicy(members,images)
        val tiles=members.map { it.tileVote }.distinct(); val depths=members.map { it.depthVote }.distinct()
        val mapping=mapOf("background_tile" to "tile_geometry","geometry" to "simplification","stroke_fill" to "stroke_fill",
            "rounding" to "corners","simplification" to "simplification","contour" to "stroke_fill","scale_padding" to "optical_scale",
            "optical_alignment" to "negative_space","negative_space" to "negative_space","light_shadow_depth" to "shadow_gradient",
            "gradient_use" to "shadow_gradient","wordmarks" to "wordmark_handling")
        val rules=DNA_DRAWING.associateWith { key -> mapping[key]?.let(::recurring)
            ?: DnaRule("Unknown; preserve the target's essential identifying geometry",DnaProvenance.UNKNOWN,emptyList()) }
        DnaFamily("family${index+1}",ids,members.map { it.symbolType }.distinct()+listOf(DnaSymbolType.UNKNOWN).filter { members.none { r->r.symbolType==it } },
            if(members.size>=2 && tiles.size==1) tiles.single() else DnaTile.UNKNOWN,
            if(members.size>=2 && depths.size==1) depths.single() else DnaDepth.UNKNOWN,policy,measuredFamilyRoles(members,images),rules,
            emptyList(),emptyList(),listOf(DnaRule("Never copy any reference logo or symbol",DnaProvenance.INFERRED,ids)))
    }
    // More than eight genuinely different votes stays uncertain; never silently collapse unrelated styles.
    require(families.size<=8) { "Too many observed families; review a smaller source set." }
    return StyleDnaV1(families,families.first().id,listOf("Family counts describe analyzed samples, not the entire pack or calibrated confidence.",
        "Quick uses conservative deterministic recurrence; unknown traits require human review.")).also { it.encode() }
}
private fun measuredFamily(reports: List<DnaIconReport>,images: Map<String,ByteArray>) = reports.map { r ->
    val png=checkNotNull(images[r.sampleHash]); SampledReference(r.sampleId,"style",r.sampleHash,png,familyColorFeatures(png))
}
/** Measure principal paint from stable opaque interiors. Resampling a two-color boundary
 * creates intermediate RGB values; those edge mixtures are not additional paint colors.
 * Geometry and legacy aggregate measurements remain unchanged. Fine/soft artwork falls
 * back to the ordinary alpha-aware measurement rather than inventing a fixed palette. */
private fun familyColorFeatures(png: ByteArray): ReferenceFeatures {
    val features=referenceFeatures(png)
    val bitmap=checkNotNull(android.graphics.BitmapFactory.decodeByteArray(png,0,png.size))
    try {
        val interior=mutableListOf<Pair<Int,Double>>()
        for(y in 2 until bitmap.height-2 step 4) for(x in 2 until bitmap.width-2 step 4) {
            val color=bitmap.getPixel(x,y)
            if(android.graphics.Color.alpha(color)<224) continue
            val neighbors=listOf(bitmap.getPixel(x-1,y),bitmap.getPixel(x+1,y),bitmap.getPixel(x,y-1),bitmap.getPixel(x,y+1))
            if(neighbors.all { android.graphics.Color.alpha(it)>=224 && colorDistance(color,it)<=.02 })
                interior+=(color or (0xff shl 24)) to 1.0
        }
        if(interior.size<20) return features
        val palette=measuredColors(interior)
        return features.copy(palette=palette.map { colorHex(it.first) },paletteWeights=palette.map { it.second })
    } finally {bitmap.recycle()}
}
internal fun measuredFamilyPolicy(reports: List<DnaIconReport>,images: Map<String,ByteArray>) =
    checkNotNull(packStyleEvidence(measuredFamily(reports,images))).colorPolicy
private fun measuredFamilyRoles(reports: List<DnaIconReport>,images: Map<String,ByteArray>): List<DnaColorRole> {
    val evidence=checkNotNull(packStyleEvidence(measuredFamily(reports,images)))
    return evidence.swatches.groupBy { if(it.role=="neutral") "neutral" else "accent" }.map { (role,swatches) ->
        DnaColorRole(role,swatches.map { it.argb },swatches.maxOf { it.icons },
            if(evidence.colorPolicy==PackColorPolicy.ADAPT_BRAND_COLORS && role=="accent") "VARIABLE" else "TENTATIVE") }
}
/** Reject unsupported mandatory palette/tile/depth assertions rather than beautifying unknowns. */
internal fun validateDnaSynthesis(dna: StyleDnaV1,reports: List<DnaIconReport>,images: Map<String,ByteArray>) {
    dna.encode(); require(dna.families.flatMap { it.evidenceIds }.toSet()==reports.map { it.sampleId }.toSet())
    for(family in dna.families) {
        val members=family.evidenceIds.map { id->reports.single { it.sampleId==id } }
        val local=measuredFamilyPolicy(members,images)
        if(family.colorPolicy in setOf(PackColorPolicy.FIXED_PALETTE,PackColorPolicy.MONOCHROME,PackColorPolicy.DUOTONE)) require(family.colorPolicy==local)
        if(local==PackColorPolicy.ADAPT_BRAND_COLORS) require(family.colorPolicy in setOf(PackColorPolicy.ADAPT_BRAND_COLORS,PackColorPolicy.MIXED,PackColorPolicy.UNKNOWN))
        if(family.tile !in setOf(DnaTile.UNKNOWN,DnaTile.MIXED)) require(members.count { it.tileVote==family.tile }>=maxOf(2,(members.size+1)/2))
        if(family.depth !in setOf(DnaDepth.UNKNOWN,DnaDepth.MIXED)) require(members.count { it.depthVote==family.depth }>=maxOf(2,(members.size+1)/2))
        (family.drawing.values+family.must).filter { it.provenance==DnaProvenance.OBSERVED }.forEach { require(it.evidenceIds.size>=2) }
        family.colorRoles.forEach { role ->
            val actual=members.count { report -> referenceFeatures(images.getValue(report.sampleHash)).palette.any { color -> role.colors.any { colorDistance(android.graphics.Color.parseColor(color),android.graphics.Color.parseColor(it))<=.05 } } }
            if(role.colors.isNotEmpty()) require(role.observedCount<=actual)
        }
    }
}

/** Only reviewed grammar is compiled. Per-reference silhouettes/identities never enter this recipe. */
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
    private fun recipe(f: DnaFamily,target: DnaSymbolType): String {
        fun instruction(key: String): String = f.drawing.getValue(key).let { r ->
            if(r.provenance==DnaProvenance.UNKNOWN) "UNKNOWN: preserve target geometry; do not invent this trait"
            else "${r.provenance.name}: ${r.value}" }
        return buildString {
            appendLine("Reviewed Style DNA family ${f.id}; symbol type ${target.name}. Evidence counts are sampled observations, not confidence.")
            appendLine("Transform IMAGE 1 only. Preserve its recognizable silhouette, essential logotype and meaningful native colors.")
            appendLine("1. Simplify target geometry: ${instruction("geometry")}; ${instruction("simplification")}; contour: ${instruction("contour")}")
            appendLine("2. Set optical scale, alignment and negative space: ${instruction("scale_padding")}; ${instruction("optical_alignment")}; ${instruction("negative_space")}")
            appendLine("3. Apply fill/stroke, rounding and tile: ${instruction("stroke_fill")}; ${instruction("rounding")}; tile policy ${f.tile}: ${instruction("background_tile")}")
            appendLine("4. Lighting/depth policy ${f.depth}: ${instruction("light_shadow_depth")}; gradient: ${instruction("gradient_use")}")
            appendLine("5. Color policy ${f.colorPolicy.name}. " + when(f.colorPolicy) {
                PackColorPolicy.ADAPT_BRAND_COLORS,PackColorPolicy.MIXED,PackColorPolicy.UNKNOWN -> "Preserve original target hues. Neutral tile colors never impose a fixed logo palette."
                else -> "Intentional recoloring is permitted only by this explicitly reviewed recurring family policy; preserve identity." })
            f.colorRoles.forEach { appendLine("Color role ${it.role}: ${it.strength}; ${it.colors.joinToString(", ")}; ${it.observedCount} sampled reports. These are role observations.") }
            val key=when(target) { DnaSymbolType.WORDMARK->"wordmarks";DnaSymbolType.MONOGRAM->"monograms";DnaSymbolType.GEOMETRIC_PICTOGRAM->"geometric_pictograms";DnaSymbolType.COMPLEX_EMBLEM->"complex_emblems";else->null }
            if(key!=null) appendLine("Target-type handling: ${instruction(key)}") else appendLine("Target type is UNKNOWN; retain the original essential typography and symbol without guessing its brand.")
            f.must.forEach { appendLine("Must (${it.provenance}): ${it.value}") }; f.should.forEach { appendLine("Should (${it.provenance}): ${it.value}") };f.mustNot.forEach { appendLine("Must not (${it.provenance}): ${it.value}") }
            appendLine("6. Check recognition and legibility at 48px. Produce one centered transparent 1024x1024 PNG.")
            append("No reference symbols, collages, generic modernization or arbitrary dark rounded tiles. Unknown/mixed depth is not permission for glow, 3D, gradients or shadows.")
        }.also { require(it.length<=6000) { "Reviewed recipe exceeds the bounded generation budget." } }
    }
    fun specification(saved: SavedStyleDna) = recipe(saved,DnaSymbolType.UNKNOWN)
}
