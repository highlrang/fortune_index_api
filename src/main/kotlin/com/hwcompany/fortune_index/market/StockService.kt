package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StockService(
    private val properties: StockMarketProperties,
    private val yahooFinanceClient: YahooFinanceClient,
    private val kisStockInfoClient: KisStockInfoClient
) {
    fun getStockInfo(ticker: String): StockInfo {
        val normalizedTicker = ticker.trim()
        require(normalizedTicker.isNotEmpty()) { "ticker must not be blank" }

        return runCatching {
            when (properties.provider) {
                MarketDataProvider.YAHOO -> yahooFinanceClient.getStockInfo(normalizedTicker)
                MarketDataProvider.KIS -> kisStockInfoClient.getStockInfo(normalizedTicker)
            }
        }.getOrElse { ex ->
            logger.warn("Failed to fetch stock info for ticker={}", normalizedTicker, ex)
            StockInfo(
                ticker = normalizedTicker,
                currentPrice = BigDecimal.ZERO,
                changeRate = BigDecimal.ZERO,
                sector = properties.fallbackSector,
                source = properties.provider,
                fallback = true
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(StockService::class.java)
    }
}
