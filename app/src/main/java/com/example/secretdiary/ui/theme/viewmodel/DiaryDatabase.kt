package com.example.secretdiary.ui.theme.viewmodel

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.secretdiary.ui.theme.viewmodel.DiaryEntry

@Database(entities = [DiaryEntry::class], version = 2, exportSchema = false, autoMigrations = [])
abstract class DiaryDatabase : RoomDatabase() {

    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary_database"
                ).fallbackToDestructiveMigration() // <-- temporary for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}