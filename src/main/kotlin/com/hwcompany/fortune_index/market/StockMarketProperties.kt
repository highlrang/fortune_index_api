package com.hwcompany.fortune_index.market

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.market")
data class StockMarketProperties(
    var provider: MarketDataProvider = MarketDataProvider.YAHOO,
    var fallbackSector: String = "UNKNOWN",
    var yahoo: YahooProperties = YahooProperties(),
    var kis: KisProperties = KisProperties()
)

data class YahooProperties(
    var baseUrl: String = "https://query1.finance.yahoo.com",
    var quotePath: String = "/v7/finance/quote",
    var modules: String = "assetProfile"
)

data class KisProperties(
    var appKey: String = "",
    var appSecret: String = "",
    var baseUrl: String = "https://openapi.koreainvestment.com:9443",
    var quotePath: String = "/uapi/domestic-stock/v1/quotations/inquire-price",
    var symbolInfoPath: String = "/uapi/domestic-stock/v1/quotations/search-info",
    var tokenPath: String = "/oauth2/tokenP",
    var quoteTrId: String = "FHKST01010100",
    var symbolInfoTrId: String = "CTPF1604R"
)

enum class MarketDataProvider {
    YAHOO,
    KIS
}
