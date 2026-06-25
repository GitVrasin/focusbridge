package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.model.InterventionEvent
import com.focusbridge.domain.model.UsageRecord
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.UsageRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class GetDailySummaryUseCaseTest {

    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var usageRepository: UsageRepository
    private lateinit var interventionRepository: InterventionRepository
    private lateinit var useCase: GetDailySummaryUseCase

    private val today = LocalDate.now().toEpochDay().toInt()

    private val instagram = DistractingApp(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        dailyLimitMs = 15 * 60_000L
    )
    private val tiktok = DistractingApp(
        packageName = "com.zhiliaoapp.musically",
        displayName = "TikTok",
        dailyLimitMs = 10 * 60_000L
    )

    @Before
    fun setUp() {
        appConfigRepository = mockk()
        usageRepository = mockk()
        interventionRepository = mockk()
        useCase = GetDailySummaryUseCase(appConfigRepository, usageRepository, interventionRepository)
    }

    @Test
    fun `summary reflects over-limit apps correctly`() = runTest {
        coEvery { appConfigRepository.getActiveApps() } returns listOf(instagram, tiktok)
        coEvery { usageRepository.getRecordsForDay(today) } returns listOf(
            UsageRecord(packageName = instagram.packageName, dateEpochDay = today, totalTimeMs = 20 * 60_000L), // over
            UsageRecord(packageName = tiktok.packageName, dateEpochDay = today, totalTimeMs = 5 * 60_000L)      // under
        )
        coEvery { interventionRepository.getEventsForDay(today) } returns listOf(
            InterventionEvent(packageName = instagram.packageName, triggeredAt = 0L, usageAtTriggerMs = 15 * 60_000L, wasAccepted = true),
            InterventionEvent(packageName = instagram.packageName, triggeredAt = 1L, usageAtTriggerMs = 16 * 60_000L, wasAccepted = false)
        )

        val summary = useCase(today)

        assertEquals(2, summary.appUsages.size)
        val igUsage = summary.appUsages.first { it.app.packageName == instagram.packageName }
        assertTrue(igUsage.isOverLimit)
        assertEquals(20 * 60_000L, igUsage.usageMs)

        val ttUsage = summary.appUsages.first { it.app.packageName == tiktok.packageName }
        assertFalse(ttUsage.isOverLimit)

        assertEquals(2, summary.totalInterventions)
        assertEquals(1, summary.totalAccepted)
        assertEquals(1, summary.totalDismissed)
    }

    @Test
    fun `app with no usage record shows zero`() = runTest {
        coEvery { appConfigRepository.getActiveApps() } returns listOf(instagram)
        coEvery { usageRepository.getRecordsForDay(today) } returns emptyList()
        coEvery { interventionRepository.getEventsForDay(today) } returns emptyList()

        val summary = useCase(today)

        val usage = summary.appUsages.first()
        assertEquals(0L, usage.usageMs)
        assertFalse(usage.isOverLimit)
    }

    @Test
    fun `empty app list produces empty summary`() = runTest {
        coEvery { appConfigRepository.getActiveApps() } returns emptyList()
        coEvery { usageRepository.getRecordsForDay(today) } returns emptyList()
        coEvery { interventionRepository.getEventsForDay(today) } returns emptyList()

        val summary = useCase(today)

        assertTrue(summary.appUsages.isEmpty())
        assertEquals(0, summary.totalInterventions)
    }
}
