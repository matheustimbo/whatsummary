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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupMessagesUiState(
    val groupName: String = "",
    val messages: List<CapturedMessage> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class GroupMessagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val summaryScheduler: SummaryScheduler
) : ViewModel() {

    // Nav library already URL-decodes path args when they're typed as StringType,
    // but we defensively decode once in case any caller stores a raw value.
    private val groupName: String = savedStateHandle.get<String>("groupName")
        ?.let { runCatching { Uri.decode(it) }.getOrDefault(it) }
        ?: ""

    val uiState: StateFlow<GroupMessagesUiState> = messageRepository
        .observeRecentMessages(groupName, days = 7)
        .map { messages ->
            GroupMessagesUiState(
                groupName = groupName,
                messages = messages,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupMessagesUiState(groupName = groupName)
        )

    private val _justQueuedSummary = MutableStateFlow(false)
    val justQueuedSummary: StateFlow<Boolean> = _justQueuedSummary.asStateFlow()

    fun summarizeNow() {
        viewModelScope.launch {
            summaryScheduler.runOnce()
            _justQueuedSummary.value = true
        }
    }

    fun consumeQueuedFlag() {
        _justQueuedSummary.value = false
    }
}
