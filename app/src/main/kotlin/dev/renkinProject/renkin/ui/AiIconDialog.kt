@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.renkinProject.renkin.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import dev.renkinProject.renkin.data.UploadedImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.navigation.compose.hiltViewModel
import dev.renkinProject.renkin.ai.AiIconResult
import dev.renkinProject.renkin.ai.AiIconViewModel
import dev.renkinProject.renkin.ai.AiSettings

@Composable
internal fun AiIconDialog(
    target: String,
    appName: String,
    original: Bitmap?,
    onUse: (AiIconResult) -> Unit,
    onDismiss: () -> Unit
) {
    val model: AiIconViewModel = hiltViewModel()
    var showSettings by remember { mutableStateOf(false) }
    DisposableEffect(target) {
        model.open(target)
        onDispose { model.close() }
    }
    AiScreen("AI · $appName", onDismiss) {
        Text(if (model.settings?.provider == AiSettings.STABILITY)
            "Stability uses two paid API calls per icon (style transfer + transparent background); new-account trial credits may cover early tests."
            else "One paid API request per Generate or Regenerate. ChatGPT Plus is separate.")
        original?.let { Image(it.asImageBitmap(), "Original app icon", Modifier.size(88.dp)) }
        TextButton(onClick = { showSettings = true }, enabled = !model.busy) { Text("AI settings & style references") }
        model.settings?.let { Text("${it.model} · ${it.quality} · ${it.referenceNames.size} references") }
        OutlinedTextField(value = model.appInstruction, onValueChange = model::setInstruction,
            label = { Text("App-specific instruction (optional)") },
            supportingText = { Text("Adds to the shared style; blank inherits it. ${model.appInstruction.length}/2000") },
            modifier = Modifier.fillMaxWidth(), enabled = !model.busy, minLines = 2)
        TextButton(onClick = { model.setInstruction("") }, enabled = !model.busy && model.appInstruction.isNotEmpty()) {
            Text("Reset to global style")
        }
        if (!model.hasKey) Text("Add an API key for the selected provider in AI settings.")
        if (model.busy) {
            WavyLoadingBar(Modifier.fillMaxWidth())
            Text("Generating can take several minutes. Keep the app open.")
            TextButton(onClick = model::cancel) { Text("Cancel request") }
        }
        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        model.result?.let { result ->
            HorizontalDivider()
            Text("Saved preview on dark and light backgrounds")
            Text("A restored preview may have been generated with earlier style settings.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(result.bitmap.asImageBitmap(), "Generated icon on dark background",
                    Modifier.weight(1f).background(Color(0xff242424)).padding(12.dp))
                Image(result.bitmap.asImageBitmap(), "Generated icon on light background",
                    Modifier.weight(1f).background(Color(0xffeeeeee)).padding(12.dp))
            }
            Text("Check recognizability, padding and transparency. Saved results remain available in Upload.")
            Button(onClick = { onUse(result) }, enabled = !model.busy) { Text("Use in editor") }
            Text("Then press the editor’s check mark to apply the icon.")
        }
        Button(onClick = { model.generate(original) }, enabled =
            !model.busy && !model.settingsBusy && model.hasKey && model.settings != null && original != null) {
            Text(if (model.result == null) "Generate" else "Regenerate")
        }
        TextButton(onClick = onDismiss) { Text("Close") }
    }
    if (showSettings) AiSettingsScreen(onDismiss = { showSettings = false })
}

