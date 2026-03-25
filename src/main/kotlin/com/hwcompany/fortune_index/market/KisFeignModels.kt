package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

data class KisTokenRequest(
    val grant_type: String,
    val appkey: String,
    val appsecret: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisTokenResponse(
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
data class KisHashKeyResponse(
    @JsonProperty("HASH")
    val hash: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisQuoteResponse(
    val output: KisQuotePayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisQuotePayload(
    val stckPrpr: String? = null,
    val prdyCtrt: String? = null,
    val stckOprc: String? = null,
    val stckHgpr: String? = null,
    val stckLwpr: String? = null,
    val acmlVol: String? = null,
    val per: String? = null,
    val pbr: String? = null,
    val eps: String? = null,
    val bps: String? = null,
    val htsAvls: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisSymbolInfoResponse(
    val output: KisSymbolInfoPayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisSymbolInfoPayload(
    val prdtAbrvName: String? = null,
    val stdIdstClsfCdName: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisApiResponseEnvelope(
    @JsonProperty("rt_cd")
    val rtCd: String? = null,
    @JsonProperty("msg_cd")
    val msgCd: String? = null,
    @JsonProperty("msg1")
    val msg1: String? = null,
    val output: JsonNode? = null,
    val output1: JsonNode? = null,
    val output2: JsonNode? = null,
    @JsonProperty("ctx_area_fk100")
    val ctxAreaFk100: String? = null,
    @JsonProperty("ctx_area_nk100")
    val ctxAreaNk100: String? = null
)
