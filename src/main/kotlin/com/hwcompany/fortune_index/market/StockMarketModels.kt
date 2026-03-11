package com.hwcompany.fortune_index.market

import java.math.BigDecimal

data class StockMarketSnapshot(
    val stockCode: String,
    val stockName: String,
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val sectorName: String?,
    val marketNarrative: String
)

data class StockMarketRequest(
    val stockCode: String,
    val stockName: String,
    val sectorOutlook: String? = null
)
