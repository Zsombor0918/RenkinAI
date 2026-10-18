package dev.renkinProject.renkin.ai.style

/** Historical fixture construction only. Local synthesis is independent of the model's family/palette prose. No additional POST. */
internal fun synthesizeDnaLocally(reports: List<DnaIconReport>, images: Map<String,ByteArray>): StyleDnaV1 {
    require(reports.isNotEmpty())
    val families = reports.groupBy { Triple(it.familyVote,it.tileVote,it.depthVote) }.entries.sortedByDescending { it.value.size }.mapIndexed { index,(_,members) ->
        val ids=members.map { it.sampleId }
        fun recurring(trait: String): DnaRule {
            val groups=members.filter { it.traits.getValue(trait).provenance==DnaProvenance.OBSERVED }.mapNotNull { report ->
                val raw=report.traits.getValue(trait).value
                val value=if(trait=="logo_silhouette") abstractDnaSilhouette(raw) else raw
                value?.let { it to report }
            }.groupBy({it.first},{it.second})
            val best=groups.maxByOrNull { it.value.size }
            return if(best!=null && best.value.size>=2 && best.value.size*2>=members.size)
                DnaRule(best.key,DnaProvenance.OBSERVED,best.value.map { it.sampleId })
            else DnaRule("Unknown; per-image observations do not establish a recurring drawing rule",DnaProvenance.UNKNOWN,emptyList())
        }
        val policy=measuredFamilyPolicy(members,images)
        val tiles=members.map { it.tileVote }.distinct(); val depths=members.map { it.depthVote }.distinct()
        val mapping=mapOf("background_tile" to "tile_geometry","geometry" to "logo_silhouette","stroke_fill" to "stroke_fill",
            "rounding" to "corners","simplification" to "simplification","contour" to "logo_silhouette","scale_padding" to "optical_scale",
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
