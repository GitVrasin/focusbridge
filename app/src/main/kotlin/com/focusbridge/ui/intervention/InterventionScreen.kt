package com.focusbridge.ui.intervention

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbridge.domain.model.SessionIntent
import com.focusbridge.ui.theme.OverLimitRed

@Composable
fun InterventionScreen(viewModel: InterventionViewModel) {
    val state by viewModel.uiState.collectAsState()

    when (state.phase) {
        InterventionPhase.INTENT_CAPTURE -> IntentCaptureScreen(
            appDisplayName = state.appDisplayName,
            onIntentSelected = viewModel::onIntentSelected
        )
        InterventionPhase.INTERVENTION -> InterventionContentScreen(
            state = state,
            onGoToGoal = viewModel::onGoToGoal,
            onContinue = viewModel::onContinue,
            onMuteForToday = viewModel::onMuteForToday
        )
    }
}

@Composable
private fun IntentCaptureScreen(
    appDisplayName: String,
    onIntentSelected: (SessionIntent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤔", fontSize = 48.sp)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Why did you open",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    appDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SessionIntent.entries.forEach { intent ->
                    OutlinedButton(
                        onClick = { onIntentSelected(intent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            intent.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InterventionContentScreen(
    state: InterventionUiState,
    onGoToGoal: () -> Unit,
    onContinue: () -> Unit,
    onMuteForToday: () -> Unit
) {
    val sessionMinutes = (state.sessionDurationMs / 60_000).toInt().coerceAtLeast(1)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: usage context
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Text("⏱", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    if (state.isGlobalMode) "Combined screen time" else "You've been on",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    state.appDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = OverLimitRed,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "for $sessionMinutes min this session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Intent summary
                if (state.selectedIntent != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Opened because: ${state.selectedIntent.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Middle: goal reminder
            if (state.goalTitle.isNotBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Your goal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        '"' + state.goalTitle + '"',
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            // Bottom: actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.nextActionLabel != null) {
                    Button(
                        onClick = onGoToGoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "GO TO GOAL",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                "▶  ${state.nextActionLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onMuteForToday) {
                    Text(
                        "Mute for Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
