package com.whatsummary.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsummary.data.db.entity.TrackedGroup
import com.whatsummary.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<TrackedGroup> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
) {
    val filteredGroups: List<TrackedGroup>
        get() = if (searchQuery.isBlank()) groups
        else groups.filter { it.groupName.contains(searchQuery, ignoreCase = true) }
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.getAllGroups()
                .catch { /* silent */ }
                .collect { groups ->
                    _uiState.update { it.copy(groups = groups, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleGroup(groupName: String, enabled: Boolean) {
        viewModelScope.launch {
            groupRepository.setEnabled(groupName, enabled)
        }
    }
}
