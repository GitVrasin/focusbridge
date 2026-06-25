package com.focusbridge.domain.model

data class DailySummary(
    val dateEpochDay: Int,
    val appUsages: List<AppUsageSummary>,
    val totalInterventions: Int,
    val totalAccepted: Int,
    val totalDismissed: Int
)

data class AppUsageSummary(
    val app: DistractingApp,
    val usageMs: Long,
    val isOverLimit: Boolean = usageMs >= app.dailyLimitMs
)
