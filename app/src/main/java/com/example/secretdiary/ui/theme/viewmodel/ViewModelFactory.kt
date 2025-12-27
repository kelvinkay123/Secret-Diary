package com.example.secretdiary.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.secretdiary.ui.theme.viewmodel.DiaryRepository
import com.example.secretdiary.data.location.LocationHelper

class ViewModelFactory(
    private val repository: DiaryRepository,
    private val locationHelper: LocationHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DiaryDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryDetailViewModel(repository, locationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}