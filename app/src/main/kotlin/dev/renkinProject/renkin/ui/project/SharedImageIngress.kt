package dev.renkinProject.renkin.ui.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle

/** Persist the request even when process death precedes the shell's consuming effect. */
internal class EditorEntryRequests(private val savedState: SavedStateHandle) {
    var version by mutableStateOf(savedState.get<Long>(VERSION) ?: 0L)
        private set

    fun request() {
        version++
        savedState[VERSION] = version
    }

    private companion object { const val VERSION = "projectEditorEntryVersion" }
}

/** Primitive Activity-saved ingress state; importing remains in UploadedImageStore. */
internal class SharedImageIngress(var consumedUri: String? = null) {
    fun accept(uri: String): Boolean {
        if (consumedUri == uri) return false
        consumedUri = uri
        return true
    }

    /** A separately delivered SEND is a new user action, even for the same URI. */
    fun onNewIntent() { consumedUri = null }
}
