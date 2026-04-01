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
    @JsonProperty("stck_prpr")
    val stckPrpr: String? = null,
    @JsonProperty("prdy_ctrt")
    val prdyCtrt: String? = null,
    @JsonProperty("stck_oprc")
    val stckOprc: String? = null,
    @JsonProperty("stck_hgpr")
    val stckHgpr: String? = null,
    @JsonProperty("stck_lwpr")
    val stckLwpr: String? = null,
    @JsonProperty("acml_vol")
    val acmlVol: String? = null,
    @JsonProperty("per")
    val per: String? = null,
    @JsonProperty("pbr")
    val pbr: String? = null,
    @JsonProperty("eps")
    val eps: String? = null,
    @JsonProperty("bps")
    val bps: String? = null,
    @JsonProperty("hts_avls")
    val htsAvls: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisSymbolInfoResponse(
    val output: KisSymbolInfoPayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisSymbolInfoPayload(
    @JsonProperty("prdt_abrv_name")
    val prdtAbrvName: String? = null,
    @JsonProperty("std_idst_clsf_cd_name")
    val stdIdstClsfCdName: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisOverseasPriceResponse(
    val output: KisOverseasPricePayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisOverseasPricePayload(
    @JsonProperty("last")
    val last: String? = null,
    @JsonProperty("rate")
    val rate: String? = null,
    @JsonProperty("rsym")
    val realTimeSymbol: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisOverseasSearchInfoResponse(
    val output: KisOverseasSearchInfoPayload? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KisOverseasSearchInfoPayload(
    @JsonProperty("prdt_name")
    val productName: String? = null,
    @JsonProperty("ovrs_item_name")
    val overseasItemName: String? = null,
    @JsonProperty("tr_crcy_cd")
    val tradingCurrencyCode: String? = null
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
