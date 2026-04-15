package com.hwcompany.fortune_index.home

data class HomeSummaryResponse(
    val investmentIndex: HomeInvestmentIndexResponse,
)

data class HomeInvestmentIndexResponse(
    val totalScore: Int,
    val summary: String,
    val fortune: HomeFortuneSnapshot,
    val tarot: HomeTarotSnapshot
)

data class HomeFortuneSnapshot(
    val dailyGanji: String,
    val score: Int
)

data class HomeTarotSnapshot(
    val cardName: String,
    val score: Int
)
