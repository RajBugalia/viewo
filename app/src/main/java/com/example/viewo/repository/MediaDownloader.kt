package com.example.viewo.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object MediaDownloader {
    private val client = OkHttpClient()

    suspend fun downloadMedia(context: Context, url: String, fileName: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure media directory exists
                val mediaDir = File(context.filesDir, "media")
                if (!mediaDir.exists()) {
                    mediaDir.mkdirs()
                }

                val targetFile = File(mediaDir, fileName)
                
                // If it already exists and isn't empty, assume it's fully downloaded for MVP
                if (targetFile.exists() && targetFile.length() > 0) {
                    return@withContext targetFile
                }

                Log.d("MediaDownloader", "Downloading $url to ${targetFile.absolutePath}")

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("MediaDownloader", "Failed to download $url: ${response.code}")
                    return@withContext null
                }

                response.body?.let { body ->
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(targetFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    return@withContext targetFile
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e("MediaDownloader", "Exception downloading $url", e)
                return@withContext null
            }
        }
    }
}
