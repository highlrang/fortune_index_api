package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.daily.DailyFortuneCacheService
import com.hwcompany.fortune_index.domain.model.HomeTarotDrawHistory
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotDrawResult
import com.hwcompany.fortune_index.zodiac.TodayZodiacFortuneService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class HomeService(
    private val dailyFortuneCacheService: DailyFortuneCacheService,
    private val todayZodiacFortuneService: TodayZodiacFortuneService,
    private val userRepository: UserRepository,
    private val tarotDeckService: TarotDeckService,
    private val homeTarotDrawHistoryRepository: HomeTarotDrawHistoryRepository,
    private val objectMapper: ObjectMapper
) {
    fun getSummary(
        authenticatedUser: AuthenticatedUser? = null,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): HomeSummaryResponse {
        val date = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        val snapshot = dailyFortuneCacheService.getDailyFortune(date)
        val zodiacFortune = todayZodiacFortuneService.getFortuneByDate(date)
        val ctx = authenticatedUser?.let { loadUserContext(it.userId) }
        return HomeSummaryResponse(
            summary = dailySummary(snapshot.totalScore, snapshot.summary, ctx),
            saju = HomeCardSnapshot(
                name = snapshot.dailyGanji,
                summary = sajuSummary(snapshot.sajuScore, ctx)
            ),
            tarot = HomeCardSnapshot(
                name = snapshot.tarotCardName,
                summary = tarotSummary(snapshot.tarotScore, ctx)
            ),
            zodiac = HomeCardSnapshot(
                name = zodiacFortune.moonSign,
                summary = zodiacSummary(zodiacFortune.moonSign, ctx)
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
    fun saveDailyTarotCards(
        authenticatedUser: AuthenticatedUser,
        request: SaveHomeDailyTarotDrawRequest,
        now: ZonedDateTime = ZonedDateTime.now(SEOUL_ZONE_ID)
    ): HomeDailyTarotDrawResponse {
        validateSelectedTarotIndices(request.tarotIndices)
        val user = requireActiveUser(authenticatedUser)
        val drawDate = now.withZoneSameInstant(SEOUL_ZONE_ID).toLocalDate()
        homeTarotDrawHistoryRepository.findByUserIdAndDrawDate(authenticatedUser.userId, drawDate)
            ?.let { throw ResponseStatusException(HttpStatus.CONFLICT, "home tarot draw already saved for date: $drawDate") }

        val deckVersionId = request.tarotDeckVersionId.trim()
        val cards = try {
            tarotDeckService.drawReading(
                subscriptionTier = user.subscriptionTier,
                deckVersionId = deckVersionId,
                indices = request.tarotIndices
            ).cards
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "invalid tarot draw request", ex)
        }

        val history = try {
            homeTarotDrawHistoryRepository.save(
                HomeTarotDrawHistory(
                    user = user,
                    drawDate = drawDate,
                    drawnAt = LocalDateTime.now(SEOUL_ZONE_ID),
                    deckVersionId = deckVersionId,
                    cardsJson = objectMapper.writeValueAsString(cards)
                )
            )
        } catch (ex: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "home tarot draw already saved for date: $drawDate", ex)
        }
        return history.toResponse()
    }

    private fun loadUserContext(userId: Long): UserContext? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        // TODO: Rebuild trend from the future consulting history feedback table.
        return UserContext(riskProfile = user.investmentRiskProfile, trend = Trend.FLAT)
    }

    private fun dailySummary(totalScore: Int, defaultSummary: String, ctx: UserContext?): String {
        if (ctx == null) {
            return defaultSummary
        }
        return when {
            totalScore >= 80 -> when (ctx.trend) {
                Trend.UP -> when (ctx.riskProfile) {
                    InvestmentRiskProfile.AGGRESSIVE -> "최근 흐름을 과감하게 이어가기 좋은 날"
                    InvestmentRiskProfile.STABLE -> "최근 흐름을 안정적으로 이어가기 좋은 날"
                }
                Trend.DOWN -> "기회가 보여도 리스크 기준을 먼저 세울 날"
                Trend.FLAT -> when (ctx.riskProfile) {
                    InvestmentRiskProfile.AGGRESSIVE -> "강한 흐름을 선별해 활용하기 좋은 날"
                    InvestmentRiskProfile.STABLE -> "좋은 흐름을 차분히 확인하며 움직일 날"
                }
            }
            totalScore >= 65 -> when (ctx.trend) {
                Trend.UP -> "최근 성과를 지키며 한 번 더 살펴볼 날"
                Trend.DOWN -> "속도보다 회복과 점검에 집중할 날"
                Trend.FLAT -> when (ctx.riskProfile) {
                    InvestmentRiskProfile.AGGRESSIVE -> "움직이되 기준을 분명히 잡을 날"
                    InvestmentRiskProfile.STABLE -> "차분한 판단으로 안정감을 챙길 날"
                }
            }
            totalScore >= 50 -> when (ctx.trend) {
                Trend.UP -> "기회를 고르며 무리하지 않게 이어갈 날"
                Trend.DOWN -> "관망하며 최근 선택을 복기할 날"
                Trend.FLAT -> when (ctx.riskProfile) {
                    InvestmentRiskProfile.AGGRESSIVE -> "작게 탐색하며 흐름을 확인할 날"
                    InvestmentRiskProfile.STABLE -> "상황을 살피며 기준을 지킬 날"
                }
            }
            else -> when (ctx.trend) {
                Trend.UP -> "수익을 지키며 속도를 낮출 날"
                Trend.DOWN -> "손실 방어와 휴식에 무게를 둘 날"
                Trend.FLAT -> when (ctx.riskProfile) {
                    InvestmentRiskProfile.AGGRESSIVE -> "충동 매매를 줄이고 쉬어갈 날"
                    InvestmentRiskProfile.STABLE -> "무리하지 않고 안정적으로 쉬어갈 날"
                }
            }
        }
    }

    private fun sajuSummary(score: Int, ctx: UserContext?): String = when {
        score >= 80 -> when (ctx?.trend) {
            Trend.UP -> if (ctx.riskProfile == InvestmentRiskProfile.AGGRESSIVE) "여세를 몰아갈 수 있는 날" else "결단을 믿고 밀어붙일 날"
            Trend.DOWN -> "기회처럼 보여도 한 번 더 확인할 날"
            else -> "결단이 잘 맞는 날"
        }
        score >= 65 -> when (ctx?.trend) {
            Trend.UP -> "최근 흐름을 이어가기 좋은 날"
            Trend.DOWN -> "안정 위주로 점검하기 좋은 날"
            else -> "안정적으로 풀리는 날"
        }
        score >= 50 -> when (ctx?.trend) {
            Trend.UP -> "차분히 살피며 기회를 찾기 좋은 날"
            Trend.DOWN -> "지금은 관망이 유리한 날"
            else -> "차분히 살피기 좋은 날"
        }
        else -> when (ctx?.trend) {
            Trend.UP -> "속도를 잠깐 늦춰도 흐름은 살아있는 날"
            Trend.DOWN -> "전략을 재정비하며 쉬어가는 날"
            else -> "속도를 늦추는 게 좋은 날"
        }
    }

    private fun tarotSummary(score: Int, ctx: UserContext?): String = when {
        score >= 80 -> when (ctx?.trend) {
            Trend.UP -> if (ctx.riskProfile == InvestmentRiskProfile.AGGRESSIVE) "전환 흐름에 올라탈 수 있는 날" else "전환 흐름을 차분히 활용할 날"
            Trend.DOWN -> "전환 신호가 있지만 방향을 먼저 확인할 날"
            else -> "전환 흐름이 강한 날"
        }
        score >= 65 -> when (ctx?.trend) {
            Trend.UP -> "최근 흐름을 점검하며 이동하기 좋은 날"
            Trend.DOWN -> "점검에 집중하며 이동은 신중하게"
            else -> "점검과 이동이 좋은 날"
        }
        score >= 50 -> when (ctx?.trend) {
            Trend.UP -> "기다리며 기회를 선별하기 좋은 날"
            Trend.DOWN -> "관망하며 최근 결과를 복기할 날"
            else -> "관망이 유리한 날"
        }
        else -> when (ctx?.trend) {
            Trend.UP -> "멈추면서도 다음을 준비하는 날"
            Trend.DOWN -> "확실하게 쉬어가는 날"
            else -> "잠시 쉬어가는 날"
        }
    }

    private fun zodiacSummary(moonSign: String, ctx: UserContext?): String = when (ctx?.trend) {
        Trend.UP -> zodiacUpText(moonSign)
        Trend.DOWN -> zodiacDownText(moonSign)
        else -> zodiacBaseText(moonSign)
    }

    private fun zodiacBaseText(moonSign: String): String = when (moonSign) {
        "양자리" -> "돌파력이 앞서는 날"
        "황소자리" -> "실속을 챙기기 좋은 날"
        "쌍둥이자리" -> "정보를 탐색하기 좋은 날"
        "게자리" -> "감정 변동을 살피는 날"
        "사자자리" -> "표현력이 돋보이는 날"
        "처녀자리" -> "세밀하게 정리하기 좋은 날"
        "천칭자리" -> "균형을 맞춰가기 좋은 날"
        "전갈자리" -> "집중력을 살리기 좋은 날"
        "사수자리" -> "확장과 모험이 잘 맞는 날"
        "염소자리" -> "현실적으로 접근하기 좋은 날"
        "물병자리" -> "시각을 새롭게 열기 좋은 날"
        "물고기자리" -> "직감에 귀 기울이기 좋은 날"
        else -> "별자리 흐름"
    }

    private fun zodiacUpText(moonSign: String): String = when (moonSign) {
        "양자리" -> "돌파력을 활용하기 좋은 날"
        "황소자리" -> "착실하게 수확하기 좋은 날"
        "쌍둥이자리" -> "정보를 빠르게 활용하기 좋은 날"
        "게자리" -> "감각적으로 움직이기 좋은 날"
        "사자자리" -> "자신감을 살려 움직이기 좋은 날"
        "처녀자리" -> "꼼꼼하게 수확을 챙기기 좋은 날"
        "천칭자리" -> "균형 잡힌 흐름을 이어가기 좋은 날"
        "전갈자리" -> "집중력을 발휘해 수익을 챙길 날"
        "사수자리" -> "확장 흐름을 과감하게 활용할 날"
        "염소자리" -> "현실적인 전략으로 수확하기 좋은 날"
        "물병자리" -> "새로운 관점으로 기회를 잡기 좋은 날"
        "물고기자리" -> "직감을 믿고 움직이기 좋은 날"
        else -> "별자리 흐름"
    }

    private fun zodiacDownText(moonSign: String): String = when (moonSign) {
        "양자리" -> "돌파보다 점검이 먼저인 날"
        "황소자리" -> "실속 위주로 방어하기 좋은 날"
        "쌍둥이자리" -> "정보 수집에 집중하며 기다릴 날"
        "게자리" -> "감정에 휩쓸리지 않게 주의할 날"
        "사자자리" -> "자신감보다 검토가 먼저인 날"
        "처녀자리" -> "세밀한 점검으로 손실을 줄일 날"
        "천칭자리" -> "균형을 찾으며 방어적으로 접근할 날"
        "전갈자리" -> "집중력으로 리스크를 점검할 날"
        "사수자리" -> "확장보다 현실 점검이 먼저인 날"
        "염소자리" -> "현실적으로 위험 관리 기준을 확인할 날"
        "물병자리" -> "기존 포지션 점검이 먼저인 날"
        "물고기자리" -> "직감보다 데이터를 먼저 확인할 날"
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

    private fun validateSelectedTarotIndices(tarotIndices: List<Int>) {
        if (tarotIndices.size != HOME_TAROT_DRAW_COUNT) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "tarotIndices must contain exactly $HOME_TAROT_DRAW_COUNT cards"
            )
        }
        if (tarotIndices.distinct().size != tarotIndices.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotIndices must not contain duplicates")
        }
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

    private data class UserContext(val riskProfile: InvestmentRiskProfile, val trend: Trend)

    private enum class Trend { UP, FLAT, DOWN }

    private companion object {
        private const val HOME_TAROT_DRAW_COUNT = 3
        private val SEOUL_ZONE_ID = java.time.ZoneId.of("Asia/Seoul")
        private val TAROT_DRAW_RESULT_LIST_TYPE = object : TypeReference<List<TarotDrawResult>>() {}
    }
}
