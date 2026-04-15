package com.whatsummary.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.db.entity.TrackedGroup
import com.whatsummary.data.repository.GroupRepository
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.worker.SummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupActivity(
    val group: TrackedGroup,
    val lastMessage: CapturedMessage?,
    val countToday: Int
)

data class HomeUiState(
    val groups: List<GroupActivity> = emptyList(),
    val isLoading: Boolean = true,
    val justQueuedSummary: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val summaryScheduler: SummaryScheduler
) : ViewModel() {

    private val _actionState = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        groupRepository.getAllGroups(),
        messageRepository.observeLatestPerGroup(),
        messageRepository.observeTodayCountPerGroup(),
        _actionState
    ) { groups, latest, counts, queued ->
        val latestByGroup = latest.associateBy { it.groupName }
        val countByGroup = counts.associate { it.groupName to it.count }

        // Sort: groups with any captured message sorted by last message desc,
        // then groups with no activity yet alphabetically at the bottom.
        val activity = groups.map { group ->
            GroupActivity(
                group = group,
                lastMessage = latestByGroup[group.groupName],
                countToday = countByGroup[group.groupName] ?: 0
            )
        }.sortedWith(
            compareByDescending<GroupActivity> { it.lastMessage?.timestamp ?: Long.MIN_VALUE }
                .thenBy { it.group.groupName.lowercase() }
        )

        HomeUiState(
            groups = activity,
            isLoading = false,
            justQueuedSummary = queued
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun toggleGroup(groupName: String, enabled: Boolean) {
        viewModelScope.launch {
            groupRepository.setEnabled(groupName, enabled)
        }
    }

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
