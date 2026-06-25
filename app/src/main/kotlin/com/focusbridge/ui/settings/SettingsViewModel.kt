package com.focusbridge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.model.NextActionType
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val appConfigRepository: AppConfigRepository
) : ViewModel() {

    val activeGoal: StateFlow<Goal?> = goalRepository.observeActiveGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeApps: StateFlow<List<DistractingApp>> = appConfigRepository.observeActiveApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Edit goal
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

    // Next actions
    private val _nextActions = MutableStateFlow<List<NextAction>>(emptyList())
    val nextActions: StateFlow<List<NextAction>> = _nextActions.asStateFlow()

    fun loadNextActions(goalId: Long) {
        viewModelScope.launch {
            _nextActions.value = goalRepository.getNextActionsForGoal(goalId)
        }
    }

    fun addNextAction(goalId: Long, label: String, url: String) {
        viewModelScope.launch {
            goalRepository.upsertNextAction(
                NextAction(
                    goalId = goalId,
                    label = label,
                    type = NextActionType.URL,
                    target = url,
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
}
