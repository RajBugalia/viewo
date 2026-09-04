package com.example.viewo.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "proof_of_play")
data class ProofOfPlayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val screenId: String,
    val campaignId: String,
    val mediaId: String,
    val timestamp: String,
    val durationSeconds: Int,
    val status: String
)
