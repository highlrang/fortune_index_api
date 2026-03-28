package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import java.time.LocalDate

data class StockInfo(
    val ticker: String,
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val sector: String,
    val source: MarketDataProvider,
    val fallback: Boolean = false,
    val marketDataAsOf: LocalDate = LocalDate.now(),
    val tradingSnapshot: TradingSnapshot = TradingSnapshot(),
    val fundamentals: FundamentalSnapshot = FundamentalSnapshot(),
    val marketNarrative: String = ""
)

data class TradingSnapshot(
    val openPrice: BigDecimal? = null,
    val highPrice: BigDecimal? = null,
    val lowPrice: BigDecimal? = null,
    val volume: Long? = null
)

data class FundamentalSnapshot(
    val marketCap: BigDecimal? = null,
    val trailingPe: BigDecimal? = null,
    val forwardPe: BigDecimal? = null,
    val priceToBook: BigDecimal? = null,
    val eps: BigDecimal? = null,
    val bps: BigDecimal? = null,
    val operatingMarginRatio: BigDecimal? = null,
    val returnOnEquityRatio: BigDecimal? = null
)

fun StockInfo.toAiPayload(): Map<String, Any?> =
    linkedMapOf(
        "currentPrice" to currentPrice,
        "changeRate" to changeRate,
        "sector" to sector,
        "fallback" to fallback,
        "marketDataAsOf" to marketDataAsOf,
        "marketNarrative" to marketNarrative,
        "tradingSnapshot" to tradingSnapshot,
        "fundamentals" to fundamentals
    )
