package com.focusbridge.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusbridge.ui.theme.FocusBridgeTheme
import com.focusbridge.ui.theme.PrimaryBlue
import com.focusbridge.ui.theme.UnderLimitGreen

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    FocusBridgeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.currentStep) {
                0 -> WelcomeStep(onNext = viewModel::nextStep)
                1 -> GoalStep(
                    title = state.goalTitle,
                    description = state.goalDescription,
                    onTitleChange = viewModel::setGoalTitle,
                    onDescChange = viewModel::setGoalDescription,
                    onNext = viewModel::nextStep
                )
                2 -> PickAppsStep(
                    apps = state.installedApps,
                    selected = state.selectedPackages,
                    onToggle = viewModel::toggleApp,
                    onNext = viewModel::nextStep
                )
                3 -> SetLimitsStep(
                    selectedApps = state.installedApps.filter { it.packageName in state.selectedPackages },
                    limits = state.appLimitsMinutes,
                    onLimitChange = viewModel::setAppLimit,
                    onNext = viewModel::nextStep
                )
                4 -> NextActionStep(
                    label = state.nextActionLabel,
                    url = state.nextActionUrl,
                    onLabelChange = viewModel::setNextActionLabel,
                    onUrlChange = viewModel::setNextActionUrl,
                    onNext = viewModel::nextStep
                )
                5 -> PermissionsStep(
                    hasUsage = state.hasUsagePermission,
                    hasNotification = state.hasNotificationPermission,
                    isSaving = state.isSaving,
                    onCheckPermissions = viewModel::checkPermissions,
                    onComplete = { viewModel.completeOnboarding(onComplete) }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    OnboardingScaffold(
        title = "Focus Bridge",
        subtitle = "Stop scrolling.\nStart doing.",
        body = {
            Text(
                text = "When you open a distracting app, Focus Bridge interrupts you and redirects you to what actually matters.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        primaryLabel = "Get Started",
        onPrimary = onNext
    )
}

@Composable
private fun GoalStep(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingScaffold(
        title = "Your Goal",
        subtitle = "What are you working toward?",
        body = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Goal (e.g. Finish my Kotlin course)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescChange,
                label = { Text("Why does this matter? (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        primaryLabel = "Continue",
        onPrimary = onNext,
        primaryEnabled = title.isNotBlank()
    )
}

@Composable
private fun PickAppsStep(
    apps: List<InstalledAppInfo>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Which apps pull you away?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${selected.size} selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(apps) { app ->
                val isSelected = app.packageName in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(app.packageName) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(app.displayName, style = MaterialTheme.typography.bodyLarge)
                        if (app.isSuggested) {
                            Text(
                                "Suggested",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Continue") }
    }
}

@Composable
private fun SetLimitsStep(
    selectedApps: List<InstalledAppInfo>,
    limits: Map<String, Int>,
    onLimitChange: (String, Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Set Daily Limits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("How many minutes per day is okay?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(selectedApps) { app ->
                val minutes = limits[app.packageName] ?: 15
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(app.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text("$minutes min", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { onLimitChange(app.packageName, it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Continue")
        }
    }
}

@Composable
private fun NextActionStep(
    label: String,
    url: String,
    onLabelChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingScaffold(
        title = "Your Redirect",
        subtitle = "When we interrupt you, where should we send you?",
        body = {
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text("Label (e.g. My Udemy course)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("URL (https://...)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        primaryLabel = "Continue",
        onPrimary = onNext,
        primaryEnabled = label.isNotBlank() && url.isNotBlank()
    )
}

@Composable
private fun PermissionsStep(
    hasUsage: Boolean,
    hasNotification: Boolean,
    isSaving: Boolean,
    onCheckPermissions: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onCheckPermissions() }

    // Re-check permissions when this step regains composition focus
    LaunchedEffect(Unit) { onCheckPermissions() }

    OnboardingScaffold(
        title = "Two Permissions",
        subtitle = "Focus Bridge needs these to work reliably.",
        body = {
            PermissionRow(
                label = "Usage Access",
                description = "See which apps you're using and for how long",
                granted = hasUsage,
                onGrant = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
            Spacer(Modifier.height(16.dp))
            PermissionRow(
                label = "Notifications",
                description = "Alert you when screen is off",
                granted = hasNotification,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Battery optimization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "For reliable monitoring on Samsung, Xiaomi, or Huawei, disable battery optimization for Focus Bridge in your phone's settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }) { Text("Open battery settings") }
                }
            }
        },
        primaryLabel = if (isSaving) "Setting up…" else "Start Monitoring",
        onPrimary = onComplete,
        primaryEnabled = hasUsage && !isSaving
    )
}

@Composable
private fun PermissionRow(
    label: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (granted) UnderLimitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!granted) {
            TextButton(onClick = onGrant) { Text("Grant") }
        }
    }
}

@Composable
private fun OnboardingScaffold(
    title: String,
    subtitle: String,
    body: @Composable ColumnScope.() -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(56.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(32.dp))
        body()
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(primaryLabel, style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(24.dp))
    }
}
