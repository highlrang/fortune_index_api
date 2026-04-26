package com.hwcompany.fortune_index.zodiac

import com.hwcompany.fortune_index.astrology.AstrologyService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class TodayZodiacFortuneService(
    private val astrologyService: AstrologyService
) {
    fun getTodayFortune(now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): TodayZodiacFortuneResponse =
        getFortuneByDate(now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate())

    @Cacheable(cacheNames = [TODAY_ZODIAC_FORTUNE_CACHE], key = "#date.toString()")
    fun getFortuneByDate(date: LocalDate): TodayZodiacFortuneResponse =
        astrologyService.calculateDailyZodiacFortune(
            date = date,
            zoneId = SEOUL_ZONE_ID,
            latitude = SEOUL_LATITUDE,
            longitude = SEOUL_LONGITUDE,
            locationName = SEOUL_LOCATION_NAME
        )

    companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val SEOUL_LOCATION_NAME = "서울"
        private const val SEOUL_LATITUDE = 37.56
        private const val SEOUL_LONGITUDE = 126.97
        const val TODAY_ZODIAC_FORTUNE_CACHE = "todayZodiacFortune"
    }
}
