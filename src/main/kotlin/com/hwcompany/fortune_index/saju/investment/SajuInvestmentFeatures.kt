package com.hwcompany.fortune_index.saju.investment

data class SajuInvestmentFeatures(
    val baseTraits: List<String>,
    val dynamicSignals: List<String>,
    val riskFlags: List<String>,
    val hiddenElementRatios: Map<String, Int>,
    val branchStageCounts: BranchStageCounts,
    val relationSignals: List<RelationSignal>,
    val confidence: Int
)

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
