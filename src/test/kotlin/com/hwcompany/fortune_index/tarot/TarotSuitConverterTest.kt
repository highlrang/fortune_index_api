package com.hwcompany.fortune_index.tarot

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class TarotSuitConverterTest {
    private val converter = TarotSuitConverter()

    @Test
    fun `converter normalizes legacy pentacle value`() {
        assertEquals(TarotSuit.PENTACLES, converter.convertToEntityAttribute("PENTACLE"))
        assertEquals(TarotSuit.PENTACLES, converter.convertToEntityAttribute("PENTACLES"))
    }

    @Test
    fun `converter normalizes singular legacy suit values`() {
        assertEquals(TarotSuit.WANDS, converter.convertToEntityAttribute("WAND"))
        assertEquals(TarotSuit.CUPS, converter.convertToEntityAttribute("CUP"))
        assertEquals(TarotSuit.SWORDS, converter.convertToEntityAttribute("SWORD"))
    }

    @Test
    fun `converter returns null for blank input`() {
        assertNull(converter.convertToEntityAttribute(null))
        assertNull(converter.convertToEntityAttribute(" "))
    }
}
