package com.focusbridge.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.net.toUri
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.model.NextActionType
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
        packageName: String,
        displayName: String,
        usageMs: Long,
        limitMs: Long,
        nextAction: NextAction?,
        eventId: Long,
        goalTitle: String? = null,
        sessionId: Long = 0L
    ) {
        val interventionIntent = InterventionActivity.createIntent(
            context = context,
            packageName = packageName,
            displayName = displayName,
            usageMs = usageMs,
            limitMs = limitMs,
            nextActionId = nextAction?.id,
            nextActionLabel = nextAction?.label,
            nextActionTarget = nextAction?.target,
            nextActionType = nextAction?.type?.name,
            eventId = eventId,
            sessionId = sessionId
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            interventionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionPendingIntent = nextAction?.target?.let { target ->
            val actionIntent = buildGoalActionIntent(target, nextAction.type)
            PendingIntent.getActivity(
                context,
                (eventId + 10_000).toInt(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val usageMin = (usageMs / 60_000).toInt().coerceAtLeast(1)

        val notification = notificationHelper.buildInterventionNotification(
            appName = displayName,
            usageMinutes = usageMin,
            goalText = goalTitle,
            actionLabel = nextAction?.label,
            actionPendingIntent = actionPendingIntent,
            fullScreenIntent = fullScreenPendingIntent
        )
        notificationHelper.postNotification(
            ServiceNotificationHelper.INTERVENTION_NOTIFICATION_ID,
            notification
        )

        if (powerManager.isInteractive) {
            runCatching { context.startActivity(interventionIntent) }
        }
    }

    private fun buildGoalActionIntent(target: String, type: NextActionType?): Intent {
        return when (type) {
            NextActionType.YOUTUBE -> {
                val videoId = Regex("""(?:v=|youtu\.be/|embed/|shorts/)([a-zA-Z0-9_-]{11})""")
                    .find(target)?.groupValues?.get(1)
                if (videoId != null) {
                    Intent(Intent.ACTION_VIEW, "vnd.youtube:$videoId".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } else {
                    Intent(Intent.ACTION_VIEW, target.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            NextActionType.SPOTIFY -> {
                Intent(Intent.ACTION_VIEW, target.toUri())
                    .setPackage("com.spotify.music")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            NextActionType.APP_INTENT -> {
                context.packageManager.getLaunchIntentForPackage(target)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ?: Intent(Intent.ACTION_VIEW, target.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            else -> Intent(Intent.ACTION_VIEW, target.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
