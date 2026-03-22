package com.hwcompany.fortune_index.profile

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.SajuResult
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.saju.TenStar
import com.hwcompany.fortune_index.tarot.TarotArcanaType
import com.hwcompany.fortune_index.tarot.TarotCard
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
    private val sajuAnalyzer: SajuAnalyzer
) {
    @Transactional(readOnly = true)
    fun getProfileDetails(userId: Long): MyProfileDetailsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId") }

        val birthTarot = runCatching { buildBirthTarot(user.birthInfo.birthDate.toString()) }.getOrNull()
        val saju = runCatching {
            sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(userId)
                ?.let {
                    buildSajuProfile(
                        sajuResult = it,
                        birthDateTime = LocalDateTime.of(
                            user.birthInfo.birthDate,
                            user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
                        )
                    )
                }
        }.getOrNull()

        return MyProfileDetailsResponse(
            birthTarot = birthTarot,
            saju = saju
        )
    }

    private fun buildBirthTarot(dateDigits: String): BirthTarotResponse {
        val numerologyNumber = reduceToBirthTarotNumber(dateDigits.filter(Char::isDigit).sumOf { it.digitToInt() })
        val card = MAJOR_ARCANA_BY_NUMBER.getValue(numerologyNumber)

        return BirthTarotResponse(
            name = card.displayName,
            koreanName = card.toKoreanName(),
            number = card.cardNumber,
            meaning = card.uprightMeaning,
            imageUrl = card.imageUrl
        )
    }

    private fun buildSajuProfile(
        sajuResult: SajuResult,
        birthDateTime: LocalDateTime
    ): SajuProfileResponse {
        val consultingResult = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = birthDateTime,
            referenceDateTime = LocalDateTime.now(DEFAULT_ZONE_ID),
            zoneId = DEFAULT_ZONE_ID
        )
        val natalChart = consultingResult.analysis.natalChart
        val dayMaster = natalChart.day.heavenlyStem

        return SajuProfileResponse(
            palza = listOf(
                natalChart.year.toHanjaString(),
                natalChart.month.toHanjaString(),
                natalChart.day.toHanjaString(),
                natalChart.hour.toHanjaString()
            ),
            ohang = sajuResult.fiveElements.toResponse(),
            sipsung = listOf(
                sajuAnalyzer.calculateTenStar(dayMaster, natalChart.year.heavenlyStem).labelKo(),
                sajuAnalyzer.calculateTenStar(dayMaster, natalChart.month.heavenlyStem).labelKo(),
                sajuAnalyzer.calculateTenStar(dayMaster, natalChart.day.heavenlyStem).labelKo(),
                sajuAnalyzer.calculateTenStar(dayMaster, natalChart.hour.heavenlyStem).labelKo()
            ),
            daeun = consultingResult.currentFortune.majorFortune.toDescription(),
            sewun = consultingResult.currentFortune.toYearlyDescription()
        )
    }

    private fun reduceToBirthTarotNumber(value: Int): Int {
        var reduced = value
        while (reduced > 22) {
            reduced = reduced.toString().sumOf { it.digitToInt() }
        }
        return reduced.coerceAtLeast(1)
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

    private fun com.hwcompany.fortune_index.saju.MajorFortuneDto.toDescription(): String =
        "${startAge}-${endAge}세: ${pillar.toHanjaString()} 대운"

    private fun com.hwcompany.fortune_index.saju.CurrentFortuneDto.toYearlyDescription(): String =
        "${referenceYear}년 흐름: ${yearlyFortune.stemTenStar.labelKo()}과 ${majorFortune.stemTenStar.labelKo()}의 균형"

    private fun Pillar.toHanjaString(): String = heavenlyStem.toHanja() + earthlyBranch.toHanja()

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

    private fun TarotCard.toKoreanName(): String =
        when (this) {
            TarotCard.THE_FOOL -> "바보"
            TarotCard.THE_MAGICIAN -> "마법사"
            TarotCard.THE_HIGH_PRIESTESS -> "여사제"
            TarotCard.THE_EMPRESS -> "여황제"
            TarotCard.THE_EMPEROR -> "황제"
            TarotCard.THE_HIEROPHANT -> "교황"
            TarotCard.THE_LOVERS -> "연인"
            TarotCard.THE_CHARIOT -> "전차"
            TarotCard.STRENGTH -> "힘"
            TarotCard.THE_HERMIT -> "은둔자"
            TarotCard.WHEEL_OF_FORTUNE -> "운명의 수레바퀴"
            TarotCard.JUSTICE -> "정의"
            TarotCard.THE_HANGED_MAN -> "매달린 남자"
            TarotCard.DEATH -> "죽음"
            TarotCard.TEMPERANCE -> "절제"
            TarotCard.THE_DEVIL -> "악마"
            TarotCard.THE_TOWER -> "탑"
            TarotCard.THE_STAR -> "별"
            TarotCard.THE_MOON -> "달"
            TarotCard.THE_SUN -> "태양"
            TarotCard.JUDGEMENT -> "심판"
            TarotCard.THE_WORLD -> "세계"
            else -> displayName
        }

    companion object {
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private val HUNDRED = BigDecimal("100")
        private val MAJOR_ARCANA_BY_NUMBER = TarotCard.entries
            .filter { it.arcanaType == TarotArcanaType.MAJOR }
            .associateBy { it.cardNumber }
    }
}
