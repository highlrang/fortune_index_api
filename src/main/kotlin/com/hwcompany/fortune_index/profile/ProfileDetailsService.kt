package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.SajuResult
import com.hwcompany.fortune_index.domain.model.UserGender
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuCoreEnergy
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuInterpretationCategory
import com.hwcompany.fortune_index.saju.SajuInterpretationService
import com.hwcompany.fortune_index.saju.TenStar
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotArcanaType
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotCardMetadataEntity
import com.hwcompany.fortune_index.tarot.TarotCardMetadataRepository
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ProfileDetailsService(
    private val userRepository: UserRepository,
    private val sajuResultRepository: SajuResultRepository,
    private val sajuAnalyzer: SajuAnalyzer,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val sajuInterpretationService: SajuInterpretationService
) {
    @Transactional(readOnly = true)
    fun getProfileDetails(userId: Long): MyProfileDetailsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId") }

        val birthTarot = runCatching {
            buildBirthTarot(
                dateDigits = user.birthInfo.birthDate.toString(),
                preferredDeckVersionId = user.preferredTarotDeckId
            )
        }.getOrNull()
        val saju = runCatching {
            sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(userId)
                ?.let {
                        buildSajuProfile(
                            sajuResult = it,
                            birthDateTime = LocalDateTime.of(
                                user.birthInfo.birthDate,
                                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
                            ),
                            gender = user.gender
                        )
                }
        }.getOrNull()

        return MyProfileDetailsResponse(
            birthTarot = birthTarot,
            saju = saju
        )
    }

    private fun buildBirthTarot(
        dateDigits: String,
        preferredDeckVersionId: String?
    ): BirthTarotResponse {
        val numerologyNumber = reduceToBirthTarotNumber(dateDigits.filter(Char::isDigit).sumOf { it.digitToInt() })
        val canonicalCard = MAJOR_ARCANA_BY_NUMBER.getValue(numerologyNumber)
        val deckVersionId = resolveBirthTarotDeckVersionId(preferredDeckVersionId)
        val card = tarotCardMetadataRepository.findByDeckVersion_IdAndCode(
            deckVersionId = deckVersionId,
            code = canonicalCard.code
        ) ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "birth tarot metadata not found for deckVersionId=$deckVersionId, code=${canonicalCard.code}"
        )

        return card.toBirthTarotResponse(number = numerologyNumber)
    }

    private fun resolveBirthTarotDeckVersionId(preferredDeckVersionId: String?): String {
        val candidateId = preferredDeckVersionId?.trim()?.ifBlank { null } ?: DEFAULT_TAROT_DECK_VERSION_ID
        val deck = tarotDeckVersionRepository.findById(candidateId).orElse(null)
            ?: return DEFAULT_TAROT_DECK_VERSION_ID
        if (!deck.active || deck.deckRole != TarotDeckRole.MAIN) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        return deck.id
    }

    private fun buildSajuProfile(
        sajuResult: SajuResult,
        birthDateTime: LocalDateTime,
        gender: UserGender
    ): SajuProfileResponse {
        val consultingResult = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = birthDateTime,
            referenceDateTime = LocalDateTime.now(DEFAULT_ZONE_ID),
            zoneId = DEFAULT_ZONE_ID,
            gender = gender
        )
        val natalChart = consultingResult.analysis.natalChart
        val dayMasterStem = natalChart.day.heavenlyStem

        return SajuProfileResponse(
            palza = listOf(
                natalChart.year.toHanjaString(),
                natalChart.month.toHanjaString(),
                natalChart.day.toHanjaString(),
                natalChart.hour.toHanjaString()
            ),
            ohang = sajuResult.fiveElements.toResponse(),
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
            sewun = consultingResult.currentFortune.toYearlyInsight()
        )
    }

    private fun reduceToBirthTarotNumber(value: Int): Int {
        var reduced = value
        while (reduced > 22) {
            reduced = reduced.toString().sumOf { it.digitToInt() }
        }
        return if (reduced == 22) 0 else reduced.coerceAtLeast(1)
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
            "${dayPillar.toKoreanString()} 일주는 나를 가장 잘 보여주는 기둥이에요. " +
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
            "월지는 태어날 때의 계절 공기 같은 거예요. " +
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

    private fun com.hwcompany.fortune_index.saju.MajorFortuneDto.toInsight(): FortuneInsightResponse {
        val template = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.FORTUNE_TYPE,
            "MAJOR"
        )?.summaryEasy ?: "대운은 10년 정도 이어지는 큰 흐름이에요. 지금은 {stemSummary} {branchSummary}"
        return FortuneInsightResponse(
            name = "${startAge}-${endAge}세 ${pillar.toKoreanString()} (${pillar.toHanjaString()})",
            summary = template
                .replace("{stemSummary}", stemTenStarSummary(stemTenStar))
                .replace("{branchSummary}", branchTenStarSummary(branchTenStar))
        )
    }

    private fun com.hwcompany.fortune_index.saju.CurrentFortuneDto.toYearlyInsight(): FortuneInsightResponse {
        val template = sajuInterpretationService.getInterpretation(
            SajuInterpretationCategory.FORTUNE_TYPE,
            "YEARLY"
        )?.summaryEasy ?: "세운은 올해의 흐름이에요. 올해는 {stemSummary} {branchSummary}"
        return FortuneInsightResponse(
            name = "${referenceYear}년 ${yearlyFortune.pillar.toKoreanString()} (${yearlyFortune.pillar.toHanjaString()})",
            summary = template
                .replace("{stemSummary}", stemTenStarSummary(yearlyFortune.stemTenStar))
                .replace("{branchSummary}", branchTenStarSummary(yearlyFortune.branchTenStar))
        )
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
        sajuInterpretationService.getInterpretation(SajuInterpretationCategory.TEN_STAR, tenStar.name)?.summaryEasy
            ?.let { "$it 좋고," }
            ?: "${tenStar.toSimpleMeaning()} 좋고,"

    private fun branchTenStarSummary(tenStar: TenStar): String =
        sajuInterpretationService.getInterpretation(SajuInterpretationCategory.TEN_STAR, tenStar.name)?.summaryEasy
            ?.let { "$it 흐름도 함께 와요." }
            ?: "${tenStar.toSimpleMeaning()} 흐름도 함께 와요."

    private fun EarthlyBranch.toZodiac(): com.hwcompany.fortune_index.common.Zodiac =
        com.hwcompany.fortune_index.common.Zodiac.entries.first { it.branch == this }

    private fun TarotCardMetadataEntity.toBirthTarotResponse(number: Int): BirthTarotResponse =
        BirthTarotResponse(
            deckVersionId = deckVersion.id,
            name = name,
            koreanName = koreanName,
            number = number,
            meaning = meaning,
            description = description,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )

    companion object {
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private val HUNDRED = BigDecimal("100")
        private val MAJOR_ARCANA_BY_NUMBER = TarotCard.entries
            .filter { it.arcanaType == TarotArcanaType.MAJOR }
            .associateBy { it.cardNumber }
    }
}
