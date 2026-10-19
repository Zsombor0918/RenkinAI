package dev.renkinProject.renkin.ai.style

import android.app.Application
import android.graphics.*
import dev.renkinProject.renkin.data.project.sha256
import kotlinx.coroutines.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

internal fun dnaFixturePlan(mode: DnaAnalysisMode = DnaAnalysisMode.STANDARD,count: Int = 24): Pair<StyleDnaPlan,Map<String,ByteArray>> {
    val images=(0 until count).map { analysisFixturePng(2,it) }.associateBy(::sha256)
    require(images.size==count)
    val old=analysisFixtureRequest(1).context
    val plan=StyleDnaPlan(old,mode,images.keys.mapIndexed { i,h->DnaSampleBinding("s%02d".format(i+1),h) },
        ReferenceSampleReport(count,count,0,0,0,false,count<8),"drawing grammar","keep original identity",sha256("draft".toByteArray()))
    return plan to images
}
internal fun dnaFixtureReports(plan: StyleDnaPlan,ids: List<String> = plan.samples.map { it.id })=ids.map { id->
    DnaIconReport.parse(dnaReportFixture(id,plan.bindings.getValue(id)))
}
internal fun dnaReportsBody(reports: List<DnaIconReport>)=canonicalJson(json("schema" to 1,"reports" to JSONArray(reports.map { it.json() })))

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleDnaWorkflowTest {
    @Test fun oldAndNewPromptJournalsKeepTheirVersionAndConsumedSteps() = runBlocking {
        for (version in DNA_PROMPT_VERSIONS) {
            val (fresh, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
            val plan = fresh.copy(promptVersion = version)
            val journal = StyleDnaJournal(analysisFixtureContext(temporary.newFolder()))
            val reports = dnaFixtureReports(plan)
            assertTrue(journal.claim(plan, images))
            val step = plan.steps.single()
            journal.submitted(plan, step)
            journal.completed(plan, step, HistoricalDnaReceipt("resp_archived"), reports)
            journal.result(plan, SavedStyleDna(synthesizeDnaLocally(reports, images), reports, plan.model,
                plan.detail, plan.algorithm, "family1", version))
            journal.release(plan)
            val record = checkNotNull(journal.record(1, plan.id))
            val bytes = record.encode()
            assertEquals(record, StyleDnaRecord.parse(bytes))
            assertEquals(version, record.plan.promptVersion)
            assertEquals(bytes, journal.record(1, plan.id)!!.encode())
            assertFalse(journal.claim(plan, images))
        }
    }

    @Test fun overallRecipeUsesConcreteGenerationDecisionsAndKeepsRawEvidence() {
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        val values = mapOf("tile_geometry" to "Standalone dark silhouette forms; no separate background tile",
            "logo_silhouette" to "Compact geometric masses with soft external contours",
            "stroke_fill" to "Solid fills without an outline", "corners" to "Softened corners",
            "optical_scale" to "High optical fill with a narrow clear outer margin; qualitative, not measured",
            "negative_space" to "Keep open counters and distinct internal gaps",
            "shadow_gradient" to "Subtle gradients and soft highlights; shallow depth, no heavy extrusion",
            "luminance" to "Dark neutral base with vivid accents", "color_roles" to "Dark base plus vivid native brand accents, not monochrome",
            "wordmark_handling" to "Retain text only when essential to target brand identity",
            "simplification" to "Reduce small detail to a few bold geometric parts")
        val reports = dnaFixtureReports(plan).map { report -> report.copy(
            traits = DNA_TRAITS.associateWith { DnaObservation(values.getValue(it), DnaProvenance.OBSERVED) },
            tileVote = DnaTile.NONE, depthVote = DnaDepth.GRADIENT, colorPolicy = PackColorPolicy.ADAPT_BRAND_COLORS) }
        val raw = synthesizeDnaLocally(reports, images)
        val saved = SavedStyleDna(raw, reports, plan.model, plan.detail, plan.algorithm, DNA_PACK_STYLE)
        val before = saved.encode()
        val recipe = StyleDnaCompiler.recipe(saved, DnaSymbolType.WORDMARK)
        for (label in listOf("Background mode:", "Dominant shape / silhouette:", "Optical fill / padding:",
            "Edges / corners:", "Stroke / outline:", "Simplification / complexity ceiling:", "Color policy:",
            "Gradient / highlight / depth:", "Text policy:", "Negative space:", "Brand-preservation rule:",
            "Abstraction rule:", "Pack consistency:")) assertTrue(label + " missing: " + recipe, recipe.contains(label))
        for (value in listOf("standalone dark silhouette", "compact geometric masses", "vivid native brand accents",
            "high optical fill", "subtle gradients", "essential to target brand identity")) assertTrue(value, recipe.lowercase().contains(value))
        assertFalse(recipe.contains("UNKNOWN:")); assertFalse(recipe.contains("Not established"))
        assertEquals(before, saved.encode()); assertEquals(saved, SavedStyleDna.parse(before))
        assertEquals(reports, saved.reports); assertEquals(raw, saved.dna)
        assertTrue(recipe.length < 4000)
        assertNull(abstractDnaSilhouette("FOREIGN_SOURCE_LOGO"))
        assertNull(abstractDnaSilhouette("not compact geometric masses with soft external contours"))
        assertNull(abstractDnaSilhouette("compact geometric masses with soft external contours of a reference brand"))
    }
    @Test fun overallUsesSupportedSynthesisRulesWithoutTurningConflictsIntoUniversalRules() {
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.STANDARD, 12)
        val reports = dnaFixtureReports(plan).mapIndexed { index, report -> report.copy(traits = report.traits +
            ("logo_silhouette" to DnaObservation("Distinct contour observation number $index", DnaProvenance.OBSERVED))) }
        val local = synthesizeDnaLocally(reports, images)
        val rule = DnaRule("Standalone dark silhouette forms with compact geometric masses", DnaProvenance.INFERRED, reports.map { it.sampleId })
        val family = local.families.single().copy(drawing = local.families.single().drawing + ("geometry" to rule))
        val saved = SavedStyleDna(local.copy(families = listOf(family)), reports, plan.model, plan.detail, plan.algorithm, DNA_PACK_STYLE)
        val overall = packDnaFamily(saved)
        assertEquals(rule.value.lowercase(), overall.drawing.getValue("geometry").value.lowercase())
        assertEquals(rule.evidenceIds, overall.drawing.getValue("geometry").evidenceIds)
        val halves = reports.chunked(6).mapIndexed { index, members -> family.copy(id="part$index", evidenceIds=members.map { it.sampleId },
            colorRoles=emptyList(), drawing=family.drawing.mapValues { (_, value) -> value.copy(evidenceIds=members.map { it.sampleId }) } +
                ("geometry" to rule.copy(value=if(index==0) "Rounded simple masses" else "Angular complex contours", evidenceIds=members.map { it.sampleId })),
            must=emptyList(),should=emptyList(),mustNot=emptyList()) }
        val conflict = saved.copy(dna=local.copy(families=halves,recommendedFamily=halves.first().id))
        assertEquals(DnaProvenance.UNKNOWN, packDnaFamily(conflict).drawing.getValue("geometry").provenance)
        val sparse = saved.copy(reports=reports.map { it.copy(traits=it.traits.mapValues { (_, value) -> value.copy(provenance=DnaProvenance.UNKNOWN) }) },
            dna=local.copy(families=listOf(family.copy(drawing=family.drawing.mapValues { (_, value) -> value.copy(provenance=DnaProvenance.UNKNOWN,evidenceIds=emptyList()) }))))
        val sparseRecipe=StyleDnaCompiler.recipe(sparse,DnaSymbolType.UNKNOWN)
        assertTrue(sparseRecipe.contains("Unestablished traits:")); assertFalse(sparseRecipe.contains("UNKNOWN:"))
    }
    @Test fun representativeReferencesPreferUnflaggedEvidenceAndNeverPadAnotherFamily() {
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        val reports = dnaFixtureReports(plan).mapIndexed { index, report -> report.copy(
            familyVote=if(index<6) DnaFamilyVote.OUTLINE else DnaFamilyVote.SOLID,
            outlierReason=if(index==0) "Atypical construction for this sample" else "") }
        val raw=synthesizeDnaLocally(reports,images)
        val saved=SavedStyleDna(raw,reports,plan.model,plan.detail,plan.algorithm,DNA_PACK_STYLE)
        val available=plan.samples.map { it.hash }
        val refs=dnaReferenceHashes(saved,DNA_PACK_STYLE,available)
        assertEquals(5,refs.size); assertFalse(refs.contains(reports.first().sampleHash))
        assertTrue(refs.contains(reports[6].sampleHash)); assertNotEquals(available.take(5),refs)
        val family=raw.families.first()
        val familyRefs=dnaReferenceHashes(saved,family.id,available)
        assertTrue(familyRefs.all { hash -> reports.single { it.sampleHash==hash }.sampleId in family.evidenceIds })
        assertEquals(refs,dnaReferenceHashes(saved,DNA_PACK_STYLE,available))
    }
    @Test fun packWideDerivedReviewNormalizesSynonymsWithoutRewritingRawFamiliesOrIdentity() {
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 12)
        val reports = dnaFixtureReports(plan).mapIndexed { index, report -> report.copy(
            familyVote = DnaFamilyVote.entries[index % 7],
            traits = report.traits + mapOf(
                "stroke_fill" to DnaObservation(listOf("solid fill with smooth edges, no outline", "solid filled shapes with no visible stroke", "solid fills with no outline")[index % 3], DnaProvenance.OBSERVED),
                "corners" to DnaObservation(listOf("fully rounded ends", "rounded terminals and softened corners")[index % 2], DnaProvenance.OBSERVED),
                "logo_silhouette" to DnaObservation("PRIVATE_SOURCE_IDENTITY", DnaProvenance.OBSERVED))) }
        val raw = synthesizeDnaLocally(reports, images)
        val historical = SavedStyleDna(raw, reports, plan.model, plan.detail, plan.algorithm, raw.recommendedFamily)
        val bytes = historical.encode()
        val global = historical.copy(selectedFamily = "__pack__")
        assertEquals(global, SavedStyleDna.parse(global.encode()))
        val recipe = StyleDnaCompiler.specification(global)
        assertTrue(recipe, recipe.contains("solid filled shapes without an outline"))
        assertTrue(recipe, recipe.contains("rounded terminals and softened corners"))
        assertFalse(recipe.contains("PRIVATE_SOURCE_IDENTITY"))
        assertEquals(bytes, SavedStyleDna.parse(bytes).encode())
        assertEquals(raw, global.dna); assertEquals(reports, global.reports)
        assertNotEquals("rounded terminals and softened corners", normalizedDnaObservation("corners","no rounded terminals"))
        assertNotEquals("solid filled shapes without an outline", normalizedDnaObservation("stroke_fill","not solid fills with no outline"))
        assertNotEquals("rounded terminals and softened corners", normalizedDnaObservation("corners","rounded terminals except sharp corners"))
        assertTrue(StyleDnaCompiler.family(global).json().getJSONArray("evidence_ids").length() == reports.size)
        val selected = dnaReferenceHashes(global,DNA_PACK_STYLE,plan.samples.map { it.hash })
        assertEquals(5,selected.size); assertEquals(5,selected.distinct().size)
        assertEquals(selected,dnaReferenceHashes(global,DNA_PACK_STYLE,plan.samples.map { it.hash }))
        val small = raw.families.first { it.observedCount < 3 }
        val smallRefs = dnaReferenceHashes(global,small.id,plan.samples.map { it.hash })
        assertEquals(small.observedCount,smallRefs.size)
        assertTrue(dnaReferencesMatch(global,small.id,smallRefs))
        assertFalse(dnaReferencesMatch(global,small.id,plan.samples.take(5).map { it.hash }))
    }
    @get:Rule val temporary=TemporaryFolder()
    private fun journal()=StyleDnaJournal(analysisFixtureContext(temporary.root))

    @Test fun trustedParentAliasKeepsAtomicRecoveryAndConsumedReservations() {
        val canonical = journal()
        assertTrue(canonical.records(1).isEmpty())
        val (plan, images) = dnaFixturePlan(DnaAnalysisMode.QUICK, 2)
        assertTrue(canonical.claim(plan, images)); canonical.submitted(plan, plan.steps.first()); canonical.release(plan)
        val file = File(temporary.root, "style_dna/1/plan-${plan.id}.json")
        val durable = file.readBytes()
        assertTrue(file.renameTo(File(file.path + ".bak")))
        File(file.path + ".new").writeText("unfinished private bytes")
        val aliased = StyleDnaJournal(analysisFixtureContext(File(temporary.root, ".")))
        val recovered = aliased.records(1).single()
        assertEquals(DnaStatus.UNCERTAIN, recovered.status)
        assertEquals(1, recovered.submitted)
        assertEquals(plan, recovered.plan)
        assertFalse(aliased.claim(plan, images))
        assertFalse(canonical.claim(plan, images))
        assertNotNull(StyleDnaRecord.parse(String(durable)))
        assertEquals(recovered, canonical.latest(1))
        val orphan = plan.copy(id = java.util.UUID.randomUUID().toString(),
            samples = plan.samples) // New plan ID, still consumed step IDs.
        File(file.parentFile, "plan-${orphan.id}.json.new").writeText("interrupted")
        assertFalse(aliased.claim(orphan, images))
        assertEquals(recovered, aliased.latest(1))
    }

    @Test fun canonicalContextStillRejectsLinkedJournalAndProfileDirectories() {
        val outside = temporary.newFolder("outside")
        val root = File(temporary.root, "style_dna")
        java.nio.file.Files.createSymbolicLink(root.toPath(), outside.toPath())
        assertTrue(runCatching { journal().records(1) }.isFailure)
        java.nio.file.Files.delete(root.toPath()) // Only remove the fixture link, never its target.
        assertTrue(root.mkdir())
        java.nio.file.Files.createSymbolicLink(File(root, "1").toPath(), outside.toPath())
        assertTrue(runCatching { journal().records(1) }.isFailure)
        assertTrue(outside.listFiles()!!.isEmpty())
    }

    @Test fun safeRendererSeparatesDeliberateStatefulExclusionsFromDarkVisibleAndBrokenArt() {
        val resources=org.robolectric.RuntimeEnvironment.getApplication().resources
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(20,20,22));val paint=Paint().apply {color=Color.rgb(25,25,28)}
            Canvas(bitmap).drawCircle(128f,128f,65f,paint)
            val result=diagnoseReference(android.graphics.drawable.BitmapDrawable(resources,bitmap))
            assertTrue(result is ReferenceRead.Rendered);assertTrue(referenceFeatures((result as ReferenceRead.Rendered).png).occupancy>.9)
            assertEquals(ReferenceFailure.UNSUPPORTED_STATEFUL,(diagnoseReference(android.graphics.drawable.StateListDrawable()) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.UNSUPPORTED_DYNAMIC,(diagnoseReference(android.graphics.drawable.AnimationDrawable()) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.EMPTY_ALPHA,(diagnoseReference(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)) as ReferenceRead.Failed).reason)
            assertEquals(ReferenceFailure.NEAR_EMPTY_ALPHA,(diagnoseReference(android.graphics.drawable.ColorDrawable(Color.argb(10,255,255,255))) as ReferenceRead.Failed).reason)
            val broken=object:android.graphics.drawable.Drawable(){override fun draw(canvas:Canvas){throw IllegalStateException("private resource path")}
                override fun setAlpha(alpha:Int){};override fun setColorFilter(filter:ColorFilter?){};@Deprecated("Deprecated")override fun getOpacity()=PixelFormat.TRANSLUCENT}
            assertEquals(ReferenceFailure.RENDER_EXCEPTION,(diagnoseReference(broken) as ReferenceRead.Failed).reason)
        } finally {bitmap.recycle()}
    }
    @Test fun boundedSixHundredCandidateScanRetainsTwentyFourSeparateThumbnailsAndMinorityChecks()=runBlocking {
        val art=(0 until 54).associate {"art$it" to analysisFixturePng(it%3,it)}
        for(size in listOf(50,150,500,1500)) for(cap in listOf(300,600)) {
            val mapping=(0 until size).associate {"entry%04d/Main".format(it) to "art${it%54}"}
            val before=System.nanoTime();val runtime=Runtime.getRuntime();val heap=runtime.totalMemory()-runtime.freeMemory()
            val r=ReferenceSampler().sample(FixtureReferenceReader(art,mapping),cap,24)
            assertEquals(minOf(size,cap),r.report.examined);r.report.validate();assertTrue(r.samples.size<=24)
            assertTrue(r.samples.sumOf {it.png.size.toLong()}<=8L*1024*1024)
            println("DNA_SCAN mappings=$size cap=$cap examined=${r.report.examined} distinct=${r.report.valid} selected=${r.samples.size} elapsedMs=${(System.nanoTime()-before)/1000000} heapDeltaBytes=${runtime.totalMemory()-runtime.freeMemory()-heap} nativePeak=NOT_MEASURED")
        }
    }

    @Test fun cancellationAndColdRecoveryKeepUniqueRightsConsumedAcrossOwners()=runBlocking {
        val (plan,images)=dnaFixturePlan();val j=journal();assertTrue(j.claim(plan,images));j.submitted(plan,plan.steps.first());j.release(plan)
        val recovered=journal().latest(1)!!;assertEquals(DnaStatus.UNCERTAIN,recovered.status);assertEquals(1,recovered.submitted)
        assertFalse(journal().claim(plan,images))
        val foreign=plan.copy(context=plan.context.copy(profileId=2),id=java.util.UUID.randomUUID().toString())
        assertFalse(journal().claim(foreign,images))
        j.invalidateProfile(1);assertNull(journal().latest(1));assertFalse(journal().claim(plan,images))
    }
    @Test fun strictPerImageBindingAndEnumsRejectFabricatedUnknownPartialOrOverflowingObservations() {
        val (plan,_)=dnaFixturePlan(DnaAnalysisMode.QUICK,2);val original=dnaReportsBody(dnaFixtureReports(plan))
        val r=org.json.JSONObject(original);r.getJSONArray("reports").getJSONObject(0).put("sample_hash","f".repeat(64))
        assertTrue(runCatching {StyleDnaCodec.parseReports(r.toString(),plan.bindings)}.isFailure)
        for(change in listOf("BAD_FAMILY","null")) {val p=org.json.JSONObject(original);p.getJSONArray("reports").getJSONObject(0).put("family_vote",change)
            assertTrue(runCatching {StyleDnaCodec.parseReports(p.toString(),plan.bindings)}.isFailure) }
        val p=org.json.JSONObject(original);p.getJSONArray("reports").getJSONObject(0).getJSONObject("traits").getJSONObject("corners").put("value","x".repeat(241))
        assertTrue(runCatching {StyleDnaCodec.parseReports(p.toString(),plan.bindings)}.isFailure)
    }
    @Test fun uncertainPaletteCannotForceDarkTileOrGradientAndReferenceIdentityNeverEntersRecipe() {
        val (plan,images)=dnaFixturePlan(DnaAnalysisMode.QUICK,12)
        val reports=dnaFixtureReports(plan).map {it.copy(traits=it.traits+ ("logo_silhouette" to DnaObservation("FOREIGN_SOURCE_LOGO",DnaProvenance.OBSERVED))) }
        val dna=synthesizeDnaLocally(reports,images)
        val f=dna.families.first();assertEquals(PackColorPolicy.ADAPT_BRAND_COLORS,f.colorPolicy)
        assertEquals(DnaTile.NONE,f.tile);assertEquals(DnaDepth.NONE,f.depth)
        val recipe=StyleDnaCompiler.recipe(dna,DnaSymbolType.WORDMARK,f.id)
        assertFalse(recipe.contains("FOREIGN_SOURCE_LOGO"));assertTrue(recipe.contains("Preserve original target hues"));assertTrue(recipe.contains("wordmark") || recipe.contains("Target-type"))
    }
}
