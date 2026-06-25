package com.focusbridge.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.data.prefs.UserPreferencesDataStore
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.model.NextActionType
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.GoalRepository
import com.focusbridge.ui.onboarding.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val goalRepository: GoalRepository,
    private val appConfigRepository: AppConfigRepository,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    val activeGoal: StateFlow<Goal?> = goalRepository.observeActiveGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeApps: StateFlow<List<DistractingApp>> = appConfigRepository.observeActiveApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Global limit mode ---

    val isGlobalLimitMode: StateFlow<Boolean> = userPrefs.isGlobalLimitMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val globalLimitMs: StateFlow<Long> = userPrefs.globalLimitMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30 * 60 * 1000L)

    fun setGlobalLimitMode(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setGlobalLimitMode(enabled) }
    }

    fun setGlobalLimitMs(limitMs: Long) {
        viewModelScope.launch { userPrefs.setGlobalLimitMs(limitMs) }
    }

    // --- Edit goal ---

    private val _editGoalTitle = MutableStateFlow("")
    val editGoalTitle: StateFlow<String> = _editGoalTitle.asStateFlow()

    private val _editGoalDesc = MutableStateFlow("")
    val editGoalDesc: StateFlow<String> = _editGoalDesc.asStateFlow()

    fun loadGoalForEdit() {
        viewModelScope.launch {
            val goal = goalRepository.getActiveGoal()
            _editGoalTitle.value = goal?.title ?: ""
            _editGoalDesc.value = goal?.description ?: ""
        }
    }

    fun setEditGoalTitle(v: String) = _editGoalTitle.update { v }
    fun setEditGoalDesc(v: String) = _editGoalDesc.update { v }

    fun saveGoal(onSaved: () -> Unit) {
        viewModelScope.launch {
            val current = goalRepository.getActiveGoal()
            goalRepository.upsertGoal(
                Goal(
                    id = current?.id ?: 0,
                    title = _editGoalTitle.value,
                    description = _editGoalDesc.value
                )
            )
            onSaved()
        }
    }

    // --- App limit management ---

    fun updateAppLimit(packageName: String, limitMs: Long) {
        viewModelScope.launch {
            appConfigRepository.updateDailyLimit(packageName, limitMs)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            appConfigRepository.removeApp(packageName)
        }
    }

    // --- Next actions ---

    private val _nextActions = MutableStateFlow<List<NextAction>>(emptyList())
    val nextActions: StateFlow<List<NextAction>> = _nextActions.asStateFlow()

    fun loadNextActions(goalId: Long) {
        viewModelScope.launch {
            _nextActions.value = goalRepository.getNextActionsForGoal(goalId)
        }
    }

    fun addNextAction(
        goalId: Long,
        label: String,
        target: String,
        type: NextActionType = NextActionType.URL
    ) {
        viewModelScope.launch {
            goalRepository.upsertNextAction(
                NextAction(
                    goalId = goalId,
                    label = label,
                    type = type,
                    target = target,
                    sortOrder = _nextActions.value.size
                )
            )
            loadNextActions(goalId)
        }
    }

    fun deleteNextAction(actionId: Long, goalId: Long) {
        viewModelScope.launch {
            goalRepository.deleteNextAction(actionId)
            loadNextActions(goalId)
        }
    }

    // --- Add apps flow (for AddAppsScreen) ---

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredInstalledApps: StateFlow<List<InstalledAppInfo>> =
        combine(_installedApps, _searchQuery) { apps, query ->
            if (query.isBlank()) apps
            else apps.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedToAdd = MutableStateFlow<Set<String>>(emptySet())
    val selectedToAdd: StateFlow<Set<String>> = _selectedToAdd.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val existingPkgs = appConfigRepository.getActiveApps().map { it.packageName }.toSet()

            // Query by launcher intent so YouTube, Gmail, Chrome etc. appear even though
            // they are technically system apps (FLAG_SYSTEM would exclude them).
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
                .filter { it !in existingPkgs && it != context.packageName }
                .mapNotNull { pkg ->
                    runCatching {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        InstalledAppInfo(
                            packageName = pkg,
                            displayName = pm.getApplicationLabel(appInfo).toString(),
                            isSuggested = pkg in DistractingApp.SUGGESTED_PACKAGES
                        )
                    }.getOrNull()
                }
                .sortedWith(
                    compareByDescending<InstalledAppInfo> { it.isSuggested }.thenBy { it.displayName }
                )
            _installedApps.value = apps
        }
    }

    fun setSearchQuery(query: String) = _searchQuery.update { query }

    fun toggleAppToAdd(packageName: String) {
        _selectedToAdd.update { set ->
            if (packageName in set) set - packageName else set + packageName
        }
    }

    fun saveSelectedApps(onDone: () -> Unit) {
        viewModelScope.launch {
            val toAdd = _selectedToAdd.value
            _installedApps.value
                .filter { it.packageName in toAdd }
                .forEach { app ->
                    appConfigRepository.upsertApp(
                        DistractingApp(
                            packageName = app.packageName,
                            displayName = app.displayName,
                            dailyLimitMs = 15 * 60 * 1000L
                        )
                    )
                }
            _selectedToAdd.value = emptySet()
            onDone()
        }
    }
}
