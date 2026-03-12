package com.hwcompany.fortune_index.scheduler

import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultRequest
import com.hwcompany.fortune_index.consulting.ConsultResponse
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import com.hwcompany.fortune_index.consulting.ConsultingService
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.ConsultingHistoryRepository
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatedConsultingSchedulerService(
    private val userRepository: UserRepository,
    private val consultingHistoryRepository: ConsultingHistoryRepository,
    private val consultingService: ConsultingService,
    private val schedulerProperties: SchedulerProperties
) {
    @Transactional
    fun generateDailyConsultings(
        modes: Iterable<AnalysisMode> = AnalysisMode.entries
    ): DailyConsultingBatchResult {
        val zoneId = ZoneId.of(schedulerProperties.dailyConsulting.zone)
        val now = LocalDateTime.now(zoneId)
        val startOfDay = now.toLocalDate().atStartOfDay()
        val endOfDay = startOfDay.plusDays(1)
        val activeUsers = userRepository.findByAccountStatus(UserAccountStatus.ACTIVE)
        val modeList = modes.toList()

        var createdCount = 0
        var skippedCount = 0
        var failedCount = 0
        val items = mutableListOf<DailyConsultingGeneratedItem>()

        activeUsers.forEach { user ->
            modeList.forEach { mode ->
                val alreadyExists = consultingHistoryRepository.existsByUserIdAndAnalysisModeAndConsultedAtBetween(
                    userId = requireNotNull(user.id),
                    analysisMode = mode,
                    start = startOfDay,
                    end = endOfDay
                )
                if (alreadyExists) {
                    skippedCount += 1
                    return@forEach
                }

                val request = buildRandomRequest(user, mode, now)
                runCatching {
                    consultingService.consult(request)
                }.onSuccess { response ->
                    createdCount += 1
                    items += response.toGeneratedItem(request)
                }.onFailure { ex ->
                    failedCount += 1
                    logger.error(
                        "Failed to generate scheduled consulting userId={} mode={}",
                        user.id,
                        mode,
                        ex
                    )
                }
            }
        }

        logger.info(
            "Daily consulting batch finished users={} created={} skipped={} failed={}",
            activeUsers.size,
            createdCount,
            skippedCount,
            failedCount
        )

        return DailyConsultingBatchResult(
            userCount = activeUsers.size,
            createdCount = createdCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            items = items
        )
    }

    private fun buildRandomRequest(user: User, mode: AnalysisMode, now: LocalDateTime): ConsultRequest {
        return ConsultRequest(
            userId = requireNotNull(user.id),
            mode = mode,
            scenario = ConsultingScenario.entries.random(random),
            stockCode = schedulerProperties.dailyConsulting.stockCandidates.random(random),
            tarotIndices = if (mode.includesTarot()) randomTarotIndices() else null,
            tarotInterpretationMode = if (mode.includesTarot()) TarotInterpretationMode.MAIN_TRADITIONAL else null,
            question = resolveQuestion(mode),
            referenceDateTime = now
        )
    }

    private fun resolveQuestion(mode: AnalysisMode): String {
        val questions = when (mode) {
            AnalysisMode.ONLY_STOCK -> schedulerProperties.dailyConsulting.questions.onlyStock
            AnalysisMode.STOCK_SAJU -> schedulerProperties.dailyConsulting.questions.stockSaju
            AnalysisMode.STOCK_TAROT -> schedulerProperties.dailyConsulting.questions.stockTarot
            AnalysisMode.STOCK_ALL -> schedulerProperties.dailyConsulting.questions.stockAll
        }
        return questions.random(random)
    }

    private fun randomTarotIndices(): List<Int> =
        TarotCard.entries.indices.shuffled(random).take(TAROT_CARD_COUNT)

    private companion object {
        private const val TAROT_CARD_COUNT = 3
        private val logger = LoggerFactory.getLogger(AutomatedConsultingSchedulerService::class.java)
        private val random = Random.Default
    }
}

data class DailyConsultingBatchResult(
    val userCount: Int,
    val createdCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val items: List<DailyConsultingGeneratedItem> = emptyList()
)

data class DailyConsultingGeneratedItem(
    val userId: Long,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario,
    val question: String?,
    val historyId: Long,
    val aiSummary: String
)

private fun ConsultResponse.toGeneratedItem(request: ConsultRequest): DailyConsultingGeneratedItem =
    DailyConsultingGeneratedItem(
        userId = request.userId,
        mode = request.mode,
        scenario = request.scenario,
        question = request.question,
        historyId = history.id,
        aiSummary = ai.finalAdvice
    )
