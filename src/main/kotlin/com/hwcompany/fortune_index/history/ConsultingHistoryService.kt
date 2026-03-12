package com.hwcompany.fortune_index.history

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.FiveElementsInput
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import com.hwcompany.fortune_index.consulting.ConsultingHistoryListItemResponse
import com.hwcompany.fortune_index.domain.model.ConsultingFeedback
import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import com.hwcompany.fortune_index.domain.model.FiveElementsProfile
import com.hwcompany.fortune_index.domain.model.SajuSnapshot
import com.hwcompany.fortune_index.domain.model.StockQuoteSnapshot
import com.hwcompany.fortune_index.domain.model.TarotHistorySnapshot
import com.hwcompany.fortune_index.domain.model.TarotOrientation
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotReadingResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ConsultingHistoryService(
    private val consultingHistoryRepository: ConsultingHistoryRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    /**
     * 기존 서비스와의 호환을 위해 남겨 둔 저장 메서드다.
     * 내부적으로는 신규 엔티티 필드도 함께 채워서 저장한다.
     */
    @Transactional
    fun saveHistory(command: SaveConsultingHistoryCommand): ConsultingHistoryDetailResponse {
        val user = userRepository.findById(command.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${command.userId}") }

        val history = consultingHistoryRepository.save(
            ConsultingHistory(
                user = user,
                analysisMode = AnalysisMode.STOCK_ALL,
                scenario = null,
                consultedAt = command.consultedAt,
                selectedStockName = command.stockName,
                stockSnapshot = StockQuoteSnapshot(
                    ticker = command.stockInfo.ticker,
                    companyName = command.stockName,
                    marketPrice = command.stockInfo.currentPrice,
                    priceChangeRate = command.stockInfo.changeRate,
                    capturedAt = command.consultedAt
                ),
                sajuSnapshot = SajuSnapshot(
                    fiveElements = FiveElementsProfile(
                        wood = command.fiveElements.wood,
                        fire = command.fiveElements.fire,
                        earth = command.fiveElements.earth,
                        metal = command.fiveElements.metal,
                        water = command.fiveElements.water
                    ),
                    summary = command.sajuSummary
                ),
                tarotSnapshot = legacyTarotSnapshot(command),
                aiAnswerText = command.aiAnswerText,
                analysisResultJson = "{}",
                aiResponseJson = objectMapper.writeValueAsString(
                    mapOf(
                        "mode" to AnalysisMode.STOCK_ALL.name,
                        "analysis_results" to mapOf(
                            "market_analysis" to mapOf("title" to "증시 관련 분석", "content" to ""),
                            "tarot_analysis" to mapOf("title" to "타로 카드 분석", "content" to ""),
                            "saju_analysis" to mapOf("title" to "사주 분석", "content" to "")
                        ),
                        "overall_summary" to command.aiAnswerText,
                        "risk_score" to 50
                    )
                ),
                shareKey = UUID.randomUUID().toString()
            )
        )

        return history.toDetailResponse(objectMapper)
    }

    /**
     * 신규 하이브리드 상담 결과를 JSON 원문까지 포함해 저장한다.
     */
    @Transactional
    fun saveHybridHistory(command: SaveHybridConsultingHistoryCommand): SharedConsultingHistoryResponse {
        val user = userRepository.findById(command.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${command.userId}") }

        val history = consultingHistoryRepository.save(
            ConsultingHistory(
                user = user,
                analysisMode = command.mode,
                scenario = command.scenario,
                consultedAt = command.consultedAt,
                selectedStockName = command.stockName,
                stockSnapshot = StockQuoteSnapshot(
                    ticker = command.stockCode,
                    companyName = command.stockName,
                    marketPrice = command.stockInfo.currentPrice,
                    priceChangeRate = command.stockInfo.changeRate,
                    capturedAt = command.consultedAt
                ),
                sajuSnapshot = command.sajuResult.toSnapshot(),
                tarotSnapshot = command.tarotReading.toSnapshot(objectMapper),
                aiAnswerText = command.aiResponse.finalAdvice,
                analysisResultJson = command.analysisResultJson,
                aiResponseJson = command.aiResponse.rawJson,
                shareKey = UUID.randomUUID().toString()
            )
        )

        return history.toSharedResponse(objectMapper)
    }

    @Transactional(readOnly = true)
    fun getHybridHistoryList(userId: Long): List<ConsultingHistoryListItemResponse> {
        verifyUserExists(userId)
        return consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(userId)
            .map { history ->
                ConsultingHistoryListItemResponse(
                    id = requireNotNull(history.id),
                    shareKey = history.shareKey,
                    mode = history.analysisMode,
                    scenario = history.scenario,
                    stockCode = history.stockSnapshot.ticker,
                    stockName = history.selectedStockName,
                    consultedAt = history.consultedAt,
                    aiSummary = history.aiAnswerText,
                    tarotInterpretationMode = history.tarotSnapshot.interpretationMode?.name,
                    tarotCardCodes = history.tarotSnapshot.toStoredCards(objectMapper).map { it.code },
                    tarotCardNames = history.tarotSnapshot.toStoredCards(objectMapper).map { it.name }
                )
            }
    }

    @Transactional(readOnly = true)
    fun getHybridHistoryDetail(historyId: Long, userId: Long?): SharedConsultingHistoryResponse {
        val history = when (userId) {
            null -> consultingHistoryRepository.findById(historyId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "consulting history not found: $historyId")
            }

            else -> {
                verifyUserExists(userId)
                consultingHistoryRepository.findByIdAndUserId(historyId, userId)
                    ?: throw ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "consulting history not found: userId=$userId, historyId=$historyId"
                    )
            }
        }

        return history.toSharedResponse(objectMapper)
    }

    @Transactional(readOnly = true)
    fun getSharedHistory(shareKey: String): SharedConsultingHistoryResponse =
        consultingHistoryRepository.findByShareKey(shareKey)
            ?.toSharedResponse(objectMapper)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "shared consulting history not found: $shareKey")

    @Transactional(readOnly = true)
    fun getHistories(userId: Long, pageable: Pageable): Page<ConsultingHistorySummaryResponse> {
        verifyUserExists(userId)
        return consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(userId, pageable)
            .map { it.toSummaryResponse(objectMapper) }
    }

    @Transactional(readOnly = true)
    fun getHistoryDates(userId: Long, pageable: Pageable): Page<ConsultingHistoryDateSummaryResponse> {
        verifyUserExists(userId)
        val grouped = consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(userId)
            .groupBy { it.consultedAt.toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, histories) ->
                ConsultingHistoryDateSummaryResponse(
                    date = date,
                    totalConsultings = histories.size,
                    labels = histories
                        .groupingBy { it.toLabel() }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .map { (label, count) ->
                            ConsultingHistoryDateLabelResponse(
                                code = label.code,
                                title = label.title,
                                count = count
                            )
                        }
                )
            }

        val startIndex = pageable.offset.toInt().coerceAtMost(grouped.size)
        val endIndex = (startIndex + pageable.pageSize).coerceAtMost(grouped.size)
        val content = if (startIndex >= endIndex) emptyList() else grouped.subList(startIndex, endIndex)

        return PageImpl(content, pageable, grouped.size.toLong())
    }

    @Transactional(readOnly = true)
    fun getHistoryDetailsByDate(userId: Long, date: LocalDate): ConsultingHistoryDateDetailResponse {
        verifyUserExists(userId)
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        val histories = consultingHistoryRepository.findByUserIdAndConsultedAtBetweenOrderByConsultedAtDesc(userId, start, end)

        return ConsultingHistoryDateDetailResponse(
            date = date,
            consultings = histories.map { it.toDateItemResponse(objectMapper) }
        )
    }

    @Transactional(readOnly = true)
    fun getHistoryDetail(userId: Long, historyId: Long): ConsultingHistoryDetailResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )
        return history.toDetailResponse(objectMapper)
    }

    @Transactional
    fun updateRetro(userId: Long, historyId: Long, request: UpdateConsultingRetroRequest): ConsultingHistoryDetailResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )

        history.feedback = request.feedback
        history.realizedProfitRate = request.realizedProfitRate
        history.retroNote = request.retroNote?.trim()?.takeIf { it.isNotEmpty() }
        history.retrospectedAt = request.retrospectedAt ?: LocalDateTime.now()

        return history.toDetailResponse(objectMapper)
    }

    @Transactional
    fun updateReview(userId: Long, historyId: Long, request: UpdateConsultingReviewRequest): ConsultingHistoryReviewResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )

        history.feedback = request.satisfaction
        history.realizedProfitRate = request.realizedProfitRate
        history.retroNote = request.reviewNote?.trim()?.takeIf { it.isNotEmpty() }
        history.retrospectedAt = request.reviewedAt ?: LocalDateTime.now()

        return history.toReviewResponse()
    }

    @Transactional(readOnly = true)
    fun getRetroStats(userId: Long): ConsultingRetroStatsResponse {
        verifyUserExists(userId)
        val histories = consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(
            userId = userId,
            pageable = Pageable.unpaged()
        ).content

        val reviewed = histories.filter { it.feedback != null || it.realizedProfitRate != null }
        val helpfulCount = reviewed.count { it.feedback == ConsultingFeedback.HELPFUL }
        val profitableCount = reviewed.count { (it.realizedProfitRate ?: BigDecimal.ZERO) > BigDecimal.ZERO }
        val realizedProfitCount = reviewed.count { it.realizedProfitRate != null }
        val averageProfitRate = if (realizedProfitCount == 0) {
            BigDecimal.ZERO
        } else {
            reviewed
                .mapNotNull { it.realizedProfitRate }
                .reduce(BigDecimal::add)
                .divide(BigDecimal.valueOf(realizedProfitCount.toLong()), 4, RoundingMode.HALF_UP)
        }

        return ConsultingRetroStatsResponse(
            totalConsultings = histories.size,
            reviewedConsultings = reviewed.size,
            helpfulConsultings = helpfulCount,
            helpfulRate = percentage(helpfulCount, reviewed.size),
            profitableConsultings = profitableCount,
            profitabilityRate = percentage(profitableCount, reviewed.size),
            averageRealizedProfitRate = averageProfitRate
        )
    }

    private fun verifyUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId")
        }
    }

    private fun legacyTarotSnapshot(command: SaveConsultingHistoryCommand): TarotHistorySnapshot =
        TarotHistorySnapshot(
            interpretationMode = TarotInterpretationMode.MAIN_TRADITIONAL,
            cardsJson = objectMapper.writeValueAsString(
                listOf(
                    StoredTarotCardSnapshot(
                        selectedIndex = command.tarotIndex,
                        code = command.tarotCard.code,
                        deckType = command.tarotCard.deckType.name,
                        name = command.tarotCard.displayName,
                        cardNumber = command.tarotCard.cardNumber,
                        sortOrder = command.tarotCard.sortOrder,
                        arcanaType = command.tarotCard.arcanaType.name,
                        suit = command.tarotCard.suit?.name,
                        meaning = command.tarotCard.uprightMeaning,
                        imageUrl = command.tarotCard.imageUrl,
                        videoUrl = command.tarotCard.videoUrl
                    )
                )
            ),
            summary = command.tarotCard.displayName
        )

    private fun percentage(numerator: Int, denominator: Int): BigDecimal {
        if (denominator == 0) return BigDecimal.ZERO
        return BigDecimal.valueOf(numerator.toLong())
            .multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(denominator.toLong()), 2, RoundingMode.HALF_UP)
    }
}

