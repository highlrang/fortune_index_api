package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.hwcompany.fortune_index.market.StockMarketProperties
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class HomeMarketChartClient(
    webClientBuilder: WebClient.Builder,
    properties: StockMarketProperties
) {
    private val webClient = webClientBuilder
        .baseUrl(properties.yahoo.baseUrl)
        .build()

    fun fetchChart(index: HomeSupportedIndex, period: HomeChartPeriod): List<HomeChartSample> =
        runCatching {
            val response = webClient.get()
                .uri { builder ->
                    builder.path("/v8/finance/chart/{ticker}")
                        .queryParam("range", period.yahooRange)
                        .queryParam("interval", period.yahooInterval)
                        .build(index.ticker)
                }
                .retrieve()
                .bodyToMono<YahooChartResponse>()
                .block()
                ?: throw IllegalStateException("Yahoo chart response is empty")

            val result = response.chart?.result?.firstOrNull()
                ?: throw IllegalStateException("Yahoo chart payload is missing")
            val timestamps = result.timestamp.orEmpty()
            val closes = result.indicators?.quote?.firstOrNull()?.close.orEmpty()

            timestamps.zip(closes)
                .mapNotNull { (epochSecond, close) ->
                    close?.let {
                        HomeChartSample(
                            timestamp = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(epochSecond),
                                index.zoneId
                            ),
                            value = it
                        )
                    }
                }
        }.getOrElse { ex ->
            logger.warn("Failed to fetch chart data for index={}", index.code, ex)
            emptyList()
        }

    private companion object {
        private val logger = LoggerFactory.getLogger(HomeMarketChartClient::class.java)
    }
}

data class HomeChartSample(
    val timestamp: ZonedDateTime,
    val value: Double
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooChartResponse(
    val chart: YahooChartContainer? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooChartContainer(
    val result: List<YahooChartResult>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooChartResult(
    val timestamp: List<Long>? = null,
    val indicators: YahooChartIndicators? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooChartIndicators(
    val quote: List<YahooChartQuote>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooChartQuote(
    val close: List<Double?>? = null
)

enum class HomeChartPeriod(
    val value: String,
    val yahooRange: String,
    val yahooInterval: String
) {
    ONE_DAY("1D", "1d", "30m"),
    ONE_WEEK("1W", "5d", "1h"),
    ONE_MONTH("1M", "1mo", "1d");

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
