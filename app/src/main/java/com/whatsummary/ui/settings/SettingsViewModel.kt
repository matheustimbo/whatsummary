package com.whatsummary.ui.settings

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.data.repository.GroupRepository
import com.whatsummary.data.repository.SummaryRepository
import com.whatsummary.worker.SummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

data class SettingsUiState(
    val summaryHour: Int = 22,
    val summaryMinute: Int = 0,
    val hasApiKey: Boolean = false,
    val llmModel: String = UserPreferences.MODEL_HAIKU,
    val retentionDays: Int = 30,
    val notificationServiceActive: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showTimePicker: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferences,
    private val summaryScheduler: SummaryScheduler,
    private val summaryRepository: SummaryRepository,
    private val groupRepository: GroupRepository,
    private val moshi: Moshi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        val serviceActive = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

        _uiState.update {
            it.copy(
                summaryHour = preferences.summaryTimeHour,
                summaryMinute = preferences.summaryTimeMinute,
                hasApiKey = !preferences.apiKey.isNullOrBlank(),
                llmModel = preferences.llmModel,
                retentionDays = preferences.retentionDays,
                notificationServiceActive = serviceActive
            )
        }
    }

    fun updateSummaryTime(hour: Int, minute: Int) {
        preferences.summaryTimeHour = hour
        preferences.summaryTimeMinute = minute
        summaryScheduler.schedule()
        _uiState.update { it.copy(summaryHour = hour, summaryMinute = minute, showTimePicker = false) }
    }

    fun updateApiKey(key: String) {
        preferences.apiKey = key.ifBlank { null }
        _uiState.update { it.copy(hasApiKey = key.isNotBlank()) }
    }

    fun updateModel(model: String) {
        preferences.llmModel = model
        _uiState.update { it.copy(llmModel = model) }
    }

    fun updateRetentionDays(days: Int) {
        preferences.retentionDays = days
        _uiState.update { it.copy(retentionDays = days) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun showTimePicker() {
        _uiState.update { it.copy(showTimePicker = true) }
    }

    fun dismissTimePicker() {
        _uiState.update { it.copy(showTimePicker = false) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            summaryRepository.deleteAll()
            groupRepository.deleteAll()
            preferences.clearAll()
            _uiState.update { it.copy(showDeleteConfirmation = false) }
            refreshState()
        }
    }

    fun exportData(): String {
        var json = "[]"
        viewModelScope.launch {
            val summaries = summaryRepository.getAllSummariesSnapshot()
            val type = Types.newParameterizedType(
                List::class.java,
                Map::class.java
            )
            val data = summaries.map { s ->
                mapOf(
                    "group" to s.groupName,
                    "date" to s.date,
                    "content" to s.content,
                    "messageCount" to s.messageCount.toString(),
                    "model" to s.modelUsed
                )
            }
            @Suppress("UNCHECKED_CAST")
            val adapter = moshi.adapter<Any>(type)
            json = adapter.toJson(data)
        }
        return json
    }
}
