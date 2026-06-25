package com.focusbridge.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.focusbridge.data.prefs.UserPreferencesDataStore
import com.focusbridge.service.UsageMonitorService
import com.focusbridge.ui.navigation.AppNavHost
import com.focusbridge.ui.navigation.Screen
import com.focusbridge.ui.theme.FocusBridgeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPrefs: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusBridgeTheme {
                val isOnboardingComplete by userPrefs.isOnboardingComplete.collectAsState(initial = false)

                val startDestination = if (isOnboardingComplete) {
                    Screen.Dashboard.route
                } else {
                    Screen.Onboarding.route
                }

                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure service is running whenever the main UI is visible
        ensureMonitorServiceRunning()
    }

    private fun ensureMonitorServiceRunning() {
        val intent = UsageMonitorService.startIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
