package com.hwcompany.fortune_index.market

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/kis")
@Tag(name = "한국투자증권 연동 API", description = "한국투자증권 시세 및 시장 정보 연동 기능")
class KisIntegrationController(
    private val kisMarketInsightService: KisMarketInsightService
) {
    @Operation(summary = "거래량 순위 조회")
    @GetMapping("/market/rankings/volume-rank")
    fun getVolumeRank(
        @RequestParam(defaultValue = "J") marketDivisionCode: String,
        @RequestParam(defaultValue = "20171") screenDivisionCode: String,
        @RequestParam(defaultValue = "0000") inputIscd: String,
        @RequestParam(defaultValue = "0") divisionClassCode: String,
        @RequestParam(defaultValue = "0") belongingClassCode: String,
        @RequestParam(defaultValue = "111111111") targetClassCode: String,
        @RequestParam(defaultValue = "0000000000") targetExcludeClassCode: String,
        @RequestParam(defaultValue = "0") inputPrice1: String,
        @RequestParam(defaultValue = "0") inputPrice2: String,
        @RequestParam(defaultValue = "0") volumeCount: String,
        @RequestParam(defaultValue = "0") inputDate1: String
    ): KisApiRawResponse =
        kisMarketInsightService.getVolumeRank(
            VolumeRankLookupRequest(
                marketDivisionCode = marketDivisionCode,
                screenDivisionCode = screenDivisionCode,
                inputIscd = inputIscd,
                divisionClassCode = divisionClassCode,
                belongingClassCode = belongingClassCode,
                targetClassCode = targetClassCode,
                targetExcludeClassCode = targetExcludeClassCode,
                inputPrice1 = inputPrice1,
                inputPrice2 = inputPrice2,
                volumeCount = volumeCount,
                inputDate1 = inputDate1
            )
        )

    @Operation(summary = "업종 지수 시세 조회")
    @GetMapping("/market/sectors/index-price")
    fun getSectorIndex(
        @RequestParam indexCode: String,
        @RequestParam(defaultValue = "U") marketDivisionCode: String
    ): KisApiRawResponse =
        kisMarketInsightService.getSectorIndex(
            SectorIndexLookupRequest(
                indexCode = indexCode,
                marketDivisionCode = marketDivisionCode
            )
        )

    @Operation(summary = "업종 구성 종목 조회")
    @GetMapping("/market/sectors/constituents")
    fun getSectorConstituents(
        @RequestParam indexCode: String,
        @RequestParam(defaultValue = "U") marketDivisionCode: String
    ): KisApiRawResponse =
        kisMarketInsightService.getSectorConstituents(
            SectorConstituentsLookupRequest(
                indexCode = indexCode,
                marketDivisionCode = marketDivisionCode
            )
        )

    @Operation(summary = "증시 휴장일 조회")
    @GetMapping("/market/holiday")
    fun getHoliday(
        @RequestParam @NotBlank baseDate: String
    ): KisApiRawResponse =
        kisMarketInsightService.getHoliday(HolidayLookupRequest(baseDate = baseDate))

    @Operation(summary = "상승률/하락률 상위 종목 조회")
    @GetMapping("/market/rankings/top-updown")
    fun getTopUpdown(
        @RequestParam(defaultValue = "J") marketDivisionCode: String,
        @RequestParam(defaultValue = "20170") screenDivisionCode: String,
        @RequestParam(defaultValue = "0") rankingSortCode: String
    ): KisApiRawResponse =
        kisMarketInsightService.getTopUpdown(
            TopUpdownLookupRequest(
                marketDivisionCode = marketDivisionCode,
                screenDivisionCode = screenDivisionCode,
                rankingSortCode = rankingSortCode
            )
        )

    @Operation(summary = "시가총액 상위 조회")
    @GetMapping("/market/rankings/market-cap")
    fun getMarketCap(
        @RequestParam(defaultValue = "J") marketDivisionCode: String,
        @RequestParam(defaultValue = "20174") screenDivisionCode: String,
        @RequestParam(defaultValue = "0") divisionClassCode: String,
        @RequestParam(defaultValue = "0000") inputIscd: String,
        @RequestParam(defaultValue = "0") targetClassCode: String,
        @RequestParam(defaultValue = "0") targetExcludeClassCode: String,
        @RequestParam(defaultValue = "") inputPrice1: String,
        @RequestParam(defaultValue = "") inputPrice2: String,
        @RequestParam(defaultValue = "") volumeCount: String
    ): KisApiRawResponse =
        kisMarketInsightService.getMarketCap(
            MarketCapLookupRequest(
                marketDivisionCode = marketDivisionCode,
                screenDivisionCode = screenDivisionCode,
                divisionClassCode = divisionClassCode,
                inputIscd = inputIscd,
                targetClassCode = targetClassCode,
                targetExcludeClassCode = targetExcludeClassCode,
                inputPrice1 = inputPrice1,
                inputPrice2 = inputPrice2,
                volumeCount = volumeCount
            )
        )
}
