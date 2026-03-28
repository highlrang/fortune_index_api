package com.hwcompany.fortune_index.scheduler

import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultRequest
import com.hwcompany.fortune_index.consulting.ConsultResponse
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import com.hwcompany.fortune_index.consulting.ConsultingService
import com.hwcompany.fortune_index.consulting.ScheduledSectorContext
import com.hwcompany.fortune_index.consulting.SectorMarketContext
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.ConsultingHistoryRepository
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatedConsultingSchedulerService(
    private val userRepository: UserRepository,
    private val consultingHistoryRepository: ConsultingHistoryRepository,
    private val consultingService: ConsultingService,
    private val schedulerProperties: SchedulerProperties,
    private val tarotDeckService: TarotDeckService
) {
    @Transactional
    fun generateDailyConsultings(
        modes: Iterable<AnalysisMode> = AnalysisMode.entries
    ): DailyConsultingBatchResult {
        val zoneId = ZoneId.of(schedulerProperties.dailyConsulting.zone)
        val now = LocalDateTime.now(zoneId)
        return generateConsultings(
            modes = modes,
            now = now,
            deduplicationWindow = SchedulerDeduplicationWindow.DAILY
        )
    }

    @Transactional
    fun generateHourlyRandomConsultings(referenceTime: LocalDateTime? = null): DailyConsultingBatchResult {
        val zoneId = ZoneId.of(schedulerProperties.hourlyRandomConsulting.zone)
        val now = referenceTime ?: LocalDateTime.now(zoneId)
        val randomMode = AnalysisMode.entries.random(random)
        return generateConsultings(
            modes = listOf(randomMode),
            now = now,
            deduplicationWindow = SchedulerDeduplicationWindow.HOURLY
        )
    }

    private fun generateConsultings(
        modes: Iterable<AnalysisMode>,
        now: LocalDateTime,
        deduplicationWindow: SchedulerDeduplicationWindow
    ): DailyConsultingBatchResult {
        val activeUsers = userRepository.findByAccountStatus(UserAccountStatus.ACTIVE)
        val modeList = modes.toList()
        val (windowStart, windowEnd) = deduplicationWindow.resolveWindow(now)

        var createdCount = 0
        var skippedCount = 0
        var failedCount = 0
        val items = mutableListOf<DailyConsultingGeneratedItem>()

        activeUsers.forEach { user ->
            modeList.forEach modeLoop@{ mode ->
                val alreadyExists = consultingHistoryRepository.existsByUserIdAndAnalysisModeAndConsultedAtBetween(
                    userId = requireNotNull(user.id),
                    analysisMode = mode,
                    start = windowStart,
                    end = windowEnd
                )
                if (alreadyExists) {
                    skippedCount += 1
                    return@modeLoop
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
        val scenario = ConsultingScenario.entries.random(random)
        val scheduledSectorContext = buildRandomSectorContext()
        return ConsultRequest(
            userId = requireNotNull(user.id),
            mode = mode,
            scenario = scenario,
            stockName = scheduledSectorContext.sectors.joinToString(" + "),
            tarotIndices = if (mode.includesTarot()) randomTarotIndices() else null,
            tarotDeckVersionId = if (mode.includesTarot()) DEFAULT_TAROT_DECK_VERSION_ID else null,
            tarotInterpretationMode = if (mode.includesTarot()) TarotInterpretationMode.MAIN_TRADITIONAL else null,
            question = resolveQuestion(mode, scenario, scheduledSectorContext.sectors),
            referenceDateTime = now,
            scheduledSectorContext = scheduledSectorContext
        )
    }

    private fun resolveQuestion(
        mode: AnalysisMode,
        scenario: ConsultingScenario,
        sectors: List<String>
    ): String {
        val sectorText = sectors.joinToString(", ")
        val lens = when (mode) {
            AnalysisMode.ONLY_STOCK -> "시장 신호만 기준으로"
            AnalysisMode.STOCK_SAJU -> "시장 신호와 내 사주 흐름을 함께 봐서"
            AnalysisMode.STOCK_TAROT -> "시장 신호와 타로 흐름을 함께 봐서"
            AnalysisMode.STOCK_ALL -> "시장 신호, 사주, 타로를 모두 반영해서"
        }

        return when (scenario) {
            ConsultingScenario.TIMING_ENTRY ->
                "$sectorText 섹터 조합을 $lens 지금 진입해도 되는지, 분할 진입이 맞는지 판단해줘."

            ConsultingScenario.TIMING_EXIT ->
                "$sectorText 섹터 조합을 $lens 지금 비중을 줄이거나 익절하는 편이 나은지 판단해줘."

            ConsultingScenario.SAJU_MATCH ->
                "$sectorText 섹터 조합이 $lens 내 투자 성향과 궁합이 맞는지, 오래 들고 가도 되는지 봐줘."

            ConsultingScenario.RESCUE_PLAN ->
                "$sectorText 섹터 조합을 $lens 물린 포지션이라면 손절, 물타기, 보유 중 무엇이 더 현실적인지 정리해줘."

            ConsultingScenario.MENTAL_GUIDE ->
                "$sectorText 섹터 조합을 $lens 지금 멘탈이 흔들릴 때 어떤 원칙으로 대응해야 하는지 조언해줘."
        }
    }

    private fun buildRandomSectorContext(): ScheduledSectorContext {
        val sectorPool = schedulerProperties.dailyConsulting.representativeSectors
        require(sectorPool.size >= MIN_SECTOR_COMBINATION_SIZE) {
            "representativeSectors must contain at least $MIN_SECTOR_COMBINATION_SIZE entries"
        }

        val sectorCount = random.nextInt(MIN_SECTOR_COMBINATION_SIZE, MAX_SECTOR_COMBINATION_SIZE + 1)
        val sectors = sectorPool.shuffled(random).take(sectorCount)
        val marketTone = randomMarketTone()

        return ScheduledSectorContext(
            sectors = sectors,
            marketContext = SectorMarketContext(
                sector = sectors.joinToString(" + "),
                marketDataAsOf = LocalDateTime.now().toLocalDate().toString(),
                referenceSignal = marketTone.referenceSignal,
                sectorBias = marketTone.sectorBias,
                dataReliability = "개별 종목이 아닌 대표 섹터 조합 기반으로 단순화한 스케줄용 시장 컨텍스트",
                marketNarrative = "대표 섹터 조합을 바탕으로 생성한 스케줄 전용 시장 서사입니다.",
                tradingSignal = "스케줄 생성 컨텍스트라 개별 거래 세부 지표는 포함하지 않음",
                fundamentalSignal = "스케줄 생성 컨텍스트라 개별 펀더멘털 지표는 포함하지 않음"
            )
        )
    }

    private fun randomMarketTone(): SchedulerMarketTone =
        SCHEDULER_MARKET_TONES.random(random)

    private fun randomTarotIndices(): List<Int> =
        (0 until tarotDeckService.getDeckCardCount(DEFAULT_TAROT_DECK_VERSION_ID)).shuffled(random).take(TAROT_CARD_COUNT)

    private companion object {
        private const val TAROT_CARD_COUNT = 3
        private const val MIN_SECTOR_COMBINATION_SIZE = 2
        private const val MAX_SECTOR_COMBINATION_SIZE = 3
        private val logger = LoggerFactory.getLogger(AutomatedConsultingSchedulerService::class.java)
        private val random = Random.Default
        private val SCHEDULER_MARKET_TONES = listOf(
            SchedulerMarketTone(
                referenceSignal = "대표 섹터 전반에 완만한 순환매와 선택적 강세가 감지됨",
                sectorBias = "추격 매수보다 분할 접근이 유리한 중립 이상 흐름"
            ),
            SchedulerMarketTone(
                referenceSignal = "대표 섹터 전반에 매수세가 약해지고 관망 심리가 짙어짐",
                sectorBias = "방어적 접근과 현금 비중 점검이 필요한 약세 흐름"
            ),
            SchedulerMarketTone(
                referenceSignal = "대표 섹터 간 강약이 뚜렷하게 갈리며 테마별 차별화가 진행 중",
                sectorBias = "확신이 약한 구간이라 선별 대응이 필요한 혼조 흐름"
            )
        )
    }
}

private data class SchedulerMarketTone(
    val referenceSignal: String,
    val sectorBias: String
)

private enum class SchedulerDeduplicationWindow {
    DAILY,
    HOURLY;

    fun resolveWindow(now: LocalDateTime): Pair<LocalDateTime, LocalDateTime> =
        when (this) {
            DAILY -> {
                val start = now.toLocalDate().atStartOfDay()
                start to start.plusDays(1)
            }

            HOURLY -> {
                val start = now.truncatedTo(ChronoUnit.HOURS)
                start to start.plusHours(1)
            }
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
