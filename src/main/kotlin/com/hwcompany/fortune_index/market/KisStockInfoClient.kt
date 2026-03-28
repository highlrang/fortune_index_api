package com.hwcompany.fortune_index.market

import org.springframework.stereotype.Component

@Component
class KisStockInfoClient(
    private val kisStockMarketClient: KisStockMarketClient,
    private val properties: StockMarketProperties
) {
    fun getStockInfo(ticker: String): StockInfo {
        val snapshot = kisStockMarketClient.fetchSnapshot(ticker)

        return StockInfo(
            ticker = snapshot.stockCode,
            currentPrice = snapshot.currentPrice,
            changeRate = snapshot.changeRate,
            sector = snapshot.sectorName ?: properties.fallbackSector,
            source = MarketDataProvider.KIS,
            marketDataAsOf = snapshot.marketDataAsOf,
            tradingSnapshot = snapshot.tradingSnapshot,
            fundamentals = snapshot.fundamentals,
            marketNarrative = snapshot.marketNarrative
        )
    }
}
