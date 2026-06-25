package com.focusbridge.data.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

enum class AppForegroundState { FOREGROUND, BACKGROUND, UNKNOWN }

data class AppStateSnapshot(
    val state: AppForegroundState,
    val eventTimeMs: Long = 0L
)

interface UsageStatsWrapper {
    fun hasPermission(): Boolean
    fun queryTotalTimeMs(packageName: String, beginMs: Long, endMs: Long): Long
    fun queryCycleUsageMs(packageName: String, fromMs: Long, toMs: Long): Long
    fun getTodayStartMs(): Long
    /** Returns the last known foreground/background state of [packageName] in the given window. */
    fun queryAppState(packageName: String, windowMs: Long, nowMs: Long): AppStateSnapshot
}

@Singleton
class SystemUsageStatsWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageStatsWrapper {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val appOpsManager: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    override fun hasPermission(): Boolean {
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun queryTotalTimeMs(packageName: String, beginMs: Long, endMs: Long): Long {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginMs,
            endMs
        )
        return stats
            .filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
    }

    @Suppress("DEPRECATION")
    override fun queryCycleUsageMs(packageName: String, fromMs: Long, toMs: Long): Long {
        if (fromMs >= toMs) return 0L
        val events = usageStatsManager.queryEvents(fromMs, toMs) ?: return 0L
        val event = UsageEvents.Event()
        var totalMs = 0L
        var sessionStart = -1L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (sessionStart < 0) sessionStart = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (sessionStart >= 0) {
                        totalMs += event.timeStamp - sessionStart
                        sessionStart = -1L
                    }
                }
            }
        }
        if (sessionStart >= 0) totalMs += toMs - sessionStart
        return totalMs
    }

    @Suppress("DEPRECATION")
    override fun queryAppState(packageName: String, windowMs: Long, nowMs: Long): AppStateSnapshot {
        val fromMs = nowMs - windowMs
        val events = usageStatsManager.queryEvents(fromMs, nowMs)
            ?: return AppStateSnapshot(AppForegroundState.UNKNOWN)
        val event = UsageEvents.Event()
        var lastState = AppForegroundState.UNKNOWN
        var lastEventTimeMs = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastState = AppForegroundState.FOREGROUND
                    lastEventTimeMs = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    lastState = AppForegroundState.BACKGROUND
                    lastEventTimeMs = event.timeStamp
                }
            }
        }
        return AppStateSnapshot(lastState, lastEventTimeMs)
    }

    override fun getTodayStartMs(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
