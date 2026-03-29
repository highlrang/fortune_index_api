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
            changeRate >= BigDecimal("3.0") -> "기대감이 빠르게 번지지만 과열을 경계해야 하는 상태"
            changeRate > BigDecimal.ZERO -> "조심스러운 낙관이 스며드는 상태"
            changeRate <= BigDecimal("-3.0") -> "불안과 방어 심리가 함께 짙어지는 상태"
            changeRate < BigDecimal.ZERO -> "숨 고르기와 경계가 겹치는 상태"
            else -> "방향을 고르지 못한 탐색 구간"
        }
        val stamina = when {
            fundamentals.trailingPe != null || fundamentals.priceToBook != null ->
                "기초 체력의 흔적은 보이지만 숫자 자체보다 분위기 해석에만 제한적으로 써야 합니다."
            fundamentals.marketCap != null || fundamentals.eps != null || fundamentals.bps != null ->
                "기초 체력 단서는 있으나 확정 판단의 재료로 밀어붙이면 안 됩니다."
            else -> "기초 체력 단서는 희미해 오늘은 공기의 결을 읽는 비중이 더 큽니다."
        }

        return buildString {
            append("${marketDataAsOf} 기준 $sectorLabel 섹터는 $direction 입니다.")
            append(" $stamina")
            append(" 이 데이터는 추천 근거가 아니라 오늘의 외부 기류를 읽는 현상 지표입니다.")
        }
    }
}

private data class KisSymbolInfoOutput(
    val stockName: String? = null,
    val sectorName: String? = null
)
