package com.demonlab.lune.tools

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.demonlab.lune.R
import com.demonlab.lune.ui.activities.Lune
import kotlinx.coroutines.*
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.regex.Pattern
import android.util.Log
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.widget.RemoteViews
import android.media.AudioDeviceInfo

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var secondaryPlayer: MediaPlayer? = null
    private var isCrossfading = false
    private var mediaSession: MediaSessionCompat? = null
    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    internal var equalizer: Equalizer? = null
    internal var bassBoost: BassBoost? = null
    internal var virtualizer: Virtualizer? = null
    
    private var secondaryEqualizer: Equalizer? = null
    private var secondaryBassBoost: BassBoost? = null
    private var secondaryVirtualizer: Virtualizer? = null

    private lateinit var settingsManager: SettingsManager
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeLoss = false
    private var widgetUpdateJob: Job? = null
    private var lastSongForBlur: Song? = null
    private var lastBlurredBitmap: Bitmap? = null

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (another app claimed audio): pause
                wasPlayingBeforeLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient loss (incoming call, etc): pause, try to resume later
                wasPlayingBeforeLoss = isPlaying()
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Notification/brief sound: lower volume
                mediaPlayer?.setVolume(0.2f, 0.2f)
                secondaryPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus: restore volume and resume if we were playing
                mediaPlayer?.setVolume(1f, 1f)
                secondaryPlayer?.setVolume(1f, 1f)
                if (wasPlayingBeforeLoss) {
                    resume()
                    wasPlayingBeforeLoss = false
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY = "com.demonlab.lune.ACTION_PLAY"
        const val ACTION_PAUSE = "com.demonlab.lune.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.demonlab.lune.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.demonlab.lune.ACTION_NEXT"
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { resume() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { 
                    PlaybackManager.getInstance(applicationContext).playNextFromService()
                }
                override fun onSkipToPrevious() {
                    PlaybackManager.getInstance(applicationContext).playPreviousFromService()
                }
                override fun onSeekTo(pos: Long) { seekTo(pos.toInt()) }
            })
            isActive = true
        }
        settingsManager = SettingsManager.getInstance(this)
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    private fun setupAudioFx(sessionId: Int, isSecondary: Boolean = false) {
        try {
            if (isSecondary) {
                secondaryEqualizer?.release()
                secondaryBassBoost?.release()
                secondaryVirtualizer?.release()
            } else {
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()
            }
            
            val eq = Equalizer(0, sessionId).apply {
                enabled = settingsManager.isEqEnabled
                val storedBands = settingsManager.eqBandLevels.split(",").filter { it.isNotEmpty() }
                if (storedBands.size == numberOfBands.toInt()) {
                    for (i in 0 until numberOfBands) {
                        try {
                            setBandLevel(i.toShort(), storedBands[i].toShort())
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
            
            val bb = BassBoost(0, sessionId).apply {
                enabled = settingsManager.isBassBoostEnabled
                if (strengthSupported) {
                    setStrength(900.toShort())
                }
            }
            
            val virt = Virtualizer(0, sessionId).apply {
                enabled = settingsManager.isSpatialAudioEnabled
                if (strengthSupported) {
                    setStrength(1000.toShort())
                }
            }

            if (isSecondary) {
                secondaryEqualizer = eq
                secondaryBassBoost = bb
                secondaryVirtualizer = virt
            } else {
                equalizer = eq
                bassBoost = bb
                virtualizer = virt
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_PREVIOUS -> PlaybackManager.getInstance(this).playPreviousFromService()
            ACTION_NEXT -> PlaybackManager.getInstance(this).playNextFromService()
        }
        return START_NOT_STICKY
    }


    override fun onBind(intent: Intent?): IBinder = binder

    fun playSong(song: Song) {
        isCrossfading = false
        PlaybackManager.getInstance(applicationContext).isTransitioning = false
        monitorJob?.cancel()
        requestAudioFocus()

        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.setOnErrorListener(null)
        mediaPlayer?.release()
        secondaryPlayer?.setOnCompletionListener(null)
        secondaryPlayer?.setOnErrorListener(null)
        secondaryPlayer?.release()
        secondaryPlayer = null
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, song.uri)
            setOnPreparedListener {
                start()
                val sessionId = audioSessionId
                setupAudioFx(sessionId, false)
                setVolume(1f, 1f)
                updatePlaybackState()
                
                // Load metadata/notifications AFTER starting for instant audio response
                serviceScope.launch {
                    val art = fetchAlbumArt(song)
                    updateMetadata(song, art)
                    showNotification(song, true, art)
                    PlaybackManager.getInstance(applicationContext).clearLyrics()
                    extractLyrics(song)
                }
            }
            setOnErrorListener { _, _, _ -> 
                true // returning true prevents onCompletionListener from firing on broken tracks
            }
            prepareAsync()
            setOnCompletionListener {
                if (!isCrossfading) {
                    PlaybackManager.getInstance(applicationContext).playNextFromService(true)
                }
            }
        }
        
        // Start monitor regardless of metadata
        startCrossfadeMonitor()
    }

    fun crossfadeToSong(song: Song) {
        if (!isCrossfading) {
            performCrossfade(song)
        }
    }

    private var monitorJob: Job? = null
    private fun startCrossfadeMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                val playbackManager = PlaybackManager.getInstance(applicationContext)
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying && (playbackManager.isCrossfade || playbackManager.isAutomix) && !isCrossfading) {
                    val remaining = mp.duration - mp.currentPosition
                    val duration = mp.duration
                    val triggerMs = 12000L // 12s per user request (Spotify-style)
                    
                    // Only trigger if we have enough duration and are near the end
                    if (duration > triggerMs && remaining in 1..triggerMs && mp.currentPosition > (duration / 2)) {
                        val nextSong = playbackManager.getNextSong()
                        if (nextSong != null) {
                            Log.d("MusicService", "Crossfade triggered for next song: ${nextSong.title}")
                            performCrossfade(nextSong)
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun performCrossfade(nextSong: Song) {
        isCrossfading = true
        val playbackManager = PlaybackManager.getInstance(applicationContext)
        playbackManager.isTransitioning = true
        val fadeDurationMs = 12000L // 12s per user request
        
            secondaryPlayer?.setOnCompletionListener(null)
            secondaryPlayer?.setOnErrorListener(null)
            secondaryPlayer?.release()
            
            secondaryPlayer = MediaPlayer()
            
            playbackManager.clearLyrics()
            serviceScope.launch {
                extractLyrics(nextSong)
            }

            serviceScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        secondaryPlayer?.setDataSource(applicationContext, nextSong.uri)
                        secondaryPlayer?.setVolume(0f, 0f)
                        secondaryPlayer?.prepare() 
                    } catch (e: Exception) {
                        Log.e("MusicService", "Failed to prepare secondary player", e)
                    }
                }
                
                val sessionId = secondaryPlayer?.audioSessionId ?: 0
                if (sessionId != 0) {
                    setupAudioFx(sessionId, true)
                }
                
                secondaryPlayer?.start()
                secondaryPlayer?.setOnCompletionListener {
                    if (!isCrossfading) {
                        PlaybackManager.getInstance(applicationContext).playNextFromService(true)
                    }
                }

                val steps = 100 
                val interval = fadeDurationMs / steps
                val isAutomix = PlaybackManager.getInstance(applicationContext).isAutomix
                
                for (i in 1..steps) {
                    if (!isCrossfading) break
                    
                    while (!PlaybackManager.getInstance(applicationContext).isPlaying && isCrossfading) {
                        delay(500)
                    }
                    if (!isCrossfading) break
                    
                    val normalizedNext = i.toFloat() / steps
                    
                    // Crossfade curves
                    val volNext: Float
                    val volCurrent: Float
                    
                    if (isAutomix) {
                        // Constant Power Crossfade for Automix: Sum of squares = 1.0
                        // This prevents volume spikes and dips.
                        val angle = (normalizedNext * Math.PI / 2)
                        volNext = Math.sin(angle).toFloat()
                        volCurrent = Math.cos(angle).toFloat()
                    } else {
                        // Standard symmetric crossfade (Power curve)
                        volNext = normalizedNext * normalizedNext
                        val normalizedCurrent = 1f - normalizedNext
                        volCurrent = normalizedCurrent * normalizedCurrent
                    }
                    
                    mediaPlayer?.setVolume(volCurrent, volCurrent)
                    secondaryPlayer?.setVolume(volNext, volNext)
                    delay(interval)
                }
                
                if (!isCrossfading) return@launch

                val oldPlayer = mediaPlayer
                mediaPlayer = secondaryPlayer
                secondaryPlayer = null
                
                // Swap effects: release old primary, promote secondary to primary
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()
                
                equalizer = secondaryEqualizer
                bassBoost = secondaryBassBoost
                virtualizer = secondaryVirtualizer
                
                secondaryEqualizer = null
                secondaryBassBoost = null
                secondaryVirtualizer = null
                
                mediaPlayer?.setVolume(1f, 1f)

                withContext(Dispatchers.IO) {
                    oldPlayer?.setOnCompletionListener(null)
                    oldPlayer?.setOnErrorListener(null)
                    oldPlayer?.release()
                }
                
                isCrossfading = false
            playbackManager.isTransitioning = false
            PlaybackManager.getInstance(applicationContext).updateCurrentSongState(nextSong)
            
            val art = fetchAlbumArt(nextSong)
            updateMetadata(nextSong, art)
            updatePlaybackState()
            showNotification(nextSong, true, art)
            extractLyrics(nextSong) // Keep one at the end just in case or for state sync
            startCrossfadeMonitor() 
        }
    }

    private suspend fun fetchAlbumArt(song: Song): android.graphics.Bitmap? {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(song.coverUrl ?: song.albumArtUri)
            .allowHardware(false)
            .build()
        
        val result = loader.execute(request)
        return (result as? SuccessResult)?.drawable?.let {
            val bitmap = android.graphics.Bitmap.createBitmap(
                it.intrinsicWidth.coerceAtLeast(1),
                it.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bitmap
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        secondaryPlayer?.pause()
        PlaybackManager.getInstance(applicationContext).updatePlayingState(false)
        updatePlaybackState()
        stopWidgetUpdateTimer()
        updateWidget()
        serviceScope.launch {
            val song = currentSong() ?: return@launch
            val art = fetchAlbumArt(song)
            showNotification(song, false, art)
        }
    }

    fun resume() {
        mediaPlayer?.start()
        secondaryPlayer?.start()
        PlaybackManager.getInstance(applicationContext).updatePlayingState(true)
        updatePlaybackState()
        startWidgetUpdateTimer()
        updateWidget()
        serviceScope.launch {
            val song = currentSong() ?: return@launch
            val art = fetchAlbumArt(song)
            showNotification(song, true, art)
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true || secondaryPlayer?.isPlaying == true
    fun currentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun duration(): Int = mediaPlayer?.duration ?: 0
    fun getAudioSessionId(): Int = mediaPlayer?.audioSessionId ?: 0
    fun seekTo(pos: Int) { 
        mediaPlayer?.seekTo(pos)
        updatePlaybackState()
    }

    /** Seeks to position 0 without resuming playback. Called when queue ends naturally. */
    fun resetPlayerProgress() {
        try { 
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0) 
        } catch (e: Exception) { /* ignore invalid state */ }
        updatePlaybackState()
        serviceScope.launch {
            val song = currentSong() ?: return@launch
            val art = fetchAlbumArt(song)
            showNotification(song, false, art)
        }
    }

    private fun extractLyrics(song: Song) {
        Log.d("MusicService", "Extracting lyrics for: ${song.title}")
        serviceScope.launch(Dispatchers.IO) {
            val playbackManager = PlaybackManager.getInstance(applicationContext)
            
            withContext(Dispatchers.Main) {
                playbackManager.updateLyrics(null)
            }
            
            // 1. Try to find a .lrc file in the same directory
            val songFile = File(song.path)
            val lrcFile = File(songFile.parent, songFile.nameWithoutExtension + ".lrc")
            
            if (lrcFile.exists()) {
                try {
                    Log.d("MusicService", "Found .lrc file: ${lrcFile.absolutePath}")
                    val lrcContent = lrcFile.readText()
                    withContext(Dispatchers.Main) {
                        playbackManager.updateLyrics(lrcContent)
                    }
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 2. Try to extract embedded lyrics via Jaudiotagger (Best for FLAC/Vorbis)
            try {
                val f = File(song.path)
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(f)
                val tag = audioFile.tag
                if (tag != null) {
                    val embedded = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                    
                    if (!embedded.isNullOrBlank()) {
                        Log.i("MusicService", "Jaudiotagger extraction success. Length: ${embedded.length}")
                        withContext(Dispatchers.Main) {
                            playbackManager.updateLyrics(embedded)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Jaudiotagger failed: ${e.message}")
            }

            // 3. Fallback to MediaMetadataRetriever (Native)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(applicationContext, song.uri)
                var embeddedLyrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    retriever.extractMetadata(13 /* MediaMetadataRetriever.METADATA_KEY_LYRIC */)
                } else null
                
                // FLAC/Vorbis Fallback: If still null, try reading the file header for "LYRICS=" or "UNSYNCEDLYRICS="
                Log.i("MusicService", "Extracted lyrics length from retriever: ${embeddedLyrics?.length ?: 0}")
                
                if (embeddedLyrics.isNullOrBlank()) {
                    Log.i("MusicService", "No lyrics in retriever, trying manual scan on ${song.path}")
                    embeddedLyrics = tryExtractManual(song.path)
                }
                
                Log.i("MusicService", "Final extracted lyrics length: ${embeddedLyrics?.length ?: 0}")
                if (embeddedLyrics != null) {
                    Log.i("MusicService", "Final lyrics snippet: ${embeddedLyrics.take(50)}...")
                }

                withContext(Dispatchers.Main) {
                    playbackManager.updateLyrics(embeddedLyrics)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    private fun tryExtractManual(path: String): String? {
        try {
            val file = File(path)
            if (!file.exists()) return null
            
            // Read first 1MB. FLAC/MP3 metadata blocks are usually within this range.
            val bufferSize = 1024 * 1024
            val buffer = ByteArray(bufferSize.coerceAtMost(file.length().toInt()))
            file.inputStream().use { it.read(buffer) }
            
            val tags = listOf("UNSYNCEDLYRICS=", "LYRICS=", "USLT=", "unsyncedlyrics=", "lyrics=")
            for (tag in tags) {
                val tagBytes = tag.toByteArray(Charsets.UTF_8)
                val index = findBytes(buffer, tagBytes)
                if (index != -1) {
                    // Vorbis Comment Check: The 4 bytes before the tag name often store the length (Little Endian)
                    // Format: [Length (4 bytes)] [NAME=VALUE]
                    // Since 'index' points to 'NAME', the length field is at index-4
                    if (index >= 4) {
                        val len = (buffer[index - 4].toInt() and 0xFF) or
                                 ((buffer[index - 3].toInt() and 0xFF) shl 8) or
                                 ((buffer[index - 2].toInt() and 0xFF) shl 16) or
                                 ((buffer[index - 1].toInt() and 0xFF) shl 24)
                        
                        // If length is plausible (e.g. 100 bytes to 128KB), use it
                        if (len in tagBytes.size..131072) {
                            val totalLength = len - tagBytes.size
                            if (index + tagBytes.size + totalLength <= buffer.size) {
                                val result = String(buffer, index + tagBytes.size, totalLength, Charsets.UTF_8).trim()
                                Log.i("MusicService", "SUCCESS: Extracted via Vorbis length: ${result.length} chars")
                                return result
                            }
                        }
                    }

                    // Fallback to previous regex if Vorbis check fails
                    val start = index + tagBytes.size
                    val length = (65536).coerceAtMost(buffer.size - start)
                    val raw = String(buffer, start, length, Charsets.UTF_8)
                    val nextTagPattern = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]|\\r?\\n[A-Z0-9_]{3,}=")
                    val matcher = nextTagPattern.matcher(raw)
                    val end = if (matcher.find()) matcher.start() else raw.length
                    
                    val result = raw.substring(0, end).trim()
                    if (result.length > 5) return result
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun findBytes(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty()) return -1
        for (i in 0..haystack.size - needle.size) {
            var found = true
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun updateMetadata(song: Song, art: android.graphics.Bitmap? = null) {
        val metadataBuilder = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION, song.duration.toLong())
        
        art?.let {
            metadataBuilder.putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }
            
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying()) android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING 
                    else android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
        
        val stateBuilder = android.support.v4.media.session.PlaybackStateCompat.Builder()
            .setActions(
                android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, currentPosition().toLong(), 1.0f)
        
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun currentSong(): Song? {
        return PlaybackManager.getInstance(this).currentSong
    }

    private fun showNotification(song: Song, isPlaying: Boolean, art: android.graphics.Bitmap? = null) {
        val channelId = "music_playback_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Music Playback", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val intent = Intent(this, Lune::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                getServicePendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                getServicePendingIntent(ACTION_PLAY)
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(art)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setOngoing(isPlaying)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", getServicePendingIntent(ACTION_PREVIOUS))
            .addAction(playPauseAction)
            .addAction(android.R.drawable.ic_media_next, "Next", getServicePendingIntent(ACTION_NEXT))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession?.sessionToken))
            .build()

        startForeground(1, notification)
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onDestroy() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        secondaryEqualizer?.release()
        secondaryBassBoost?.release()
        secondaryVirtualizer?.release()
        mediaPlayer?.release()
        secondaryPlayer?.release()
        mediaSession?.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun setEqEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    fun setEqBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        bassBoost?.enabled = enabled
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        virtualizer?.enabled = enabled
    }

    private fun startWidgetUpdateTimer() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            while (isActive) {
                if (isPlaying()) {
                    updateWidget()
                }
                delay(1000)
            }
        }
    }

    private fun stopWidgetUpdateTimer() {
        widgetUpdateJob?.cancel()
    }

    private fun updateWidget() {
        val song = currentSong()
        val isPlaying = isPlaying()
        val progress = if (duration() > 0) currentPosition().toFloat() / duration() else 0f
        
        serviceScope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val componentName = ComponentName(applicationContext, LuneWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isEmpty()) return@launch

            val views = RemoteViews(packageName, R.layout.lune_widget_layout)
            
            if (song != null) {
                views.setTextViewText(R.id.widget_title, song.title)
                views.setTextViewText(R.id.widget_artist, song.artist)
                views.setProgressBar(R.id.widget_progress, 100, (progress * 100).toInt(), false)
                views.setImageViewResource(R.id.widget_play_pause, 
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                
                // Audio Output
                views.setImageViewResource(R.id.widget_output_icon, getOutputIconRes())
                views.setTextViewText(R.id.widget_output_text, getOutputName())

                // Fetch and round art
                val art = fetchAlbumArt(song)
                if (art != null) {
                    val roundedArt = LuneWidgetProvider.getRoundedCornerBitmap(art, 40) // 40px radius
                    views.setImageViewBitmap(R.id.widget_cover, roundedArt)
                    
                    // Handle blurred background
                    if (lastSongForBlur != song || lastBlurredBitmap == null) {
                        lastSongForBlur = song
                        lastBlurredBitmap = LuneWidgetProvider.getBlurredBitmap(this@MusicService, art, 25, 25)
                    }
                    
                    lastBlurredBitmap?.let {
                        views.setImageViewBitmap(R.id.widget_blurred_background, it)
                        views.setViewVisibility(R.id.widget_blurred_background, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.widget_dark_overlay, android.view.View.VISIBLE)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_cover, R.drawable.ic_launcher_foreground)
                    views.setViewVisibility(R.id.widget_blurred_background, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_dark_overlay, android.view.View.GONE)
                }
            } else {
                views.setTextViewText(R.id.widget_title, getString(R.string.no_song_playing))
                views.setTextViewText(R.id.widget_artist, "")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
                views.setImageViewResource(R.id.widget_cover, R.drawable.ic_launcher_foreground)
                views.setViewVisibility(R.id.widget_blurred_background, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_dark_overlay, android.view.View.GONE)
            }

            // Button Intents
            views.setOnClickPendingIntent(R.id.widget_play_pause, getWidgetServicePendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY))
            views.setOnClickPendingIntent(R.id.widget_prev, getWidgetServicePendingIntent(ACTION_PREVIOUS))
            views.setOnClickPendingIntent(R.id.widget_next, getWidgetServicePendingIntent(ACTION_NEXT))
            
            // Open App
            val intent = Intent(applicationContext, Lune::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }

    private fun getWidgetServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getOutputIconRes(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return R.drawable.ic_bluetooth
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return R.drawable.ic_headphones
                }
            }
        }
        return R.drawable.ic_speaker
    }

    private fun getOutputName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return getString(R.string.output_bluetooth)
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return getString(R.string.output_headphones)
                }
            }
        }
        return getString(R.string.output_speaker)
    }
}
