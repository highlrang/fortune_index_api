package com.hwcompany.fortune_index.saju.investment

data class SajuInvestmentFeatures(
    val baseTraits: List<String>,
    val dynamicSignals: List<String>,
    val riskFlags: List<String>,
    val hiddenElementRatios: Map<String, Int>,
    val branchStageCounts: BranchStageCounts,
    val relationSignals: List<RelationSignal>,
    val confidence: Int,
    val energyBalance: EnergyBalance,
    val energyBalanceDescription: String
)

enum class EnergyBalance {
    STRONG, WEAK, BALANCED
}

internal fun EnergyBalance.toDescription(): String = when (this) {
    EnergyBalance.STRONG ->
        "타고난 기운이 강해 고집을 버리고 포트폴리오를 분산할 때 수익이 나는 성향입니다."
    EnergyBalance.WEAK ->
        "타고난 기운이 약해 대세 흐름과 검증된 우량주에 편승하여 집중할 때 수익이 나는 성향입니다."
    EnergyBalance.BALANCED ->
        "기운이 고르게 균형 잡혀 공격과 수비를 상황에 따라 유연하게 조절할 수 있는 성향입니다."
}

data class BranchStageCounts(
    val saengji: Int,
    val wangji: Int,
    val myoji: Int
)

data class RelationSignal(
    val type: String,
    val code: String,
    val target: String,
    val weight: Int,
    val messageKey: String
)
