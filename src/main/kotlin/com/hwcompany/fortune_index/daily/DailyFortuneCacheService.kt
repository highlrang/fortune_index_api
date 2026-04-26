package com.hwcompany.fortune_index.daily

import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.saju.GanzhiCalculator
import com.hwcompany.fortune_index.tarot.TarotCard
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class DailyFortuneCacheService {

    @Cacheable(cacheNames = [DAILY_FORTUNE_CACHE], key = "#date.toString()")
    fun getDailyFortune(date: LocalDate): DailyFortuneSnapshot {
        val seoulDateTime = date.atStartOfDay(SEOUL_ZONE_ID).toLocalDateTime()
        val dayPillar = GanzhiCalculator.calculate(seoulDateTime, SEOUL_ZONE_ID).day
        val tarotCard = TarotCard.deck()[Math.floorMod(date.toEpochDay().toInt(), TarotCard.entries.size)]
        val dailyGanji = dayPillar.heavenlyStem.labelKo() + dayPillar.earthlyBranch.labelKo()
        val sajuScore = 40 + Math.floorMod(dayPillar.heavenlyStem.ordinal * 12 + dayPillar.earthlyBranch.ordinal, 46)
        val tarotScore = 40 + Math.floorMod(tarotCard.ordinal * 7 + date.dayOfMonth, 46)
        val totalScore = (sajuScore + tarotScore) / 2

        return DailyFortuneSnapshot(
            date = date,
            totalScore = totalScore,
            summary = summarize(totalScore),
            dailyGanji = dailyGanji,
            sajuScore = sajuScore,
            tarotCardName = tarotCard.koreanDisplayName,
            tarotScore = tarotScore
        )
    }

    private fun summarize(totalScore: Int): String =
        when {
            totalScore >= 80 -> "흐름이 좋은 날"
            totalScore >= 65 -> "차분히 가면 좋은 날"
            totalScore >= 50 -> "상황을 살피기 좋은 날"
            totalScore >= 35 -> "천천히 움직일 날"
            else -> "쉬어가는 게 좋은 날"
        }

    companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        const val DAILY_FORTUNE_CACHE = "dailyFortune"
    }
}

data class DailyFortuneSnapshot(
    val date: LocalDate,
    val totalScore: Int,
    val summary: String,
    val dailyGanji: String,
    val sajuScore: Int,
    val tarotCardName: String,
    val tarotScore: Int
)
