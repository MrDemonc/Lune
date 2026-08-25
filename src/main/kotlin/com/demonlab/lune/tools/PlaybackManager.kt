package com.demonlab.lune.tools

import androidx.compose.runtime.*
import com.demonlab.lune.audio.AudioEngine
import com.demonlab.lune.audio.AudioEngineListener
import com.demonlab.lune.data.DataManager
import com.demonlab.lune.data.ParsedLyrics
import com.demonlab.lune.data.Song
import kotlinx.coroutines.*
import java.io.File
import kotlin.random.Random

class PlaybackManager private constructor() : AudioEngineListener {
    private val audioEngine = AudioEngine.instance
    private val dataManager = DataManager.instance
    private val settings = SettingsManager.instance
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Observable states for Compose UI
    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentPositionMs by mutableLongStateOf(0L)
        private set
    var currentDurationMs by mutableLongStateOf(0L)
        private set
    var activeQueue by mutableStateOf<List<Song>>(emptyList())
        private set
    var originalQueue by mutableStateOf<List<Song>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(-1)
        private set

    // Observable favorites reactive set
    var favoritesSet by mutableStateOf<Set<Long>>(emptySet())
        private set

    var lyrics by mutableStateOf(ParsedLyrics())
        private set
    var currentLyricIndex by mutableIntStateOf(-1)
        private set

    // Visualizer simulated wave / spectrum bars (32 bands)
    var visualizerFrequencies by mutableStateOf(FloatArray(32) { 0.05f })
        private set

    // Sleep Timer
    var sleepTimerMinutes by mutableIntStateOf(0)
        private set
    var sleepTimerRemainingSec by mutableIntStateOf(0)
        private set
    private var sleepTimerJob: Job? = null
    private var visualizerJob: Job? = null

    private var trackStartTime = 0L

    init {
        favoritesSet = HashSet(dataManager.getFavorites())
        audioEngine.setListener(this)
        startVisualizerLoop()
        restoreLastSession()
    }

    fun isFavorite(songId: Long): Boolean = favoritesSet.contains(songId)

    fun playSong(song: Song, playlist: List<Song> = emptyList()) {
        val queue = if (playlist.isNotEmpty()) playlist else listOf(song)
        originalQueue = queue
        if (settings.isShuffle) {
            val remaining = queue.filter { it.id != song.id }.shuffled()
            activeQueue = listOf(song) + remaining
            currentIndex = 0
        } else {
            activeQueue = queue
            currentIndex = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }
        playCurrentSong()
    }

    fun playQueueIndex(index: Int) {
        if (index in activeQueue.indices) {
            currentIndex = index
            playCurrentSong()
        }
    }

    private fun playCurrentSong() {
        if (currentIndex !in activeQueue.indices) return
        val song = activeQueue[currentIndex]
        currentSong = song

        // Parse lyrics
        lyrics = LyricsParser.parse(song.lyrics)
        currentLyricIndex = -1

        val file = File(song.path)
        if (file.exists()) {
            val loaded = audioEngine.load(file)
            if (loaded) {
                audioEngine.setVolume(settings.volume)
                audioEngine.setBalance(settings.balance)
                audioEngine.play()
                isPlaying = true
                trackStartTime = System.currentTimeMillis()
                currentDurationMs = song.duration
                saveState()
                MprisManager.instance.notifyMetadataChanged(song)
                MprisManager.instance.notifyStateChanged()
            }
        }
    }

    fun togglePlayPause() {
        if (currentSong == null && activeQueue.isNotEmpty()) {
            currentIndex = 0
            playCurrentSong()
            return
        }
        val song = currentSong ?: return
        if (isPlaying) {
            audioEngine.pause()
            isPlaying = false
        } else {
            val file = File(song.path)
            if (audioEngine.loadedFile == null && file.exists()) {
                audioEngine.load(file, currentPositionMs)
                audioEngine.setVolume(settings.volume)
                audioEngine.setBalance(settings.balance)
            }
            audioEngine.play()
            isPlaying = true
            trackStartTime = System.currentTimeMillis() - currentPositionMs
        }
        MprisManager.instance.notifyStateChanged()
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        if (currentIndex < activeQueue.size - 1) {
            currentIndex++
            playCurrentSong()
        } else if (settings.repeatMode == 2) { // Repeat All
            currentIndex = 0
            playCurrentSong()
        } else {
            audioEngine.stop()
            isPlaying = false
        }
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        if (currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }
        if (currentIndex > 0) {
            currentIndex--
            playCurrentSong()
        } else if (settings.repeatMode == 2) {
            currentIndex = activeQueue.size - 1
            playCurrentSong()
        } else {
            seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        currentPositionMs = positionMs
        audioEngine.seekTo(positionMs)
        updateLyricIndex(positionMs)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1.5f)
        settings.volume = clamped
        audioEngine.setVolume(clamped)
        settings.saveSettings()
        MprisManager.instance.notifyStateChanged()
    }

