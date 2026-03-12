package com.demonlab.lune.tools

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class CoverProvider {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetchCoverUrl(artist: String, title: String): String? {
        return try {
            val sanitizedArtist = artist.trim().lowercase()
            
            // Remove common suffixes like (Official Audio), [Official Video], etc. from titles which confuse Deezer
            var sanitizedTitle = title.replace(Regex("(?i)\\s*[\\(\\[].*?(official|audio|video|lyric|music).*?[\\)\\]]"), "").trim()
            if (sanitizedTitle.isBlank()) sanitizedTitle = title

            val searchQuery = if (sanitizedArtist.contains("<unknown>") || sanitizedArtist.contains("<desconocido>") || sanitizedArtist.isBlank()) {
                sanitizedTitle
            } else {
                "$artist $sanitizedTitle"
            }
            val query = URLEncoder.encode(searchQuery, "UTF-8")
            val url = "https://api.deezer.com/search?q=$query"
            
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                
                val searchResponse = gson.fromJson(body, DeezerSearchResponse::class.java)
                searchResponse.data.firstOrNull()?.album?.cover_medium
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadAndSaveCover(context: Context, songId: Long, url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                val dir = context.getDir("covers", Context.MODE_PRIVATE)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "$songId.jpg")
                FileOutputStream(file).use { it.write(bytes) }
                Uri.fromFile(file).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private data class DeezerSearchResponse(
        val data: List<DeezerData>
    )

    private data class DeezerData(
        val album: DeezerAlbum
    )

    private data class DeezerAlbum(
        val cover_medium: String
    )
}
