package com.hwcompany.fortune_index.investmentindex

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvestmentIndexServiceTest {
    private val service = InvestmentIndexService(
        marketIndexQuoteClient = object : MarketIndexQuoteClient {
            override fun getChangeRate(ticker: String): Double =
                when (ticker) {
                    "^KS11" -> 2.5
                    "^IXIC" -> -1.2
                    else -> 0.0
                }
        }
    )

    @Test
    fun `KST 장중에는 KOSPI를 선택한다`() {
        val response = service.getInvestmentIndex(
            ZonedDateTime.of(2026, 3, 16, 10, 0, 0, 0, ZONE_ID)
        )

        assertEquals("KOSPI", response.detail.selectedMarket)
        assertEquals(75, response.detail.marketScore)
        assertEquals(2.5, response.detail.marketRawValue)
        assertEquals(
            (response.detail.marketScore * 0.70 + response.detail.sajuScore * 0.15 + response.detail.tarotScore * 0.15).toInt(),
            response.totalScore
        )
    }

    @Test
    fun `KST 야간 장중에는 NASDAQ을 선택한다`() {
        val response = service.getInvestmentIndex(
            ZonedDateTime.of(2026, 3, 16, 23, 0, 0, 0, ZONE_ID)
        )

        assertEquals("NASDAQ", response.detail.selectedMarket)
        assertEquals(38, response.detail.marketScore)
        assertEquals(-1.2, response.detail.marketRawValue)
        assertTrue(response.detail.dailyGanji.isNotBlank())
        assertTrue(response.detail.tarotCardName.isNotBlank())
    }

    @Test
    fun `비활성 시간대에는 직전 세션 시장을 기준으로 산출한다`() {
        val response = service.getInvestmentIndex(
            ZonedDateTime.of(2026, 3, 16, 6, 0, 0, 0, ZONE_ID)
        )

        assertEquals("NASDAQ", response.detail.selectedMarket)
    }

    companion object {
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
