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
    var modules: String = "assetProfile,summaryDetail,financialData,defaultKeyStatistics,price"
)

data class KisProperties(
    var appKey: String = "",
    var appSecret: String = "",
    var baseUrl: String = "https://openapi.koreainvestment.com:9443",
    var tokenRefreshBufferSeconds: Long = 300,
    var hashKeyPath: String = "/uapi/hashkey",
    var quotePath: String = "/uapi/domestic-stock/v1/quotations/inquire-price",
    var symbolInfoPath: String = "/uapi/domestic-stock/v1/quotations/search-info",
    var overseasPricePath: String = "/uapi/overseas-price/v1/quotations/price",
    var overseasSearchInfoPath: String = "/uapi/overseas-price/v1/quotations/search-info",
    var volumeRankPath: String = "/uapi/domestic-stock/v1/quotations/volume-rank",
    var sectorIndexPath: String = "/uapi/domestic-stock/v1/quotations/inquire-index-price",
    var sectorConstituentsPath: String = "/uapi/domestic-stock/v1/quotations/inquire-index-member",
    var holidayPath: String = "/uapi/domestic-stock/v1/quotations/chk-holiday",
    var topUpdownPath: String = "/uapi/domestic-stock/v1/ranking/top-updown",
    var marketCapPath: String = "/uapi/domestic-stock/v1/ranking/market-cap",
    var tokenPath: String = "/oauth2/tokenP",
    var quoteTrId: String = "FHKST01010100",
    var symbolInfoTrId: String = "CTPF1604R",
    var overseasPriceTrId: String = "HHDFS00000300",
    var overseasSearchInfoTrId: String = "CTPF1702R",
    var volumeRankTrId: String = "FHPST01710000",
    var sectorIndexTrId: String = "FHPUP02110000",
    var sectorConstituentsTrId: String = "FHPUP02120000",
    var holidayTrId: String = "CTCA0903R",
    var topUpdownTrId: String = "FHPST01700000",
    var marketCapTrId: String = "FHPST01740000"
)

enum class MarketDataProvider {
    YAHOO,
    KIS
}
