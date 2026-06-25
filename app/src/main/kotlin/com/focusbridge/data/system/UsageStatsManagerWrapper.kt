package com.focusbridge.data.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface UsageStatsWrapper {
    fun hasPermission(): Boolean
    fun queryTotalTimeMs(packageName: String, beginMs: Long, endMs: Long): Long
    fun getTodayStartMs(): Long
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

    override fun getTodayStartMs(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
