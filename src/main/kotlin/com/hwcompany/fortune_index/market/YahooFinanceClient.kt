package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class YahooFinanceClient(
    webClientBuilder: WebClient.Builder,
    private val properties: StockMarketProperties
) {
    private val webClient = webClientBuilder
        .baseUrl(properties.yahoo.baseUrl)
        .build()

    fun getStockInfo(ticker: String): StockInfo {
        val quoteResponse = webClient.get()
            .uri { builder ->
                builder.path(properties.yahoo.quotePath)
                    .queryParam("symbols", ticker)
                    .build()
            }
            .retrieve()
            .bodyToMono<YahooQuoteResponse>()
            .block()
            ?: throw IllegalStateException("Yahoo quote response is empty")

        val quote = quoteResponse.quoteResponse?.result?.firstOrNull()
            ?: throw IllegalStateException("Yahoo quote payload is missing")

        val summary = fetchQuoteSummary(ticker)
        val sector = summary?.assetProfile?.sector?.takeIf { it.isNotBlank() } ?: properties.fallbackSector
        val fundamentals = FundamentalSnapshot(
            marketCap = summary?.price?.marketCap?.raw,
            trailingPe = summary?.summaryDetail?.trailingPE?.raw,
            forwardPe = summary?.summaryDetail?.forwardPE?.raw,
            priceToBook = summary?.defaultKeyStatistics?.priceToBook?.raw,
            operatingMarginRatio = summary?.financialData?.operatingMargins?.raw,
            returnOnEquityRatio = summary?.financialData?.returnOnEquity?.raw
        )
        val tradingSnapshot = TradingSnapshot(
            openPrice = quote.regularMarketOpen.toBigDecimalOrNullSafe(),
            highPrice = quote.regularMarketDayHigh.toBigDecimalOrNullSafe(),
            lowPrice = quote.regularMarketDayLow.toBigDecimalOrNullSafe(),
            volume = quote.regularMarketVolume?.toLong()
        )

        return StockInfo(
            ticker = quote.symbol ?: ticker,
            currentPrice = quote.regularMarketPrice.toBigDecimalOrZero(),
            changeRate = quote.regularMarketChangePercent.toBigDecimalOrZero(),
            sector = sector,
            source = MarketDataProvider.YAHOO,
            marketDataAsOf = LocalDate.now(),
            tradingSnapshot = tradingSnapshot,
            fundamentals = fundamentals,
            marketNarrative = buildNarrative(
                asOf = LocalDate.now(),
                sector = sector,
                changeRate = quote.regularMarketChangePercent.toBigDecimalOrZero(),
                fundamentals = fundamentals
            )
        )
    }

    private fun fetchQuoteSummary(ticker: String): YahooQuoteSummaryItem? {
        val response = runCatching {
            webClient.get()
                .uri { builder ->
                    builder.path("/v10/finance/quoteSummary/$ticker")
                        .queryParam("modules", properties.yahoo.modules)
                        .build()
                }
                .retrieve()
                .bodyToMono<YahooQuoteSummaryResponse>()
                .block()
        }.getOrElse { ex ->
            logger.warn("Failed to fetch Yahoo quote summary for ticker={}. Falling back to lightweight quote only.", ticker, ex)
            return null
        } ?: return null

        return response.quoteSummary?.result
            ?.firstOrNull()
    }

    private fun Number?.toBigDecimalOrZero(): BigDecimal =
        this?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun Number?.toBigDecimalOrNullSafe(): BigDecimal? =
        this?.toString()?.toBigDecimalOrNull()

    private fun buildNarrative(
        asOf: LocalDate,
        sector: String,
        changeRate: BigDecimal,
        fundamentals: FundamentalSnapshot
    ): String {
        val direction = when {
            changeRate >= BigDecimal("3.0") -> "수급이 강하게 몰리며 기대가 급격히 높아진 구간"
            changeRate > BigDecimal.ZERO -> "기대가 살아 있으나 과열 여부를 함께 점검해야 하는 구간"
            changeRate <= BigDecimal("-3.0") -> "위험회피가 강해져 방어 해석이 우선되는 구간"
            changeRate < BigDecimal.ZERO -> "단기 조정으로 눈높이가 낮아진 구간"
            else -> "방향성을 탐색하는 구간"
        }
        val fundamentalSignals = buildList {
            fundamentals.trailingPe?.let { add("PER ${it.stripTrailingZeros().toPlainString()}배") }
            fundamentals.forwardPe?.let { add("선행 PER ${it.stripTrailingZeros().toPlainString()}배") }
            fundamentals.priceToBook?.let { add("PBR ${it.stripTrailingZeros().toPlainString()}배") }
            fundamentals.operatingMarginRatio?.let { add("영업이익률 ${(it * BigDecimal("100")).stripTrailingZeros().toPlainString()}%") }
            fundamentals.returnOnEquityRatio?.let { add("ROE ${(it * BigDecimal("100")).stripTrailingZeros().toPlainString()}%") }
        }

        return buildString {
            append("$asOf 기준 $sector 섹터는 $direction")
            if (fundamentalSignals.isNotEmpty()) {
                append("이며 확보된 기초체력 신호는 ${fundamentalSignals.joinToString(", ")} 입니다.")
            } else {
                append("입니다. 다만 확보된 펀더멘털 지표가 제한적이라 가격 흐름 해석 비중이 더 큽니다.")
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(YahooFinanceClient::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteResponse(
    val quoteResponse: YahooQuoteResultWrapper? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteResultWrapper(
    val result: List<YahooQuoteResult>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteResult(
    val symbol: String? = null,
    val regularMarketPrice: Number? = null,
    val regularMarketChangePercent: Number? = null,
    val regularMarketOpen: Number? = null,
    val regularMarketDayHigh: Number? = null,
    val regularMarketDayLow: Number? = null,
    val regularMarketVolume: Number? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteSummaryResponse(
    val quoteSummary: YahooQuoteSummaryWrapper? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteSummaryWrapper(
    val result: List<YahooQuoteSummaryItem>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooQuoteSummaryItem(
    val assetProfile: YahooAssetProfile? = null,
    val summaryDetail: YahooSummaryDetail? = null,
    val financialData: YahooFinancialData? = null,
    val defaultKeyStatistics: YahooDefaultKeyStatistics? = null,
    val price: YahooPrice? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooAssetProfile(
    val sector: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooSummaryDetail(
    val trailingPE: YahooRawNumber? = null,
    val forwardPE: YahooRawNumber? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooFinancialData(
    val operatingMargins: YahooRawNumber? = null,
    val returnOnEquity: YahooRawNumber? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooDefaultKeyStatistics(
    val priceToBook: YahooRawNumber? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooPrice(
    val marketCap: YahooRawNumber? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooRawNumber(
    val raw: BigDecimal? = null
)
