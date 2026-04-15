package com.whatsummary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.ui.navigation.Screen
import com.whatsummary.ui.navigation.WhatsummaryNavHost
import com.whatsummary.ui.theme.WhatsummaryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (preferences.onboardingCompleted) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        setContent {
            WhatsummaryTheme {
                val navController = rememberNavController()
                WhatsummaryNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
