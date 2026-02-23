package com.example.englishdictionary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_entries")
data class UserEntryEntity(
    @PrimaryKey
    val entryId: String,
    val deckId: String,
    val term: String,
    val displayTerm: String,
    val pos: String,
    val meaningEn: String,
    val meaningJa: String,
    val loreNote: String,
    val canonicalTranslation: String,
    val tagsCsv: String,
    val synonymsCsv: String,
    val confusablesCsv: String,
    val examplesJson: String,
    val sourceQuotesJson: String,
    val updatedAt: String
)
