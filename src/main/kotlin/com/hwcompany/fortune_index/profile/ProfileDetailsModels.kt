package com.hwcompany.fortune_index.profile

data class MyProfileDetailsResponse(
    val birthTarot: BirthTarotResponse?,
    val saju: SajuProfileResponse?
)

data class BirthTarotResponse(
    val deckVersionId: String,
    val name: String,
    val koreanName: String?,
    val number: Int,
    val meaning: String,
    val description: String,
    val imageUrl: String?,
    val videoUrl: String?
)

data class SajuProfileResponse(
    val palza: List<String>,
    val ohang: SajuOhangResponse,
    val ilju: SajuInsightResponse,
    val wolji: SajuInsightResponse,
    val daeun: FortuneInsightResponse,
    val sewun: FortuneInsightResponse
)

data class SajuOhangResponse(
    val wood: Int,
    val fire: Int,
    val earth: Int,
    val metal: Int,
    val water: Int
)

data class SajuInsightResponse(
    val name: String,
    val summary: String
)

data class FortuneInsightResponse(
    val name: String,
    val summary: String
)
