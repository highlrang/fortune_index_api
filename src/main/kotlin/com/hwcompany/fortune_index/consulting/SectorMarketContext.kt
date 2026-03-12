package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.market.StockInfo
import java.math.BigDecimal

data class SectorMarketContext(
    val sector: String,
    val referenceSignal: String,
    val sectorBias: String,
    val dataReliability: String,
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
        "개별 종목의 당일 움직임을 섹터 해석용 참고 신호로만 단순화한 데이터"
    }

    return SectorMarketContext(
        sector = sector,
        referenceSignal = referenceSignal,
        sectorBias = sectorBias,
        dataReliability = dataReliability
    )
}
