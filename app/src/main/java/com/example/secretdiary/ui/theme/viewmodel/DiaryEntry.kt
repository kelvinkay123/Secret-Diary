package com.example.secretdiary.ui.theme.viewmodel

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val content: String,
    val timestamp: Long,

    // IMAGE
    val imageUri: String? = null,

    // VIDEO (NEW)
    val mediaUri: String? = null,
    val isVideo: Boolean = false,

    // LOCATION
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null
)
