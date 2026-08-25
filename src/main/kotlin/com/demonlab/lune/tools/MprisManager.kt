package com.demonlab.lune.tools

import com.demonlab.lune.data.Song
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import java.io.File

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2Root : DBusInterface {
    fun Raise()
    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MediaPlayer2Player : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offsetMicroseconds: Long)
    fun SetPosition(trackId: DBusPath, positionMicroseconds: Long)
    fun OpenUri(uri: String)
}

class MprisManager private constructor() {
    private var connection: DBusConnection? = null
    private var isInitialized = false

    fun start() {
        if (isInitialized) return
        Thread {
            try {
                val conn = DBusConnectionBuilder.forSessionBus().build()
                conn.requestBusName("org.mpris.MediaPlayer2.lune")
                
                val service = MprisService()
                conn.exportObject("/org/mpris/MediaPlayer2", service)
                
                connection = conn
                isInitialized = true
                println("MPRIS2 Service started successfully on org.mpris.MediaPlayer2.lune")
            } catch (e: Exception) {
                println("Could not start MPRIS2 service: ${e.message}")
            }
        }.start()
    }

    fun notifyStateChanged() {
        val conn = connection ?: return
        try {
            val pm = PlaybackManager.instance
            val currentSong = pm.currentSong
            val status = if (pm.isPlaying) "Playing" else if (currentSong != null) "Paused" else "Stopped"
            
            val changedProps = mutableMapOf<String, Variant<*>>()
            changedProps["PlaybackStatus"] = Variant(status)
            changedProps["Position"] = Variant(pm.currentPositionMs * 1000L)
            changedProps["Volume"] = Variant(SettingsManager.instance.volume.toDouble().coerceIn(0.0, 1.0))
            changedProps["Shuffle"] = Variant(SettingsManager.instance.isShuffle)
            
            val loopStatus = when (SettingsManager.instance.repeatMode) {
                1 -> "Track"
                2 -> "Playlist"
                else -> "None"
            }
            changedProps["LoopStatus"] = Variant(loopStatus)

            val signal = Properties.PropertiesChanged(
                "/org/mpris/MediaPlayer2",
                "org.mpris.MediaPlayer2.Player",
                changedProps,
                emptyList()
            )
            conn.sendMessage(signal)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun notifyMetadataChanged(song: Song?) {
        val conn = connection ?: return
        try {
            val metadata = mutableMapOf<String, Variant<*>>()
            if (song != null) {
                metadata["mpris:trackid"] = Variant(DBusPath("/com/demonlab/lune/track/${song.id}"))
                metadata["mpris:length"] = Variant(song.duration * 1000L) // microseconds
                metadata["xesam:title"] = Variant(song.title)
                metadata["xesam:artist"] = Variant(arrayOf(song.artist), "as")
                metadata["xesam:album"] = Variant(song.album)
                metadata["xesam:url"] = Variant("file://${song.path}")
                
                if (song.coverPath != null && File(song.coverPath).exists()) {
                    val coverUri = "file://${File(song.coverPath).absolutePath}"
                    metadata["mpris:artUrl"] = Variant(coverUri)
                    metadata["xesam:artUrl"] = Variant(coverUri)
                } else {
                    getAppIconUrl()?.let { appIcon ->
                        metadata["mpris:artUrl"] = Variant(appIcon)
                        metadata["xesam:artUrl"] = Variant(appIcon)
                    }
                }
            } else {
                metadata["mpris:trackid"] = Variant(DBusPath("/org/mpris/MediaPlayer2/TrackList/NoTrack"))
                getAppIconUrl()?.let { appIcon ->
                    metadata["mpris:artUrl"] = Variant(appIcon)
                    metadata["xesam:artUrl"] = Variant(appIcon)
                }
            }

            val changedProps = mapOf(
                "Metadata" to Variant(metadata, "a{sv}"),
                "PlaybackStatus" to Variant(if (PlaybackManager.instance.isPlaying) "Playing" else "Paused")
            )

            val signal = Properties.PropertiesChanged(
                "/org/mpris/MediaPlayer2",
                "org.mpris.MediaPlayer2.Player",
                changedProps,
                emptyList()
            )
            conn.sendMessage(signal)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun stop() {
        try {
            connection?.disconnect()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        val instance: MprisManager by lazy { MprisManager() }
    }
}

class MprisService : MediaPlayer2Root, MediaPlayer2Player, Properties {
    override fun isRemote(): Boolean = false
    override fun getObjectPath(): String = "/org/mpris/MediaPlayer2"

    override fun Raise() {
        // Bring window to front
    }

    override fun Quit() {
        PlaybackManager.instance.setSleepTimer(0)
        SettingsManager.instance.saveSettings()
        MprisManager.instance.stop()
        Runtime.getRuntime().halt(0)
    }

    override fun Next() {
        PlaybackManager.instance.playNext()
    }

    override fun Previous() {
        PlaybackManager.instance.playPrevious()
    }

    override fun Pause() {
        if (PlaybackManager.instance.isPlaying) {
            PlaybackManager.instance.togglePlayPause()
        }
    }

    override fun PlayPause() {
        PlaybackManager.instance.togglePlayPause()
    }

    override fun Stop() {
        if (PlaybackManager.instance.isPlaying) {
            PlaybackManager.instance.togglePlayPause()
        }
    }

    override fun Play() {
        if (!PlaybackManager.instance.isPlaying) {
            PlaybackManager.instance.togglePlayPause()
        }
    }

    override fun Seek(offsetMicroseconds: Long) {
        val offsetMs = offsetMicroseconds / 1000L
        val currentPos = PlaybackManager.instance.currentPositionMs
        PlaybackManager.instance.seekTo((currentPos + offsetMs).coerceAtLeast(0L))
    }

    override fun SetPosition(trackId: DBusPath, positionMicroseconds: Long) {
        val posMs = positionMicroseconds / 1000L
        PlaybackManager.instance.seekTo(posMs.coerceAtLeast(0L))
    }

    override fun OpenUri(uri: String) {
        // Can open uri
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(interfaceName: String, propertyName: String): A {
        return (getAllProperties(interfaceName)[propertyName] ?: Variant("")) as A
    }

    override fun <A : Any?> Set(interfaceName: String, propertyName: String, value: A) {
        if (interfaceName == "org.mpris.MediaPlayer2.Player") {
            if (propertyName == "Volume") {
                val vol = when (value) {
                    is Double -> value.toFloat()
                    is Float -> value
                    is Number -> value.toFloat()
                    else -> 1f
                }
                PlaybackManager.instance.setVolume(vol)
            } else if (propertyName == "Shuffle") {
                if (value is Boolean && value != SettingsManager.instance.isShuffle) {
                    PlaybackManager.instance.toggleShuffle()
                }
            } else if (propertyName == "LoopStatus") {
                when (value?.toString()) {
                    "None" -> { SettingsManager.instance.repeatMode = 0; SettingsManager.instance.saveSettings() }
                    "Track" -> { SettingsManager.instance.repeatMode = 1; SettingsManager.instance.saveSettings() }
                    "Playlist" -> { SettingsManager.instance.repeatMode = 2; SettingsManager.instance.saveSettings() }
                }
            }
        }
    }

    override fun GetAll(interfaceName: String): Map<String, Variant<*>> {
        return getAllProperties(interfaceName)
    }

    private fun getAllProperties(interfaceName: String): Map<String, Variant<*>> {
        val pm = PlaybackManager.instance
        val settings = SettingsManager.instance
        return when (interfaceName) {
            "org.mpris.MediaPlayer2" -> mapOf(
                "CanQuit" to Variant(true),
                "CanRaise" to Variant(false),
                "HasTrackList" to Variant(false),
                "Identity" to Variant("Lune"),
                "DesktopEntry" to Variant("lune"),
                "SupportedUriSchemes" to Variant(arrayOf("file"), "as"),
                "SupportedMimeTypes" to Variant(arrayOf("audio/mpeg", "audio/flac", "audio/ogg", "audio/wav", "audio/x-flac", "audio/mp4"), "as")
            )
            "org.mpris.MediaPlayer2.Player" -> {
                val currentSong = pm.currentSong
                val metadata = mutableMapOf<String, Variant<*>>()
                
                if (currentSong != null) {
                    metadata["mpris:trackid"] = Variant(DBusPath("/com/demonlab/lune/track/${currentSong.id}"))
                    metadata["mpris:length"] = Variant(currentSong.duration * 1000L) // microseconds
                    metadata["xesam:title"] = Variant(currentSong.title)
                    metadata["xesam:artist"] = Variant(arrayOf(currentSong.artist), "as")
                    metadata["xesam:album"] = Variant(currentSong.album)
                    metadata["xesam:url"] = Variant("file://${currentSong.path}")
                    
                    if (currentSong.coverPath != null && File(currentSong.coverPath).exists()) {
                        val coverUri = "file://${File(currentSong.coverPath).absolutePath}"
                        metadata["mpris:artUrl"] = Variant(coverUri)
                        metadata["xesam:artUrl"] = Variant(coverUri)
                    } else {
                        getAppIconUrl()?.let { appIcon ->
                            metadata["mpris:artUrl"] = Variant(appIcon)
                            metadata["xesam:artUrl"] = Variant(appIcon)
                        }
                    }
                } else {
                    metadata["mpris:trackid"] = Variant(DBusPath("/org/mpris/MediaPlayer2/TrackList/NoTrack"))
                    getAppIconUrl()?.let { appIcon ->
                        metadata["mpris:artUrl"] = Variant(appIcon)
                        metadata["xesam:artUrl"] = Variant(appIcon)
                    }
                }

                val status = if (pm.isPlaying) "Playing" else if (currentSong != null) "Paused" else "Stopped"
                val loopStatus = when (settings.repeatMode) {
                    1 -> "Track"
                    2 -> "Playlist"
                    else -> "None"
                }

                mapOf(
                    "PlaybackStatus" to Variant(status),
                    "LoopStatus" to Variant(loopStatus),
                    "Rate" to Variant(1.0),
                    "Shuffle" to Variant(settings.isShuffle),
                    "Metadata" to Variant(metadata, "a{sv}"),
                    "Volume" to Variant(settings.volume.toDouble().coerceIn(0.0, 1.0)),
                    "Position" to Variant(pm.currentPositionMs * 1000L), // microseconds
                    "MinimumRate" to Variant(1.0),
                    "MaximumRate" to Variant(1.0),
                    "CanGoNext" to Variant(pm.activeQueue.isNotEmpty()),
                    "CanGoPrevious" to Variant(pm.activeQueue.isNotEmpty()),
                    "CanPlay" to Variant(currentSong != null || pm.activeQueue.isNotEmpty()),
                    "CanPause" to Variant(currentSong != null),
                    "CanSeek" to Variant(currentSong != null),
                    "CanControl" to Variant(true)
                )
            }
            else -> emptyMap()
        }
    }
}

fun getAppIconUrl(): String? {
    return try {
        val configDir = File(System.getProperty("user.home"), ".config/lune").apply { mkdirs() }
        val iconFile = File(configDir, "icon.png")
        if (!iconFile.exists()) {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icons/icon.png")
                ?: File("src/main/resources/icons/icon.png").takeIf { it.exists() }?.inputStream()
            stream?.use { input ->
                iconFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (iconFile.exists()) {
            "file://${iconFile.absolutePath}"
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

