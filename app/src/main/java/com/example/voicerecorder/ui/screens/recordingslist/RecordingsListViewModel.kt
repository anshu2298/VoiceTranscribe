package com.example.voicerecorder.ui.screens.recordingslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecorder.data.db.RecordingEntity
import com.example.voicerecorder.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsListViewModel @Inject constructor(
    private val repository: RecordingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<RecordingEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query -> repository.searchRecordings(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecording(id: String) {
        viewModelScope.launch {
            repository.deleteRecording(id)
        }
    }
}