data class SaveHybridConsultingHistoryCommand(
    val userId: Long,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario,
    val stockCode: String,
    val stockName: String,
    val stockInfo: StockInfo,
    val sajuResult: SajuConsultingResult?,
    val tarotReading: TarotReadingResult?,
    val analysisResultJson: String,
    val aiResponse: HybridConsultingAiResponse,
    val consultedAt: LocalDateTime = LocalDateTime.now()
)

data class SaveConsultingHistoryCommand(
    val userId: Long,
    val stockName: String,
    val stockInfo: StockInfo,
    val fiveElements: FiveElementsInput,
    val sajuSummary: String,
    val tarotIndex: Int,
    val tarotCard: TarotCard,
    val orientation: TarotOrientation = TarotOrientation.UPRIGHT,
    val aiAnswerText: String,
    val consultedAt: LocalDateTime = LocalDateTime.now()
)

data class ConsultingHistorySummaryResponse(
    val id: Long,
    val scenario: ConsultingScenario?,
    val consultedAt: LocalDateTime,
    val selectedStockName: String,
    val stockTicker: String,
    val stockPrice: BigDecimal,
    val stockChangeRate: BigDecimal,
    val tarotInterpretationMode: String?,
    val tarotCardCodes: List<String>,
    val tarotCardNames: List<String>,
    val feedback: String? = null,
    val realizedProfitRate: BigDecimal? = null,
    val retrospectedAt: LocalDateTime? = null
)

