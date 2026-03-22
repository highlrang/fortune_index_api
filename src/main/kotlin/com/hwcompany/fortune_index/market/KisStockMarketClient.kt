package com.hwcompany.fortune_index.market

import java.math.BigDecimal
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
            marketNarrative = ""
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
            changeRate = output.prdyCtrt.toBigDecimalOrZero()
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
}

private data class KisQuoteOutput(
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal
)

private data class KisSymbolInfoOutput(
    val stockName: String? = null,
    val sectorName: String? = null
)
