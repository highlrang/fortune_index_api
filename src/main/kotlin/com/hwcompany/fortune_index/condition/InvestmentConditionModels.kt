package com.hwcompany.fortune_index.condition

data class InvestmentConditionResponse(
    val totalScore: Int,
    val summary: String,
    val customized: Boolean,
    val fortune: InvestmentConditionFortuneSnapshot,
    val tarot: InvestmentConditionTarotSnapshot
)

data class InvestmentConditionFortuneSnapshot(
    val dailyGanji: String,
    val score: Int
)

data class InvestmentConditionTarotSnapshot(
    val cardName: String,
    val score: Int
)

data class UpdateInvestmentConditionRequest(
    val totalScore: Int
)
