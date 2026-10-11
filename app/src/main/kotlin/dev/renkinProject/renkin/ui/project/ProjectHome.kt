package dev.renkinProject.renkin.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.renkinProject.renkin.R
import dev.renkinProject.renkin.data.DEFAULT_PROFILE_ID
import dev.renkinProject.renkin.data.Profile
import dev.renkinProject.renkin.ui.CenteredFullscreenContent
import dev.renkinProject.renkin.ui.WavyLoadingBar
import dev.renkinProject.renkin.ui.theme.CardShape

internal enum class ProjectHomeAvailability { INITIALIZING, READY, UNAVAILABLE }

internal data class ProjectHomeState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long = DEFAULT_PROFILE_ID,
    val availability: ProjectHomeAvailability = ProjectHomeAvailability.INITIALIZING,
    val busy: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val operationFailed: Boolean = false,
    val operationInterrupted: Boolean = false
) {
    val actionsEnabled: Boolean get() = availability == ProjectHomeAvailability.READY && !busy && profiles.isNotEmpty()
}

/** Stateless UI: stable profile IDs and truthful saved/session flags, no coverage estimates. */
@Composable
internal fun ProjectHome(
    state: ProjectHomeState,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    onExtend: () -> Unit,
    onClassicEditor: () -> Unit,
    onRetry: () -> Unit,
    onSetup: (Long) -> Unit = {}
) {
    CenteredFullscreenContent(maximumContentWidth = 960.dp) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("project-home")
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.projectHomeBrand), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(R.string.projectHomeTitle),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(stringResource(R.string.projectHomeDescription), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                BoxWithConstraints {
                    if (maxWidth >= 600.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ProjectActionCard(R.string.projectCreate, R.string.projectCreateDescription,
                                Icons.Filled.Add, "project-create", state.actionsEnabled, onCreate, Modifier.weight(1f))
                            ProjectActionCard(R.string.projectExtend, R.string.projectExtendDescription,
                                Icons.Filled.Extension, "project-extend", state.actionsEnabled, onExtend, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ProjectActionCard(R.string.projectCreate, R.string.projectCreateDescription,
                                Icons.Filled.Add, "project-create", state.actionsEnabled, onCreate)
                            ProjectActionCard(R.string.projectExtend, R.string.projectExtendDescription,
                                Icons.Filled.Extension, "project-extend", state.actionsEnabled, onExtend)
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.projectAutomationLater), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = onClassicEditor,
                        enabled = state.actionsEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-classic")
                    ) { Text(stringResource(R.string.projectClassicEditor)) }
                }
            }
            if (state.busy || state.availability == ProjectHomeAvailability.INITIALIZING) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.testTag("project-loading"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(if (state.busy) R.string.projectOpening else R.string.projectInitializing))
                        WavyLoadingBar(Modifier.fillMaxWidth())
                    }
                }
            }
            if (state.availability == ProjectHomeAvailability.UNAVAILABLE || state.operationFailed || state.operationInterrupted) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(shape = CardShape, modifier = Modifier.fillMaxWidth().testTag("project-error")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(when {
                                state.operationInterrupted -> R.string.projectInterrupted
                                state.operationFailed -> R.string.projectOperationFailed
                                else -> R.string.projectUnavailable
                            }))
                            OutlinedButton(
                                onClick = onRetry, enabled = !state.busy,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("project-retry")
                            ) { Text(stringResource(R.string.projectReload)) }
                        }
                    }
                }
            }
            if (state.availability == ProjectHomeAvailability.READY) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.projectListTitle), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
                if (state.profiles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.projectEmpty), modifier = Modifier.testTag("project-empty"))
                            OutlinedButton(onClick = onRetry, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.projectReload))
                            }
                        }
                    }
                } else if (state.profiles.size == 1 && state.profiles.single().id == DEFAULT_PROFILE_ID) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(R.string.projectDefaultOnly), modifier = Modifier.testTag("project-default-only"))
                    }
                }
            }
            items(state.profiles, key = { it.id }) { profile ->
                ProjectCard(
                    profile = profile,
                    active = state.availability == ProjectHomeAvailability.READY && profile.id == state.activeProfileId,
                    unsaved = profile.id == state.activeProfileId && state.hasUnsavedChanges,
                    enabled = state.actionsEnabled,
                    onOpen = { onOpen(profile.id) },
                    onSetup = { onSetup(profile.id) }
                )
            }
        }
    }
}

@Composable
private fun ProjectActionCard(
    title: Int, description: Int, icon: ImageVector, tag: String,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null)
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) {
                Text(stringResource(title))
            }
        }
    }
}

@Composable
private fun ProjectCard(profile: Profile, active: Boolean, unsaved: Boolean, enabled: Boolean, onOpen: () -> Unit, onSetup: () -> Unit) {
    val openDescription = stringResource(R.string.projectOpenDescription, profile.name, profile.id)
    Card(shape = CardShape, modifier = Modifier.fillMaxWidth().testTag("project-${profile.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.projectIdentity, profile.id), style = MaterialTheme.typography.labelMedium)
            if (profile.description.isNotBlank()) {
                Text(profile.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (active) Text(stringResource(R.string.projectActive), color = MaterialTheme.colorScheme.primary)
            if (unsaved) Text(stringResource(R.string.projectUnsaved), color = MaterialTheme.colorScheme.tertiary)
            if (profile.hasUnbuiltChanges) Text(stringResource(R.string.unbuiltChanges), color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton(
                onClick = onOpen, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-open-${profile.id}")
                    .semantics { contentDescription = openDescription }
            ) { Text(stringResource(R.string.projectContinue)) }
            OutlinedButton(onClick = onSetup, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-setup-${profile.id}")) {
                Text(stringResource(R.string.setupReturn))
            }
        }
    }
}
