package com.demonlab.lune.tools

import android.content.*
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.os.IBinder
import androidx.annotation.OptIn
import com.demonlab.lune.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.ui.graphics.vector.ImageVector
import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.util.Calendar
import com.demonlab.lune.tools.playlist.QueueManager

class PlaybackManager private constructor(private val context: Context) {
    private val settings = SettingsManager.getInstance(context)
    private var musicService: MusicService? = null
    private var isBound = false
    private var pendingPlaySong: Song? = null
    private var pendingRestoreSong: Song? = null
    private var pendingRestorePosition: Long = 0L
    private var pendingRestorePlay: Boolean = false
    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentLyrics by mutableStateOf<String?>(null)
        private set
    var activePlaylist by mutableStateOf<List<Song>>(emptyList())
        private set
    var activePlaylistId by mutableStateOf<Long?>(null)
        private set
    var queueSections: List<QueuedSection> = emptyList()
    var activeCategory by mutableStateOf<String?>(settings.activeCategory)
        private set
    var activePlaylistName by mutableStateOf<String?>(null)
        private set
    private var shuffledIndices: List<Int> = emptyList()
    private var currentShufflePosition: Int = -1
    
    var sortOption by mutableStateOf(settings.sortOption)
        private set
    var isSortAscending by mutableStateOf(settings.isSortAscending)
        private set

    var isShuffle by mutableStateOf(settings.isShuffle)
    var isCrossfade by mutableStateOf(settings.isCrossfade)
    var isAutomix by mutableStateOf(settings.isAutomix)
    var repeatMode by mutableStateOf(settings.repeatMode) // 0: Off, 1: One, 2: All
    var isQueueFinished by mutableStateOf(false) // true when last song ended naturally
    var isTransitioning by mutableStateOf(false) 
    
    var isEqEnabled by mutableStateOf(settings.isEqEnabled)
        private set
    var eqBandsCount: Short by mutableStateOf(0)
        private set
    var eqBandsRange: ShortArray? by mutableStateOf(null)
        private set
    var isBassBoostEnabled by mutableStateOf(settings.isBassBoostEnabled)
        private set
    var isSpatialAudioEnabled by mutableStateOf(settings.isSpatialAudioEnabled)
        private set

    private var frontQueueInsertCount = 0
    private var bassBoostOffset: Short = 0

    val playbackStateSaver = PlaybackStateSaver(
        context.getSharedPreferences("lune_state", Context.MODE_PRIVATE)
    )
    var stateRestored by mutableStateOf(false)
        private set

    fun resetQueueCounts() {
        frontQueueInsertCount = 0
    }

    fun refreshActivePlaylist(newSongs: List<Song>) {
        if (activePlaylistId == null) return
        activePlaylist = newSongs
        if (isShuffle) {
            updateShuffledQueue(keepCurrentFirst = true)
        }
    }

    fun savePlaybackState(wasPlaying: Boolean = isPlaying) {
        val pos = musicService?.currentPosition()?.toLong() ?: 0L
        playbackStateSaver.save(
            SavedPlaybackState(
                currentSongId = currentSong?.id ?: -1L,
                playbackPositionMs = pos,
                queueIds = activePlaylist.map { it.id },
                playlistId = activePlaylistId,
                playlistName = activePlaylistName,
                playlistCategory = activeCategory,
                shuffledIndices = if (isShuffle) shuffledIndices else emptyList(),
                shufflePosition = if (isShuffle) currentShufflePosition else -1,
                frontQueueInsertCount = frontQueueInsertCount,
                wasPlaying = wasPlaying,
                queueSections = queueSections
            )
        )
    }

    fun restorePlaybackState(songs: List<Song>) {
        val saved = playbackStateSaver.restore() ?: run {
            stateRestored = true
            return
        }
        if (saved.currentSongId == -1L || saved.queueIds.isEmpty()) {
            stateRestored = true
            return
        }
        val restoredSongs = saved.queueIds.mapNotNull { id -> songs.find { it.id == id } }
        if (restoredSongs.isEmpty()) {
            stateRestored = true
            return
        }
        val restoredCurrent = restoredSongs.find { it.id == saved.currentSongId }
            ?: restoredSongs.first()

        // Read shuffle preference for this playlist
        val plId = saved.playlistId
        val shuffle = if (plId != null) settings.getPlaylistShuffle(plId) else settings.isShuffle

        currentSong = restoredCurrent
        activePlaylist = restoredSongs
        activePlaylistId = saved.playlistId
        activePlaylistName = saved.playlistName
        queueSections = saved.queueSections
        if (saved.playlistCategory != null) {
            activeCategory = saved.playlistCategory
            settings.activeCategory = saved.playlistCategory
        }

        isShuffle = shuffle

        if (shuffle && saved.shuffledIndices.isNotEmpty()) {
            // Validate and restore shuffled indices
            val validIndices = saved.shuffledIndices.filter { it < restoredSongs.size }
            if (validIndices.isNotEmpty()) {
                shuffledIndices = validIndices
                currentShufflePosition = saved.shufflePosition.coerceIn(0, validIndices.size - 1)
            } else {
                updateShuffledQueue(keepCurrentFirst = false)
            }
        } else {
            updateShuffledQueue(keepCurrentFirst = false)
        }

        frontQueueInsertCount = saved.frontQueueInsertCount
        isQueueFinished = false
        stateRestored = true

        // Restore playback position (always paused — user taps play to resume)
        if (musicService != null) {
            musicService?.restorePlayback(restoredCurrent, saved.playbackPositionMs, andPlay = false)
        } else {
            pendingRestoreSong = restoredCurrent
            pendingRestorePosition = saved.playbackPositionMs
            pendingRestorePlay = false
        }
    }

