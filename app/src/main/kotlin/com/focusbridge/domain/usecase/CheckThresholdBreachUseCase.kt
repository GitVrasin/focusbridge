package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.DistractingApp
import javax.inject.Inject

data class BreachResult(
    val app: DistractingApp,
    val currentUsageMs: Long
)

class CheckThresholdBreachUseCase @Inject constructor() {

    /**
     * Per-app mode: returns BreachResult if cycle usage (time since last trigger) reached the threshold.
     * Cooldown is replaced by cycle-based reset — after every trigger the caller resets the cycle start.
     */
    fun execute(app: DistractingApp, cycleUsageMs: Long): BreachResult? {
        return if (cycleUsageMs >= app.dailyLimitMs) BreachResult(app, cycleUsageMs) else null
    }

    /**
     * Global mode: returns true if combined cycle usage across all apps reached the global limit.
     */
    fun executeGlobal(totalCycleUsageMs: Long, globalLimitMs: Long): Boolean {
        return totalCycleUsageMs >= globalLimitMs
    }
}
