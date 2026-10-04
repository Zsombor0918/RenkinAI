package dev.renkinProject.renkin.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.renkinProject.renkin.data.UploadedImageStore
import dev.renkinProject.renkin.ui.getBitmapFromURI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AiIconViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: AiSettingsStore,
    private val credentials: AiCredentialStore,
    private val prompts: AiPromptStore,
    private val service: AiIconGenerationService
) : ViewModel() {
    var settings by mutableStateOf<AiSettings?>(null); private set
    var hasKey by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var settingsBusy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var result by mutableStateOf<AiIconResult?>(null); private set
    var appInstruction by mutableStateOf(""); private set
    private var promptTask: Job? = null
    private var task: Job? = null
    private var target: String? = null
    private var session = 0

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            safely {
                settings = settingsStore.load()
                hasKey = withContext(Dispatchers.IO) { credentials.hasKey(settings!!.provider) }
            }
        }
    }

    fun open(value: String) {
        close()
        target = value
        result = null
        error = null
        appInstruction = ""
        busy = true
        val openedSession = session
        task = viewModelScope.launch {
            try { safely {
                val instruction = prompts.load(value)
                val previous = service.previous(value)
                if (session == openedSession) { appInstruction = instruction; result = previous }
            } }
            finally { if (session == openedSession) busy = false }
        }
    }

    fun generate(source: Bitmap?) {
        if (busy || settingsBusy) return
        val identity = target ?: return
        val config = settings?.copy(appInstruction = appInstruction) ?: return
        if (source == null) { error = "The original app icon could not be read."; return }
        busy = true
        error = null
        val requestSession = session
        task = viewModelScope.launch {
            try { safely { prompts.save(identity, config.appInstruction); result = service.generate(identity, source, config) } }
            finally { if (session == requestSession) busy = false }
        }
    }

    fun setInstruction(value: String) {
        if (value.length > 2000 || busy) return
        appInstruction = value
        val identity = target ?: return
        promptTask?.cancel()
        promptTask = viewModelScope.launch { safely { prompts.save(identity, value) } }
    }

    fun close() {
        session++
        task?.cancel()
        task = null
        target = null
        busy = false
    }

    fun cancel() {
        session++
        task?.cancel()
        task = null
        busy = false
        error = "Cancelled locally. A submitted API request may still be billed. Saved images remain in Upload."
    }

    fun saveSettings(value: AiSettings, newKey: String, onSaved: () -> Unit) {
        if (settingsBusy) return
        settingsBusy = true
        error = null
        viewModelScope.launch {
            try {
                safely {
                    value.validate()
                    if (newKey.isNotBlank()) withContext(Dispatchers.IO) { credentials.save(value.provider, newKey) }
                    settingsStore.save(value)
                    settings = value
                    hasKey = withContext(Dispatchers.IO) { credentials.hasKey(value.provider) }
                    onSaved()
                }
            } finally { settingsBusy = false }
        }
    }

    fun hasProviderKey(provider: String): Boolean = credentials.hasKey(provider)

    fun removeKey(provider: String = settings?.provider ?: AiSettings.OPENAI) {
        if (settingsBusy) return
        settingsBusy = true
        viewModelScope.launch {
            try { safely { withContext(Dispatchers.IO) { credentials.clear(provider) }; hasKey = withContext(Dispatchers.IO) { credentials.hasKey(settings?.provider ?: AiSettings.OPENAI) } } }
            finally { settingsBusy = false }
        }
    }

    fun importReferences(uris: List<Uri>, onImported: (List<String>) -> Unit) {
        if (settingsBusy || uris.isEmpty()) return
        settingsBusy = true
        error = null
        viewModelScope.launch {
            try {
                safely {
                    val names = withContext(Dispatchers.IO) {
                        uris.take(5).map { uri ->
                            val source = getBitmapFromURI(context, uri) ?: error("A reference image could not be decoded.")
                            val scale = minOf(1f, 512f / maxOf(source.width, source.height))
                            val image = Bitmap.createScaledBitmap(source,
                                (source.width * scale).toInt().coerceAtLeast(1),
                                (source.height * scale).toInt().coerceAtLeast(1), true)
                            try { UploadedImageStore.save(context, image).name }
                            finally { if (image !== source) image.recycle(); source.recycle() }
                        }
                    }
                    onImported(names)
                }
            } finally { settingsBusy = false }
        }
    }

    private suspend fun safely(block: suspend () -> Unit) {
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message?.take(400) ?: "The operation failed." }
    }
}