    var playbackSpeed by mutableStateOf(settings.playbackSpeed)
        private set
    var playbackPitch by mutableStateOf(settings.playbackPitch)
        private set
    var isTuning432 by mutableStateOf(settings.isTuning432)
        private set
    var reverbPreset by mutableStateOf(settings.reverbPreset)
        private set
    var balance by mutableStateOf(settings.balance)
        private set
    var dynamicsPreset by mutableStateOf(settings.dynamicsPreset)
        private set
    var isLoudnessEnabled by mutableStateOf(settings.isLoudnessEnabled)
        private set
    var loudnessGain by mutableStateOf(settings.loudnessGain)
        private set
    var eqBandLevels by mutableStateOf(
        settings.eqBandLevels.split(",")
            .mapNotNull { it.toShortOrNull() }
    )
    var activeEqPresetName by mutableStateOf(settings.activeEqPresetName)
        private set

    var dailyListeningTime by mutableLongStateOf(settings.dailyListeningTime)
        private set

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var statsJob: Job? = null
    private var pendingStatsTimeMs: Long = 0
    
    var sleepTimerMinutes by mutableStateOf(0) // 0 means off
    private var sleepTimerHandler: android.os.Handler? = null
    private var sleepTimerRunnable: Runnable? = null

    // Visualizer
    var isFullPlayerVisualizerEnabled by mutableStateOf(settings.isFullPlayerVisualizerEnabled)
    var isMiniPlayerVisualizerEnabled by mutableStateOf(settings.isMiniPlayerVisualizerEnabled)
    
    private val _visualizerData = MutableStateFlow(FloatArray(48) { 0.1f })
    val visualizerData = _visualizerData.asStateFlow()
    private var visualizer: Visualizer? = null
    private var lastMagnitudes = FloatArray(48) { 0.1f }
    private val smoothingFactor = 1f // 0.7f for very fast and reactive movement

    private val _favoriteChanged = MutableSharedFlow<Pair<Long, Boolean>>(extraBufferCapacity = 1)
    val favoriteChanged: SharedFlow<Pair<Long, Boolean>> = _favoriteChanged.asSharedFlow()

    private val _refreshNotification = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshNotification: SharedFlow<Unit> = _refreshNotification.asSharedFlow()




