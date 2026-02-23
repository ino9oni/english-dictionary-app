package com.example.englishdictionary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StudyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StudyLogEntity>)

    @Query("SELECT * FROM study_logs")
    suspend fun getAll(): List<StudyLogEntity>

    @Query(
        "SELECT entryId AS entryId, COUNT(*) AS wrongCount " +
            "FROM study_logs " +
            "WHERE deckId = :deckId AND rating = 'UNKNOWN' " +
            "GROUP BY entryId"
    )
    fun observeWrongCounts(deckId: String): Flow<List<EntryWrongCountRow>>
}

data class EntryWrongCountRow(
    val entryId: String,
    val wrongCount: Int
)
