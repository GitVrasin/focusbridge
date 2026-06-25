package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import javax.inject.Inject

data class BreachResult(
    val app: DistractingApp,
    val currentUsageMs: Long
)

class CheckThresholdBreachUseCase @Inject constructor() {

    /**
     * Per-app mode: returns BreachResult if this app exceeded its daily limit AND cooldown passed.
     */
    fun execute(
        app: DistractingApp,
        currentUsageMs: Long,
        lastInterventionMs: Long,
        nowMs: Long
    ): BreachResult? {
        val limitExceeded = currentUsageMs >= app.dailyLimitMs
        // Strictly greater than: exactly-at-boundary does not re-trigger (avoids same-tick double-fire)
        val cooldownPassed = (nowMs - lastInterventionMs) > app.cooldownMs
        return if (limitExceeded && cooldownPassed) BreachResult(app, currentUsageMs) else null
    }

    /**
     * Global mode: returns true if combined usage across all apps exceeded the global limit AND
     * the cooldown since the last global intervention has passed.
     */
    fun executeGlobal(
        totalUsageMs: Long,
        globalLimitMs: Long,
        lastInterventionMs: Long,
        nowMs: Long,
        cooldownMs: Long = DistractingApp.DEFAULT_COOLDOWN_MS
    ): Boolean {
        val limitExceeded = totalUsageMs >= globalLimitMs
        val cooldownPassed = (nowMs - lastInterventionMs) > cooldownMs
        return limitExceeded && cooldownPassed
    }
}
