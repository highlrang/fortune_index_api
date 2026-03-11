package com.hwcompany.fortune_index.market

import java.math.BigDecimal

data class StockInfo(
    val ticker: String,
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val sector: String,
    val source: MarketDataProvider,
    val fallback: Boolean = false
)
