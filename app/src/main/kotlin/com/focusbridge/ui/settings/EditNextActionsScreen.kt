package com.focusbridge.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusbridge.domain.model.NextActionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNextActionsScreen(
    goalId: Long,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val actions by viewModel.nextActions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(goalId) { viewModel.loadNextActions(goalId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Next Actions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (actions.size < 3) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add action")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    "The first action is the \"Go to Goal\" destination shown during an intervention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            items(actions, key = { it.id }) { action ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(action.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${action.type.name}  •  ${action.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { viewModel.deleteNextAction(action.id, goalId) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNextActionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { label, target, type ->
                viewModel.addNextAction(goalId, label, target, type)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddNextActionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, NextActionType) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NextActionType.URL) }

    val typeOptions = listOf(
        NextActionType.URL to "URL",
        NextActionType.YOUTUBE to "YouTube",
        NextActionType.SPOTIFY to "Spotify",
        NextActionType.APP_INTENT to "App"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Next Action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. My Udemy Course)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    typeOptions.forEach { (type, chipLabel) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(chipLabel, style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = {
                        Text(
                            when (selectedType) {
                                NextActionType.APP_INTENT -> "Package name (e.g. com.spotify.music)"
                                NextActionType.YOUTUBE -> "YouTube URL"
                                NextActionType.SPOTIFY -> "Spotify URL"
                                else -> "URL (https://...)"
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (selectedType == NextActionType.APP_INTENT)
                            KeyboardType.Text else KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(label, target, selectedType) },
                enabled = label.isNotBlank() && target.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
