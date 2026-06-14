package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.FiveElementsProfile
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.SajuBranchRecord
import com.hwcompany.fortune_index.domain.model.SajuResult
import com.hwcompany.fortune_index.domain.model.SajuStemRecord
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatureService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class SajuPersistenceService(
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
    private val sajuInvestmentFeatureService: SajuInvestmentFeatureService
) {
    @Transactional
    fun saveInitialResult(user: User): SajuResult =
        sajuResultRepository.save(buildResult(user))

    @Transactional
    fun refreshResult(user: User): SajuResult {
        val latestResult = sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(user.id))
        val refreshed = buildResult(user)

        if (latestResult == null) {
            return sajuResultRepository.save(refreshed)
        }

        latestResult.heavenlyStems = refreshed.heavenlyStems
        latestResult.earthlyBranches = refreshed.earthlyBranches
        latestResult.fiveElements = refreshed.fiveElements
        latestResult.energyBalance = refreshed.energyBalance
        latestResult.analyzedAt = refreshed.analyzedAt
        return latestResult
    }

    private fun buildResult(user: User): SajuResult {
        val consulting = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = LocalDateTime.of(
                user.birthInfo.birthDate,
                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
            ),
            zoneId = DEFAULT_ZONE_ID,
            gender = user.gender
        )
        val analysis = consulting.analysis

        return SajuResult(
            user = user,
            heavenlyStems = mutableListOf(
                analysis.natalChart.year.heavenlyStem.toStemRecord(1),
                analysis.natalChart.month.heavenlyStem.toStemRecord(2),
                analysis.natalChart.day.heavenlyStem.toStemRecord(3),
                analysis.natalChart.hour.heavenlyStem.toStemRecord(4)
            ),
            earthlyBranches = mutableListOf(
                analysis.natalChart.year.earthlyBranch.toBranchRecord(1),
                analysis.natalChart.month.earthlyBranch.toBranchRecord(2),
                analysis.natalChart.day.earthlyBranch.toBranchRecord(3),
                analysis.natalChart.hour.earthlyBranch.toBranchRecord(4)
            ),
            fiveElements = FiveElementsProfile(
                wood = analysis.fiveElementBalance.wood.toBigDecimal(),
                fire = analysis.fiveElementBalance.fire.toBigDecimal(),
                earth = analysis.fiveElementBalance.earth.toBigDecimal(),
                metal = analysis.fiveElementBalance.metal.toBigDecimal(),
                water = analysis.fiveElementBalance.water.toBigDecimal()
            ),
            energyBalance = sajuInvestmentFeatureService.calculateEnergyBalance(consulting).name
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SajuPersistenceService::class.java)
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
    }
}

private fun HeavenlyStem.toStemRecord(pillarOrder: Int): SajuStemRecord =
    SajuStemRecord(
        pillarOrder = pillarOrder,
        code = name,
        labelKo = toLabelKo(),
        sortOrder = ordinal + 1
    )

private fun EarthlyBranch.toBranchRecord(pillarOrder: Int): SajuBranchRecord =
    SajuBranchRecord(
        pillarOrder = pillarOrder,
        code = name,
        labelKo = toLabelKo(),
        sortOrder = ordinal + 1
    )

private fun HeavenlyStem.toLabelKo(): String =
    when (this) {
        HeavenlyStem.GAP -> "갑"
        HeavenlyStem.EUL -> "을"
        HeavenlyStem.BYEONG -> "병"
        HeavenlyStem.JEONG -> "정"
        HeavenlyStem.MU -> "무"
        HeavenlyStem.GI -> "기"
        HeavenlyStem.GYEONG -> "경"
        HeavenlyStem.SIN -> "신"
        HeavenlyStem.IM -> "임"
        HeavenlyStem.GYE -> "계"
    }

private fun EarthlyBranch.toLabelKo(): String =
    when (this) {
        EarthlyBranch.JA -> "자"
        EarthlyBranch.CHUK -> "축"
        EarthlyBranch.IN -> "인"
        EarthlyBranch.MYO -> "묘"
        EarthlyBranch.JIN -> "진"
        EarthlyBranch.SA -> "사"
        EarthlyBranch.O -> "오"
        EarthlyBranch.MI -> "미"
        EarthlyBranch.SIN -> "신"
        EarthlyBranch.YU -> "유"
        EarthlyBranch.SUL -> "술"
        EarthlyBranch.HAE -> "해"
    }

data class SajuBackfillSummary(
    val scannedUsers: Int,
    val createdResults: Int
)
