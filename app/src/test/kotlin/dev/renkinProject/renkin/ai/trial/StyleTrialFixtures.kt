package dev.renkinProject.renkin.ai.trial

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.room.Room
import dev.renkinProject.renkin.ai.*
import dev.renkinProject.renkin.apk.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*
import dev.renkinProject.renkin.ui.project.projectCatalogStamp
import dev.renkinProject.renkin.ui.project.projectSourceStamp
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.File

internal fun trialTestContext(root: File): Context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
    override fun getFilesDir() = File(root, "files").apply { mkdirs() }
    override fun getNoBackupFilesDir() = File(root, "noBackup").apply { mkdirs() }
}
internal class StyleTrialFixture(root: File) : AutoCloseable {
    val context = trialTestContext(root)
    val db = Room.inMemoryDatabaseBuilder(context, RenkinPackDatabase::class.java).build()
    val repo = ProjectAiRepository(db, ProjectAssetStore(context))
    val journal = StyleTrialJournal(context)
    var targets = listOf(CoverageTarget("a/Main", "Alpha"), CoverageTarget("b/Main", "Beta"), CoverageTarget("c/Main", "Gamma"))
    val originals = mutableMapOf<String, TrialOriginal>().apply { targets.forEachIndexed { i, t ->
        put(t.key, TrialOriginal(projectTestPng(Color.rgb(80 + 30 * i, 60, 140), 1024), "launcher:${t.key}:1"))
    } }
    var index = CoverageIndex(CoverageStamp("source.pack", 1, 2, 3, false), CoverageIndexState.READY)
    var sourceCurrent = true
    val reader get() = object : CoverageSourceReader {
        override val index = this@StyleTrialFixture.index
        override fun isCurrent() = sourceCurrent
        override suspend fun canRender(drawableName: String) = drawableName != "broken"
    }
    suspend fun initialize(mode: String = "CREATE", refs: Int = 0) {
        RenkinPackRepository(db).replaceEverything(listOf(Profile(1, "Project"), Profile(2, "Other")), emptyList())
        val hashes = (0 until refs).map { repo.importReference(1, ByteArrayInputStream(projectTestPng(Color.rgb(20 + 30 * it, 80, 40)))).hash }
        repo.saveStyle(1, null, "minimal outline", "keep separate instruction", hashes, mode,
            if (mode == "EXTEND") "source.pack" else null, if (mode == "EXTEND") projectSourceStamp(index) else null)
        repo.saveSelection(1, 1, ProjectSelection(mode, if (mode == "EXTEND") "source.pack" else null,
            if (mode == "EXTEND") projectSourceStamp(index) else null, projectCatalogStamp(targets), targets.map { it.key }))
    }
    suspend fun plan(keys: List<String> = targets.map { it.key }, provider: String = AiSettings.OPENAI): StyleTrialPlan {
        val style = repo.load(1); val meta = checkNotNull(style.metadata); val rev = checkNotNull(style.revision)
        val context = TrialContext(1, rev.revision, meta.version, meta.mode, meta.sourcePackage, meta.sourceStamp,
            projectCatalogStamp(targets), sha256(meta.selectionJson.toByteArray()), referenceFingerprint(style.references.map { it.hash }),
            journal.epoch(1), provider, AiSettings.MODELS.first(), "medium")
        return StyleTrialPlan(context, rev.specification, rev.userInstruction, style.references.map { it.hash }, keys.map { key ->
            val o = originals.getValue(key); TrialInput(key, targets.single { it.key == key }.name, o.hash, o.origin)
        })
    }
    suspend fun images(plan: StyleTrialPlan) = plan.inputs.associate { it.originalHash to originals.getValue(it.key).png } +
        plan.referenceHashes.associateWith { repo.referenceBytes(1, it) }
    fun successful(observe: (IconGenerationRequest) -> Unit = {}) = IconGenerationGateway { request ->
        observe(request); request.beforeEndpoint(1)
        if (request.provider == AiSettings.STABILITY) request.beforeEndpoint(2)
        IconGenerationOutcome.Generated(projectTestPng(Color.WHITE, 1024), request.requestId, request.provider, 1024, 1024,
            if (request.provider == AiSettings.STABILITY) 2 else 1)
    }
    override fun close() { db.close() }
}