@Composable
internal fun AiSettingsScreen(onDismiss: () -> Unit) {
    val model: AiIconViewModel = hiltViewModel()
    val saved = model.settings
    if (saved == null) {
        AiScreen("AI settings", onDismiss) {
            model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            WavyLoadingBar()
        }
        return
    }
    var style by remember { mutableStateOf(saved.style) }
    var provider by remember { mutableStateOf(saved.provider) }
    var selectedModel by remember { mutableStateOf(saved.model) }
    var quality by remember { mutableStateOf(saved.quality) }
    var references by remember { mutableStateOf(saved.referenceNames) }
    // Deliberately not rememberSaveable: never include credentials in saved-instance state.
    var newKey by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        model.importReferences(uris) { references = it }
    }
    AiScreen("AI settings", onDismiss, secure = true) {
        Text("AI service")
        AiSettings.PROVIDERS.forEach { name ->
            TextButton(onClick = { if (provider != name) { provider = name; newKey = "" } }, enabled = !model.settingsBusy) {
                Text((if (provider == name) "✓ " else "") + name)
            }
        }
        val providerHasKey = model.hasProviderKey(provider)
        Text(if (provider == AiSettings.STABILITY) "Stability AI · trial credits are limited"
            else "OpenAI · usage billed separately from ChatGPT Plus")
        Text(if (provider == AiSettings.STABILITY)
            "Stability AI offers trial credits on new developer accounts. This integration uses 8 credits for style transfer and 5 more to remove the background (13 credits per accepted generation). It uses the first selected reference image."
            else "Set a small API spending limit before generating. Each retry costs separately. " +
            "The app sends the original icon, your style and selected references. App identifiers stay on this phone.")
        OutlinedTextField(newKey, { newKey = it }, Modifier.fillMaxWidth(),
            label = { Text(if (providerHasKey) "Replace saved key (optional)" else "$provider API key") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            enabled = !model.settingsBusy)
        Text(if (providerHasKey) "Key saved on this device. It is excluded from backups." else "No API key saved for $provider.")
        if (providerHasKey) TextButton(onClick = { newKey = ""; model.removeKey(provider) }, enabled = !model.settingsBusy) { Text("Remove saved key") }
        OutlinedTextField(style, { if (it.length <= 6000) style = it }, Modifier.fillMaxWidth(),
            label = { Text("Shared style prompt") }, minLines = 4, enabled = !model.settingsBusy)
        if (provider == AiSettings.OPENAI) {
            Text("Image model")
            AiSettings.MODELS.forEach { name ->
                TextButton(onClick = { selectedModel = name }, enabled = !model.settingsBusy) {
                    Text((if (selectedModel == name) "✓ " else "") + name)
                }
            }
            Row {
                AiSettings.QUALITIES.forEach { name ->
                    TextButton(onClick = { quality = name }, enabled = !model.settingsBusy) {
                        Text((if (quality == name) "✓ " else "") + name)
                    }
                }
            }
        }
        Text("Style references: ${references.size}/5. Choose representative icons from the pack you want to match.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            references.forEach { AiReferencePreview(it) }
        }
        TextButton(onClick = { picker.launch("image/*") }, enabled = !model.settingsBusy) { Text("Choose / replace reference images") }
        if (references.isNotEmpty()) TextButton(onClick = { references = emptyList() }, enabled = !model.settingsBusy) { Text("Clear references") }
        if (model.settingsBusy) WavyLoadingBar()
        model.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            model.saveSettings(AiSettings(provider, style, selectedModel, quality, references), newKey) {
                newKey = ""
                onDismiss()
            }
        }, enabled = !model.settingsBusy && style.isNotBlank()) { Text("Save") }
    }
}

@Composable
internal fun AiScreen(
    title: String,
    onDismiss: () -> Unit,
    secure: Boolean = false,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false,
        securePolicy = if (secure) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(topBar = {
                TopAppBar(title = { Text(title) }, navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                })
            }) { padding ->
                CenteredFullscreenContent(Modifier.padding(padding)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                }
            }
        }
    }
}

@Composable
private fun AiReferencePreview(name: String) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, name) {
        value = withContext(Dispatchers.IO) {
            val file = File(UploadedImageStore.directory(context), name)
            if (!name.matches(Regex("img_[0-9]+_[0-9]+\\.png")) || file.length() !in 1..(4 * 1024 * 1024)) null
            else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth !in 1..512 || bounds.outHeight !in 1..512) null
                else BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = 4 })
            }
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Style reference", Modifier.size(44.dp).background(Color.DarkGray)) }
}
