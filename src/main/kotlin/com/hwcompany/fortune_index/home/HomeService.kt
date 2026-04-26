package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.daily.DailyFortuneCacheService
import com.hwcompany.fortune_index.zodiac.TodayZodiacFortuneService
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class HomeService(
    private val dailyFortuneCacheService: DailyFortuneCacheService,
    private val todayZodiacFortuneService: TodayZodiacFortuneService
) {
    fun getSummary(now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): HomeSummaryResponse {
        val date = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        val snapshot = dailyFortuneCacheService.getDailyFortune(date)
        val zodiacFortune = todayZodiacFortuneService.getFortuneByDate(date)
        return HomeSummaryResponse(
            summary = snapshot.summary,
            saju = HomeCardSnapshot(
                name = snapshot.dailyGanji,
                summary = sajuSummary(snapshot.sajuScore)
            ),
            tarot = HomeCardSnapshot(
                name = snapshot.tarotCardName,
                summary = tarotSummary(snapshot.tarotScore)
            ),
            zodiac = HomeCardSnapshot(
                name = zodiacFortune.moonSign,
                summary = zodiacSummary(zodiacFortune.marketMood)
            )
        )
    }

    private fun sajuSummary(score: Int): String =
        when {
            score >= 80 -> "결단이 잘 맞는 날"
            score >= 65 -> "안정적으로 풀리는 날"
            score >= 50 -> "차분히 살피기 좋은 날"
            else -> "속도를 늦추는 게 좋은 날"
        }

    private fun tarotSummary(score: Int): String =
        when {
            score >= 80 -> "전환 흐름이 강한 날"
            score >= 65 -> "점검과 이동이 좋은 날"
            score >= 50 -> "관망이 유리한 날"
            else -> "잠시 쉬어가는 날"
        }

    private fun zodiacSummary(marketMood: String): String =
        when {
            marketMood.contains("안정") -> "안정 흐름"
            marketMood.contains("정보") -> "탐색 흐름"
            marketMood.contains("심리") -> "조심할 흐름"
            else -> "별자리 흐름"
        }

    private companion object {
        private val SEOUL_ZONE_ID = java.time.ZoneId.of("Asia/Seoul")
    }
}
