package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.annotation.JsonAlias
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.auth.requireSameUserId
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SharedConsultingHistoryResponse
import com.hwcompany.fortune_index.market.MarketDataProvider
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.tarot.TarotAssistantDeckSelection
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotDeckType
import com.hwcompany.fortune_index.tarot.TarotDrawGroupResult
import com.hwcompany.fortune_index.tarot.TarotDrawResult
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotReadingResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "재물 운세 상담 API", description = "사주, 타로, 관심 분야 흐름을 바탕으로 오늘의 마음과 재물 흐름을 읽어주는 기능")
class ConsultingController(
    private val consultingService: ConsultingService,
    private val consultingHistoryService: ConsultingHistoryService
) {
    @Operation(summary = "재물 운세 상담 요청")
    @PostMapping("/consult")
    fun consult(
        authentication: Authentication,
        @Valid @RequestBody request: ConsultRequest
    ): ConsultResponse {
        authentication.requireSameUserId(request.userId)
        return consultingService.consult(request)
    }

    @Operation(summary = "운세 질문 시나리오 목록 조회")
    @GetMapping("/scenarios")
    fun getScenarios(): List<ConsultingScenarioOptionResponse> =
        ConsultingScenario.entries.map {
            ConsultingScenarioOptionResponse(
                code = it.name,
                title = it.title,
                description = it.description
            )
        }

    @Operation(summary = "운세 상담 이력 목록 조회")
    @GetMapping("/history")
    fun getHistoryList(
        authentication: Authentication,
        @RequestParam userId: Long
    ): List<ConsultingHistoryListItemResponse> {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHybridHistoryList(userId)
    }
}

data class ConsultRequest(
    @field:NotNull
    val userId: Long,
    @field:NotNull
    val mode: AnalysisMode,
    val scenario: ConsultingScenario? = null,
    @field:NotBlank
    @field:JsonAlias("stockName")
    val focusLabel: String,
    @field:JsonAlias("stockCode")
    val focusCode: String? = null,
    val tarotIndices: List<Int>? = null,
    val tarotDeckVersionId: String? = null,
    val assistantDeckSelections: List<AssistantDeckSelectionRequest>? = null,
    val tarotInterpretationMode: TarotInterpretationMode? = null,
    @field:NotBlank
    val question: String,
    val referenceDateTime: LocalDateTime? = null,
    @field:JsonAlias("scheduledSectorContext")
    val scheduledInterestContext: ScheduledInterestContext? = null
)

data class AssistantDeckSelectionRequest(
    @field:NotBlank
    val deckVersionId: String,
    val selectedIndices: List<Int>? = null
)

data class ScheduledInterestContext(
    @field:JsonAlias("sectors")
    val interestAreas: List<String>,
    @field:JsonAlias("marketContext")
    val flowContext: SectorMarketContext
)

fun AssistantDeckSelectionRequest.toTarotAssistantDeckSelection(): TarotAssistantDeckSelection =
    TarotAssistantDeckSelection(
        deckVersionId = deckVersionId,
        selectedIndices = selectedIndices
    )

fun ScheduledInterestContext.toSyntheticStockInfo(focusLabel: String): StockInfo =
    StockInfo(
        ticker = focusLabel,
        currentPrice = BigDecimal.ZERO,
        changeRate = BigDecimal.ZERO,
        sector = interestAreas.joinToString(" + "),
        source = MarketDataProvider.LOCAL,
        fallback = true
    )

fun String.toSyntheticStockInfo(focusCode: String? = null): StockInfo =
    StockInfo(
        ticker = focusCode?.takeIf { it.isNotBlank() } ?: this,
        currentPrice = BigDecimal.ZERO,
        changeRate = BigDecimal.ZERO,
        sector = "UNKNOWN",
        source = MarketDataProvider.LOCAL,
        fallback = true
    )

data class ConsultResponse(
    val mode: AnalysisMode,
    val focus: FocusConsultResponse,
    val saju: SajuConsultingResult?,
    val tarot: TarotConsultResponse?,
    val ai: HybridConsultingAiResponse,
    val history: SharedConsultingHistoryResponse,
    val marketEvidence: MarketEvidenceResponse
)

data class MarketEvidenceResponse(
    val routing: RoutingEvidenceResponse,
    val marketAsOf: LocalDateTime? = null,
    val positionAsOf: LocalDateTime? = null,
    val newsAsOf: LocalDateTime? = null,
    val priceFresh: Boolean,
    val positionFresh: Boolean,
    val newsFresh: Boolean,
    val marketDataUsed: Boolean,
    val marketMoodDataUsed: Boolean,
    val symbolQuoteUsed: Boolean,
    val positionDataUsed: Boolean,
    val webSearchUsed: Boolean,
    val grounded: Boolean,
    val citations: List<MarketEvidenceCitationResponse>,
    val staleReasons: List<String> = emptyList()
) {
    fun withSearchEvidence(evidence: com.hwcompany.fortune_index.ai.HybridConsultingEvidence): MarketEvidenceResponse =
        copy(
            newsAsOf = LocalDateTime.now(ZoneId.of("Asia/Seoul")).takeIf { webSearchUsed },
            newsFresh = !webSearchUsed || evidence.grounded,
            grounded = evidence.grounded,
            citations = evidence.citations.map { MarketEvidenceCitationResponse(title = it.title, url = it.url) },
            staleReasons = buildList {
                addAll(staleReasons)
                if (webSearchUsed && !evidence.grounded) add("최신 소식을 충분히 확인하지 못했어요")
            }.distinct()
        )
}

