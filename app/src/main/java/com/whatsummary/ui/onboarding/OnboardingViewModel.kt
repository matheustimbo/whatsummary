package com.whatsummary.ui.onboarding

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.api.AnthropicClient
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.worker.SummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val notificationPermissionGranted: Boolean = false,
    val apiKey: String = "",
    val apiKeyValid: Boolean? = null,
    val apiKeyTesting: Boolean = false,
    val summaryHour: Int = 22,
    val summaryMinute: Int = 0
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferences,
    private val anthropicClient: AnthropicClient,
    private val summaryScheduler: SummaryScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun checkNotificationPermission() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun onApiKeyChanged(key: String) {
        _uiState.update { it.copy(apiKey = key, apiKeyValid = null) }
    }

    fun testApiKey() {
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) return

        _uiState.update { it.copy(apiKeyTesting = true) }
        preferences.apiKey = key

        viewModelScope.launch {
            val valid = anthropicClient.testApiKey()
            _uiState.update { it.copy(apiKeyValid = valid, apiKeyTesting = false) }
            if (!valid) {
                preferences.apiKey = null
            }
        }
    }

    fun onTimeChanged(hour: Int, minute: Int) {
        _uiState.update { it.copy(summaryHour = hour, summaryMinute = minute) }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        preferences.apiKey = state.apiKey.trim()
        preferences.summaryTimeHour = state.summaryHour
        preferences.summaryTimeMinute = state.summaryMinute
        preferences.onboardingCompleted = true
        summaryScheduler.schedule()
    }
}
