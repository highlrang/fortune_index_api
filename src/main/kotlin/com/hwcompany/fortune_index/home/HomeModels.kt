package com.hwcompany.fortune_index.home

import java.time.ZonedDateTime

data class HomeSummaryResponse(
    val investmentIndex: HomeInvestmentIndexResponse,
    val stocks: HomeStocksResponse
)

data class HomeInvestmentIndexResponse(
    val status: HomeDataStatus,
    val totalScore: Int,
    val summary: String,
    val market: HomeMarketSnapshot,
    val fortune: HomeFortuneSnapshot,
    val tarot: HomeTarotSnapshot
)

data class HomeMarketSnapshot(
    val status: HomeDataStatus,
    val code: String,
    val label: String,
    val value: Double,
    val score: Int,
    val change: Double,
    val changeRate: Double,
    val asOf: ZonedDateTime
)

data class HomeFortuneSnapshot(
    val dailyGanji: String,
    val score: Int
)

data class HomeTarotSnapshot(
    val cardName: String,
    val score: Int
)

data class HomeStocksResponse(
    val domesticStatus: HomeDataStatus,
    val domesticAsOf: ZonedDateTime,
    val domestic: List<HomeStockItem>,
    val foreignStatus: HomeDataStatus,
    val foreignAsOf: ZonedDateTime,
    val foreign: List<HomeStockItem>
)

data class HomeStockItem(
    val ticker: String,
    val name: String,
    val price: Double,
    val changeRate: Double,
    val currency: String
)

data class HomeIndexChartResponse(
    val status: HomeDataStatus,
    val indexCode: String,
    val label: String,
    val period: String,
    val asOf: ZonedDateTime,
    val points: List<HomeIndexChartPoint>
)

data class HomeIndexChartPoint(
    val time: String,
    val value: Double
)

enum class HomeDataStatus {
    OK,
    MARKET_CLOSED,
    UNAVAILABLE
}
