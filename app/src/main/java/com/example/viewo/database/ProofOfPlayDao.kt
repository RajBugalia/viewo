package com.example.viewo.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProofOfPlayDao {
    @Insert
    suspend fun insertRecord(record: ProofOfPlayEntity)

    @Query("SELECT * FROM proof_of_play")
    suspend fun getAllUnsyncedRecords(): List<ProofOfPlayEntity>

    @Query("DELETE FROM proof_of_play WHERE id IN (:ids)")
    suspend fun deleteSyncedRecords(ids: List<String>)
}
