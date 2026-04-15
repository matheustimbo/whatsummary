package com.whatsummary.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object SummaryDetail : Screen("summary/{summaryId}") {
        fun createRoute(summaryId: Long) = "summary/$summaryId"
    }
    data object Groups : Screen("groups")
    data object Settings : Screen("settings")
}
