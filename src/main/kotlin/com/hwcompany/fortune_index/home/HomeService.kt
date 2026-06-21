package com.hwcompany.fortune_index.home

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.common.Zodiac
import com.hwcompany.fortune_index.daily.DailyFortuneCacheService
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.HomeTarotDrawHistory
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.domain.model.WesternZodiacSign
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.GanzhiCalculator
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotCardMetadataRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotDrawResult
import com.hwcompany.fortune_index.tarot.resolveBirthTarotCard
import com.hwcompany.fortune_index.zodiac.TodayZodiacFortuneService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    private val tarotCardMetadataRepository: TarotCardMetadataRepository,
    private val homeTarotDrawHistoryRepository: HomeTarotDrawHistoryRepository,
    private val homeSummaryInterpretationService: HomeSummaryInterpretationService,
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
        val ganzhiResult = GanzhiCalculator.calculate(date.atStartOfDay(), SEOUL_ZONE_ID)
        val dayPillar = ganzhiResult.day
        val monthlyPillar = ganzhiResult.month
        val dayGanji = dayPillar.toSajuGanji()
        val tarotCard = TarotCard.deck()[Math.floorMod(date.toEpochDay().toInt(), TarotCard.entries.size)]
        return HomeSummaryResponse(
            summary = dailySummary(snapshot.totalScore, snapshot.summary, ctx),
            saju = HomeCardSnapshot(
                name = "${dayGanji.koreanName}일",
                summary = sajuSummary(snapshot.sajuScore, ctx),
                symbol = sajuSymbol(dayPillar),
                detail = sajuDetail(dayGanji, dayPillar, snapshot.sajuScore, ctx)
            ),
            tarot = HomeCardSnapshot(
                name = tarotCard.koreanDisplayName,
                summary = tarotSummary(snapshot.tarotScore, ctx),
                symbol = tarotSymbol(tarotCard),
                detail = tarotDetail(tarotCard, snapshot.tarotScore, ctx)
            ),
            zodiac = HomeCardSnapshot(
                name = zodiacFortune.moonSign,
                summary = zodiacSummary(zodiacFortune.moonSign, ctx),
                detail = zodiacDetail(zodiacFortune.moonSign, ctx)
            ),
            monthlyFortune = buildMonthlyFortune(monthlyPillar)
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
            ?.let { throw ResponseStatusException(HttpStatus.CONFLICT, "해당 날짜의 홈 타로 뽑기가 이미 저장되었습니다: $drawDate") }

        val deckVersionId = request.tarotDeckVersionId.trim()
        val cards = try {
            tarotDeckService.drawReading(
                subscriptionTier = user.subscriptionTier,
                deckVersionId = deckVersionId,
                indices = request.tarotIndices
            ).cards
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message ?: "타로 뽑기 요청이 올바르지 않습니다.", ex)
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
            throw ResponseStatusException(HttpStatus.CONFLICT, "해당 날짜의 홈 타로 뽑기가 이미 저장되었습니다: $drawDate", ex)
        }
        return history.toResponse()
    }

    private fun loadUserContext(userId: Long): UserContext? {
        val user = userRepository.findById(userId).orElse(null) ?: return null
        val birthDateTime = LocalDateTime.of(
            user.birthInfo.birthDate,
            user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
        )
        val natalDayPillar = GanzhiCalculator.calculate(birthDateTime, SEOUL_ZONE_ID).day
        val birthTarotCard = runCatching {
            TarotCard.fromCode(user.birthTarotCardCode ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString()).code)
        }.getOrElse {
            resolveBirthTarotCard(user.birthInfo.birthDate.toString())
        }
        val westernZodiac = user.westernZodiac ?: WesternZodiacSign.from(user.birthInfo.birthDate)
        // TODO: Rebuild trend from the future consulting history feedback table.
        return UserContext(
            riskProfile = user.investmentRiskProfile,
            trend = Trend.FLAT,
            natalDayGanji = natalDayPillar.toSajuGanji(),
            natalDayPillar = natalDayPillar,
            birthTarotCard = birthTarotCard,
            westernZodiac = westernZodiac
        )
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

    private fun sajuSymbol(dayPillar: Pillar): HomeCardSymbol {
        val zodiac = Zodiac.entries.first { it.branch == dayPillar.earthlyBranch }
        return HomeCardSymbol(
            label = "${stemColor(dayPillar.heavenlyStem)} ${animalLabel(zodiac)}",
            description = "${stemMood(dayPillar.heavenlyStem)} 기운과 ${animalDescription(zodiac)} 흐름이 만나는 상징"
        )
    }

    private fun sajuDetail(dayGanji: SajuGanji, dayPillar: Pillar, score: Int, ctx: UserContext?): HomeCardDetail {
        val reviewed = findReviewedContent(HomeSummaryInterpretationCategory.SAJU_DAY, dayGanji.code, ctx)
        val dailyBody = reviewed?.dailyBody
            ?.normalizeTodaySajuDayLabel(dayGanji)
            ?: sajuDailyBody(dayGanji, dayPillar, score)
        return HomeCardDetail(
            body = dailyBody,
            dailyBody = dailyBody,
            personalBody = null,
            points = null
        )
    }

    private fun sajuDailyBody(dayGanji: SajuGanji, dayPillar: Pillar, score: Int): String =
        "${dayGanji.koreanName}일은 ${stemMarketMood(dayPillar.heavenlyStem)} 천간과 ${branchMarketMood(dayGanji.zodiac)} 지지가 만난 흐름입니다. " +
            "${sajuMarketPsychology(dayPillar.heavenlyStem, dayGanji.zodiac)} " +
            sajuMarketStance(score, dayGanji.zodiac)

    private fun buildMonthlyFortune(pillar: Pillar): HomeMonthlyFortuneSnapshot {
        val ganji = pillar.toSajuGanji()
        return HomeMonthlyFortuneSnapshot(
            ganjiLabel = "${ganji.koreanName}월",
            summary = "${ganji.koreanName}월은 ${stemMarketMood(pillar.heavenlyStem)} 천간과 ${branchMarketMood(ganji.zodiac)} 지지 기운이 흐릅니다. " +
                sajuMarketPsychology(pillar.heavenlyStem, ganji.zodiac)
        )
    }

    private fun tarotSymbol(card: TarotCard): HomeCardSymbol =
        HomeCardSymbol(
            label = tarotSymbolLabel(card),
            description = card.uprightMeaning
        )

    private fun tarotDetail(card: TarotCard, score: Int, ctx: UserContext?): HomeCardDetail {
        val reviewed = findReviewedContent(HomeSummaryInterpretationCategory.TAROT_CARD, card.code, ctx)
        val dailyBody = reviewed?.dailyBody?.localizeTarotCardName(card) ?: tarotDailyBody(card, score)
        val fallbackPersonalBody = ctx?.let { personalTarotBody(card, it.birthTarotCard) }
        val personalBody = reviewed?.personalBodyTemplate
            ?.let { template -> ctx?.let { renderReviewedTemplate(template, tarotPlaceholders(card, it.birthTarotCard)) } }
            ?.hideBirthTarotCardMention()
            ?: fallbackPersonalBody
        val deckVersionId = tarotDeckService.getActiveMainDeckVersionId()
        val dbCard = tarotCardMetadataRepository.findByDeckVersion_IdAndSelectedIndex(deckVersionId, card.ordinal)
        return HomeCardDetail(
            body = combineDetailBodies(dailyBody, personalBody),
            dailyBody = dailyBody,
            personalBody = personalBody,
            points = reviewed?.points ?: tarotPoints(card, score, ctx),
            imageUrl = dbCard?.imageUrl ?: card.imageUrl,
            videoUrl = dbCard?.videoUrl ?: card.videoUrl
        )
    }

    private fun zodiacDetail(moonSign: String, ctx: UserContext?): HomeCardDetail {
        val reviewed = findReviewedContent(HomeSummaryInterpretationCategory.ZODIAC_MOON, moonSign, ctx)
        val dailyBody = reviewed?.dailyBody ?: zodiacDailyBody(moonSign)
        return HomeCardDetail(
            body = dailyBody,
            dailyBody = dailyBody,
            personalBody = null,
            points = null
        )
    }

    private fun stemMarketMood(stem: HeavenlyStem): String =
        when (stem) {
            HeavenlyStem.GAP -> "새 흐름을 세우는"
            HeavenlyStem.EUL -> "작게 조정하는"
            HeavenlyStem.BYEONG -> "심리를 빠르게 키우는"
            HeavenlyStem.JEONG -> "한 재료에 집중하는"
            HeavenlyStem.MU -> "무겁게 중심을 잡는"
            HeavenlyStem.GI -> "현실 관리를 중시하는"
            HeavenlyStem.GYEONG -> "판단을 선명히 하는"
            HeavenlyStem.SIN -> "선별과 정리에 강한"
            HeavenlyStem.IM -> "큰 흐름을 보는"
            HeavenlyStem.GYE -> "미세한 변화를 보는"
        }

    private fun branchMarketMood(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "정보와 속도의"
            Zodiac.CHUK -> "축적과 버티기의"
            Zodiac.IN -> "초반 추진의"
            Zodiac.MYO -> "섬세한 조정의"
            Zodiac.JIN -> "조건을 쌓는"
            Zodiac.SA -> "숨은 재료가 드러나는"
            Zodiac.O -> "활기와 속도의"
            Zodiac.MI -> "정리와 보완의"
            Zodiac.SIN -> "전환에 민감한"
            Zodiac.YU -> "선별과 정리의"
            Zodiac.SUL -> "방어와 원칙의"
            Zodiac.HAE -> "다음 국면을 보는"
        }

    private fun sajuMarketPsychology(stem: HeavenlyStem, zodiac: Zodiac): String =
        when {
            stem in setOf(HeavenlyStem.BYEONG, HeavenlyStem.JEONG) || zodiac in setOf(Zodiac.SA, Zodiac.O) ->
                "단기 수익을 빨리 확인하고 싶은 마음이 커지기 쉽습니다."
            stem in setOf(HeavenlyStem.MU, HeavenlyStem.GI) || zodiac in setOf(Zodiac.CHUK, Zodiac.JIN, Zodiac.MI, Zodiac.SUL) ->
                "빠르게 움직이기보다 자리를 지키려는 분위기가 강합니다."
            stem in setOf(HeavenlyStem.GYEONG, HeavenlyStem.SIN) || zodiac in setOf(Zodiac.SIN, Zodiac.YU) ->
                "애매한 선택지를 줄이고 핵심만 남기기 좋은 흐름입니다."
            else ->
                "새 신호를 찾는 마음과 기다리려는 마음이 섞입니다."
        }

    private fun sajuMarketStance(score: Int, zodiac: Zodiac): String =
        when {
            score >= 80 -> "전부 베팅하기보다 이익을 지키는 쪽이 안정적입니다."
            score >= 65 -> "포지션을 넓히기보다 필요한 만큼만 조정하세요."
            zodiac in setOf(Zodiac.CHUK, Zodiac.MI, Zodiac.SUL) -> "시장을 이기려 하기보다 방어 비중을 편하게 두세요."
            else -> "움직임을 줄이고 판단의 무게를 확인하세요."
        }

    private fun tarotDailyBody(card: TarotCard, score: Int): String {
        val action = tarotInvestmentAction(card, score)
        return "${card.koreanDisplayName}은 투자 관점에서 '$action' 신호로 읽을 수 있습니다. " +
            tarotActionMeaning(action) + " " +
            "감정보다 포지션의 안정성을 먼저 확인하세요."
    }

    private fun tarotInvestmentAction(card: TarotCard, score: Int): String =
        when {
            card in setOf(TarotCard.FOUR_OF_WANDS, TarotCard.TEMPERANCE, TarotCard.THE_EMPEROR, TarotCard.KING_OF_PENTACLES) -> "포트폴리오 유지"
            card in setOf(TarotCard.NINE_OF_CUPS, TarotCard.TEN_OF_CUPS, TarotCard.SIX_OF_WANDS, TarotCard.THE_SUN, TarotCard.THE_WORLD) -> "수익 실현"
            card in setOf(TarotCard.FOUR_OF_SWORDS, TarotCard.THE_HERMIT, TarotCard.THE_HANGED_MAN, TarotCard.THE_MOON) -> "현금 확보"
            card in setOf(TarotCard.THE_FOOL, TarotCard.THE_MAGICIAN, TarotCard.THE_CHARIOT, TarotCard.ACE_OF_WANDS) && score >= 65 -> "과감한 베팅"
            card.suit == com.hwcompany.fortune_index.tarot.TarotSuit.PENTACLES -> "포트폴리오 유지"
            card.suit == com.hwcompany.fortune_index.tarot.TarotSuit.SWORDS -> "현금 확보"
            card.suit == com.hwcompany.fortune_index.tarot.TarotSuit.WANDS && score >= 65 -> "과감한 베팅"
            card.suit == com.hwcompany.fortune_index.tarot.TarotSuit.CUPS && score >= 65 -> "수익 실현"
            else -> "포트폴리오 유지"
        }

    private fun tarotActionMeaning(action: String): String =
        when (action) {
            "현금 확보" -> "불확실한 자리를 줄이고 여력을 남기라는 뜻입니다."
            "수익 실현" -> "성과가 있다면 일부를 현실화하라는 뜻입니다."
            "과감한 베팅" -> "충동이 아니라 준비된 기준 위에서 움직이라는 뜻입니다."
            else -> "기반을 흔들지 말고 균형을 점검하라는 뜻입니다."
        }

    private fun tarotPoints(card: TarotCard, score: Int, ctx: UserContext?): List<String> {
        val action = tarotInvestmentAction(card, score)
        return listOf(
            "매수/매도: ${tarotPositionGuide(action)}",
            "리스크 관리: ${tarotRiskGuide(action, score)}",
            "마인드셋: ${tarotMindsetGuide(card, ctx)}"
        )
    }

    private fun tarotPositionGuide(action: String): String =
        when (action) {
            "현금 확보" -> "신규 진입보다 불확실한 포지션 축소에 집중하세요."
            "수익 실현" -> "오른 자리는 일부 익절하고 남은 비중만 유지하세요."
            "과감한 베팅" -> "확인된 자리만 작게 진입하고 추격 매수는 피하세요."
            else -> "신규 진입보다 보유 비중 유지에 집중하세요."
        }

    private fun tarotRiskGuide(action: String, score: Int): String =
        when (action) {
            "현금 확보" -> "손절 기준을 넓히지 말고 현금 비중을 먼저 확보하세요."
            "수익 실현" -> "수익 구간의 되돌림 한도를 미리 정하세요."
            "과감한 베팅" -> if (score >= 80) "진입 전 손절 라인을 숫자로 고정하세요." else "비중을 절반 이하로 낮춰 변동성을 줄이세요."
            else -> "리밸런싱은 하되 총 위험 노출은 늘리지 마세요."
        }

    private fun tarotMindsetGuide(card: TarotCard, ctx: UserContext?): String =
        if (ctx?.trend == Trend.DOWN) {
            "손실 회복 욕구보다 ${tarotSymbolLabel(card)} 기준을 먼저 보세요."
        } else {
            "시장이 지루해도 ${tarotSymbolLabel(card)} 흐름을 벗어난 거래는 줄이세요."
        }

    private fun zodiacDailyBody(moonSign: String): String {
        val bias = zodiacMarketBias(moonSign)
        val pace = zodiacInvestmentPace(moonSign)
        return "달이 $moonSign 흐름에 머물러 $bias 쪽에 관심이 모이기 쉽습니다. " +
            "투자 호흡은 ${pace}에 더 잘 맞습니다. " +
            zodiacMarketCaution(moonSign)
    }

    private fun zodiacMarketBias(moonSign: String): String =
        when (moonSign) {
            "양자리", "사자자리", "사수자리" -> "성장주, 모멘텀 자산, 강한 테마"
            "황소자리", "처녀자리", "염소자리" -> "가치주, 배당주, 실물 자산처럼 기반이 보이는 자산"
            "쌍둥이자리", "천칭자리", "물병자리" -> "기술주, 플랫폼, 정보 흐름이 빠른 섹터"
            "게자리", "전갈자리", "물고기자리" -> "방어주, 소비 안정 섹터, 심리적 안전감이 큰 자산"
            else -> "기준이 분명한 자산"
        }

    private fun zodiacInvestmentPace(moonSign: String): String =
        when (moonSign) {
            "양자리", "쌍둥이자리", "사자자리", "사수자리", "물병자리" -> "짧은 관찰과 빠른 의사결정"
            "황소자리", "처녀자리", "염소자리" -> "느린 호흡과 중장기 점검"
            "게자리", "전갈자리", "물고기자리" -> "감정 변동을 낮춘 신중한 관망"
            else -> "무리하지 않는 속도 조절"
        }

    private fun zodiacMarketCaution(moonSign: String): String =
        when (moonSign) {
            "황소자리" -> "단기 급등보다 버틸 근거를 확인하세요."
            "물병자리" -> "아이디어가 수익 구조로 이어지는지 검증하세요."
            "양자리", "사자자리", "사수자리" -> "강한 종목일수록 손절 기준을 먼저 세우세요."
            "쌍둥이자리", "천칭자리" -> "뉴스보다 확인된 가격 흐름을 우선하세요."
            "게자리", "전갈자리", "물고기자리" -> "불안과 근거를 분리하세요."
            else -> "감당 가능한 변동폭을 먼저 확인하세요."
        }
    
    private fun personalTarotBody(todayCard: TarotCard, birthCard: TarotCard): String {
        val relation = when {
            todayCard == birthCard -> "익숙한 ${tarotSymbolLabel(birthCard)} 성향이 오늘도 강하게 반복됩니다."
            todayCard.arcanaType == birthCard.arcanaType -> "기본 성향의 ${tarotSymbolLabel(birthCard)} 흐름이 오늘 카드와 같은 결로 움직입니다."
            else -> "기본 성향의 ${tarotSymbolLabel(birthCard)} 흐름과 오늘 카드의 ${tarotSymbolLabel(todayCard)} 메시지가 서로 다른 방향을 봅니다."
        }
        return "$relation 따라서 오늘은 '${tarotSymbolLabel(todayCard)}' 메시지를 우선 보세요."
    }

    private fun combineDetailBodies(dailyBody: String, personalBody: String?): String =
        listOfNotNull(dailyBody, personalBody).joinToString("\n\n")

    private fun findReviewedContent(
        category: HomeSummaryInterpretationCategory,
        code: String,
        ctx: UserContext?
    ): ReviewedHomeSummaryInterpretation? =
        homeSummaryInterpretationService.findReviewedContent(
            category = category,
            code = code,
            variant = ctx?.riskProfile?.name
        )

    private fun renderReviewedTemplate(template: String, placeholders: Map<String, String>): String? {
        val rendered = placeholders.entries.fold(template) { current, (key, value) ->
            current.replace("{$key}", value)
        }.trim()
        return rendered
            .takeIf(String::isNotBlank)
            ?.takeUnless { UNSUPPORTED_PLACEHOLDER_REGEX.containsMatchIn(it) }
    }

    private fun tarotPlaceholders(todayCard: TarotCard, birthCard: TarotCard): Map<String, String> =
        mapOf(
            "birthCard" to birthCard.koreanDisplayName,
            "birthCardKeyword" to tarotSymbolLabel(birthCard),
            "todayCard" to todayCard.koreanDisplayName,
            "todayCardKeyword" to tarotSymbolLabel(todayCard)
        )

    private fun String.normalizeTodaySajuDayLabel(dayGanji: SajuGanji): String =
        replace(Regex("${Regex.escape(dayGanji.koreanName)}\\s*일주"), "${dayGanji.koreanName}일")

    private fun String.hideBirthTarotCardMention(): String =
        replace(BIRTH_TAROT_CARD_WITH_NAME_REGEX, "개인 성향의 ")
            .replace("탄생 카드의 ", "개인 성향의 ")
            .replace("탄생 카드", "개인 성향")

    private fun String.localizeTarotCardName(card: TarotCard): String =
        replace(card.displayName, card.koreanDisplayName)

    private fun stemColor(stem: HeavenlyStem): String =
        when (stem) {
            HeavenlyStem.GAP,
            HeavenlyStem.EUL -> "푸른"
            HeavenlyStem.BYEONG,
            HeavenlyStem.JEONG -> "붉은"
            HeavenlyStem.MU,
            HeavenlyStem.GI -> "노란"
            HeavenlyStem.GYEONG,
            HeavenlyStem.SIN -> "하얀"
            HeavenlyStem.IM,
            HeavenlyStem.GYE -> "검은"
        }

    private fun stemMood(stem: HeavenlyStem): String =
        when (stem) {
            HeavenlyStem.GAP,
            HeavenlyStem.EUL -> "성장과 조율의"
            HeavenlyStem.BYEONG,
            HeavenlyStem.JEONG -> "활력과 표현의"
            HeavenlyStem.MU,
            HeavenlyStem.GI -> "안정과 축적의"
            HeavenlyStem.GYEONG,
            HeavenlyStem.SIN -> "정리와 판단의"
            HeavenlyStem.IM,
            HeavenlyStem.GYE -> "관찰과 유연함의"
        }

    private fun animalLabel(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "쥐"
            Zodiac.CHUK -> "소"
            Zodiac.IN -> "호랑이"
            Zodiac.MYO -> "토끼"
            Zodiac.JIN -> "용"
            Zodiac.SA -> "뱀"
            Zodiac.O -> "말"
            Zodiac.MI -> "양"
            Zodiac.SIN -> "원숭이"
            Zodiac.YU -> "닭"
            Zodiac.SUL -> "개"
            Zodiac.HAE -> "돼지"
        }

    private fun animalDescription(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "빠르게 감지하고 기회를 찾는"
            Zodiac.CHUK -> "차분하게 버티며 흐름을 쌓아가는"
            Zodiac.IN -> "초반 추진력으로 길을 여는"
            Zodiac.MYO -> "섬세하게 균형을 살피는"
            Zodiac.JIN -> "큰 흐름을 움직이는"
            Zodiac.SA -> "안쪽의 변화를 민감하게 읽는"
            Zodiac.O -> "활력 있게 앞으로 나아가는"
            Zodiac.MI -> "속도를 낮추고 기반을 다지는"
            Zodiac.SIN -> "상황을 기민하게 바꾸는"
            Zodiac.YU -> "기준을 세우고 정리하는"
            Zodiac.SUL -> "원칙을 지키며 방어하는"
            Zodiac.HAE -> "흐름을 받아들이며 다음을 준비하는"
        }

    private fun Pillar.toSajuGanji(): SajuGanji =
        SajuGanji.of(heavenlyStem, Zodiac.entries.first { it.branch == earthlyBranch })

    private fun tarotSymbolLabel(card: TarotCard): String =
        when (card) {
            TarotCard.THE_FOOL -> "새로운 시작"
            TarotCard.THE_MAGICIAN -> "의지와 실행"
            TarotCard.THE_HIGH_PRIESTESS -> "직관과 지혜"
            TarotCard.THE_EMPRESS -> "풍요와 성장"
            TarotCard.THE_EMPEROR -> "질서와 책임"
            TarotCard.THE_HIEROPHANT -> "전통과 기준"
            TarotCard.THE_LOVERS -> "선택과 조화"
            TarotCard.THE_CHARIOT -> "전진과 통제"
            TarotCard.STRENGTH -> "인내와 힘"
            TarotCard.THE_HERMIT -> "성찰과 탐구"
            TarotCard.WHEEL_OF_FORTUNE -> "전환과 타이밍"
            TarotCard.JUSTICE -> "균형과 책임"
            TarotCard.THE_HANGED_MAN -> "유예와 관점"
            TarotCard.DEATH -> "정리와 변화"
            TarotCard.TEMPERANCE -> "절제와 조율"
            TarotCard.THE_DEVIL -> "집착과 경고"
            TarotCard.THE_TOWER -> "충격과 재구성"
            TarotCard.THE_STAR -> "희망과 회복"
            TarotCard.THE_MOON -> "불확실성과 검증"
            TarotCard.THE_SUN -> "명확성과 활력"
            TarotCard.JUDGEMENT -> "각성과 결산"
            TarotCard.THE_WORLD -> "완성과 마무리"
            else -> card.uprightMeaning.substringBefore(",")
        }

    private fun requireActiveUser(authenticatedUser: AuthenticatedUser): User {
        val user = userRepository.findById(authenticatedUser.userId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: ${authenticatedUser.userId}")
            }
        if (user.accountStatus != UserAccountStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "활성화된 사용자가 아닙니다.")
        }
        return user
    }

    private fun validateSelectedTarotIndices(tarotIndices: List<Int>) {
        if (tarotIndices.size != HOME_TAROT_DRAW_COUNT) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "tarotIndices는 정확히 ${HOME_TAROT_DRAW_COUNT}장의 카드를 포함해야 합니다."
            )
        }
        if (tarotIndices.distinct().size != tarotIndices.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotIndices에는 중복된 카드가 포함될 수 없습니다.")
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

    private data class UserContext(
        val riskProfile: InvestmentRiskProfile,
        val trend: Trend,
        val natalDayGanji: SajuGanji,
        val natalDayPillar: Pillar,
        val birthTarotCard: TarotCard,
        val westernZodiac: WesternZodiacSign
    )

    private enum class Trend { UP, FLAT, DOWN }

    private companion object {
        private const val HOME_TAROT_DRAW_COUNT = 3
        private val SEOUL_ZONE_ID = java.time.ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private val TAROT_DRAW_RESULT_LIST_TYPE = object : TypeReference<List<TarotDrawResult>>() {}
        private val UNSUPPORTED_PLACEHOLDER_REGEX = Regex("\\{[A-Za-z][A-Za-z0-9]*}")
        private val BIRTH_TAROT_CARD_WITH_NAME_REGEX = Regex("\\S+\\s+탄생 카드의\\s+")
    }
}
