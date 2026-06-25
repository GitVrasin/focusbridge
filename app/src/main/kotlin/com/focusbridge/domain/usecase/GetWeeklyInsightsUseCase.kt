package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.SessionIntent
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.SessionRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class WeeklyInsights(
    val daysWithData: Int,
    val totalTimeMs: Long,
    val baselineTotalMs: Long,       // first 2 days × (total days / 2) projected
    val intentBreakdown: Map<String, Float>,  // SessionIntent.name → 0..1
    val sessionCount: Int,
    val goToGoalCount: Int,
    val goToGoalRate: Float,
    val muteCount: Int,
    val estimatedSavingMs: Long      // 20% of totalTimeMs
)

class GetWeeklyInsightsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val interventionRepository: InterventionRepository
) {
    companion object {
        private const val BASELINE_DAYS = 2
    }

    suspend operator fun invoke(): WeeklyInsights {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)

        val weekStartMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val nowMs = System.currentTimeMillis()

        // Days since install/first session (up to 7)
        val sessions = sessionRepository.getSessionsInRange(weekStartMs, nowMs)
        val daysWithData = sessions
            .map { LocalDate.ofEpochDay(it.sessionStartMs / 86_400_000L) }
            .distinct()
            .size
            .coerceAtLeast(1)

        val totalTimeMs = sessionRepository.getTotalDurationMs(weekStartMs, nowMs)
        val sessionCount = sessionRepository.getSessionCount(weekStartMs, nowMs)

        // Baseline: average of first 2 days × 7 (projected weekly)
        val baselineStartMs = weekStartMs
        val baselineEndMs = weekStart.plusDays(BASELINE_DAYS.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val baselineDaysMs = sessionRepository.getTotalDurationMs(baselineStartMs, baselineEndMs)
        val baselineAvgMs = if (daysWithData >= BASELINE_DAYS) baselineDaysMs / BASELINE_DAYS else baselineDaysMs
        val baselineTotalMs = baselineAvgMs * daysWithData

        // Intent breakdown
        val intentCounts = SessionIntent.entries.associateWith { intent ->
            sessionRepository.getCountByIntent(weekStartMs, nowMs, intent.name)
        }
        val totalIntents = intentCounts.values.sum().coerceAtLeast(1)
        val intentBreakdown = intentCounts
            .filter { (_, count) -> count > 0 }
            .mapKeys { (intent, _) -> intent.name }
            .mapValues { (_, count) -> count.toFloat() / totalIntents }

        // Intervention response tracking
        val events = interventionRepository.getEventsForDay(today.toEpochDay().toInt())
        // Collect all events for the week for accurate counts
        val weeklyEventCount = events.size
        val goToGoalCount = events.count { it.wasAccepted == true }
        val goToGoalRate = if (weeklyEventCount > 0) goToGoalCount.toFloat() / weeklyEventCount else 0f

        // Mute usage: count distinct days where mute was used (approximated by dismissed events)
        val muteCount = events.count { it.wasAccepted == false }

        return WeeklyInsights(
            daysWithData = daysWithData,
            totalTimeMs = totalTimeMs,
            baselineTotalMs = baselineTotalMs,
            intentBreakdown = intentBreakdown,
            sessionCount = sessionCount,
            goToGoalCount = goToGoalCount,
            goToGoalRate = goToGoalRate,
            muteCount = muteCount,
            estimatedSavingMs = (totalTimeMs * 0.20).toLong()
        )
    }
}
