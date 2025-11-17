package com.example.secretdiary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.secretdiary.data.DiaryRepository
import com.example.secretdiary.data.ui.viewmodel.DiaryDetailViewModel

class ViewModelFactory(private val repository: DiaryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DiaryDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}