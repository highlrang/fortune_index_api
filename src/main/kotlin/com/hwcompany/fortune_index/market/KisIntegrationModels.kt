package com.hwcompany.fortune_index.market

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.NotBlank

data class KisApiRawResponse(
    val rtCd: String?,
    val msgCd: String?,
    val msg1: String?,
    val output: JsonNode?,
    val output1: JsonNode?,
    val output2: JsonNode?,
    val ctxAreaFk100: String?,
    val ctxAreaNk100: String?
)

fun KisApiResponseEnvelope.toRawResponse(): KisApiRawResponse =
    KisApiRawResponse(
        rtCd = rtCd,
        msgCd = msgCd,
        msg1 = msg1,
        output = output,
        output1 = output1,
        output2 = output2,
        ctxAreaFk100 = ctxAreaFk100,
        ctxAreaNk100 = ctxAreaNk100
    )

data class SectorIndexLookupRequest(
    @field:NotBlank
    val indexCode: String,
    val marketDivisionCode: String = "U"
)

data class SectorConstituentsLookupRequest(
    @field:NotBlank
    val indexCode: String,
    val marketDivisionCode: String = "U"
)

data class HolidayLookupRequest(
    @field:NotBlank
    val baseDate: String
)

data class TopUpdownLookupRequest(
    val marketDivisionCode: String = "J",
    val screenDivisionCode: String = "20170",
    val rankingSortCode: String = "0"
)

data class VolumeRankLookupRequest(
    val marketDivisionCode: String = "J",
    val screenDivisionCode: String = "20171",
    val inputIscd: String = "0000",
    val divisionClassCode: String = "0",
    val belongingClassCode: String = "0",
    val targetClassCode: String = "111111111",
    val targetExcludeClassCode: String = "0000000000",
    val inputPrice1: String = "0",
    val inputPrice2: String = "0",
    val volumeCount: String = "0",
    val inputDate1: String = "0"
)

data class MarketCapLookupRequest(
    val marketDivisionCode: String = "J",
    val screenDivisionCode: String = "20174",
    val divisionClassCode: String = "0",
    val inputIscd: String = "0000",
    val targetClassCode: String = "0",
    val targetExcludeClassCode: String = "0",
    val inputPrice1: String = "",
    val inputPrice2: String = "",
    val volumeCount: String = ""
)
