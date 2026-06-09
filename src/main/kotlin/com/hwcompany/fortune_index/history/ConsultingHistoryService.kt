package com.hwcompany.fortune_index.history

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.ai.HybridConsultingPayload
import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultingHistoryAnalysisResponse
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import com.hwcompany.fortune_index.consulting.ConsultingHistoryListItemResponse
import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.domain.model.ConsultingFeedback
import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import com.hwcompany.fortune_index.domain.model.FiveElementsProfile
import com.hwcompany.fortune_index.domain.model.InvestmentFocusSnapshot
import com.hwcompany.fortune_index.domain.model.SajuSnapshot
import com.hwcompany.fortune_index.domain.model.TarotHistorySnapshot
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.tarot.TarotDeckRole
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
                investmentSnapshot = InvestmentFocusSnapshot(
                    ticker = ConsultingHistoryPersistenceSanitizer.ticker(command.focusLabel),
                    label = ConsultingHistoryPersistenceSanitizer.focusLabel(command.focusLabel),
                    currentValue = BigDecimal.ZERO,
                    changeRate = BigDecimal.ZERO,
                    capturedAt = command.consultedAt
                ),
                sajuSnapshot = command.sajuResult.toSnapshot(),
                tarotSnapshot = command.tarotReading.toSnapshot(objectMapper),
                question = command.question,
                analysisResultJson = command.analysisResultJson,
                aiResponseJson = command.aiResponse.rawJson,
                aiSafetyGuardApplied = command.aiResponse.safetyGuard?.applied == true,
                aiSafetyGuardReason = command.aiResponse.safetyGuard?.reason,
                aiSafetyGuardMatchedRules = command.aiResponse.safetyGuard?.matchedRules?.joinToString(","),
                aiSafetyGuardOriginalText = command.aiResponse.safetyGuard?.originalText,
                aiSafetyGuardSanitizedText = command.aiResponse.safetyGuard?.sanitizedText,
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
                val aiResponse = history.toStoredAiResponse(objectMapper)
                ConsultingHistoryListItemResponse(
                    id = requireNotNull(history.id),
                    shareKey = history.shareKey,
                    mode = history.analysisMode,
                    scenario = history.scenario,
                    question = history.question,
                    focusLabel = history.displayFocusLabel(),
                    consultedAt = history.consultedAt,
                    stabilityScore = aiResponse.stabilityScore(),
                    overallSummary = aiResponse.overallSummary(),
                    analysis = aiResponse.toAnalysisResponse(history.analysisMode),
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
    fun getHelpfulHistories(userId: Long, pageable: Pageable): Page<ConsultingHistorySummaryResponse> {
        verifyUserExists(userId)
        return consultingHistoryRepository.findByUserIdAndFeedbackOrderByConsultedAtDesc(
            userId = userId,
            feedback = ConsultingFeedback.HELPFUL,
            pageable = pageable
        ).map { it.toSummaryResponse(objectMapper) }
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
    fun likeHistory(userId: Long, historyId: Long): ConsultingHistoryLikeResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )

        history.feedback = ConsultingFeedback.HELPFUL
        history.retrospectedAt = SeoulTime.now()

        return history.toLikeResponse()
    }

    @Transactional
    fun unlikeHistory(userId: Long, historyId: Long): ConsultingHistoryLikeResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )

        history.feedback = null
        history.retrospectedAt = SeoulTime.now()

        return history.toLikeResponse()
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
        history.retroNote = ConsultingHistoryPersistenceSanitizer.nullableText(request.retroNote)
        history.retrospectedAt = request.retrospectedAt ?: SeoulTime.now()

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
        history.retrospectedAt = request.reviewedAt ?: SeoulTime.now()

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
    val focusLabel: String,
    val question: String,
    val sajuResult: SajuConsultingResult?,
    val tarotReading: TarotReadingResult?,
    val analysisResultJson: String,
    val aiResponse: HybridConsultingAiResponse,
    val consultedAt: LocalDateTime = SeoulTime.now()
)

