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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class ProjectPreparationTest {
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
