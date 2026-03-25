package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import java.time.LocalDate
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
                fallback = true,
                marketDataAsOf = LocalDate.now(),
                marketNarrative = "시장 데이터 확보에 실패해 최신 시장 서사는 생성하지 못했습니다."
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(StockService::class.java)
    }
}