data class ConsultingHistorySummaryResponse(
    val id: Long,
    val scenario: ConsultingScenario?,
    val consultedAt: LocalDateTime,
    val question: String?,
    val selectedFocusLabel: String,
    val currentValue: BigDecimal,
    val changeRate: BigDecimal,
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
    val question: String?,
    val label: ConsultingHistoryDateLabelResponse,
    val shareKey: String,
    val selectedFocusLabel: String,
    val aiAnswerText: String,
    val focus: FocusSnapshotResponse,
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
    val selectedFocusLabel: String,
    val focus: FocusSnapshotResponse,
    val saju: SajuSnapshotResponse,
    val tarot: TarotSnapshotResponse,
    val question: String?,
    val stabilityScore: Int,
    val overallSummary: String,
    val analysis: ConsultingHistoryAnalysisResponse,
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
    val focus: FocusSnapshotResponse,
    val saju: SajuSnapshotResponse?,
    val tarot: TarotSnapshotResponse?,
    val question: String?,
    val stabilityScore: Int,
    val overallSummary: String,
    val analysis: ConsultingHistoryAnalysisResponse,
    val analysisResultJson: String,
    val aiResponseJson: String
)

data class FocusSnapshotResponse(
    val label: String,
    val currentValue: BigDecimal,
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
    val cards: List<TarotCardHistoryResponse>,
    val assistantDecks: List<TarotDeckHistoryResponse> = emptyList()
)

data class TarotDeckHistoryResponse(
    val deckVersionId: String?,
    val deckType: String,
    val deckRole: String,
    val cardSetId: String?,
    val cards: List<TarotCardHistoryResponse>
)

data class TarotCardHistoryResponse(
    // Stable card index within a deck version. This is not the UI slot index.
    val selectedIndex: Int,
    val code: String,
    val deckType: String,
    val deckRole: String = TarotDeckRole.MAIN.name,
    val deckVersionId: String? = null,
    val cardSetId: String? = null,
    val name: String,
    val koreanName: String? = null,
    val cardNumber: Int,
    val sortOrder: Int,
    val arcanaType: String?,
    val suit: String?,
    val meaning: String,
    val description: String?,
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

data class ConsultingHistoryLikeResponse(
    val historyId: Long,
    val liked: Boolean,
    val satisfaction: String?,
    val reviewedAt: LocalDateTime?
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
            summary = ConsultingHistoryPersistenceSanitizer.sajuSummary("사주 분석이 포함되지 않은 상담입니다.")
        )

        else -> SajuSnapshot(
            fiveElements = FiveElementsProfile(
                wood = BigDecimal.valueOf(analysis.fiveElementBalance.wood.toLong()),
                fire = BigDecimal.valueOf(analysis.fiveElementBalance.fire.toLong()),
                earth = BigDecimal.valueOf(analysis.fiveElementBalance.earth.toLong()),
                metal = BigDecimal.valueOf(analysis.fiveElementBalance.metal.toLong()),
                water = BigDecimal.valueOf(analysis.fiveElementBalance.water.toLong())
            ),
            summary = ConsultingHistoryPersistenceSanitizer.sajuSummary(
                "일간 ${dayMaster.symbol}, 일지 ${dayBranch.symbol}, 월지 ${monthBranch.symbol}, 대운 ${currentFortune.majorFortune.pillar}, 세운 ${currentFortune.yearlyFortune.pillar}"
            )
        )
    }

private fun TarotReadingResult?.toSnapshot(objectMapper: ObjectMapper): TarotHistorySnapshot =
    when (this) {
        null -> TarotHistorySnapshot(
            interpretationMode = null,
            cardsJson = "[]",
            summary = ConsultingHistoryPersistenceSanitizer.tarotSummary("타로 분석이 포함되지 않은 상담입니다.")
        )

        else -> TarotHistorySnapshot(
            interpretationMode = interpretationMode,
            cardsJson = objectMapper.writeValueAsString(
                cards.map { draw ->
                    StoredTarotCardSnapshot(
                        selectedIndex = draw.index,
                        code = draw.card.code,
                        deckType = draw.card.deckType.name,
                        deckRole = draw.card.deckRole.name,
                        deckVersionId = draw.card.deckVersionId,
                        cardSetId = draw.card.cardSetId,
                        name = draw.card.name,
                        koreanName = draw.card.koreanName,
                        cardNumber = draw.card.cardNumber,
                        sortOrder = draw.card.sortOrder,
                        arcanaType = draw.card.arcanaType?.name,
                        suit = draw.card.suit?.name,
                        meaning = draw.card.meaning,
                        description = draw.card.description,
                        imageUrl = draw.card.imageUrl,
                        videoUrl = draw.card.videoUrl
                    )
                } + assistantDecks.flatMap { deck ->
                    deck.cards.map { draw ->
                        StoredTarotCardSnapshot(
                            selectedIndex = draw.index,
                            code = draw.card.code,
                            deckType = draw.card.deckType.name,
                            deckRole = draw.card.deckRole.name,
                            deckVersionId = draw.card.deckVersionId,
                            cardSetId = draw.card.cardSetId,
                            name = draw.card.name,
                            koreanName = draw.card.koreanName,
                            cardNumber = draw.card.cardNumber,
                            sortOrder = draw.card.sortOrder,
                            arcanaType = draw.card.arcanaType?.name,
                            suit = draw.card.suit?.name,
                            meaning = draw.card.meaning,
                            description = draw.card.description,
                            imageUrl = draw.card.imageUrl,
                            videoUrl = draw.card.videoUrl
                        )
                    }
                }
            ),
            summary = ConsultingHistoryPersistenceSanitizer.tarotSummary(
                (cards + assistantDecks.flatMap { it.cards }).joinToString(" / ") { it.card.name }
            )
        )
    }

