package com.focusbridge.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object DailySummary : Screen("daily_summary")
    data object Settings : Screen("settings")
    data object EditGoal : Screen("edit_goal")
    data object EditApps : Screen("edit_apps")
    data object AddApps : Screen("add_apps")
    data object LimitMode : Screen("limit_mode")
    data object EditNextActions : Screen("edit_next_actions/{goalId}") {
        fun route(goalId: Long) = "edit_next_actions/$goalId"
    }
}
