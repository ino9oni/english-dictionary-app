package com.example.englishdictionary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: String,
    val entryId: String,
    val rating: String,
    val reviewedAtEpochMillis: Long
)
