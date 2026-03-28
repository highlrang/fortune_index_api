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
        "특정 종목명, 종목코드, 정확한 가격, 목표가, 개별 기업 이슈는 언급하지 않고 섹터 중심 일반론으로만 해석"
)

fun StockInfo.toSectorMarketContext(): SectorMarketContext {
    val referenceSignal = when {
        changeRate >= BigDecimal("3.0") -> "참조 신호가 강한 상승 압력을 보임"
        changeRate > BigDecimal.ZERO -> "참조 신호가 완만한 상승 흐름을 보임"
        changeRate <= BigDecimal("-3.0") -> "참조 신호가 강한 하락 압력을 보임"
        changeRate < BigDecimal.ZERO -> "참조 신호가 완만한 약세 흐름을 보임"
        else -> "참조 신호가 뚜렷한 방향성 없이 횡보 중"
    }

    val sectorBias = when {
        fallback -> "데이터 불완전으로 중립"
        changeRate >= BigDecimal("3.0") -> "공격적 추격보다 리스크 점검이 우선되는 강세"
        changeRate > BigDecimal.ZERO -> "완만한 강세"
        changeRate <= BigDecimal("-3.0") -> "방어적 접근이 우선되는 약세"
        changeRate < BigDecimal.ZERO -> "보수적 접근이 필요한 약세"
        else -> "방향성 유보"
    }

    val dataReliability = if (fallback) {
        "실시간 시장 데이터 확보에 실패해 섹터 일반론만 가능"
    } else {
        "개별 종목의 당일 움직임과 확보된 기초 지표를 섹터 해석용 참고 신호로 단순화한 데이터"
    }

    val tradingSignal = buildString {
        val rangeText = listOfNotNull(
            tradingSnapshot.openPrice?.let { "시가 ${it.stripTrailingZeros().toPlainString()}" },
            tradingSnapshot.highPrice?.let { "고가 ${it.stripTrailingZeros().toPlainString()}" },
            tradingSnapshot.lowPrice?.let { "저가 ${it.stripTrailingZeros().toPlainString()}" }
        ).joinToString(", ")
        if (rangeText.isNotBlank()) {
            append(rangeText)
        }
        tradingSnapshot.volume?.let { volume ->
            if (isNotEmpty()) append(", ")
            append("거래량 ${"%,d".format(volume)}")
        }
        if (isEmpty()) {
            append("당일 거래 세부 지표는 제한적")
        }
    }

    val fundamentalSignal = buildList {
        fundamentals.marketCap?.let { add("시총 ${it.stripTrailingZeros().toPlainString()}") }
        fundamentals.trailingPe?.let { add("PER ${it.stripTrailingZeros().toPlainString()}배") }
        fundamentals.forwardPe?.let { add("선행 PER ${it.stripTrailingZeros().toPlainString()}배") }
        fundamentals.priceToBook?.let { add("PBR ${it.stripTrailingZeros().toPlainString()}배") }
        fundamentals.eps?.let { add("EPS ${it.stripTrailingZeros().toPlainString()}") }
        fundamentals.bps?.let { add("BPS ${it.stripTrailingZeros().toPlainString()}") }
        fundamentals.operatingMarginRatio?.let {
            add("영업이익률 ${(it * BigDecimal("100")).stripTrailingZeros().toPlainString()}%")
        }
        fundamentals.returnOnEquityRatio?.let {
            add("ROE ${(it * BigDecimal("100")).stripTrailingZeros().toPlainString()}%")
        }
    }.joinToString(", ").ifBlank { "확보된 펀더멘털 지표가 제한적" }

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
