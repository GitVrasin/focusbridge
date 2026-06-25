package com.focusbridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.focusbridge.R
import com.focusbridge.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val SERVICE_CHANNEL_ID = "focusbridge_monitor"
        const val INTERVENTION_CHANNEL_ID = "focusbridge_intervention"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val INTERVENTION_NOTIFICATION_ID = 1002
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Focus Bridge Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification while Focus Bridge is monitoring your app usage"
                setShowBadge(false)
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                INTERVENTION_CHANNEL_ID,
                "Focus Interventions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you exceed your app time limit"
            }
        )
    }

    fun buildServiceNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Focus Bridge is watching")
            .setContentText("Monitoring app usage in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun buildInterventionNotification(
        appName: String,
        usageMinutes: Int,
        limitMinutes: Int,
        fullScreenIntent: PendingIntent
    ): Notification {
        return Notification.Builder(context, INTERVENTION_CHANNEL_ID)
            .setContentTitle("Time limit reached: $appName")
            .setContentText("You've used $appName for $usageMinutes min (limit: $limitMinutes min)")
            .setSmallIcon(R.drawable.ic_notification)
            .setFullScreenIntent(fullScreenIntent, true)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
    }

    fun cancelIntervention() {
        notificationManager.cancel(INTERVENTION_NOTIFICATION_ID)
    }

    fun postNotification(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }
}
