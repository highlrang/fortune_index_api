package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import com.hwcompany.fortune_index.auth.requireSameUserId
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
import com.hwcompany.fortune_index.history.SharedConsultingHistoryResponse
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.StockService
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotDeckType
import com.hwcompany.fortune_index.tarot.TarotReadingResult
import com.hwcompany.fortune_index.tarot.TarotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.core.Authentication

@Service
class ConsultingService(
    private val userRepository: UserRepository,
    private val stockService: StockService,
    private val tarotService: TarotService,
    private val sajuAnalyzer: SajuAnalyzer,
    private val promptStrategies: List<com.hwcompany.fortune_index.consulting.prompt.PromptProvider>,
    private val hybridConsultingAiClient: HybridConsultingAiClient,
    private val consultingHistoryService: ConsultingHistoryService,
    private val objectMapper: ObjectMapper,
    private val llmPromptTemplateService: LlmPromptTemplateService
) {
    private val promptStrategyByMode = AnalysisMode.entries.associateWith { mode ->
        promptStrategies.firstOrNull { it.supports(mode) }
            ?: error("PromptProvider is missing for mode=$mode")
    }

    /**
     * 상담 요청 하나를 끝까지 처리한다.
     * 사용자 조회, 데이터 수집, 프롬프트 전략 선택, AI 호출, 이력 저장을 한 메서드에서 묶는다.
     */
    @Transactional
    fun consult(request: ConsultRequest): ConsultResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${request.userId}") }
        validateTarotRequest(request)

        val stock = stockService.getStockInfo(request.stockCode)
        val tarotReading = request.mode.includesTarot().takeIf { it }?.let {
            tarotService.drawReading(
                indices = request.tarotIndices,
                interpretationMode = request.tarotInterpretationMode ?: TarotInterpretationMode.MAIN_TRADITIONAL
            )
        }
        val birthDateTime = LocalDateTime.of(
            user.birthInfo.birthDate,
            user.birthInfo.birthTime ?: DEFAULT_BIRTH_TIME
        )
        val saju = request.mode.includesSaju().takeIf { it }?.let {
            sajuAnalyzer.analyzeForConsulting(
                birthDateTime = birthDateTime,
                referenceDateTime = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID),
                zoneId = DEFAULT_ZONE_ID
            )
        }

        val payload = buildPayload(
            request = request,
            stock = stock,
            saju = saju,
            tarotReading = tarotReading,
            userName = user.name,
            riskProfile = user.investmentRiskProfile
        )
        val prompt = buildScenarioAwareSystemMessage(request)
        val aiResponse = hybridConsultingAiClient.requestJsonAdvice(
            systemMessage = prompt,
            payload = payload
        )

        val savedHistory = consultingHistoryService.saveHybridHistory(
            SaveHybridConsultingHistoryCommand(
                userId = requireNotNull(user.id),
                mode = request.mode,
                stockCode = stock.ticker,
                stockName = request.stockName ?: stock.ticker,
                stockInfo = stock,
                scenario = request.scenario,
                sajuResult = saju,
                tarotReading = tarotReading,
                analysisResultJson = objectMapper.writeValueAsString(payload),
                aiResponse = aiResponse,
                consultedAt = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID)
            )
        )

        return ConsultResponse(
            mode = request.mode,
            stock = StockConsultResponse.from(stock, request.stockName),
            saju = saju,
            tarot = tarotReading?.let { TarotConsultResponse.from(it) },
            ai = aiResponse,
            history = savedHistory
        )
    }

    private fun buildPayload(
        request: ConsultRequest,
        stock: StockInfo,
        saju: SajuConsultingResult?,
        tarotReading: TarotReadingResult?,
        userName: String,
        riskProfile: InvestmentRiskProfile
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf(
                "mode" to request.mode.name,
                "user" to mapOf(
                    "id" to request.userId,
                    "name" to userName,
                    "investmentRiskProfile" to riskProfile.name,
                    "investmentRiskProfileLabel" to when (riskProfile) {
                        InvestmentRiskProfile.STABLE -> "안정형"
                        InvestmentRiskProfile.AGGRESSIVE -> "공격형"
                    }
                ),
                "scenario" to mapOf(
                    "code" to request.scenario.name,
                    "title" to request.scenario.title,
                    "description" to request.scenario.description,
                    "focusQuestion" to request.scenario.focusQuestion()
                ),
                "question" to (request.question ?: defaultQuestion(request.mode)),
                "marketContext" to stock.toSectorMarketContext(),
                "internalStockData" to mapOf(
                    "code" to stock.ticker,
                    "name" to (request.stockName ?: stock.ticker),
                    "currentPrice" to stock.currentPrice,
                    "changeRate" to stock.changeRate,
                    "sector" to stock.sector,
                    "fallback" to stock.fallback
                ),
                "saju" to saju,
                "tarot" to tarotReading?.let {
                    mapOf(
                        "interpretationMode" to it.interpretationMode.name,
                        "cards" to it.cards.map { draw ->
                            mapOf(
                                "selectedIndex" to draw.index,
                                "code" to draw.card.code,
                                "deckType" to draw.card.deckType.name,
                                "name" to draw.card.displayName,
                                "sortOrder" to draw.card.sortOrder,
                                "arcanaType" to draw.card.arcanaType.name,
                                "suit" to draw.card.suit?.name,
                                "meaning" to draw.card.uprightMeaning,
                                "imageUrl" to draw.card.imageUrl,
                                "videoUrl" to draw.card.videoUrl
                            )
                        }
                    )
                }
            )
        )

    private fun buildScenarioAwareSystemMessage(request: ConsultRequest): String =
        buildString {
            append(promptStrategyByMode.getValue(request.mode).buildSystemMessage())
            append('\n')
            append("이번 상담 시나리오는 ${request.scenario.name}(${request.scenario.title})이다. ")
            append(request.scenario.systemInstructionAddon())
        }

    private fun validateTarotRequest(request: ConsultRequest) {
        if (!request.mode.includesTarot()) {
            if (!request.tarotIndices.isNullOrEmpty() || request.tarotInterpretationMode != null) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tarotIndices and tarotInterpretationMode are only allowed for tarot modes"
                )
            }
            return
        }

        if (request.tarotIndices.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotIndices is required for tarot modes")
        }
    }

    private fun defaultQuestion(mode: AnalysisMode): String =
        when (mode) {
            AnalysisMode.ONLY_STOCK -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_ONLY_STOCK)
            AnalysisMode.STOCK_SAJU -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_STOCK_SAJU)
            AnalysisMode.STOCK_TAROT -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_STOCK_TAROT)
            AnalysisMode.STOCK_ALL -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_STOCK_ALL)
        }

    private companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val DEFAULT_BIRTH_TIME = java.time.LocalTime.NOON
    }
}

