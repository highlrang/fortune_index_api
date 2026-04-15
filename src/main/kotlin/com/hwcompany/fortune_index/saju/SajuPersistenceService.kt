package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class SajuPersistenceService(
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository
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
        latestResult.analyzedAt = refreshed.analyzedAt
        return latestResult
    }

    private fun buildResult(user: User): SajuResult {
        val analysis = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = LocalDateTime.of(
                user.birthInfo.birthDate,
                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
            ),
            zoneId = DEFAULT_ZONE_ID,
            gender = user.gender
        ).analysis

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
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SajuPersistenceService::class.java)
        private val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val DEFAULT_BIRTH_TIME: LocalTime = LocalTime.NOON
        private const val DEFAULT_BATCH_SIZE = 500
    }
}

private fun HeavenlyStem.toStemRecord(pillarOrder: Int): SajuStemRecord =
    SajuStemRecord(
        pillarOrder = pillarOrder,
        code = name,
        labelKo = labelKo(),
        sortOrder = sortOrder()
    )

private fun EarthlyBranch.toBranchRecord(pillarOrder: Int): SajuBranchRecord =
    SajuBranchRecord(
        pillarOrder = pillarOrder,
        code = name,
        labelKo = labelKo(),
        sortOrder = sortOrder()
    )

data class SajuBackfillSummary(
    val scannedUsers: Int,
    val createdResults: Int
)
