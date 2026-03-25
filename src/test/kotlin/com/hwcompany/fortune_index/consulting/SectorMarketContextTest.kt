package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.market.FundamentalSnapshot
import com.hwcompany.fortune_index.market.MarketDataProvider
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.TradingSnapshot
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import java.math.BigDecimal
import java.time.LocalDate

class SectorMarketContextTest {
    @Test
    fun `sector market context exposes latest narrative and fundamentals`() {
        val stock = StockInfo(
            ticker = "005930",
            currentPrice = BigDecimal("71200"),
            changeRate = BigDecimal("4.10"),
            sector = "SEMICONDUCTOR",
            source = MarketDataProvider.KIS,
            marketDataAsOf = LocalDate.of(2026, 3, 25),
            tradingSnapshot = TradingSnapshot(
                openPrice = BigDecimal("70000"),
                highPrice = BigDecimal("71500"),
                lowPrice = BigDecimal("69800"),
                volume = 12345678L
            ),
            fundamentals = FundamentalSnapshot(
                marketCap = BigDecimal("520000000000000"),
                trailingPe = BigDecimal("14.2"),
                priceToBook = BigDecimal("1.7"),
                operatingMarginRatio = BigDecimal("0.22"),
                returnOnEquityRatio = BigDecimal("0.18")
            ),
            marketNarrative = "2026-03-25 기준 반도체 섹터는 수급이 강하게 붙은 상태입니다."
        )

        val context = stock.toSectorMarketContext()

        assertEquals("2026-03-25", context.marketDataAsOf)
        assertContains(context.marketNarrative, "반도체")
        assertContains(context.tradingSignal, "시가 70000")
        assertContains(context.tradingSignal, "거래량 12,345,678")
        assertContains(context.fundamentalSignal, "PER 14.2배")
        assertContains(context.fundamentalSignal, "영업이익률 22%")
        assertContains(context.fundamentalSignal, "ROE 18%")
    }
}