@RestController
@RequestMapping("/api")
@Tag(name = "종합 상담 API", description = "주식, 사주, 타로 기반 종합 상담 기능")
class ConsultingController(
    private val consultingService: ConsultingService,
    private val consultingHistoryService: ConsultingHistoryService
) {
    @Operation(summary = "종합 투자 상담 요청")
    @PostMapping("/consult")
    fun consult(
        authentication: Authentication,
        @Valid @RequestBody request: ConsultRequest
    ): ConsultResponse {
        authentication.requireSameUserId(request.userId)
        return consultingService.consult(request)
    }

    @Operation(summary = "질문 시나리오 목록 조회")
    @GetMapping("/scenarios")
    fun getScenarios(): List<ConsultingScenarioOptionResponse> =
        ConsultingScenario.entries.map {
            ConsultingScenarioOptionResponse(
                code = it.name,
                title = it.title,
                description = it.description
            )
        }

    @Operation(summary = "상담 이력 목록 조회")
    @GetMapping("/history")
    fun getHistoryList(
        authentication: Authentication,
        @RequestParam userId: Long
    ): List<ConsultingHistoryListItemResponse> {
        authentication.requireSameUserId(userId)
        return consultingHistoryService.getHybridHistoryList(userId)
    }

    @Operation(summary = "상담 이력 상세 조회")
    @GetMapping("/history/{historyId}")
    fun getHistoryDetail(
        authentication: Authentication,
        @PathVariable historyId: Long,
        @RequestParam(required = false) userId: Long?
    ): SharedConsultingHistoryResponse {
        val requestedUserId = userId ?: authentication.requireAuthenticatedUser().userId
        authentication.requireSameUserId(requestedUserId)
        return consultingHistoryService.getHybridHistoryDetail(historyId, requestedUserId)
    }

    @Operation(summary = "공유 상담 이력 조회")
    @GetMapping("/history/share/{shareKey}")
    fun getSharedHistory(@PathVariable shareKey: String): SharedConsultingHistoryResponse =
        consultingHistoryService.getSharedHistory(shareKey)
}

