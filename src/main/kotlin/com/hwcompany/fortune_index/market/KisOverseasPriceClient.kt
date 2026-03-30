package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KisOverseasPriceClient(
    private val properties: StockMarketProperties,
    private val kisHeaderFactory: KisHeaderFactory,
    private val kisAccessTokenService: KisAccessTokenService,
    private val kisMarketFeignClient: KisMarketFeignClient
) {
    fun fetchSnapshot(symbol: String, market: KisOverseasMarket): KisOverseasStockSnapshot {
        val accessToken = kisAccessTokenService.getAccessToken()
        val price = kisMarketFeignClient.fetchOverseasPrice(
            headers = kisHeaderFactory.authenticatedHeaders(accessToken, properties.kis.overseasPriceTrId),
            exchangeCode = market.exchangeCode,
            symbol = symbol
        ).output
        val info = runCatching {
            kisMarketFeignClient.fetchOverseasSearchInfo(
                headers = kisHeaderFactory.authenticatedHeaders(accessToken, properties.kis.overseasSearchInfoTrId),
                productTypeCode = market.productTypeCode,
                productCode = symbol
            ).output
        }.onFailure { ex ->
            logger.warn("Failed to fetch overseas search info. symbol={}, market={}", symbol, market.name, ex)
        }.getOrNull()

        return KisOverseasStockSnapshot(
            ticker = symbol,
            name = info?.productName?.takeIf { it.isNotBlank() }
                ?: info?.overseasItemName?.takeIf { it.isNotBlank() }
                ?: symbol,
            price = price?.last.toBigDecimalOrZero(),
            changeRate = price?.rate.toBigDecimalOrZero(),
            currency = info?.tradingCurrencyCode?.takeIf { it.isNotBlank() } ?: market.currency
        )
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal =
        this?.trim()?.replace(",", "")?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private companion object {
        private val logger = LoggerFactory.getLogger(KisOverseasPriceClient::class.java)
    }
}

data class KisOverseasStockSnapshot(
    val ticker: String,
    val name: String,
    val price: BigDecimal,
    val changeRate: BigDecimal,
    val currency: String
)

enum class KisOverseasMarket(
    val exchangeCode: String,
    val productTypeCode: String,
    val currency: String
) {
    NASDAQ("NAS", "512", "USD"),
    NYSE("NYS", "513", "USD"),
    AMEX("AMS", "529", "USD")
}
