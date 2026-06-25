package com.focusbridge.ui.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.focusbridge.ui.theme.FocusBridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InterventionActivity : ComponentActivity() {

    private val viewModel: InterventionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read intent extras
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: packageName
        val usageMs = intent.getLongExtra(EXTRA_USAGE_MS, 0L)
        val limitMs = intent.getLongExtra(EXTRA_LIMIT_MS, 0L)
        val nextActionId = intent.getLongExtra(EXTRA_NEXT_ACTION_ID, -1L).takeIf { it >= 0 }
        val nextActionLabel = intent.getStringExtra(EXTRA_NEXT_ACTION_LABEL)
        val nextActionTarget = intent.getStringExtra(EXTRA_NEXT_ACTION_TARGET)
        val nextActionType = intent.getStringExtra(EXTRA_NEXT_ACTION_TYPE)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)

        viewModel.init(
            packageName = packageName,
            displayName = displayName,
            usageMs = usageMs,
            limitMs = limitMs,
            nextActionId = nextActionId,
            nextActionLabel = nextActionLabel,
            nextActionTarget = nextActionTarget,
            nextActionType = nextActionType,
            eventId = eventId,
        )

        // Intercept back press → treat as dismiss
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                viewModel.onDismiss()
            }
        })

        // Consume effects
        lifecycleScope.launch {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is InterventionEffect.OpenUrl -> openUrl(effect.url)
                    is InterventionEffect.Finish -> finish()
                }
            }
        }

        setContent {
            FocusBridgeTheme {
                InterventionScreen(viewModel = viewModel)
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "pkg"
        private const val EXTRA_DISPLAY_NAME = "display_name"
        private const val EXTRA_USAGE_MS = "usage_ms"
        private const val EXTRA_LIMIT_MS = "limit_ms"
        private const val EXTRA_NEXT_ACTION_ID = "next_action_id"
        private const val EXTRA_NEXT_ACTION_LABEL = "next_action_label"
        private const val EXTRA_NEXT_ACTION_TARGET = "next_action_target"
        private const val EXTRA_NEXT_ACTION_TYPE = "next_action_type"
        private const val EXTRA_EVENT_ID = "event_id"

        fun createIntent(
            context: Context,
            packageName: String,
            displayName: String,
            usageMs: Long,
            limitMs: Long,
            nextActionId: Long?,
            nextActionLabel: String?,
            nextActionTarget: String?,
            nextActionType: String?,
            eventId: Long
        ) = Intent(context, InterventionActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_DISPLAY_NAME, displayName)
            putExtra(EXTRA_USAGE_MS, usageMs)
            putExtra(EXTRA_LIMIT_MS, limitMs)
            nextActionId?.let { putExtra(EXTRA_NEXT_ACTION_ID, it) }
            nextActionLabel?.let { putExtra(EXTRA_NEXT_ACTION_LABEL, it) }
            nextActionTarget?.let { putExtra(EXTRA_NEXT_ACTION_TARGET, it) }
            nextActionType?.let { putExtra(EXTRA_NEXT_ACTION_TYPE, it) }
            putExtra(EXTRA_EVENT_ID, eventId)
        }
    }
}
