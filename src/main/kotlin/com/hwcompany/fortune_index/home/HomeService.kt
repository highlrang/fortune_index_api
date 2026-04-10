package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.investmentindex.InvestmentIndexService
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

        return HomeIndexChartResponse(
            status = HomeDataStatus.UNAVAILABLE,
            indexCode = index.code,
            label = index.label,
            period = chartPeriod.value,
            asOf = now.withZoneSameInstant(index.zoneId),
            points = emptyList()
        )
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
            totalScore >= 80 -> "마음이 비교적 가볍고 흐름이 잘 풀리는 날"
            totalScore >= 65 -> "서두르지 않고 차분히 살피기 좋은 날"
            totalScore >= 50 -> "조용히 상황을 지켜보며 감을 익히기 좋은 날"
            totalScore >= 35 -> "한 번 더 생각하고 천천히 움직이는 편이 좋은 날"
            else -> "무리하지 말고 마음부터 쉬게 해 주는 편이 좋은 날"
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
