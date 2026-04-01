package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.investmentindex.InvestmentIndexService
import com.hwcompany.fortune_index.investmentindex.SupportedMarket
import com.hwcompany.fortune_index.market.KisDomesticIndexClient
import com.hwcompany.fortune_index.market.KisOverseasMarket
import com.hwcompany.fortune_index.market.KisOverseasPriceClient
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.YahooFinanceClient
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HomeService(
    private val investmentIndexService: InvestmentIndexService,
    private val homeDomesticRankingClient: HomeDomesticRankingClient,
    private val kisDomesticIndexClient: KisDomesticIndexClient,
    private val kisOverseasPriceClient: KisOverseasPriceClient,
    private val yahooFinanceClient: YahooFinanceClient,
    private val homeMarketChartClient: HomeMarketChartClient
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
        val samples = homeMarketChartClient.fetchChart(index, chartPeriod)
        val marketClosed = isMarketLikelyClosed(index, nowInMarketZone)
        val points = samples.map { sample ->
            HomeIndexChartPoint(
                time = sample.timestamp.toLocalTime().withSecond(0).withNano(0).toString(),
                value = sample.value
            )
        }
        val normalizedPoints = if (marketClosed && chartPeriod == HomeChartPeriod.ONE_DAY) emptyList() else points

        val status = when {
            marketClosed && chartPeriod == HomeChartPeriod.ONE_DAY -> HomeDataStatus.MARKET_CLOSED
            normalizedPoints.isNotEmpty() -> HomeDataStatus.OK
            marketClosed -> HomeDataStatus.MARKET_CLOSED
            else -> HomeDataStatus.UNAVAILABLE
        }

        return HomeIndexChartResponse(
            status = status,
            indexCode = index.code,
            label = index.label,
            period = chartPeriod.value,
            asOf = samples.lastOrNull()?.timestamp ?: now.withZoneSameInstant(index.zoneId),
            points = normalizedPoints
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
            SupportedMarket.KOSPI -> {
                val snapshot = runCatching { kisDomesticIndexClient.fetchSnapshot(KOSPI_INDEX_CODE) }
                    .onFailure { ex -> logger.warn("Failed to fetch KOSPI home index from KIS", ex) }
                    .getOrNull()
                val status = when {
                    marketClosed -> HomeDataStatus.MARKET_CLOSED
                    snapshot != null -> HomeDataStatus.OK
                    else -> HomeDataStatus.UNAVAILABLE
                }

                HomeMarketSnapshot(
                    status = status,
                    code = homeIndex.code,
                    label = homeIndex.label,
                    value = snapshot?.currentValue ?: 0.0,
                    score = marketScore,
                    change = snapshot?.change ?: 0.0,
                    changeRate = snapshot?.changeRate ?: 0.0,
                    asOf = snapshot?.asOf ?: now.withZoneSameInstant(homeIndex.zoneId)
                )
            }

            SupportedMarket.NASDAQ -> {
                val quote = runCatching { yahooFinanceClient.getStockInfo(homeIndex.ticker) }
                    .onFailure { ex -> logger.warn("Failed to fetch NASDAQ home index from Yahoo", ex) }
                    .getOrNull()
                val status = when {
                    marketClosed -> HomeDataStatus.MARKET_CLOSED
                    quote != null -> HomeDataStatus.OK
                    else -> HomeDataStatus.UNAVAILABLE
                }

                HomeMarketSnapshot(
                    status = status,
                    code = homeIndex.code,
                    label = homeIndex.label,
                    value = quote?.currentPrice.toDoubleSafe(),
                    score = marketScore,
                    change = estimateChangeAmount(quote),
                    changeRate = quote?.changeRate.toDoubleSafe(),
                    asOf = now.withZoneSameInstant(homeIndex.zoneId)
                )
            }
        }
    }

    private fun fetchDomesticStocks(): List<HomeStockItem> =
        runCatching { homeDomesticRankingClient.fetchTopDomesticStocks(MAX_HOME_STOCKS) }
            .onFailure { ex -> logger.warn("Failed to fetch domestic home ranking stocks from KIS", ex) }
            .getOrDefault(emptyList())
            .map { ranked ->
                HomeStockItem(
                    ticker = ranked.ticker,
                    name = ranked.name,
                    price = ranked.price.toDoubleSafe(),
                    changeRate = ranked.changeRate.toDoubleSafe(),
                    currency = "KRW"
                )
            }

    private fun fetchForeignStocks(): List<HomeStockItem> =
        FOREIGN_HOME_STOCKS.mapNotNull { homeStock ->
            val market = homeStock.market ?: return@mapNotNull null
            fetchForeignStockFromKis(homeStock, market)
                ?: fetchForeignStockFromYahoo(homeStock)
        }.take(MAX_HOME_STOCKS)

    private fun fetchForeignStockFromKis(
        homeStock: HomeStockSeed,
        market: KisOverseasMarket
    ): HomeStockItem? =
        runCatching { kisOverseasPriceClient.fetchSnapshot(homeStock.ticker, market) }
            .onFailure { ex ->
                logger.warn("Failed to fetch foreign home stock from KIS. ticker={}, market={}", homeStock.ticker, market.name, ex)
            }
            .getOrNull()
            ?.let { quote ->
                HomeStockItem(
                    ticker = quote.ticker,
                    name = quote.name.ifBlank { homeStock.name },
                    price = quote.price.toDouble(),
                    changeRate = quote.changeRate.toDoubleSafe(),
                    currency = quote.currency
                )
            }

    private fun fetchForeignStockFromYahoo(homeStock: HomeStockSeed): HomeStockItem? =
        runCatching { yahooFinanceClient.getStockInfo(homeStock.ticker) }
            .onFailure { ex ->
                logger.warn("Failed to fetch foreign home stock from Yahoo fallback. ticker={}", homeStock.ticker, ex)
            }
            .getOrNull()
            ?.let { quote ->
                HomeStockItem(
                    ticker = quote.ticker,
                    name = homeStock.name,
                    price = quote.currentPrice.toDoubleSafe(),
                    changeRate = quote.changeRate.toDoubleSafe(),
                    currency = "USD"
                )
            }

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

    private fun estimateChangeAmount(quote: StockInfo?): Double {
        if (quote == null) {
            return 0.0
        }
        val currentPrice = quote.currentPrice
        val changeRate = quote.changeRate
        if (changeRate.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0
        }

        val previousPrice = currentPrice.divide(
            BigDecimal.ONE + changeRate.divide(BigDecimal("100")),
            6,
            java.math.RoundingMode.HALF_UP
        )
        return currentPrice.subtract(previousPrice).toDoubleSafe()
    }

    private fun summarize(totalScore: Int): String =
        when {
            totalScore >= 80 -> "매수하기 좋은 날"
            totalScore >= 65 -> "분할 매수를 보기 좋은 날"
            totalScore >= 50 -> "관망하며 확인할 날"
            totalScore >= 35 -> "신중하게 접근할 날"
            else -> "보수적으로 쉬어갈 날"
        }

    private fun BigDecimal?.toDoubleSafe(): Double =
        this?.toDouble() ?: 0.0

    private fun isWeekend(dayOfWeek: DayOfWeek): Boolean =
        dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

    private companion object {
        private val logger = LoggerFactory.getLogger(HomeService::class.java)
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val NEW_YORK_ZONE_ID: ZoneId = ZoneId.of("America/New_York")
        private val KOREA_MARKET_OPEN: LocalTime = LocalTime.of(9, 0)
        private val KOREA_MARKET_CLOSE: LocalTime = LocalTime.of(15, 30)
        private val US_MARKET_OPEN: LocalTime = LocalTime.of(9, 30)
        private val US_MARKET_CLOSE: LocalTime = LocalTime.of(16, 0)
        private const val MAX_HOME_STOCKS = 10
        private const val KOSPI_INDEX_CODE = "0001"

        private val FOREIGN_HOME_STOCKS = listOf(
            HomeStockSeed("AAPL", "Apple", KisOverseasMarket.NASDAQ),
            HomeStockSeed("MSFT", "Microsoft", KisOverseasMarket.NASDAQ),
            HomeStockSeed("NVDA", "NVIDIA", KisOverseasMarket.NASDAQ),
            HomeStockSeed("AMZN", "Amazon", KisOverseasMarket.NASDAQ),
            HomeStockSeed("META", "Meta", KisOverseasMarket.NASDAQ),
            HomeStockSeed("GOOGL", "Alphabet", KisOverseasMarket.NASDAQ),
            HomeStockSeed("TSLA", "Tesla", KisOverseasMarket.NASDAQ),
            HomeStockSeed("AVGO", "Broadcom", KisOverseasMarket.NASDAQ),
            HomeStockSeed("COST", "Costco", KisOverseasMarket.NASDAQ),
            HomeStockSeed("NFLX", "Netflix", KisOverseasMarket.NASDAQ)
        )
    }
}

private data class HomeStockSeed(
    val ticker: String,
    val name: String,
    val market: KisOverseasMarket? = null
)
