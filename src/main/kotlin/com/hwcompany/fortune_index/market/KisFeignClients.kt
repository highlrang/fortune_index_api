package com.hwcompany.fortune_index.market

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "kisTokenFeignClient",
    url = "\${app.market.kis.base-url}"
)
interface KisTokenFeignClient {
    @PostMapping("\${app.market.kis.token-path}")
    fun issueAccessToken(
        @RequestHeader headers: Map<String, String>,
        @RequestBody request: KisTokenRequest
    ): KisTokenResponse
}

@FeignClient(
    name = "kisHashKeyFeignClient",
    url = "\${app.market.kis.base-url}"
)
interface KisHashKeyFeignClient {
    @PostMapping("\${app.market.kis.hash-key-path}")
    fun issueHashKey(
        @RequestHeader headers: Map<String, String>,
        @RequestBody request: Map<String, String>
    ): KisHashKeyResponse
}

@FeignClient(
    name = "kisMarketFeignClient",
    url = "\${app.market.kis.base-url}"
)
interface KisMarketFeignClient {
    @GetMapping("\${app.market.kis.volume-rank-path}")
    fun fetchVolumeRank(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.quote-path}")
    fun fetchQuote(
        @RequestHeader headers: Map<String, String>,
        @RequestParam("FID_COND_MRKT_DIV_CODE") marketDivisionCode: String,
        @RequestParam("FID_INPUT_ISCD") stockCode: String
    ): KisQuoteResponse

    @GetMapping("\${app.market.kis.symbol-info-path}")
    fun fetchSymbolInfo(
        @RequestHeader headers: Map<String, String>,
        @RequestParam("PDNO") stockCode: String,
        @RequestParam("PRDT_TYPE_CD") productTypeCode: String
    ): KisSymbolInfoResponse

    @GetMapping("\${app.market.kis.overseas-price-path}")
    fun fetchOverseasPrice(
        @RequestHeader headers: Map<String, String>,
        @RequestParam("AUTH") auth: String = "",
        @RequestParam("EXCD") exchangeCode: String,
        @RequestParam("SYMB") symbol: String
    ): KisOverseasPriceResponse

    @GetMapping("\${app.market.kis.overseas-search-info-path}")
    fun fetchOverseasSearchInfo(
        @RequestHeader headers: Map<String, String>,
        @RequestParam("PRDT_TYPE_CD") productTypeCode: String,
        @RequestParam("PDNO") productCode: String
    ): KisOverseasSearchInfoResponse

    @GetMapping("\${app.market.kis.overseas-rank-path}")
    fun fetchOverseasRank(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.sector-index-path}")
    fun fetchSectorIndex(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.sector-constituents-path}")
    fun fetchSectorConstituents(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.holiday-path}")
    fun fetchHoliday(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.top-updown-path}")
    fun fetchTopUpdown(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope

    @GetMapping("\${app.market.kis.market-cap-path}")
    fun fetchMarketCap(
        @RequestHeader headers: Map<String, String>,
        @RequestParam params: Map<String, String>
    ): KisApiResponseEnvelope
}
