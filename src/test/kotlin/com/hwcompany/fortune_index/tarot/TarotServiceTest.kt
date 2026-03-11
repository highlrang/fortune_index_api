package com.hwcompany.fortune_index.tarot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TarotServiceTest {
    private val tarotService = TarotService()

    @Test
    fun `index가 있으면 해당 위치 카드를 반환한다`() {
        val result = tarotService.drawCard(0)

        assertEquals(0, result.index)
        assertEquals(TarotCard.THE_FOOL, result.card)
    }

    @Test
    fun `index가 null이면 셔플 후 카드 한 장을 반환한다`() {
        val result = tarotService.drawCard(null)

        assertEquals(true, result.index in TarotCard.entries.indices)
        assertEquals(TarotCard.entries[result.index], result.card)
    }

    @Test
    fun `범위를 벗어난 index는 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            tarotService.drawCard(78)
        }
    }
}
