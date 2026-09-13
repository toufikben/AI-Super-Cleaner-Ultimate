package com.aisupercleaner.ultimate.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.aisupercleaner.ultimate.data.history.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(repository: HistoryRepository) : ViewModel() {
    val entries: StateFlow<List<HistoryEntry>> = repository.observeAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
}
