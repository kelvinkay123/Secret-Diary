package com.example.secretdiary.ui.theme.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secretdiary.data.location.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DiaryDetailViewModel(
    private val repository: DiaryRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _entry = MutableStateFlow<DiaryEntry?>(null)
    val entry = _entry.asStateFlow()

    // ✅ Preview for UI (can be image OR video)
    private val _previewUri = MutableStateFlow<String?>(null)

    @Suppress("unused") // UI may collect this later
    val previewUri = _previewUri.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private var loadJob: Job? = null

    fun loadEntry(id: Int) {
        if (id == -1) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getEntryById(id).collectLatest { diaryEntry ->
                _entry.value = diaryEntry
                _previewUri.value = diaryEntry?.mediaUri ?: diaryEntry?.imageUri
            }
        }
    }

    @Suppress("unused") // call from UI when you want auto-location
    fun loadLocation() {
        if (_entry.value == null || _entry.value?.id == 0) {
            viewModelScope.launch {
                locationHelper.getCurrentLocation { loc ->
                    _location.value = loc
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        val currentTime = System.currentTimeMillis()
        _entry.value =
            _entry.value?.copy(title = newTitle)
                ?: DiaryEntry(
                    id = 0,
                    title = newTitle,
                    content = "",
                    timestamp = currentTime
                )
    }

    fun onContentChange(newContent: String) {
        val currentTime = System.currentTimeMillis()
        _entry.value =
            _entry.value?.copy(content = newContent)
                ?: DiaryEntry(
                    id = 0,
                    title = "",
                    content = newContent,
                    timestamp = currentTime
                )
    }

    fun onMediaCaptured(uri: String, isVideo: Boolean) {
        if (uri.isBlank()) return

        val current = _entry.value ?: DiaryEntry(
            id = 0,
            title = "",
            content = "",
            timestamp = System.currentTimeMillis()
        )

        val cleanedContent = removeMediaTags(current.content)

        val tag = if (isVideo) "[Video: $uri]" else "[Image: $uri]"
        val updatedContent = (cleanedContent + "\n" + tag).trim()

        _entry.value = if (isVideo) {
            current.copy(
                content = updatedContent,
                mediaUri = uri,
                isVideo = true,
                imageUri = null
            )
        } else {
            current.copy(
                content = updatedContent,
                imageUri = uri,
                mediaUri = null,
                isVideo = false
            )
        }

        _previewUri.value = uri
    }

    @Suppress("unused") // backwards compatible helper
    fun onImageUriChange(uri: String) {
        onMediaCaptured(uri = uri, isVideo = false)
    }

    fun saveEntry() {
        viewModelScope.launch {
            val currentEntry = _entry.value ?: return@launch

            val locationData = _location.value
            val entryToSave = if (locationData != null) {
                currentEntry.copy(
                    latitude = locationData.latitude,
                    longitude = locationData.longitude
                )
            } else {
                currentEntry
            }

            if (entryToSave.id == 0) {
                repository.insert(entryToSave.copy(timestamp = System.currentTimeMillis()))
            } else {
                repository.update(entryToSave)
            }
        }
    }

    private fun removeMediaTags(content: String): String {
        if (content.isBlank()) return content
        return content.lines()
            .filterNot { it.trim().startsWith("[Image:") || it.trim().startsWith("[Video:") }
            .joinToString("\n")
            .trim()
    }
}