private fun ConsultingHistory.toSummaryResponse(objectMapper: ObjectMapper): ConsultingHistorySummaryResponse =
    ConsultingHistorySummaryResponse(
        id = requireNotNull(id),
        scenario = scenario,
        consultedAt = consultedAt,
        question = question,
        selectedFocusLabel = displayFocusLabel(),
        currentValue = investmentSnapshot.currentValue,
        changeRate = investmentSnapshot.changeRate,
        tarotInterpretationMode = tarotSnapshot.interpretationMode?.name,
        tarotCardCodes = tarotSnapshot.toStoredCards(objectMapper).map { it.code },
        tarotCardNames = tarotSnapshot.toStoredCards(objectMapper).map { it.name },
        feedback = feedback?.name,
        realizedProfitRate = realizedProfitRate,
        retrospectedAt = retrospectedAt
    )

private fun ConsultingHistory.toDetailResponse(objectMapper: ObjectMapper): ConsultingHistoryDetailResponse =
    toStoredAiResponse(objectMapper).let { aiResponse ->
        ConsultingHistoryDetailResponse(
            id = requireNotNull(id),
            userId = requireNotNull(user.id),
            mode = analysisMode,
            scenario = scenario,
            shareKey = shareKey,
            consultedAt = consultedAt,
            selectedFocusLabel = displayFocusLabel(),
            focus = investmentSnapshot.toResponse(displayFocusLabel()),
            saju = sajuSnapshot.toResponse(),
            tarot = tarotSnapshot.toResponse(objectMapper),
            question = question,
            stabilityScore = aiResponse.stabilityScore(),
            overallSummary = aiResponse.overallSummary(),
            analysis = aiResponse.toAnalysisResponse(analysisMode),
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
    }

private fun ConsultingHistory.toDateItemResponse(objectMapper: ObjectMapper): ConsultingHistoryDateItemResponse =
    toStoredAiResponse(objectMapper).let { aiResponse ->
        ConsultingHistoryDateItemResponse(
            id = requireNotNull(id),
            consultedAt = consultedAt,
            mode = analysisMode,
            scenario = scenario,
            question = question,
            label = toLabel().toResponse(1),
            shareKey = shareKey,
            selectedFocusLabel = displayFocusLabel(),
            aiAnswerText = aiResponse.overallSummary(),
            focus = investmentSnapshot.toResponse(displayFocusLabel()),
            tarotCardNames = tarotSnapshot.toStoredCards(objectMapper).map { it.name },
            review = toReviewResponse()
        )
    }

private fun ConsultingHistory.toReviewResponse(): ConsultingHistoryReviewResponse =
    ConsultingHistoryReviewResponse(
        satisfaction = feedback?.name,
        realizedProfitRate = realizedProfitRate,
        reviewNote = retroNote,
        reviewedAt = retrospectedAt,
        reviewed = feedback != null || realizedProfitRate != null || !retroNote.isNullOrBlank()
    )

private fun ConsultingHistory.toLikeResponse(): ConsultingHistoryLikeResponse =
    ConsultingHistoryLikeResponse(
        historyId = requireNotNull(id),
        liked = feedback == ConsultingFeedback.HELPFUL,
        satisfaction = feedback?.name,
        reviewedAt = retrospectedAt
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
        AnalysisMode.INVESTMENT_SAJU -> "투자 + 사주 상담"
        AnalysisMode.INVESTMENT_TAROT -> "투자 + 타로 상담"
        AnalysisMode.INVESTMENT_ZODIAC -> "투자 + 별자리 상담"
        AnalysisMode.INVESTMENT_ALL -> "투자 + 사주 + 타로 + 별자리 상담"
    }

private fun ConsultingHistory.toSharedResponse(objectMapper: ObjectMapper): SharedConsultingHistoryResponse =
    toStoredAiResponse(objectMapper).let { aiResponse ->
        SharedConsultingHistoryResponse(
            id = requireNotNull(id),
            userId = requireNotNull(user.id),
            mode = analysisMode,
            scenario = scenario,
            shareKey = shareKey,
            consultedAt = consultedAt,
            focus = investmentSnapshot.toResponse(displayFocusLabel()),
            saju = sajuSnapshot.toResponse().takeIf { analysisMode.includesSaju() },
            tarot = tarotSnapshot.toResponse(objectMapper).takeIf { analysisMode.includesTarot() },
            question = question,
            stabilityScore = aiResponse.stabilityScore(),
            overallSummary = aiResponse.overallSummary(),
            analysis = aiResponse.toAnalysisResponse(analysisMode),
            analysisResultJson = analysisResultJson,
            aiResponseJson = aiResponseJson
        )
    }

private fun ConsultingHistory.toStoredAiResponse(objectMapper: ObjectMapper): HybridConsultingPayload =
    objectMapper.readValue(aiResponseJson, HybridConsultingPayload::class.java)

private fun HybridConsultingPayload.overallSummary(): String = overall_summary

private fun HybridConsultingPayload.stabilityScore(): Int = stability_score

private fun HybridConsultingPayload.tarotAnalysisContent(mode: AnalysisMode): String? =
    analysis_results.tarot_analysis?.content.takeIf { mode.includesTarot() }

private fun HybridConsultingPayload.sajuAnalysisContent(mode: AnalysisMode): String? =
    analysis_results.saju_analysis?.content.takeIf { mode.includesSaju() }

private fun HybridConsultingPayload.zodiacAnalysisContent(mode: AnalysisMode): String? =
    analysis_results.zodiac_analysis?.content.takeIf { mode.includesZodiac() }

private fun HybridConsultingPayload.toAnalysisResponse(mode: AnalysisMode): ConsultingHistoryAnalysisResponse =
    ConsultingHistoryAnalysisResponse(
        saju = sajuAnalysisContent(mode),
        tarot = tarotAnalysisContent(mode),
        zodiac = zodiacAnalysisContent(mode)
    )

private fun ConsultingHistory.displayFocusLabel(): String =
    scenario?.title ?: investmentSnapshot.label

private fun InvestmentFocusSnapshot.toResponse(label: String): FocusSnapshotResponse =
    FocusSnapshotResponse(
        label = label,
        currentValue = this.currentValue,
        changeRate = this.changeRate,
        capturedAt = this.capturedAt
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
    toStoredCards(objectMapper).let { storedCards ->
        val mainCards = storedCards
            .filter { it.deckRole == TarotDeckRole.MAIN.name }
            .map { it.toHistoryCardResponse() }
        val assistantDecks = storedCards
            .filter { it.deckRole == TarotDeckRole.ASSISTANT.name }
            .groupBy { listOf(it.deckVersionId, it.deckType, it.deckRole, it.cardSetId) }
            .values
            .map { deckCards ->
                TarotDeckHistoryResponse(
                    deckVersionId = deckCards.first().deckVersionId,
                    deckType = deckCards.first().deckType,
                    deckRole = deckCards.first().deckRole,
                    cardSetId = deckCards.first().cardSetId,
                    cards = deckCards.map { it.toHistoryCardResponse() }
                )
            }

        TarotSnapshotResponse(
            interpretationMode = interpretationMode?.name,
            summary = summary,
            cards = mainCards,
            assistantDecks = assistantDecks
        )
    }

private data class StoredTarotCardSnapshot(
    val selectedIndex: Int,
    val code: String,
    val deckType: String = "TAROT",
    val deckRole: String = TarotDeckRole.MAIN.name,
    val deckVersionId: String? = null,
    val cardSetId: String? = null,
    val name: String,
    val koreanName: String? = null,
    val cardNumber: Int,
    val sortOrder: Int = cardNumber,
    val arcanaType: String?,
    val suit: String?,
    val meaning: String,
    val description: String? = null,
    val imageUrl: String?,
    val videoUrl: String? = null
)

private fun StoredTarotCardSnapshot.toHistoryCardResponse(): TarotCardHistoryResponse =
    TarotCardHistoryResponse(
        selectedIndex = selectedIndex,
        code = code,
        deckType = deckType,
        deckRole = deckRole,
        deckVersionId = deckVersionId,
        cardSetId = cardSetId,
        name = name,
        koreanName = koreanName,
        cardNumber = cardNumber,
        sortOrder = sortOrder,
        arcanaType = arcanaType,
        suit = suit,
        meaning = meaning,
        description = description,
        imageUrl = imageUrl,
        videoUrl = videoUrl
    )
