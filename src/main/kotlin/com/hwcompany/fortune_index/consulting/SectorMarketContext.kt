package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.market.StockInfo
import java.math.BigDecimal

data class SectorMarketContext(
    val sector: String,
    val marketDataAsOf: String,
    val referenceSignal: String,
    val sectorBias: String,
    val dataReliability: String,
    val marketNarrative: String,
    val tradingSignal: String,
    val fundamentalSignal: String,
    val safetyRule: String =
        "특정 종목명, 종목코드, 정확한 가격, 목표가, 개별 기업 이슈는 언급하지 않고 현상 지표를 운세 언어로 번역한다"
)

fun StockInfo.toSectorMarketContext(): SectorMarketContext {
    val referenceSignal = when {
        changeRate >= BigDecimal("3.0") -> "기대감의 파도가 빠르게 번지는 흐름"
        changeRate > BigDecimal.ZERO -> "조심스러운 낙관이 스며드는 흐름"
        changeRate <= BigDecimal("-3.0") -> "불안의 그림자가 길게 드리우는 흐름"
        changeRate < BigDecimal.ZERO -> "마음이 쉽게 위축될 수 있는 흐름"
        else -> "방향을 고르지 못한 채 공기가 머무는 흐름"
    }

    val sectorBias = when {
        fallback -> "데이터가 흐릿해 섣부른 해석을 경계해야 하는 흐름"
        changeRate >= BigDecimal("3.0") -> "기세는 강하지만 마음이 과열되기 쉬운 흐름"
        changeRate > BigDecimal.ZERO -> "조용히 문이 열리지만 속도 조절이 필요한 흐름"
        changeRate <= BigDecimal("-3.0") -> "방어 본능이 짙어져 중심을 지켜야 하는 흐름"
        changeRate < BigDecimal.ZERO -> "작게 움츠러들며 숨을 고르기 쉬운 흐름"
        else -> "해석을 서두르기보다 관찰이 필요한 흐름"
    }

    val dataReliability = if (fallback) {
        "실시간 시장 데이터 확보가 흔들려 오늘의 공기를 거칠게만 읽을 수 있음"
    } else {
        "실시간 시장 숫자를 재물 기운 해석용 현상 지표로 번역한 데이터"
    }

    val tradingSignal = buildString {
        val hasRange = tradingSnapshot.openPrice != null || tradingSnapshot.highPrice != null || tradingSnapshot.lowPrice != null
        val hasVolume = tradingSnapshot.volume != null

        when {
            hasRange && hasVolume -> append("장중 흔들림과 군중의 움직임이 함께 감지되는 날")
            hasRange -> append("가격 파동은 보이지만 군중의 발걸음은 또렷하지 않은 날")
            hasVolume -> append("마음들이 분주하게 오가지만 방향은 단정하기 어려운 날")
            else -> append("당일 거래의 결을 읽을 단서는 제한적인 날")
        }
    }

    val fundamentalSignal = when {
        fundamentals.trailingPe != null || fundamentals.priceToBook != null || fundamentals.marketCap != null ->
            "겉으로 보이는 체력 신호는 남아 있으나 숫자 자체보다 분위기 해석에만 제한적으로 써야 하는 날"
        fundamentals.eps != null || fundamentals.bps != null || fundamentals.operatingMarginRatio != null || fundamentals.returnOnEquityRatio != null ->
            "기초 체력의 결은 감지되지만 확정 판단으로 밀어붙일 정도는 아닌 날"
        else -> "확보된 기초 체력 지표가 적어 바깥 공기 해석 비중이 큰 날"
    }

    return SectorMarketContext(
        sector = sector,
        marketDataAsOf = marketDataAsOf.toString(),
        referenceSignal = referenceSignal,
        sectorBias = sectorBias,
        dataReliability = dataReliability,
        marketNarrative = marketNarrative.ifBlank {
            "${marketDataAsOf} 기준 $sector 섹터의 최신 서사를 생성하기에는 데이터가 부족합니다."
        },
        tradingSignal = tradingSignal,
        fundamentalSignal = fundamentalSignal
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
        focusArea = sector,
        observedAt = marketDataAsOf,
        externalMood = marketNarrative,
        crowdTemperature = referenceSignal,
        volatilityWave = tradingSignal,
        staminaSignal = fundamentalSignal
    )
