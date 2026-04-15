package com.whatsummary.ui.messages

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.worker.SummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupMessagesUiState(
    val groupName: String = "",
    val messages: List<CapturedMessage> = emptyList(),
    val isLoading: Boolean = true,
    val isSummarizing: Boolean = false,
    val justQueuedSummary: Boolean = false
)

@HiltViewModel
class GroupMessagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val summaryScheduler: SummaryScheduler
) : ViewModel() {

    private val groupName: String = Uri.decode(savedStateHandle["groupName"] ?: "")

    val uiState: StateFlow<GroupMessagesUiState> = messageRepository
        .observeRecentMessages(groupName, days = 7)
        .let { flow ->
            kotlinx.coroutines.flow.combine(
                flow,
                MutableStateFlow(Unit)
            ) { messages, _ ->
                GroupMessagesUiState(
                    groupName = groupName,
                    messages = messages,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupMessagesUiState(groupName = groupName)
        )

    private val _actionState = MutableStateFlow(false)
    val justQueuedSummary: StateFlow<Boolean> = _actionState.asStateFlow()

    fun summarizeNow() {
        viewModelScope.launch {
            summaryScheduler.runOnce()
            _actionState.value = true
        }
    }

    fun consumeQueuedFlag() {
        _actionState.value = false
    }
}
