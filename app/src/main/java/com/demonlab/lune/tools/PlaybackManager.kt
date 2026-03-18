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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.util.Calendar

class PlaybackManager private constructor(private val context: Context) {
    private val settings = SettingsManager.getInstance(context)
    private var musicService: MusicService? = null
    private var isBound = false
    private var pendingPlaySong: Song? = null
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
    var activeCategory by mutableStateOf<String?>(settings.activeCategory)
        private set
    private var shuffledIndices: List<Int> = emptyList()
    private var currentShufflePosition: Int = -1
    
    var isShuffle by mutableStateOf(settings.isShuffle)
    var isCrossfade by mutableStateOf(settings.isCrossfade)
    var isAutomix by mutableStateOf(settings.isAutomix)
    var repeatMode by mutableStateOf(settings.repeatMode) // 0: Off, 1: One, 2: All
    var isQueueFinished by mutableStateOf(false) // true when last song ended naturally
    var isTransitioning by mutableStateOf(false) 
    
    var isEqEnabled by mutableStateOf(settings.isEqEnabled)
        private set
    var isBassBoostEnabled by mutableStateOf(settings.isBassBoostEnabled)
        private set
    var isSpatialAudioEnabled by mutableStateOf(settings.isSpatialAudioEnabled)
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

    fun play(song: Song, playlist: List<Song> = emptyList(), playlistId: Long? = null, category: String? = null) {
        currentSong = song
        isPlaying = true
        if (playlist.isNotEmpty() && (playlist != activePlaylist || activePlaylist.isEmpty() || playlistId != activePlaylistId)) {
            activePlaylist = playlist
            activePlaylistId = playlistId
            
            if (category != null) {
                activeCategory = category
                settings.activeCategory = category
            }
        }
        
        // Sync shuffle state for this playlist/context (Moved outside if to catch state changes)
        if (playlistId != null) {
            isShuffle = settings.getPlaylistShuffle(playlistId)
        } else {
            isShuffle = settings.isShuffle
        }
        
        if (isShuffle) updateShuffledQueue()
        
        isQueueFinished = false
        settings.lastPlayedSongId = song.id
        if (playlistId != null) settings.lastPlaylistId = playlistId else settings.lastPlaylistId = -1L
        
        startStatsTracking()
        // Ensure currentShufflePosition is updated
        if (isShuffle && (shuffledIndices.size != activePlaylist.size)) {
            updateShuffledQueue()
        } else if (isShuffle && shuffledIndices.isNotEmpty()) {
            val idx = activePlaylist.indexOfFirst { it.id == song.id }
            if (idx != -1) {
                currentShufflePosition = shuffledIndices.indexOf(idx)
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
        
        val allIndices = activePlaylist.indices.toMutableList()
        val currentIdx = activePlaylist.indexOfFirst { it.id == currentSong?.id }
        
        if (keepCurrentFirst && currentIdx != -1) {
            allIndices.remove(currentIdx)
            allIndices.shuffle()
            allIndices.add(0, currentIdx)
            currentShufflePosition = 0
        } else {
            allIndices.shuffle()
            currentShufflePosition = 0
        }
        
        shuffledIndices = allIndices
    }


    fun playNextFromService(isNaturalEnd: Boolean = false) {
        if (activePlaylist.isEmpty()) return
        
        if (repeatMode == 1) { // Repeat One
            currentSong?.let { play(it) }
            return
        }

        val nextSong = if (isShuffle) {
            if (shuffledIndices.size != activePlaylist.size) {
                updateShuffledQueue()
            }
            val nextPos = (currentShufflePosition + 1)
            if (nextPos >= shuffledIndices.size) {
                if (repeatMode == 2) { // Repeat All
                    currentShufflePosition = 0
                    activePlaylist[shuffledIndices[0]]
                } else {
                    if (!isNaturalEnd) return
                    // Natural end of queue: show Play icon and reset progress
                    isPlaying = false
                    isQueueFinished = true
                    musicService?.resetPlayerProgress()
                    return
                }
            } else {
                currentShufflePosition = nextPos
                activePlaylist[shuffledIndices[currentShufflePosition]]
            }
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == currentSong?.id }
            if (currentIndex != -1 && currentIndex < activePlaylist.size - 1) {
                activePlaylist[currentIndex + 1]
            } else if (repeatMode == 2) { // Repeat All
                activePlaylist[0]
            } else {
                if (!isNaturalEnd) return
                // Natural end of queue: show Play icon and reset progress
                isPlaying = false
                isQueueFinished = true
                musicService?.resetPlayerProgress()
                return
            }
        }

        play(nextSong)
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
        // Sync shuffle position if needed
        if (isShuffle && shuffledIndices.isNotEmpty()) {
            val idx = activePlaylist.indexOfFirst { it.id == song.id }
            if (idx != -1) {
                currentShufflePosition = shuffledIndices.indexOf(idx)
            }
        }
    }

    fun pause() {
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
        isPlaying = false
        musicService?.stopSelf()
        stopStatsTracking()
    }

    private fun startStatsTracking() {
        if (statsJob != null) return
        statsJob = managerScope.launch {
            while (isActive) {
                delay(1000)
                updateStats()
            }
        }
    }

    private fun stopStatsTracking() {
        statsJob?.cancel()
        statsJob = null
    }

    private fun updateStats() {
        val now = System.currentTimeMillis()
        if (!isSameDay(now, settings.lastStatsResetTimestamp)) {
            dailyListeningTime = 0
            settings.dailyListeningTime = 0
            settings.lastStatsResetTimestamp = now
        } else {
            dailyListeningTime += 1000
            settings.dailyListeningTime = dailyListeningTime
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        if (t2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        
        val pId = activePlaylistId
        if (pId != null) {
            settings.setPlaylistShuffle(pId, isShuffle)
        } else {
            settings.isShuffle = isShuffle
        }
        
        if (isShuffle) updateShuffledQueue()
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

    fun toggleEq() {
        isEqEnabled = !isEqEnabled
        settings.isEqEnabled = isEqEnabled
        musicService?.setEqEnabled(isEqEnabled)
        // Clear preset badge when EQ is disabled
        if (!isEqEnabled) {
            activeEqPresetName = ""
            settings.activeEqPresetName = ""
        }
    }

    fun setEqBandLevel(band: Short, level: Short) {
        musicService?.setEqBandLevel(band, level)
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
        val numBands = getEqNumberOfBands()
        for (i in 0 until numBands) {
            setEqBandLevel(i.toShort(), 0)
        }
        activeEqPresetName = ""
        settings.activeEqPresetName = ""
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBassBoost() {
        isBassBoostEnabled = !isBassBoostEnabled
        settings.isBassBoostEnabled = isBassBoostEnabled
        musicService?.setBassBoostEnabled(isBassBoostEnabled)
    }

    fun toggleSpatialAudio() {
        isSpatialAudioEnabled = !isSpatialAudioEnabled
        settings.isSpatialAudioEnabled = isSpatialAudioEnabled
        musicService?.setSpatialAudioEnabled(isSpatialAudioEnabled)
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

    fun getProgress(): Float {
        musicService?.let {
            val duration = it.duration()
            if (duration > 0) {
                return it.currentPosition().toFloat() / duration
            }
        }
        return 0f
    }

    fun toggleFavorite(onFavoriteToggled: (() -> Unit)? = null) {
        val song = currentSong ?: return
        val newFavoriteStatus = !song.isFavorite
        currentSong = song.copy(isFavorite = newFavoriteStatus)
        
        // Persist to DB
        val metadataManager = MetadataManager(context)
        kotlinx.coroutines.MainScope().launch {
            metadataManager.updateFavoriteStatus(song.id, newFavoriteStatus)
            onFavoriteToggled?.invoke()
        }
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

    fun setVolume(percent: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (percent * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }
}
