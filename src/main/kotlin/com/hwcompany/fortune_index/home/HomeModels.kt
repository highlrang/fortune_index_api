package com.hwcompany.fortune_index.home

import java.time.ZoneId
import java.time.ZonedDateTime

data class HomeSummaryResponse(
    val investmentIndex: HomeInvestmentIndexResponse,
    val stocks: HomeStocksResponse
)

data class HomeInvestmentIndexResponse(
    val status: HomeDataStatus,
    val totalScore: Int,
    val summary: String,
    val fortune: HomeFortuneSnapshot,
    val tarot: HomeTarotSnapshot
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

enum class HomeChartPeriod(
    val value: String
) {
    ONE_DAY("1D"),
    ONE_WEEK("1W"),
    ONE_MONTH("1M");

    companion object {
        fun from(raw: String): HomeChartPeriod =
            entries.firstOrNull { it.value.equals(raw.trim(), ignoreCase = true) } ?: ONE_DAY
    }
}

enum class HomeSupportedIndex(
    val code: String,
    val label: String,
    val ticker: String,
    val zoneId: ZoneId
) {
    KOSPI("KOSPI", "코스피", "^KS11", ZoneId.of("Asia/Seoul")),
    NASDAQ("NASDAQ", "나스닥", "^IXIC", ZoneId.of("America/New_York"));

    companion object {
        fun from(code: String): HomeSupportedIndex =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: KOSPI
    }
}
