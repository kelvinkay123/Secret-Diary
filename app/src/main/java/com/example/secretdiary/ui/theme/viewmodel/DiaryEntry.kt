package com.example.secretdiary.ui.theme.viewmodel

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: Long,
    val imageUri: String? = null,

    // NEW LOCATION FIELDS
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null
)
