package com.demonlab.lune.tools

import android.net.Uri

data class Song(
    val id: Long,
    val albumId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val path: String,
    val dateAdded: Long = 0,
    val albumArtUri: Uri? = null,
    val genre: String? = null,
    val folderName: String,
    val isHiFi: Boolean = false,
    val coverUrl: String? = null,
    val isFavorite: Boolean = false,
    val lyrics: String? = null
)
