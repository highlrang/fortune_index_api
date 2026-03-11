package com.hwcompany.fortune_index.market

import java.math.BigDecimal
import org.springframework.stereotype.Service

@Service
class StockMarketService(
    private val kisStockMarketClient: KisStockMarketClient
) {
    fun getSnapshot(request: StockMarketRequest): StockMarketSnapshot {
        val marketData = kisStockMarketClient.fetchSnapshot(request.stockCode)
        val narrative = buildNarrative(
            stockName = request.stockName,
            sectorName = marketData.sectorName,
            changeRate = marketData.changeRate,
            sectorOutlook = request.sectorOutlook
        )

        return marketData.copy(
            stockName = request.stockName,
            marketNarrative = narrative
        )
    }

    private fun buildNarrative(
        stockName: String,
        sectorName: String?,
        changeRate: BigDecimal,
        sectorOutlook: String?
    ): String {
        val directionText = when {
            changeRate > BigDecimal.ZERO -> "${changeRate.stripTrailingZeros().toPlainString()}% 상승 중"
            changeRate < BigDecimal.ZERO -> "${changeRate.abs().stripTrailingZeros().toPlainString()}% 하락 중"
            else -> "보합권"
        }
        val sectorText = sectorName?.let { "$it 섹터에 속해 있고" } ?: "관련 섹터 흐름과 함께 보면"
        val outlookText = sectorOutlook?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""

        return "지금 $stockName 은(는) $directionText이고, $sectorText$outlookText".trim()
    }
}
