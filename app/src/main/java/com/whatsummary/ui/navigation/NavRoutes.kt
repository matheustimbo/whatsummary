package com.whatsummary.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object SummaryDetail : Screen("summary/{summaryId}") {
        fun createRoute(summaryId: Long) = "summary/$summaryId"
    }
    data object Groups : Screen("groups")
    data object GroupMessages : Screen("group_messages/{groupName}") {
        fun createRoute(groupName: String) = "group_messages/${Uri.encode(groupName)}"
    }
    data object Settings : Screen("settings")
}
