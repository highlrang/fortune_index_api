package com.hwcompany.fortune_index.zodiac

data class TodayZodiacFortuneResponse(
    val date: String,
    val baseDateTimeKst: String,
    val baseDateTimeUtc: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val moonSign: String,
    val moonEnglishName: String,
    val marketMood: String,
    val aspect: ZodiacAspectResponse?,
    val summary: String,
    val guidance: String,
    val planets: List<ZodiacPlanetPositionResponse>
)

data class ZodiacAspectResponse(
    val between: String,
    val type: String,
    val angle: Double,
    val orb: Double,
    val interpretation: String
)

data class ZodiacPlanetPositionResponse(
    val body: String,
    val sign: String,
    val englishName: String,
    val degree: Double,
    val longitude: Double
)
