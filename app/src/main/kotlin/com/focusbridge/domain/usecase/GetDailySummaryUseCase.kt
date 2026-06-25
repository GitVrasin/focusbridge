package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.AppUsageSummary
import com.focusbridge.domain.model.DailySummary
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.UsageRepository
import java.time.LocalDate
import javax.inject.Inject

class GetDailySummaryUseCase @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val usageRepository: UsageRepository,
    private val interventionRepository: InterventionRepository
) {
    suspend operator fun invoke(dateEpochDay: Int = LocalDate.now().toEpochDay().toInt()): DailySummary {
        val apps = appConfigRepository.getActiveApps()
        val usageRecords = usageRepository.getRecordsForDay(dateEpochDay)
        val usageMap = usageRecords.associateBy { it.packageName }
        val events = interventionRepository.getEventsForDay(dateEpochDay)

        val appUsages = apps.map { app ->
            AppUsageSummary(
                app = app,
                usageMs = usageMap[app.packageName]?.totalTimeMs ?: 0L
            )
        }

        return DailySummary(
            dateEpochDay = dateEpochDay,
            appUsages = appUsages,
            totalInterventions = events.size,
            totalAccepted = events.count { it.wasAccepted == true },
            totalDismissed = events.count { it.wasAccepted == false }
        )
    }
}
