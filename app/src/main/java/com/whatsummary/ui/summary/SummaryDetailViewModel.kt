package com.whatsummary.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.db.entity.Summary
import com.whatsummary.data.llm.ApiSummaryGenerator
import com.whatsummary.data.llm.LocalSummaryGenerator
import com.whatsummary.data.llm.SummaryGenerator
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.data.repository.SummaryRepository
import com.whatsummary.util.PromptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SummaryDetailUiState(
    val summary: Summary? = null,
    val isLoading: Boolean = true,
    val isRegenerating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SummaryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val summaryRepository: SummaryRepository,
    private val messageRepository: MessageRepository,
    private val apiGenerator: ApiSummaryGenerator,
    private val localGenerator: LocalSummaryGenerator,
    private val preferences: UserPreferences
) : ViewModel() {

    private val summaryId: Long = savedStateHandle["summaryId"]!!

    private val _uiState = MutableStateFlow(SummaryDetailUiState())
    val uiState: StateFlow<SummaryDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            summaryRepository.getSummaryById(summaryId)
                .filterNotNull()
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary, isLoading = false) }
                }
        }
    }

    fun regenerate() {
        val summary = _uiState.value.summary ?: return
        _uiState.update { it.copy(isRegenerating = true) }

        viewModelScope.launch {
            val date = LocalDate.parse(summary.date)
            val messages = messageRepository.getMessagesForGroupOnDate(summary.groupName, date)
            if (messages.isEmpty()) {
                _uiState.update { it.copy(isRegenerating = false, error = "Sem mensagens para resumir") }
                return@launch
            }

            val generator: SummaryGenerator = when (preferences.inferenceMode) {
                UserPreferences.MODE_API -> apiGenerator
                else -> if (localGenerator.isAvailable()) localGenerator else apiGenerator
            }
            val modelLabel = if (generator is ApiSummaryGenerator) preferences.llmModel else "gemma-3-1b-local"

            val prompt = PromptBuilder.buildSummaryPrompt(summary.groupName, summary.date, messages)

            generator.generateSummary(prompt)
                .onSuccess { newContent ->
                    val updated = summary.copy(
                        content = newContent,
                        modelUsed = modelLabel,
                        createdAt = System.currentTimeMillis()
                    )
                    summaryRepository.saveSummary(updated)
                    _uiState.update { it.copy(isRegenerating = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isRegenerating = false, error = e.message) }
                }
        }
    }
}
