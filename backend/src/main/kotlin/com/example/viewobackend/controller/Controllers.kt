package com.example.viewobackend.controller

import com.example.viewobackend.model.*
import com.example.viewobackend.repository.*
import org.springframework.web.bind.annotation.*
import jakarta.annotation.PostConstruct
import java.util.UUID

@RestController
@RequestMapping("/api")
class MainController(
    val screenRepository: ScreenRepository,
    val campaignRepository: CampaignRepository,
    val mediaRepository: MediaRepository,
    val playlistRepository: PlaylistRepository,
    val proofOfPlayRepository: ProofOfPlayRepository
) {
    @PostConstruct
    fun initData() {
        // No dummy data
    }

    @GetMapping("/screens")
    fun getScreens(): List<Screen> {
        val now = System.currentTimeMillis()
        return screenRepository.findAll().map { screen ->
            val diff = now - screen.lastHeartbeatTimestamp
            if (screen.status != ScreenStatus.WARNING) { // Keep WARNING manually set if needed
                if (diff <= 15000) {
                    screen.status = ScreenStatus.ONLINE
                    screen.lastHeartbeat = "Just now"
                } else {
                    screen.status = ScreenStatus.OFFLINE
                    val seconds = diff / 1000
                    if (seconds < 60) screen.lastHeartbeat = "$seconds seconds ago"
                    else if (seconds < 3600) screen.lastHeartbeat = "${seconds / 60} mins ago"
                    else screen.lastHeartbeat = "${seconds / 3600} hours ago"
                }
                screenRepository.save(screen)
            }
            screen
        }
    }

    @GetMapping("/campaigns")
    fun getCampaigns() = campaignRepository.findAll()

    @DeleteMapping("/campaigns/{id}")
    fun deleteCampaign(@PathVariable id: String): Map<String, String> {
        campaignRepository.deleteById(id)
        return mapOf("status" to "ok")
    }

    @PostMapping("/campaigns")
    fun createCampaign(@RequestBody request: CampaignRequest): Campaign {
        val newCampaign = Campaign(
            id = "C-" + java.util.UUID.randomUUID().toString().take(8),
            name = request.name,
            description = request.description ?: "",
            playlistId = request.playlistId,
            startDate = request.startDate,
            endDate = request.endDate,
            orientation = request.orientation,
            selectedScreens = request.screenIds,
            status = CampaignStatus.ACTIVE
        )
        return campaignRepository.save(newCampaign)
    }

    @GetMapping("/media")
    fun getMedia() = mediaRepository.findAll()

    @DeleteMapping("/media/{id}")
    fun deleteMedia(@PathVariable id: String): Map<String, String> {
        mediaRepository.deleteById(id)
        return mapOf("status" to "ok")
    }

    @PostMapping("/media")
    fun createMedia(@RequestBody request: MediaRequest): Media {
        val newMedia = Media(
            id = "M-" + java.util.UUID.randomUUID().toString().take(8),
            name = request.name,
            type = request.type,
            url = request.url,
            thumbnailUrl = null,
            durationSeconds = request.durationSeconds
        )
        return mediaRepository.save(newMedia)
    }

    @PostMapping("/media/upload")
    fun uploadMedia(@RequestParam("file") file: org.springframework.web.multipart.MultipartFile): Map<String, String> {
        val uploadPath = java.nio.file.Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath()
        val uploadDir = uploadPath.toFile()
        if (!uploadDir.exists()) uploadDir.mkdirs()

        val originalFilename = file.originalFilename ?: "unknown"
        val extension = originalFilename.substringAfterLast(".", "")
        val uniqueFilename = "${java.util.UUID.randomUUID()}.$extension"
        val targetFile = java.io.File(uploadDir, uniqueFilename)
        
        file.transferTo(targetFile)

        // Hardcoding emulator IP for MVP
        val fileUrl = "http://10.46.135.64:9876/uploads/$uniqueFilename"
        return mapOf("url" to fileUrl)
    }

    @GetMapping("/playlists")
    fun getPlaylists() = playlistRepository.findAll()

    @DeleteMapping("/playlists/{id}")
    fun deletePlaylist(@PathVariable id: String): Map<String, String> {
        playlistRepository.deleteById(id)
        return mapOf("status" to "ok")
    }

    @PostMapping("/playlists")
    fun createPlaylist(@RequestBody request: PlaylistRequest): Playlist {
        val items = request.mediaIds.mapNotNull { mediaRepository.findById(it).orElse(null) }
        val newPlaylist = Playlist(
            id = "P-" + java.util.UUID.randomUUID().toString().take(8),
            name = request.name,
            mediaItems = items,
            defaultDurationSeconds = request.defaultDurationSeconds ?: 10
        )
        return playlistRepository.save(newPlaylist)
    }

    @PostMapping("/screens/pair")
    fun pairScreen(@RequestBody request: PairingRequest): Screen {
        val now = System.currentTimeMillis()
        val screen = screenRepository.findById(request.pairingCode).orElse(
            Screen(
                id = request.pairingCode,
                name = request.name,
                location = request.location ?: "Unknown Location",
                status = ScreenStatus.ONLINE,
                lastHeartbeat = "Just now",
                lastHeartbeatTimestamp = now,
                currentCampaign = null
            )
        )
        screen.name = request.name
        screen.location = request.location ?: "Unknown Location"
        screen.status = ScreenStatus.ONLINE
        screen.lastHeartbeat = "Just now"
        screen.lastHeartbeatTimestamp = now
        return screenRepository.save(screen)
    }

    @DeleteMapping("/screens/{id}")
    fun deleteScreen(@PathVariable id: String): Map<String, String> {
        screenRepository.deleteById(id)
        return mapOf("status" to "ok")
    }

    @PostMapping("/player/heartbeat")
    fun heartbeat(@RequestBody request: Map<String, String>): Map<String, String> {
        val pairingCode = request["pairingCode"] ?: return mapOf("status" to "error")
        screenRepository.findById(pairingCode).ifPresent { screen ->
            screen.lastHeartbeatTimestamp = System.currentTimeMillis()
            screen.status = ScreenStatus.ONLINE
            screen.lastHeartbeat = "Just now"
            screenRepository.save(screen)
        }
        return mapOf("status" to "ok")
    }

    @GetMapping("/player/{pairingCode}/assignment")
    fun getPlayerAssignment(@PathVariable pairingCode: String): org.springframework.http.ResponseEntity<PlayerAssignment> {
        val screenOpt = screenRepository.findById(pairingCode)
        if (screenOpt.isEmpty) {
            // Screen is not paired yet
            return org.springframework.http.ResponseEntity.notFound().build()
        }

        // Screen is paired. Find the active campaign for this screen.
        var activeCampaign = campaignRepository.findAll().firstOrNull { 
            it.status == CampaignStatus.ACTIVE && it.selectedScreens.contains(pairingCode)
        }

        if (activeCampaign != null) {
            try {
                val now = java.time.LocalDateTime.now()
                val start = java.time.LocalDateTime.parse(activeCampaign.startDate)
                val end = java.time.LocalDateTime.parse(activeCampaign.endDate)
                if (now.isAfter(end)) {
                    activeCampaign.status = CampaignStatus.COMPLETED
                    campaignRepository.save(activeCampaign)
                    activeCampaign = null
                } else if (now.isBefore(start)) {
                    // For MVP, just let it play immediately even if scheduled slightly in the future
                    // activeCampaign = null
                }
            } catch (e: Exception) {
                // If dates are unparseable, leave it active for MVP safely
            }
        }

        if (activeCampaign == null) {
            // Paired, but no campaign assigned
            return org.springframework.http.ResponseEntity.ok(PlayerAssignment(isPaired = true, campaign = null, playlist = null))
        }

        val playlist = playlistRepository.findById(activeCampaign.playlistId).orElse(null)
        
        return org.springframework.http.ResponseEntity.ok(
            PlayerAssignment(
                isPaired = true,
                campaign = activeCampaign,
                playlist = playlist
            )
        )
    }

    @PostMapping("/player/proof-of-play")
    fun submitProofOfPlay(@RequestBody records: List<ProofOfPlayRequest>): String {
        records.forEach { req ->
            val pop = ProofOfPlay(
                screenId = req.screenId,
                campaignId = req.campaignId,
                mediaId = req.mediaId,
                timestamp = req.timestamp,
                durationSeconds = req.durationSeconds,
                status = req.status
            )
            proofOfPlayRepository.save(pop)
        }
        return "{\"status\": \"ok\"}"
    }

    @GetMapping("/proof-of-play")
    fun getProofOfPlayRecords() = proofOfPlayRepository.findAll().sortedByDescending { it.timestamp }
}

data class ProofOfPlayRequest(
    val screenId: String,
    val campaignId: String,
    val mediaId: String,
    val timestamp: String,
    val durationSeconds: Int,
    val status: String
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
    val defaultDurationSeconds: Int? = null
)
