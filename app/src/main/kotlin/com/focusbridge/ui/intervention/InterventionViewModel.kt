package com.focusbridge.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.domain.repository.GoalRepository
import com.focusbridge.domain.usecase.ExtendLimitUseCase
import com.focusbridge.domain.usecase.RecordInterventionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InterventionUiState(
    val packageName: String = "",
    val appDisplayName: String = "",
    val usageMs: Long = 0L,
    val limitMs: Long = 0L,
    val nextActionId: Long? = null,
    val nextActionLabel: String? = null,
    val nextActionTarget: String? = null,
    val nextActionType: String? = null,
    val eventId: Long = -1L,
    val goalTitle: String = "",
    val isLoading: Boolean = false,
    val canExtend: Boolean = true
)

sealed interface InterventionEffect {
    data class OpenUrl(val url: String) : InterventionEffect
    data object Finish : InterventionEffect
}

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val recordInterventionUseCase: RecordInterventionUseCase,
    private val extendLimitUseCase: ExtendLimitUseCase,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterventionUiState())
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<InterventionEffect>()
    val effects: SharedFlow<InterventionEffect> = _effects.asSharedFlow()

    fun init(
        packageName: String,
        displayName: String,
        usageMs: Long,
        limitMs: Long,
        nextActionId: Long?,
        nextActionLabel: String?,
        nextActionTarget: String?,
        nextActionType: String?,
        eventId: Long
    ) {
        _uiState.update {
            it.copy(
                packageName = packageName,
                appDisplayName = displayName,
                usageMs = usageMs,
                limitMs = limitMs,
                nextActionId = nextActionId,
                nextActionLabel = nextActionLabel,
                nextActionTarget = nextActionTarget,
                nextActionType = nextActionType,
                eventId = eventId
            )
        }
        viewModelScope.launch {
            val goal = goalRepository.getActiveGoal()
            _uiState.update { it.copy(goalTitle = goal?.title ?: "") }
        }
    }

    fun onTakeAction() {
        viewModelScope.launch {
            val state = _uiState.value
            state.nextActionId?.let { actionId ->
                recordInterventionUseCase.recordAccepted(state.eventId, actionId)
            }
            state.nextActionTarget?.let { target ->
                _effects.emit(InterventionEffect.OpenUrl(target))
            }
            _effects.emit(InterventionEffect.Finish)
        }
    }

    fun onExtend() {
        viewModelScope.launch {
            val state = _uiState.value
            val packageName = state.packageName
            recordInterventionUseCase.recordExtension(packageName, state.usageMs)
            val result = extendLimitUseCase(packageName)
            val canExtend = result !is ExtendLimitUseCase.ExtendResult.LimitReached
            _uiState.update { it.copy(canExtend = canExtend) }
            _effects.emit(InterventionEffect.Finish)
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            val eventId = _uiState.value.eventId
            if (eventId >= 0) {
                recordInterventionUseCase.recordDismissed(eventId)
            }
            _effects.emit(InterventionEffect.Finish)
        }
    }
}
