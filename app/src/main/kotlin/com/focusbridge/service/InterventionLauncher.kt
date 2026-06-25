package com.focusbridge.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.model.NextAction
import com.focusbridge.ui.intervention.InterventionActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterventionLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: ServiceNotificationHelper
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun trigger(
        app: DistractingApp,
        usageMs: Long,
        nextAction: NextAction?,
        eventId: Long
    ) {
        val intent = InterventionActivity.createIntent(
            context = context,
            packageName = app.packageName,
            displayName = app.displayName,
            usageMs = usageMs,
            limitMs = app.dailyLimitMs,
            nextActionId = nextAction?.id,
            nextActionLabel = nextAction?.label,
            nextActionTarget = nextAction?.target,
            nextActionType = nextAction?.type?.name,
            eventId = eventId
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (powerManager.isInteractive) {
            // Screen is on: launch full-screen Activity for immediate visual interruption
            context.startActivity(intent)
        } else {
            // Screen is off: use full-screen intent notification so it appears on lock screen
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                eventId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val usageMin = (usageMs / 60_000).toInt()
            val limitMin = (app.dailyLimitMs / 60_000).toInt()
            val notification = notificationHelper.buildInterventionNotification(
                appName = app.displayName,
                usageMinutes = usageMin,
                limitMinutes = limitMin,
                fullScreenIntent = fullScreenPendingIntent
            )
            notificationHelper.postNotification(
                ServiceNotificationHelper.INTERVENTION_NOTIFICATION_ID,
                notification
            )
        }
    }
}
