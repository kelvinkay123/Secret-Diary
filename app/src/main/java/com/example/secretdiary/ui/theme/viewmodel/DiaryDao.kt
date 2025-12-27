package com.example.secretdiary.ui.theme.viewmodel

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.secretdiary.ui.theme.viewmodel.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry)

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    fun getEntryById(id: Int): Flow<DiaryEntry?>

    @Query("UPDATE diary_entries SET title = :title, content = :content, timestamp = :timestamp, imageUri = :imageUri WHERE id = :id")
    suspend fun updateEntry(id: Int, title: String, content: String, timestamp: Long, imageUri: String?): Int
}
