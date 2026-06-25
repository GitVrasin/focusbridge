package com.focusbridge.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusbridge.domain.model.AppUsageSummary
import com.focusbridge.ui.theme.OverLimitRed
import com.focusbridge.ui.theme.PrimaryBlue
import com.focusbridge.ui.theme.UnderLimitGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSummary: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val goal by viewModel.activeGoal.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Bridge", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Permission warning banner
            if (!state.hasUsagePermission) {
                PermissionWarningBanner()
                Spacer(Modifier.height(12.dp))
            }

            // Date + Goal
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE MMM d"))
            Text(today, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            goal?.let { g ->
                Text(
                    '"' + g.title + '"',
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(20.dp))

            // App usage cards
            val summary = state.summary
            if (summary != null && summary.appUsages.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(summary.appUsages) { usage ->
                        AppUsageCard(usage)
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Interventions", summary.totalInterventions.toString())
                    VerticalDivider(Modifier.height(48.dp))
                    StatItem("Actions taken", summary.totalAccepted.toString())
                    VerticalDivider(Modifier.height(48.dp))
                    StatItem("Dismissed", summary.totalDismissed.toString())
                }

                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onOpenSummary,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("View full summary") }

            } else if (summary != null) {
                // Configured but no apps yet
                Text(
                    "No distracting apps configured yet.\nGo to Settings to add some.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Settings")
                }
            } else {
                // Loading or no permission
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppUsageCard(usage: AppUsageSummary) {
    val usageMin = (usage.usageMs / 60_000).toInt()
    val limitMin = (usage.app.dailyLimitMs / 60_000).toInt()
    val progress = (usage.usageMs.toFloat() / usage.app.dailyLimitMs).coerceIn(0f, 1f)
    val barColor = if (usage.isOverLimit) OverLimitRed else PrimaryBlue

    Card(
        modifier = Modifier.width(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                usage.app.displayName.take(10),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${usageMin}m / ${limitMin}m",
                style = MaterialTheme.typography.bodySmall,
                color = if (usage.isOverLimit) OverLimitRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionWarningBanner() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = OverLimitRed.copy(alpha = 0.15f))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = OverLimitRed)
            Spacer(Modifier.width(8.dp))
            Text(
                "Monitoring is off — grant Usage Access to enable",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) { Text("Fix") }
        }
    }
}
