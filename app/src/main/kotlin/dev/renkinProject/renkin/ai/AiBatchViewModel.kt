package dev.renkinProject.renkin.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.apk.ApplicationProvider
import dev.renkinProject.renkin.drawable.BitmapIconDrawable
import dev.renkinProject.renkin.drawable.toSafeBitmapOrNull
import dev.renkinProject.renkin.packages.PackageInfoStruct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AiBatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val provider: ApplicationProvider,
    private val settingsStore: AiSettingsStore,
    private val credentials: AiCredentialStore,
    private val prompts: AiPromptStore,
    private val service: AiIconGenerationService,
    private val store: AiBatchStore
) : ViewModel() {
    var settings by mutableStateOf<AiSettings?>(null); private set
    var session by mutableStateOf<AiBatchSession?>(null); private set
    var selected by mutableStateOf<Set<String>>(emptySet()); private set
    var instructions by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var busy by mutableStateOf(false); private set
    var loading by mutableStateOf(true); private set
    var pauseRequested by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var hasKey by mutableStateOf(false); private set
    var appliedKeys by mutableStateOf<Set<String>>(emptySet()); private set
    private var profileId: Long? = null
    private var runner: AiBatchRunner? = null
    private var collector: Job? = null
    private var task: Job? = null
    private var cancelRequested = false
    val candidates: List<PackageInfoStruct> get() = missingAiApps(provider.applicationList, provider.lockedIconKeys)
    fun open(profile: Long) {
        if (profileId == profile && !loading) { refreshSettings(); return }
        profileId = profile
        loading = true
        viewModelScope.launch {
            try {
                provider.ensureInitialized()
                refreshSettingsNow()
                instructions = provider.applicationList.associate { it.key to prompts.load("$profile:${it.key}") }
                val old = store.load(profile)
                if (old != null) {
                    appliedKeys = appliedKeys + old.items.filter { it.status == AiBatchStatus.SUCCEEDED }.map { it.key }
                    install(old)
                    runner!!.recoverInterrupted()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message?.take(400) ?: "Could not restore the batch." }
            finally { loading = false }
        }
    }
    private suspend fun refreshSettingsNow() {
        settings = settingsStore.load()
        hasKey = credentials.hasKey(settings!!.provider)
    }
    fun refreshSettings() { viewModelScope.launch { refreshSettingsNow() } }
    fun select(key: String, checked: Boolean) { if (!busy) selected = if (checked) selected + key else selected - key }
    fun selectAll(keys: Set<String>) { if (!busy) selected = keys }
    fun setInstruction(key: String, value: String) {
        if (busy || value.length > 2000) return
        instructions = instructions + (key to value)
        val profile = profileId ?: return
        val batch = runner
        if (batch != null) loading = true
        viewModelScope.launch {
            try { prompts.save("$profile:$key", value); batch?.updateInstruction(key, value) }
            catch (e: Exception) { error = e.message?.take(400) ?: "Could not save the app instruction." }
            finally { if (batch != null) loading = false }
        }
    }
    private fun status(key: String): AiBatchTarget {
        if (provider.isProfileSwitching || provider.activeProfileId != profileId) return AiBatchTarget.PROFILE_CHANGED
        val app = provider.applicationList.firstOrNull { it.key == key } ?: return AiBatchTarget.UNAVAILABLE
        return if (key in provider.lockedIconKeys) AiBatchTarget.UNAVAILABLE
            else if (app.createdIcon != null) AiBatchTarget.FILLED else AiBatchTarget.MISSING
    }
    private fun connected(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    private fun source(key: String): Bitmap {
        val app = provider.applicationList.firstOrNull { it.key == key } ?: error("The app is no longer installed.")
        val original = synchronized(app.icon) { app.icon.toSafeBitmapOrNull(512, 512) }
            ?: error("The original app icon could not be read.")
        return original.copy(Bitmap.Config.ARGB_8888, false) ?: error("Could not copy source icon.")
    }
    private fun install(value: AiBatchSession) {
        session = value
        runner = AiBatchRunner(value, store::save, { status(it) }, { item ->
            val image = source(item.key)
            try {
                service.matchingPrevious("${value.profileId}:${item.key}", value.settings.copy(appInstruction = item.instruction), image)
                    ?.let { result -> result.bitmap.recycle(); result.file.name }
            } finally { image.recycle() }
        }, { item ->
            check(status(item.key) == AiBatchTarget.MISSING) { "Target or profile changed before request." }
            val source = source(item.key)
            try {
                val result = service.generate("${value.profileId}:${item.key}", source,
                    value.settings.copy(appInstruction = item.instruction))
                result.bitmap.recycle() // PNG is durable; apply decodes only this item.
                result.file.name
            } finally { source.recycle() }
        }, { item ->
            val result = service.savedResult(item.fileName ?: error("No saved PNG."))
            try {
                val outcome = provider.applyAiMissingIconAndSave(value.profileId, item.key, BitmapIconDrawable(result.bitmap))
                if (outcome == AiBatchTarget.MISSING) appliedKeys = appliedKeys + item.key
                else result.bitmap.recycle()
                outcome
            } catch (e: Exception) { result.bitmap.recycle(); throw e }
        }, ::connected)
        collector?.cancel()
        val observed = runner!!
        collector = viewModelScope.launch { observed.state.collectLatest { session = it } }
    }
    /** Called only after the UI's explicit paid-request confirmation. */
    fun start() {
        if (busy || loading) return
        val profile = profileId ?: return
        val config = settings ?: return
        val chosen = candidates.filter { it.key in selected }
        if (chosen.size > 1000) { error = "Select at most 1000 icons in one batch."; return }
        if (chosen.isEmpty()) { error = "Select at least one missing icon."; return }
        if (!hasKey) { error = "Add a key for ${config.provider} in AI settings."; return }
        if (!connected()) { error = "No internet connection."; return }
        if (provider.activeProfileId != profile) { error = "Profile changed. Reopen Global options."; return }
        install(AiBatchSession(profile, config, chosen.map { AiBatchItem(it.key, it.appName, instructions[it.key].orEmpty()) }))
        run()
    }
    /** Failed/uncertain items remain failed; Resume only runs pending or saved GENERATED items. */
    fun run(retryKey: String? = null) {
        if (busy || loading || runner == null) return
        busy = true; pauseRequested = false; error = null
        val current = runner!!
        task = viewModelScope.launch {
            try {
                credentials.read(session!!.settings.provider) // preflight decrypt once, without logging or sending a request
                if (retryKey != null) current.retry(retryKey)
                current.record()
                current.run(retryKey)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message?.take(400) ?: "Batch failed." }
            finally { if (!cancelRequested) busy = false }
        }
    }
    fun pause() { pauseRequested = true; runner?.pause() }
    fun cancel() {
        val current = runner ?: return
        if (cancelRequested) return
        cancelRequested = true; busy = true
        pause()
        val activeTask = task
        activeTask?.cancel()
        viewModelScope.launch {
            try { activeTask?.join(); withContext(NonCancellable) { current.cancelRemaining() } }
            catch (e: Exception) { error = e.message?.take(400) ?: "Could not record cancellation." }
            finally { cancelRequested = false; busy = false }
        }
    }
    fun newSelection() {
        if (busy) return
        collector?.cancel(); runner = null; session = null; selected = emptySet()
        loading = true
        viewModelScope.launch {
            try { profileId?.let { store.clear(it) }; refreshSettingsNow() }
            catch (e: Exception) { error = e.message?.take(400) ?: "Could not clear the old task." }
            finally { loading = false }
        }
    }
}

internal fun missingAiApps(apps: List<PackageInfoStruct>, lockedKeys: Set<String>): List<PackageInfoStruct> =
    apps.filter { it.createdIcon == null && it.key !in lockedKeys }
