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

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val response = api.getPlayerAssignment(_pairingCode.value)
                    if (response.isSuccessful) {
                        val assignment = response.body()
                        if (assignment != null && assignment.isPaired) {
                            if (assignment.campaign != null && assignment.playlist != null && assignment.playlist.mediaItems.isNotEmpty()) {
                                
                                // SMART SYNC: Check and download missing media
                                var allDownloaded = true
                                val localMediaItems = assignment.playlist.mediaItems.toMutableList()

                                for (i in localMediaItems.indices) {
                                    val media = localMediaItems[i]
                                    var cached = mediaCacheDao.getMediaCache(media.id)
                                    
                                    if (cached == null || !java.io.File(cached.localFilePath).exists()) {
                                        if (_playerState.value !is PlayerState.Playing) {
                                            _playerState.value = PlayerState.Downloading
                                        }
                                        
                                        val extension = media.url.substringAfterLast(".", "mp4")
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

                                    // Replace the remote URL with the local file path for playback
                                    if (cached != null) {
                                        localMediaItems[i] = media.copy(url = "file://${cached.localFilePath}")
                                    }
                                }

                                if (allDownloaded) {
                                    val localPlaylist = assignment.playlist.copy(mediaItems = localMediaItems)
                                    val localAssignment = assignment.copy(playlist = localPlaylist)
                                    _playerState.value = PlayerState.Playing(localAssignment)
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
