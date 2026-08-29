package com.demonlab.lune.tools

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object CharsetUtils {

    private val GB18030_CHARSET by lazy {
        try {
            Charset.forName("GB18030")
        } catch (_: Exception) {
            try {
                Charset.forName("GBK")
            } catch (_: Exception) {
                Charsets.UTF_8
            }
        }
    }

    private val SHIFT_JIS_CHARSET by lazy {
        try {
            Charset.forName("Shift_JIS")
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    private val BIG5_CHARSET by lazy {
        try {
            Charset.forName("Big5")
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    /**
     * Decodifica un array de bytes detectando automáticamente si es UTF-8 (con o sin BOM),
     * GB18030 / GBK (chino), Shift-JIS (japonés), Big5 (chino tradicional) o ISO-8859-1.
     */
    fun decodeBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // 1. Detectar BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        // 2. Intentar decodificar como UTF-8 estricto
        try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // No es UTF-8 valido, probar encodings asiaticos comunes
        }

        // 3. Probar GB18030 (cubre GBK y GB2312)
        try {
            val decoder = GB18030_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continuar
        }

        // 4. Probar Shift-JIS
        try {
            val decoder = SHIFT_JIS_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continuar
        }

        // 5. Probar Big5
        try {
            val decoder = BIG5_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continuar
        }

        // 6. Fallback a decodificacion permisiva UTF-8 o ISO-8859-1
        val fallback = String(bytes, Charsets.UTF_8)
        return sanitizeText(fallback)
    }

    /**
     * Lee un archivo de texto detectando automaticamente la codificacion de caracteres.
     */
    fun readText(file: File): String {
        return try {
            val bytes = file.readBytes()
            decodeBytes(bytes)
        } catch (_: Exception) {
            file.readText()
        }
    }

    /**
     * Corrige casos comunes de mojibake, como:
     * - "R路I路O路T" -> "R·I·O·T" (cuando el punto medio U+00B7 fue interpretado erroneamente en GBK)
     * - Mojibake de ISO-8859-1 que originalmente era UTF-8 o GBK
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return input ?: ""
        var text = input

        // Caso reportado: "R路I路O路T" -> "R·I·O·T"
        // En GBK, 0xC2 0xB7 (que es "·" en UTF-8) se decodifica como "路" (lu).
        // Cuando aparece "路" separando letras o numeros latinos, es inequivocamente el punto medio "·".
        text = text.replace(Regex("(?<=[A-Za-z0-9])\\s*路\\s*(?=[A-Za-z0-9])"), "·")

        // Reparacion de mojibake clasico ISO-8859-1 -> UTF-8
        if (looksLikeIsoUtf8Mojibake(text)) {
            try {
                val isoBytes = text.toByteArray(Charsets.ISO_8859_1)
                val candidateUtf8 = String(isoBytes, Charsets.UTF_8)
                if (candidateUtf8.length < text.length) {
                    text = candidateUtf8
                }
            } catch (_: Exception) {
                // Mantener original
            }
        }

        return text
    }

    private fun looksLikeIsoUtf8Mojibake(text: String): Boolean {
        return text.contains("Ã") || text.contains("Â·") || text.contains("Â")
    }
}
