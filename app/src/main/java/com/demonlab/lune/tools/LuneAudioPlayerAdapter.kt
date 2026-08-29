package com.demonlab.lune.tools

import android.content.ContentUris
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class LuneAudioPlayerAdapter(
    private val service: MusicService
) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val mainHandler = Handler(Looper.getMainLooper())
    var currentArtworkData: ByteArray? = null

    private val availableCommands = Player.Commands.Builder()
        .addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA,
            Player.COMMAND_SET_REPEAT_MODE,
            Player.COMMAND_SET_SHUFFLE_MODE
        )
        .build()

    override fun getState(): State {
        val pm = PlaybackManager.getInstance(service)
        val currentSong = pm.currentSong
        val isPlaying = service.isPlaying()

        val stateBuilder = State.Builder()
            .setAvailableCommands(availableCommands)
            .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (currentSong != null) Player.STATE_READY else Player.STATE_IDLE)
            .setRepeatMode(pm.repeatMode)
            .setShuffleModeEnabled(pm.isShuffle)

        if (currentSong != null) {
            val artUri = if (currentSong.coverUrl != null) {
                Uri.parse(currentSong.coverUrl)
            } else {
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    currentSong.albumId
                )
            }

            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(currentSong.title)
                .setArtist(currentSong.artist)
                .setAlbumTitle(currentSong.album)
                .setArtworkUri(artUri)
                .setIsPlayable(true)

            currentArtworkData?.let {
                metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }

            val mediaMetadata = metadataBuilder.build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(currentSong.id.toString())
                .setUri(currentSong.uri)
                .setMediaMetadata(mediaMetadata)
                .build()

            val durationUs = (currentSong.duration * 1000L).coerceAtLeast(0L)
            val itemData = MediaItemData.Builder(currentSong.id.toString())
                .setMediaItem(mediaItem)
                .setMediaMetadata(mediaMetadata)
                .setDurationUs(durationUs)
                .setIsSeekable(true)
                .build()

            stateBuilder.setPlaylist(listOf(itemData))
            stateBuilder.setCurrentMediaItemIndex(0)
            stateBuilder.setContentPositionMs(PositionSupplier {
                service.currentPosition().toLong().coerceAtLeast(0L)
            })
        } else {
            stateBuilder.setPlaylist(emptyList())
            stateBuilder.setContentPositionMs(0L)
        }

        return stateBuilder.build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            service.resume()
        } else {
            service.pause()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val pm = PlaybackManager.getInstance(service)
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                pm.playNextFromService()
            }
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                pm.playPreviousFromService()
            }
            else -> {
                if (positionMs != C.TIME_UNSET) {
                    service.seekTo(positionMs.toInt())
                }
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        val pm = PlaybackManager.getInstance(service)
        pm.repeatMode = repeatMode
        notifyStateChanged()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        val pm = PlaybackManager.getInstance(service)
        if (pm.isShuffle != shuffleModeEnabled) {
            pm.toggleShuffle()
            notifyStateChanged()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        service.pause()
        return Futures.immediateVoidFuture()
    }

    fun notifyStateChanged() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidateState()
        } else {
            mainHandler.post { invalidateState() }
        }
    }
}
