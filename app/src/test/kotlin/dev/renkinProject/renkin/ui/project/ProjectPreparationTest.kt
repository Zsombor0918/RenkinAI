package dev.renkinProject.renkin.ui.project

import dev.renkinProject.renkin.apk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test

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
}
