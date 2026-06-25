package com.focusbridge.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditGoal: () -> Unit,
    onEditApps: () -> Unit,
    onEditNextActions: (Long) -> Unit,
    onEditLimitMode: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val goal by viewModel.activeGoal.collectAsState()
    val isGlobalMode by viewModel.isGlobalLimitMode.collectAsState()
    val globalLimitMs by viewModel.globalLimitMs.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionLabel("Goal")
            SettingsRow(
                title = "Edit goal",
                subtitle = goal?.title ?: "Not set",
                onClick = onEditGoal
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("Distracting Apps")
            SettingsRow(
                title = "Manage apps & limits",
                onClick = onEditApps
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("Limit Mode")
            SettingsRow(
                title = "Limit enforcement",
                subtitle = if (isGlobalMode)
                    "Global: ${(globalLimitMs / 60_000).toInt()} min combined"
                else
                    "Per-app: individual limits",
                onClick = onEditLimitMode
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("Next Actions")
            SettingsRow(
                title = "Edit redirect actions",
                subtitle = "What opens when you get interrupted",
                onClick = {
                    goal?.id?.let { onEditNextActions(it) }
                },
                enabled = goal != null
            )

            Spacer(Modifier.height(12.dp))
            SectionLabel("System")
            SettingsRow(
                title = "Battery optimization",
                subtitle = "Disable for reliable monitoring on Samsung/Xiaomi",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            )
            SettingsRow(
                title = "Usage access",
                subtitle = "Required for monitoring",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