data class ConsultingHistoryDateSummaryResponse(
    val date: LocalDate,
    val totalConsultings: Int,
    val labels: List<ConsultingHistoryDateLabelResponse>
)

data class ConsultingHistoryDateLabelResponse(
    val code: String,
    val title: String,
    val count: Int
)

data class ConsultingHistoryDateDetailResponse(
    val date: LocalDate,
    val consultings: List<ConsultingHistoryDateItemResponse>
)

data class ConsultingHistoryDateItemResponse(
    val id: Long,
    val consultedAt: LocalDateTime,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario?,
    val label: ConsultingHistoryDateLabelResponse,
    val shareKey: String,
    val selectedStockName: String,
    val aiAnswerText: String,
    val stock: StockSnapshotResponse,
    val tarotCardNames: List<String>,
    val review: ConsultingHistoryReviewResponse
)

data class ConsultingHistoryDetailResponse(
    val id: Long,
    val userId: Long,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario?,
    val shareKey: String,
    val consultedAt: LocalDateTime,
    val selectedStockName: String,
    val stock: StockSnapshotResponse,
    val saju: SajuSnapshotResponse,
    val tarot: TarotSnapshotResponse,
    val aiAnswerText: String,
    val analysisResultJson: String,
    val aiResponseJson: String,
    val retro: ConsultingRetroResponse
)

