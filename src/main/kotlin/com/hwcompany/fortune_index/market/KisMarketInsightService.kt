package com.hwcompany.fortune_index.market

import org.springframework.stereotype.Service

@Service
class KisMarketInsightService(
    private val properties: StockMarketProperties,
    private val kisHeaderFactory: KisHeaderFactory,
    private val kisStockMarketClient: KisStockMarketClient,
    private val kisMarketFeignClient: KisMarketFeignClient
) {
    fun getVolumeRank(request: VolumeRankLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.volumeRankTrId,
            response = kisMarketFeignClient.fetchVolumeRank(
                headers = authenticatedHeaders(properties.kis.volumeRankTrId),
                params = linkedMapOf(
                    "FID_COND_MRKT_DIV_CODE" to request.marketDivisionCode,
                    "FID_COND_SCR_DIV_CODE" to request.screenDivisionCode,
                    "FID_INPUT_ISCD" to request.inputIscd,
                    "FID_DIV_CLS_CODE" to request.divisionClassCode,
                    "FID_BLNG_CLS_CODE" to request.belongingClassCode,
                    "FID_TRGT_CLS_CODE" to request.targetClassCode,
                    "FID_TRGT_EXLS_CLS_CODE" to request.targetExcludeClassCode,
                    "FID_INPUT_PRICE_1" to request.inputPrice1,
                    "FID_INPUT_PRICE_2" to request.inputPrice2,
                    "FID_VOL_CNT" to request.volumeCount,
                    "FID_INPUT_DATE_1" to request.inputDate1
                )
            )
        )

    fun getSectorIndex(request: SectorIndexLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.sectorIndexTrId,
            response = kisMarketFeignClient.fetchSectorIndex(
                headers = authenticatedHeaders(properties.kis.sectorIndexTrId),
                params = linkedMapOf(
                    "FID_COND_MRKT_DIV_CODE" to request.marketDivisionCode,
                    "FID_INPUT_ISCD" to request.indexCode
                )
            )
        )

    fun getSectorConstituents(request: SectorConstituentsLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.sectorConstituentsTrId,
            response = kisMarketFeignClient.fetchSectorConstituents(
                headers = authenticatedHeaders(properties.kis.sectorConstituentsTrId),
                params = linkedMapOf(
                    "FID_COND_MRKT_DIV_CODE" to request.marketDivisionCode,
                    "FID_INPUT_ISCD" to request.indexCode
                )
            )
        )

    fun getHoliday(request: HolidayLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.holidayTrId,
            response = kisMarketFeignClient.fetchHoliday(
                headers = authenticatedHeaders(properties.kis.holidayTrId),
                params = linkedMapOf("BASS_DT" to request.baseDate)
            )
        )

    fun getTopUpdown(request: TopUpdownLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.topUpdownTrId,
            response = kisMarketFeignClient.fetchTopUpdown(
                headers = authenticatedHeaders(properties.kis.topUpdownTrId),
                params = linkedMapOf(
                    "FID_COND_MRKT_DIV_CODE" to request.marketDivisionCode,
                    "FID_COND_SCR_DIV_CODE" to request.screenDivisionCode,
                    "FID_RANK_SORT_CLS_CODE" to request.rankingSortCode
                )
            )
        )

    fun getMarketCap(request: MarketCapLookupRequest): KisApiRawResponse =
        fetch(
            trId = properties.kis.marketCapTrId,
            response = kisMarketFeignClient.fetchMarketCap(
                headers = authenticatedHeaders(properties.kis.marketCapTrId),
                params = linkedMapOf(
                    "FID_COND_MRKT_DIV_CODE" to request.marketDivisionCode,
                    "FID_COND_SCR_DIV_CODE" to request.screenDivisionCode,
                    "FID_DIV_CLS_CODE" to request.divisionClassCode,
                    "FID_INPUT_ISCD" to request.inputIscd,
                    "FID_TRGT_CLS_CODE" to request.targetClassCode,
                    "FID_TRGT_EXLS_CLS_CODE" to request.targetExcludeClassCode,
                    "FID_INPUT_PRICE_1" to request.inputPrice1,
                    "FID_INPUT_PRICE_2" to request.inputPrice2,
                    "FID_VOL_CNT" to request.volumeCount
                )
            )
        )

    private fun authenticatedHeaders(trId: String): Map<String, String> =
        kisHeaderFactory.authenticatedHeaders(kisStockMarketClient.getAccessToken(), trId)

    private fun fetch(trId: String, response: KisApiResponseEnvelope): KisApiRawResponse {
        require(trId.isNotBlank()) { "trId must not be blank" }
        return response.toRawResponse()
    }
}
