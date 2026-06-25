package com.focusbridge.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.focusbridge.data.prefs.UserPreferencesDataStore
import com.focusbridge.data.system.AppForegroundState
import com.focusbridge.data.system.UsageStatsWrapper
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.GoalRepository
import com.focusbridge.domain.repository.SessionRepository
import com.focusbridge.domain.repository.UsageRepository
import com.focusbridge.domain.usecase.GetNextActionUseCase
import com.focusbridge.domain.usecase.RecordInterventionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class UsageMonitorService : LifecycleService() {

    @Inject lateinit var usageStatsWrapper: UsageStatsWrapper
    @Inject lateinit var appConfigRepository: AppConfigRepository
    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var goalRepository: GoalRepository
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var sessionTracker: SessionTracker
    @Inject lateinit var getNextActionUseCase: GetNextActionUseCase
    @Inject lateinit var recordInterventionUseCase: RecordInterventionUseCase
    @Inject lateinit var interventionLauncher: InterventionLauncher
    @Inject lateinit var notificationHelper: ServiceNotificationHelper
    @Inject lateinit var userPrefs: UserPreferencesDataStore

    private var isMonitoring = false
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "FocusBridge.Monitor"
        private const val POLL_INTERVAL_MS = 30_000L
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L
        private const val DATA_RETENTION_DAYS = 30
        // Window for querying foreground state: slightly larger than poll interval.
        private const val STATE_WINDOW_MS = POLL_INTERVAL_MS + 10_000L
        // Startup window to catch sessions already in progress.
        private const val STARTUP_WINDOW_MS = 10 * 60 * 1000L

        const val GLOBAL_KEY = "__global__"

        fun startIntent(context: Context) = Intent(context, UsageMonitorService::class.java)

        private const val ACTION_STOP = "com.focusbridge.STOP_MONITOR"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()

        val notification = notificationHelper.buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ServiceNotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(ServiceNotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        scheduleHeartbeat()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        wakeLock?.release()
        wakeLock = null
        Log.w(TAG, "Service destroyed — heartbeat will restart if needed")
    }

    private fun startMonitoringLoop() {
        if (isMonitoring) return
        isMonitoring = true

        lifecycleScope.launch {
            val onboardingComplete = userPrefs.isOnboardingComplete.first()
            if (!onboardingComplete) {
                Log.d(TAG, "Onboarding not complete — stopping service")
                stopSelf()
                return@launch
            }

            val todayStartMs = usageStatsWrapper.getTodayStartMs()
            val nowMs = System.currentTimeMillis()

            usageRepository.deleteOlderThan(LocalDate.now().toEpochDay().toInt() - DATA_RETENTION_DAYS)

            // Restore sessions that were open when the service last died (crash recovery).
            sessionRepository.getOpenSessions(todayStartMs).forEach { session ->
                sessionTracker.restoreSession(
                    packageName = session.packageName,
                    dbId = session.id,
                    startMs = session.sessionStartMs,
                    intentType = session.intentType
                )
            }

            // Detect sessions already in progress at startup (app opened before service started).
            val activeAppsAtStart = appConfigRepository.getActiveApps()
            for (app in activeAppsAtStart) {
                if (sessionTracker.getSession(app.packageName) != null) continue
                val state = usageStatsWrapper.queryAppState(app.packageName, STARTUP_WINDOW_MS, nowMs)
                if (state.state == AppForegroundState.FOREGROUND) {
                    val startMs = if (state.eventTimeMs > 0) state.eventTimeMs else nowMs
                    val dbId = sessionRepository.startSession(app.packageName, startMs)
                    sessionTracker.onForeground(app.packageName, nowMs, dbId)
                    Log.d(TAG, "Detected in-progress session at startup: ${app.packageName}")
                }
            }

            var lastTrackedDay = LocalDate.now().toEpochDay().toInt()

            while (isMonitoring) {
                val pollNow = System.currentTimeMillis()
                val today = LocalDate.now().toEpochDay().toInt()

                if (today != lastTrackedDay) {
                    // Day rollover: close all open sessions and reset.
                    sessionTracker.allSessions().forEach { (pkg, session) ->
                        sessionRepository.closeSession(
                            sessionId = session.dbId,
                            endMs = pollNow,
                            durationMs = pollNow - session.startMs,
                            interventionTriggered = session.interventionTriggered
                        )
                    }
                    sessionTracker.clearAll()
                    usageRepository.deleteOlderThan(today - DATA_RETENTION_DAYS)
                    lastTrackedDay = today
                }

                userPrefs.recordServicePing()

                if (!usageStatsWrapper.hasPermission()) {
                    Log.w(TAG, "Usage access permission revoked — pausing monitoring")
                    delay(10_000)
                    continue
                }

                val dayStartMs = usageStatsWrapper.getTodayStartMs()
                val activeApps = appConfigRepository.getActiveApps()
                val isMuted = userPrefs.muteUntilMs.first() > pollNow
                val isGlobalMode = userPrefs.isGlobalLimitMode.first()

                if (isGlobalMode) {
                    processGlobalMode(activeApps, dayStartMs, pollNow, isMuted)
                } else {
                    processPerAppMode(activeApps, dayStartMs, pollNow, isMuted)
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun processPerAppMode(
        activeApps: List<DistractingApp>,
        dayStartMs: Long,
        nowMs: Long,
        isMuted: Boolean
    ) {
        for (app in activeApps) {
            // Update daily usage record.
            val totalUsageMs = usageStatsWrapper.queryTotalTimeMs(app.packageName, dayStartMs, nowMs)
            usageRepository.upsertRecord(app.packageName, LocalDate.now().toEpochDay().toInt(), totalUsageMs)

            val appState = usageStatsWrapper.queryAppState(app.packageName, STATE_WINDOW_MS, nowMs)

            when (appState.state) {
                AppForegroundState.FOREGROUND -> {
                    val isNewSession = sessionTracker.onForeground(app.packageName, nowMs)
                    if (isNewSession) {
                        val dbId = sessionRepository.startSession(app.packageName, nowMs)
                        sessionTracker.setDbId(app.packageName, dbId)
                        Log.d(TAG, "Session started: ${app.packageName}")
                    }

                    if (!isMuted && !sessionTracker.hasInterventionTriggered(app.packageName)) {
                        val sessionDurationMs = sessionTracker.getSessionDurationMs(app.packageName, nowMs)
                        if (sessionDurationMs >= app.dailyLimitMs) {
                            fireIntervention(app.packageName, app.displayName, app.dailyLimitMs, sessionDurationMs, nowMs)
                        }
                    }
                }

                AppForegroundState.BACKGROUND, AppForegroundState.UNKNOWN -> {
                    val ended = sessionTracker.checkSessionEnd(app.packageName, nowMs)
                    if (ended != null) {
                        sessionRepository.closeSession(
                            sessionId = ended.dbId,
                            endMs = nowMs,
                            durationMs = nowMs - ended.startMs,
                            interventionTriggered = ended.interventionTriggered
                        )
                    }
                }
            }
        }
    }

    private suspend fun processGlobalMode(
        activeApps: List<DistractingApp>,
        dayStartMs: Long,
        nowMs: Long,
        isMuted: Boolean
    ) {
        val globalLimitMs = userPrefs.globalLimitMs.first()
        var anyInForeground = false
        var totalDailyUsageMs = 0L

        for (app in activeApps) {
            val totalUsageMs = usageStatsWrapper.queryTotalTimeMs(app.packageName, dayStartMs, nowMs)
            usageRepository.upsertRecord(app.packageName, LocalDate.now().toEpochDay().toInt(), totalUsageMs)
            totalDailyUsageMs += totalUsageMs

            val appState = usageStatsWrapper.queryAppState(app.packageName, STATE_WINDOW_MS, nowMs)
            if (appState.state == AppForegroundState.FOREGROUND) {
                anyInForeground = true
                sessionTracker.onForeground(app.packageName, nowMs)
            } else {
                sessionTracker.checkSessionEnd(app.packageName, nowMs)
            }
        }

        // Global: treat combined daily usage as the session value (no per-session reset needed).
        if (!isMuted && anyInForeground && !sessionTracker.hasInterventionTriggered(GLOBAL_KEY)) {
            if (totalDailyUsageMs >= globalLimitMs) {
                val appNames = activeApps.take(3).joinToString(" + ") { it.displayName }
                val displayName = if (activeApps.size > 3) "$appNames + ${activeApps.size - 3} more" else appNames

                sessionTracker.onForeground(GLOBAL_KEY, nowMs)
                val session = sessionTracker.getSession(GLOBAL_KEY)
                val dbId = session?.dbId?.takeIf { it > 0L }
                    ?: sessionRepository.startSession(GLOBAL_KEY, nowMs).also {
                        sessionTracker.setDbId(GLOBAL_KEY, it)
                    }

                val virtualApp = DistractingApp(
                    packageName = GLOBAL_KEY,
                    displayName = displayName,
                    dailyLimitMs = globalLimitMs,
                )
                fireIntervention(virtualApp.packageName, virtualApp.displayName, globalLimitMs, totalDailyUsageMs, nowMs)
            }
        }
    }

    private suspend fun fireIntervention(
        packageName: String,
        displayName: String,
        thresholdMs: Long,
        usageMs: Long,
        nowMs: Long
    ) {
        sessionTracker.markInterventionTriggered(packageName)
        val session = sessionTracker.getSession(packageName)
        val sessionId = session?.dbId ?: 0L

        if (sessionId > 0L) sessionRepository.markInterventionTriggered(sessionId)

        val nextAction = getNextActionUseCase()
        val goalTitle = goalRepository.getActiveGoal()?.title
        val eventId = recordInterventionUseCase.recordTriggered(packageName, usageMs, sessionId)

        acquireWakeLockForLaunch()
        interventionLauncher.trigger(
            packageName = packageName,
            displayName = displayName,
            usageMs = usageMs,
            limitMs = thresholdMs,
            nextAction = nextAction,
            eventId = eventId,
            goalTitle = goalTitle,
            sessionId = sessionId
        )

        Log.d(TAG, "Intervention fired: $packageName session=${usageMs}ms threshold=${thresholdMs}ms")
    }

    private fun acquireWakeLockForLaunch() {
        wakeLock?.release()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusBridge:InterventionLaunch"
        ).also { it.acquire(5_000L) }
    }

    private fun scheduleHeartbeat() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, HeartbeatReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMs = System.currentTimeMillis() + HEARTBEAT_INTERVAL_MS
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, intent)
        }
    }
}
