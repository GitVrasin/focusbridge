package com.focusbridge.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusbridge.domain.model.SessionIntent
import com.focusbridge.domain.usecase.WeeklyInsights
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.insights == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No data yet. Keep the app running for a few days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }

            else -> InsightsContent(
                insights = state.insights!!,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun InsightsContent(insights: WeeklyInsights, modifier: Modifier = Modifier) {
    val totalMin = (insights.totalTimeMs / 60_000).toInt()
    val baselineMin = (insights.baselineTotalMs / 60_000).toInt()
    val savingMin = (insights.estimatedSavingMs / 60_000).toInt()

    val trendPct = if (insights.baselineTotalMs > 0) {
        ((insights.totalTimeMs - insights.baselineTotalMs).toFloat() / insights.baselineTotalMs * 100).roundToInt()
    } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Last ${insights.daysWithData} days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // --- Total time card ---
        InsightCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total time on distracting apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(formatMinutes(totalMin),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                }
                if (trendPct != null) {
                    val sign = if (trendPct > 0) "+" else ""
                    val color = if (trendPct <= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                    Column(horizontalAlignment = Alignment.End) {
                        Text("vs baseline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$sign$trendPct%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = color)
                    }
                }
            }
            if (savingMin > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reducing by 20% would save ~${formatMinutes(savingMin)} over this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Sessions + goal rate card ---
        InsightCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Sessions", "${insights.sessionCount}")
                StatItem("Went to goal", "${insights.goToGoalCount}")
                StatItem("Goal rate", "${(insights.goToGoalRate * 100).roundToInt()}%")
            }
        }

        // --- Why you opened these apps ---
        if (insights.intentBreakdown.isNotEmpty()) {
            InsightCard {
                Text("Why you opened these apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                insights.intentBreakdown
                    .entries
                    .sortedByDescending { it.value }
                    .forEach { (intentName, fraction) ->
                        val label = runCatching {
                            SessionIntent.valueOf(intentName).label
                        }.getOrDefault(intentName)
                        val pct = (fraction * 100).roundToInt()

                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(120.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.weight(1f).height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$pct%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
            }
        }

        // --- Baseline comparison note ---
        if (insights.daysWithData < 3) {
            InsightCard {
                Text(
                    "Keep going — full insights appear after 3 days of tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InsightCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
