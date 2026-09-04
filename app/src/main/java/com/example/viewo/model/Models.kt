package com.example.viewo.model

enum class ScreenStatus {
    ONLINE, OFFLINE, WARNING
}

data class Screen(
    val id: String,
    val name: String,
    val location: String,
    val status: ScreenStatus,
    val lastHeartbeat: String,
    val currentCampaign: String?,
    val playerVersion: String = "v1.0"
)

enum class CampaignStatus {
    DRAFT, SCHEDULED, ACTIVE, COMPLETED
}

data class Campaign(
    val id: String,
    val name: String,
    val description: String,
    val playlistId: String,
    val startDate: String,
    val endDate: String,
    val orientation: String,
    val selectedScreens: List<String>, // List of screen IDs
    val status: CampaignStatus
)

enum class MediaType {
    IMAGE, VIDEO
}

data class Media(
    val id: String,
    val name: String,
    val type: MediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null // For video
)

data class Playlist(
    val id: String,
    val name: String,
    val mediaItems: List<Media>,
    val defaultDurationSeconds: Int = 10
)

data class PairingRequest(
    val pairingCode: String,
    val name: String,
    val location: String?
)

data class PlayerAssignment(
    val isPaired: Boolean,
    val campaign: Campaign?,
    val playlist: Playlist?
)

data class CampaignRequest(
    val name: String,
    val description: String?,
    val playlistId: String,
    val screenIds: List<String>,
    val startDate: String,
    val endDate: String,
    val orientation: String
)

data class MediaRequest(
    val name: String,
    val type: MediaType,
    val url: String,
    val durationSeconds: Int?
)

data class PlaylistRequest(
    val name: String,
    val mediaIds: List<String>,
    val defaultDurationSeconds: Int
)

data class UploadResponse(
    val url: String
)

data class ProofOfPlayRequest(
    val screenId: String,
    val campaignId: String,
    val mediaId: String,
    val timestamp: String,
    val durationSeconds: Int,
    val status: String
)

data class ProofOfPlayRecord(
    val id: String,
    val screenId: String,
    val campaignId: String,
    val mediaId: String,
    val timestamp: String,
    val durationSeconds: Int,
    val status: String
)
