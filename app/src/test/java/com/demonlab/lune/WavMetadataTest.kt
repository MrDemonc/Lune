package com.demonlab.lune

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavMetadataTest {

    private fun createDummyWavFile(file: File) {
        // Minimal valid PCM WAV (44.1kHz, 16-bit, mono, 1 second of silence)
        val sampleRate = 44100
        val channels = 1
        val bitsPerSample = 16
        val dataSize = sampleRate * channels * (bitsPerSample / 8) // 1 second
        val chunkSize = 36 + dataSize

        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(chunkSize)
        buffer.put("WAVE".toByteArray())
        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * (bitsPerSample / 8)) // ByteRate
        buffer.putShort((channels * (bitsPerSample / 8)).toShort()) // BlockAlign
        buffer.putShort(bitsPerSample.toShort())
        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        // fill with silence
        for (i in 0 until dataSize) {
            buffer.put(0.toByte())
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
    }

    @Test
    fun testReadAndWriteWavMetadata() {
        val tempFile = File.createTempFile("test_audio", ".wav")
        try {
            createDummyWavFile(tempFile)

            // Read with jaudiotagger
            val audioFile = AudioFileIO.read(tempFile)
            assertNotNull(audioFile)
            assertEquals(44100, audioFile.audioHeader.sampleRateAsNumber)

            // Create tag & write
            var tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, "Test WAV Title")
            tag.setField(FieldKey.ARTIST, "Test WAV Artist")
            tag.setField(FieldKey.ALBUM, "Test WAV Album")
            tag.setField(FieldKey.GENRE, "Acoustic")
            tag.setField(FieldKey.TRACK, "5")
            audioFile.commit()

            // Re-read file
            val reloaded = AudioFileIO.read(tempFile)
            val readTag = reloaded.tag
            assertNotNull(readTag)

            val title = readTag.getFirst(FieldKey.TITLE)
            val artist = readTag.getFirst(FieldKey.ARTIST)
            val album = readTag.getFirst(FieldKey.ALBUM)
            val genre = readTag.getFirst(FieldKey.GENRE)
            val track = readTag.getFirst(FieldKey.TRACK)

            assertEquals("Test WAV Title", title)
            assertEquals("Test WAV Artist", artist)
            assertEquals("Test WAV Album", album)
            assertEquals("Acoustic", genre)
            assertEquals("5", track)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testArtworkInWav() {
        val tempFile = File.createTempFile("test_artwork", ".wav")
        try {
            createDummyWavFile(tempFile)

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault
            val dummyJpegBytes = byteArrayOf(-1, -40, -1, -32, 0, 16, 74, 70, 73, 70) // FFD8FFE0 0010JFIF header
            val artwork = org.jaudiotagger.tag.images.StandardArtwork()
            artwork.binaryData = dummyJpegBytes
            artwork.mimeType = "image/jpeg"
            tag.setField(artwork)
            audioFile.commit()

            val reloaded = AudioFileIO.read(tempFile)
            val readArtwork = reloaded.tag?.firstArtwork
            assertNotNull(readArtwork)
            assertEquals("image/jpeg", readArtwork?.mimeType)
            assertEquals(10, readArtwork?.binaryData?.size)
        } finally {
            tempFile.delete()
        }
    }
}
