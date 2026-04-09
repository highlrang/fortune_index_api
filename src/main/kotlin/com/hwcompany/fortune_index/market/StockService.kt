package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class StockService(
    private val properties: StockMarketProperties
) {
    fun getStockInfo(ticker: String): StockInfo {
        val normalizedTicker = ticker.trim()
        require(normalizedTicker.isNotEmpty()) { "ticker must not be blank" }

        return StockInfo(
            ticker = normalizedTicker,
            currentPrice = BigDecimal.ZERO,
            changeRate = BigDecimal.ZERO,
            sector = properties.fallbackSector,
            source = properties.provider,
            fallback = true,
            marketDataAsOf = LocalDate.now(),
            marketNarrative = "외부 시장 데이터 연동이 제거되어 최신 시장 서사는 제공하지 않습니다."
        )
    }
}
