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
import com.focusbridge.data.system.UsageStatsWrapper
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.UsageRepository
import com.focusbridge.domain.usecase.CheckThresholdBreachUseCase
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
    @Inject lateinit var interventionRepository: InterventionRepository
    @Inject lateinit var checkThresholdBreachUseCase: CheckThresholdBreachUseCase
    @Inject lateinit var getNextActionUseCase: GetNextActionUseCase
    @Inject lateinit var recordInterventionUseCase: RecordInterventionUseCase
    @Inject lateinit var interventionLauncher: InterventionLauncher
    @Inject lateinit var notificationHelper: ServiceNotificationHelper
    @Inject lateinit var userPrefs: UserPreferencesDataStore

    // In-memory cooldown guard: last trigger timestamp per package
    // Populated from DB on start, then updated in memory to avoid DB hit on every 2s tick
    private val lastTriggerCache = mutableMapOf<String, Long>()

    private var isMonitoring = false
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "FocusBridge.Monitor"
        private const val POLL_INTERVAL_MS = 2_000L
        private const val HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes
        private const val DATA_RETENTION_DAYS = 30

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
            // Check onboarding gate
            val onboardingComplete = userPrefs.isOnboardingComplete.first()
            if (!onboardingComplete) {
                Log.d(TAG, "Onboarding not complete — stopping service")
                stopSelf()
                return@launch
            }

            // Load initial cooldown state from DB to seed the in-memory cache
            appConfigRepository.getActiveApps().forEach { app ->
                lastTriggerCache[app.packageName] =
                    interventionRepository.getLastTriggerTime(app.packageName)
            }

            // Purge old usage records (keep 30 days)
            val cutoffDay = LocalDate.now().toEpochDay().toInt() - DATA_RETENTION_DAYS
            usageRepository.deleteOlderThan(cutoffDay)

            var lastTrackedDay = LocalDate.now().toEpochDay().toInt()

            while (isMonitoring) {
                val now = System.currentTimeMillis()
                val today = LocalDate.now().toEpochDay().toInt()

                // Day rollover: reset cooldown cache and run retention sweep
                if (today != lastTrackedDay) {
                    lastTriggerCache.clear()
                    usageRepository.deleteOlderThan(today - DATA_RETENTION_DAYS)
                    lastTrackedDay = today
                }

                // Record service liveness for health monitoring
                userPrefs.recordServicePing()

                if (!usageStatsWrapper.hasPermission()) {
                    Log.w(TAG, "Usage access permission revoked — pausing monitoring")
                    delay(10000)
                    continue
                }

                val todayStartMs = usageStatsWrapper.getTodayStartMs()
                val activeApps = appConfigRepository.getActiveApps()

                for (app in activeApps) {
                    val usageMs = usageStatsWrapper.queryTotalTimeMs(app.packageName, todayStartMs, now)
                    usageRepository.upsertRecord(app.packageName, today, usageMs)
                    checkAndTrigger(app, usageMs, now)
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkAndTrigger(app: DistractingApp, usageMs: Long, nowMs: Long) {
        val lastTrigger = lastTriggerCache[app.packageName] ?: 0L
        checkThresholdBreachUseCase.execute(app, usageMs, lastTrigger, nowMs)
            ?: return

        val nextAction = getNextActionUseCase()
        val eventId = recordInterventionUseCase.recordTriggered(app.packageName, usageMs)

        // Update in-memory cache immediately to prevent re-trigger in next tick
        lastTriggerCache[app.packageName] = nowMs

        acquireWakeLockForLaunch()
        interventionLauncher.trigger(app, usageMs, nextAction, eventId)

        Log.d(TAG, "Intervention triggered for ${app.packageName} at ${usageMs}ms usage")
    }

    private fun acquireWakeLockForLaunch() {
        wakeLock?.release()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusBridge:InterventionLaunch"
        ).also {
            it.acquire(5_000L) // Release after 5s — enough to launch Activity
        }
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
