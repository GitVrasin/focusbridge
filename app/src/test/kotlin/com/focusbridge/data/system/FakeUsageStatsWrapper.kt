package com.focusbridge.data.system

class FakeUsageStatsWrapper(
    private val permissionGranted: Boolean = true,
    private val todayStartMs: Long = System.currentTimeMillis() - 12 * 3600 * 1000L
) : UsageStatsWrapper {

    private val usageMap = mutableMapOf<String, Long>()
    private val appStateMap = mutableMapOf<String, AppStateSnapshot>()

    fun setUsage(packageName: String, totalMs: Long) {
        usageMap[packageName] = totalMs
    }

    fun setAppState(packageName: String, state: AppForegroundState, eventTimeMs: Long = System.currentTimeMillis()) {
        appStateMap[packageName] = AppStateSnapshot(state, eventTimeMs)
    }

    override fun hasPermission(): Boolean = permissionGranted

    override fun queryTotalTimeMs(packageName: String, beginMs: Long, endMs: Long): Long =
        usageMap[packageName] ?: 0L

    override fun queryCycleUsageMs(packageName: String, fromMs: Long, toMs: Long): Long =
        usageMap[packageName] ?: 0L

    override fun queryAppState(packageName: String, windowMs: Long, nowMs: Long): AppStateSnapshot =
        appStateMap[packageName] ?: AppStateSnapshot(AppForegroundState.UNKNOWN)

    override fun getTodayStartMs(): Long = todayStartMs
}
