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
        dailyLimitMs = 15 * 60 * 1000L,  // 15 minutes
        cooldownMs = 30 * 60 * 1000L      // 30 minutes
    )

    // 2 hours in ms — comfortably larger than testApp.cooldownMs (30 min) so "no prior trigger" tests pass
    private val now = 7_200_000L

    @Before
    fun setUp() {
        useCase = CheckThresholdBreachUseCase()
    }

    @Test
    fun `returns null when usage is below limit`() {
        val result = useCase.execute(testApp, 14 * 60_000L, 0L, now)
        assertNull(result)
    }

    @Test
    fun `returns null when usage exactly equals limit minus 1ms`() {
        val result = useCase.execute(testApp, testApp.dailyLimitMs - 1, 0L, now)
        assertNull(result)
    }

    @Test
    fun `returns BreachResult when usage equals limit and cooldown passed`() {
        val lastTrigger = now - testApp.cooldownMs - 1L
        val result = useCase.execute(testApp, testApp.dailyLimitMs, lastTrigger, now)
        assertNotNull(result)
        assertEquals(testApp, result!!.app)
        assertEquals(testApp.dailyLimitMs, result.currentUsageMs)
    }

    @Test
    fun `returns BreachResult when usage exceeds limit and no prior trigger`() {
        val result = useCase.execute(testApp, testApp.dailyLimitMs + 1000, 0L, now)
        assertNotNull(result)
    }

    @Test
    fun `returns null when limit exceeded but cooldown not passed`() {
        val lastTrigger = now - testApp.cooldownMs + 1L // 1ms before cooldown expires
        val result = useCase.execute(testApp, testApp.dailyLimitMs + 1000, lastTrigger, now)
        assertNull(result)
    }

    @Test
    fun `returns null when cooldown exactly has not passed`() {
        val lastTrigger = now - testApp.cooldownMs
        // cooldownMs = 30min, now - lastTrigger = 30min exactly → not > cooldown
        val result = useCase.execute(testApp, testApp.dailyLimitMs, lastTrigger, now)
        // (now - lastTrigger) = cooldownMs → NOT > cooldown → no breach
        assertNull(result)
    }

    @Test
    fun `returns BreachResult when cooldown just expired by 1ms`() {
        val lastTrigger = now - testApp.cooldownMs - 1L
        val result = useCase.execute(testApp, testApp.dailyLimitMs, lastTrigger, now)
        assertNotNull(result)
    }

    @Test
    fun `returns BreachResult for large usage values`() {
        val huggeUsage = Long.MAX_VALUE / 2
        val result = useCase.execute(testApp, huggeUsage, 0L, now)
        assertNotNull(result)
        assertEquals(huggeUsage, result!!.currentUsageMs)
    }
}
