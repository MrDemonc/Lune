package com.demonlab.lune.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Pure Kotlin WavPack (.wv) decoder, header parser, and APEv2 metadata/cover/lyrics reader.
 * Decodes .wv files to standard PCM WAV in cache so Android's MediaPlayer (and all Lune AudioFX,
 * equalizer, visualizer, crossfade, and speed/pitch features) works seamlessly without C++/JNI.
 */
object WavPackDecoder {

    private const val TAG = "WavPackDecoder"
    private const val MAX_CACHE_FILES = 6
    private const val MAX_CACHE_BYTES = 250L * 1024L * 1024L // 250 MB

    private val decodeLocks = ConcurrentHashMap<String, Any>()

    // WavPack header flags
    private const val BYTES_STORED = 0x00000003
    private const val MONO_FLAG = 0x00000004
    private const val HYBRID_FLAG = 0x00000008
    private const val JOINT_STEREO = 0x00000010
    private const val FLOAT_DATA = 0x00000080
    private const val INITIAL_BLOCK = 0x00000800
    private const val FINAL_BLOCK = 0x00001000
    private const val SHIFT_LSB = 13
    private const val SHIFT_MASK = 0x1F shl SHIFT_LSB
    private const val SRATE_LSB = 23
    private const val SRATE_MASK = 0x0F shl SRATE_LSB
    private const val FALSE_STEREO = 0x40000000
    private const val DSD_FLAG = -0x80000000 // 0x80000000

    // Sub-block IDs
    private const val ID_UNIQUE = 0x3F
    private const val ID_ODD_SIZE = 0x40
    private const val ID_LARGE = 0x80

    private const val ID_DECORR_TERMS = 0x02
    private const val ID_DECORR_WEIGHTS = 0x03
    private const val ID_DECORR_SAMPLES = 0x04
    private const val ID_ENTROPY_VARS = 0x05
    private const val ID_HYBRID_PROFILE = 0x06
    private const val ID_FLOAT_INFO = 0x08
    private const val ID_INT32_INFO = 0x09
    private const val ID_WV_BITSTREAM = 0x0A
    private const val ID_FLOAT_DATA = 0x0B
    private const val ID_CHANNEL_INFO = 0x0D
    private const val ID_SAMPLE_RATE = 0x27

    // Float flags (from WavPack float.c)
    private const val FLOAT_SHIFT_ONES = 0x01
    private const val FLOAT_SHIFT_SAME = 0x02
    private const val FLOAT_SHIFT_SENT = 0x04
    private const val FLOAT_ZEROS_SENT = 0x08
    private const val FLOAT_NEG_ZEROS = 0x10
    private const val FLOAT_EXCEPTIONS = 0x20

    private val SAMPLE_RATES = intArrayOf(
        6000, 8000, 9600, 11025, 12000, 16000, 22050, 24000,
        32000, 44100, 48000, 64000, 88200, 96000, 192000, 0
    )

    private val EXP2_TABLE = intArrayOf(
        0x100, 0x101, 0x101, 0x102, 0x103, 0x103, 0x104, 0x105,
        0x106, 0x106, 0x107, 0x108, 0x108, 0x109, 0x10a, 0x10b,
        0x10b, 0x10c, 0x10d, 0x10e, 0x10e, 0x10f, 0x110, 0x110,
        0x111, 0x112, 0x113, 0x113, 0x114, 0x115, 0x116, 0x116,
        0x117, 0x118, 0x119, 0x119, 0x11a, 0x11b, 0x11c, 0x11d,
        0x11d, 0x11e, 0x11f, 0x120, 0x120, 0x121, 0x122, 0x123,
        0x124, 0x124, 0x125, 0x126, 0x127, 0x128, 0x128, 0x129,
        0x12a, 0x12b, 0x12c, 0x12c, 0x12d, 0x12e, 0x12f, 0x130,
        0x130, 0x131, 0x132, 0x133, 0x134, 0x135, 0x135, 0x136,
        0x137, 0x138, 0x139, 0x13a, 0x13a, 0x13b, 0x13c, 0x13d,
        0x13e, 0x13f, 0x140, 0x141, 0x141, 0x142, 0x143, 0x144,
        0x145, 0x146, 0x147, 0x148, 0x148, 0x149, 0x14a, 0x14b,
        0x14c, 0x14d, 0x14e, 0x14f, 0x150, 0x151, 0x151, 0x152,
        0x153, 0x154, 0x155, 0x156, 0x157, 0x158, 0x159, 0x15a,
        0x15b, 0x15c, 0x15d, 0x15e, 0x15e, 0x15f, 0x160, 0x161,
        0x162, 0x163, 0x164, 0x165, 0x166, 0x167, 0x168, 0x169,
        0x16a, 0x16b, 0x16c, 0x16d, 0x16e, 0x16f, 0x170, 0x171,
        0x172, 0x173, 0x174, 0x175, 0x176, 0x177, 0x178, 0x179,
        0x17a, 0x17b, 0x17c, 0x17d, 0x17e, 0x17f, 0x180, 0x181,
        0x182, 0x183, 0x184, 0x185, 0x187, 0x188, 0x189, 0x18a,
        0x18b, 0x18c, 0x18d, 0x18e, 0x18f, 0x190, 0x191, 0x192,
        0x193, 0x195, 0x196, 0x197, 0x198, 0x199, 0x19a, 0x19b,
        0x19c, 0x19d, 0x19f, 0x1a0, 0x1a1, 0x1a2, 0x1a3, 0x1a4,
        0x1a5, 0x1a6, 0x1a8, 0x1a9, 0x1aa, 0x1ab, 0x1ac, 0x1ad,
        0x1af, 0x1b0, 0x1b1, 0x1b2, 0x1b3, 0x1b4, 0x1b6, 0x1b7,
        0x1b8, 0x1b9, 0x1ba, 0x1bc, 0x1bd, 0x1be, 0x1bf, 0x1c0,
        0x1c2, 0x1c3, 0x1c4, 0x1c5, 0x1c6, 0x1c8, 0x1c9, 0x1ca,
        0x1cb, 0x1cd, 0x1ce, 0x1cf, 0x1d0, 0x1d2, 0x1d3, 0x1d4,
        0x1d6, 0x1d7, 0x1d8, 0x1d9, 0x1db, 0x1dc, 0x1dd, 0x1de,
        0x1e0, 0x1e1, 0x1e2, 0x1e4, 0x1e5, 0x1e6, 0x1e8, 0x1e9,
        0x1ea, 0x1ec, 0x1ed, 0x1ee, 0x1f0, 0x1f1, 0x1f2, 0x1f4,
        0x1f5, 0x1f6, 0x1f8, 0x1f9, 0x1fa, 0x1fc, 0x1fd, 0x1ff
    )

