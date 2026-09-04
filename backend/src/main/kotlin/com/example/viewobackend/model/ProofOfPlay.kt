package com.example.viewobackend.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
data class ProofOfPlay(
    @Id val id: String = UUID.randomUUID().toString(),
    val screenId: String,
    val campaignId: String,
    val mediaId: String,
    val timestamp: String,
    val durationSeconds: Int,
    val status: String // E.g., "COMPLETED", "INTERRUPTED"
)
