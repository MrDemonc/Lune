package com.demonlab.lune.tools.playlist

import com.demonlab.lune.tools.Song

object QueueManager {

    fun moveToFront(playlist: List<Song>, currentSong: Song, targetSong: Song, frontCount: Int = 0): List<Song> {
        val currentIdx = playlist.indexOfFirst { it.id == currentSong.id }
        if (currentIdx == -1) return playlist

        val targetIdx = playlist.indexOfFirst { it.id == targetSong.id }
        val mutable = playlist.toMutableList()

        if (targetIdx != -1) {
            mutable.removeAt(targetIdx)
        }

        val newCurrentIdx = mutable.indexOfFirst { it.id == currentSong.id }
        if (newCurrentIdx == -1) return playlist

        val insertAt = (newCurrentIdx + 1 + frontCount).coerceIn(0, mutable.size)
        mutable.add(insertAt, targetSong)
        return mutable
    }

    fun moveToEnd(playlist: List<Song>, currentSong: Song, targetSong: Song): List<Song> {
        val currentIdx = playlist.indexOfFirst { it.id == currentSong.id }
        val targetIdx = playlist.indexOfFirst { it.id == targetSong.id }
        if (targetIdx == -1 || currentIdx == -1 || targetIdx == currentIdx) return playlist

        val mutable = playlist.toMutableList()
        mutable.removeAt(targetIdx)
        mutable.add(targetSong)
        return mutable
    }
}
