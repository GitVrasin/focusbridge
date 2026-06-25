package com.focusbridge.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.data.prefs.UserPreferencesDataStore
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.model.NextActionType
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val displayName: String,
    val isSuggested: Boolean
)

data class OnboardingUiState(
    val currentStep: Int = 0,
    val goalTitle: String = "",
    val goalDescription: String = "",
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val appLimitsMinutes: Map<String, Int> = emptyMap(),
    val nextActionLabel: String = "",
    val nextActionUrl: String = "",
    val hasUsagePermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val appConfigRepository: AppConfigRepository,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        checkPermissions()
    }

    fun setGoalTitle(title: String) = _uiState.update { it.copy(goalTitle = title) }
    fun setGoalDescription(desc: String) = _uiState.update { it.copy(goalDescription = desc) }

    fun toggleApp(packageName: String) {
        _uiState.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            if (packageName in selected) selected.remove(packageName)
            else selected.add(packageName)
            val limits = state.appLimitsMinutes.toMutableMap()
            if (packageName !in limits) limits[packageName] = 15 // default 15 min
            state.copy(selectedPackages = selected, appLimitsMinutes = limits)
        }
    }

    fun setAppLimit(packageName: String, minutes: Int) {
        _uiState.update { state ->
            state.copy(appLimitsMinutes = state.appLimitsMinutes + (packageName to minutes))
        }
    }

    fun setNextActionLabel(label: String) = _uiState.update { it.copy(nextActionLabel = label) }
    fun setNextActionUrl(url: String) = _uiState.update { it.copy(nextActionUrl = url) }

    fun checkPermissions() {
        val hasUsage = checkUsagePermission()
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true

        _uiState.update { it.copy(hasUsagePermission = hasUsage, hasNotificationPermission = hasNotif) }
    }

    fun nextStep() = _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    fun prevStep() = _uiState.update { if (it.currentStep > 0) it.copy(currentStep = it.currentStep - 1) else it }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value

            // Save goal
            val goalId = goalRepository.upsertGoal(
                Goal(title = state.goalTitle, description = state.goalDescription)
            )

            // Save next action
            if (state.nextActionLabel.isNotBlank() && state.nextActionUrl.isNotBlank()) {
                goalRepository.upsertNextAction(
                    NextAction(
                        goalId = goalId,
                        label = state.nextActionLabel,
                        type = NextActionType.URL,
                        target = state.nextActionUrl,
                        sortOrder = 0
                    )
                )
            }

            // Save selected apps with limits
            state.selectedPackages.forEach { pkg ->
                val appInfo = state.installedApps.find { it.packageName == pkg } ?: return@forEach
                val limitMin = state.appLimitsMinutes[pkg] ?: 15
                appConfigRepository.upsertApp(
                    DistractingApp(
                        packageName = pkg,
                        displayName = appInfo.displayName,
                        dailyLimitMs = limitMin * 60 * 1000L
                    )
                )
            }

            userPrefs.setOnboardingComplete(true)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { appInfo ->
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        displayName = pm.getApplicationLabel(appInfo).toString(),
                        isSuggested = appInfo.packageName in DistractingApp.SUGGESTED_PACKAGES
                    )
                }
                .sortedWith(compareByDescending<InstalledAppInfo> { it.isSuggested }.thenBy { it.displayName })

            val preSelected = apps.filter { it.isSuggested }.map { it.packageName }.toSet()
            val defaultLimits = preSelected.associateWith { 15 }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    installedApps = apps,
                    selectedPackages = preSelected,
                    appLimitsMinutes = defaultLimits
                )
            }
        }
    }

    private fun checkUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
