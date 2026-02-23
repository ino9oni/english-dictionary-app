package com.example.englishdictionary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SrsStateDao {
    @Query("SELECT * FROM srs_states WHERE deckId = :deckId")
    fun observeByDeck(deckId: String): Flow<List<SrsStateEntity>>

    @Query("SELECT * FROM srs_states WHERE deckId = :deckId AND entryId = :entryId LIMIT 1")
    suspend fun get(deckId: String, entryId: String): SrsStateEntity?

    @Query("SELECT * FROM srs_states")
    suspend fun getAll(): List<SrsStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SrsStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<SrsStateEntity>)
}
