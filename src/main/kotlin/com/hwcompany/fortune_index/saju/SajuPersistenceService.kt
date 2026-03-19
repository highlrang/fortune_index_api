package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.FiveElementsProfile
import com.hwcompany.fortune_index.domain.model.SajuBranchRecord
import com.hwcompany.fortune_index.domain.model.SajuResult
import com.hwcompany.fortune_index.domain.model.SajuStemRecord
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.domain.model.sortOrder
import com.hwcompany.fortune_index.history.UserRepository
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SajuPersistenceService(
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun saveInitialResult(user: User): SajuResult =
        sajuResultRepository.save(buildResult(user))

    @Transactional
    fun backfillMissingResults(batchSize: Int = DEFAULT_BATCH_SIZE): SajuBackfillSummary {
        require(batchSize > 0) { "batchSize must be positive" }

        var scannedUsers = 0
        var createdResults = 0
        var lastSeenUserId = 0L

        while (true) {
            val users = userRepository.findByIdGreaterThanOrderByIdAsc(
                id = lastSeenUserId,
                pageable = PageRequest.of(0, batchSize)
            )
            if (users.isEmpty()) {
                break
            }

            val userIds = users.mapNotNull { it.id }
            val existingUserIds = sajuResultRepository.findExistingUserIds(userIds).toSet()
            val missingUsers = users.filter { requireNotNull(it.id) !in existingUserIds }

            scannedUsers += users.size
            if (missingUsers.isNotEmpty()) {
                sajuResultRepository.saveAll(missingUsers.map(::buildResult))
                createdResults += missingUsers.size
            }

            lastSeenUserId = requireNotNull(users.last().id)
        }

        val summary = SajuBackfillSummary(
            scannedUsers = scannedUsers,
            createdResults = createdResults
        )
        logger.info(
            "Completed saju backfill. scannedUsers={}, createdResults={}",
            summary.scannedUsers,
            summary.createdResults
        )
        return summary
    }

    private fun buildResult(user: User): SajuResult {
        val analysis = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = LocalDateTime.of(
                user.birthInfo.birthDate,
                user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
            ),
            zoneId = DEFAULT_ZONE_ID
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

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toStemRecord(pillarOrder: Int): SajuStemRecord =
    SajuStemRecord(
        pillarOrder = pillarOrder,
        code = name,
        labelKo = labelKo(),
        sortOrder = sortOrder()
    )

private fun com.hwcompany.fortune_index.domain.model.EarthlyBranch.toBranchRecord(pillarOrder: Int): SajuBranchRecord =
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
