package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.investmentindex.InvestmentIndexService
import com.hwcompany.fortune_index.investmentindex.SupportedMarket
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.springframework.stereotype.Service

@Service
class HomeService(
    private val investmentIndexService: InvestmentIndexService
) {
    fun getSummary(now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): HomeSummaryResponse {
        val nowInSeoul = now.withZoneSameInstant(SEOUL_ZONE_ID)
        val nowInNewYork = nowInSeoul.withZoneSameInstant(NEW_YORK_ZONE_ID)
        val indexResponse = investmentIndexService.getInvestmentIndex(nowInSeoul)
        val selectedMarket = investmentIndexService.resolveMarket(nowInSeoul)
        val marketSnapshot = fetchMarketSnapshot(selectedMarket, nowInSeoul, indexResponse.detail.marketScore)
        val domesticFetched = fetchDomesticStocks()
        val foreignFetched = fetchForeignStocks()
        val domesticStatus = stockSectionStatus(
            items = domesticFetched,
            zoneId = SEOUL_ZONE_ID,
            now = nowInSeoul
        )
        val foreignStatus = stockSectionStatus(
            items = foreignFetched,
            zoneId = NEW_YORK_ZONE_ID,
            now = nowInNewYork
        )
        val domesticStocks = domesticFetched
        val foreignStocks = foreignFetched

        return HomeSummaryResponse(
            investmentIndex = HomeInvestmentIndexResponse(
                status = HomeDataStatus.OK,
                totalScore = indexResponse.totalScore,
                summary = summarize(indexResponse.totalScore),
                market = marketSnapshot,
                fortune = HomeFortuneSnapshot(
                    dailyGanji = indexResponse.detail.dailyGanji,
                    score = indexResponse.detail.sajuScore
                ),
                tarot = HomeTarotSnapshot(
                    cardName = indexResponse.detail.tarotCardName,
                    score = indexResponse.detail.tarotScore
                )
            ),
            stocks = HomeStocksResponse(
                domesticStatus = domesticStatus,
                domesticAsOf = nowInSeoul,
                domestic = domesticStocks,
                foreignStatus = foreignStatus,
                foreignAsOf = nowInSeoul,
                foreign = foreignStocks
            )
        )
    }

    fun getIndexChart(indexCode: String, period: String, now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): HomeIndexChartResponse {
        val index = HomeSupportedIndex.from(indexCode)
        val chartPeriod = HomeChartPeriod.from(period)
        val nowInMarketZone = now.withZoneSameInstant(index.zoneId)
        val marketClosed = isMarketLikelyClosed(index, nowInMarketZone)
        val status = if (marketClosed) HomeDataStatus.MARKET_CLOSED else HomeDataStatus.UNAVAILABLE

        return HomeIndexChartResponse(
            status = status,
            indexCode = index.code,
            label = index.label,
            period = chartPeriod.value,
            asOf = now.withZoneSameInstant(index.zoneId),
            points = emptyList()
        )
    }

    private fun fetchMarketSnapshot(
        market: SupportedMarket,
        now: ZonedDateTime,
        marketScore: Int
    ): HomeMarketSnapshot {
        val homeIndex = when (market) {
            SupportedMarket.KOSPI -> HomeSupportedIndex.KOSPI
            SupportedMarket.NASDAQ -> HomeSupportedIndex.NASDAQ
        }
        val marketClosed = isMarketLikelyClosed(homeIndex, now.withZoneSameInstant(homeIndex.zoneId))

        return when (market) {
            SupportedMarket.KOSPI,
            SupportedMarket.NASDAQ -> HomeMarketSnapshot(
                status = if (marketClosed) HomeDataStatus.MARKET_CLOSED else HomeDataStatus.UNAVAILABLE,
                code = homeIndex.code,
                label = homeIndex.label,
                value = 0.0,
                score = marketScore,
                change = 0.0,
                changeRate = 0.0,
                asOf = now.withZoneSameInstant(homeIndex.zoneId)
            )
        }
    }

    private fun fetchDomesticStocks(): List<HomeStockItem> = emptyList()

    private fun fetchForeignStocks(): List<HomeStockItem> = emptyList()

    private fun stockSectionStatus(
        items: List<HomeStockItem>,
        zoneId: ZoneId,
        now: ZonedDateTime
    ): HomeDataStatus =
        when {
            isMarketLikelyClosed(zoneId = zoneId, now = now) -> HomeDataStatus.MARKET_CLOSED
            items.isNotEmpty() -> HomeDataStatus.OK
            else -> HomeDataStatus.UNAVAILABLE
        }

    private fun isMarketLikelyClosed(index: HomeSupportedIndex, now: ZonedDateTime): Boolean =
        isMarketLikelyClosed(zoneId = index.zoneId, now = now)

    private fun isMarketLikelyClosed(zoneId: ZoneId, now: ZonedDateTime): Boolean {
        if (isWeekend(now.dayOfWeek)) {
            return true
        }

        val localTime = now.toLocalTime()
        return when (zoneId) {
            SEOUL_ZONE_ID -> localTime.isBefore(KOREA_MARKET_OPEN) || localTime.isAfter(KOREA_MARKET_CLOSE)
            NEW_YORK_ZONE_ID -> localTime.isBefore(US_MARKET_OPEN) || localTime.isAfter(US_MARKET_CLOSE)
            else -> false
        }
    }

    private fun summarize(totalScore: Int): String =
        when {
            totalScore >= 80 -> "매수하기 좋은 날"
            totalScore >= 65 -> "분할 매수를 보기 좋은 날"
            totalScore >= 50 -> "관망하며 확인할 날"
            totalScore >= 35 -> "신중하게 접근할 날"
            else -> "보수적으로 쉬어갈 날"
        }
    private fun isWeekend(dayOfWeek: DayOfWeek): Boolean =
        dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val NEW_YORK_ZONE_ID: ZoneId = ZoneId.of("America/New_York")
        private val KOREA_MARKET_OPEN: LocalTime = LocalTime.of(9, 0)
        private val KOREA_MARKET_CLOSE: LocalTime = LocalTime.of(15, 30)
        private val US_MARKET_OPEN: LocalTime = LocalTime.of(9, 30)
        private val US_MARKET_CLOSE: LocalTime = LocalTime.of(16, 0)
    }
}
