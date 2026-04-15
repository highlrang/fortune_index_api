package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.saju.GanzhiCalculator
import com.hwcompany.fortune_index.tarot.TarotCard
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class HomeService(
) {
    fun getSummary(now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)): HomeSummaryResponse {
        val snapshot = todayInvestmentIndex(now)
        return HomeSummaryResponse(
            investmentIndex = HomeInvestmentIndexResponse(
                totalScore = snapshot.totalScore,
                summary = summarize(snapshot.totalScore),
                fortune = HomeFortuneSnapshot(
                    dailyGanji = snapshot.dailyGanji,
                    score = snapshot.sajuScore
                ),
                tarot = HomeTarotSnapshot(
                    cardName = snapshot.tarotCardName,
                    score = snapshot.tarotScore
                )
            )
        )
    }

    private fun todayInvestmentIndex(now: ZonedDateTime): HomeInvestmentIndexSnapshot {
        val seoulNow = now.withZoneSameInstant(SEOUL_ZONE_ID)
        val dayPillar = GanzhiCalculator.calculate(seoulNow.toLocalDateTime(), SEOUL_ZONE_ID).day
        val tarotCard = TarotCard.deck()[Math.floorMod(seoulNow.toLocalDate().toEpochDay().toInt(), TarotCard.entries.size)]
        val dailyGanji = dayPillar.heavenlyStem.labelKo() + dayPillar.earthlyBranch.labelKo()
        val sajuScore = 40 + Math.floorMod(dayPillar.heavenlyStem.ordinal * 12 + dayPillar.earthlyBranch.ordinal, 46)
        val tarotScore = 40 + Math.floorMod(tarotCard.ordinal * 7 + seoulNow.dayOfMonth, 46)
        val totalScore = (sajuScore + tarotScore) / 2

        return HomeInvestmentIndexSnapshot(
            totalScore = totalScore,
            dailyGanji = dailyGanji,
            sajuScore = sajuScore,
            tarotCardName = tarotCard.koreanDisplayName,
            tarotScore = tarotScore
        )
    }

    private fun summarize(totalScore: Int): String =
        when {
            totalScore >= 80 -> "마음이 비교적 가볍고 흐름이 잘 풀리는 날"
            totalScore >= 65 -> "서두르지 않고 차분히 살피기 좋은 날"
            totalScore >= 50 -> "조용히 상황을 지켜보며 감을 익히기 좋은 날"
            totalScore >= 35 -> "한 번 더 생각하고 천천히 움직이는 편이 좋은 날"
            else -> "무리하지 말고 마음부터 쉬게 해 주는 편이 좋은 날"
        }

    private data class HomeInvestmentIndexSnapshot(
        val totalScore: Int,
        val dailyGanji: String,
        val sajuScore: Int,
        val tarotCardName: String,
        val tarotScore: Int
    )

    private companion object {
        private val SEOUL_ZONE_ID = java.time.ZoneId.of("Asia/Seoul")
    }
}
