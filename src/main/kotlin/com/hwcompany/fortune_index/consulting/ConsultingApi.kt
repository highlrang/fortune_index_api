package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.auth.AuthenticatedUser
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SharedConsultingHistoryResponse
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
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
@Tag(name = "투자 심리 운세 상담 API", description = "사주, 타로, 관심 분야 흐름을 바탕으로 오늘의 주식 투자 심리와 판단 기준을 읽어주는 기능")
class ConsultingController(
    private val consultingService: ConsultingService,
    private val consultingHistoryService: ConsultingHistoryService
) {
    @Operation(summary = "투자 심리 운세 상담 요청")
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
        SCENARIO_DISPLAY_ORDER.map { it }.distinctBy { it.title }.map {
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
        @RequestParam userId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): List<ConsultingHistoryListItemResponse> {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHybridHistoryList(userId, pageable, startDate, endDate)
    }
}

private val SCENARIO_DISPLAY_ORDER = listOf(
    ConsultingScenario.FLOW_CHECK,
    ConsultingScenario.ENTRY_READY,
    ConsultingScenario.HOLD_OR_EXIT,
    ConsultingScenario.MENTAL_CARE
)

data class ConsultRequest(
    @field:NotNull
    val userId: Long,
    @field:NotNull
    val mode: AnalysisMode,
    val scenario: ConsultingScenario? = null,
    val focusLabel: String? = null,
    val tarotIndices: List<Int>? = null,
    val tarotDeckVersionId: String? = null,
    val assistantDeckSelections: List<AssistantDeckSelectionRequest>? = null,
    val tarotInterpretationMode: TarotInterpretationMode? = null,
    val question: String? = null,
    val referenceDateTime: LocalDateTime? = null
)

data class AssistantDeckSelectionRequest(
    @field:NotBlank
    val deckVersionId: String,
    val selectedIndices: List<Int>? = null
)

fun AssistantDeckSelectionRequest.toTarotAssistantDeckSelection(): TarotAssistantDeckSelection =
    TarotAssistantDeckSelection(
        deckVersionId = deckVersionId,
        selectedIndices = selectedIndices
    )

data class ConsultResponse(
    val mode: AnalysisMode,
    val focus: FocusConsultResponse,
    val saju: SajuConsultingResult?,
    val zodiac: ZodiacConsultResponse?,
    val tarot: TarotConsultResponse?,
    val ai: HybridConsultingAiResponse,
    val history: SharedConsultingHistoryResponse
)

data class ZodiacConsultResponse(
    val sign: String,
    val signKo: String,
    val element: String,
    val headline: String
) {
    companion object {
        fun from(profile: ZodiacConsultingProfile): ZodiacConsultResponse =
            ZodiacConsultResponse(
                sign = profile.sign.name,
                signKo = profile.sign.koreanName,
                element = profile.sign.element,
                headline = profile.headline
            )
    }
}

data class FocusConsultResponse(
    val label: String,
    val currentValue: BigDecimal?,
    val changeRate: BigDecimal?,
    val interestArea: String?,
    val fallback: Boolean
) {
    companion object {
        fun fromLabel(focusLabel: String): FocusConsultResponse =
            FocusConsultResponse(
                label = focusLabel,
                currentValue = null,
                changeRate = null,
                interestArea = null,
                fallback = true
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
    val mode: AnalysisMode,
    val scenario: ConsultingScenario?,
    val question: String?,
    val focusLabel: String,
    val consultedAt: LocalDateTime,
    val stabilityScore: Int,
    val overallSummary: String?,
    val analysis: ConsultingHistoryAnalysisResponse,
    val tarotInterpretationMode: String?,
    val tarotCardCodes: List<String>,
    val tarotCardNames: List<String>
)

data class ConsultingHistoryAnalysisResponse(
    val saju: String? = null,
    val tarot: String? = null,
    val zodiac: String? = null
)

private fun Authentication.requireSameUserId(targetUserId: Long): AuthenticatedUser {
    val authenticatedUser = principal as? AuthenticatedUser
        ?: error("인증 사용자 정보를 찾을 수 없습니다.")
    if (authenticatedUser.userId != targetUserId) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN, "다른 사용자의 리소스에 접근할 수 없습니다.")
    }
    return authenticatedUser
}
