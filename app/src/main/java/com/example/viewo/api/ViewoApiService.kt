package com.example.viewo.api

import com.example.viewo.model.Campaign
import com.example.viewo.model.Media
import com.example.viewo.model.Playlist
import com.example.viewo.model.Screen
import retrofit2.http.GET
import retrofit2.http.POST

interface ViewoApiService {
    @GET("screens")
    suspend fun getScreens(): List<Screen>

    @GET("campaigns")
    suspend fun getCampaigns(): List<Campaign>

    @POST("campaigns")
    suspend fun createCampaign(@retrofit2.http.Body request: com.example.viewo.model.CampaignRequest): Campaign

    @GET("media")
    suspend fun getMedia(): List<Media>

    @POST("media")
    suspend fun createMedia(@retrofit2.http.Body request: com.example.viewo.model.MediaRequest): Media

    @retrofit2.http.Multipart
    @POST("media/upload")
    suspend fun uploadMedia(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): com.example.viewo.model.UploadResponse

    @GET("playlists")
    suspend fun getPlaylists(): List<Playlist>

    @POST("playlists")
    suspend fun createPlaylist(@retrofit2.http.Body request: com.example.viewo.model.PlaylistRequest): Playlist

    @POST("player/proof-of-play")
    suspend fun submitProofOfPlay(@retrofit2.http.Body records: List<com.example.viewo.model.ProofOfPlayRequest>): retrofit2.Response<Void>

    @GET("proof-of-play")
    suspend fun getProofOfPlayRecords(): List<com.example.viewo.model.ProofOfPlayRecord>

    @POST("screens/pair")
    suspend fun pairScreen(@retrofit2.http.Body request: com.example.viewo.model.PairingRequest): Screen

    @GET("player/{pairingCode}/assignment")
    suspend fun getPlayerAssignment(@retrofit2.http.Path("pairingCode") pairingCode: String): retrofit2.Response<com.example.viewo.model.PlayerAssignment>

    @POST("player/heartbeat")
    suspend fun sendHeartbeat(@retrofit2.http.Body request: Map<String, String>): retrofit2.Response<Void>

    @retrofit2.http.DELETE("screens/{id}")
    suspend fun deleteScreen(@retrofit2.http.Path("id") id: String): retrofit2.Response<Void>

    @retrofit2.http.DELETE("campaigns/{id}")
    suspend fun deleteCampaign(@retrofit2.http.Path("id") id: String): retrofit2.Response<Void>

    @retrofit2.http.DELETE("media/{id}")
    suspend fun deleteMedia(@retrofit2.http.Path("id") id: String): retrofit2.Response<Void>

    @retrofit2.http.DELETE("playlists/{id}")
    suspend fun deletePlaylist(@retrofit2.http.Path("id") id: String): retrofit2.Response<Void>
}
