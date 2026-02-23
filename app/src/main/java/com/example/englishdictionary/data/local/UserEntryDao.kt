package com.example.englishdictionary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserEntryDao {
    @Query("SELECT * FROM user_entries WHERE deckId = :deckId")
    fun observeByDeck(deckId: String): Flow<List<UserEntryEntity>>

    @Query("SELECT * FROM user_entries WHERE deckId = :deckId AND entryId = :entryId LIMIT 1")
    fun observeEntry(deckId: String, entryId: String): Flow<UserEntryEntity?>

    @Query("SELECT * FROM user_entries")
    suspend fun getAll(): List<UserEntryEntity>

    @Query("SELECT * FROM user_entries")
    fun observeAll(): Flow<List<UserEntryEntity>>

    @Query("SELECT * FROM user_entries WHERE deckId = :deckId")
    suspend fun getByDeck(deckId: String): List<UserEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<UserEntryEntity>)
}
