package com.hwcompany.fortune_index.astrology

import com.hwcompany.fortune_index.domain.model.WesternZodiacSign
import com.hwcompany.fortune_index.profile.AstrologyAngleResponse
import com.hwcompany.fortune_index.profile.AstrologyHouseResponse
import com.hwcompany.fortune_index.profile.AstrologyPlanetResponse
import com.hwcompany.fortune_index.profile.AstrologyProfileResponse
import com.hwcompany.fortune_index.zodiac.TodayZodiacFortuneResponse
import com.hwcompany.fortune_index.zodiac.ZodiacAspectResponse
import com.hwcompany.fortune_index.zodiac.ZodiacPlanetPositionResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.springframework.stereotype.Service
import swisseph.SweConst
import swisseph.SweDate
import swisseph.SwissEph

@Service
class AstrologyService {
    private val swissEph: SwissEph by lazy { SwissEph() }

    fun calculateNatalChart(
        birthDate: LocalDate,
        birthTime: LocalTime,
        latitude: Double,
        longitude: Double,
        houseSystem: Char = DEFAULT_HOUSE_SYSTEM
    ): AstrologyProfileResponse {
        val utcDateTime = birthDate.atTime(birthTime)
            .atZone(DEFAULT_ZONE_ID)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        val julianDayUt = toJulianDayUtc(utcDateTime)

        val cusps = DoubleArray(13)
        val ascmc = DoubleArray(10)
        val houseResult = swissEph.swe_houses(
            julianDayUt,
            0,
            latitude,
            longitude,
            houseSystem.code,
            cusps,
            ascmc
        )
        require(houseResult != SweConst.ERR) { "failed to calculate houses" }

        val natalPlanets = NATAL_PLANETS.map { planet ->
            calculatePlanet(
                julianDayUt = julianDayUt,
                planet = planet,
                cusps = cusps
            )
        }
        val currentJulianDayUt = toJulianDayUtc(LocalDateTime.now(ZoneOffset.UTC))
        val transits = TRANSIT_PLANETS.map { planet ->
            calculatePlanet(
                julianDayUt = currentJulianDayUt,
                planet = planet,
                cusps = cusps
            )
        }

        return AstrologyProfileResponse(
            houseSystem = "Placidus",
            utcBirthDateTime = utcDateTime.toString(),
            ascendant = toAngleResponse(ascmc[0]),
            secondHouse = toHouseResponse(2, cusps[2], natalPlanets),
            eighthHouse = toHouseResponse(8, cusps[8], natalPlanets),
            natalPlanets = natalPlanets,
            transits = transits
        )
    }

    fun calculateDailyZodiacFortune(
        date: LocalDate,
        zoneId: ZoneId = DEFAULT_ZONE_ID,
        latitude: Double,
        longitude: Double,
        locationName: String,
        baseTime: LocalTime = DEFAULT_DAILY_FORTUNE_BASE_TIME
    ): TodayZodiacFortuneResponse {
        val baseKstDateTime = date.atTime(baseTime)
        val utcDateTime = baseKstDateTime.atZone(zoneId)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        val julianDayUt = toJulianDayUtc(utcDateTime)
        val planets = DAILY_FORTUNE_PLANETS.map { planet ->
            calculateSkyPlanet(
                julianDayUt = julianDayUt,
                planet = planet
            )
        }
        val moon = planets.first { it.body == "MOON" }
        val aspect = calculateAspect(
            left = planets.first { it.body == "VENUS" },
            right = planets.first { it.body == "JUPITER" }
        )

        return TodayZodiacFortuneResponse(
            date = date.toString(),
            baseDateTimeKst = baseKstDateTime.toString(),
            baseDateTimeUtc = utcDateTime.toString(),
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            moonSign = moon.sign,
            moonEnglishName = moon.englishName,
            marketMood = marketMood(moon.sign),
            aspect = aspect,
            summary = buildDailySummary(moon.sign, aspect),
            guidance = buildDailyGuidance(moon.sign, aspect),
            planets = planets
        )
    }

