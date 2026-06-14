package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.astrology.AstrologyService
import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.WesternZodiacSign
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.FiveElementBalance
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuCoreEnergy
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuInterpretationCategory
import com.hwcompany.fortune_index.saju.SajuInterpretationService
import com.hwcompany.fortune_index.saju.SajuPersistenceService
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.saju.TenStar
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotArcanaType
import com.hwcompany.fortune_index.tarot.TarotBirthCardInterpretation
import com.hwcompany.fortune_index.tarot.TarotBirthCardRepository
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotCardMetadataEntity
import com.hwcompany.fortune_index.tarot.TarotCardMetadataRepository
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.resolveBirthTarotCard
import com.hwcompany.fortune_index.tarot.toInterpretation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ProfileDetailsService(
    private val userRepository: UserRepository,
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
    private val sajuPersistenceService: SajuPersistenceService,
    private val astrologyService: AstrologyService,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository,
    private val tarotBirthCardRepository: TarotBirthCardRepository,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val sajuInterpretationService: SajuInterpretationService
) {
    @Transactional(readOnly = true)
    fun getProfileDetails(userId: Long): MyProfileDetailsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId") }

        val birthTarot = runCatching {
            buildBirthTarot(
                cardCode = user.birthTarotCardCode ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString()).code,
                preferredDeckVersionId = user.preferredTarotDeckId
            )
        }.onFailure { ex ->
            logger.warn(
                "Failed to build birth tarot. userId={}, birthDate={}, preferredDeckVersionId={}",
                user.id,
                user.birthInfo.birthDate,
                user.preferredTarotDeckId,
                ex
            )
        }.getOrNull()

        val saju = runCatching {
            if (sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(user.id)) == null) {
                sajuPersistenceService.saveInitialResult(user)
            }
            val birthDateTime = LocalDateTime.of(
                user.birthInfo.birthDate,
                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
            )
            buildSajuProfile(
                consultingResult = sajuAnalyzer.analyzeForConsulting(
                    birthDateTime = birthDateTime,
                    referenceDateTime = SeoulTime.now(),
                    zoneId = DEFAULT_ZONE_ID,
                    gender = user.gender
                )
            )
        }.getOrNull()

        val astrology = runCatching {
            val birthTime = user.birthInfo.birthTime
            val latitude = user.birthInfo.birthLatitude
            val longitude = user.birthInfo.birthLongitude
            if (birthTime == null || latitude == null || longitude == null) {
                null
            } else {
                astrologyService.calculateNatalChart(
                    birthDate = user.birthInfo.birthDate,
                    birthTime = birthTime,
                    latitude = latitude,
                    longitude = longitude
                ).copy(transits = emptyList())
            }
        }.onFailure { ex ->
            logger.warn(
                "Failed to build astrology profile. userId={}, birthDate={}, birthTime={}, latitude={}, longitude={}",
                user.id,
                user.birthInfo.birthDate,
                user.birthInfo.birthTime,
                user.birthInfo.birthLatitude,
                user.birthInfo.birthLongitude,
                ex
            )
        }.getOrNull()

        return MyProfileDetailsResponse(
            birthTarot = birthTarot,
            saju = saju,
            zodiac = buildZodiacProfile(user.westernZodiac ?: WesternZodiacSign.from(user.birthInfo.birthDate)),
            astrology = astrology
        )
    }

    private fun buildZodiacProfile(sign: WesternZodiacSign): ZodiacProfileResponse {
        return ZodiacProfileResponse(
            sign = sign.sign,
            englishName = sign.englishName,
            dateRange = sign.dateRange,
            element = sign.element,
            keyword = sign.keyword,
            summary = sign.profileSummary(),
            traits = sign.traits.zip(sign.traitDescriptions) { name, description ->
                ZodiacTraitResponse(name = name, description = description)
            }
        )
    }

    private fun WesternZodiacSign.profileSummary(): String =
        when (this) {
            WesternZodiacSign.ARIES ->
                "양자리는 기회가 보이면 빠르게 움직이는 실행형이에요. 재물 흐름에서도 망설임보다 선제적인 판단을 선호해요. 다만 속도가 빨라질수록 기준과 한도를 먼저 정해두면 추진력이 더 안정적으로 살아나요."
            WesternZodiacSign.TAURUS ->
                "황소자리는 안정감과 실속을 중시하는 축적형이에요. 급한 변화보다 검증된 가치와 꾸준한 성장을 편안하게 느껴요. 익숙한 선택에 머무르기 쉬우니 주기적으로 전제 조건이 바뀌었는지 살피면 좋아요."
            WesternZodiacSign.GEMINI ->
                "쌍둥이자리는 정보 흐름을 빠르게 읽는 탐색형이에요. 새로운 뉴스와 관점을 비교하면서 투자 아이디어를 넓혀가요. 선택지가 많아질수록 같은 기준으로 걸러내는 체크리스트가 판단을 도와줘요."
            WesternZodiacSign.CANCER ->
                "게자리는 심리적 안정과 보호를 우선하는 방어형이에요. 재물 판단에서도 생활 기반과 안전지대를 먼저 확인하려는 경향이 강해요. 불안한 날에는 시장 분위기와 실제 위험을 구분하면 감정 소모를 줄일 수 있어요."
            WesternZodiacSign.LEO ->
                "사자자리는 확신이 생기면 흐름을 주도하는 표현형이에요. 성장성과 존재감이 뚜렷한 선택에서 에너지가 크게 살아나요. 확신이 강할수록 반대 근거와 정리 기준을 함께 적어두면 판단이 균형을 잡아요."
            WesternZodiacSign.VIRGO ->
                "처녀자리는 숫자와 조건을 꼼꼼히 보는 분석형이에요. 작은 신호와 리스크를 세밀하게 점검하며 정돈된 결정을 선호해요. 분석이 길어질수록 실행 기준과 시간 제한을 두면 기회를 놓치지 않기 쉬워요."
            WesternZodiacSign.LIBRA ->
                "천칭자리는 여러 선택지 사이의 균형을 찾는 조율형이에요. 포트폴리오의 조화와 비중 감각을 중요하게 여기며 과열된 분위기를 조정하려 해요. 의견이 많을수록 최종 판단 기준 하나를 먼저 정해두면 결정이 선명해져요."
            WesternZodiacSign.SCORPIO ->
                "전갈자리는 겉보다 속의 구조를 깊게 보는 몰입형이에요. 표면적인 가격보다 숨은 리스크와 심리의 방향을 파고드는 힘이 강해요. 확신한 대상일수록 비중 제한과 휴식 규칙을 두면 몰입이 부담으로 커지는 것을 막을 수 있어요."
            WesternZodiacSign.SAGITTARIUS ->
                "사수자리는 큰 흐름과 성장 가능성을 보는 확장형이에요. 넓은 시장과 미래 테마를 읽으며 장기적인 기회를 찾는 데 강점이 있어요. 기대 수익이 클수록 하락 시나리오까지 함께 적어두면 낙관이 판단을 압도하지 않아요."
            WesternZodiacSign.CAPRICORN ->
                "염소자리는 목표와 지속 가능성을 따지는 계획형이에요. 단기 감정보다 구조, 책임, 장기 성과를 먼저 보며 안정적인 전략을 선호해요. 지나치게 보수적으로 굳지 않도록 작은 성장 비중을 열어두면 균형이 좋아져요."
            WesternZodiacSign.AQUARIUS ->
                "물병자리는 새로운 관점으로 시장 변화를 읽는 혁신형이에요. 기존 질서가 바뀌는 지점과 남들이 놓친 가능성을 찾는 힘이 돋보여요. 아이디어가 앞서갈수록 실제 실적과 검증 지표를 함께 확인하면 판단이 현실에 닿아요."
            WesternZodiacSign.PISCES ->
                "물고기자리는 분위기와 심리 흐름을 섬세하게 읽는 직감형이에요. 말로 설명되기 전의 불안과 기대를 감각적으로 받아들이는 편이에요. 직감이 강한 날일수록 가격, 기간, 손실 한도를 숫자로 적어두면 현실 감각을 지킬 수 있어요."
        }

    private fun buildBirthTarot(
        cardCode: String,
        preferredDeckVersionId: String?
    ): BirthTarotResponse {
        val canonicalCard = TarotCard.fromCode(cardCode)
        val deckVersionId = resolveBirthTarotDeckVersionId(preferredDeckVersionId)
        val card = tarotCardMetadataRepository.findByDeckVersion_IdAndCode(
            deckVersionId = deckVersionId,
            code = canonicalCard.code
        ) ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "birth tarot metadata not found for deckVersionId=$deckVersionId, code=${canonicalCard.code}"
        )
        val birthInterpretation = tarotBirthCardRepository.findByCardSetIdAndCode(
            cardSetId = card.cardSetId,
            code = canonicalCard.code
        )?.toInterpretation() ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "birth tarot interpretation not found for cardSetId=${card.cardSetId}, code=${canonicalCard.code}"
        )

        return card.toBirthTarotResponse(
            number = canonicalCard.cardNumber,
            birthInterpretation = birthInterpretation
        )
    }

    private fun resolveBirthTarotDeckVersionId(preferredDeckVersionId: String?): String {
        tarotDeckVersionRepository.findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(TarotDeckRole.MAIN)
            ?.let { return it.id }

        val candidateId = preferredDeckVersionId?.trim()?.ifBlank { null } ?: DEFAULT_TAROT_DECK_VERSION_ID
        val deck = tarotDeckVersionRepository.findById(candidateId).orElse(null)
            ?: return DEFAULT_TAROT_DECK_VERSION_ID
        if (!deck.active || deck.deckRole != TarotDeckRole.MAIN) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        return deck.id
    }

    private fun buildSajuProfile(
        consultingResult: com.hwcompany.fortune_index.saju.SajuConsultingResult
    ): SajuProfileResponse {
        val natalChart = consultingResult.analysis.natalChart
        val dayMasterStem = natalChart.day.heavenlyStem
        val currentYear = consultingResult.currentFortune.referenceYear
        val majorFortuneTimeline = consultingResult.currentFortune.majorFortuneTimeline
        val yearlyFortuneTimeline = consultingResult.currentFortune.yearlyFortuneTimeline
        val currentMajorSequence = consultingResult.currentFortune.majorFortune.sequence

        return SajuProfileResponse(
            palza = listOf(
                natalChart.year.toHanjaString(),
                natalChart.month.toHanjaString(),
                natalChart.day.toHanjaString(),
                natalChart.hour.toHanjaString()
            ),
            ohang = consultingResult.analysis.fiveElementBalance.toResponse(),
            ilju = buildDayPillarInsight(
                dayPillar = natalChart.day,
                dayMaster = consultingResult.dayMaster,
                dayBranch = consultingResult.dayBranch,
                dayBranchTenStar = sajuAnalyzer.calculateTenStar(dayMasterStem, natalChart.day.earthlyBranch)
            ),
            wolji = buildMonthBranchInsight(
                monthBranch = natalChart.month.earthlyBranch,
                monthBranchEnergy = consultingResult.monthBranch,
                monthBranchTenStar = sajuAnalyzer.calculateTenStar(dayMasterStem, natalChart.month.earthlyBranch)
            ),
            daeun = consultingResult.currentFortune.majorFortune.toInsight(),
            sewun = consultingResult.currentFortune.toYearlyInsight(),
            daeunTimeline = majorFortuneTimeline.mapIndexed { index, fortune ->
                buildDaeunTimelineItem(fortune, index, majorFortuneTimeline.size, currentMajorSequence)
            },
            sewunTimeline = yearlyFortuneTimeline.mapIndexed { index, fortune ->
                buildSewunTimelineItem(fortune, index, yearlyFortuneTimeline.size, currentYear)
            }
        )
    }

    private fun buildDaeunTimelineItem(
        fortune: com.hwcompany.fortune_index.saju.MajorFortuneDto,
        index: Int,
        total: Int,
        currentSequence: Int
    ): DaeunTimelineItem {
        val isCurrent = fortune.sequence == currentSequence && index == total / 2
        val label = when {
            isCurrent -> "현재 대운"
            index < total / 2 -> "이전 대운"
            else -> "다음 대운"
        }
        val ganji = "${fortune.pillar.toKoreanString()} (${fortune.pillar.toHanjaString()})"
        val element = com.hwcompany.fortune_index.saju.SajuAnalyzer.STEM_PROPERTIES
            .getValue(fortune.pillar.heavenlyStem).element.name.lowercase()
        val timeContext = when {
            isCurrent -> "지금은"
            index < total / 2 -> "지난 시기에는"
            else -> "이때는"
        }
        val summary = buildFortuneSummary("MAJOR", fortune.stemTenStar, fortune.branchTenStar, timeContext)
        return DaeunTimelineItem(
            label = label,
            period = "${fortune.startAge}-${fortune.endAge}세",
            ganji = ganji,
            element = element,
            summary = summary,
            isCurrent = isCurrent
        )
    }

    private fun buildSewunTimelineItem(
        fortune: com.hwcompany.fortune_index.saju.YearlyFortuneDto,
        index: Int,
        total: Int,
        currentYear: Int
    ): SewunTimelineItem {
        val offset = index - total / 2
        val isCurrent = fortune.year == currentYear
        val label = when (offset) {
            -2 -> "재작년"
            -1 -> "작년"
            0 -> "올해"
            1 -> "내년"
            2 -> "내후년"
            else -> if (offset < 0) "${-offset}년 전" else "${offset}년 후"
        }
        val ganji = "${fortune.pillar.toKoreanString()} (${fortune.pillar.toHanjaString()})"
        val element = com.hwcompany.fortune_index.saju.SajuAnalyzer.STEM_PROPERTIES
            .getValue(fortune.pillar.heavenlyStem).element.name.lowercase()
        val timeContext = when {
            isCurrent -> "올해는"
            index < total / 2 -> "그해는"
            else -> "이때는"
        }
        val summary = buildFortuneSummary("YEARLY", fortune.stemTenStar, fortune.branchTenStar, timeContext)
        return SewunTimelineItem(
            label = label,
            period = "${fortune.year}년",
            year = fortune.year,
            ganji = ganji,
            element = element,
            summary = summary,
            isCurrent = isCurrent
        )
    }

    private fun com.hwcompany.fortune_index.domain.model.FiveElementsProfile.toPercentages(): List<Int> {
        val values = listOf(
            wood,
            fire,
            earth,
            metal,
            water
        )
        val total = values.fold(BigDecimal.ZERO, BigDecimal::add)
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return listOf(0, 0, 0, 0, 0)
        }

        val scaled = values.map {
            it.multiply(HUNDRED).divide(total, 0, RoundingMode.DOWN).toInt()
        }.toMutableList()
        val remainder = 100 - scaled.sum()
        scaled[scaled.indices.maxBy { values[it] }] += remainder
        return scaled
    }

    private fun com.hwcompany.fortune_index.domain.model.FiveElementsProfile.toResponse(): SajuOhangResponse {
        val percentages = toPercentages()
        return SajuOhangResponse(
            wood = percentages[0],
            fire = percentages[1],
            earth = percentages[2],
            metal = percentages[3],
            water = percentages[4]
        )
    }

    private fun FiveElementBalance.toResponse(): SajuOhangResponse =
        SajuOhangResponse(
            wood = wood,
            fire = fire,
            earth = earth,
            metal = metal,
            water = water
        )

    private fun buildDayPillarInsight(
        dayPillar: Pillar,
        dayMaster: SajuCoreEnergy,
        dayBranch: SajuCoreEnergy,
        dayBranchTenStar: TenStar
    ): SajuInsightResponse {
        val ganji = SajuGanji.of(dayPillar.heavenlyStem, dayPillar.earthlyBranch.toZodiac())
        val dayPillarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.DAY_PILLAR,
            ganji.code
        )?.summaryEasy ?: (
            "${dayPillar.toKoreanString()}일은 나를 가장 잘 보여주는 기둥이에요. " +
                "${dayMaster.toSimpleImage()}처럼 기본 마음은 ${dayMaster.toSimpleTrait()} 편이고, " +
                "${dayBranch.toSimpleImage()} 기운이 함께 있어 ${dayBranch.toSimpleTrait()} 모습도 같이 보여요."
            )
        val tenStarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.TEN_STAR,
            dayBranchTenStar.name
        )?.summaryEasy ?: dayBranchTenStar.toSimpleMeaning()

        return SajuInsightResponse(
            name = "${dayPillar.toKoreanString()} (${dayPillar.toHanjaString()})",
            summary = "$dayPillarSummary 일지의 힘은 ${dayBranchTenStar.labelKo()}이라 $tenStarSummary"
        )
    }

    private fun buildMonthBranchInsight(
        monthBranch: EarthlyBranch,
        monthBranchEnergy: SajuCoreEnergy,
        monthBranchTenStar: TenStar
    ): SajuInsightResponse {
        val monthBranchSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.MONTH_BRANCH,
            monthBranch.name
        )?.summaryEasy ?: (
            "${monthBranch.labelKo()}는 ${monthBranchEnergy.toSimpleImage()} 기운이라 " +
                "${monthBranchEnergy.toSimpleTrait()} 분위기 속에서 힘을 쓰기 쉬워요."
            )
        val tenStarSummary = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.TEN_STAR,
            monthBranchTenStar.name
        )?.summaryEasy ?: monthBranchTenStar.toSimpleMeaning()

        return SajuInsightResponse(
            name = "${monthBranch.labelKo()} 월지 (${monthBranch.toHanja()})",
            summary = "$monthBranchSummary 월지의 힘은 ${monthBranchTenStar.labelKo()}이라 $tenStarSummary"
        )
    }

    private fun com.hwcompany.fortune_index.saju.MajorFortuneDto.toInsight(): FortuneInsightResponse =
        FortuneInsightResponse(
            name = "${startAge}-${endAge}세 ${pillar.toKoreanString()} (${pillar.toHanjaString()})",
            summary = buildFortuneSummary("MAJOR", stemTenStar, branchTenStar, "지금은")
        )

    private fun com.hwcompany.fortune_index.saju.CurrentFortuneDto.toYearlyInsight(): FortuneInsightResponse =
        FortuneInsightResponse(
            name = "${referenceYear}년 ${yearlyFortune.pillar.toKoreanString()} (${yearlyFortune.pillar.toHanjaString()})",
            summary = buildFortuneSummary("YEARLY", yearlyFortune.stemTenStar, yearlyFortune.branchTenStar, "올해는")
        )

    private fun buildFortuneSummary(
        code: String,
        stemTenStar: TenStar,
        branchTenStar: TenStar,
        timeContext: String
    ): String {
        val template = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.FORTUNE_TYPE, code
        )?.summaryEasy ?: "{timeContext} {stemSummary} {branchSummary}"
        val templateWithContext = when {
            template.contains("{timeContext}") -> template.replace("{timeContext}", timeContext)
            template.startsWith("지금은") -> timeContext + template.removePrefix("지금은")
            template.startsWith("올해는") -> timeContext + template.removePrefix("올해는")
            else -> template
        }
        return templateWithContext
            .replace("{stemSummary}", stemTenStarSummary(stemTenStar))
            .replace("{branchSummary}", branchTenStarSummary(branchTenStar))
    }

    private fun Pillar.toHanjaString(): String = heavenlyStem.toHanja() + earthlyBranch.toHanja()

    private fun Pillar.toKoreanString(): String = heavenlyStem.labelKo() + earthlyBranch.labelKo()

    private fun HeavenlyStem.toHanja(): String =
        when (this) {
            HeavenlyStem.GAP -> "甲"
            HeavenlyStem.EUL -> "乙"
            HeavenlyStem.BYEONG -> "丙"
            HeavenlyStem.JEONG -> "丁"
            HeavenlyStem.MU -> "戊"
            HeavenlyStem.GI -> "己"
            HeavenlyStem.GYEONG -> "庚"
            HeavenlyStem.SIN -> "辛"
            HeavenlyStem.IM -> "壬"
            HeavenlyStem.GYE -> "癸"
        }

    private fun EarthlyBranch.toHanja(): String =
        when (this) {
            EarthlyBranch.JA -> "子"
            EarthlyBranch.CHUK -> "丑"
            EarthlyBranch.IN -> "寅"
            EarthlyBranch.MYO -> "卯"
            EarthlyBranch.JIN -> "辰"
            EarthlyBranch.SA -> "巳"
            EarthlyBranch.O -> "午"
            EarthlyBranch.MI -> "未"
            EarthlyBranch.SIN -> "申"
            EarthlyBranch.YU -> "酉"
            EarthlyBranch.SUL -> "戌"
            EarthlyBranch.HAE -> "亥"
        }

    private fun TenStar.labelKo(): String =
        when (this) {
            TenStar.BIGYEON -> "비견"
            TenStar.GEOPJAE -> "겁재"
            TenStar.SIKSIN -> "식신"
            TenStar.SANGGWAN -> "상관"
            TenStar.PYEONJAE -> "편재"
            TenStar.JEONGJAE -> "정재"
            TenStar.PYEONGWAN -> "편관"
            TenStar.JEONGGWAN -> "정관"
            TenStar.PYEONIN -> "편인"
            TenStar.JEONGIN -> "정인"
        }

    private fun TenStar.toSimpleMeaning(): String =
        when (this) {
            TenStar.BIGYEON -> "내 힘으로 직접 해보는 일"
            TenStar.GEOPJAE -> "경쟁 속에서 내 몫을 챙기는 일"
            TenStar.SIKSIN -> "재능과 생각을 천천히 꺼내는 일"
            TenStar.SANGGWAN -> "표현이 많아지고 하고 싶은 말이 커지는 일"
            TenStar.PYEONJAE -> "새 기회와 실속을 넓게 보는 일"
            TenStar.JEONGJAE -> "돈과 계획을 차곡차곡 챙기는 일"
            TenStar.PYEONGWAN -> "규칙과 책임을 더 신경 쓰는 일"
            TenStar.JEONGGWAN -> "질서를 잘 지켜 좋은 평가를 받는 일"
            TenStar.PYEONIN -> "새 생각을 배우고 시야를 넓히는 일"
            TenStar.JEONGIN -> "도움받고 배우며 기본기를 쌓는 일"
        }

    private fun SajuCoreEnergy.toSimpleImage(): String =
        when (fiveElement) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "나무"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "불"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "흙"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "쇠"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "물"
        }

    private fun SajuCoreEnergy.toSimpleTrait(): String =
        when (fiveElement) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "자라나듯 천천히 커 가는"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "밝고 힘차게 움직이는"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "차분하고 안정적으로 버티는"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "분명하고 단단하게 정리하는"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "부드럽고 유연하게 흐르는"
        }

    private fun stemTenStarSummary(tenStar: TenStar): String =
        tenStar.toFortuneSummary()

    private fun branchTenStarSummary(tenStar: TenStar): String =
        tenStar.toFortuneSummary()

    private fun TenStar.toFortuneSummary(): String =
        sajuInterpretationService.getInterpretation(SajuInterpretationCategory.TEN_STAR, name)?.summaryEasy
            ?: "${toSimpleMeaning()} 쪽을 살피기 좋아요."

    private fun EarthlyBranch.toZodiac(): com.hwcompany.fortune_index.common.Zodiac =
        com.hwcompany.fortune_index.common.Zodiac.entries.first { it.branch == this }

    private fun TarotCardMetadataEntity.toBirthTarotResponse(
        number: Int,
        birthInterpretation: TarotBirthCardInterpretation
    ): BirthTarotResponse =
        BirthTarotResponse(
            deckVersionId = deckVersion.id,
            name = name,
            koreanName = koreanName,
            number = number,
            cardMeaning = meaning,
            cardDescription = birthInterpretation.description,
            birthMeaning = birthInterpretation.meaning,
            birthDescription = birthInterpretation.description,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )

    companion object {
        private val logger = LoggerFactory.getLogger(ProfileDetailsService::class.java)
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private val HUNDRED = BigDecimal("100")
    }
}
