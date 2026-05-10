package com.hwcompany.fortune_index.profile

data class MyProfileDetailsResponse(
    val birthTarot: BirthTarotResponse?,
    val saju: SajuProfileResponse?,
    val zodiac: ZodiacProfileResponse?,
    val astrology: AstrologyProfileResponse?
)

data class BirthTarotResponse(
    val deckVersionId: String,
    val name: String,
    val koreanName: String?,
    val number: Int,
    val cardMeaning: String,
    val cardDescription: String,
    val birthMeaning: String,
    val birthDescription: String,
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

data class ZodiacProfileResponse(
    val sign: String?,
    val englishName: String?,
    val dateRange: String?,
    val element: String?,
    val keyword: String?,
    val summary: String?,
    val traits: List<String>?
)

data class AstrologyProfileResponse(
    val houseSystem: String,
    val utcBirthDateTime: String,
    val ascendant: AstrologyAngleResponse,
    val secondHouse: AstrologyHouseResponse,
    val eighthHouse: AstrologyHouseResponse,
    val natalPlanets: List<AstrologyPlanetResponse>,
    val transits: List<AstrologyPlanetResponse>
)

data class AstrologyAngleResponse(
    val sign: String,
    val englishName: String,
    val degree: Double,
    val absoluteLongitude: Double
)

data class AstrologyHouseResponse(
    val houseNumber: Int,
    val sign: String,
    val englishName: String,
    val cuspDegree: Double,
    val cuspLongitude: Double,
    val planets: List<AstrologyPlanetResponse>
)

data class AstrologyPlanetResponse(
    val body: String,
    val sign: String,
    val englishName: String,
    val degree: Double,
    val longitude: Double,
    val house: Int
)