    private fun calculatePlanet(
        julianDayUt: Double,
        planet: AstrologyPlanet,
        cusps: DoubleArray
    ): AstrologyPlanetResponse {
        val position = DoubleArray(6)
        val errorBuffer = StringBuffer()
        val result = swissEph.swe_calc_ut(
            julianDayUt,
            planet.id,
            PLANET_CALCULATION_FLAGS,
            position,
            errorBuffer
        )
        require(result != SweConst.ERR) { "failed to calculate ${planet.displayName}: $errorBuffer" }

        val longitude = normalizeLongitude(position[0])
        val sign = WesternZodiacSign.fromLongitude(longitude)
        return AstrologyPlanetResponse(
            body = planet.displayName,
            sign = sign.sign,
            englishName = sign.englishName,
            degree = sign.degreeInSign(longitude),
            longitude = longitude,
            house = findHouse(longitude, cusps)
        )
    }

    private fun calculateSkyPlanet(
        julianDayUt: Double,
        planet: AstrologyPlanet
    ): ZodiacPlanetPositionResponse {
        val position = DoubleArray(6)
        val errorBuffer = StringBuffer()
        val result = swissEph.swe_calc_ut(
            julianDayUt,
            planet.id,
            PLANET_CALCULATION_FLAGS,
            position,
            errorBuffer
        )
        require(result != SweConst.ERR) { "failed to calculate ${planet.displayName}: $errorBuffer" }

        val longitude = normalizeLongitude(position[0])
        val sign = WesternZodiacSign.fromLongitude(longitude)
        return ZodiacPlanetPositionResponse(
            body = planet.displayName,
            sign = sign.sign,
            englishName = sign.englishName,
            degree = sign.degreeInSign(longitude),
            longitude = longitude
        )
    }

    private fun toHouseResponse(
        houseNumber: Int,
        cuspLongitude: Double,
        planets: List<AstrologyPlanetResponse>
    ): AstrologyHouseResponse {
        val normalizedCusp = normalizeLongitude(cuspLongitude)
        val sign = WesternZodiacSign.fromLongitude(normalizedCusp)
        return AstrologyHouseResponse(
            houseNumber = houseNumber,
            sign = sign.sign,
            englishName = sign.englishName,
            cuspDegree = sign.degreeInSign(normalizedCusp),
            cuspLongitude = normalizedCusp,
            planets = planets.filter { it.house == houseNumber }
        )
    }

    private fun toAngleResponse(longitude: Double): AstrologyAngleResponse {
        val normalizedLongitude = normalizeLongitude(longitude)
        val sign = WesternZodiacSign.fromLongitude(normalizedLongitude)
        return AstrologyAngleResponse(
            sign = sign.sign,
            englishName = sign.englishName,
            degree = sign.degreeInSign(normalizedLongitude),
            absoluteLongitude = normalizedLongitude
        )
    }

    private fun toJulianDayUtc(utcDateTime: LocalDateTime): Double {
        val decimalHour = utcDateTime.hour +
            (utcDateTime.minute / 60.0) +
            (utcDateTime.second / 3600.0) +
            (utcDateTime.nano / 3_600_000_000_000.0)
        return SweDate.getJulDay(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            decimalHour,
            SweDate.SE_GREG_CAL
        )
    }

    private fun findHouse(longitude: Double, cusps: DoubleArray): Int {
        for (house in 1..12) {
            val start = normalizeLongitude(cusps[house])
            val end = normalizeLongitude(cusps[if (house == 12) 1 else house + 1])
            if (isWithinArc(longitude, start, end)) {
                return house
            }
        }
        return 12
    }

    private fun isWithinArc(longitude: Double, start: Double, end: Double): Boolean =
        if (start <= end) {
            longitude >= start && longitude < end
        } else {
            longitude >= start || longitude < end
        }

