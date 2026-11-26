package com.demonlab.lune.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavPackDecoderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun isWavPackPathOrUri_detectsExtensionAndFormat() {
        assertTrue(WavPackDecoder.isWavPackPathOrUri("/sdcard/Music/track.wv", null, null))
        assertTrue(WavPackDecoder.isWavPackPathOrUri("/sdcard/Music/TRACK.WV", null, null))
        assertTrue(WavPackDecoder.isWavPackPathOrUri(null, null, "WV"))
        assertTrue(WavPackDecoder.isWavPackPathOrUri(null, null, "WavPack"))
    }

    @Test
    fun parseFile_readsHeaderAndApeV2TagsAndCoverArt() {
        val wvFile = tempFolder.newFile("sample.wv")

        // Build minimal 1-block mono 16-bit 48kHz WavPack file (48000 samples = 1000ms)
        val block = buildMinimalMonoZeroBlock(sampleRateIndex = 10 /* 48000 Hz */, totalSamples = 48000, blockSamples = 48000)
        val coverBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val apeTag = buildApeV2Tag(
            textItems = mapOf(
                "Title" to "Moonlight",
                "Artist" to "Lune Artist",
                "Album" to "Night Sky",
                "Genre" to "Ambient",
                "Track" to "3/12",
                "Lyrics" to "[00:01.00]Hello world"
            ),
            coverArtName = "cover.png",
            coverArtBytes = coverBytes
        )

        wvFile.writeBytes(block + apeTag)

        val meta = WavPackDecoder.parseFile(wvFile)
        assertNotNull(meta)
        meta!!
        assertEquals(48000, meta.sampleRate)
        assertEquals(16, meta.bitDepth)
        assertEquals(1, meta.channels)
        assertEquals(48000L, meta.totalSamples)
        assertEquals(1000L, meta.durationMs)
        assertEquals("Moonlight", meta.title)
        assertEquals("Lune Artist", meta.artist)
        assertEquals("Night Sky", meta.album)
        assertEquals("Ambient", meta.genre)
        assertEquals(3, meta.trackNumber)
        assertEquals("[00:01.00]Hello world", meta.lyrics)
        assertArrayEquals(coverBytes, meta.coverArt)
    }

    @Test
    fun decodeStreamToWav_producesValidRiffWavePcm() {
        val wvFile = tempFolder.newFile("silence.wv")
        val wavFile = tempFolder.newFile("silence.wav")

        // 4 samples of 16-bit mono silence at 44100 Hz, with trailing APEv2 tag containing "wvpk" bytes
        val block = buildMinimalMonoZeroBlock(sampleRateIndex = 9 /* 44100 Hz */, totalSamples = 4, blockSamples = 4)
        val apeWithWvpkInText = buildApeV2Tag(
            textItems = mapOf("Title" to "contains wvpk inside tag"),
            coverArtName = null,
            coverArtBytes = null
        )
        wvFile.writeBytes(block + apeWithWvpkInText)

        val ok = wvFile.inputStream().use { WavPackDecoder.decodeStreamToWav(it, wavFile) }
        assertTrue(ok)
        assertTrue(wavFile.length() == 44L + 4L * 2L)

        val wavBytes = wavFile.readBytes()
        assertEquals("RIFF", String(wavBytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wavBytes, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(wavBytes, 12, 4, Charsets.US_ASCII))
        assertEquals("data", String(wavBytes, 36, 4, Charsets.US_ASCII))

        val bb = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, bb.getShort(20).toInt()) // PCM format
        assertEquals(1, bb.getShort(22).toInt()) // 1 channel
        assertEquals(44100, bb.getInt(24)) // sample rate
        assertEquals(16, bb.getShort(34).toInt()) // bits per sample
        assertEquals(8, bb.getInt(40)) // data size = 4 samples * 2 bytes
    }

    @Test
    fun decodeStreamToWav_reconstructs32BitFloatHeaderAndSamples() {
        val wvFile = tempFolder.newFile("float32.wv")
        val wavFile = tempFolder.newFile("float32.wav")

        // 4 samples of 32-bit float mono silence at 48000 Hz (FLOAT_DATA = 0x80, BYTES_STORED = 3)
        // ID_FLOAT_INFO (0x08): 4 bytes (flags=0, shift=0, maxExp=127, normExp=127)
        // ID_FLOAT_DATA (0x0B): 2 bytes (1 word) of 0x00
        val subBlocks = byteArrayOf(
            0x05, 0x03, 0, 0, 0, 0, 0, 0,
            0x08, 0x02, 0x00, 0x00, 0x7F, 0x7F,
            0x0A, 0x02, 0, 0, 0, 0,
            0x0B, 0x01, 0, 0
        )
        val ckSize = 24 + subBlocks.size
        val flags = 3 or 0x4 or 0x80 or 0x800 or 0x1000 or (10 shl 23)
        val hdr = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        hdr.put("wvpk".toByteArray(Charsets.US_ASCII))
        hdr.putInt(ckSize)
        hdr.putShort(0x0410)
        hdr.put(0)
        hdr.put(0)
        hdr.putInt(4)
        hdr.putInt(0)
        hdr.putInt(4)
        hdr.putInt(flags)
        hdr.putInt(0)
        wvFile.writeBytes(hdr.array() + subBlocks)

        val ok = wvFile.inputStream().use { WavPackDecoder.decodeStreamToWav(it, wavFile) }
        assertTrue(ok)
        val wavBytes = wavFile.readBytes()
        val bb = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(3, bb.getShort(20).toInt()) // IEEE Float format = 3
        assertEquals(32, bb.getShort(34).toInt()) // 32 bits per sample
        assertEquals(16, bb.getInt(40)) // 4 samples * 4 bytes
        assertEquals(0.0f, bb.getFloat(44))
    }

    private fun buildMinimalMonoZeroBlock(sampleRateIndex: Int, totalSamples: Int, blockSamples: Int): ByteArray {
        // Sub-block 1: ID_ENTROPY_VARS (0x05), 3 uint16s = 6 bytes (3 words), all 0
        // Sub-block 2: ID_WV_BITSTREAM (0x0A), 4 bytes (2 words) of 0x00 (decodes zero-run to all 0s)
        val subBlocks = byteArrayOf(
            0x05, 0x03, 0, 0, 0, 0, 0, 0,
            0x0A, 0x02, 0, 0, 0, 0
        )
        val ckSize = 24 + subBlocks.size
        // flags: 16-bit (BYTES_STORED=1), MONO_FLAG (0x4), INITIAL_BLOCK (0x800), FINAL_BLOCK (0x1000), SRATE
        val flags = 1 or 0x4 or 0x800 or 0x1000 or (sampleRateIndex shl 23)

        val hdr = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        hdr.put("wvpk".toByteArray(Charsets.US_ASCII))
        hdr.putInt(ckSize)
        hdr.putShort(0x0410) // version 4.16
        hdr.put(0) // block_index_u8
        hdr.put(0) // total_samples_u8
        hdr.putInt(totalSamples)
        hdr.putInt(0) // block_index
        hdr.putInt(blockSamples)
        hdr.putInt(flags)
        hdr.putInt(0) // crc
        return hdr.array() + subBlocks
    }

    private fun buildApeV2Tag(
        textItems: Map<String, String>,
        coverArtName: String?,
        coverArtBytes: ByteArray?
    ): ByteArray {
        val itemsOut = ByteArrayOutputStream()
        var count = 0

        for ((k, v) in textItems) {
            val valBytes = v.toByteArray(Charsets.UTF_8)
            val keyBytes = k.toByteArray(Charsets.UTF_8)
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            bb.putInt(valBytes.size)
            bb.putInt(0) // UTF-8 text item
            itemsOut.write(bb.array())
            itemsOut.write(keyBytes)
            itemsOut.write(0)
            itemsOut.write(valBytes)
            count++
        }

        if (coverArtName != null && coverArtBytes != null) {
            val nameBytes = coverArtName.toByteArray(Charsets.UTF_8)
            val totalValLen = nameBytes.size + 1 + coverArtBytes.size
            val keyBytes = "Cover Art (Front)".toByteArray(Charsets.UTF_8)
            val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            bb.putInt(totalValLen)
            bb.putInt(1 shl 1) // binary item
            itemsOut.write(bb.array())
            itemsOut.write(keyBytes)
            itemsOut.write(0)
            itemsOut.write(nameBytes)
            itemsOut.write(0)
            itemsOut.write(coverArtBytes)
            count++
        }

        val itemsBytes = itemsOut.toByteArray()
        val tagSize = itemsBytes.size + 32 // includes footer

        val footer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        footer.put("APETAGEX".toByteArray(Charsets.US_ASCII))
        footer.putInt(2000)
        footer.putInt(tagSize)
        footer.putInt(count)
        footer.putInt(0) // flags
        footer.putLong(0) // reserved

        return itemsBytes + footer.array()
    }
}