    fun setBalance(bal: Float) {
        val clamped = bal.coerceIn(0f, 1f)
        settings.balance = clamped
        audioEngine.setBalance(clamped)
        settings.saveSettings()
    }

    fun toggleShuffle() {
        settings.isShuffle = !settings.isShuffle
        settings.saveSettings()
        val curr = currentSong
        if (curr != null && originalQueue.isNotEmpty()) {
            if (settings.isShuffle) {
                val remaining = originalQueue.filter { it.id != curr.id }.shuffled()
                activeQueue = listOf(curr) + remaining
                currentIndex = 0
            } else {
                activeQueue = originalQueue
                currentIndex = originalQueue.indexOfFirst { it.id == curr.id }.coerceAtLeast(0)
            }
        } else if (curr != null && activeQueue.isNotEmpty()) {
            if (settings.isShuffle) {
                originalQueue = activeQueue
                val remaining = activeQueue.filter { it.id != curr.id }.shuffled()
                activeQueue = listOf(curr) + remaining
                currentIndex = 0
            }
        }
        MprisManager.instance.notifyStateChanged()
    }

    fun toggleRepeatMode() {
        settings.repeatMode = (settings.repeatMode + 1) % 3
        settings.saveSettings()
        MprisManager.instance.notifyStateChanged()
    }

    fun toggleFavorite(song: Song) {
        val favorites = dataManager.getFavorites()
        if (favorites.contains(song.id)) {
            favorites.remove(song.id)
        } else {
            favorites.add(song.id)
        }
        dataManager.saveFavorites(favorites)
        favoritesSet = HashSet(favorites)

        activeQueue = activeQueue.map {
            if (it.id == song.id) it.copy(isFavorite = favorites.contains(song.id)) else it
        }
        originalQueue = originalQueue.map {
            if (it.id == song.id) it.copy(isFavorite = favorites.contains(song.id)) else it
        }
        if (currentSong?.id == song.id) {
            currentSong = currentSong?.copy(isFavorite = favorites.contains(song.id))
        }
    }

    fun addToQueue(song: Song, playNext: Boolean = false) {
        val mutableActive = activeQueue.toMutableList()
        val mutableOrig = originalQueue.toMutableList()

        if (playNext && currentIndex in mutableActive.indices) {
            mutableActive.add(currentIndex + 1, song)
            val origIdx = mutableOrig.indexOfFirst { it.id == currentSong?.id }
            if (origIdx >= 0) {
                mutableOrig.add(origIdx + 1, song)
            } else {
                mutableOrig.add(song)
            }
        } else {
            mutableActive.add(song)
            mutableOrig.add(song)
        }
        activeQueue = mutableActive
        originalQueue = mutableOrig

        if (currentSong == null) {
            currentIndex = 0
            playCurrentSong()
        }
    }

