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
    val imageUrl: String?,
    val videoUrl: String?
)

data class SajuProfileResponse(
    val palza: List<String>,
    val ohang: SajuOhangResponse,
    val sipsung: List<String>,
    val daeun: String,
    val sewun: String
)

data class SajuOhangResponse(
    val wood: Int,
    val fire: Int,
    val earth: Int,
    val metal: Int,
    val water: Int
)
