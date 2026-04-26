package com.hwcompany.fortune_index.home

data class HomeSummaryResponse(
    val summary: String,
    val saju: HomeCardSnapshot,
    val tarot: HomeCardSnapshot,
    val zodiac: HomeCardSnapshot
)

data class HomeCardSnapshot(
    val name: String,
    val summary: String
)