data class SharedConsultingHistoryResponse(
    val id: Long,
    val userId: Long,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario?,
    val shareKey: String,
    val consultedAt: LocalDateTime,
    val stock: StockSnapshotResponse,
    val saju: SajuSnapshotResponse,
    val tarot: TarotSnapshotResponse,
    val aiAnswerText: String,
    val analysisResultJson: String,
    val aiResponseJson: String
)

data class StockSnapshotResponse(
    val ticker: String,
    val companyName: String,
    val marketPrice: BigDecimal,
    val changeRate: BigDecimal,
    val capturedAt: LocalDateTime
)

data class SajuSnapshotResponse(
    val wood: BigDecimal,
    val fire: BigDecimal,
    val earth: BigDecimal,
    val metal: BigDecimal,
    val water: BigDecimal,
    val summary: String
)

data class TarotSnapshotResponse(
    val interpretationMode: String?,
    val summary: String,
    val cards: List<TarotCardHistoryResponse>
)

data class TarotCardHistoryResponse(
    val selectedIndex: Int,
    val code: String,
    val deckType: String,
    val name: String,
    val cardNumber: Int,
    val sortOrder: Int,
    val arcanaType: String,
    val suit: String?,
    val meaning: String,
    val imageUrl: String?,
    val videoUrl: String?
)

data class ConsultingRetroResponse(
    val feedback: String?,
    val realizedProfitRate: BigDecimal?,
    val retroNote: String?,
    val retrospectedAt: LocalDateTime?,
    val reviewed: Boolean
)

data class UpdateConsultingRetroRequest(
    val feedback: ConsultingFeedback? = null,
    val realizedProfitRate: BigDecimal? = null,
    val retroNote: String? = null,
    val retrospectedAt: LocalDateTime? = null
)

data class UpdateConsultingReviewRequest(
    val satisfaction: ConsultingFeedback? = null,
    val realizedProfitRate: BigDecimal? = null,
    val reviewNote: String? = null,
    val reviewedAt: LocalDateTime? = null
)

data class ConsultingHistoryReviewResponse(
    val satisfaction: String?,
    val realizedProfitRate: BigDecimal?,
    val reviewNote: String?,
    val reviewedAt: LocalDateTime?,
    val reviewed: Boolean
)

data class ConsultingRetroStatsResponse(
    val totalConsultings: Int,
    val reviewedConsultings: Int,
    val helpfulConsultings: Int,
    val helpfulRate: BigDecimal,
    val profitableConsultings: Int,
    val profitabilityRate: BigDecimal,
    val averageRealizedProfitRate: BigDecimal
)

