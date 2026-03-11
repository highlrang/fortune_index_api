package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
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

        val sector = fetchSector(ticker)

        return StockInfo(
            ticker = quote.symbol ?: ticker,
            currentPrice = quote.regularMarketPrice.toBigDecimalOrZero(),
            changeRate = quote.regularMarketChangePercent.toBigDecimalOrZero(),
            sector = sector,
            source = MarketDataProvider.YAHOO
        )
    }

    private fun fetchSector(ticker: String): String {
        val response = webClient.get()
            .uri { builder ->
                builder.path("/v10/finance/quoteSummary/$ticker")
                    .queryParam("modules", properties.yahoo.modules)
                    .build()
            }
            .retrieve()
            .bodyToMono<YahooQuoteSummaryResponse>()
            .block()
            ?: return properties.fallbackSector

        return response.quoteSummary?.result
            ?.firstOrNull()
            ?.assetProfile
            ?.sector
            ?.takeIf { it.isNotBlank() }
            ?: properties.fallbackSector
    }

    private fun Number?.toBigDecimalOrZero(): BigDecimal =
        this?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
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
    val regularMarketChangePercent: Number? = null
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
    val assetProfile: YahooAssetProfile? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class YahooAssetProfile(
    val sector: String? = null
)
