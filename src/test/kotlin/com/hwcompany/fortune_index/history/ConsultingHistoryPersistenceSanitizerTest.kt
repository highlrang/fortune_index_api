package com.hwcompany.fortune_index.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsultingHistoryPersistenceSanitizerTest {
    @Test
    fun `stock name is trimmed and truncated to column length`() {
        val value = "  ${"A".repeat(300)}  "

        val sanitized = ConsultingHistoryPersistenceSanitizer.stockName(value)

        assertEquals(100, sanitized.length)
        assertEquals("A".repeat(100), sanitized)
    }

    @Test
    fun `ticker is trimmed and truncated to column length`() {
        val value = "  NASDAQ-LONG-TICKER-123456  "

        val sanitized = ConsultingHistoryPersistenceSanitizer.ticker(value)

        assertEquals(20, sanitized.length)
        assertEquals("NASDAQ-LONG-TICKER-1", sanitized)
    }

    @Test
    fun `nullable text drops blank content`() {
        assertNull(ConsultingHistoryPersistenceSanitizer.nullableText("   "))
        assertEquals("memo", ConsultingHistoryPersistenceSanitizer.nullableText(" memo "))
    }

    @Test
    fun `saju summary is trimmed and truncated to column length`() {
        val value = "  ${"B".repeat(600)}  "

        val sanitized = ConsultingHistoryPersistenceSanitizer.sajuSummary(value)

        assertEquals(500, sanitized.length)
        assertEquals("B".repeat(500), sanitized)
    }

    @Test
    fun `tarot summary is trimmed and truncated to column length`() {
        val value = "  ${"C".repeat(350)}  "

        val sanitized = ConsultingHistoryPersistenceSanitizer.tarotSummary(value)

        assertEquals(300, sanitized.length)
        assertEquals("C".repeat(300), sanitized)
    }
}