private fun SajuConsultingResult?.toSnapshot(): SajuSnapshot =
    when (this) {
        null -> SajuSnapshot(
            fiveElements = FiveElementsProfile(),
            summary = "사주 분석이 포함되지 않은 상담입니다."
        )

        else -> SajuSnapshot(
            fiveElements = FiveElementsProfile(
                wood = BigDecimal.valueOf(analysis.fiveElementBalance.wood.toLong()),
                fire = BigDecimal.valueOf(analysis.fiveElementBalance.fire.toLong()),
                earth = BigDecimal.valueOf(analysis.fiveElementBalance.earth.toLong()),
                metal = BigDecimal.valueOf(analysis.fiveElementBalance.metal.toLong()),
                water = BigDecimal.valueOf(analysis.fiveElementBalance.water.toLong())
            ),
            summary = "일간 ${dayMaster.symbol}, 일지 ${dayBranch.symbol}, 월지 ${monthBranch.symbol}, 대운 ${currentFortune.majorFortune.pillar}, 세운 ${currentFortune.yearlyFortune.pillar}"
        )
    }

private fun TarotReadingResult?.toSnapshot(objectMapper: ObjectMapper): TarotHistorySnapshot =
    when (this) {
        null -> TarotHistorySnapshot(
            interpretationMode = null,
            cardsJson = "[]",
            summary = "타로 분석이 포함되지 않은 상담입니다."
        )

        else -> TarotHistorySnapshot(
            interpretationMode = interpretationMode,
            cardsJson = objectMapper.writeValueAsString(
                cards.map { draw ->
                    StoredTarotCardSnapshot(
                        selectedIndex = draw.index,
                        code = draw.card.code,
                        deckType = draw.card.deckType.name,
                        name = draw.card.displayName,
                        cardNumber = draw.card.cardNumber,
                        sortOrder = draw.card.sortOrder,
                        arcanaType = draw.card.arcanaType.name,
                        suit = draw.card.suit?.name,
                        meaning = draw.card.uprightMeaning,
                        imageUrl = draw.card.imageUrl,
                        videoUrl = draw.card.videoUrl
                    )
                }
            ),
            summary = cards.joinToString(" / ") { it.card.displayName }
        )
    }

private fun ConsultingHistory.toSummaryResponse(objectMapper: ObjectMapper): ConsultingHistorySummaryResponse =
    ConsultingHistorySummaryResponse(
        id = requireNotNull(id),
        scenario = scenario,
        consultedAt = consultedAt,
        selectedStockName = selectedStockName,
        stockTicker = stockSnapshot.ticker,
        stockPrice = stockSnapshot.marketPrice,
        stockChangeRate = stockSnapshot.priceChangeRate,
        tarotInterpretationMode = tarotSnapshot.interpretationMode?.name,
        tarotCardCodes = tarotSnapshot.toStoredCards(objectMapper).map { it.code },
        tarotCardNames = tarotSnapshot.toStoredCards(objectMapper).map { it.name },
        feedback = feedback?.name,
        realizedProfitRate = realizedProfitRate,
        retrospectedAt = retrospectedAt
    )

private fun ConsultingHistory.toDetailResponse(objectMapper: ObjectMapper): ConsultingHistoryDetailResponse =
    ConsultingHistoryDetailResponse(
        id = requireNotNull(id),
        userId = requireNotNull(user.id),
        mode = analysisMode,
        scenario = scenario,
        shareKey = shareKey,
        consultedAt = consultedAt,
        selectedStockName = selectedStockName,
        stock = stockSnapshot.toResponse(),
        saju = sajuSnapshot.toResponse(),
        tarot = tarotSnapshot.toResponse(objectMapper),
        aiAnswerText = aiAnswerText,
        analysisResultJson = analysisResultJson,
        aiResponseJson = aiResponseJson,
        retro = ConsultingRetroResponse(
            feedback = feedback?.name,
            realizedProfitRate = realizedProfitRate,
            retroNote = retroNote,
            retrospectedAt = retrospectedAt,
            reviewed = feedback != null || realizedProfitRate != null || !retroNote.isNullOrBlank()
        )
    )

private fun ConsultingHistory.toDateItemResponse(objectMapper: ObjectMapper): ConsultingHistoryDateItemResponse =
    ConsultingHistoryDateItemResponse(
        id = requireNotNull(id),
        consultedAt = consultedAt,
        mode = analysisMode,
        scenario = scenario,
        label = toLabel().toResponse(1),
        shareKey = shareKey,
        selectedStockName = selectedStockName,
        aiAnswerText = aiAnswerText,
        stock = stockSnapshot.toResponse(),
        tarotCardNames = tarotSnapshot.toStoredCards(objectMapper).map { it.name },
        review = toReviewResponse()
    )