    fun removeFromQueue(index: Int) {
        if (index !in activeQueue.indices) return
        val removedSong = activeQueue[index]
        val mutableActive = activeQueue.toMutableList()
        mutableActive.removeAt(index)
        activeQueue = mutableActive

        val mutableOrig = originalQueue.toMutableList()
        mutableOrig.remove(removedSong)
        originalQueue = mutableOrig

        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex) {
            if (currentIndex in activeQueue.indices) {
                playCurrentSong()
            } else if (activeQueue.isNotEmpty()) {
                currentIndex = 0
                playCurrentSong()
            } else {
                audioEngine.stop()
                currentSong = null
                isPlaying = false
            }
        }
    }

    fun clearQueue() {
        audioEngine.stop()
        activeQueue = emptyList()
        originalQueue = emptyList()
        currentSong = null
        currentIndex = -1
        isPlaying = false
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerJob?.cancel()

        if (minutes <= 0) {
            sleepTimerRemainingSec = 0
            return
        }

        sleepTimerRemainingSec = minutes * 60
        sleepTimerJob = scope.launch {
            while (sleepTimerRemainingSec > 0) {
                delay(1000)
                sleepTimerRemainingSec--
            }
            audioEngine.pause()
            isPlaying = false
            sleepTimerMinutes = 0
        }
    }

    private fun startVisualizerLoop() {
        visualizerJob?.cancel()
        visualizerJob = scope.launch {
            val random = Random(System.currentTimeMillis())
            while (isActive) {
                if (isPlaying) {
                    val bands = FloatArray(32) { i ->
                        val base = (kotlin.math.sin((System.currentTimeMillis() + i * 150) / 300.0) * 0.35 + 0.45).toFloat()
                        val noise = (random.nextFloat() * 0.25f)
                        (base + noise).coerceIn(0.1f, 1.0f)
                    }
                    visualizerFrequencies = bands
                } else {
                    visualizerFrequencies = FloatArray(32) { 0.05f }
                }
                delay(60)
            }
        }
    }

    private fun updateLyricIndex(positionMs: Long) {
        if (!lyrics.isSynced || lyrics.lines.isEmpty()) return
        val idx = lyrics.lines.indexOfLast { it.timestampMs <= positionMs }
        currentLyricIndex = idx
    }

    private fun saveState() {
        val curr = currentSong ?: return
        dataManager.savePlaybackState(
            DataManager.SavedPlaybackState(
                lastSongId = curr.id,
                lastPositionMs = currentPositionMs,
                queueSongIds = (if (originalQueue.isNotEmpty()) originalQueue else activeQueue).map { it.id },
                queueIndex = (if (originalQueue.isNotEmpty()) originalQueue else activeQueue).indexOfFirst { it.id == curr.id }.coerceAtLeast(0)
            )
        )
    }

    private fun restoreLastSession() {
        val saved = dataManager.getSavedState() ?: return
        val cached = MusicProvider.instance.getCachedSongs()
        if (cached.isEmpty()) return

        val songMap = cached.associateBy { it.id }
        val restoredQueue = saved.queueSongIds.mapNotNull { songMap[it] }

        if (restoredQueue.isNotEmpty()) {
            originalQueue = restoredQueue
            val song = if (settings.isShuffle) {
                val s = restoredQueue.getOrNull(saved.queueIndex.coerceIn(0, restoredQueue.size - 1)) ?: restoredQueue.first()
                val remaining = restoredQueue.filter { it.id != s.id }.shuffled()
                activeQueue = listOf(s) + remaining
                currentIndex = 0
                s
            } else {
                activeQueue = restoredQueue
                currentIndex = saved.queueIndex.coerceIn(0, restoredQueue.size - 1)
                activeQueue[currentIndex]
            }

            currentSong = song
            lyrics = LyricsParser.parse(song.lyrics)
            currentDurationMs = song.duration
            currentPositionMs = saved.lastPositionMs
            updateLyricIndex(saved.lastPositionMs)

            // Preload file into AudioEngine so pressing play immediately resumes from saved timestamp
            val file = File(song.path)
            if (file.exists()) {
                audioEngine.load(file, saved.lastPositionMs)
                audioEngine.setVolume(settings.volume)
                audioEngine.setBalance(settings.balance)
            }
            MprisManager.instance.notifyMetadataChanged(song)
            MprisManager.instance.notifyStateChanged()
        }
    }

    // AudioEngineListener callbacks
    override fun onTrackEnded() {
        scope.launch {
            currentSong?.let { song ->
                val listenedTime = System.currentTimeMillis() - trackStartTime
                dataManager.recordPlay(song.id, listenedTime.coerceAtLeast(1000L), song.artist)
            }

            if (settings.repeatMode == 1) { // Repeat One
                seekTo(0L)
                audioEngine.play()
                isPlaying = true
            } else {
                playNext()
            }
        }
    }

    override fun onPositionChanged(positionMs: Long, durationMs: Long) {
        currentPositionMs = positionMs
        if (durationMs > 0) currentDurationMs = durationMs
        updateLyricIndex(positionMs)
    }

    override fun onStateChanged(playing: Boolean) {
        isPlaying = playing
        MprisManager.instance.notifyStateChanged()
    }

    override fun onError(message: String) {
        println("PlaybackManager error: $message")
    }

    companion object {
        val instance: PlaybackManager by lazy { PlaybackManager() }
    }
}
