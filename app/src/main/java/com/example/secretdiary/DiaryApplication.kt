package com.example.secretdiary

import android.app.Application
import com.example.secretdiary.ui.theme.viewmodel.DiaryDatabase
import com.example.secretdiary.ui.theme.viewmodel.DiaryRepository

class DiaryApplication : Application() {
    // Using by lazy so the database and repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { DiaryDatabase.getDatabase(this) }
    val repository by lazy { DiaryRepository(database.diaryDao()) }
}