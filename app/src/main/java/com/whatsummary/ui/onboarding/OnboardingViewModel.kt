package com.whatsummary.ui.onboarding

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.api.AnthropicClient
import com.whatsummary.data.llm.ModelDownloadManager
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
    // Step 2: Mode selection
    val inferenceMode: String = UserPreferences.MODE_LOCAL,
    // Step 3a: Local model download
    val modelDownloadProgress: Float = 0f,
    val modelDownloaded: Boolean = false,
    val modelDownloading: Boolean = false,
    val modelDownloadError: String? = null,
    // Step 3b: API key (if API mode)
    val apiKey: String = "",
    val apiKeyValid: Boolean? = null,
    val apiKeyTesting: Boolean = false,
    // Step 4: Schedule
    val summaryHour: Int = 22,
    val summaryMinute: Int = 0
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferences,
    private val anthropicClient: AnthropicClient,
    private val summaryScheduler: SummaryScheduler,
    private val modelDownloadManager: ModelDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(
        modelDownloaded = modelDownloadManager.isModelDownloaded()
    ))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            modelDownloadManager.downloadState.collect { state ->
                when (state) {
                    is ModelDownloadManager.DownloadState.Downloading -> {
                        _uiState.update {
                            it.copy(
                                modelDownloading = true,
                                modelDownloadProgress = state.progress,
                                modelDownloadError = null
                            )
                        }
                    }
                    is ModelDownloadManager.DownloadState.Completed -> {
                        _uiState.update {
                            it.copy(modelDownloaded = true, modelDownloading = false)
                        }
                    }
                    is ModelDownloadManager.DownloadState.Error -> {
                        _uiState.update {
                            it.copy(modelDownloading = false, modelDownloadError = state.message)
                        }
                    }
                    is ModelDownloadManager.DownloadState.Idle -> {}
                }
            }
        }
    }

    fun checkNotificationPermission() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun onInferenceModeChanged(mode: String) {
        _uiState.update { it.copy(inferenceMode = mode) }
    }

    fun downloadModel() {
        viewModelScope.launch {
            modelDownloadManager.downloadModel()
        }
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

    fun canAdvanceFromStep(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            1 -> state.notificationPermissionGranted
            2 -> true // Mode selection is always valid
            3 -> when (state.inferenceMode) {
                UserPreferences.MODE_LOCAL -> state.modelDownloaded
                UserPreferences.MODE_API -> state.apiKeyValid == true
                else -> false
            }
            else -> true
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        preferences.inferenceMode = state.inferenceMode
        if (state.inferenceMode == UserPreferences.MODE_API) {
            preferences.apiKey = state.apiKey.trim()
        }
        preferences.summaryTimeHour = state.summaryHour
        preferences.summaryTimeMinute = state.summaryMinute
        preferences.onboardingCompleted = true
        summaryScheduler.schedule()
    }
}
