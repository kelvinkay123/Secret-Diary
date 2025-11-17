package com.example.secretdiary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secretdiary.data.DiaryEntry
import com.example.secretdiary.data.DiaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryListViewModel(private val repository: DiaryRepository) : ViewModel() {

    val allEntries: StateFlow<List<DiaryEntry>> = repository.allEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}