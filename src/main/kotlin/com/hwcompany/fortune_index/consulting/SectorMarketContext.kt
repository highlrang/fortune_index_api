package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.market.StockInfo
data class SectorMarketContext(
    val interestArea: String,
    val marketDataAsOf: String,
    val referenceSignal: String,
    val flowBias: String,
    val dataReliability: String,
    val marketNarrative: String,
    val tradingSignal: String,
    val fundamentalSignal: String,
    val safetyRule: String =
        "이름이나 코드, 정확한 숫자, 단정적인 예측은 말하지 않고 오늘의 흐름을 쉬운 운세 언어로 풀어낸다"
)

fun StockInfo.toSectorMarketContext(): SectorMarketContext {
    return SectorMarketContext(
        interestArea = sector,
        marketDataAsOf = marketDataAsOf.toString(),
        referenceSignal = "지금 보이는 숫자 대신 ${sector.ifBlank { "선택한 영역" }}의 상징적 흐름을 읽는 상태",
        flowBias = "숫자보다 사용자의 감정 리듬과 재물 감각에 초점을 두어야 하는 흐름",
        dataReliability = "별도 숫자 연동 없이 만든 운세 해석용 관심 분야 흐름",
        marketNarrative = marketNarrative.ifBlank {
            "${marketDataAsOf} 기준 ${sector.ifBlank { "선택한 영역" }}의 바깥 공기를 상징적으로 해석하는 컨텍스트입니다."
        },
        tradingSignal = "실시간 거래 신호 없이 질문과 섹터의 결만 참고하는 날",
        fundamentalSignal = "기초 체력 수치 대신 사용자의 재물 감각과 해석 흐름을 우선하는 날"
    )
}

data class MarketPhenomenonContext(
    val focusArea: String,
    val observedAt: String,
    val externalMood: String,
    val crowdTemperature: String,
    val volatilityWave: String,
    val staminaSignal: String,
    val interpretationRule: String =
        "모든 KIS 숫자는 특정 대상 추천이 아니라 사용자의 재물 기운을 비추는 현상 지표로만 해석한다"
)

fun SectorMarketContext.toMarketPhenomenonContext(): MarketPhenomenonContext =
    MarketPhenomenonContext(
        focusArea = interestArea,
        observedAt = marketDataAsOf,
        externalMood = marketNarrative,
        crowdTemperature = referenceSignal,
        volatilityWave = tradingSignal,
        staminaSignal = fundamentalSignal
    )
