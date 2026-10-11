package dev.renkinProject.renkin.ui.project

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