data class ConsultRequest(
    @field:NotNull
    val userId: Long,
    @field:NotNull
    val mode: AnalysisMode,
    @field:NotNull
    val scenario: ConsultingScenario,
    @field:NotBlank
    val stockCode: String,
    val stockName: String? = null,
    val tarotIndices: List<Int>? = null,
    val tarotInterpretationMode: TarotInterpretationMode? = null,
    val question: String? = null,
    val referenceDateTime: LocalDateTime? = null
)

data class ConsultResponse(
    val mode: AnalysisMode,
    val stock: StockConsultResponse,
    val saju: SajuConsultingResult?,
    val tarot: TarotConsultResponse?,
    val ai: HybridConsultingAiResponse,
    val history: SharedConsultingHistoryResponse
)

data class StockConsultResponse(
    val code: String,
    val name: String,
    val currentPrice: java.math.BigDecimal,
    val changeRate: java.math.BigDecimal,
    val sector: String,
    val fallback: Boolean
) {
    companion object {
        fun from(stock: StockInfo, stockName: String?): StockConsultResponse =
            StockConsultResponse(
                code = stock.ticker,
                name = stockName ?: stock.ticker,
                currentPrice = stock.currentPrice,
                changeRate = stock.changeRate,
                sector = stock.sector,
                fallback = stock.fallback
            )
    }
}

data class TarotConsultResponse(
    val interpretationMode: TarotInterpretationMode,
    val cards: List<TarotCardConsultResponse>
) {
    companion object {
        fun from(reading: TarotReadingResult): TarotConsultResponse =
            TarotConsultResponse(
                interpretationMode = reading.interpretationMode,
                cards = reading.cards.map { TarotCardConsultResponse.from(it) }
            )
    }
}

data class TarotCardConsultResponse(
    val selectedIndex: Int,
    val code: String,
    val deckType: TarotDeckType,
    val name: String,
    val sortOrder: Int,
    val arcanaType: String,
    val suit: String?,
    val meaning: String,
    val imageUrl: String,
    val videoUrl: String?
) {
    companion object {
        fun from(draw: com.hwcompany.fortune_index.tarot.TarotDrawResult): TarotCardConsultResponse =
            TarotCardConsultResponse(
                selectedIndex = draw.index,
                code = draw.card.code,
                deckType = draw.card.deckType,
                name = draw.card.displayName,
                sortOrder = draw.card.sortOrder,
                arcanaType = draw.card.arcanaType.name,
                suit = draw.card.suit?.name,
                meaning = draw.card.uprightMeaning,
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
    val stockCode: String,
    val stockName: String,
    val consultedAt: LocalDateTime,
    val aiSummary: String,
    val tarotInterpretationMode: String?,
    val tarotCardCodes: List<String>,
    val tarotCardNames: List<String>
)
