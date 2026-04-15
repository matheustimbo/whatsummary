package com.whatsummary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.whatsummary.ui.groups.GroupsScreen
import com.whatsummary.ui.home.HomeScreen
import com.whatsummary.ui.onboarding.OnboardingScreen
import com.whatsummary.ui.settings.SettingsScreen
import com.whatsummary.ui.summary.SummaryDetailScreen

@Composable
fun WhatsummaryNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onSummaryClick = { summaryId ->
                    navController.navigate(Screen.SummaryDetail.createRoute(summaryId))
                },
                onGroupsClick = {
                    navController.navigate(Screen.Groups.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.SummaryDetail.route,
            arguments = listOf(navArgument("summaryId") { type = NavType.LongType })
        ) {
            SummaryDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Groups.route) {
            GroupsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
