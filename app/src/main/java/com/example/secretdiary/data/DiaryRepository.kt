package com.example.secretdiary.data

import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {

    val allEntries: Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    fun getEntryById(id: Int): Flow<DiaryEntry?> {
        return diaryDao.getEntryById(id)
    }

    suspend fun insert(entry: DiaryEntry) {
        diaryDao.insert(entry)
    }

    suspend fun update(entry: DiaryEntry) {
        diaryDao.update(entry)
    }

    suspend fun delete(entry: DiaryEntry) {
        diaryDao.delete(entry)
    }
}