package com.example.viewo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.viewo.api.RetrofitClient
import com.example.viewo.api.ViewoApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.viewo.database.MediaCacheEntity
import com.example.viewo.database.ViewoDatabase
import com.example.viewo.model.PlayerAssignment
import com.example.viewo.repository.MediaDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PlayerState {
    object Unpaired : PlayerState()
    object WaitingForCampaign : PlayerState()
    object Downloading : PlayerState() // New state for downloading
    data class Playing(val assignment: PlayerAssignment) : PlayerState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val api: ViewoApiService
) : AndroidViewModel(application) {
    private val mediaCacheDao = ViewoDatabase.getDatabase(application).mediaCacheDao()
    private val proofOfPlayDao = ViewoDatabase.getDatabase(application).proofOfPlayDao()
    private val appContext = application.applicationContext

    private val _pairingCode = MutableStateFlow(generatePairingCode())
    val pairingCode: StateFlow<String> = _pairingCode

    private val prefs = application.getSharedPreferences("viewo_player_prefs", android.content.Context.MODE_PRIVATE)

    private val _deviceSyncMode = MutableStateFlow(
        try {
            com.example.viewo.model.DeviceSyncMode.valueOf(prefs.getString("sync_mode", "AUTO") ?: "AUTO")
        } catch (e: Exception) {
            com.example.viewo.model.DeviceSyncMode.AUTO
        }
    )
    val deviceSyncMode: StateFlow<com.example.viewo.model.DeviceSyncMode> = _deviceSyncMode

    fun setDeviceSyncMode(mode: com.example.viewo.model.DeviceSyncMode) {
        _deviceSyncMode.value = mode
        prefs.edit().putString("sync_mode", mode.name).apply()
    }

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Unpaired)
    val playerState: StateFlow<PlayerState> = _playerState

    init {
        startPolling()
        startPoPSyncLoop()
        startHeartbeatLoop()
    }

    private fun generatePairingCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { allowedChars.random() }.joinToString("")
    }

    private suspend fun cacheMediaItems(items: List<com.example.viewo.model.Media>): Pair<Boolean, List<com.example.viewo.model.Media>> {
        var allDownloaded = true
        val localItems = items.toMutableList()
        for (i in localItems.indices) {
            val media = localItems[i]
            if (media.url.isBlank()) continue
            try {
                var cached = mediaCacheDao.getMediaCache(media.id)
                if (cached == null || !java.io.File(cached.localFilePath).exists()) {
                    if (_playerState.value !is PlayerState.Playing) {
                        _playerState.value = PlayerState.Downloading
                    }
                    val cleanUrl = media.url.substringBefore("?")
                    val extension = cleanUrl.substringAfterLast(".", "mp4")
                    val fileName = "${media.id}.$extension"
                    val dynamicUrl = com.example.viewo.Constants.getDynamicUrl(media.url)
                    val downloadedFile = MediaDownloader.downloadMedia(appContext, dynamicUrl, fileName)
                    if (downloadedFile != null) {
                        cached = MediaCacheEntity(media.id, media.url, downloadedFile.absolutePath)
                        mediaCacheDao.insertMediaCache(cached)
                    } else {
                        allDownloaded = false
                    }
                }
                if (cached != null) {
                    localItems[i] = media.copy(url = "file://${cached.localFilePath}")
                }
            } catch (e: Exception) {
                allDownloaded = false
            }
        }
        return Pair(allDownloaded, localItems)
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val response = api.getPlayerAssignment(_pairingCode.value)
                    if (response.isSuccessful) {
                        val assignment = response.body()
                        if (assignment != null && assignment.isPaired) {
                            val campaign = assignment.campaign
                            if (campaign != null) {
                                if (campaign.layoutType == "SPLIT" && !campaign.zones.isNullOrEmpty()) {
                                    // MULTI-SCREEN SPLIT MODE: cache media for each zone
                                    var allZonesDownloaded = true
                                    val updatedZones = mutableListOf<com.example.viewo.model.ZoneAssignment>()
                                    for (zone in campaign.zones) {
                                        val zMedia = zone.playlist?.mediaItems ?: emptyList()
                                        if (zMedia.isNotEmpty()) {
                                            val (downloaded, localZMedia) = cacheMediaItems(zMedia)
                                            if (!downloaded) allZonesDownloaded = false
                                            val updatedPlaylist = zone.playlist!!.copy(mediaItems = localZMedia)
                                            updatedZones.add(zone.copy(playlist = updatedPlaylist))
                                        } else {
                                            updatedZones.add(zone)
                                        }
                                    }
                                    val finalZones = if (allZonesDownloaded) updatedZones else campaign.zones
                                    val updatedCampaign = campaign.copy(zones = finalZones)
                                    val localAssignment = assignment.copy(campaign = updatedCampaign)
                                    _playerState.value = PlayerState.Playing(localAssignment)
                                } else if (assignment.playlist != null && assignment.playlist.mediaItems.isNotEmpty()) {
                                    // SINGLE SCREEN MODE
                                    val (allDownloaded, localMediaItems) = cacheMediaItems(assignment.playlist.mediaItems)
                                    val finalMedia = if (allDownloaded) localMediaItems else assignment.playlist.mediaItems
                                    val localPlaylist = assignment.playlist.copy(mediaItems = finalMedia)
                                    val localAssignment = assignment.copy(playlist = localPlaylist)
                                    _playerState.value = PlayerState.Playing(localAssignment)
                                } else {
                                    _playerState.value = PlayerState.WaitingForCampaign
                                }
                            } else {
                                _playerState.value = PlayerState.WaitingForCampaign
                            }
                        }
                    } else if (response.code() == 404) {
                        _playerState.value = PlayerState.Unpaired
                    }
                } catch (e: Exception) {
                    // Offline resilience: 
                    // If network fails, we do NOT change the state.
                    // If we are already Playing(localAssignment), ExoPlayer will just keep looping!
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun logProofOfPlay(campaignId: String, mediaId: String, durationSeconds: Int, status: String) {
        viewModelScope.launch {
            val record = com.example.viewo.database.ProofOfPlayEntity(
                screenId = _pairingCode.value,
                campaignId = campaignId,
                mediaId = mediaId,
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                durationSeconds = durationSeconds,
                status = status
            )
            proofOfPlayDao.insertRecord(record)
        }
    }

    private fun startPoPSyncLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    val unsynced = proofOfPlayDao.getAllUnsyncedRecords()
                    if (unsynced.isNotEmpty()) {
                        val requests = unsynced.map {
                            com.example.viewo.model.ProofOfPlayRequest(
                                screenId = it.screenId,
                                campaignId = it.campaignId,
                                mediaId = it.mediaId,
                                timestamp = it.timestamp,
                                durationSeconds = it.durationSeconds,
                                status = it.status
                            )
                        }
                        
                        // Assuming all unsynced records in this batch belong to the same screen
                        val screenId = unsynced.first().screenId
                        val response = api.submitProofOfPlay(screenId, requests)
                        if (response.isSuccessful) {
                            proofOfPlayDao.deleteSyncedRecords(unsynced.map { it.id })
                        }
                    }
                } catch (e: Exception) {
                    // Ignore, try again next loop
                }
                delay(10000) // Try to sync every 10 seconds
            }
        }
    }

    private fun startHeartbeatLoop() {
        viewModelScope.launch {
            while (true) {
                try {
                    val state = _playerState.value
                    if (state !is PlayerState.Unpaired) {
                        api.sendHeartbeat(mapOf("pairingCode" to _pairingCode.value))
                    }
                } catch (e: Exception) {
                    // Ignore, network might be down momentarily
                }
                delay(10000) // Send heartbeat every 10 seconds
            }
        }
    }
}