data class RoutingEvidenceResponse(
    val requiresMarketData: Boolean,
    val requiresFortuneFlowData: Boolean,
    val requiresSymbolQuote: Boolean,
    val requiresPositionData: Boolean,
    val requiresWebSearch: Boolean,
    val questionType: String,
    val reason: String
) {
    companion object {
        fun from(decision: ConsultingRoutingDecision): RoutingEvidenceResponse =
            RoutingEvidenceResponse(
                requiresMarketData = decision.requiresSymbolQuote,
                requiresFortuneFlowData = decision.requiresMarketMoodData,
                requiresSymbolQuote = decision.requiresSymbolQuote,
                requiresPositionData = decision.requiresPositionData,
                requiresWebSearch = decision.requiresWebSearch,
                questionType = decision.questionType,
                reason = decision.reason
            )
    }
}

data class MarketEvidenceCitationResponse(
    val title: String,
    val url: String
)

data class FocusConsultResponse(
    val label: String,
    val currentValue: BigDecimal,
    val changeRate: BigDecimal,
    val interestArea: String,
    val fallback: Boolean
) {
    companion object {
        fun from(stock: StockInfo, focusLabel: String): FocusConsultResponse =
            FocusConsultResponse(
                label = focusLabel,
                currentValue = stock.currentPrice,
                changeRate = stock.changeRate,
                interestArea = stock.sector,
                fallback = stock.fallback
            )
    }
}

data class TarotConsultResponse(
    val interpretationMode: TarotInterpretationMode,
    val cards: List<TarotCardConsultResponse>,
    val assistantDecks: List<TarotDeckConsultResponse> = emptyList()
) {
    companion object {
        fun from(reading: TarotReadingResult): TarotConsultResponse =
            TarotConsultResponse(
                interpretationMode = reading.interpretationMode,
                cards = reading.cards.map { TarotCardConsultResponse.from(it) },
                assistantDecks = reading.assistantDecks.map { TarotDeckConsultResponse.from(it) }
            )
    }
}

data class TarotDeckConsultResponse(
    val deckVersionId: String,
    val deckType: TarotDeckType,
    val deckRole: TarotDeckRole,
    val cardSetId: String,
    val cards: List<TarotCardConsultResponse>
) {
    companion object {
        fun from(drawGroup: TarotDrawGroupResult): TarotDeckConsultResponse =
            TarotDeckConsultResponse(
                deckVersionId = drawGroup.deckVersionId,
                deckType = drawGroup.deckType,
                deckRole = drawGroup.deckRole,
                cardSetId = drawGroup.cardSetId,
                cards = drawGroup.cards.map { TarotCardConsultResponse.from(it) }
            )
    }
}

data class TarotCardConsultResponse(
    val selectedIndex: Int,
    val code: String,
    val deckType: TarotDeckType,
    val deckRole: TarotDeckRole,
    val deckVersionId: String,
    val cardSetId: String,
    val name: String,
    val sortOrder: Int,
    val arcanaType: String?,
    val suit: String?,
    val meaning: String,
    val imageUrl: String?,
    val videoUrl: String?
) {
    companion object {
        fun from(draw: TarotDrawResult): TarotCardConsultResponse =
            TarotCardConsultResponse(
                selectedIndex = draw.index,
                code = draw.card.code,
                deckType = draw.card.deckType,
                deckRole = draw.card.deckRole,
                deckVersionId = draw.card.deckVersionId,
                cardSetId = draw.card.cardSetId,
                name = draw.card.name,
                sortOrder = draw.card.sortOrder,
                arcanaType = draw.card.arcanaType?.name,
                suit = draw.card.suit?.name,
                meaning = draw.card.meaning,
                imageUrl = draw.card.imageUrl,
                videoUrl = draw.card.videoUrl
            )
    }
}

data class ConsultingHistoryListItemResponse(
    val id: Long,
    val shareKey: String,
    val mode: AnalysisMode,
    val scenario: ConsultingScenario?,
    val focusLabel: String,
    val consultedAt: LocalDateTime,
    val aiSummary: String,
    val tarotInterpretationMode: String?,
    val tarotCardCodes: List<String>,
    val tarotCardNames: List<String>
)
