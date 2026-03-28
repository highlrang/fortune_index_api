package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class KisStockMarketClient(
    private val properties: StockMarketProperties,
    private val kisHeaderFactory: KisHeaderFactory,
    private val kisAccessTokenService: KisAccessTokenService,
    private val kisHashKeyFeignClient: KisHashKeyFeignClient,
    private val kisMarketFeignClient: KisMarketFeignClient
) {
    fun fetchSnapshot(stockCode: String): StockMarketSnapshot {
        val accessToken = getAccessToken()
        val quote = fetchQuote(stockCode, accessToken)
        val symbolInfo = fetchSymbolInfo(stockCode, accessToken)

        return StockMarketSnapshot(
            stockCode = stockCode,
            stockName = symbolInfo.stockName ?: stockCode,
            currentPrice = quote.currentPrice,
            changeRate = quote.changeRate,
            sectorName = symbolInfo.sectorName,
            marketNarrative = quote.toNarrative(symbolInfo.sectorName),
            marketDataAsOf = quote.marketDataAsOf,
            tradingSnapshot = quote.tradingSnapshot,
            fundamentals = quote.fundamentals
        )
    }

    private fun fetchQuote(stockCode: String, accessToken: String): KisQuoteOutput {
        val response = kisMarketFeignClient.fetchQuote(
            headers = kisHeaderFactory.authenticatedHeaders(accessToken, properties.kis.quoteTrId),
            marketDivisionCode = "J",
            stockCode = stockCode
        )

        val output = response.output ?: throw IllegalStateException("KIS 현재가 데이터가 없습니다.")
        return KisQuoteOutput(
            currentPrice = output.stckPrpr.toBigDecimalOrZero(),
            changeRate = output.prdyCtrt.toBigDecimalOrZero(),
            tradingSnapshot = TradingSnapshot(
                openPrice = output.stckOprc.toBigDecimalOrNullSafe(),
                highPrice = output.stckHgpr.toBigDecimalOrNullSafe(),
                lowPrice = output.stckLwpr.toBigDecimalOrNullSafe(),
                volume = output.acmlVol.toLongOrNullSafe()
            ),
            fundamentals = FundamentalSnapshot(
                marketCap = output.htsAvls.toBigDecimalOrNullSafe(),
                trailingPe = output.per.toBigDecimalOrNullSafe(),
                priceToBook = output.pbr.toBigDecimalOrNullSafe(),
                eps = output.eps.toBigDecimalOrNullSafe(),
                bps = output.bps.toBigDecimalOrNullSafe()
            ),
            marketDataAsOf = LocalDate.now()
        )
    }

    private fun fetchSymbolInfo(stockCode: String, accessToken: String): KisSymbolInfoOutput {
        val response = runCatching {
            kisMarketFeignClient.fetchSymbolInfo(
                headers = kisHeaderFactory.authenticatedHeaders(accessToken, properties.kis.symbolInfoTrId),
                stockCode = stockCode,
                productTypeCode = "300"
            )
        }.getOrNull() ?: return KisSymbolInfoOutput()

        return KisSymbolInfoOutput(
            stockName = response.output?.prdtAbrvName,
            sectorName = response.output?.stdIdstClsfCdName
        )
    }

    fun getAccessToken(): String {
        return kisAccessTokenService.getAccessToken()
    }

    fun issueHashKey(requestBody: Map<String, String>): String {
        val accessToken = getAccessToken()
        val response = kisHashKeyFeignClient.issueHashKey(
            headers = kisHeaderFactory.hashKeyHeaders(accessToken),
            request = requestBody
        )

        return response.hash ?: throw IllegalStateException("KIS hashkey 생성에 실패했습니다.")
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal =
        this?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun String?.toBigDecimalOrNullSafe(): BigDecimal? =
        this?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

    private fun String?.toLongOrNullSafe(): Long? =
        this?.trim()?.replace(",", "")?.takeIf { it.isNotEmpty() }?.toLongOrNull()
}

private data class KisQuoteOutput(
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal,
    val tradingSnapshot: TradingSnapshot,
    val fundamentals: FundamentalSnapshot,
    val marketDataAsOf: LocalDate
) {
    fun toNarrative(sectorName: String?): String {
        val sectorLabel = sectorName?.takeIf { it.isNotBlank() } ?: "해당 섹터"
        val direction = when {
            changeRate >= BigDecimal("3.0") -> "매수 열기가 강하게 붙은 상태"
            changeRate > BigDecimal.ZERO -> "완만하게 위험자산 선호가 살아나는 상태"
            changeRate <= BigDecimal("-3.0") -> "변동성이 커지며 방어 심리가 강해진 상태"
            changeRate < BigDecimal.ZERO -> "숨 고르기와 경계가 함께 나타나는 상태"
            else -> "방향성 탐색 구간"
        }
        val valuation = buildList {
            fundamentals.trailingPe?.let { add("PER ${it.stripTrailingZeros().toPlainString()}배") }
            fundamentals.priceToBook?.let { add("PBR ${it.stripTrailingZeros().toPlainString()}배") }
        }.joinToString(", ")

        return buildString {
            append("${marketDataAsOf} 기준 $sectorLabel 섹터는 $direction 입니다.")
            if (valuation.isNotBlank()) {
                append(" 현재 확보된 밸류에이션 신호는 $valuation 수준입니다.")
            }
            tradingSnapshot.volume?.let { volume ->
                append(" 누적 거래량은 ${"%,d".format(volume)}주입니다.")
            }
        }
    }
}

private data class KisSymbolInfoOutput(
    val stockName: String? = null,
    val sectorName: String? = null
)
