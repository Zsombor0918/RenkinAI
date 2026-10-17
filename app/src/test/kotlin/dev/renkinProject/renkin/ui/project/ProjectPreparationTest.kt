package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.apk.*
import android.app.Application
import dev.renkinProject.renkin.data.project.ProjectSelection
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import dev.renkinProject.renkin.ai.style.*
import dev.renkinProject.renkin.ai.trial.*
import dev.renkinProject.renkin.data.*
import dev.renkinProject.renkin.data.project.*

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectPreparationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun localPackReferencesKeepChosenBytesOrderAndOwnershipWithoutAnyProviderOrKey() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            RenkinPackRepository(f.db).replaceEverything(listOf(Profile(1, "Project"), Profile(2, "Other")), emptyList())
            val artwork = FixtureReferenceReader((0 until 12).associate { "draw$it" to analysisFixturePng(it % 3, it) })
            val owner = object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T): T { check(id == 1L); return operation() }
                override suspend fun catalogStamp() = error("Sampling must not depend on targets")
                override suspend fun reader(source: String, referenceIndex: Boolean) = artwork
            }
            val local = LocalStyleReferences(f.repo, ReferenceSampler(), owner)
            val stamp = projectSourceStamp(artwork.index)
            val candidates = local.sample(1, "fixture.pack", stamp)
            assertTrue(candidates.samples.samples.size in 2..12)
            val chosen = listOf(candidates.samples.samples.last(), candidates.samples.samples.first())
            val assets = chosen.map { local.select(1, candidates, it.hash) }
            val draft = ProjectStyleDraft("Match the reviewed visual references; preserve the target identity.", "", assets.map { it.hash },
                localReferenceOrigin = LocalReferenceOrigin.parse(candidates.origin.encode()))
            val saved = local.save(1, null, draft, ProjectSetupMode.EXTEND, "fixture.pack", stamp)
            assertEquals(chosen.map { it.hash }, saved.references.map { it.hash })
            chosen.forEach { assertArrayEquals(it.png, f.repo.referenceBytes(1, it.hash)) }
            assertEquals(saved, f.repo.load(1))
            assertTrue(f.repo.load(2).references.isEmpty())
            assertTrue(runCatching { local.select(2, candidates, chosen[0].hash) }.isFailure)
            assertTrue(f.journal.records(1).isEmpty())
        }
    }

    @Test fun localReferenceSelectionAndSaveRejectChangedSourceMissingResourceAndWrongRoute() = runBlocking {
        StyleTrialFixture(temporary.root).use { f ->
            RenkinPackRepository(f.db).replaceEverything(listOf(Profile(1, "Project")), emptyList())
            val artwork = FixtureReferenceReader(mapOf("draw" to analysisFixturePng(0, 1)))
            var missing = false
            val owner = object : AnalysisSessionOwner {
                override suspend fun <T> owned(id: Long, operation: suspend () -> T) = operation()
                override suspend fun catalogStamp() = error("No target read")
                override suspend fun reader(source: String, referenceIndex: Boolean): CoverageSourceReader =
                    object : CoverageSourceReader by artwork {
                        override suspend fun inspectReference(drawableName: String): ReferenceRead =
                            if (missing) ReferenceRead.Failed(ReferenceFailure.MISSING_DRAWABLE) else artwork.inspectReference(drawableName)
                    }
            }
            val local = LocalStyleReferences(f.repo, ReferenceSampler(), owner)
            val stamp = projectSourceStamp(artwork.index)
            val candidates = local.sample(1, "fixture.pack", stamp)
            val hash = candidates.samples.samples.single().hash
            val asset = local.select(1, candidates, hash)
            val draft = ProjectStyleDraft("Reviewed", "", listOf(asset.hash), localReferenceOrigin = candidates.origin)
            missing = true
            assertTrue(runCatching { local.select(1, candidates, hash) }.isFailure)
            assertTrue(runCatching { local.save(1, null, draft, ProjectSetupMode.EXTEND, "fixture.pack", stamp) }.isFailure)
            missing = false; artwork.current = false
            assertTrue(runCatching { local.save(1, null, draft, ProjectSetupMode.EXTEND, "fixture.pack", stamp) }.isFailure)
            artwork.current = true
            assertTrue(runCatching { local.save(1, null, draft, ProjectSetupMode.CREATE, null, null) }.isFailure)
            assertTrue(runCatching { local.save(2, null, draft, ProjectSetupMode.EXTEND, "fixture.pack", stamp) }.isFailure)
            assertNull(f.repo.load(1).revision)
            assertTrue(f.journal.records(1).isEmpty())
        }
    }

    @Test fun queuedWriteRechecksOwnerAfterProfileSwitchAndNeverRunsForTheWrongProfile() = runBlocking {
        val mutex = Mutex(locked = true)
        var active = 1L
        var writes = 0
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { withProjectOwner(mutex, 1, { active }, { true }) { writes++; "saved" } }
        }
        active = 2
        mutex.unlock()
        assertTrue(pending.await().isFailure)
        assertEquals(0, writes)
        assertEquals("saved 2", withProjectOwner(mutex, 2, { active }, { true }) { writes++; "saved 2" })
        assertEquals(1, writes)
    }

    @Test fun incompleteProfileBaselineBlocksMetadataSave() = runBlocking {
        var writes = 0
        val result = runCatching { withProjectOwner(Mutex(), 1, { 1 }, { false }) { writes++ } }
        assertTrue(result.isFailure)
        assertEquals(0, writes)
    }

    @Test fun liveLauncherInventoryRemovesUninstalledKeysAndDisablesUnloadedOrUncertainKeys() {
        val loaded = listOf(CoverageTarget("keep/Main", "Kept", LocalIconState.CUSTOM_OR_MANUAL),
            CoverageTarget("removed/Main", "Removed"), CoverageTarget("work/Main", "Work"))
        val current = linkedMapOf("keep/Main" to false, "added/Main" to false, "work/Main" to true)
        val verified = revalidateLauncherTargets(loaded, current)
        assertEquals(current.keys.toList(), verified.map { it.key })
        assertTrue(verified.first().selectable)
        assertEquals(LocalIconState.CUSTOM_OR_MANUAL, verified.first().local)
        assertEquals(LocalIconState.UNKNOWN, verified[1].local)
        assertFalse(verified[1].selectable)
        assertTrue(verified[2].ambiguousUser)
        assertFalse(verified[2].selectable)
    }

    private fun reader(index: CoverageIndex) = object : CoverageSourceReader {
        override val index = index
        override fun isCurrent() = true
        override suspend fun canRender(drawableName: String): Boolean = error("Saving preparation must not render or generate icons")
    }

    @Test fun explicitExtendSaveReadsFreshIndexAfterRescanInsteadOfUnavailableCache() = runBlocking {
        val targets = listOf(CoverageTarget("pkg/Main", "App"))
        val fresh = CoverageIndex(CoverageStamp("pack.source", 2, 20, 1, false), CoverageIndexState.READY,
            mappings = mapOf("pkg/Main" to "updated_drawable"))
        val selection = ProjectSelection("EXTEND", "pack.source", projectSourceStamp(fresh), projectCatalogStamp(targets), listOf("pkg/Main"))
        var reads = 0
        verifyProjectSelectionContext(selection, targets) { pack, rescan ->
            assertEquals("pack.source", pack); assertTrue(rescan); reads++
            reader(if (rescan) fresh else fresh.copy(state = CoverageIndexState.UNAVAILABLE))
        }
        assertEquals(1, reads)
    }

    @Test fun changedSourceOrFreshLauncherCannotSilentlySaveThePreviousSelection() = runBlocking {
        val targets = listOf(CoverageTarget("pkg/Main", "App"))
        val index = CoverageIndex(CoverageStamp("pack.source", 1, 10, 1, false), CoverageIndexState.READY)
        val selection = ProjectSelection("EXTEND", "pack.source", projectSourceStamp(index), projectCatalogStamp(targets), listOf("pkg/Main"))
        assertTrue(runCatching { verifyProjectSelectionContext(selection, targets) { _, _ ->
            reader(index.copy(stamp = index.stamp.copy(versionCode = 2)))
        } }.isFailure)
        var reads = 0
        assertTrue(runCatching { verifyProjectSelectionContext(selection,
            revalidateLauncherTargets(targets, mapOf("new/Main" to false))) { _, _ -> reads++; reader(index) }
        }.isFailure)
        assertEquals(0, reads)
    }
}
