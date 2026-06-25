package com.focusbridge.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusbridge.domain.usecase.GetWeeklyInsightsUseCase
import com.focusbridge.domain.usecase.WeeklyInsights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val insights: WeeklyInsights? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getWeeklyInsightsUseCase: GetWeeklyInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = InsightsUiState(isLoading = true)
            val insights = runCatching { getWeeklyInsightsUseCase() }.getOrNull()
            _uiState.value = InsightsUiState(insights = insights, isLoading = false)
        }
    }
}
