package com.hwcompany.fortune_index.tarot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TarotServiceTest {
    private val tarotService = TarotService()

    @Test
    fun `index가 있으면 해당 위치 카드를 반환한다`() {
        val result = tarotService.drawCard(0)

        assertEquals(0, result.index)
        assertEquals(DEFAULT_TAROT_DECK_VERSION_ID, result.card.deckVersionId)
        assertEquals(TarotCard.THE_FOOL.code, result.card.code)
        assertEquals("The Fool", result.card.name)
    }

    @Test
    fun `index가 null이면 셔플 후 카드 한 장을 반환한다`() {
        val result = tarotService.drawCard(null)

        assertTrue(result.index in TarotCard.entries.indices)
        assertEquals(TarotCard.entries[result.index].code, result.card.code)
    }

    @Test
    fun `범위를 벗어난 index는 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            tarotService.drawCard(78)
        }
    }
}
