package com.example.viewo.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaCacheDao {
    @Query("SELECT * FROM media_cache WHERE id = :id")
    suspend fun getMediaCache(id: String): MediaCacheEntity?

    @Query("SELECT * FROM media_cache")
    suspend fun getAllMediaCache(): List<MediaCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCache(mediaCache: MediaCacheEntity)
    
    @Query("DELETE FROM media_cache WHERE id = :id")
    suspend fun deleteMediaCache(id: String)
}