    data class WavPackMetadata(
        val sampleRate: Int,
        val bitDepth: Int,
        val channels: Int,
        val totalSamples: Long,
        val durationMs: Long,
        val bitrate: Int?,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val genre: String? = null,
        val trackNumber: Int = 0,
        val lyrics: String? = null,
        val coverArt: ByteArray? = null
    )

    data class ApeTags(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val genre: String? = null,
        val trackNumber: Int = 0,
        val lyrics: String? = null,
        val coverArt: ByteArray? = null
    )

    fun isWavPackPathOrUri(path: String?, uri: Uri? = null, format: String? = null): Boolean {
        if (format?.equals("WV", ignoreCase = true) == true || format?.equals("WAVPACK", ignoreCase = true) == true) {
            return true
        }
        if (!path.isNullOrBlank() && path.substringAfterLast('.', "").equals("wv", ignoreCase = true)) {
            return true
        }
        val uriStr = uri?.toString()
        if (!uriStr.isNullOrBlank() && uriStr.substringBefore('?').substringAfterLast('.', "").equals("wv", ignoreCase = true)) {
            return true
        }
        return false
    }

    /**
     * Parses WavPack header and APEv2 tags from a local file without decoding audio samples.
     */
    fun parseFile(file: File): WavPackMetadata? {
        if (!file.exists() || !file.canRead() || file.length() < 32) return null
        return try {
            FileInputStream(file).use { fis ->
                parseChannel(fis.channel, file.length())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse WavPack metadata: ${e.message}")
            null
        }
    }

    /**
     * Parses WavPack header and APEv2 tags from a content:// or file:// Uri.
     */
    fun parseUri(context: Context, uri: Uri): WavPackMetadata? {
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank()) return parseFile(File(path))
        }
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val ch = fis.channel
                    parseChannel(ch, ch.size())
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse WavPack URI metadata: ${e.message}")
            null
        }
    }

    private fun parseChannel(channel: FileChannel, fileSize: Long): WavPackMetadata? {
        if (fileSize < 32) return null
        val apeTags = readApeTags(channel, fileSize)
        val headerOffset = findFirstWvpkBlock(channel, fileSize) ?: return null
        val headerBuf = ByteBuffer.allocate(32)
        channel.position(headerOffset)
        readChannelFully(channel, headerBuf)
        val hdr = parseBlockHeader(headerBuf.array()) ?: return null

        var sampleRate = SAMPLE_RATES[(hdr.flags and SRATE_MASK) ushr SRATE_LSB]
        var channels = if ((hdr.flags and MONO_FLAG) != 0) 1 else 2
        val bitDepth = ((hdr.flags and BYTES_STORED) + 1) * 8

        val subBytesCount = (hdr.ckSize - 24).toInt().coerceIn(0, 1024 * 1024)
        if (subBytesCount > 0 && headerOffset + 32 + subBytesCount <= fileSize) {
            val subBuf = ByteBuffer.allocate(subBytesCount)
            readChannelFully(channel, subBuf)
            val subData = subBuf.array()
            var pos = 0
            while (pos + 2 <= subData.size) {
                val id = subData[pos++].toInt() and 0xFF
                val wordCount: Int
                if ((id and ID_LARGE) != 0) {
                    if (pos + 3 > subData.size) break
                    wordCount = (subData[pos].toInt() and 0xFF) or
                        ((subData[pos + 1].toInt() and 0xFF) shl 8) or
                        ((subData[pos + 2].toInt() and 0xFF) shl 16)
                    pos += 3
                } else {
                    wordCount = subData[pos++].toInt() and 0xFF
                }
                val byteCount = wordCount * 2
                if (pos + byteCount > subData.size) break
                val actualSize = if ((id and ID_ODD_SIZE) != 0 && byteCount > 0) byteCount - 1 else byteCount
                when (id and ID_UNIQUE) {
                    ID_SAMPLE_RATE -> {
                        if (actualSize >= 3) {
                            var sr = (subData[pos].toInt() and 0xFF) or
                                ((subData[pos + 1].toInt() and 0xFF) shl 8) or
                                ((subData[pos + 2].toInt() and 0xFF) shl 16)
                            if (actualSize >= 4) {
                                sr = sr or ((subData[pos + 3].toInt() and 0xFF) shl 24)
                            }
                            if (sr > 0) sampleRate = sr
                        }
                    }
                    ID_CHANNEL_INFO -> {
                        if (actualSize >= 1) {
                            val ch = subData[pos].toInt() and 0xFF
                            if (ch > 0) channels = ch
                        }
                    }
                }
                pos += byteCount
            }
        }

        if (sampleRate <= 0) sampleRate = 44100
        val totalSamples = hdr.totalSamples
        val durationMs = if (totalSamples > 0L && sampleRate > 0) {
            (totalSamples * 1000L) / sampleRate
        } else 0L

        val bitrate = if (durationMs > 0) {
            ((fileSize * 8.0) / (durationMs / 1000.0)).toInt()
        } else null

        return WavPackMetadata(
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            channels = channels,
            totalSamples = totalSamples,
            durationMs = durationMs,
            bitrate = bitrate,
            title = apeTags?.title,
            artist = apeTags?.artist,
            album = apeTags?.album,
            genre = apeTags?.genre,
            trackNumber = apeTags?.trackNumber ?: 0,
            lyrics = apeTags?.lyrics,
            coverArt = apeTags?.coverArt
        )
    }

    fun extractCoverArt(file: File): ByteArray? = readApeTags(file)?.coverArt

    fun extractCoverArt(context: Context, uri: Uri): ByteArray? = readApeTags(context, uri)?.coverArt

    fun extractLyrics(file: File): String? = readApeTags(file)?.lyrics

    fun extractLyrics(context: Context, uri: Uri): String? = readApeTags(context, uri)?.lyrics

    fun readApeTags(file: File): ApeTags? {
        if (!file.exists() || !file.canRead() || file.length() < 32) return null
        return try {
            FileInputStream(file).use { fis ->
                readApeTags(fis.channel, file.length())
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read APEv2 tags: ${e.message}")
            null
        }
    }

    fun readApeTags(context: Context, uri: Uri): ApeTags? {
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank()) return readApeTags(File(path))
        }
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val ch = fis.channel
                    readApeTags(ch, ch.size())
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read APEv2 tags from URI: ${e.message}")
            null
        }
    }

    /**
     * Reads APEv2 tags from the end of a WavPack file (supports optional trailing 128-byte ID3v1 tag).
     */
    private fun readApeTags(channel: FileChannel, len: Long): ApeTags? {
        if (len < 32) return null
        return try {
            val footerOffsets = longArrayOf(len - 32L, len - 160L)
            var footerPos = -1L
            val buf = ByteArray(32)
            val bbWrap = ByteBuffer.wrap(buf)
            for (offset in footerOffsets) {
                if (offset < 0) continue
                bbWrap.clear()
                channel.position(offset)
                readChannelFully(channel, bbWrap)
                if (buf[0] == 'A'.code.toByte() && buf[1] == 'P'.code.toByte() &&
                    buf[2] == 'E'.code.toByte() && buf[3] == 'T'.code.toByte() &&
                    buf[4] == 'A'.code.toByte() && buf[5] == 'G'.code.toByte() &&
                    buf[6] == 'E'.code.toByte() && buf[7] == 'X'.code.toByte()
                ) {
                    footerPos = offset
                    break
                }
            }
            if (footerPos < 0) return null

            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(8)
            val version = bb.int
            val tagSize = bb.int.toLong() and 0xFFFFFFFFL // includes footer, excludes header
            val itemCount = bb.int

            if (version != 1000 && version != 2000) return null
            if (tagSize < 32 || tagSize > 32L * 1024L * 1024L) return null
            if (itemCount <= 0 || itemCount > 10000) return null

            val itemsStart = footerPos + 32L - tagSize
            if (itemsStart < 0) return null
            val itemsLen = (footerPos - itemsStart).toInt()
            if (itemsLen <= 0) return null

            val data = ByteArray(itemsLen)
            channel.position(itemsStart)
            readChannelFully(channel, ByteBuffer.wrap(data))

            var title: String? = null
            var artist: String? = null
            var albumArtist: String? = null
            var album: String? = null
            var genre: String? = null
            var trackNumber = 0
            var lyrics: String? = null
            var frontCover: ByteArray? = null
            var otherCover: ByteArray? = null

            var pos = 0
            for (i in 0 until itemCount) {
                if (pos + 8 > data.size) break
                val valueLen = (data[pos].toInt() and 0xFF) or
                    ((data[pos + 1].toInt() and 0xFF) shl 8) or
                    ((data[pos + 2].toInt() and 0xFF) shl 16) or
                    ((data[pos + 3].toInt() and 0xFF) shl 24)
                val flags = (data[pos + 4].toInt() and 0xFF) or
                    ((data[pos + 5].toInt() and 0xFF) shl 8) or
                    ((data[pos + 6].toInt() and 0xFF) shl 16) or
                    ((data[pos + 7].toInt() and 0xFF) shl 24)
                pos += 8

                val keyStart = pos
                while (pos < data.size && data[pos] != 0.toByte()) {
                    pos++
                }
                if (pos >= data.size) break
                val key = String(data, keyStart, pos - keyStart, Charsets.UTF_8)
                pos++ // skip null terminator

                if (valueLen < 0 || pos + valueLen > data.size) break
                val itemType = (flags ushr 1) and 0x03

                if (itemType == 0) {
                    val value = String(data, pos, valueLen, Charsets.UTF_8).trim()
                    when (key.lowercase()) {
                        "title" -> if (value.isNotEmpty()) title = value
                        "artist" -> if (value.isNotEmpty()) artist = value
                        "album artist", "albumartist" -> if (value.isNotEmpty()) albumArtist = value
                        "album" -> if (value.isNotEmpty()) album = value
                        "genre" -> if (value.isNotEmpty()) genre = value
                        "track" -> {
                            val parsed = value.substringBefore('/').trim().toIntOrNull()
                            if (parsed != null && parsed > 0) trackNumber = parsed
                        }
                        "lyrics", "unsyncedlyrics", "unsynced lyrics" -> {
                            if (value.isNotEmpty()) lyrics = value
                        }
                    }
                } else if (itemType == 1 && key.startsWith("cover art", ignoreCase = true)) {
                    var nameEnd = pos
                    val valueEnd = pos + valueLen
                    while (nameEnd < valueEnd && data[nameEnd] != 0.toByte()) {
                        nameEnd++
                    }
                    if (nameEnd < valueEnd) {
                        val imgStart = nameEnd + 1
                        if (imgStart < valueEnd) {
                            val bytes = data.copyOfRange(imgStart, valueEnd)
                            if (key.contains("front", ignoreCase = true)) {
                                frontCover = bytes
                            } else if (otherCover == null) {
                                otherCover = bytes
                            }
                        }
                    }
                }

                pos += valueLen
            }

            ApeTags(
                title = title,
                artist = artist ?: albumArtist,
                album = album,
                genre = genre,
                trackNumber = trackNumber,
                lyrics = lyrics,
                coverArt = frontCover ?: otherCover
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read APEv2 tags: ${e.message}")
            null
        }
    }

    /**
     * Returns a cached decoded .wav file for the given .wv file or content URI, decoding if needed.
     */
    fun getOrDecodeWav(
        context: Context,
        path: String?,
        uri: Uri?
    ): File? {
        val sourceFile = path?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.canRead() }
        val cacheKeySource = if (sourceFile != null) {
            "${sourceFile.absolutePath}:${sourceFile.length()}:${sourceFile.lastModified()}"
        } else {
            uri?.toString() ?: return null
        }

        val cacheDir = File(context.cacheDir, "wv_cache").apply { if (!exists()) mkdirs() }
        val hash = sha256Short(cacheKeySource)
        val cachedWav = File(cacheDir, "$hash.wav")
        if (cachedWav.exists() && cachedWav.length() > 44L) {
            cachedWav.setLastModified(System.currentTimeMillis())
            return cachedWav
        }

        val lock = decodeLocks.getOrPut(hash) { Any() }
        synchronized(lock) {
            if (cachedWav.exists() && cachedWav.length() > 44L) {
                cachedWav.setLastModified(System.currentTimeMillis())
                return cachedWav
            }
            val tempFile = File(cacheDir, "$hash.tmp")
            return try {
                val ok = if (sourceFile != null) {
                    FileInputStream(sourceFile).use { input ->
                        decodeStreamToWav(input, tempFile)
                    }
                } else if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        decodeStreamToWav(input, tempFile)
                    } ?: false
                } else {
                    false
                }

                if (ok && tempFile.exists() && tempFile.length() > 44L) {
                    if (cachedWav.exists()) cachedWav.delete()
                    if (!tempFile.renameTo(cachedWav)) {
                        tempFile.copyTo(cachedWav, overwrite = true)
                        tempFile.delete()
                    }
                    trimCache(cacheDir, cachedWav)
                    cachedWav
                } else {
                    tempFile.delete()
                    null
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error decoding WavPack file: ${e.message}", e)
                tempFile.delete()
                null
            }
        }
    }

    private fun trimCache(cacheDir: File, keep: File) {
        try {
            val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".wav") && f.absolutePath != keep.absolutePath }
                ?.sortedBy { it.lastModified() }
                ?.toMutableList() ?: return
            var totalBytes = files.sumOf { it.length() } + keep.length()
            while (files.size >= MAX_CACHE_FILES || totalBytes > MAX_CACHE_BYTES) {
                val oldest = files.removeFirstOrNull() ?: break
                totalBytes -= oldest.length()
                oldest.delete()
            }
        } catch (_: Throwable) {}
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * Decodes a WavPack stream to a standard PCM WAV file.
     */
    fun decodeStreamToWav(input: InputStream, destWavFile: File): Boolean {
        val buffered = input.buffered(64 * 1024)
        var wavOut: BufferedOutputStream? = null
        var pcmBytesWritten = 0L
        var sampleRate = 0
        var channels = 0
        var bytesPerSample = 0
        var isFloat = false
        var expectedTotalSamples = -1L
        var decodedSamplesCount = 0L
        var foundFirstBlock = false

        try {
            wavOut = BufferedOutputStream(FileOutputStream(destWavFile), 64 * 1024)
            // Write 44-byte placeholder WAV header; we will patch sizes at the end
            wavOut.write(ByteArray(44))

            val headerBuf = ByteArray(32)
            while (true) {
                if (expectedTotalSamples > 0 && decodedSamplesCount >= expectedTotalSamples) {
                    break
                }
                val hdr = if (!foundFirstBlock) {
                    readNextBlockHeader(buffered, headerBuf)
                } else {
                    readSequentialBlockHeader(buffered, headerBuf)
                } ?: break
                foundFirstBlock = true

                if (expectedTotalSamples < 0 && hdr.totalSamples > 0) {
                    expectedTotalSamples = hdr.totalSamples
                }

                val subBytesLen = (hdr.ckSize - 24).toInt()
                if (subBytesLen < 0 || subBytesLen > 16 * 1024 * 1024) {
                    throw IOException("Invalid WavPack block size: ${hdr.ckSize}")
                }
                val blockData = ByteArray(subBytesLen)
                readFully(buffered, blockData)

                if (hdr.blockSamples <= 0L) {
                    continue
                }
                if ((hdr.flags and DSD_FLAG) != 0) {
                    throw IOException("DSD-in-WavPack is not supported by pure Kotlin PCM decoder")
                }

                // Skip secondary multichannel blocks to keep stereo/mono WAV output valid
                val isPrimaryBlock = (hdr.flags and INITIAL_BLOCK) != 0
                if (!isPrimaryBlock) {
                    continue
                }

                val blockChannels = if ((hdr.flags and MONO_FLAG) != 0) 1 else 2
                val blockBps = (hdr.flags and BYTES_STORED) + 1
                val blockIsFloat = (hdr.flags and FLOAT_DATA) != 0

                val decoded = decodeBlock(hdr, blockData)
                if (sampleRate == 0) {
                    sampleRate = decoded.sampleRate
                    channels = blockChannels
                    bytesPerSample = blockBps
                    isFloat = blockIsFloat
                }

                val outBuf = ByteArray(decoded.samples.size * blockBps)
                var outIdx = 0
                for (s in decoded.samples) {
                    when (blockBps) {
                        1 -> {
                            outBuf[outIdx++] = ((s + 128) and 0xFF).toByte()
                        }
                        2 -> {
                            outBuf[outIdx++] = (s and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 8) and 0xFF).toByte()
                        }
                        3 -> {
                            outBuf[outIdx++] = (s and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 8) and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 16) and 0xFF).toByte()
                        }
                        4 -> {
                            outBuf[outIdx++] = (s and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 8) and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 16) and 0xFF).toByte()
                            outBuf[outIdx++] = ((s ushr 24) and 0xFF).toByte()
                        }
                    }
                }
                wavOut.write(outBuf, 0, outIdx)
                pcmBytesWritten += outIdx
                decodedSamplesCount += hdr.blockSamples
            }

            wavOut.flush()
            wavOut.close()
            wavOut = null

            if (pcmBytesWritten <= 0 || sampleRate <= 0 || channels <= 0 || bytesPerSample <= 0) {
                return false
            }

            RandomAccessFile(destWavFile, "rw").use { raf ->
                raf.seek(0)
                writeWavHeader(
                    raf = raf,
                    sampleRate = sampleRate,
                    channels = channels,
                    bitsPerSample = bytesPerSample * 8,
                    dataBytes = pcmBytesWritten,
                    isFloat = isFloat
                )
            }
            return true
        } finally {
            try { wavOut?.close() } catch (_: Throwable) {}
        }
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        dataBytes: Long,
        isFloat: Boolean
    ) {
        val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        val byteRate = sampleRate.toLong() * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        val riffSize = (36L + dataBytes).coerceAtMost(0xFFFFFFFFL).toInt()
        val dataSize = dataBytes.coerceAtMost(0xFFFFFFFFL).toInt()

        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(riffSize)
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16)
        bb.putShort(if (isFloat) 3 else 1)
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate.toInt())
        bb.putShort(blockAlign)
        bb.putShort(bitsPerSample.toShort())
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(dataSize)
        raf.write(bb.array())
    }

    private data class BlockHeader(
        val ckSize: Long,
        val version: Int,
        val totalSamples: Long,
        val blockIndex: Long,
        val blockSamples: Long,
        val flags: Int,
        val crc: Int
    )

    private fun parseBlockHeader(b: ByteArray): BlockHeader? {
        if (b.size < 32) return null
        if (b[0] != 'w'.code.toByte() || b[1] != 'v'.code.toByte() ||
            b[2] != 'p'.code.toByte() || b[3] != 'k'.code.toByte()
        ) return null
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        bb.position(4)
        val ckSize = bb.int.toLong() and 0xFFFFFFFFL
        val version = bb.short.toInt() and 0xFFFF
        val blockIndexU8 = bb.get().toInt() and 0xFF
        val totalSamplesU8 = bb.get().toInt() and 0xFF
        val totalSamplesLow = bb.int.toLong() and 0xFFFFFFFFL
        val blockIndexLow = bb.int.toLong() and 0xFFFFFFFFL
        val blockSamples = bb.int.toLong() and 0xFFFFFFFFL
        val flags = bb.int
        val crc = bb.int

        val totalSamples = if (totalSamplesLow == 0xFFFFFFFFL) {
            -1L
        } else {
            totalSamplesLow or (totalSamplesU8.toLong() shl 32)
        }
        val blockIndex = blockIndexLow or (blockIndexU8.toLong() shl 32)

        return BlockHeader(
            ckSize = ckSize,
            version = version,
            totalSamples = totalSamples,
            blockIndex = blockIndex,
            blockSamples = blockSamples,
            flags = flags,
            crc = crc
        )
    }

    private fun findFirstWvpkBlock(channel: FileChannel, fileSize: Long): Long? {
        val maxSearch = fileSize.coerceAtMost(2L * 1024L * 1024L)
        val buf = ByteArray(4096)
        val bb = ByteBuffer.wrap(buf)
        var offset = 0L
        while (offset + 32 <= maxSearch) {
            bb.clear()
            channel.position(offset)
            val n = channel.read(bb)
            if (n < 4) break
            for (i in 0..(n - 4)) {
                if (buf[i] == 'w'.code.toByte() && buf[i + 1] == 'v'.code.toByte() &&
                    buf[i + 2] == 'p'.code.toByte() && buf[i + 3] == 'k'.code.toByte()
                ) {
                    return offset + i
                }
            }
            offset += (n - 3).coerceAtLeast(1)
        }
        return null
    }

    private fun readChannelFully(channel: FileChannel, bb: ByteBuffer) {
        while (bb.hasRemaining()) {
            if (channel.read(bb) < 0) throw IOException("Unexpected EOF in WavPack channel")
        }
    }

    private fun readNextBlockHeader(input: InputStream, headerBuf: ByteArray): BlockHeader? {
        var matched = 0
        val sig = byteArrayOf('w'.code.toByte(), 'v'.code.toByte(), 'p'.code.toByte(), 'k'.code.toByte())
        while (matched < 4) {
            val b = input.read()
            if (b == -1) return null
            val byteVal = b.toByte()
            if (byteVal == sig[matched]) {
                headerBuf[matched] = byteVal
                matched++
            } else if (byteVal == sig[0]) {
                headerBuf[0] = byteVal
                matched = 1
            } else {
                matched = 0
            }
        }
        readFully(input, headerBuf, 4, 28)
        return parseBlockHeader(headerBuf)
    }

    private fun readSequentialBlockHeader(input: InputStream, headerBuf: ByteArray): BlockHeader? {
        var read = 0
        while (read < 4) {
            val n = input.read(headerBuf, read, 4 - read)
            if (n < 0) return null
            read += n
        }
        // Check for APEv2 ("APET") or ID3v1 ("TAG") at end of stream
        if (headerBuf[0] != 'w'.code.toByte() || headerBuf[1] != 'v'.code.toByte() ||
            headerBuf[2] != 'p'.code.toByte() || headerBuf[3] != 'k'.code.toByte()
        ) {
            return null
        }
        readFully(input, headerBuf, 4, 28)
        return parseBlockHeader(headerBuf)
    }

    private fun readFully(input: InputStream, buf: ByteArray, offset: Int = 0, length: Int = buf.size) {
        var read = 0
        while (read < length) {
            val n = input.read(buf, offset + read, length - read)
            if (n < 0) throw IOException("Unexpected EOF in WavPack stream")
            read += n
        }
    }

    // --- Core Block Decoder ---

    private class DecorrPass {
        var term: Int = 0
        var delta: Int = 0
        var weightA: Int = 0
        var weightB: Int = 0
        val samplesA = IntArray(8)
        val samplesB = IntArray(8)
    }

    private class EntropyData {
        val median = IntArray(3)
        var slowLevel: Int = 0
        var errorLimit: Int = 0
    }

    private class DecodedBlock(
        val sampleRate: Int,
        val samples: IntArray
    )

    private fun decodeBlock(hdr: BlockHeader, subData: ByteArray): DecodedBlock {
        var sampleRate = SAMPLE_RATES[(hdr.flags and SRATE_MASK) ushr SRATE_LSB]
        val isMono = (hdr.flags and MONO_FLAG) != 0
        val isFalseStereo = (hdr.flags and FALSE_STEREO) != 0
        val isHybrid = (hdr.flags and HYBRID_FLAG) != 0
        val isJointStereo = (hdr.flags and JOINT_STEREO) != 0
        val isFloat = (hdr.flags and FLOAT_DATA) != 0
        val shiftAmount = (hdr.flags and SHIFT_MASK) ushr SHIFT_LSB
        val numSamples = hdr.blockSamples.toInt()
        val channels = if (isMono) 1 else 2
        val effectiveMono = isMono || isFalseStereo

        var decorrPasses: Array<DecorrPass> = emptyArray()
        val entropy = arrayOf(EntropyData(), EntropyData())
        var bitstreamBytes: ByteArray? = null
        var bitstreamLen = 0
        var floatDataBytes: ByteArray? = null
        var floatDataLen = 0

        var int32Zeros = 0
        var int32Ones = 0
        var int32Dups = 0

        var floatFlags = 0
        var floatShift = 0
        var floatMaxExp = 0

        var pos = 0
        while (pos + 2 <= subData.size) {
            val id = subData[pos++].toInt() and 0xFF
            val wordCount: Int
            if ((id and ID_LARGE) != 0) {
                if (pos + 3 > subData.size) break
                wordCount = (subData[pos].toInt() and 0xFF) or
                    ((subData[pos + 1].toInt() and 0xFF) shl 8) or
                    ((subData[pos + 2].toInt() and 0xFF) shl 16)
                pos += 3
            } else {
                wordCount = subData[pos++].toInt() and 0xFF
            }
            val byteCount = wordCount * 2
            if (pos + byteCount > subData.size) break
            val actualSize = if ((id and ID_ODD_SIZE) != 0 && byteCount > 0) byteCount - 1 else byteCount

            when (id and ID_UNIQUE) {
                ID_DECORR_TERMS -> {
                    val count = actualSize.coerceAtMost(16)
                    decorrPasses = Array(count) { DecorrPass() }
                    for (i in 0 until count) {
                        val b = subData[pos + i].toInt() and 0xFF
                        val p = decorrPasses[count - 1 - i]
                        p.term = (b and 0x1F) - 5
                        p.delta = (b ushr 5) and 0x07
                    }
                }
                ID_DECORR_WEIGHTS -> {
                    var idx = 0
                    val count = decorrPasses.size
                    for (i in 0 until count) {
                        val p = decorrPasses[count - 1 - i]
                        if (idx < actualSize) {
                            p.weightA = restoreWeight(subData[pos + idx++])
                        }
                        if (!effectiveMono && idx < actualSize) {
                            p.weightB = restoreWeight(subData[pos + idx++])
                        }
                    }
                }
                ID_DECORR_SAMPLES -> {
                    var idx = 0
                    val count = decorrPasses.size
                    for (i in 0 until count) {
                        val p = decorrPasses[count - 1 - i]
                        val term = p.term
                        if (term > 8) {
                            if (idx + 3 < actualSize) {
                                p.samplesA[0] = exp2s(readShortLE(subData, pos + idx))
                                p.samplesA[1] = exp2s(readShortLE(subData, pos + idx + 2))
                                idx += 4
                            }
                            if (!effectiveMono && idx + 3 < actualSize) {
                                p.samplesB[0] = exp2s(readShortLE(subData, pos + idx))
                                p.samplesB[1] = exp2s(readShortLE(subData, pos + idx + 2))
                                idx += 4
                            }
                        } else if (term < 0) {
                            if (idx + 3 < actualSize) {
                                p.samplesA[0] = exp2s(readShortLE(subData, pos + idx))
                                p.samplesB[0] = exp2s(readShortLE(subData, pos + idx + 2))
                                idx += 4
                            }
                        } else if (term in 1..8) {
                            for (m in 0 until term) {
                                if (idx + 1 < actualSize) {
                                    p.samplesA[m] = exp2s(readShortLE(subData, pos + idx))
                                    idx += 2
                                }
                                if (!effectiveMono && idx + 1 < actualSize) {
                                    p.samplesB[m] = exp2s(readShortLE(subData, pos + idx))
                                    idx += 2
                                }
                            }
                        }
                    }
                }
                ID_ENTROPY_VARS -> {
                    var idx = 0
                    for (m in 0 until 3) {
                        if (idx + 1 < actualSize) {
                            entropy[0].median[m] = exp2s(readUShortLE(subData, pos + idx))
                            idx += 2
                        }
                    }
                    if (!effectiveMono) {
                        for (m in 0 until 3) {
                            if (idx + 1 < actualSize) {
                                entropy[1].median[m] = exp2s(readUShortLE(subData, pos + idx))
                                idx += 2
                            }
                        }
                    }
                }
                ID_HYBRID_PROFILE -> {
                    var idx = 0
                    if (isHybrid) {
                        if (idx + 1 < actualSize) {
                            entropy[0].slowLevel = exp2s(readUShortLE(subData, pos + idx))
                            idx += 2
                        }
                        if (!effectiveMono && idx + 1 < actualSize) {
                            entropy[1].slowLevel = exp2s(readUShortLE(subData, pos + idx))
                            idx += 2
                        }
                    }
                }
                ID_FLOAT_INFO -> {
                    if (actualSize >= 4) {
                        floatFlags = subData[pos].toInt() and 0xFF
                        floatShift = subData[pos + 1].toInt() and 0xFF
                        floatMaxExp = subData[pos + 2].toInt() and 0xFF
                    }
                }
                ID_INT32_INFO -> {
                    if (actualSize >= 4) {
                        int32Zeros = subData[pos + 1].toInt() and 0xFF
                        int32Ones = subData[pos + 2].toInt() and 0xFF
                        int32Dups = subData[pos + 3].toInt() and 0xFF
                    }
                }
                ID_WV_BITSTREAM -> {
                    bitstreamBytes = subData.copyOfRange(pos, pos + actualSize)
                    bitstreamLen = actualSize
                }
                ID_FLOAT_DATA -> {
                    floatDataBytes = subData.copyOfRange(pos, pos + actualSize)
                    floatDataLen = actualSize
                }
                ID_SAMPLE_RATE -> {
                    if (actualSize >= 3) {
                        var sr = (subData[pos].toInt() and 0xFF) or
                            ((subData[pos + 1].toInt() and 0xFF) shl 8) or
                            ((subData[pos + 2].toInt() and 0xFF) shl 16)
                        if (actualSize >= 4) {
                            sr = sr or ((subData[pos + 3].toInt() and 0xFF) shl 24)
                        }
                        if (sr > 0) sampleRate = sr
                    }
                }
            }
            pos += byteCount
        }

        if (sampleRate <= 0) sampleRate = 44100
        val outSamples = IntArray(numSamples * channels)

        if (bitstreamBytes != null && bitstreamLen > 0) {
            val reader = BitReader(bitstreamBytes, bitstreamLen)
            val wordDecoder = WordDecoder(reader, entropy)

            val rawBuffer = IntArray(numSamples * (if (effectiveMono) 1 else 2))
            wordDecoder.getWords(rawBuffer, numSamples, effectiveMono)

            if (effectiveMono) {
                for (i in 0 until numSamples) {
                    var sample = rawBuffer[i]
                    for (p in decorrPasses) {
                        val term = p.term
                        if (term > 8) {
                            val sam = if (term == 17) {
                                2 * p.samplesA[0] - p.samplesA[1]
                            } else {
                                (3 * p.samplesA[0] - p.samplesA[1]) shr 1
                            }
                            p.samplesA[1] = p.samplesA[0]
                            val temp = applyWeight(p.weightA, sam) + sample
                            p.weightA = updateWeight(p.weightA, p.delta, sam, sample)
                            p.samplesA[0] = temp
                            sample = temp
                        } else if (term in 1..8) {
                            val k = i and 7
                            val m = (i + term) and 7
                            val sam = p.samplesA[k]
                            val temp = applyWeight(p.weightA, sam) + sample
                            p.weightA = updateWeight(p.weightA, p.delta, sam, sample)
                            p.samplesA[m] = temp
                            sample = temp
                        }
                    }
                    rawBuffer[i] = sample
                }
            } else {
                for (i in 0 until numSamples) {
                    var left = rawBuffer[i * 2]
                    var right = rawBuffer[i * 2 + 1]
                    for (p in decorrPasses) {
                        val term = p.term
                        when {
                            term > 8 -> {
                                val samA = if (term == 17) {
                                    2 * p.samplesA[0] - p.samplesA[1]
                                } else {
                                    (3 * p.samplesA[0] - p.samplesA[1]) shr 1
                                }
                                p.samplesA[1] = p.samplesA[0]
                                val tempA = applyWeight(p.weightA, samA) + left
                                p.weightA = updateWeight(p.weightA, p.delta, samA, left)
                                p.samplesA[0] = tempA
                                left = tempA

                                val samB = if (term == 17) {
                                    2 * p.samplesB[0] - p.samplesB[1]
                                } else {
                                    (3 * p.samplesB[0] - p.samplesB[1]) shr 1
                                }
                                p.samplesB[1] = p.samplesB[0]
                                val tempB = applyWeight(p.weightB, samB) + right
                                p.weightB = updateWeight(p.weightB, p.delta, samB, right)
                                p.samplesB[0] = tempB
                                right = tempB
                            }
                            term in 1..8 -> {
                                val k = i and 7
                                val m = (i + term) and 7

                                val samA = p.samplesA[k]
                                val tempA = applyWeight(p.weightA, samA) + left
                                p.weightA = updateWeight(p.weightA, p.delta, samA, left)
                                p.samplesA[m] = tempA
                                left = tempA

                                val samB = p.samplesB[k]
                                val tempB = applyWeight(p.weightB, samB) + right
                                p.weightB = updateWeight(p.weightB, p.delta, samB, right)
                                p.samplesB[m] = tempB
                                right = tempB
                            }
                            term == -1 -> {
                                val samA = p.samplesB[0]
                                val tempA = applyWeight(p.weightA, samA) + left
                                p.weightA = updateWeightClip(p.weightA, p.delta, samA, left)
                                left = tempA

                                val tempB = applyWeight(p.weightB, left) + right
                                p.weightB = updateWeightClip(p.weightB, p.delta, left, right)
                                p.samplesB[0] = tempB
                                right = tempB
                            }
                            term == -2 -> {
                                val samB = p.samplesA[0]
                                val tempB = applyWeight(p.weightB, samB) + right
                                p.weightB = updateWeightClip(p.weightB, p.delta, samB, right)
                                right = tempB

                                val tempA = applyWeight(p.weightA, right) + left
                                p.weightA = updateWeightClip(p.weightA, p.delta, right, left)
                                p.samplesA[0] = tempA
                                left = tempA
                            }
                            term == -3 -> {
                                val samA = p.samplesB[0]
                                val samB = p.samplesA[0]
                                val tempA = applyWeight(p.weightA, samA) + left
                                p.weightA = updateWeightClip(p.weightA, p.delta, samA, left)
                                val tempB = applyWeight(p.weightB, samB) + right
                                p.weightB = updateWeightClip(p.weightB, p.delta, samB, right)
                                p.samplesA[0] = tempA
                                p.samplesB[0] = tempB
                                left = tempA
                                right = tempB
                            }
                        }
                    }

                    if (isJointStereo) {
                        right -= (left shr 1)
                        left += right
                    }
                    rawBuffer[i * 2] = left
                    rawBuffer[i * 2 + 1] = right
                }
            }

            val floatReader = if (isFloat && floatDataBytes != null && floatDataLen > 0) {
                BitReader(floatDataBytes, floatDataLen)
            } else null

            if (isFalseStereo) {
                for (i in 0 until numSamples) {
                    val s = if (isFloat) {
                        reconstructFloat32(rawBuffer[i], floatFlags, floatShift, floatMaxExp, floatReader)
                    } else {
                        postProcessSample(rawBuffer[i], int32Zeros, int32Ones, int32Dups, shiftAmount)
                    }
                    outSamples[i * 2] = s
                    outSamples[i * 2 + 1] = s
                }
            } else {
                for (i in rawBuffer.indices) {
                    outSamples[i] = if (isFloat) {
                        reconstructFloat32(rawBuffer[i], floatFlags, floatShift, floatMaxExp, floatReader)
                    } else {
                        postProcessSample(rawBuffer[i], int32Zeros, int32Ones, int32Dups, shiftAmount)
                    }
                }
            }
        }

        return DecodedBlock(sampleRate, outSamples)
    }

    /**
     * Reconstructs 32-bit IEEE 754 float bits from WavPack integer mantissa + ID_FLOAT_INFO + ID_FLOAT_DATA.
     */
    private fun reconstructFloat32(
        sampleIn: Int,
        floatFlags: Int,
        floatShift: Int,
        floatMaxExp: Int,
        floatReader: BitReader?
    ): Int {
        var value = sampleIn
        val shiftCount = floatShift.coerceIn(0, 31)

        if (value == 0) {
            if ((floatFlags and FLOAT_ZEROS_SENT) != 0 && floatReader != null && floatReader.getBit() != 0) {
                val exp = floatReader.getBits(8)
                val mantissa = floatReader.getBits(23)
                val sign = floatReader.getBit()
                return (sign shl 31) or ((exp and 0xFF) shl 23) or (mantissa and 0x7FFFFF)
            }
            if ((floatFlags and FLOAT_NEG_ZEROS) != 0 && floatReader != null && floatReader.getBit() != 0) {
                return Int.MIN_VALUE // -0.0f
            }
            return 0 // +0.0f
        }

        if ((floatFlags and FLOAT_EXCEPTIONS) != 0 && floatReader != null && floatReader.getBit() != 0) {
            val sign = if (value < 0) 1 else 0
            val mantissa = (if (value < 0) -value else value) and 0x7FFFFF
            return (sign shl 31) or (0xFF shl 23) or mantissa
        }

        val sign = if (value < 0) 1 else 0
        var mag = if (value < 0) -value.toLong() else value.toLong()

        if (shiftCount > 0) {
            when {
                (floatFlags and FLOAT_SHIFT_ONES) != 0 -> {
                    mag = (mag shl shiftCount) or ((1L shl shiftCount) - 1L)
                }
                (floatFlags and FLOAT_SHIFT_SAME) != 0 -> {
                    val fill = if ((mag and 1L) != 0L) ((1L shl shiftCount) - 1L) else 0L
                    mag = (mag shl shiftCount) or fill
                }
                (floatFlags and FLOAT_SHIFT_SENT) != 0 && floatReader != null -> {
                    val lowBits = floatReader.getBits(shiftCount).toLong() and ((1L shl shiftCount) - 1L)
                    mag = (mag shl shiftCount) or lowBits
                }
                else -> {
                    mag = mag shl shiftCount
                }
            }
        }

        var exp = floatMaxExp
        if (mag == 0L) return if (sign != 0) Int.MIN_VALUE else 0
        while (mag < 0x800000L && exp > 0) {
            mag = mag shl 1
            exp--
        }
        while (mag >= 0x1000000L && exp < 255) {
            mag = mag ushr 1
            exp++
        }

        val mantissa = (mag and 0x7FFFFFL).toInt()
        return (sign shl 31) or ((exp and 0xFF) shl 23) or mantissa
    }

    private fun postProcessSample(
        sampleIn: Int,
        zeros: Int,
        ones: Int,
        dups: Int,
        shiftAmount: Int
    ): Int {
        var s = sampleIn
        if (zeros > 0) {
            s = s shl zeros
        } else if (ones > 0) {
            s = (s shl ones) or ((1 shl ones) - 1)
        } else if (dups > 0) {
            s = if ((s and 1) != 0) {
                (s shl dups) or ((1 shl dups) - 1)
            } else {
                s shl dups
            }
        }
        if (shiftAmount > 0) {
            s = s shl shiftAmount
        }
        return s
    }

    private fun readShortLE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or (buf[offset + 1].toInt() shl 8)
    }

    private fun readUShortLE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or ((buf[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun restoreWeight(b: Byte): Int {
        var result = b.toInt() shl 3
        if (result > 0) result += (result + 64) ushr 7
        return result
    }

    private fun exp2s(log: Int): Int {
        if (log < 0) return -exp2s(-log)
        val value = EXP2_TABLE[log and 0xFF] or 0x100
        val shift = log ushr 8
        return if (shift <= 9) value ushr (9 - shift) else value shl (shift - 9)
    }

    private fun applyWeight(weight: Int, sample: Int): Int {
        return ((weight.toLong() * sample.toLong() + 512L) shr 10).toInt()
    }

    private fun updateWeight(weight: Int, delta: Int, source: Int, result: Int): Int {
        if (source == 0 || result == 0) return weight
        return if ((source xor result) < 0) weight - delta else weight + delta
    }

    private fun updateWeightClip(weight: Int, delta: Int, source: Int, result: Int): Int {
        if (source == 0 || result == 0) return weight
        val w = if ((source xor result) < 0) weight - delta else weight + delta
        return w.coerceIn(-1024, 1024)
    }

    // --- Bitstream & Entropy Decoder ---

    private class BitReader(private val data: ByteArray, private val length: Int) {
        private var bytePos = 0
        private var sr = 0L
        private var bc = 0

        fun getBit(): Int {
            if (bc == 0) {
                if (bytePos >= length) return 0
                sr = (data[bytePos++].toLong() and 0xFFL)
                bc = 8
            }
            val bit = (sr and 1L).toInt()
            sr = sr ushr 1
            bc--
            return bit
        }

        fun getBits(nBits: Int): Int {
            var res = 0
            var shift = 0
            var rem = nBits
            while (rem > 0) {
                if (bc == 0) {
                    if (bytePos >= length) return res
                    sr = (data[bytePos++].toLong() and 0xFFL)
                    bc = 8
                }
                val take = minOf(rem, bc)
                val mask = (1L shl take) - 1L
                res = res or (((sr and mask).toInt()) shl shift)
                sr = sr ushr take
                bc -= take
                shift += take
                rem -= take
            }
            return res
        }
    }

    private class WordDecoder(
        private val reader: BitReader,
        private val entropy: Array<EntropyData>
    ) {
        private var holdingZero = false
        private var holdingOne = false
        private var zerosAcc = 0

        fun getWords(buffer: IntArray, numSamples: Int, isMono: Boolean) {
            val totalValues = if (isMono) numSamples else numSamples * 2
            var i = 0
            while (i < totalValues) {
                if (entropy[0].median[0] < 2 && entropy[1].median[0] < 2 && !holdingZero && !holdingOne) {
                    if (zerosAcc > 0) {
                        zerosAcc--
                        buffer[i++] = 0
                        continue
                    } else {
                        val cbits = readUnary(33)
                        if (cbits == 1) {
                            buffer[i++] = 0
                            continue
                        } else if (cbits in 2..32) {
                            zerosAcc = (1 shl (cbits - 1)) + reader.getBits(cbits - 1) - 1
                            buffer[i++] = 0
                            continue
                        } else if (cbits >= 33) {
                            while (i < totalValues) buffer[i++] = 0
                            return
                        }
                        // cbits == 0: 0 zeros in run, fall through to decode next word normally
                    }
                }

                val c = if (isMono) entropy[0] else entropy[i and 1]
                buffer[i++] = getWord(c)
            }
        }

        private fun getWord(c: EntropyData): Int {
            var onesCount: Int
            if (holdingZero) {
                onesCount = 0
                holdingZero = false
            } else {
                var nextWord = readUnary(16)
                if (nextWord == 16) {
                    val more = readUnary(16)
                    nextWord += if (more < 2) {
                        more
                    } else {
                        (1 shl (more - 1)) + reader.getBits(more - 1)
                    }
                }
                if (holdingOne) {
                    holdingOne = (nextWord and 1) != 0
                    onesCount = (nextWord ushr 1) + 1
                } else {
                    holdingOne = (nextWord and 1) != 0
                    onesCount = nextWord ushr 1
                }
                holdingZero = !holdingOne
            }

            val med0 = (c.median[0] ushr 4) + 1
            val med1 = (c.median[1] ushr 4) + 1
            val med2 = (c.median[2] ushr 4) + 1

            val low: Int
            val high: Int
            when (onesCount) {
                0 -> {
                    low = 0
                    high = med0 - 1
                    c.median[0] -= ((c.median[0] + 126) ushr 7) * 2
                }
                1 -> {
                    low = med0
                    high = low + med1 - 1
                    c.median[0] += ((c.median[0] + 128) ushr 7) * 5
                    c.median[1] -= ((c.median[1] + 62) ushr 6) * 2
                }
                2 -> {
                    low = med0 + med1
                    high = low + med2 - 1
                    c.median[0] += ((c.median[0] + 128) ushr 7) * 5
                    c.median[1] += ((c.median[1] + 64) ushr 6) * 5
                    c.median[2] -= ((c.median[2] + 30) ushr 5) * 2
                }
                else -> {
                    low = med0 + med1 + (onesCount - 2) * med2
                    high = low + med2 - 1
                    c.median[0] += ((c.median[0] + 128) ushr 7) * 5
                    c.median[1] += ((c.median[1] + 64) ushr 6) * 5
                    c.median[2] += ((c.median[2] + 32) ushr 5) * 5
                }
            }

            val code = readCode(high - low)
            val magnitude = low + code
            return if (magnitude == 0) {
                0
            } else {
                if (reader.getBit() != 0) -magnitude else magnitude
            }
        }

        private fun readUnary(limit: Int): Int {
            var count = 0
            while (count < limit && reader.getBit() != 0) {
                count++
            }
            return count
        }

        private fun readCode(maxCode: Int): Int {
            if (maxCode <= 0) return 0
            val bitCount = 32 - Integer.numberOfLeadingZeros(maxCode)
            val extras = (1 shl bitCount) - maxCode - 1
            var res = reader.getBits(bitCount - 1)
            if (res >= extras) {
                res = (res shl 1) - extras + reader.getBit()
            }
            return res
        }
    }
}
