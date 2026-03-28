package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import java.time.LocalDate

data class StockMarketSnapshot(
    val stockCode: String,
    val stockName: String,
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val sectorName: String?,
    val marketNarrative: String,
    val marketDataAsOf: LocalDate = LocalDate.now(),
    val tradingSnapshot: TradingSnapshot = TradingSnapshot(),
    val fundamentals: FundamentalSnapshot = FundamentalSnapshot()
)

data class StockMarketRequest(
    val stockCode: String,
    val stockName: String,
    val sectorOutlook: String? = null
)