    // Audio Output Monitoring
    var currentOutputName by mutableStateOf(context.getString(R.string.output_speaker))
    var currentOutputIcon by mutableStateOf(Icons.Default.Speaker)
    var currentVolumePercent by mutableStateOf(0f)
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolumeState()
            }
        }
    }

    private fun updateVolumeState() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolumePercent = if (max > 0) current.toFloat() / max else 0f
    }

    private val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                updateAudioOutput()
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                updateAudioOutput()
            }
        }
    } else null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            pendingPlaySong?.let { 
                musicService?.playSong(it)
                pendingPlaySong = null
            }
            pendingRestoreSong?.let { song ->
                musicService?.restorePlayback(song, pendingRestorePosition, pendingRestorePlay)
                pendingRestoreSong = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { 
                    INSTANCE = it
                    it.bindService()
                    it.initAudioOutputMonitoring()
                }
            }
        }
    }

    private fun initAudioOutputMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }
        updateAudioOutput()
        updateVolumeState()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(volumeReceiver, filter)
    }

    private fun updateAudioOutput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var bestDevice: AudioDeviceInfo? = null
            
            // Priority: Bluetooth > Wired > Speaker
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        bestDevice = device
                        break 
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        if (bestDevice == null || (bestDevice.type != AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && bestDevice.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO)) {
                            bestDevice = device
                        }
                    }
                }
            }
            
            if (bestDevice != null) {
                currentOutputName = if (bestDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || bestDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    context.getString(R.string.output_bluetooth)
                } else {
                    context.getString(R.string.output_headphones)
                }
                currentOutputIcon = if (bestDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || bestDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    Icons.Default.Bluetooth
                } else {
                    Icons.Default.Headphones
                }
            } else {
                currentOutputName = context.getString(R.string.output_speaker)
                currentOutputIcon = Icons.Default.Speaker
            }
        } else {
            // Simplified for older APIs
            currentOutputName = context.getString(R.string.output_speaker)
            currentOutputIcon = Icons.Default.Speaker
        }
    }

    private fun bindService() {
        Intent(context, MusicService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun play(song: Song, playlist: List<Song> = emptyList(), playlistId: Long? = null, playlistName: String? = null, category: String? = null, fromQueue: Boolean = false, shuffleMode: Boolean? = null, sections: List<QueuedSection> = emptyList()) {
        flushPendingStats()
        currentSong = song
        isPlaying = true
        if (playlist.isNotEmpty() && (playlist != activePlaylist || activePlaylist.isEmpty() || playlistId != activePlaylistId)) {
            queueSections = sections
            activePlaylist = playlist
            activePlaylistId = playlistId
            activePlaylistName = playlistName
            resetQueueCounts()
            
            if (category != null) {
                activeCategory = category
                settings.activeCategory = category
            }

            // Sync shuffle state for this playlist/context
            if (playlistId != null) {
                isShuffle = shuffleMode ?: settings.getPlaylistShuffle(playlistId)
                if (shuffleMode != null && settings.getPlaylistShuffle(playlistId) != shuffleMode) {
                    settings.setPlaylistShuffle(playlistId, shuffleMode)
                }
            } else {
                isShuffle = shuffleMode ?: settings.isShuffle
                if (shuffleMode != null) settings.isShuffle = shuffleMode
            }
            
            if (isShuffle) {
                if (shuffleMode != null) {
                    updateShuffledQueue(keepCurrentFirst = false)
                } else {
                    updateShuffledQueue()
                }
            }
        } else if (!fromQueue && playlist.isNotEmpty() && isShuffle) {
            // Clicked from a playlist that is already active
            updateShuffledQueue()
        }
        
        isQueueFinished = false
        settings.lastPlayedSongId = song.id
        
        startStatsTracking()
        // Ensure currentShufflePosition is updated
        if (isShuffle && (shuffledIndices.size != activePlaylist.size)) {
            updateShuffledQueue()
        } else if (isShuffle && shuffledIndices.isNotEmpty()) {
            val idx = activePlaylist.indexOfFirst { it.id == song.id }
            if (idx != -1) {
                if (fromQueue) {
                    val posInShuffle = shuffledIndices.indexOf(idx)
                    if (posInShuffle != -1 && posInShuffle > currentShufflePosition) {
                        val mutableShuffle = shuffledIndices.toMutableList()
                        mutableShuffle.removeAt(posInShuffle)
                        currentShufflePosition++
                        mutableShuffle.add(currentShufflePosition, idx)
                        shuffledIndices = mutableShuffle
                    } else {
                        currentShufflePosition = posInShuffle
                    }
                } else {
                    currentShufflePosition = shuffledIndices.indexOf(idx)
                }
            }
        }

        // Start service as foreground when playing begins
        Intent(context, MusicService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        if (musicService != null) {
            musicService?.playSong(song)
            startVisualizer()
        } else {
            pendingPlaySong = song
        }
        
        // Record stat: New Play
        updatePlaybackStats("SONG", "SONG_${song.id}", incrementCount = true)

        savePlaybackState(wasPlaying = true)
    }

    fun startVisualizer() {
        if (!isFullPlayerVisualizerEnabled && !isMiniPlayerVisualizerEnabled) {
            stopVisualizer()
            return
        }

        // Check permission before starting
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        val sessionId = musicService?.getAudioSessionId() ?: 0
        if (sessionId == 0) return

        try {
            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, data: ByteArray?, samplingRate: Int) {
                        if (data == null) return
                        val barCount = 48
                        val newMagnitudes = FloatArray(barCount)
                        
                        // Spreading frequency capture across a wider range
                        for (i in 0 until barCount) {
                            val index = 2 + (i * 4) // Skip DC/Nyquist and sample every 2nd frequency
                            if (index + 1 >= data.size) break
                            
                            val real = data[index].toInt()
                            val imag = data[index + 1].toInt()
                            val magnitude = Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
                            
                            // Normalize with a much lower divisor (32f) for maximum "growth"
                            val normalized = (magnitude / 32f).coerceIn(0.1f, 1.2f)
                            
                            // Exponential Moving Average (EMA) smoothing
                            newMagnitudes[i] = lastMagnitudes[i] + smoothingFactor * (normalized - lastMagnitudes[i])
                        }
                        
                        lastMagnitudes = newMagnitudes
                        _visualizerData.value = newMagnitudes
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        val resetData = FloatArray(48) { 0.1f }
        lastMagnitudes = resetData
        _visualizerData.value = resetData
    }

    fun toggleFullPlayerVisualizer() {
        isFullPlayerVisualizerEnabled = !isFullPlayerVisualizerEnabled
        settings.isFullPlayerVisualizerEnabled = isFullPlayerVisualizerEnabled
        if (isFullPlayerVisualizerEnabled) startVisualizer() else if (!isMiniPlayerVisualizerEnabled) stopVisualizer()
    }

    fun toggleMiniPlayerVisualizer() {
        isMiniPlayerVisualizerEnabled = !isMiniPlayerVisualizerEnabled
        settings.isMiniPlayerVisualizerEnabled = isMiniPlayerVisualizerEnabled
        if (isMiniPlayerVisualizerEnabled) startVisualizer() else if (!isFullPlayerVisualizerEnabled) stopVisualizer()
    }

    private fun updateShuffledQueue(keepCurrentFirst: Boolean = true) {
        if (activePlaylist.isEmpty()) return

        if (queueSections.isNotEmpty()) {
            val allIndices = mutableListOf<Int>()
            val currentIdx = activePlaylist.indexOfFirst { it.id == currentSong?.id }

            for (section in queueSections) {
                val sectionEnd = minOf(section.startIndex + section.count, activePlaylist.size)
                if (section.startIndex >= activePlaylist.size) continue
                val indices = (section.startIndex until sectionEnd).toMutableList()
                indices.shuffle()
                allIndices.addAll(indices)
            }

            if (keepCurrentFirst && currentIdx != -1) {
                val pos = allIndices.indexOf(currentIdx)
                if (pos > 0) {
                    allIndices.removeAt(pos)
                    allIndices.add(0, currentIdx)
                }
            }
            currentShufflePosition = 0
            shuffledIndices = allIndices
        } else {
            val allIndices = activePlaylist.indices.toMutableList()
            val currentIdx = activePlaylist.indexOfFirst { it.id == currentSong?.id }

            if (keepCurrentFirst && currentIdx != -1) {
                allIndices.remove(currentIdx)
                allIndices.shuffle()
                allIndices.add(0, currentIdx)
            } else {
                allIndices.shuffle()
            }

            currentShufflePosition = 0
            shuffledIndices = allIndices
        }
    }


    fun playNextFromService(isNaturalEnd: Boolean = false, startPlayback: Boolean = true) {
        if (activePlaylist.isEmpty()) return
        
        if (repeatMode == 1) { // Repeat One
            if (startPlayback) {
                currentSong?.let { play(it) }
            }
            return
        }

        val advance = if (isTransitioning) 2 else 1

        val nextSong = if (isShuffle) {
            if (shuffledIndices.size != activePlaylist.size) {
                updateShuffledQueue()
            }
            val nextPos = (currentShufflePosition + advance)
            if (nextPos >= shuffledIndices.size) {
                if (repeatMode == 2) { // Repeat All
                    currentShufflePosition = 0
                    activePlaylist[shuffledIndices[0]]
                } else {
                    if (!isNaturalEnd) return
                    // Natural end of queue: show Play icon and reset progress
                    isPlaying = false
                    isQueueFinished = true
                    playbackStateSaver.clear()
                    musicService?.resetPlayerProgress()
                    return
                }
            } else {
                currentShufflePosition = nextPos
                activePlaylist[shuffledIndices[currentShufflePosition]]
            }
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == currentSong?.id }
            val targetIndex = if (currentIndex != -1) currentIndex + advance else -1
            if (targetIndex != -1 && targetIndex < activePlaylist.size) {
                activePlaylist[targetIndex]
            } else if (repeatMode == 2) { // Repeat All
                activePlaylist[0]
            } else {
                if (!isNaturalEnd) return
                // Natural end of queue: show Play icon and reset progress
                isPlaying = false
                isQueueFinished = true
                playbackStateSaver.clear()
                musicService?.resetPlayerProgress()
                return
            }
        }

        if (startPlayback) {
            play(nextSong)
        } else {
            currentSong = nextSong
        }
    }

    fun playPreviousFromService() {
        if (activePlaylist.isEmpty()) return
        
        // Spotify-style: if > 3s into song, restart it; otherwise go to previous
        val currentPos = musicService?.currentPosition() ?: 0
        if (currentPos > 3000) {
            musicService?.seekTo(0)
            return
        }
        
        val prevSong = if (isShuffle) {
            if (shuffledIndices.size != activePlaylist.size) {
                updateShuffledQueue()
            }
            currentShufflePosition = if (currentShufflePosition > 0) currentShufflePosition - 1 else shuffledIndices.size - 1
            activePlaylist[shuffledIndices[currentShufflePosition]]
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex > 0) {
                activePlaylist[currentIndex - 1]
            } else {
                return
            }
        }
        play(prevSong)
    }

    fun getNextSong(): Song? {
        if (activePlaylist.isEmpty() || currentSong == null) return null
        if (repeatMode == 1) return currentSong

        return if (isShuffle) {
            if (shuffledIndices.size != activePlaylist.size) {
                updateShuffledQueue()
            }
            val nextPos = (currentShufflePosition + 1)
            if (nextPos >= shuffledIndices.size) {
                if (repeatMode == 2) activePlaylist[shuffledIndices[0]] else null
            } else {
                activePlaylist[shuffledIndices[nextPos]]
            }
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex != -1 && currentIndex < activePlaylist.size - 1) {
                activePlaylist[currentIndex + 1]
            } else if (repeatMode == 2) {
                activePlaylist[0]
            } else {
                null
            }
        }
    }

    /** Called after a metadata edit — updates currentSong and queue without changing isPlaying */
    fun updateSongMetadata(song: Song) {
        if (currentSong?.id == song.id) {
            currentSong = song
        }
        
        // Update the song in the active playlist (queue)
        val newList = activePlaylist.map { 
            if (it.id == song.id) song else it 
        }
        if (newList != activePlaylist) {
            activePlaylist = newList
        }
    }

    fun updateLyrics(lyrics: String?) {
        Log.i("PlaybackManager", "Updating lyrics. Length: ${lyrics?.length ?: 0}")
        currentLyrics = lyrics
    }

    fun clearLyrics() {
        Log.i("PlaybackManager", "Clearing lyrics")
        currentLyrics = null
    }

    /** Called by MusicService after a crossfade completes — updates state without re-triggering playSong */
    fun updateCurrentSongState(song: Song) {
        currentSong = song
        isPlaying = true
        startVisualizer()
        // Sync shuffle position if needed
        if (isShuffle && shuffledIndices.isNotEmpty()) {
            val idx = activePlaylist.indexOfFirst { it.id == song.id }
            if (idx != -1) {
                currentShufflePosition = shuffledIndices.indexOf(idx)
            }
        }
        
        // Record stat: New Play (Automatic/Crossfade)
        updatePlaybackStats("SONG", "SONG_${song.id}", incrementCount = true)
    }

    fun pause() {
        savePlaybackState(wasPlaying = false)
        flushPendingStats()
        isPlaying = false
        musicService?.pause()
        stopStatsTracking()
    }

    fun resume() {
        if (isQueueFinished && activePlaylist.isNotEmpty()) {
            isQueueFinished = false
            val firstSong = if (isShuffle) {
                updateShuffledQueue(keepCurrentFirst = false)
                activePlaylist[shuffledIndices[0]]
            } else {
                activePlaylist[0]
            }
            play(firstSong)
            return
        }
        isPlaying = true
        musicService?.resume()
        startStatsTracking()
    }

    fun stop() {
        flushPendingStats()
        isPlaying = false
        musicService?.stopSelf()
        stopStatsTracking()
    }

    fun stopAndClearQueue() {
        stop()
        activePlaylist = emptyList()
        activePlaylistId = null
        activePlaylistName = null
        activeCategory = null
        currentSong = null
        settings.activeCategory = ""
    }

    private fun startStatsTracking() {
        if (statsJob != null) return
        statsJob = managerScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                if (!isSameDay(now, settings.lastStatsResetTimestamp)) {
                    pendingStatsTimeMs = 0
                    dailyListeningTime = 0
                    settings.dailyListeningTime = 0
                    settings.lastStatsResetTimestamp = now
                } else {
                    pendingStatsTimeMs += 1000
                    dailyListeningTime += 1000
                    settings.dailyListeningTime = dailyListeningTime
                }
            }
        }
    }

    private fun flushPendingStats() {
        if (pendingStatsTimeMs == 0L) return
        val song = currentSong
        if (song != null) {
            updatePlaybackStats("SONG", "SONG_${song.id}", timeMs = pendingStatsTimeMs)
            if (song.artist.isNotBlank() && song.artist != "<unknown>") {
                updatePlaybackStats("ARTIST", "ARTIST_${song.artist}", timeMs = pendingStatsTimeMs)
            }
        }
        val pId = activePlaylistId
        if (pId != null && pId != -1L) {
            updatePlaybackStats("PLAYLIST", "PLAYLIST_$pId", timeMs = pendingStatsTimeMs)
        }
        pendingStatsTimeMs = 0
    }

    private fun stopStatsTracking() {
        statsJob?.cancel()
        statsJob = null
    }

    private fun updatePlaybackStats(type: String, id: String, timeMs: Long = 0, incrementCount: Boolean = false) {
        managerScope.launch(Dispatchers.IO) {
            try {
                val db = com.demonlab.lune.data.MusicDatabase.getDatabase(context)
                val dao = db.playbackStatsDao()
                val existing = dao.getStatsById(id)
                if (existing != null) {
                    dao.insertStats(existing.copy(
                        playCount = if (incrementCount) existing.playCount + 1 else existing.playCount,
                        totalTimeMs = existing.totalTimeMs + timeMs,
                        lastPlayed = System.currentTimeMillis()
                    ))
                } else {
                    dao.insertStats(com.demonlab.lune.data.PlaybackStats(
                        id = id,
                        type = type,
                        playCount = if (incrementCount) 1 else 0,
                        totalTimeMs = timeMs
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun setSortSettings(option: String, ascending: Boolean) {
        sortOption = option
        isSortAscending = ascending
        settings.sortOption = option
        settings.isSortAscending = ascending
        
        resortActivePlaylist()
    }

    fun resortActivePlaylist() {
        if (activePlaylist.isNotEmpty()) {
            activePlaylist = getSortedList(activePlaylist)
            if (isShuffle) {
                updateShuffledQueue()
            }
        }
    }

    fun getSortedList(list: List<Song>, option: String = sortOption, ascending: Boolean = isSortAscending): List<Song> {
        val comparator = when (option) {
            "ALPHABETICAL" -> compareBy<Song> { it.title.lowercase(java.util.Locale.getDefault()) }
            "ARTIST" -> compareBy<Song> { it.artist.lowercase(java.util.Locale.getDefault()) }
            "DURATION" -> compareBy<Song> { it.duration }
            "DATE_ADDED" -> compareBy<Song> { it.dateAdded }
            else -> compareBy<Song> { it.title.lowercase(java.util.Locale.getDefault()) }
        }
        
        return if (ascending) {
            list.sortedWith(comparator)
        } else {
            list.sortedWith(comparator.reversed())
        }
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        resetQueueCounts()

        val pId = activePlaylistId
        if (pId != null) {
            settings.setPlaylistShuffle(pId, isShuffle)
        }
        settings.isShuffle = isShuffle

        if (isShuffle) updateShuffledQueue()

        _refreshNotification.tryEmit(Unit)
    }

    fun toggleCrossfade() {
        isCrossfade = !isCrossfade
        settings.isCrossfade = isCrossfade
    }

    fun toggleAutomix() {
        isAutomix = !isAutomix
        settings.isAutomix = isAutomix
    }

    fun toggleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3
        settings.repeatMode = repeatMode
    }

    fun getEqNumberOfBands(): Short = musicService?.equalizer?.numberOfBands ?: 0
    fun getEqBandLevelRange(): ShortArray? = musicService?.equalizer?.bandLevelRange
    fun getEqCenterFreq(band: Short): Int = musicService?.equalizer?.getCenterFreq(band) ?: 0
    fun getEqBandLevel(band: Short): Short = musicService?.equalizer?.getBandLevel(band) ?: 0

    fun refreshEqState() {
        eqBandsCount = musicService?.equalizer?.numberOfBands ?: 0
        eqBandsRange = musicService?.equalizer?.bandLevelRange
    }

    fun toggleEq() {
        isEqEnabled = !isEqEnabled
        settings.isEqEnabled = isEqEnabled
        musicService?.setEqEnabled(isEqEnabled)
        if (!isEqEnabled) {
            settings.lastEqPresetName = activeEqPresetName
            settings.lastEqBandLevels = eqBandLevels.joinToString(",")
            activeEqPresetName = ""
            settings.activeEqPresetName = ""
        } else {
            val lastName = settings.lastEqPresetName
            val lastLevels = settings.lastEqBandLevels
            if (lastLevels.isNotBlank()) {
                val levelList = lastLevels.split(",").mapNotNull { it.toShortOrNull() }
                if (levelList.size == getEqNumberOfBands().toInt()) {
                    for (i in levelList.indices) {
                        musicService?.setEqBandLevel(i.toShort(), levelList[i])
                    }
                    eqBandLevels = levelList.toList()
                    settings.eqBandLevels = eqBandLevels.joinToString(",")
                }
                activeEqPresetName = lastName
                settings.activeEqPresetName = lastName
            } else {
                applyEqPreset(0)
            }
        }
    }

    fun setEqBandLevel(band: Short, level: Short) {
        val adjusted = if (band <= 1 && bassBoostOffset != 0.toShort()) {
            (level + bassBoostOffset).coerceIn(-1500, 1500).toShort()
        } else {
            level
        }
        musicService?.setEqBandLevel(band, adjusted)
        val numBands = getEqNumberOfBands().toInt()
        val currentBands = eqBandLevels.toMutableList()
        if (currentBands.size != numBands) {
            currentBands.clear()
            for (i in 0 until numBands) {
                currentBands.add(getEqBandLevel(i.toShort()))
            }
        }
        if (band < currentBands.size) {
            currentBands[band.toInt()] = level
        }
        eqBandLevels = currentBands.toList()
        settings.eqBandLevels = eqBandLevels.joinToString(",")
    }

    fun resetEq() {
        if (activeEqPresetName.isEmpty()) {
            val numBands = getEqNumberOfBands()
            for (i in 0 until numBands) {
                setEqBandLevel(i.toShort(), 0)
            }
        } else if (isCustomPreset(activeEqPresetName)) {
            val saved = getSavedCustomPresets().find { it.first == activeEqPresetName }
            if (saved != null) {
                applyCustomPreset(saved.first, saved.second)
            }
        } else {
            val presets = getEqPresets()
            val index = presets.indexOf(activeEqPresetName)
            if (index >= 0) {
                applyEqPreset(index.toShort())
            }
        }
    }

    fun getEqPresets(): List<String> {
        val count = musicService?.equalizer?.numberOfPresets ?: 0
        val presets = mutableListOf<String>()
        for (i in 0 until count) {
            presets.add(musicService?.equalizer?.getPresetName(i.toShort()) ?: "Preset $i")
        }
        return presets
    }

    fun applyEqPreset(presetIndex: Short) {
        if (!isEqEnabled) return
        try {
            musicService?.equalizer?.usePreset(presetIndex)
            val numBands = getEqNumberOfBands().toInt()
            val currentBands = mutableListOf<Short>()
            for (i in 0 until numBands) {
                currentBands.add(getEqBandLevel(i.toShort()))
            }
            eqBandLevels = currentBands.toList()
            settings.eqBandLevels = eqBandLevels.joinToString(",")
            // Track active preset name
            val name = musicService?.equalizer?.getPresetName(presetIndex) ?: ""
            activeEqPresetName = name
            settings.activeEqPresetName = name
            if (isBassBoostEnabled) {
                for (i in 0..1) {
                    if (i < eqBandLevels.size) {
                        val adjusted = (eqBandLevels[i] + bassBoostOffset).coerceIn(-1500, 1500).toShort()
                        musicService?.setEqBandLevel(i.toShort(), adjusted)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedCustomPresets(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        try {
            val json = org.json.JSONArray(settings.customEqPresetsJson)
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                result.add(obj.getString("name") to obj.getString("levels"))
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveCustomEqPreset(name: String) {
        val levels = eqBandLevels.joinToString(",")
        val presets = try {
            org.json.JSONArray(settings.customEqPresetsJson)
        } catch (_: Exception) {
            org.json.JSONArray()
        }
        // Replace existing with same name or add new
        var found = false
        for (i in 0 until presets.length()) {
            val obj = presets.getJSONObject(i)
            if (obj.getString("name") == name) {
                obj.put("levels", levels)
                found = true
                break
            }
        }
        if (!found) {
            val entry = org.json.JSONObject()
            entry.put("name", name)
            entry.put("levels", levels)
            presets.put(entry)
        }
        settings.customEqPresetsJson = presets.toString()
        activeEqPresetName = name
        settings.activeEqPresetName = name
    }

    fun deleteCustomEqPreset(name: String) {
        val presets = try {
            org.json.JSONArray(settings.customEqPresetsJson)
        } catch (_: Exception) {
            org.json.JSONArray()
        }
        val updated = org.json.JSONArray()
        for (i in 0 until presets.length()) {
            val obj = presets.getJSONObject(i)
            if (obj.getString("name") != name) {
                updated.put(obj)
            }
        }
        settings.customEqPresetsJson = updated.toString()
        if (activeEqPresetName == name) {
            applyEqPreset(0)
        }
    }

    fun isCustomPreset(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        if (name == "Custom") return true
        return getSavedCustomPresets().any { it.first == name }
    }

    fun applyCustomPreset(name: String, levels: String) {
        val levelList = levels.split(",").mapNotNull { it.toShortOrNull() }
        if (levelList.size == getEqNumberOfBands().toInt()) {
            for (i in levelList.indices) {
                musicService?.setEqBandLevel(i.toShort(), levelList[i])
            }
            eqBandLevels = levelList.toList()
            settings.eqBandLevels = eqBandLevels.joinToString(",")
            activeEqPresetName = name
            settings.activeEqPresetName = name
            if (isBassBoostEnabled) {
                for (i in 0..1) {
                    if (i < eqBandLevels.size) {
                        val adjusted = (eqBandLevels[i] + bassBoostOffset).coerceIn(-1500, 1500).toShort()
                        musicService?.setEqBandLevel(i.toShort(), adjusted)
                    }
                }
            }
        }
    }

    fun toggleBassBoost() {
        isBassBoostEnabled = !isBassBoostEnabled
        settings.isBassBoostEnabled = isBassBoostEnabled
        if (isBassBoostEnabled) {
            bassBoostOffset = 500.toShort()
            for (i in 0..1) {
                if (i < eqBandLevels.size) {
                    val adjusted = (eqBandLevels[i] + bassBoostOffset).coerceIn(-1500, 1500).toShort()
                    musicService?.setEqBandLevel(i.toShort(), adjusted)
                }
            }
            musicService?.setBassBoostEnabled(true)
            musicService?.setBassBoostStrength(200)
        } else {
            bassBoostOffset = 0
            for (i in 0..1) {
                if (i < eqBandLevels.size) {
                    musicService?.setEqBandLevel(i.toShort(), eqBandLevels[i])
                }
            }
            musicService?.setBassBoostEnabled(false)
        }
    }

    fun toggleSpatialAudio() {
        isSpatialAudioEnabled = !isSpatialAudioEnabled
        settings.isSpatialAudioEnabled = isSpatialAudioEnabled
        musicService?.setSpatialAudioEnabled(isSpatialAudioEnabled)
    }

    fun updateReverbPreset(preset: Int) {
        reverbPreset = preset
        settings.reverbPreset = preset
        musicService?.setReverbPreset(preset)
    }

    fun updateBalance(balance: Float) {
        this.balance = balance
        settings.balance = balance
        musicService?.applyBalance(balance)
    }

    fun updateDynamicsPreset(preset: Int) {
        dynamicsPreset = preset
        settings.dynamicsPreset = preset
        musicService?.setDynamicsPreset(preset)
    }

    fun toggleLoudness() {
        isLoudnessEnabled = !isLoudnessEnabled
        settings.isLoudnessEnabled = isLoudnessEnabled
        if (!isLoudnessEnabled) {
            loudnessGain = 0
            settings.loudnessGain = 0
        }
        musicService?.setLoudnessEnabled(isLoudnessEnabled)
        musicService?.setLoudnessGain(loudnessGain)
    }

    fun updateLoudnessGain(gain: Int) {
        loudnessGain = gain
        settings.loudnessGain = gain
        musicService?.setLoudnessGain(gain)
    }

    fun toggleSleepTimer() {
        sleepTimerMinutes = when (sleepTimerMinutes) {
            0 -> 15
            15 -> 30
            30 -> 60
            else -> 0
        }
        
        startSleepTimer()
    }

    private fun startSleepTimer() {
        sleepTimerHandler?.removeCallbacks(sleepTimerRunnable ?: return)
        if (sleepTimerMinutes > 0) {
            if (sleepTimerHandler == null) sleepTimerHandler = android.os.Handler(android.os.Looper.getMainLooper())
            sleepTimerRunnable = Runnable {
                pause()
                sleepTimerMinutes = 0
            }
            sleepTimerHandler?.postDelayed(sleepTimerRunnable!!, sleepTimerMinutes * 60 * 1000L)
        }
    }

    fun seekTo(progress: Float) {
        musicService?.let {
            val position = (progress * it.duration()).toInt()
            it.seekTo(position)
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        settings.playbackSpeed = speed
        musicService?.setPlaybackParams(speed, playbackPitch)
    }

    fun updatePitch(pitch: Float) {
        playbackPitch = pitch
        settings.playbackPitch = pitch
        musicService?.setPlaybackParams(playbackSpeed, pitch)
    }

    fun toggleTuning432() {
        isTuning432 = !isTuning432
        settings.isTuning432 = isTuning432
        val target = if (isTuning432) 432f / 440f else 1.0f
        updatePitch(target)
    }

    fun getProgress(): Float {
        musicService?.let {
            val duration = it.duration()
            if (duration > 0) {
                return it.currentPosition().toFloat() / duration
            }
        }
        return 0f
    }

    fun toggleFavorite(song: Song? = null, onFavoriteToggled: ((Song) -> Unit)? = null): Song? {
        val targetSong = song ?: currentSong ?: return null
        val newFavoriteStatus = !targetSong.isFavorite
        val updatedSong = targetSong.copy(isFavorite = newFavoriteStatus)
        
        if (song == null || currentSong?.id == targetSong.id) {
            currentSong = updatedSong
        }
        
        // Update the song in the active playlist too
        val newList = activePlaylist.map { 
            if (it.id == targetSong.id) updatedSong else it 
        }
        if (newList != activePlaylist) {
            activePlaylist = newList
        }
        
        // Persist to DB
        val metadataManager = MetadataManager(context)
        kotlinx.coroutines.MainScope().launch {
            metadataManager.updateFavoriteStatus(targetSong.id, newFavoriteStatus)
            _favoriteChanged.emit(targetSong.id to newFavoriteStatus)
            _refreshNotification.tryEmit(Unit)
            onFavoriteToggled?.invoke(updatedSong)
        }
        
        return updatedSong
    }

    fun updatePlayingState(playing: Boolean) {
        isPlaying = playing
    }

    var isInAnyPlaylist by mutableStateOf(false)
        private set

    fun checkPlaylistStatus() {
        val songId = currentSong?.id ?: return
        kotlinx.coroutines.MainScope().launch {
            val count = com.demonlab.lune.data.MusicDatabase.getDatabase(context).playlistDao().getPlaylistCountForSong(songId)
            isInAnyPlaylist = count > 0
        }
    }

    fun getCurrentQueue(): List<Song> {
        if (activePlaylist.isEmpty()) return emptyList()
        return if (isShuffle && shuffledIndices.size == activePlaylist.size) {
            shuffledIndices.map { activePlaylist[it] }
        } else {
            activePlaylist
        }
    }

    fun reorderQueueForSong(song: Song, moveToFront: Boolean) {
        val current = currentSong ?: return
        if (moveToFront) {
            val oldIdx = activePlaylist.indexOfFirst { it.id == song.id }
            activePlaylist = QueueManager.moveToFront(activePlaylist, current, song, frontQueueInsertCount)
            val newIdx = activePlaylist.indexOfFirst { it.id == song.id }

            if (oldIdx != newIdx) {
                frontQueueInsertCount++

                if (isShuffle && shuffledIndices.isNotEmpty()) {
                    val mutable = shuffledIndices.toMutableList()

                    mutable.remove(oldIdx)
                    for (i in mutable.indices) {
                        val v = mutable[i]
                        if (oldIdx < newIdx && v in (oldIdx + 1)..newIdx) {
                            mutable[i] = v - 1
                        } else if (oldIdx > newIdx && v in newIdx until oldIdx) {
                            mutable[i] = v + 1
                        }
                    }

                    mutable.remove(newIdx)
                    val currentSongIdx = activePlaylist.indexOfFirst { it.id == current.id }
                    val insertPos = (mutable.indexOf(currentSongIdx) + 1).coerceAtMost(mutable.size)
                    mutable.add(insertPos, newIdx)
                    shuffledIndices = mutable
                }
            }
        } else {
            val oldIdx = activePlaylist.indexOfFirst { it.id == song.id }
            activePlaylist = QueueManager.moveToEnd(activePlaylist, current, song)
            val newIdx = activePlaylist.indexOfFirst { it.id == song.id }

            if (oldIdx != newIdx && isShuffle && shuffledIndices.isNotEmpty()) {
                val mutable = shuffledIndices.toMutableList()
                mutable.remove(oldIdx)
                for (i in mutable.indices) {
                    val v = mutable[i]
                    if (oldIdx < newIdx && v in (oldIdx + 1)..newIdx) {
                        mutable[i] = v - 1
                    } else if (oldIdx > newIdx && v in newIdx until oldIdx) {
                        mutable[i] = v + 1
                    }
                }
                mutable.remove(newIdx)
                mutable.add(newIdx)
                shuffledIndices = mutable
            }
        }
    }

    fun setVolume(percent: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (percent * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    fun removeFromQueue(songId: Long) {
        val idx = activePlaylist.indexOfFirst { it.id == songId }
        if (idx == -1) return

        val mutable = activePlaylist.toMutableList()
        mutable.removeAt(idx)
        activePlaylist = mutable

        if (queueSections.isNotEmpty()) {
            queueSections = queueSections.map { section ->
                val sectionEnd = section.startIndex + section.count
                var newStart = section.startIndex
                var newCount = section.count
                if (idx < section.startIndex) newStart--
                if (idx in section.startIndex until sectionEnd) newCount--
                QueuedSection(section.title, newStart, newCount)
            }
        }

        if (isShuffle && shuffledIndices.isNotEmpty()) {
            val shufMutable = shuffledIndices.toMutableList()
            shufMutable.remove(idx)
            for (i in shufMutable.indices) {
                if (shufMutable[i] > idx) shufMutable[i]--
            }
            shuffledIndices = shufMutable
        }
    }

    fun moveToNextInQueue(songId: Long) {
        val song = activePlaylist.find { it.id == songId } ?: return
        val current = currentSong ?: return
        if (song.id == current.id) return

        val mutable = activePlaylist.toMutableList()
        val oldIdx = mutable.indexOfFirst { it.id == songId }

        val songSection = if (queueSections.isNotEmpty()) {
            queueSections.firstOrNull { oldIdx in it.startIndex until (it.startIndex + it.count) }
        } else null

        mutable.removeAt(oldIdx)
        val currentIdx = mutable.indexOfFirst { it.id == current.id }

        var insertPos = currentIdx + 1
        if (songSection != null) {
            insertPos = insertPos.coerceIn(songSection.startIndex, songSection.startIndex + songSection.count - 1)
        }

        insertPos = insertPos.coerceIn(0, mutable.size)
        mutable.add(insertPos, song)
        activePlaylist = mutable
        val newIdx = insertPos

        // Update queueSections to reflect the new activePlaylist layout
        if (queueSections.isNotEmpty()) {
            val songSectionIdx = queueSections.indexOfFirst { oldIdx in it.startIndex until (it.startIndex + it.count) }
            queueSections = queueSections.mapIndexed { idx, section ->
                val sectionEnd = section.startIndex + section.count
                var newStart = section.startIndex
                var newCount = section.count

                if (oldIdx < section.startIndex) newStart--
                if (oldIdx in section.startIndex until sectionEnd) newCount--

                if (idx == songSectionIdx) {
                    newCount++
                    if (insertPos < newStart) newStart++
                } else {
                    if (insertPos <= newStart) newStart++
                }

                QueuedSection(section.title, newStart, newCount)
            }
        }

        if (isShuffle && shuffledIndices.isNotEmpty()) {
            val shufMutable = shuffledIndices.toMutableList()
            shufMutable.remove(oldIdx)
            for (i in shufMutable.indices) {
                val v = shufMutable[i]
                if (oldIdx < newIdx && v in (oldIdx + 1)..newIdx) {
                    shufMutable[i] = v - 1
                } else if (oldIdx > newIdx && v in newIdx until oldIdx) {
                    shufMutable[i] = v + 1
                }
            }
            val currentSongIdx = activePlaylist.indexOfFirst { it.id == current.id }
            val currentPosInShuffle = shufMutable.indexOf(currentSongIdx)
            var insertPosShuffle = (currentPosInShuffle + 1).coerceAtMost(shufMutable.size)

            if (songSection != null) {
                insertPosShuffle = insertPosShuffle.coerceIn(
                    songSection.startIndex,
                    songSection.startIndex + songSection.count
                )
            }

            insertPosShuffle = insertPosShuffle.coerceAtMost(shufMutable.size)
            shufMutable.add(insertPosShuffle, newIdx)
            shuffledIndices = shufMutable
        }
    }
}

data class QueuedSection(val title: String, val startIndex: Int, val count: Int)