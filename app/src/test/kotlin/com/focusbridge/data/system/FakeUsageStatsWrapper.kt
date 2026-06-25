package com.focusbridge.data.system

class FakeUsageStatsWrapper(
    private val permissionGranted: Boolean = true,
    private val todayStartMs: Long = System.currentTimeMillis() - 12 * 3600 * 1000L
) : UsageStatsWrapper {

    private val usageMap = mutableMapOf<String, Long>()

    fun setUsage(packageName: String, totalMs: Long) {
        usageMap[packageName] = totalMs
    }

    override fun hasPermission(): Boolean = permissionGranted

    override fun queryTotalTimeMs(packageName: String, beginMs: Long, endMs: Long): Long =
        usageMap[packageName] ?: 0L

    override fun getTodayStartMs(): Long = todayStartMs
}
