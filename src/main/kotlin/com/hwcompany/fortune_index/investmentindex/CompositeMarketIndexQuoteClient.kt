package com.hwcompany.fortune_index.investmentindex

import com.hwcompany.fortune_index.market.KisDomesticIndexClient
import com.hwcompany.fortune_index.market.YahooFinanceClient
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class CompositeMarketIndexQuoteClient(
    private val kisDomesticIndexClient: KisDomesticIndexClient,
    private val yahooFinanceClient: YahooFinanceClient
) : MarketIndexQuoteClient {
    override fun getChangeRate(ticker: String): Double =
        when (ticker) {
            SupportedMarket.KOSPI.ticker -> kisDomesticIndexClient.fetchSnapshot(KOSPI_INDEX_CODE)?.changeRate
                ?: yahooFinanceClient.getStockInfo(ticker).changeRate.toDouble()
            else -> yahooFinanceClient.getStockInfo(ticker).changeRate.toDouble()
        }

    private companion object {
        private const val KOSPI_INDEX_CODE = "0001"
    }
}
