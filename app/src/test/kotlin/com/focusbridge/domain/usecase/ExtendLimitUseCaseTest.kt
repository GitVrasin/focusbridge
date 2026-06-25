package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.InterventionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ExtendLimitUseCaseTest {

    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var interventionRepository: InterventionRepository
    private lateinit var useCase: ExtendLimitUseCase

    private val today = LocalDate.now().toEpochDay().toInt()
    private val testPackage = "com.instagram.android"
    private val testApp = DistractingApp(
        packageName = testPackage,
        displayName = "Instagram",
        dailyLimitMs = 15 * 60_000L
    )

    @Before
    fun setUp() {
        appConfigRepository = mockk()
        interventionRepository = mockk()
        useCase = ExtendLimitUseCase(appConfigRepository, interventionRepository)
    }

    @Test
    fun `extends limit when under max extensions`() = runTest {
        coEvery { interventionRepository.getExtensionCount(testPackage, today) } returns 0
        coEvery { appConfigRepository.getApp(testPackage) } returns testApp
        coEvery { appConfigRepository.updateDailyLimit(any(), any()) } returns Unit

        val result = useCase(testPackage)

        assertTrue(result is ExtendLimitUseCase.ExtendResult.Extended)
        val extended = result as ExtendLimitUseCase.ExtendResult.Extended
        assertEquals(testApp.dailyLimitMs + ExtendLimitUseCase.EXTENSION_MS, extended.newLimitMs)

        coVerify { appConfigRepository.updateDailyLimit(testPackage, testApp.dailyLimitMs + ExtendLimitUseCase.EXTENSION_MS) }
    }

    @Test
    fun `returns LimitReached when max extensions reached`() = runTest {
        coEvery { interventionRepository.getExtensionCount(testPackage, today) } returns ExtendLimitUseCase.MAX_EXTENSIONS_PER_DAY

        val result = useCase(testPackage)

        assertTrue(result is ExtendLimitUseCase.ExtendResult.LimitReached)
        coVerify(exactly = 0) { appConfigRepository.updateDailyLimit(any(), any()) }
    }

    @Test
    fun `returns AppNotFound when app does not exist`() = runTest {
        coEvery { interventionRepository.getExtensionCount(testPackage, today) } returns 0
        coEvery { appConfigRepository.getApp(testPackage) } returns null

        val result = useCase(testPackage)

        assertTrue(result is ExtendLimitUseCase.ExtendResult.AppNotFound)
    }

    @Test
    fun `allows last extension before hitting max`() = runTest {
        coEvery { interventionRepository.getExtensionCount(testPackage, today) } returns ExtendLimitUseCase.MAX_EXTENSIONS_PER_DAY - 1
        coEvery { appConfigRepository.getApp(testPackage) } returns testApp
        coEvery { appConfigRepository.updateDailyLimit(any(), any()) } returns Unit

        val result = useCase(testPackage)

        assertTrue(result is ExtendLimitUseCase.ExtendResult.Extended)
    }
}
