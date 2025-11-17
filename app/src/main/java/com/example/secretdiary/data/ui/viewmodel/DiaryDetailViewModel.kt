package com.example.secretdiary.data.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secretdiary.data.DiaryEntry
import com.example.secretdiary.data.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DiaryDetailViewModel(private val repository: DiaryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryDetailUiState())
    val uiState: StateFlow<DiaryDetailUiState> = _uiState

    fun loadEntry(entryId: Int) {
        if (entryId == -1) {
            _uiState.value = DiaryDetailUiState()
            return
        }

        viewModelScope.launch {
            repository.getEntryById(entryId).first { entry ->
                entry?.let {
                    _uiState.value = DiaryDetailUiState(
                        id = it.id,
                        title = it.title,
                        content = it.content,
                        isEntryFound = true
                    )
                }
                true
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun saveEntry(onSaveFinished: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank() || state.content.isBlank()) {
                // Show an error message or similar
                return@launch
            }

            val entry = DiaryEntry(
                id = state.id,
                title = state.title,
                content = state.content,
                timestamp = System.currentTimeMillis()
            )

            if (state.id == 0) {
                repository.insert(entry)
            } else {
                repository.update(entry)
            }
            onSaveFinished()
        }
    }
}

data class DiaryDetailUiState(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val isEntryFound: Boolean = false
)