private fun ConsultingHistory.toReviewResponse(): ConsultingHistoryReviewResponse =
    ConsultingHistoryReviewResponse(
        satisfaction = feedback?.name,
        realizedProfitRate = realizedProfitRate,
        reviewNote = retroNote,
        reviewedAt = retrospectedAt,
        reviewed = feedback != null || realizedProfitRate != null || !retroNote.isNullOrBlank()
    )

private data class ConsultingHistoryLabel(
    val code: String,
    val title: String
)

private fun ConsultingHistory.toLabel(): ConsultingHistoryLabel =
    when (scenario) {
        null -> ConsultingHistoryLabel(
            code = analysisMode.name,
            title = analysisMode.toDisplayTitle()
        )

        else -> ConsultingHistoryLabel(
            code = requireNotNull(scenario).name,
            title = requireNotNull(scenario).title
        )
    }

private fun ConsultingHistoryLabel.toResponse(count: Int): ConsultingHistoryDateLabelResponse =
    ConsultingHistoryDateLabelResponse(
        code = code,
        title = title,
        count = count
    )

private fun AnalysisMode.toDisplayTitle(): String =
    when (this) {
        AnalysisMode.ONLY_STOCK -> "주식 상담"
        AnalysisMode.STOCK_SAJU -> "주식 + 사주 상담"
        AnalysisMode.STOCK_TAROT -> "주식 + 타로 상담"
        AnalysisMode.STOCK_ALL -> "주식 + 사주 + 타로 상담"
    }

private fun ConsultingHistory.toSharedResponse(objectMapper: ObjectMapper): SharedConsultingHistoryResponse =
    SharedConsultingHistoryResponse(
        id = requireNotNull(id),
        userId = requireNotNull(user.id),
        mode = analysisMode,
        scenario = scenario,
        shareKey = shareKey,
        consultedAt = consultedAt,
        stock = stockSnapshot.toResponse(),
        saju = sajuSnapshot.toResponse(),
        tarot = tarotSnapshot.toResponse(objectMapper),
        aiAnswerText = aiAnswerText,
        analysisResultJson = analysisResultJson,
        aiResponseJson = aiResponseJson
    )

private fun StockQuoteSnapshot.toResponse(): StockSnapshotResponse =
    StockSnapshotResponse(
        ticker = ticker,
        companyName = companyName,
        marketPrice = marketPrice,
        changeRate = priceChangeRate,
        capturedAt = capturedAt
    )

private fun SajuSnapshot.toResponse(): SajuSnapshotResponse =
    SajuSnapshotResponse(
        wood = fiveElements.wood,
        fire = fiveElements.fire,
        earth = fiveElements.earth,
        metal = fiveElements.metal,
        water = fiveElements.water,
        summary = summary
    )

private fun TarotHistorySnapshot.toStoredCards(objectMapper: ObjectMapper): List<StoredTarotCardSnapshot> =
    objectMapper.readValue(cardsJson, object : TypeReference<List<StoredTarotCardSnapshot>>() {})

private fun TarotHistorySnapshot.toResponse(objectMapper: ObjectMapper): TarotSnapshotResponse =
    TarotSnapshotResponse(
        interpretationMode = interpretationMode?.name,
        summary = summary,
        cards = toStoredCards(objectMapper).map {
            TarotCardHistoryResponse(
                selectedIndex = it.selectedIndex,
                code = it.code,
                deckType = it.deckType,
                name = it.name,
                cardNumber = it.cardNumber,
                sortOrder = it.sortOrder,
                arcanaType = it.arcanaType,
                suit = it.suit,
                meaning = it.meaning,
                imageUrl = it.imageUrl,
                videoUrl = it.videoUrl
            )
        }
    )

private data class StoredTarotCardSnapshot(
    val selectedIndex: Int,
    val code: String,
    val deckType: String = "TAROT",
    val name: String,
    val cardNumber: Int,
    val sortOrder: Int = cardNumber,
    val arcanaType: String,
    val suit: String?,
    val meaning: String,
    val imageUrl: String?,
    val videoUrl: String? = null
)
