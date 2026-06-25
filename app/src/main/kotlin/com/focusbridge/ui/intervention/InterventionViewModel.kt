package com.focusbridge.ui.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.data.prefs.UserPreferencesDataStore
import com.focusbridge.domain.model.SessionIntent
import com.focusbridge.domain.repository.GoalRepository
import com.focusbridge.domain.usecase.RecordInterventionUseCase
import com.focusbridge.service.UsageMonitorService
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

enum class InterventionPhase { INTENT_CAPTURE, INTERVENTION }

data class InterventionUiState(
    val phase: InterventionPhase = InterventionPhase.INTENT_CAPTURE,
    val packageName: String = "",
    val appDisplayName: String = "",
    val sessionDurationMs: Long = 0L,
    val limitMs: Long = 0L,
    val selectedIntent: SessionIntent? = null,
    val nextActionId: Long? = null,
    val nextActionLabel: String? = null,
    val nextActionTarget: String? = null,
    val nextActionType: String? = null,
    val eventId: Long = -1L,
    val sessionId: Long = 0L,
    val goalTitle: String = "",
    val isGlobalMode: Boolean = false
)

sealed interface InterventionEffect {
    data class OpenAction(val target: String, val type: String?) : InterventionEffect
    data object Finish : InterventionEffect
}

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val recordInterventionUseCase: RecordInterventionUseCase,
    private val goalRepository: GoalRepository,
    private val userPrefs: UserPreferencesDataStore
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
        eventId: Long,
        sessionId: Long = 0L
    ) {
        val isGlobal = packageName == UsageMonitorService.GLOBAL_KEY
        _uiState.update {
            it.copy(
                packageName = packageName,
                appDisplayName = displayName,
                sessionDurationMs = usageMs,
                limitMs = limitMs,
                nextActionId = nextActionId,
                nextActionLabel = nextActionLabel,
                nextActionTarget = nextActionTarget,
                nextActionType = nextActionType,
                eventId = eventId,
                sessionId = sessionId,
                isGlobalMode = isGlobal,
                // Global mode skips intent capture — it tracks combined time, not a single session open.
                phase = if (isGlobal) InterventionPhase.INTERVENTION else InterventionPhase.INTENT_CAPTURE
            )
        }
        viewModelScope.launch {
            val goal = goalRepository.getActiveGoal()
            _uiState.update { it.copy(goalTitle = goal?.title ?: "") }
        }
    }

    fun onIntentSelected(intent: SessionIntent) {
        viewModelScope.launch {
            val state = _uiState.value
            recordInterventionUseCase.recordIntentSelected(state.eventId, state.sessionId, intent.name)
            _uiState.update {
                it.copy(
                    selectedIntent = intent,
                    phase = InterventionPhase.INTERVENTION
                )
            }
        }
    }

    fun onGoToGoal() {
        viewModelScope.launch {
            val state = _uiState.value
            state.nextActionId?.let { actionId ->
                recordInterventionUseCase.recordAccepted(state.eventId, actionId)
            }
            state.nextActionTarget?.let { target ->
                _effects.emit(InterventionEffect.OpenAction(target, state.nextActionType))
            }
            _effects.emit(InterventionEffect.Finish)
        }
    }

    fun onContinue() {
        viewModelScope.launch {
            val eventId = _uiState.value.eventId
            if (eventId >= 0) {
                recordInterventionUseCase.recordDismissed(eventId)
            }
            _effects.emit(InterventionEffect.Finish)
        }
    }

    // Back press on intent capture screen also counts as continue (user goes back to the app).
    fun onDismiss() = onContinue()

    fun onMuteForToday() {
        viewModelScope.launch {
            userPrefs.muteForToday()
            val eventId = _uiState.value.eventId
            if (eventId >= 0) {
                recordInterventionUseCase.recordDismissed(eventId)
            }
            _effects.emit(InterventionEffect.Finish)
        }
    }
}
