package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.ai.FiveElementsInput
import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import com.hwcompany.fortune_index.domain.model.ConsultingFeedback
import com.hwcompany.fortune_index.domain.model.FiveElementsProfile
import com.hwcompany.fortune_index.domain.model.SajuSnapshot
import com.hwcompany.fortune_index.domain.model.StockQuoteSnapshot
import com.hwcompany.fortune_index.domain.model.TarotCardDraw
import com.hwcompany.fortune_index.domain.model.TarotHistorySnapshot
import com.hwcompany.fortune_index.domain.model.TarotOrientation
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.tarot.TarotCard
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ConsultingHistoryService(
    private val consultingHistoryRepository: ConsultingHistoryRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun saveHistory(command: SaveConsultingHistoryCommand): ConsultingHistoryDetailResponse {
        val user = userRepository.findById(command.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${command.userId}") }

        val history = consultingHistoryRepository.save(
            ConsultingHistory(
                user = user,
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
                tarotSnapshot = TarotHistorySnapshot(
                    tarotCardId = command.tarotCard.name,
                    tarotDraw = TarotCardDraw(
                        cardName = command.tarotCard.displayName,
                        orientation = command.orientation,
                        interpretation = command.tarotCard.uprightMeaning
                    ),
                    imageUrl = command.tarotCard.imageUrl,
                    selectedIndex = command.tarotIndex
                ),
                aiAnswerText = command.aiAnswerText
            )
        )

        return history.toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun getHistories(userId: Long, pageable: Pageable): Page<ConsultingHistorySummaryResponse> {
        verifyUserExists(userId)
        return consultingHistoryRepository.findByUserIdOrderByConsultedAtDesc(userId, pageable)
            .map { it.toSummaryResponse() }
    }

    @Transactional(readOnly = true)
    fun getHistoryDetail(userId: Long, historyId: Long): ConsultingHistoryDetailResponse {
        verifyUserExists(userId)
        val history = consultingHistoryRepository.findByIdAndUserId(historyId, userId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "consulting history not found: userId=$userId, historyId=$historyId"
            )
        return history.toDetailResponse()
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

        return history.toDetailResponse()
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
        val profitableCount = reviewed.count { (it.realizedProfitRate ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO }
        val averageProfitRate = reviewed
            .mapNotNull { it.realizedProfitRate }
            .takeIf { it.isNotEmpty() }
            ?.reduce(java.math.BigDecimal::add)
            ?.divide(java.math.BigDecimal.valueOf(reviewed.count { it.realizedProfitRate != null }.toLong()), 4, java.math.RoundingMode.HALF_UP)
            ?: java.math.BigDecimal.ZERO

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

    private fun percentage(numerator: Int, denominator: Int): java.math.BigDecimal {
        if (denominator == 0) return java.math.BigDecimal.ZERO
        return java.math.BigDecimal.valueOf(numerator.toLong())
            .multiply(java.math.BigDecimal("100"))
            .divide(java.math.BigDecimal.valueOf(denominator.toLong()), 2, java.math.RoundingMode.HALF_UP)
    }
}

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
    val consultedAt: LocalDateTime,
    val selectedStockName: String,
    val stockTicker: String,
    val stockPrice: java.math.BigDecimal,
    val stockChangeRate: java.math.BigDecimal,
    val tarotCardId: String,
    val tarotCardName: String,
    val feedback: String? = null,
    val realizedProfitRate: java.math.BigDecimal? = null,
    val retrospectedAt: LocalDateTime? = null
)

data class ConsultingHistoryDetailResponse(
    val id: Long,
    val userId: Long,
    val consultedAt: LocalDateTime,
    val selectedStockName: String,
    val stock: StockSnapshotResponse,
    val saju: SajuSnapshotResponse,
    val tarot: TarotSnapshotResponse,
    val aiAnswerText: String,
    val retro: ConsultingRetroResponse
)

data class StockSnapshotResponse(
    val ticker: String,
    val companyName: String,
    val marketPrice: java.math.BigDecimal,
    val changeRate: java.math.BigDecimal,
    val capturedAt: LocalDateTime
)

data class SajuSnapshotResponse(
    val wood: java.math.BigDecimal,
    val fire: java.math.BigDecimal,
    val earth: java.math.BigDecimal,
    val metal: java.math.BigDecimal,
    val water: java.math.BigDecimal,
    val summary: String
)

data class TarotSnapshotResponse(
    val tarotCardId: String,
    val cardName: String,
    val interpretation: String,
    val imageUrl: String?,
    val selectedIndex: Int,
    val orientation: String
)

data class ConsultingRetroResponse(
    val feedback: String?,
    val realizedProfitRate: java.math.BigDecimal?,
    val retroNote: String?,
    val retrospectedAt: LocalDateTime?,
    val reviewed: Boolean
)

data class UpdateConsultingRetroRequest(
    val feedback: ConsultingFeedback? = null,
    val realizedProfitRate: java.math.BigDecimal? = null,
    val retroNote: String? = null,
    val retrospectedAt: LocalDateTime? = null
)

data class ConsultingRetroStatsResponse(
    val totalConsultings: Int,
    val reviewedConsultings: Int,
    val helpfulConsultings: Int,
    val helpfulRate: java.math.BigDecimal,
    val profitableConsultings: Int,
    val profitabilityRate: java.math.BigDecimal,
    val averageRealizedProfitRate: java.math.BigDecimal
)

private fun ConsultingHistory.toSummaryResponse(): ConsultingHistorySummaryResponse =
    ConsultingHistorySummaryResponse(
        id = requireNotNull(id),
        consultedAt = consultedAt,
        selectedStockName = selectedStockName,
        stockTicker = stockSnapshot.ticker,
        stockPrice = stockSnapshot.marketPrice,
        stockChangeRate = stockSnapshot.priceChangeRate,
        tarotCardId = tarotSnapshot.tarotCardId,
        tarotCardName = tarotSnapshot.tarotDraw.cardName,
        feedback = feedback?.name,
        realizedProfitRate = realizedProfitRate,
        retrospectedAt = retrospectedAt
    )

private fun ConsultingHistory.toDetailResponse(): ConsultingHistoryDetailResponse =
    ConsultingHistoryDetailResponse(
        id = requireNotNull(id),
        userId = requireNotNull(user.id),
        consultedAt = consultedAt,
        selectedStockName = selectedStockName,
        stock = StockSnapshotResponse(
            ticker = stockSnapshot.ticker,
            companyName = stockSnapshot.companyName,
            marketPrice = stockSnapshot.marketPrice,
            changeRate = stockSnapshot.priceChangeRate,
            capturedAt = stockSnapshot.capturedAt
        ),
        saju = SajuSnapshotResponse(
            wood = sajuSnapshot.fiveElements.wood,
            fire = sajuSnapshot.fiveElements.fire,
            earth = sajuSnapshot.fiveElements.earth,
            metal = sajuSnapshot.fiveElements.metal,
            water = sajuSnapshot.fiveElements.water,
            summary = sajuSnapshot.summary
        ),
        tarot = TarotSnapshotResponse(
            tarotCardId = tarotSnapshot.tarotCardId,
            cardName = tarotSnapshot.tarotDraw.cardName,
            interpretation = tarotSnapshot.tarotDraw.interpretation,
            imageUrl = tarotSnapshot.imageUrl,
            selectedIndex = tarotSnapshot.selectedIndex,
            orientation = tarotSnapshot.tarotDraw.orientation.name
        ),
        aiAnswerText = aiAnswerText,
        retro = ConsultingRetroResponse(
            feedback = feedback?.name,
            realizedProfitRate = realizedProfitRate,
            retroNote = retroNote,
            retrospectedAt = retrospectedAt,
            reviewed = feedback != null || realizedProfitRate != null || !retroNote.isNullOrBlank()
        )
    )
