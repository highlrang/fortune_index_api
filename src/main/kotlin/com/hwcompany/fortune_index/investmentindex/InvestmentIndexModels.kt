package com.hwcompany.fortune_index.investmentindex

data class TotalIndexResponse(
    val totalScore: Int,
    val detail: ScoreDetail
)

data class ScoreDetail(
    val selectedMarket: String,
    val marketScore: Int,
    val marketRawValue: Double,
    val sajuScore: Int,
    val dailyGanji: String,
    val tarotScore: Int,
    val tarotCardName: String
)

