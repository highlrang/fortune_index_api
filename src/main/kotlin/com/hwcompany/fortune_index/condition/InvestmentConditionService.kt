package com.hwcompany.fortune_index.condition

import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.domain.model.DailyInvestmentCondition
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.GanzhiCalculator
import com.hwcompany.fortune_index.tarot.TarotCard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class InvestmentConditionService(
    private val investmentConditionRepository: InvestmentConditionRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun getTodayCondition(
        authenticatedUser: AuthenticatedUser,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): InvestmentConditionResponse {
        val conditionDate = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        val saved = investmentConditionRepository
            .findTopByUserIdAndConditionDateAndDeletedFalseOrderByCreatedAtDescIdDesc(
                userId = authenticatedUser.userId,
                conditionDate = conditionDate
            )

        return saved?.toResponse(customized = true)
            ?: todayInvestmentIndex(now).toResponse(customized = false)
    }

    @Transactional
    fun updateTodayCondition(
        authenticatedUser: AuthenticatedUser,
        request: UpdateInvestmentConditionRequest,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): InvestmentConditionResponse {
        val totalScore = request.totalScore.takeIf { it in SCORE_RANGE }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "totalScore must be between 0 and 100")
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}") }
        val seoulNow = now.withZoneSameInstant(SEOUL_ZONE_ID)
        val conditionDate = seoulNow.toLocalDate()
        val base = investmentConditionRepository
            .findTopByUserIdAndConditionDateAndDeletedFalseOrderByCreatedAtDescIdDesc(
                userId = authenticatedUser.userId,
                conditionDate = conditionDate
            )
            ?.toSnapshot()
            ?: todayInvestmentIndex(seoulNow)

        investmentConditionRepository.findAllByUserIdAndConditionDateAndDeletedFalse(
            userId = authenticatedUser.userId,
            conditionDate = conditionDate
        ).forEach {
            it.deleted = true
            it.deletedAt = LocalDateTime.now()
        }

        val saved = investmentConditionRepository.save(
            DailyInvestmentCondition(
                user = user,
                conditionDate = conditionDate,
                totalScore = totalScore,
                summary = summarize(totalScore),
                dailyGanji = base.dailyGanji,
                sajuScore = base.sajuScore,
                tarotCardName = base.tarotCardName,
                tarotScore = base.tarotScore
            )
        )

        return saved.toResponse(customized = true)
    }

    private fun todayInvestmentIndex(now: ZonedDateTime): InvestmentConditionSnapshot {
        val seoulNow = now.withZoneSameInstant(SEOUL_ZONE_ID)
        val dayPillar = GanzhiCalculator.calculate(seoulNow.toLocalDateTime(), SEOUL_ZONE_ID).day
        val tarotCard = TarotCard.deck()[Math.floorMod(seoulNow.toLocalDate().toEpochDay().toInt(), TarotCard.entries.size)]
        val dailyGanji = dayPillar.heavenlyStem.labelKo() + dayPillar.earthlyBranch.labelKo()
        val sajuScore = 40 + Math.floorMod(dayPillar.heavenlyStem.ordinal * 12 + dayPillar.earthlyBranch.ordinal, 46)
        val tarotScore = 40 + Math.floorMod(tarotCard.ordinal * 7 + seoulNow.dayOfMonth, 46)
        val totalScore = (sajuScore + tarotScore) / 2

        return InvestmentConditionSnapshot(
            conditionDate = seoulNow.toLocalDate(),
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
            totalScore >= 80 -> "마음이 비교적 가볍고 흐름이 잘 풀리는 날"
            totalScore >= 65 -> "서두르지 않고 차분히 살피기 좋은 날"
            totalScore >= 50 -> "조용히 상황을 지켜보며 감을 익히기 좋은 날"
            totalScore >= 35 -> "한 번 더 생각하고 천천히 움직이는 편이 좋은 날"
            else -> "무리하지 말고 마음부터 쉬게 해 주는 편이 좋은 날"
        }

    private fun DailyInvestmentCondition.toResponse(customized: Boolean): InvestmentConditionResponse =
        InvestmentConditionResponse(
            totalScore = totalScore,
            summary = summary,
            customized = customized,
            fortune = InvestmentConditionFortuneSnapshot(
                dailyGanji = dailyGanji,
                score = sajuScore
            ),
            tarot = InvestmentConditionTarotSnapshot(
                cardName = tarotCardName,
                score = tarotScore
            )
        )

    private fun DailyInvestmentCondition.toSnapshot(): InvestmentConditionSnapshot =
        InvestmentConditionSnapshot(
            conditionDate = conditionDate,
            totalScore = totalScore,
            summary = summary,
            dailyGanji = dailyGanji,
            sajuScore = sajuScore,
            tarotCardName = tarotCardName,
            tarotScore = tarotScore
        )

    private fun InvestmentConditionSnapshot.toResponse(customized: Boolean): InvestmentConditionResponse =
        InvestmentConditionResponse(
            totalScore = totalScore,
            summary = summary,
            customized = customized,
            fortune = InvestmentConditionFortuneSnapshot(
                dailyGanji = dailyGanji,
                score = sajuScore
            ),
            tarot = InvestmentConditionTarotSnapshot(
                cardName = tarotCardName,
                score = tarotScore
            )
        )

    private data class InvestmentConditionSnapshot(
        val conditionDate: LocalDate,
        val totalScore: Int,
        val summary: String,
        val dailyGanji: String,
        val sajuScore: Int,
        val tarotCardName: String,
        val tarotScore: Int
    )

    private companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val SCORE_RANGE = 0..100
    }
}
