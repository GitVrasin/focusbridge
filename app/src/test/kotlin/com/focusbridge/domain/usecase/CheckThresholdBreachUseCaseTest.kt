package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CheckThresholdBreachUseCaseTest {

    private lateinit var useCase: CheckThresholdBreachUseCase

    private val testApp = DistractingApp(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        dailyLimitMs = 2 * 60 * 1000L,  // 2 minutes (minimum supported threshold)
        cooldownMs = 0L                  // cooldown is obsolete; cycle-based tracking handles re-trigger
    )

    @Before
    fun setUp() {
        useCase = CheckThresholdBreachUseCase()
    }

    @Test
    fun `returns null when cycle usage is below limit`() {
        val result = useCase.execute(testApp, testApp.dailyLimitMs - 1)
        assertNull(result)
    }

    @Test
    fun `returns null when cycle usage is zero`() {
        val result = useCase.execute(testApp, 0L)
        assertNull(result)
    }

    @Test
    fun `returns BreachResult when cycle usage exactly equals limit`() {
        val result = useCase.execute(testApp, testApp.dailyLimitMs)
        assertNotNull(result)
        assertEquals(testApp, result!!.app)
        assertEquals(testApp.dailyLimitMs, result.currentUsageMs)
    }

    @Test
    fun `returns BreachResult when cycle usage exceeds limit`() {
        val result = useCase.execute(testApp, testApp.dailyLimitMs + 1000)
        assertNotNull(result)
        assertEquals(testApp.dailyLimitMs + 1000, result!!.currentUsageMs)
    }

    @Test
    fun `returns BreachResult for large cycle usage values`() {
        val hugeUsage = Long.MAX_VALUE / 2
        val result = useCase.execute(testApp, hugeUsage)
        assertNotNull(result)
        assertEquals(hugeUsage, result!!.currentUsageMs)
    }

    @Test
    fun `executeGlobal returns false when combined cycle usage is below limit`() {
        val result = useCase.executeGlobal(totalCycleUsageMs = 5_000L, globalLimitMs = 10_000L)
        assertFalse(result)
    }

    @Test
    fun `executeGlobal returns true when combined cycle usage equals limit`() {
        val result = useCase.executeGlobal(totalCycleUsageMs = 10_000L, globalLimitMs = 10_000L)
        assertTrue(result)
    }

    @Test
    fun `executeGlobal returns true when combined cycle usage exceeds limit`() {
        val result = useCase.executeGlobal(totalCycleUsageMs = 15_000L, globalLimitMs = 10_000L)
        assertTrue(result)
    }

    @Test
    fun `cycle reset allows re-trigger immediately after reset`() {
        // Simulate: trigger happened (caller reset cycle), next cycle usage = 0 → no breach
        val afterResetUsage = 0L
        val result = useCase.execute(testApp, afterResetUsage)
        assertNull(result)
    }

    @Test
    fun `re-triggers when cycle usage reaches threshold again after reset`() {
        // Second cycle completes the threshold
        val result = useCase.execute(testApp, testApp.dailyLimitMs)
        assertNotNull(result)
    }
}
