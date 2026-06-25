package com.focusbridge.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusbridge.domain.model.DistractingApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppsScreen(
    onBack: () -> Unit,
    onAddApps: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apps by viewModel.activeApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddApps,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Apps") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppLimitRow(
                    app = app,
                    onLimitChange = { mins -> viewModel.updateAppLimit(app.packageName, mins * 60_000L) },
                    onRemove = { viewModel.removeApp(app.packageName) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            if (apps.isEmpty()) {
                item {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        "No apps added yet. Tap \"Add Apps\" to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Extra space so FAB doesn't overlap last item
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AppLimitRow(
    app: DistractingApp,
    onLimitChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val currentMinutes = (app.dailyLimitMs / 60_000).toInt().coerceAtLeast(2)

    Column(Modifier.padding(vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "Session threshold · ${app.packageName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = currentMinutes.toFloat(),
                onValueChange = { onLimitChange(it.toInt().coerceAtLeast(2)) },
                valueRange = 2f..120f,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$currentMinutes min",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}
