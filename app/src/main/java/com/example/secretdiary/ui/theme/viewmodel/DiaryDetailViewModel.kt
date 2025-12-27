package com.example.secretdiary.ui.theme.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secretdiary.data.location.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiaryDetailViewModel(
    private val repository: DiaryRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _entry = MutableStateFlow<DiaryEntry?>(null)
    val entry = _entry.asStateFlow()

    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    fun loadEntry(id: Int) {
        if (id != -1) {
            viewModelScope.launch {
                repository.getEntryById(id).collect { diaryEntry ->
                    _entry.value = diaryEntry
                    _imageUri.value = extractUriFromContent(diaryEntry?.content)
                }
            }
        }
    }

    fun loadLocation() {
        if (_entry.value?.id == 0 || _entry.value == null) {
            viewModelScope.launch {
                locationHelper.getCurrentLocation { loc ->
                    _location.value = loc
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        val currentTime = System.currentTimeMillis()
        // When creating a new entry, explicitly set id = 0. This is required after the Room library update.
        _entry.value = _entry.value?.copy(title = newTitle) ?: DiaryEntry(id = 0, title = newTitle, content = "", timestamp = currentTime)
    }

    fun onContentChange(newContent: String) {
        val currentTime = System.currentTimeMillis()
        // When creating a new entry, explicitly set id = 0.
        _entry.value = _entry.value?.copy(content = newContent) ?: DiaryEntry(id = 0, title = "", content = newContent, timestamp = currentTime)
    }

    fun onImageUriChange(uri: String) {
        _imageUri.value = uri
        val currentContent = _entry.value?.content ?: ""
        if (!currentContent.contains(uri)) {
            val newContent = "$currentContent\n[Image: $uri]".trim()
            onContentChange(newContent)
        }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _entry.value?.let { currentEntry ->
                val locationData = _location.value
                var entryToSave = currentEntry

                if (locationData != null) {
                    entryToSave = entryToSave.copy(
                        latitude = locationData.latitude,
                        longitude = locationData.longitude
                    )
                }

                // Correctly handle insert vs. update.
                // If id is 0, it's a new entry that needs a new timestamp.
                if (entryToSave.id == 0) {
                    repository.insert(entryToSave.copy(timestamp = System.currentTimeMillis()))
                } else {
                    // Otherwise, it's an existing entry to update.
                    repository.update(entryToSave)
                }
            }
        }
    }

    private fun extractUriFromContent(content: String?): String? {
        return content?.lines()?.find { it.startsWith("[Image:") && it.endsWith("]") }
            ?.removePrefix("[Image:")?.removeSuffix("]")?.trim()
    }
}