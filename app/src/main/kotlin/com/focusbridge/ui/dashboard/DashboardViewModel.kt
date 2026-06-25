package com.focusbridge.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.data.system.UsageStatsWrapper
import com.focusbridge.domain.model.DailySummary
import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.usecase.GetDailySummaryUseCase
import com.focusbridge.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val summary: DailySummary? = null,
    val hasUsagePermission: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val goalRepository: GoalRepository,
    private val usageStatsWrapper: UsageStatsWrapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val activeGoal: StateFlow<Goal?> = goalRepository.observeActiveGoal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSummary()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                loadSummary()
                delay(10_000L) // Refresh every 10s
            }
        }
    }

    private suspend fun loadSummary() {
        val hasPermission = usageStatsWrapper.hasPermission()
        val summary = if (hasPermission) getDailySummaryUseCase() else null
        _uiState.update {
            it.copy(summary = summary, hasUsagePermission = hasPermission)
        }
    }
}
