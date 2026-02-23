package com.example.englishdictionary.data.local

import androidx.room.Entity

@Entity(
    tableName = "srs_states",
    primaryKeys = ["deckId", "entryId"]
)
data class SrsStateEntity(
    val deckId: String,
    val entryId: String,
    val phase: String,
    val ease: Double,
    val intervalDays: Int,
    val dueEpochDay: Long,
    val lastReviewedAtEpochMillis: Long?,
    val lapseCount: Int
)
