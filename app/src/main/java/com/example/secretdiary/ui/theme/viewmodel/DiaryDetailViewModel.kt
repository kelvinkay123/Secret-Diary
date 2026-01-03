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

    // Used by UI to preview last captured media (image OR video uri string)
    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private var loadJob: Job? = null

    fun loadEntry(id: Int) {
        if (id == -1) return

        // ✅ Prevent multiple collectors running at the same time
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getEntryById(id).collectLatest { diaryEntry ->
                _entry.value = diaryEntry
                _imageUri.value = extractLastMediaUriFromContent(diaryEntry?.content)
            }
        }
    }

    fun loadLocation() {
        // ✅ Only auto-load for NEW entries
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

    /**
     * Called after returning from CameraScreen.
     * Stores the uri and appends a tag into content once.
     */
    fun onMediaCaptured(uri: String, isVideo: Boolean) {
        if (uri.isBlank()) return

        _imageUri.value = uri

        val tag = if (isVideo) "[Video: $uri]" else "[Image: $uri]"
        val currentContent = _entry.value?.content.orEmpty()

        // ✅ Avoid duplicates
        if (!currentContent.contains(tag)) {
            val newContent = (currentContent + "\n" + tag).trim()
            onContentChange(newContent)
        }
    }

    /**
     * Backwards compatible helper if any old code still calls it.
     */
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

            // ✅ Insert vs update
            if (entryToSave.id == 0) {
                repository.insert(entryToSave.copy(timestamp = System.currentTimeMillis()))
            } else {
                repository.update(entryToSave)
            }
        }
    }

    private fun extractLastMediaUriFromContent(content: String?): String? {
        if (content.isNullOrBlank()) return null

        // Find last [Image: ...] or [Video: ...] line
        val line = content.lines()
            .lastOrNull { it.startsWith("[Image:") || it.startsWith("[Video:") }
            ?: return null

        return line
            .removePrefix("[Image:")
            .removePrefix("[Video:")
            .removeSuffix("]")
            .trim()
    }
}
