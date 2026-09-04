package com.example.viewo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MediaCacheEntity::class, ProofOfPlayEntity::class], version = 2, exportSchema = false)
abstract class ViewoDatabase : RoomDatabase() {

    abstract fun mediaCacheDao(): MediaCacheDao
    abstract fun proofOfPlayDao(): ProofOfPlayDao

    companion object {
        @Volatile
        private var INSTANCE: ViewoDatabase? = null

        fun getDatabase(context: Context): ViewoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ViewoDatabase::class.java,
                    "viewo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
