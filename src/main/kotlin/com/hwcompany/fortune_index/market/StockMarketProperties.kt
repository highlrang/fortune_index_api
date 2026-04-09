package com.hwcompany.fortune_index.market

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.market")
data class StockMarketProperties(
    var provider: MarketDataProvider = MarketDataProvider.LOCAL,
    var fallbackSector: String = "UNKNOWN"
)

enum class MarketDataProvider {
    LOCAL
}
