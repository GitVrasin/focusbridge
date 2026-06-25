package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import javax.inject.Inject

data class BreachResult(
    val app: DistractingApp,
    val currentUsageMs: Long
)

class CheckThresholdBreachUseCase @Inject constructor() {

    /**
     * Returns a BreachResult if the app has exceeded its daily limit AND the cooldown
     * period since the last intervention has passed. Returns null if no breach.
     */
    fun execute(
        app: DistractingApp,
        currentUsageMs: Long,
        lastInterventionMs: Long,
        nowMs: Long
    ): BreachResult? {
        val limitExceeded = currentUsageMs >= app.dailyLimitMs
        val cooldownPassed = (nowMs - lastInterventionMs) >= app.cooldownMs
        return if (limitExceeded && cooldownPassed) BreachResult(app, currentUsageMs) else null
    }
}
