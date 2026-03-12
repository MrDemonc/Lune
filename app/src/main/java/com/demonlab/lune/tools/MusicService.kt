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
    private lateinit var settingsManager: SettingsManager
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeLoss = false

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

    private fun setupAudioFx(sessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            
            equalizer = Equalizer(0, sessionId).apply {
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
            
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = settingsManager.isBassBoostEnabled
                if (strengthSupported) {
                    setStrength(900.toShort()) // High strength for retumbo effect
                }
            }
            
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = settingsManager.isSpatialAudioEnabled
                if (strengthSupported) {
                    setStrength(1000.toShort()) // Gives deeper bass feel
                }
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
                setupAudioFx(sessionId)
                setVolume(1f, 1f)
                updatePlaybackState()
                
                // Load metadata/notifications AFTER starting for instant audio response
                serviceScope.launch {
                    val art = fetchAlbumArt(song)
                    updateMetadata(song, art)
                    showNotification(song, true, art)
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
        val fadeDurationMs = 12000L // 12s per user request
        
        secondaryPlayer?.setOnCompletionListener(null)
        secondaryPlayer?.setOnErrorListener(null)
        secondaryPlayer?.release()
        secondaryPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, nextSong.uri)
            setVolume(0f, 0f)
            prepare() // Secondary can prepare synchronously as it's not blocking the active track
            start()
            setOnCompletionListener {
                if (!isCrossfading) {
                    playbackManager.playNextFromService(true)
                }
            }
        }

        serviceScope.launch {
            val steps = 100 // More steps for longer 12s fade
            val interval = fadeDurationMs / steps
            val isAutomix = PlaybackManager.getInstance(applicationContext).isAutomix
            
            for (i in 1..steps) {
                if (!isCrossfading) break
                
                // If paused, wait here without completing the crossfade
                while (!PlaybackManager.getInstance(applicationContext).isPlaying && isCrossfading) {
                    delay(500)
                }
                if (!isCrossfading) break
                
                val normalizedNext = i.toFloat() / steps
                
                // Next song fades in normally
                val volNext = normalizedNext * normalizedNext
                
                // Current song volume logic
                val volCurrent = if (isAutomix) {
                    // Spotify-style: Keep current song at 100% for first 66 steps (approx 8s), then fade
                    if (i < 66) {
                        1.0f
                    } else {
                        val fadeProgress = (i - 66).toFloat() / (steps - 66)
                        (1f - fadeProgress) * (1f - fadeProgress)
                    }
                } else {
                    // Standard symmetric crossfade
                    val normalizedCurrent = 1f - normalizedNext
                    normalizedCurrent * normalizedCurrent
                }
                
                mediaPlayer?.setVolume(volCurrent, volCurrent)
                secondaryPlayer?.setVolume(volNext, volNext)
                delay(interval)
            }
            
            if (!isCrossfading) return@launch

            val oldPlayer = mediaPlayer
            mediaPlayer = secondaryPlayer
            secondaryPlayer = null
            
            // Re-apply AudioFx to the new primary player
            mediaPlayer?.audioSessionId?.let { setupAudioFx(it) }
            
            mediaPlayer?.setVolume(1f, 1f)

            withContext(Dispatchers.IO) {
                oldPlayer?.setOnCompletionListener(null)
                oldPlayer?.setOnErrorListener(null)
                oldPlayer?.release()
            }
            
            isCrossfading = false
            playbackManager.updateCurrentSongState(nextSong)
            
            val art = fetchAlbumArt(nextSong)
            updateMetadata(nextSong, art)
            updatePlaybackState()
            showNotification(nextSong, true, art)
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
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
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
}
