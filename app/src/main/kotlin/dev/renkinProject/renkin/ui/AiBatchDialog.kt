@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.renkinProject.renkin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.renkinProject.renkin.ai.*

@Composable
internal fun AiBatchDialog(model: AiBatchViewModel, onDismiss: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) } // start/resume/retry:<key>
    var editingKey by remember { mutableStateOf<String?>(null) }
    var instruction by remember { mutableStateOf("") }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) model.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); model.pause() }
    }
    val close = { model.pause(); onDismiss() }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(topBar = {
                TopAppBar(title = { Text("Generate missing with AI") }, navigationIcon = {
                    IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                })
            }) { padding ->
                val session = model.session
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Each selected app is a separate paid generation. Successful icons are added and saved automatically; review them before Build icon pack.")
                        Text("Keep the app open. Leaving pauses after the current icon. After restart, Resume never retries failed or uncertain requests automatically.")
                    }
                    model.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (model.loading) item { WavyLoadingBar() }
                    else if (session == null) {
                        item {
                            TextButton(onClick = { showSettings = true }) { Text("AI settings & shared style") }
                            model.settings?.let { Text("${it.provider} · ${it.model} · ${it.quality} · ${it.referenceNames.size} references") }
                            if (!model.hasKey) Text("Add an API key for the selected provider.")
                            OutlinedTextField(search, { search = it }, label = { Text("Search missing apps") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Row {
                                TextButton(onClick = { model.selectAll(model.candidates.map { it.key }.toSet()) }) { Text("Select all") }
                                TextButton(onClick = { model.selectAll(emptySet()) }) { Text("Select none") }
                            }
                            val count = model.candidates.count { it.key in model.selected }
                            Text("$count selected / ${model.candidates.size} missing")
                            Button(onClick = { confirm = "start" }, enabled = count > 0 && model.hasKey && !model.busy) {
                                Text("Generate selected ($count)")
                            }
                        }
                        val candidates = model.candidates.filter { it.appName.contains(search.trim(), true) || it.originalName.contains(search.trim(), true) }
                        if (candidates.isEmpty()) item { Text(if (model.candidates.isEmpty()) "No missing icons in this profile." else "No apps match your search.") }
                        items(candidates, key = { it.key }) { app ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(app.key in model.selected, { model.select(app.key, it) })
                                    rememberAppBitmap(app, 40.dp)?.let { Image(it, null, Modifier.size(40.dp)) }
                                    Text(app.appName, Modifier.weight(1f).padding(start = 12.dp))
                                    TextButton(onClick = { editingKey = app.key; instruction = model.instructions[app.key].orEmpty() }) { Text("Prompt") }
                                }
                                if (!model.instructions[app.key].isNullOrBlank()) Text("App-specific instruction saved", style = MaterialTheme.typography.labelMedium)
                                HorizontalDivider()
                            }
                        }
                    } else {
                        item {
                            Text("${session.done} / ${session.items.size} icons processed", style = MaterialTheme.typography.titleLarge)
                            Text("${session.settings.provider} · ${session.settings.model} · ${session.settings.quality}")
                            Text("${session.items.count { it.status == AiBatchStatus.SUCCEEDED }} saved · ${session.items.count { it.status == AiBatchStatus.FAILED }} failed")
                            session.message?.let { Text(it) }
                            if (model.busy) {
                                WavyLoadingBar()
                                Text(session.items.firstOrNull { it.status == AiBatchStatus.REQUESTING }?.name ?: "Saving / checking icons…")
                                Row {
                                    TextButton(onClick = model::pause, enabled = !model.pauseRequested) { Text(if (model.pauseRequested) "Pausing after current…" else "Pause after current") }
                                    TextButton(onClick = model::cancel) { Text("Cancel") }
                                }
                            } else {
                                if (session.items.any { it.status in setOf(AiBatchStatus.PENDING, AiBatchStatus.GENERATED) })
                                    Button(onClick = { confirm = "resume" }) { Text("Resume remaining") }
                                TextButton(onClick = model::newSelection) { Text("New selection (keep saved icons)") }
                            }
                        }
                        items(session.items, key = { it.key }) { item ->
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(when (item.status) {
                                    AiBatchStatus.PENDING -> "Waiting"
                                    AiBatchStatus.REQUESTING -> "Generating…"
                                    AiBatchStatus.GENERATED -> "PNG saved; waiting to apply"
                                    AiBatchStatus.SUCCEEDED -> "Saved and added to this profile"
                                    AiBatchStatus.FAILED -> "Failed"
                                    AiBatchStatus.SKIPPED -> "Skipped"
                                })
                                item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                if (item.status == AiBatchStatus.FAILED && !model.busy) Row {
                                    TextButton(onClick = { editingKey = item.key; instruction = model.instructions[item.key] ?: item.instruction }) { Text("Prompt") }
                                    TextButton(onClick = { confirm = "retry:${item.key}" }) { Text("Retry this app") }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
    if (showSettings) AiSettingsScreen { showSettings = false; model.refreshSettings() }
    editingKey?.let { key ->
        RenkinAlertDialog(onDismissRequest = { editingKey = null }, title = { Text("App-specific instruction") }, text = {
            Column {
                Text("Adds to the shared style; no package identifiers are sent. Blank inherits the global style.")
                OutlinedTextField(instruction, { if (it.length <= 2000) instruction = it }, minLines = 3,
                    supportingText = { Text("${instruction.length}/2000") })
                TextButton(onClick = { instruction = "" }) { Text("Reset to global style") }
            }
        }, confirmButton = { TextButton(onClick = { model.setInstruction(key, instruction); editingKey = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingKey = null }) { Text("Cancel") } })
    }
    confirm?.let { action ->
        val config = if (action == "start") model.settings else model.session?.settings
        val count = if (action == "start") model.candidates.count { it.key in model.selected }
            else if (action.startsWith("retry:")) 1 else model.session?.items?.count { it.status in setOf(AiBatchStatus.PENDING, AiBatchStatus.GENERATED) } ?: 0
        RenkinAlertDialog(onDismissRequest = { confirm = null }, title = { Text("Confirm paid generation") }, text = {
            Text("$count app(s) · ${config?.provider} · ${config?.model} · ${config?.quality}. " +
                "Matching saved PNGs are reused; each remaining app needs a paid generation. Regenerate / Retry may incur another charge. " +
                (if (config?.provider == AiSettings.STABILITY) "Stability uses two API calls per new icon. " else "") +
                "The batch saves the current profile after each success. Check your API usage if an earlier request was interrupted.")
        }, confirmButton = { TextButton(onClick = {
            confirm = null
            when { action == "start" -> model.start(); action == "resume" -> model.run(); else -> model.run(action.removePrefix("retry:")) }
        }) { Text("Generate and save") } }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
}
