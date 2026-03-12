package com.hwcompany.fortune_index.market

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class KisHeaderFactory(
    private val properties: StockMarketProperties
) {
    fun tokenHeaders(): Map<String, String> =
        linkedMapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            "appkey" to properties.kis.appKey,
            "appsecret" to properties.kis.appSecret
        )

    fun authenticatedHeaders(accessToken: String, trId: String): Map<String, String> =
        linkedMapOf(
            HttpHeaders.AUTHORIZATION to "Bearer $accessToken",
            "appkey" to properties.kis.appKey,
            "appsecret" to properties.kis.appSecret,
            "tr_id" to trId,
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE
        )

    fun hashKeyHeaders(accessToken: String): Map<String, String> =
        linkedMapOf(
            HttpHeaders.AUTHORIZATION to "Bearer $accessToken",
            "appkey" to properties.kis.appKey,
            "appsecret" to properties.kis.appSecret,
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE
        )
}
