package com.whatsummary.ui.summaries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.db.entity.Summary
import com.whatsummary.data.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummariesUiState(
    val summaries: List<Summary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummariesUiState())
    val uiState: StateFlow<SummariesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            summaryRepository.getAllSummaries()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { summaries ->
                    _uiState.update { it.copy(summaries = summaries, isLoading = false) }
                }
        }
    }
}
