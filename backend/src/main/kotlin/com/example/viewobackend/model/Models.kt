package com.example.viewobackend.model

import jakarta.persistence.*

enum class ScreenStatus { ONLINE, OFFLINE, WARNING }
enum class CampaignStatus { DRAFT, SCHEDULED, ACTIVE, COMPLETED }
enum class MediaType { IMAGE, VIDEO }

@Entity
data class Screen(
    @Id val id: String,
    var name: String,
    var location: String,
    @Enumerated(EnumType.STRING) var status: ScreenStatus,
    var lastHeartbeat: String,
    var lastHeartbeatTimestamp: Long = 0L,
    val currentCampaign: String?,
    val playerVersion: String = "v1.0"
)

@Entity
data class Campaign(
    @Id val id: String,
    val name: String,
    val description: String,
    val playlistId: String,
    val startDate: String,
    val endDate: String,
    val orientation: String,
    @ElementCollection val selectedScreens: List<String>,
    @Enumerated(EnumType.STRING) var status: CampaignStatus
)

@Entity
data class Media(
    @Id val id: String,
    val name: String,
    @Enumerated(EnumType.STRING) val type: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null
)

@Entity
data class Playlist(
    @Id val id: String,
    val name: String,
    @ManyToMany(fetch = FetchType.EAGER) val mediaItems: List<Media> = listOf(),
    val defaultDurationSeconds: Int = 10
)
