package dev.renkinProject.renkin.ui.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal sealed interface ProjectRequest {
    val leavingProfileId: Long

    data class Open(val profileId: Long, override val leavingProfileId: Long) : ProjectRequest
    data class Create(
        val name: String,
        val description: String,
        val packLabel: String,
        override val leavingProfileId: Long
    ) : ProjectRequest
}

internal sealed interface ProjectSessionState {
    data object Idle : ProjectSessionState
    data class ConfirmSave(val request: ProjectRequest) : ProjectSessionState
    data object Loading : ProjectSessionState
    data class Opened(val profileId: Long) : ProjectSessionState
    data class Failed(val createdProfileId: Long? = null) : ProjectSessionState
    data class Interrupted(val createdProfileId: Long? = null) : ProjectSessionState
}

/** The only implementation in production delegates to the existing MainViewModel session. */
internal interface ProjectSessionBackend {
    val activeProfileId: Long
    val ready: Boolean
    val canStart: Boolean
    fun hasUnsavedChanges(): Boolean
    suspend fun open(request: ProjectRequest.Open, saveFirst: Boolean): Long
    suspend fun create(request: ProjectRequest.Create, saveFirst: Boolean, onCreated: (Long) -> Unit): Long
}

/** A dirty check must be repeated under MainViewModel's mutex, after queued edits finish. */
internal class ProjectSaveRequired : Exception()

/**
 * Transient orchestration, owned by the Activity's MainViewModel, never an icon/profile store.
 * Acceptance is synchronous, so taps cannot enqueue two inserts. Configuration recreation
 * keeps this instance/job. After process restoration an accepted request is reported, never
 * replayed: a committed profile is discoverable in the ordinary Room-backed project list.
 */
internal class ProjectSessionController(
    private val scope: CoroutineScope,
    private val savedState: SavedStateHandle,
    private val backend: ProjectSessionBackend
) {
    var state: ProjectSessionState by mutableStateOf(
        if (savedState.get<Boolean>(IN_FLIGHT) == true) {
            ProjectSessionState.Interrupted(savedState[CREATED_ID])
        } else ProjectSessionState.Idle
    )
        private set

    private val accepting: Boolean
        get() = state is ProjectSessionState.Idle || state is ProjectSessionState.Failed ||
            state is ProjectSessionState.Interrupted

    fun open(profileId: Long): Boolean {
        if (!accepting || !backend.canStart) return false
        // Recovery of a failed load may reopen its active profile. Other switches must wait
        // for a trustworthy baseline, so they cannot discard a partially loaded session.
        if (!backend.ready) {
            if (profileId != backend.activeProfileId) return false
            start(ProjectRequest.Open(profileId, backend.activeProfileId), saveFirst = false)
            return true
        }
        if (backend.ready && profileId == backend.activeProfileId) {
            state = ProjectSessionState.Opened(profileId)
            return true // No save, switch, or baseline reset for the active editor.
        }
        return submit(ProjectRequest.Open(profileId, backend.activeProfileId))
    }

    fun create(name: String, description: String, packLabel: String): Boolean {
        if (!accepting || !backend.canStart || !backend.ready) return false
        if (name.isBlank() || packLabel.isBlank()) return false
        return submit(ProjectRequest.Create(name.trim(), description.trim(), packLabel.trim(), backend.activeProfileId))
    }

    private fun submit(request: ProjectRequest): Boolean {
        if (backend.hasUnsavedChanges()) state = ProjectSessionState.ConfirmSave(request)
        else start(request, saveFirst = false)
        return true
    }

    fun cancel() {
        if (state is ProjectSessionState.ConfirmSave) state = ProjectSessionState.Idle
    }

    fun saveAndContinue() {
        val request = (state as? ProjectSessionState.ConfirmSave)?.request ?: return
        start(request, saveFirst = true)
    }

    fun acknowledge() {
        if (state is ProjectSessionState.Loading || state is ProjectSessionState.ConfirmSave) return
        savedState[IN_FLIGHT] = false
        savedState.remove<Long>(CREATED_ID)
        state = ProjectSessionState.Idle
    }

    private fun start(request: ProjectRequest, saveFirst: Boolean) {
        state = ProjectSessionState.Loading
        savedState[IN_FLIGHT] = true
        savedState.remove<Long>(CREATED_ID)
        scope.launch {
            try {
                val id = when (request) {
                    is ProjectRequest.Open -> backend.open(request, saveFirst)
                    is ProjectRequest.Create -> backend.create(request, saveFirst) { savedState[CREATED_ID] = it }
                }
                check(backend.ready && backend.activeProfileId == id)
                state = ProjectSessionState.Opened(id)
            } catch (error: ProjectSaveRequired) {
                state = ProjectSessionState.ConfirmSave(request)
            } catch (error: CancellationException) {
                state = ProjectSessionState.Interrupted(savedState[CREATED_ID])
                throw error
            } catch (error: Exception) {
                state = ProjectSessionState.Failed(savedState[CREATED_ID])
            } finally {
                // Do not lose the interruption marker when the scope is destroyed.
                if (state !is ProjectSessionState.Interrupted) savedState[IN_FLIGHT] = false
            }
        }
    }

    private companion object {
        const val IN_FLIGHT = "projectRequestInFlight"
        const val CREATED_ID = "projectRequestCreatedProfileId"
    }
}
