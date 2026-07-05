package com.demonlab.lune.tools

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.net.Uri
import android.content.ContentUris
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import com.demonlab.lune.data.MusicDatabase
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.demonlab.lune.R
import com.demonlab.lune.audio.BalanceEffect
import com.demonlab.lune.audio.DynamicsEffect
import com.demonlab.lune.audio.LoudnessEffect
import com.demonlab.lune.audio.ReverbEffect
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

class MusicService : MediaBrowserServiceCompat() {
    private var mediaPlayer: MediaPlayer? = null
    private var secondaryPlayer: MediaPlayer? = null
    private var isCrossfading = false
    private var mediaSession: MediaSessionCompat? = null
    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    internal var equalizer: Equalizer? = null
    internal var bassBoost: BassBoost? = null
    internal var virtualizer: Virtualizer? = null

    internal var loudnessEffect: LoudnessEffect? = null
    internal var reverbEffect: ReverbEffect? = null
    internal var dynamicsEffect: DynamicsEffect? = null

    private var secondaryEqualizer: Equalizer? = null
    private var secondaryBassBoost: BassBoost? = null
    private var secondaryVirtualizer: Virtualizer? = null

    private lateinit var settingsManager: SettingsManager
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeLoss = false
    private var widgetUpdateJob: Job? = null
    private var spatialRampJob: Job? = null
    private var currentSpatialStrength: Short = 0
    private var lastSongForBlur: Song? = null
    private var lastBlurredBitmap: Bitmap? = null
    private var lastSongForRounded: Song? = null
    private var cachedRoundedArt: Bitmap? = null
    private var becomingNoisyReceiver: BroadcastReceiver? = null

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
        const val ACTION_SHUFFLE = "com.demonlab.lune.ACTION_SHUFFLE"
        const val ACTION_FAVORITE = "com.demonlab.lune.ACTION_FAVORITE"
        const val PAUSE_TIMEOUT_MS = 5 * 60 * 1000L
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private var pauseTimeoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        becomingNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && isPlaying()) {
                    pause()
                }
            }
        }
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

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
                override fun onCustomAction(action: String?, extras: Bundle?) {
                    val pm = PlaybackManager.getInstance(applicationContext)
                    when (action) {
                        "shuffle" -> {
                            pm.toggleShuffle()
                            pm.currentSong?.let { showNotification(it, isPlaying()) }
                            updatePlaybackState()
                        }
                        "favorite" -> {
                            pm.toggleFavorite(onFavoriteToggled = { updatedSong ->
                                val provider = MusicProvider(applicationContext)
                                provider.updateSongInCache(updatedSong)
                                showNotification(updatedSong, isPlaying())
                                updatePlaybackState()
                            })
                        }
                    }
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    if (mediaId == null) return
                    serviceScope.launch {
                        val playbackManager = PlaybackManager.getInstance(applicationContext)
                        val provider = MusicProvider(applicationContext)
                        val db = MusicDatabase.getDatabase(applicationContext)

                        val hiddenFolders = settingsManager.hiddenFolders
                        when {
                            mediaId.startsWith("song_allsongs_") -> {
                                val songId = mediaId.substringAfter("song_allsongs_").toLongOrNull() ?: return@launch
                                val songs = provider.getCachedSongs().filter { !hiddenFolders.contains(it.folderName) }
                                val targetSong = songs.find { it.id == songId } ?: return@launch
                                playbackManager.play(targetSong, songs, -100L, category = "ALL", shuffleMode = playbackManager.isShuffle)
                            }
                            mediaId.startsWith("song_favs_") -> {
                                val songId = mediaId.substringAfter("song_favs_").toLongOrNull() ?: return@launch
                                val songs = provider.getCachedSongs().filter { it.isFavorite && !hiddenFolders.contains(it.folderName) }
                                val targetSong = songs.find { it.id == songId } ?: return@launch
                                playbackManager.play(targetSong, songs, -200L, category = "FAVORITES")
                            }
                            mediaId.startsWith("song_playlist_") -> {
                                val parts = mediaId.removePrefix("song_playlist_").split("_")
                                if (parts.size < 2) return@launch
                                val playlistId = parts[0].toLongOrNull() ?: return@launch
                                val songId = parts[1].toLongOrNull() ?: return@launch

                                val songIds = db.playlistDao().getSongIdsForPlaylist(playlistId)
                                val allCached = provider.getCachedSongs()
                                val playlistSongs = songIds.mapNotNull { id -> allCached.find { it.id == id } }
                                val targetSong = playlistSongs.find { it.id == songId } ?: return@launch
                                playbackManager.play(targetSong, playlistSongs, playlistId, category = "PLAYLISTS")
                            }
                        }
                    }
                }
            })
            isActive = true
        }
        sessionToken = mediaSession?.sessionToken
        settingsManager = SettingsManager.getInstance(this)

        serviceScope.launch {
            PlaybackManager.getInstance(this@MusicService).refreshNotification.collect {
                val pm = PlaybackManager.getInstance(this@MusicService)
                val song = pm.currentSong
                if (song != null) {
                    showNotification(song, isPlaying())
                }
                updatePlaybackState()
            }
        }
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
        if (isSecondary) {
            secondaryEqualizer?.release()
            secondaryBassBoost?.release()
            secondaryVirtualizer?.release()
            loudnessEffect?.release(true)
            reverbEffect?.release(true)
            dynamicsEffect?.release(true)
        } else {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEffect?.release(false)
            reverbEffect?.release(false)
            dynamicsEffect?.release(false)
        }

        try {
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
            if (isSecondary) secondaryEqualizer = eq else equalizer = eq
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val bb = BassBoost(0, sessionId).apply {
                enabled = settingsManager.isBassBoostEnabled
                if (enabled && strengthSupported) setStrength(settingsManager.bassBoostLevel.toShort())
            }
            if (isSecondary) secondaryBassBoost = bb else bassBoost = bb
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val virt = Virtualizer(0, sessionId).apply {
                enabled = settingsManager.isSpatialAudioEnabled
                if (strengthSupported) {
                    setStrength(800.toShort())
                    currentSpatialStrength = 800.toShort()
                }
            }
            if (isSecondary) secondaryVirtualizer = virt else virtualizer = virt
        } catch (e: Exception) { e.printStackTrace() }

        try {
            if (isSecondary) {
                loudnessEffect?.setup(sessionId, true, settingsManager.isLoudnessEnabled, settingsManager.loudnessGain)
                reverbEffect?.setup(sessionId, true, settingsManager.reverbPreset)
                dynamicsEffect?.setup(sessionId, true, settingsManager.dynamicsPreset)
            } else {
                val loud = LoudnessEffect().apply {
                    setup(sessionId, false, settingsManager.isLoudnessEnabled, settingsManager.loudnessGain)
                }
                loudnessEffect = loud
                val rev = ReverbEffect().apply {
                    setup(sessionId, false, settingsManager.reverbPreset)
                }
                reverbEffect = rev
                val dyn = DynamicsEffect().apply {
                    setup(sessionId, false, settingsManager.dynamicsPreset)
                }
                dynamicsEffect = dyn
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val pm = PlaybackManager.getInstance(this)

        when (action) {
            ACTION_PLAY -> resume()
            ACTION_PAUSE -> pause()
            ACTION_PREVIOUS -> pm.playPreviousFromService()
            ACTION_NEXT -> pm.playNextFromService()
            ACTION_SHUFFLE -> {
                pm.toggleShuffle()
                currentSong()?.let { showNotification(it, isPlaying()) }
                updatePlaybackState()
            }
            ACTION_FAVORITE -> {
                pm.toggleFavorite(onFavoriteToggled = { updatedSong ->
                    val provider = MusicProvider(this)
                    provider.updateSongInCache(updatedSong)
                    showNotification(updatedSong, isPlaying())
                })
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val notification = NotificationCompat.Builder(this, "music_playback_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.no_song_playing))
            .setSilent(true)
            .build()
        startForeground(1, notification)
        super.onTaskRemoved(rootIntent)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return if (intent?.action == "android.media.browse.MediaBrowserService") {
            super.onBind(intent)
        } else {
            binder
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: androidx.media.MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        serviceScope.launch {
            val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
            val provider = MusicProvider(applicationContext)
            val db = MusicDatabase.getDatabase(applicationContext)

            try {
                when (parentId) {
                    "root" -> {
                        val songsItem = MediaDescriptionCompat.Builder()
                            .setMediaId("all_songs")
                            .setTitle(getString(R.string.tab_songs))
                            .build()
                        mediaItems.add(MediaBrowserCompat.MediaItem(songsItem, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))

                        val favoritesItem = MediaDescriptionCompat.Builder()
                            .setMediaId("favorites")
                            .setTitle(getString(R.string.tab_favorites))
                            .build()
                        mediaItems.add(MediaBrowserCompat.MediaItem(favoritesItem, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))

                        val playlistsItem = MediaDescriptionCompat.Builder()
                            .setMediaId("playlists")
                            .setTitle(getString(R.string.playlists))
                            .build()
                        mediaItems.add(MediaBrowserCompat.MediaItem(playlistsItem, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
                    }
                    "all_songs" -> {
                        val hidden = settingsManager.hiddenFolders
                        val songs = provider.getCachedSongs().filter { !hidden.contains(it.folderName) }
                        for (song in songs) {
                            val desc = MediaDescriptionCompat.Builder()
                                .setMediaId("song_allsongs_${song.id}")
                                .setTitle(song.title)
                                .setSubtitle(song.artist)
                                .setIconUri(
                                    if (song.coverUrl != null) {
                                        Uri.parse(song.coverUrl)
                                    } else {
                                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
                                    }
                                )
                                .build()
                            mediaItems.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
                        }
                    }
                    "favorites" -> {
                        val hidden = settingsManager.hiddenFolders
                        val songs = provider.getCachedSongs().filter { it.isFavorite && !hidden.contains(it.folderName) }
                        for (song in songs) {
                            val desc = MediaDescriptionCompat.Builder()
                                .setMediaId("song_favs_${song.id}")
                                .setTitle(song.title)
                                .setSubtitle(song.artist)
                                .setIconUri(
                                    if (song.coverUrl != null) {
                                        Uri.parse(song.coverUrl)
                                    } else {
                                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
                                    }
                                )
                                .build()
                            mediaItems.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
                        }
                    }
                    "playlists" -> {
                        val playlists = db.playlistDao().getAllPlaylists()
                        for (playlist in playlists) {
                            val desc = MediaDescriptionCompat.Builder()
                                .setMediaId("playlist_${playlist.id}")
                                .setTitle(playlist.name)
                                .build()
                            mediaItems.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))
                        }
                    }
                    else -> {
                        if (parentId.startsWith("playlist_")) {
                            val playlistId = parentId.removePrefix("playlist_").toLongOrNull()
                            if (playlistId != null) {
                                val songIds = db.playlistDao().getSongIdsForPlaylist(playlistId)
                                val allCached = provider.getCachedSongs()
                                val playlistSongs = songIds.mapNotNull { id -> allCached.find { it.id == id } }
                                for (song in playlistSongs) {
                                    val desc = MediaDescriptionCompat.Builder()
                                        .setMediaId("song_playlist_${playlistId}_${song.id}")
                                        .setTitle(song.title)
                                        .setSubtitle(song.artist)
                                        .setIconUri(
                                            if (song.coverUrl != null) {
                                                Uri.parse(song.coverUrl)
                                            } else {
                                                ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
                                            }
                                        )
                                        .build()
                                    mediaItems.add(MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            result.sendResult(mediaItems)
        }
    }

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
                applyBalance(PlaybackManager.getInstance(applicationContext).balance)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val pm = PlaybackManager.getInstance(applicationContext)
                        val speed = pm.playbackSpeed
                        val pitch = pm.playbackPitch
                        if (speed != 1.0f || pitch != 1.0f) {
                            val params = playbackParams
                            params.speed = speed
                            params.pitch = pitch
                            playbackParams = params
                        }
                    } catch (e: Exception) {}
                }
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
            val mp = mediaPlayer
            val fadeMs = if (settingsManager.isCrossfadeCustomDuration)
                settingsManager.crossfadeDurationSeconds * 1000L else 12000L
            val remaining = if (mp != null) (mp.duration - mp.currentPosition).toLong() else fadeMs
            performCrossfade(song, if (remaining in 1..fadeMs) remaining else fadeMs)
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
                    val maxTriggerMs = if (settingsManager.isCrossfadeCustomDuration)
                        settingsManager.crossfadeDurationSeconds * 1000L else 12000L

                    // Only fire if we have enough time left and are nearing the end
                    if (duration > maxTriggerMs && remaining in 1..maxTriggerMs && mp.currentPosition > (duration / 2)) {
                        val nextSong = playbackManager.getNextSong()
                        if (nextSong != null) {
                            Log.d("MusicService", "Crossfade triggered with duration: $remaining ms")
                            // We pass the remaining real time as the transition duration
                            performCrossfade(nextSong, remaining.toLong())
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun performCrossfade(nextSong: Song, fadeDurationMs: Long) {
        isCrossfading = true
        val playbackManager = PlaybackManager.getInstance(applicationContext)
        playbackManager.isTransitioning = true

        secondaryPlayer?.setOnCompletionListener(null)
        secondaryPlayer?.setOnErrorListener(null)
        secondaryPlayer?.release()

        secondaryPlayer = MediaPlayer()

        playbackManager.clearLyrics()
        serviceScope.launch {
            extractLyrics(nextSong)
        }

        serviceScope.launch {
            try {
                var prepared = false
                withContext(Dispatchers.IO) {
                    try {
                        secondaryPlayer?.setDataSource(applicationContext, nextSong.uri)
                        secondaryPlayer?.setVolume(0f, 0f)
                        secondaryPlayer?.prepare()
                        prepared = true
                    } catch (e: Exception) {
                        Log.e("MusicService", "Failed to prepare secondary player", e)
                    }
                }

                if (!prepared) {
                    isCrossfading = false
                    playbackManager.isTransitioning = false
                    playSong(nextSong)
                    return@launch
                }

                val sessionId = secondaryPlayer?.audioSessionId ?: 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val pm = PlaybackManager.getInstance(applicationContext)
                        val speed = pm.playbackSpeed
                        val pitch = pm.playbackPitch
                        if (speed != 1.0f || pitch != 1.0f) {
                            secondaryPlayer?.let {
                                val params = it.playbackParams
                                params.speed = speed
                                params.pitch = pitch
                                it.playbackParams = params
                            }
                        }
                    } catch (e: Exception) {}
                }

                secondaryPlayer?.start()
                
                // We ensure proper behavior in the event of premature termination
                secondaryPlayer?.setOnCompletionListener {
                    if (!isCrossfading) {
                        PlaybackManager.getInstance(applicationContext).playNextFromService(true)
                    }
                }

                val targetInterval = 50L
                val steps = (fadeDurationMs / targetInterval).toInt().coerceIn(10, 100)
                val interval = fadeDurationMs / steps

                for (i in 1..steps) {
                    if (!isCrossfading) break

                    while (!PlaybackManager.getInstance(applicationContext).isPlaying && isCrossfading) {
                        delay(100)
                    }
                    if (!isCrossfading) break

                    val progress = i.toFloat() / steps

                    // Smoother fade-in for the incoming track to compensate for RAW audio loudness
                    // and apply a headroom factor if EQ is enabled to prevent jarring volume jumps
                    // when the EQ is finally applied at the end of the transition.
                    val targetEqFactor = if (settingsManager.isEqEnabled || settingsManager.isBassBoostEnabled) 0.6f else 1.0f
                    val volCurrent = (1 - progress) * (1 - progress)
                    val volNext = (progress * progress) * targetEqFactor

                    mediaPlayer?.setVolume(volCurrent, volCurrent)
                    secondaryPlayer?.setVolume(volNext, volNext)
                    delay(interval)
                }

                if (!isCrossfading) return@launch

                val oldPlayer = mediaPlayer
                mediaPlayer = secondaryPlayer
                secondaryPlayer = null

                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()
                loudnessEffect?.release(false)
                reverbEffect?.release(false)
                dynamicsEffect?.release(false)

                val newSessionId = mediaPlayer?.audioSessionId ?: 0
                if (newSessionId != 0) {
                    setupAudioFx(newSessionId, false)
                }

                secondaryEqualizer = null
                secondaryBassBoost = null
                secondaryVirtualizer = null

                mediaPlayer?.setVolume(1f, 1f)
                applyBalance(PlaybackManager.getInstance(applicationContext).balance)
                
                // Reconfigure the listener for the promoted player
                mediaPlayer?.setOnCompletionListener {
                    if (!isCrossfading) {
                        PlaybackManager.getInstance(applicationContext).playNextFromService(true)
                    }
                }

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
            } catch (e: Exception) {
                Log.e("MusicService", "Crossfade failed: ${nextSong.title}", e)
                secondaryPlayer?.setOnCompletionListener(null)
                secondaryPlayer?.setOnErrorListener(null)
                secondaryPlayer?.release()
                secondaryPlayer = null
                playSong(nextSong)
            } finally {
                isCrossfading = false
                playbackManager.isTransitioning = false
                startCrossfadeMonitor()
            }
        }
    }

    private suspend fun fetchAlbumArt(song: Song): android.graphics.Bitmap? {
        val loader = this.imageLoader
        val request = ImageRequest.Builder(this)
            .data(song.coverUrl ?: song.uri)
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
        pauseTimeoutJob?.cancel()
        pauseTimeoutJob = serviceScope.launch {
            delay(PAUSE_TIMEOUT_MS)
            PlaybackManager.getInstance(applicationContext).savePlaybackState(wasPlaying = false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            mediaSession?.release()
            stopSelf()
        }

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
        pauseTimeoutJob?.cancel()
        requestAudioFocus()
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

    fun setPlaybackParams(speed: Float, pitch: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val wasPlaying = isPlaying()
                mediaPlayer?.let {
                    val params = it.playbackParams
                    params.speed = speed
                    params.pitch = pitch
                    it.playbackParams = params
                    if (!wasPlaying) it.pause()
                }
                secondaryPlayer?.let {
                    val params = it.playbackParams
                    params.speed = speed
                    params.pitch = pitch
                    it.playbackParams = params
                    if (!wasPlaying) it.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    fun restorePlayback(song: Song, positionMs: Long, andPlay: Boolean) {
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
                seekTo(positionMs.toInt())
                start()
                if (!andPlay) pause()
                val sessionId = audioSessionId
                setupAudioFx(sessionId, false)
                setVolume(1f, 1f)
                applyBalance(PlaybackManager.getInstance(applicationContext).balance)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val pm = PlaybackManager.getInstance(applicationContext)
                        val speed = pm.playbackSpeed
                        val pitch = pm.playbackPitch
                        if (speed != 1.0f || pitch != 1.0f) {
                            val params = playbackParams
                            params.speed = speed
                            params.pitch = pitch
                            playbackParams = params
                        }
                    } catch (e: Exception) {}
                }
                updatePlaybackState()
                serviceScope.launch {
                    val art = fetchAlbumArt(song)
                    updateMetadata(song, art)
                    showNotification(song, andPlay, art)
                    PlaybackManager.getInstance(applicationContext).clearLyrics()
                    extractLyrics(song)
                }
            }
            setOnErrorListener { _, _, _ -> true }
            prepareAsync()
            setOnCompletionListener {
                if (!isCrossfading) {
                    PlaybackManager.getInstance(applicationContext).playNextFromService(true)
                }
            }
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
            .putLong("is_favorite", if (song.isFavorite) 1L else 0L)

        art?.let {
            metadataBuilder.putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying()) android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
                    else android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED

        val pm = PlaybackManager.getInstance(this)
        val currentSong = pm.currentSong
        val extras = Bundle().apply {
            putBoolean("shuffle_mode", pm.isShuffle)
            putInt("repeat_mode", pm.repeatMode)
        }

        val stateBuilder = android.support.v4.media.session.PlaybackStateCompat.Builder()
            .setActions(
                android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO or
                android.support.v4.media.session.PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
            )
            .setState(state, currentPosition().toLong(), 1.0f)
            .setExtras(extras)

        val shuffleAction = android.support.v4.media.session.PlaybackStateCompat.CustomAction.Builder(
            "shuffle", "Shuffle",
            if (pm.isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        ).build()
        stateBuilder.addCustomAction(shuffleAction)

        val favIcon = if (currentSong?.isFavorite == true) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        val favoriteAction = android.support.v4.media.session.PlaybackStateCompat.CustomAction.Builder(
            "favorite", "Favorite", favIcon
        ).build()
        stateBuilder.addCustomAction(favoriteAction)

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

        val pm = PlaybackManager.getInstance(this)
        val isShuffleOn = pm.isShuffle
        val isFav = song.isFavorite

        val shuffleAction = NotificationCompat.Action(
            if (isShuffleOn) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle,
            "Shuffle",
            getServicePendingIntent(ACTION_SHUFFLE)
        )

        val favoriteAction = NotificationCompat.Action(
            if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            "Favorite",
            getServicePendingIntent(ACTION_FAVORITE)
        )

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
            .addAction(shuffleAction)
            .addAction(favoriteAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2, 3, 4)
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
        becomingNoisyReceiver?.let { unregisterReceiver(it) }
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEffect?.releaseAll()
        reverbEffect?.releaseAll()
        dynamicsEffect?.releaseAll()
        secondaryEqualizer?.release()
        secondaryBassBoost?.release()
        secondaryVirtualizer?.release()
        mediaPlayer?.release()
        secondaryPlayer?.release()
        mediaSession?.release()
        spatialRampJob?.cancel()
        pauseTimeoutJob?.cancel()
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
        if (enabled && bassBoost?.strengthSupported == true) {
            bassBoost?.setStrength(settingsManager.bassBoostLevel.toShort())
        }
    }

    fun setBassBoostStrength(strength: Short) {
        if (bassBoost?.strengthSupported == true) {
            bassBoost?.setStrength(strength)
        }
    }

    fun setReverbPreset(preset: Int) {
        reverbEffect?.setPreset(preset)
    }

    fun setDynamicsPreset(preset: Int) {
        dynamicsEffect?.setPreset(preset)
    }

    fun applyBalance(balance: Float) {
        val (left, right) = BalanceEffect.volumesForBalance(balance)
        mediaPlayer?.setVolume(left, right)
        secondaryPlayer?.setVolume(left, right)
    }

    fun setLoudnessEnabled(enabled: Boolean) {
        loudnessEffect?.setEnabled(enabled)
    }

    fun setLoudnessGain(gain: Int) {
        loudnessEffect?.setTargetGain(gain)
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        spatialRampJob?.cancel()
        spatialRampJob = serviceScope.launch {
            val target = if (enabled) 800.toShort() else 0.toShort()
            val start = currentSpatialStrength
            val steps = 15

            if (enabled) {
                virtualizer?.enabled = true
                secondaryVirtualizer?.enabled = true
            }

            for (i in 0..steps) {
                val progress = i.toFloat() / steps
                val eased = if (enabled) 1f - (1f - progress) * (1f - progress)
                            else progress * progress
                val value = (start + (target - start) * eased)
                    .toInt().coerceIn(0, 1000).toShort()
                if (virtualizer?.strengthSupported == true) {
                    virtualizer?.setStrength(value)
                }
                if (secondaryVirtualizer?.strengthSupported == true) {
                    secondaryVirtualizer?.setStrength(value)
                }
                delay(10)
            }

            currentSpatialStrength = target

            if (!enabled) {
                virtualizer?.enabled = false
                secondaryVirtualizer?.enabled = false
            }
        }
    }

    private fun startWidgetUpdateTimer() {
        widgetUpdateJob?.cancel()
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        widgetUpdateJob = serviceScope.launch {
            while (isActive) {
                if (isPlaying() && powerManager.isInteractive) {
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

                // Fetch and process art only if song changed or cache is missing
                if (lastSongForRounded != song || cachedRoundedArt == null) {
                    val art = fetchAlbumArt(song)
                    if (art != null) {
                        lastSongForRounded = song
                        
                        // Performance fix: We process graphics resources outside the Main Thread
                        withContext(Dispatchers.IO) {
                            cachedRoundedArt = LuneWidgetProvider.getRoundedCornerBitmap(art, 40)

                            // Also update blur cache if song changed
                            if (lastSongForBlur != song) {
                                lastSongForBlur = song
                                lastBlurredBitmap = LuneWidgetProvider.getBlurredBitmap(this@MusicService, art, 25, 25)
                            }
                        }
                    } else {
                        lastSongForRounded = null
                        cachedRoundedArt = null
                        lastSongForBlur = null
                        lastBlurredBitmap = null
                    }
                }

                if (cachedRoundedArt != null) {
                    views.setImageViewBitmap(R.id.widget_cover, cachedRoundedArt)

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