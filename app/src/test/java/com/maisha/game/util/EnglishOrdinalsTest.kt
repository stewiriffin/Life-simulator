package com.maisha.game.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishOrdinalsTest {

    @Test
    fun englishOrdinals_useCorrectSuffixes() {
        assertEquals("1st", EnglishOrdinals.format(1))
        assertEquals("2nd", EnglishOrdinals.format(2))
        assertEquals("3rd", EnglishOrdinals.format(3))
        assertEquals("4th", EnglishOrdinals.format(4))
        assertEquals("11th", EnglishOrdinals.format(11))
        assertEquals("12th", EnglishOrdinals.format(12))
        assertEquals("13th", EnglishOrdinals.format(13))
        assertEquals("21st", EnglishOrdinals.format(21))
        assertEquals("22nd", EnglishOrdinals.format(22))
        assertEquals("23rd", EnglishOrdinals.format(23))
        assertEquals("111th", EnglishOrdinals.format(111))
    }
}
