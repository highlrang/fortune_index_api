package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class KisStockMarketClient(
    restClientBuilder: RestClient.Builder,
    private val properties: StockMarketProperties
) {
    private val restClient = restClientBuilder
        .baseUrl(properties.kis.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    private val tokenCache = AtomicReference<CachedToken?>()

    fun fetchSnapshot(stockCode: String): StockMarketSnapshot {
        val accessToken = getAccessToken()
        val quote = fetchQuote(stockCode, accessToken)
        val symbolInfo = fetchSymbolInfo(stockCode, accessToken)

        return StockMarketSnapshot(
            stockCode = stockCode,
            stockName = symbolInfo.stockName ?: stockCode,
            currentPrice = quote.currentPrice,
            changeRate = quote.changeRate,
            sectorName = symbolInfo.sectorName,
            marketNarrative = ""
        )
    }

    private fun fetchQuote(stockCode: String, accessToken: String): KisQuoteOutput {
        val response = restClient.get()
            .uri { builder ->
                builder.path(properties.kis.quotePath)
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .header("appkey", properties.kis.appKey)
            .header("appsecret", properties.kis.appSecret)
            .header("tr_id", properties.kis.quoteTrId)
            .retrieve()
            .body(KisQuoteResponse::class.java)
            ?: throw IllegalStateException("KIS 현재가 응답이 비어 있습니다.")

        val output = response.output ?: throw IllegalStateException("KIS 현재가 데이터가 없습니다.")
        return KisQuoteOutput(
            currentPrice = output.stckPrpr.toBigDecimalOrZero(),
            changeRate = output.prdyCtrt.toBigDecimalOrZero()
        )
    }

    private fun fetchSymbolInfo(stockCode: String, accessToken: String): KisSymbolInfoOutput {
        val response = restClient.get()
            .uri { builder ->
                builder.path(properties.kis.symbolInfoPath)
                    .queryParam("PDNO", stockCode)
                    .queryParam("PRDT_TYPE_CD", "300")
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .header("appkey", properties.kis.appKey)
            .header("appsecret", properties.kis.appSecret)
            .header("tr_id", properties.kis.symbolInfoTrId)
            .retrieve()
            .body(KisSymbolInfoResponse::class.java)
            ?: return KisSymbolInfoOutput()

        return KisSymbolInfoOutput(
            stockName = response.output?.prdtAbrvName,
            sectorName = response.output?.stdIdstClsfCdName
        )
    }

    private fun getAccessToken(): String {
        val cached = tokenCache.get()
        if (cached != null && cached.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
            return cached.accessToken
        }

        val response = restClient.post()
            .uri(properties.kis.tokenPath)
            .body(
                mapOf(
                    "grant_type" to "client_credentials",
                    "appkey" to properties.kis.appKey,
                    "appsecret" to properties.kis.appSecret
                )
            )
            .retrieve()
            .body(KisTokenResponse::class.java)
            ?: throw IllegalStateException("KIS 토큰 응답이 비어 있습니다.")

        val accessToken = response.normalizedAccessToken
            ?: throw IllegalStateException("KIS 접근 토큰이 없습니다.")
        val expiresIn = response.normalizedExpiresIn?.toLongOrNull() ?: 3600L
        tokenCache.set(CachedToken(accessToken, Instant.now().plusSeconds(expiresIn)))
        return accessToken
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal =
        this?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

private data class CachedToken(
    val accessToken: String,
    val expiresAt: Instant
)

private data class KisQuoteOutput(
    val currentPrice: BigDecimal,
    val changeRate: BigDecimal
)

private data class KisSymbolInfoOutput(
    val stockName: String? = null,
    val sectorName: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KisTokenResponse(
    val accessToken: String? = null,
    val access_token: String? = null,
    val expiresIn: String? = null,
    val expires_in: String? = null
) {
    val normalizedAccessToken: String?
        get() = accessToken ?: access_token

    val normalizedExpiresIn: String?
        get() = expiresIn ?: expires_in
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KisQuoteResponse(
    val output: KisQuotePayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KisQuotePayload(
    val stckPrpr: String? = null,
    val prdyCtrt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KisSymbolInfoResponse(
    val output: KisSymbolInfoPayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KisSymbolInfoPayload(
    val prdtAbrvName: String? = null,
    val stdIdstClsfCdName: String? = null
)
