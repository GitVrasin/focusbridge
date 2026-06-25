package com.focusbridge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.focusbridge.ui.dashboard.DashboardScreen
import com.focusbridge.ui.onboarding.OnboardingScreen
import com.focusbridge.ui.insights.InsightsScreen
import com.focusbridge.ui.settings.AddAppsScreen
import com.focusbridge.ui.settings.DailySummaryScreen
import com.focusbridge.ui.settings.EditAppsScreen
import com.focusbridge.ui.settings.EditGoalScreen
import com.focusbridge.ui.settings.EditNextActionsScreen
import com.focusbridge.ui.settings.LimitModeScreen
import com.focusbridge.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onOpenSummary = { navController.navigate(Screen.DailySummary.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.DailySummary.route) {
            DailySummaryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditGoal = { navController.navigate(Screen.EditGoal.route) },
                onEditApps = { navController.navigate(Screen.EditApps.route) },
                onEditNextActions = { goalId ->
                    navController.navigate(Screen.EditNextActions.route(goalId))
                },
                onEditLimitMode = { navController.navigate(Screen.LimitMode.route) },
                onOpenInsights = { navController.navigate(Screen.Insights.route) }
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.EditGoal.route) {
            EditGoalScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.EditApps.route) {
            EditAppsScreen(
                onBack = { navController.popBackStack() },
                onAddApps = { navController.navigate(Screen.AddApps.route) }
            )
        }

        composable(Screen.AddApps.route) {
            AddAppsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.LimitMode.route) {
            LimitModeScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.EditNextActions.route,
            arguments = listOf(navArgument("goalId") { type = NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId") ?: return@composable
            EditNextActionsScreen(goalId = goalId, onBack = { navController.popBackStack() })
        }
    }
}
