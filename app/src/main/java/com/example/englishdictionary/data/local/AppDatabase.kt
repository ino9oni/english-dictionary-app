package com.example.englishdictionary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntryEntity::class,
        SrsStateEntity::class,
        StudyLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userEntryDao(): UserEntryDao
    abstract fun srsStateDao(): SrsStateDao
    abstract fun studyLogDao(): StudyLogDao
}
