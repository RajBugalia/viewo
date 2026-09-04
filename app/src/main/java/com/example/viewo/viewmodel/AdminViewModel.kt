package com.example.viewo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.viewo.api.RetrofitClient
import com.example.viewo.model.Campaign
import com.example.viewo.model.Media
import com.example.viewo.model.Playlist
import com.example.viewo.model.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

import com.example.viewo.api.ViewoApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val api: ViewoApiService
) : ViewModel() {

    private val _screens = MutableStateFlow<List<Screen>>(emptyList())
    val screens: StateFlow<List<Screen>> = _screens

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _proofOfPlayRecords = MutableStateFlow<List<com.example.viewo.model.ProofOfPlayRecord>>(emptyList())
    val proofOfPlayRecords: StateFlow<List<com.example.viewo.model.ProofOfPlayRecord>> = _proofOfPlayRecords

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refreshData()
        startAutoRefreshLoop()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _screens.value = api.getScreens()
                _campaigns.value = api.getCampaigns()
                _media.value = api.getMedia()
                _playlists.value = api.getPlaylists()
                _proofOfPlayRecords.value = api.getProofOfPlayRecords()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun pairScreen(pairingCode: String, name: String, location: String) {
        viewModelScope.launch {
            try {
                api.pairScreen(com.example.viewo.model.PairingRequest(pairingCode, name, location))
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to pair screen: ${e.message}"
            }
        }
    }

    fun createCampaign(name: String, description: String, playlistId: String, screenIds: List<String>, startDate: String, endDate: String, orientation: String) {
        viewModelScope.launch {
            try {
                api.createCampaign(com.example.viewo.model.CampaignRequest(name, description, playlistId, screenIds, startDate, endDate, orientation))
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to create campaign: ${e.message}"
            }
        }
    }

    fun createMedia(name: String, type: com.example.viewo.model.MediaType, url: String, durationSeconds: Int?) {
        viewModelScope.launch {
            try {
                api.createMedia(com.example.viewo.model.MediaRequest(name, type, url, durationSeconds))
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to add media: ${e.message}"
            }
        }
    }

    fun createPlaylist(name: String, mediaIds: List<String>, durationSeconds: Int) {
        viewModelScope.launch {
            try {
                api.createPlaylist(com.example.viewo.model.PlaylistRequest(name, mediaIds, durationSeconds))
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to create playlist: ${e.message}"
            }
        }
    }

    fun uploadLocalMedia(context: android.content.Context, uri: android.net.Uri, name: String, type: com.example.viewo.model.MediaType, durationSeconds: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Copy Uri to Temp File
                val contentResolver = context.contentResolver
                val extension = if (type == com.example.viewo.model.MediaType.VIDEO) "mp4" else "jpg"
                val tempFile = java.io.File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.$extension")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 2. Create MultipartBody.Part
                val mimeType = contentResolver.getType(uri) ?: if (type == com.example.viewo.model.MediaType.VIDEO) "video/*" else "image/*"
                
                // Using OkHttp 4 extension functions
                val okHttpMediaType = mimeType.toMediaTypeOrNull()
                val requestBody = tempFile.asRequestBody(okHttpMediaType)
                val part = okhttp3.MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

                // 3. Upload to Backend
                val uploadResponse = api.uploadMedia(part)
                
                // 4. Create Media Entity with returned URL
                api.createMedia(com.example.viewo.model.MediaRequest(name, type, uploadResponse.url, durationSeconds))
                
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to upload local media: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteScreen(id: String) {
        viewModelScope.launch {
            try {
                api.deleteScreen(id)
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to delete screen: ${e.message}"
            }
        }
    }

    fun deleteCampaign(id: String) {
        viewModelScope.launch {
            try {
                api.deleteCampaign(id)
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to delete campaign: ${e.message}"
            }
        }
    }

    fun deleteMedia(id: String) {
        viewModelScope.launch {
            try {
                api.deleteMedia(id)
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to delete media: ${e.message}"
            }
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            try {
                api.deletePlaylist(id)
                refreshData()
            } catch (e: Exception) {
                _error.value = "Failed to delete playlist: ${e.message}"
            }
        }
    }

    private fun startAutoRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Poll every 5 seconds
                try {
                    // Only silently refresh screens to show online/offline status updates
                    _screens.value = api.getScreens()
                } catch (e: Exception) {
                    // Ignore background refresh errors
                }
            }
        }
    }
}
