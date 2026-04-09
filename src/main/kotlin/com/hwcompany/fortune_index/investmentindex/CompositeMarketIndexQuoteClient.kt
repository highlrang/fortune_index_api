package com.hwcompany.fortune_index.investmentindex

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class CompositeMarketIndexQuoteClient : MarketIndexQuoteClient {
    override fun getChangeRate(ticker: String): Double = 0.0
}
