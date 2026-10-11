package dev.renkinProject.renkin.ui.project

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSessionTest {
    private class Backend : ProjectSessionBackend {
        override var activeProfileId = 1L
        override var ready = true
        override var canStart = true
        var dirty = false
        var failSave = false
        var failLoad = false
        var requireSaveUnderLock = false
        var loadGate: CompletableDeferred<Unit>? = null
        val events = mutableListOf<String>()
        val profiles = mutableSetOf(1L, 2L)
        var inserts = 0

        override fun hasUnsavedChanges() = dirty

        private fun prepare(leaving: Long, saveFirst: Boolean) {
            check(activeProfileId == leaving)
            if (!saveFirst && (dirty || requireSaveUnderLock)) throw ProjectSaveRequired()
            if (saveFirst) {
                events += "save:$activeProfileId"
                check(!failSave)
                dirty = false
                requireSaveUnderLock = false
            }
        }

        private suspend fun load(id: Long) {
            ready = false
            events += "load:$id"
            loadGate?.await()
            check(!failLoad)
            activeProfileId = id
            events += "baseline:$id"
            ready = true
        }

        override suspend fun open(request: ProjectRequest.Open, saveFirst: Boolean): Long {
            check(request.profileId in profiles)
            if (ready) prepare(request.leavingProfileId, saveFirst)
            load(request.profileId)
            return request.profileId
        }

        override suspend fun create(request: ProjectRequest.Create, saveFirst: Boolean, onCreated: (Long) -> Unit): Long {
            prepare(request.leavingProfileId, saveFirst)
            inserts++
            val id = 10L + inserts
            profiles += id
            events += "create:$id"
            onCreated(id)
            load(id)
            return id
        }
    }

    @Test
    fun activeEditorRoundTripNeverSwitchesSavesOrReloadsIcons() = runBlocking {
        val backend = Backend().apply { dirty = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        repeat(3) {
            assertTrue(session.open(1))
            assertEquals(ProjectSessionState.Opened(1), session.state)
            session.acknowledge() // Editor -> Home is UI-only.
        }
        assertTrue(backend.dirty)
        assertTrue(backend.events.isEmpty())
    }

    @Test
    fun dirtySwitchCancelDoesNothingThenSaveKeepsOriginBeforeLoadingTarget() = runBlocking {
        val backend = Backend().apply { dirty = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        assertTrue(session.open(2))
        assertTrue(session.state is ProjectSessionState.ConfirmSave)
        session.cancel()
        assertEquals(ProjectSessionState.Idle, session.state)
        assertTrue(backend.events.isEmpty())
        session.open(2)
        session.saveAndContinue()
        yield()
        assertEquals(listOf("save:1", "load:2", "baseline:2"), backend.events)
        assertEquals(ProjectSessionState.Opened(2), session.state)
    }

    @Test
    fun cleanSwitchWaitsForActualBaselineCompletion() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val backend = Backend().apply { loadGate = gate }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        session.open(2)
        yield()
        assertEquals(ProjectSessionState.Loading, session.state)
        assertEquals(1L, backend.activeProfileId)
        assertFalse(session.open(1))
        gate.complete(Unit)
        yield()
        assertEquals(ProjectSessionState.Opened(2), session.state)
        assertEquals(listOf("load:2", "baseline:2"), backend.events)
    }

    @Test
    fun createDoubleTapAndReobservingStateAllocateExactlyOnce() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val backend = Backend().apply { loadGate = gate }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        assertTrue(session.create("Night", "", "Night pack"))
        assertFalse(session.create("Night", "", "Night pack"))
        yield()
        repeat(5) { assertEquals(ProjectSessionState.Loading, session.state) }
        assertFalse(session.create("Night", "", "Night pack"))
        assertEquals(1, backend.inserts)
        gate.complete(Unit)
        yield()
        assertEquals(ProjectSessionState.Opened(11), session.state)
        assertFalse(session.create("Night", "", "Night pack"))
    }

    @Test
    fun cancelCreateAndSaveFailureNeverInsertAnOrphanProfile() = runBlocking {
        val backend = Backend().apply { dirty = true; failSave = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        session.create("Night", "", "Night")
        session.cancel()
        assertEquals(0, backend.inserts)
        session.create("Night", "", "Night")
        session.saveAndContinue()
        yield()
        assertTrue(session.state is ProjectSessionState.Failed)
        assertEquals(0, backend.inserts)
        assertEquals(1L, backend.activeProfileId)
    }

    @Test
    fun changedOriginAfterConfirmationFailsBeforeAnySaveOrInsert() = runBlocking {
        val backend = Backend().apply { dirty = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        session.create("Night", "", "Night")
        backend.activeProfileId = 2 // A watch deep-link acquired the shared session first.
        session.saveAndContinue()
        yield()
        assertTrue(session.state is ProjectSessionState.Failed)
        assertTrue(backend.events.isEmpty())
        assertEquals(0, backend.inserts)
    }

    @Test
    fun newlyQueuedDirtyEditReturnsToConfirmationInsteadOfDiscardingIt() = runBlocking {
        val backend = Backend().apply { requireSaveUnderLock = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        session.open(2)
        yield()
        assertTrue(session.state is ProjectSessionState.ConfirmSave)
        assertTrue(backend.events.isEmpty())
        session.saveAndContinue()
        yield()
        assertEquals(ProjectSessionState.Opened(2), session.state)
    }

    @Test
    fun failedCreateLoadRetainsDiscoverableProfileWithoutReplayingInsert() = runBlocking {
        val backend = Backend().apply { failLoad = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        session.create("Night", "", "Night")
        yield()
        assertEquals(ProjectSessionState.Failed(11), session.state)
        assertTrue(11L in backend.profiles)
        assertEquals(1, backend.inserts)
        // After restarting/loading the persisted session, continue the existing ID.
        backend.activeProfileId = 11
        backend.failLoad = false
        session.open(11)
        yield()
        assertEquals(ProjectSessionState.Opened(11), session.state)
        assertEquals(1, backend.inserts)
    }

    @Test
    fun processRestorationReportsInterruptionAndNeverExecutesAcceptedCreateAgain() = runBlocking {
        val saved = SavedStateHandle()
        val backend = Backend().apply { loadGate = CompletableDeferred() }
        val job = Job()
        val scope = CoroutineScope(coroutineContext + job)
        val original = ProjectSessionController(scope, saved, backend)
        original.create("Night", "", "Night")
        yield()
        job.cancelAndJoin()
        val restored = ProjectSessionController(this, saved, backend)
        assertEquals(ProjectSessionState.Interrupted(11), restored.state)
        yield()
        assertEquals(1, backend.inserts)
        assertTrue(11L in backend.profiles)
    }

    @Test
    fun failedSwitchAndUnavailableCatalogueNeverReportOpenedEditor() = runBlocking {
        val backend = Backend().apply { canStart = false }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        assertFalse(session.open(2))
        assertFalse(session.create("Night", "", "Night"))
        backend.canStart = true
        backend.failLoad = true
        session.open(2)
        yield()
        assertTrue(session.state is ProjectSessionState.Failed)
        assertFalse(backend.ready)
        assertFalse(session.open(2)) // Only the active session may be recovered first.
    }

    @Test
    fun invalidCreationFieldsAndRepeatedConfirmationDoNotInsertTwice() = runBlocking {
        val backend = Backend().apply { dirty = true }
        val session = ProjectSessionController(this, SavedStateHandle(), backend)
        assertFalse(session.create(" ", "", "Pack"))
        assertFalse(session.create("Night", "", " "))
        session.create("Night", "", "Night")
        session.saveAndContinue()
        session.saveAndContinue()
        yield()
        assertEquals(1, backend.inserts)
        assertEquals(listOf("save:1", "create:11", "load:11", "baseline:11"), backend.events)
    }
}
