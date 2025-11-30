package com.example.secretdiary.data.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secretdiary.data.DiaryEntry
import com.example.secretdiary.data.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiaryDetailViewModel(private val repository: DiaryRepository) : ViewModel() {

        private val _entry = MutableStateFlow<DiaryEntry?>(null)
        val entry = _entry.asStateFlow()

        // FIX: Initialize the image URI StateFlow with a default empty string.
        private val _imageUri = MutableStateFlow("")
        val imageUri = _imageUri.asStateFlow()

    fun loadEntry(id: Int) {
        if (id != -1) {
            viewModelScope.launch {
                // Using .collect() is better for observing database changes in real-time
                repository.getEntryById(id).collect { diaryEntry ->
                    _entry.value = diaryEntry
                    val uri = extractUriFromContent(diaryEntry?.content)
                    if (uri != null) {
                        _imageUri.value = uri
                    }
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        // FIX: Added the missing 'timestamp' parameter
        val currentTime = System.currentTimeMillis()
        _entry.value = _entry.value?.copy(title = newTitle) ?: DiaryEntry(title = newTitle, content = "", timestamp = currentTime)
    }

    fun onContentChange(newContent: String) {
        // FIX: Added the missing 'timestamp' parameter
        val currentTime = System.currentTimeMillis()
        _entry.value = _entry.value?.copy(content = newContent) ?: DiaryEntry(title = "", content = newContent, timestamp = currentTime)
    }

    fun onImageUriChange(uri: String) {
        _imageUri.value = uri
        val currentContent = _entry.value?.content ?: ""
        // Avoid adding duplicate image tags
        if (!currentContent.contains(uri)) {
            val newContent = "$currentContent\n[Image: $uri]".trim()
            onContentChange(newContent)
        }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _entry.value?.let {
                // Ensure timestamp is set for new entries
                val entryToSave = if (it.id == 0 && it.timestamp == 0L) {
                    it.copy(timestamp = System.currentTimeMillis())
                } else {
                    it
                }
                repository.insert(entryToSave)
            }
        }
    }

    private fun extractUriFromContent(content: String?): String? {
        return content?.lines()?.find { it.startsWith("[Image:") && it.endsWith("]") }
            ?.removePrefix("[Image:")?.removeSuffix("]")?.trim()
    }
}