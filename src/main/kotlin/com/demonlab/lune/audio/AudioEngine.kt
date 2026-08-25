package com.demonlab.lune.audio

import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.*

interface AudioEngineListener {
    fun onTrackEnded()
    fun onPositionChanged(positionMs: Long, durationMs: Long)
    fun onStateChanged(isPlaying: Boolean)
    fun onError(message: String)
}

class AudioEngine private constructor() {
    private var listener: AudioEngineListener? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null

    private var currentFile: File? = null
    @Volatile
    private var currentPositionMs = 0L
    @Volatile
    private var totalDurationMs = 0L

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(true)
    private val generationCounter = AtomicInteger(0)

    @Volatile
    var isPlaying = false
        private set

    @Volatile
    private var currentVolume = 1.0f

    @Volatile
    private var currentBalance = 0.5f // 0.0: Left, 0.5: Center, 1.0: Right

    @Volatile
    private var activeSourceLine: SourceDataLine? = null

    fun setListener(listener: AudioEngineListener) {
        this.listener = listener
    }

    val loadedFile: File? get() = currentFile

    fun load(file: File, initialPositionMs: Long = 0L): Boolean {
        if (!file.exists()) return false
        stop()
        currentFile = file
        currentPositionMs = initialPositionMs
        isPaused.set(false)
        return true
    }

    fun play() {
        val file = currentFile ?: return
        if (isPlaying && isPaused.get()) {
            isPaused.set(false)
            activeSourceLine?.start()
            listener?.onStateChanged(true)
            return
        }

        startStream(file, currentPositionMs)
    }

    fun pause() {
        if (isPlaying && !isPaused.get()) {
            isPaused.set(true)
            activeSourceLine?.stop()
            listener?.onStateChanged(false)
        }
    }

    fun stop() {
        generationCounter.incrementAndGet()
        isStopped.set(true)
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        currentPositionMs = 0L
        listener?.onStateChanged(false)
    }

    fun seekTo(positionMs: Long) {
        val file = currentFile ?: return
        currentPositionMs = positionMs.coerceAtLeast(0L)
        if (isPlaying) {
            startStream(file, currentPositionMs)
        } else {
            listener?.onPositionChanged(currentPositionMs, totalDurationMs)
        }
    }

    fun setVolume(vol: Float) {
        this.currentVolume = vol.coerceIn(0f, 2f)
    }

    fun setBalance(bal: Float) {
        this.currentBalance = bal.coerceIn(0f, 1f)
    }

    fun getPosition(): Long = currentPositionMs

    fun getDuration(): Long = totalDurationMs

    private fun startStream(file: File, startPositionMs: Long) {
        val generation = generationCounter.incrementAndGet()
        playbackJob?.cancel()

        isStopped.set(false)
        isPlaying = true
        listener?.onStateChanged(true)

        playbackJob = scope.launch {
            var localLine: SourceDataLine? = null
            var localProcess: Process? = null
            var localStream: InputStream? = null

            try {
                val format = AudioFormat(44100f, 16, 2, true, false)
                val info = DataLine.Info(SourceDataLine::class.java, format)

                val line = (AudioSystem.getLine(info) as SourceDataLine).apply {
                    open(format, 44100 * 4) // 1 second buffer
                    start()
                }
                localLine = line
                activeSourceLine = line

                val startSeconds = startPositionMs / 1000.0
                val cmd = mutableListOf(
                    "ffmpeg",
                    "-ss", String.format(java.util.Locale.US, "%.3f", startSeconds),
                    "-i", file.absolutePath,
                    "-f", "s16le",
                    "-ar", "44100",
                    "-ac", "2",
                    "-"
                )

                val process = ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()

                localProcess = process
                val stream = process.inputStream
                localStream = stream

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var streamPosMs = startPositionMs
                val bytesPerMillisecond = (44100 * 4) / 1000.0

                while (isActive && generationCounter.get() == generation && !isStopped.get()) {
                    if (isPaused.get()) {
                        delay(40)
                        continue
                    }

                    bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) {
                        // End of stream
                        break
                    }

                    // Apply Volume & Balance DSP in real time
                    applyPcmDsp(buffer, bytesRead)

                    line.write(buffer, 0, bytesRead)
                    streamPosMs += (bytesRead / bytesPerMillisecond).toLong()
                    currentPositionMs = streamPosMs

                    if (generationCounter.get() == generation) {
                        listener?.onPositionChanged(currentPositionMs, totalDurationMs)
                    }
                }

                if (generationCounter.get() == generation && !isStopped.get()) {
                    line.drain()
                    line.stop()
                    line.close()

                    isPlaying = false
                    listener?.onStateChanged(false)
                    listener?.onTrackEnded()
                }
            } catch (e: Exception) {
                if (generationCounter.get() == generation && !isStopped.get() && e !is CancellationException) {
                    e.printStackTrace()
                    isPlaying = false
                    listener?.onStateChanged(false)
                    listener?.onError(e.message ?: "Playback error")
                }
            } finally {
                try { localStream?.close() } catch (e: Exception) {}
                try { localProcess?.destroyForcibly() } catch (e: Exception) {}
                try {
                    localLine?.stop()
                    localLine?.flush()
                    localLine?.close()
                } catch (e: Exception) {}
                if (activeSourceLine == localLine) {
                    activeSourceLine = null
                }
            }
        }
    }

    private fun applyPcmDsp(buffer: ByteArray, length: Int) {
        val leftGain = (if (currentBalance <= 0.5f) 1f else 1f - (currentBalance - 0.5f) * 2f).coerceIn(0f, 1f) * currentVolume
        val rightGain = (if (currentBalance >= 0.5f) 1f else currentBalance * 2f).coerceIn(0f, 1f) * currentVolume

        var i = 0
        while (i + 3 < length) {
            // Left sample (16-bit little endian)
            var left = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            if (left >= 32768) left -= 65536
            val processedLeft = (left * leftGain).toInt().coerceIn(-32768, 32767)
            buffer[i] = (processedLeft and 0xFF).toByte()
            buffer[i + 1] = ((processedLeft shr 8) and 0xFF).toByte()

            // Right sample (16-bit little endian)
            var right = (buffer[i + 2].toInt() and 0xFF) or (buffer[i + 3].toInt() shl 8)
            if (right >= 32768) right -= 65536
            val processedRight = (right * rightGain).toInt().coerceIn(-32768, 32767)
            buffer[i + 2] = (processedRight and 0xFF).toByte()
            buffer[i + 3] = ((processedRight shr 8) and 0xFF).toByte()

            i += 4
        }
    }

    fun release() {
        stop()
    }

    companion object {
        val instance: AudioEngine by lazy { AudioEngine() }
    }
}
