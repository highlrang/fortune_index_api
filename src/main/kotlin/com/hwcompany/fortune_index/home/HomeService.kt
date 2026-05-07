package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.daily.DailyFortuneCacheService
import com.hwcompany.fortune_index.domain.model.HomeTarotDrawHistory
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotDrawResult
import com.hwcompany.fortune_index.zodiac.TodayZodiacFortuneService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class HomeService(
    private val dailyFortuneCacheService: DailyFortuneCacheService,
    private val todayZodiacFortuneService: TodayZodiacFortuneService,
    private val userRepository: UserRepository,
    private val tarotDeckService: TarotDeckService,
    private val homeTarotDrawHistoryRepository: HomeTarotDrawHistoryRepository,
    private val objectMapper: ObjectMapper
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

    @Transactional(readOnly = true)
    fun getDailyTarotDraw(
        authenticatedUser: AuthenticatedUser,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): HomeDailyTarotDrawResponse {
        requireActiveUser(authenticatedUser)
        val drawDate = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        val history = homeTarotDrawHistoryRepository.findByUserIdAndDrawDate(authenticatedUser.userId, drawDate)

        return if (history == null) {
            HomeDailyTarotDrawResponse(
                drawDate = drawDate,
                drawn = false,
                canDraw = true,
                drawnAt = null,
                deckVersionId = null,
                cards = emptyList()
            )
        } else {
            history.toResponse()
        }
    }

    @Transactional
    fun drawDailyTarotCards(
        authenticatedUser: AuthenticatedUser,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): HomeDailyTarotDrawResponse {
        val user = requireActiveUser(authenticatedUser)
        val drawDate = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        homeTarotDrawHistoryRepository.findByUserIdAndDrawDate(authenticatedUser.userId, drawDate)
            ?.let { return it.toResponse() }

        val deckVersionId = user.preferredTarotDeckId ?: tarotDeckService.getActiveMainDeckVersionId()
        val cards = tarotDeckService.drawReading(
            subscriptionTier = user.subscriptionTier,
            deckVersionId = deckVersionId,
            indices = null
        ).cards

        val history = homeTarotDrawHistoryRepository.save(
            HomeTarotDrawHistory(
                user = user,
                drawDate = drawDate,
                drawnAt = LocalDateTime.now(SEOUL_ZONE_ID),
                deckVersionId = deckVersionId,
                cardsJson = objectMapper.writeValueAsString(cards)
            )
        )
        return history.toResponse()
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

    private fun requireActiveUser(authenticatedUser: AuthenticatedUser): User {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${authenticatedUser.userId}")
            }
        if (user.accountStatus != UserAccountStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "inactive user")
        }
        return user
    }

    private fun HomeTarotDrawHistory.toResponse(): HomeDailyTarotDrawResponse =
        HomeDailyTarotDrawResponse(
            drawDate = drawDate,
            drawn = true,
            canDraw = false,
            drawnAt = drawnAt,
            deckVersionId = deckVersionId,
            cards = objectMapper.readValue(cardsJson, TAROT_DRAW_RESULT_LIST_TYPE)
        )

    private companion object {
        private val SEOUL_ZONE_ID = java.time.ZoneId.of("Asia/Seoul")
        private val TAROT_DRAW_RESULT_LIST_TYPE = object : TypeReference<List<TarotDrawResult>>() {}
    }
}