    private fun normalizeLongitude(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0) normalized += 360.0
        return normalized
    }

    private fun calculateAspect(
        left: ZodiacPlanetPositionResponse,
        right: ZodiacPlanetPositionResponse
    ): ZodiacAspectResponse? {
        val angle = angularDifference(left.longitude, right.longitude)
        val match = ASPECTS.minByOrNull { kotlin.math.abs(angle - it.angle) } ?: return null
        val orb = kotlin.math.abs(angle - match.angle)
        if (orb > match.maxOrb) {
            return null
        }
        return ZodiacAspectResponse(
            between = "${left.body}-${right.body}",
            type = match.label,
            angle = angle,
            orb = orb,
            interpretation = match.interpretation
        )
    }

    private fun angularDifference(left: Double, right: Double): Double {
        val diff = kotlin.math.abs(left - right) % 360.0
        return if (diff > 180.0) 360.0 - diff else diff
    }

    private fun marketMood(moonSign: String): String =
        when (moonSign) {
            "양자리", "사자자리", "사수자리" -> "확장 흐름이 강하지만 속도 조절이 필요한 날"
            "황소자리", "처녀자리", "염소자리" -> "안정과 실속을 우선하면 유리한 날"
            "쌍둥이자리", "천칭자리", "물병자리" -> "정보 탐색과 비교 판단이 잘 맞는 날"
            else -> "심리 변동을 살피며 방어적으로 접근하기 좋은 날"
        }

    private fun buildDailySummary(moonSign: String, aspect: ZodiacAspectResponse?): String {
        val aspectSummary = when (aspect?.type) {
            "TRINE" -> "금성과 목성이 부드럽게 연결돼 자금 흐름을 낙관적으로 보기 쉬워요."
            "SEXTILE" -> "금성과 목성이 협력 각을 이루어 기회를 차분히 고르기 좋아요."
            "CONJUNCTION" -> "금성과 목성이 한 점에 모여 기대감이 커질 수 있어요."
            "SQUARE" -> "금성과 목성이 긴장 각을 이루어 욕심과 현실 점검이 충돌하기 쉬워요."
            "OPPOSITION" -> "금성과 목성이 마주 서서 수익 기대와 리스크 인식이 흔들릴 수 있어요."
            else -> "큰 각도 이벤트보다 오늘의 분위기 결을 읽는 편이 중요해요."
        }
        return "${moonSign}의 분위기가 시장 심리를 주도하는 날이에요. $aspectSummary"
    }

    private fun buildDailyGuidance(moonSign: String, aspect: ZodiacAspectResponse?): String =
        when {
            aspect?.type in setOf("TRINE", "SEXTILE") ->
                "$moonSign 흐름이 비교적 안정적이니, 이미 검토한 자산을 다시 점검하며 기회를 선별해 보세요."
            aspect?.type in setOf("SQUARE", "OPPOSITION") ->
                "$moonSign 기운에 감정이 실리기 쉬우니, 신규 진입보다 보유 자산의 위험 관리 기준을 먼저 확인하세요."
            else ->
                "$moonSign 분위기에 휩쓸리기보다 자산 운용 기준을 한 줄로 적어 보고 움직이는 편이 좋습니다."
        }

    private data class AstrologyPlanet(
        val id: Int,
        val displayName: String
    )

    private data class AspectDefinition(
        val angle: Double,
        val label: String,
        val maxOrb: Double,
        val interpretation: String
    )

    private companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        const val DEFAULT_HOUSE_SYSTEM: Char = 'P'
        const val PLANET_CALCULATION_FLAGS: Int = SweConst.SEFLG_MOSEPH
        val DEFAULT_DAILY_FORTUNE_BASE_TIME: LocalTime = LocalTime.NOON
        val NATAL_PLANETS = listOf(
            AstrologyPlanet(SweConst.SE_SUN, "SUN"),
            AstrologyPlanet(SweConst.SE_MOON, "MOON"),
            AstrologyPlanet(SweConst.SE_VENUS, "VENUS"),
            AstrologyPlanet(SweConst.SE_JUPITER, "JUPITER")
        )
        val DAILY_FORTUNE_PLANETS = listOf(
            AstrologyPlanet(SweConst.SE_SUN, "SUN"),
            AstrologyPlanet(SweConst.SE_MOON, "MOON"),
            AstrologyPlanet(SweConst.SE_VENUS, "VENUS"),
            AstrologyPlanet(SweConst.SE_JUPITER, "JUPITER")
        )
        val TRANSIT_PLANETS = listOf(
            AstrologyPlanet(SweConst.SE_JUPITER, "JUPITER"),
            AstrologyPlanet(SweConst.SE_SATURN, "SATURN")
        )
        val ASPECTS = listOf(
            AspectDefinition(0.0, "CONJUNCTION", 6.0, "기대 심리가 과열되기 쉬운 배치예요."),
            AspectDefinition(60.0, "SEXTILE", 5.0, "협력과 탐색이 자연스럽게 맞물리는 배치예요."),
            AspectDefinition(90.0, "SQUARE", 5.0, "욕심과 현실 점검이 충돌하기 쉬운 배치예요."),
            AspectDefinition(120.0, "TRINE", 6.0, "흐름을 부드럽게 활용하기 좋은 배치예요."),
            AspectDefinition(180.0, "OPPOSITION", 6.0, "판단이 양쪽으로 흔들릴 수 있는 배치예요.")
        )
    }
}
