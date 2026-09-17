package com.demonlab.lune

import com.demonlab.lune.tools.CharsetUtils
import com.demonlab.lune.tools.normalizeForSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharsetUtilsTest {

    @Test
    fun testDiacriticInsensitiveSearchNormalization() {
        // Tiësto / Tiesto
        assertEquals("tiesto", "Tiësto".normalizeForSearch())
        assertEquals("tiesto", "Tiesto".normalizeForSearch())

        // Mötley Crüe / Motley Crue
        assertEquals("motley crue", "Mötley Crüe".normalizeForSearch())
        assertEquals("motley crue", "Motley Crue".normalizeForSearch())

        // Accents: Spanish, French, Portuguese, etc.
        assertEquals("cancion", "canción".normalizeForSearch())
        assertEquals("beyonce", "Beyoncé".normalizeForSearch())
        assertEquals("sigur ros", "Sigur Rós".normalizeForSearch())
        assertEquals("bjork", "Björk".normalizeForSearch())
        assertEquals("dvorak", "Dvořák".normalizeForSearch())
        assertEquals("zoe", "Zoë".normalizeForSearch())

        // Non-decomposing characters: Nordic, Polish, German, etc.
        assertEquals("mo", "Mø".normalizeForSearch())
        assertEquals("lodz", "Łódź".normalizeForSearch())
        assertEquals("kaelan mikla", "Kælan Mikla".normalizeForSearch())
        assertEquals("strasse", "Straße".normalizeForSearch())

        // Matching checks
        val target = "Tiësto - Adagio for Strings".normalizeForSearch()
        val query1 = "Tiesto".normalizeForSearch()
        val query2 = "tiësto".normalizeForSearch()
        val query3 = "adagio".normalizeForSearch()

        assertTrue(target.contains(query1))
        assertTrue(target.contains(query2))
        assertTrue(target.contains(query3))
    }

    @Test
    fun testBlankAndNullHandling() {
        assertEquals("", "".normalizeForSearch())
        assertEquals("", "   ".normalizeForSearch())
        assertEquals("", (null as String?).normalizeForSearch())
    }
}
