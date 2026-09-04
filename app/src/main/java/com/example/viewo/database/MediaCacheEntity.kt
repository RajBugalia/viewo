package com.example.viewo.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_cache")
data class MediaCacheEntity(
    @PrimaryKey val id: String, // E.g., M01
    val remoteUrl: String,
    val localFilePath: String
)
