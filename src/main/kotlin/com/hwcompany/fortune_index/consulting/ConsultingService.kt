package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.auth.requireAuthenticatedUser
import com.hwcompany.fortune_index.auth.requireSameUserId
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
import com.hwcompany.fortune_index.history.SharedConsultingHistoryResponse
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.market.StockService
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuCharacter
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.saju.SajuCoreEnergy
import com.hwcompany.fortune_index.saju.TenGodMapping
import com.hwcompany.fortune_index.saju.TenStar
import com.hwcompany.fortune_index.saju.TenGod
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotDeckType
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotReadingResult
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
    private val tarotDeckService: TarotDeckService,
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
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
            tarotDeckService.drawReading(
                deckVersionId = requireNotNull(request.tarotDeckVersionId),
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
        val sajuReference = sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(user.id))
            ?.let { result ->
                linkedMapOf(
                    "analyzedAt" to result.analyzedAt,
                    "heavenlyStems" to result.heavenlyStems.map { stem ->
                        mapOf(
                            "pillarOrder" to stem.pillarOrder,
                            "pillarLabel" to pillarLabel(stem.pillarOrder, true),
                            "code" to stem.code,
                            "labelKo" to stem.labelKo,
                            "sortOrder" to stem.sortOrder
                        )
                    },
                    "earthlyBranches" to result.earthlyBranches.map { branch ->
                        mapOf(
                            "pillarOrder" to branch.pillarOrder,
                            "pillarLabel" to pillarLabel(branch.pillarOrder, false),
                            "code" to branch.code,
                            "labelKo" to branch.labelKo,
                            "sortOrder" to branch.sortOrder
                        )
                    },
                    "fiveElements" to result.fiveElements,
                    "description" to "저장된 사주 원국 정보이며 code는 내부 코드, labelKo는 한글 명칭, sortOrder는 천간/지지 순번이다."
                )
            }

        val resolvedQuestion = request.question ?: defaultQuestion(request.mode)
        val payload = buildPayload(
            request = request,
            question = resolvedQuestion,
            stock = stock,
            saju = saju,
            sajuReference = sajuReference,
            tarotReading = tarotReading,
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
                question = resolvedQuestion,
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
        question: String,
        stock: StockInfo,
        saju: SajuConsultingResult?,
        sajuReference: Map<String, Any?>?,
        tarotReading: TarotReadingResult?,
        riskProfile: InvestmentRiskProfile
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf(
                "mode" to request.mode.name,
                "user" to mapOf(
                    "id" to request.userId,
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
                "question" to question,
                "marketContext" to stock.toSectorMarketContext(),
                "internalStockData" to mapOf(
                    "code" to stock.ticker,
                    "name" to (request.stockName ?: stock.ticker),
                    "currentPrice" to stock.currentPrice,
                    "changeRate" to stock.changeRate,
                    "sector" to stock.sector,
                    "fallback" to stock.fallback
                ),
                "saju" to saju?.toAiPayload(),
                "sajuReference" to sajuReference,
                "tarot" to tarotReading?.let {
                    mapOf(
                        "deckVersionId" to request.tarotDeckVersionId,
                        "interpretationMode" to it.interpretationMode.name,
                        "cards" to it.cards.map { draw ->
                            mapOf(
                                "selectedIndex" to draw.index,
                                "code" to draw.card.code,
                                "deckVersionId" to draw.card.deckVersionId,
                                "deckType" to draw.card.deckType.name,
                                "name" to draw.card.name,
                                "koreanName" to draw.card.koreanName,
                                "sortOrder" to draw.card.sortOrder,
                                "arcanaType" to draw.card.arcanaType.name,
                                "suit" to draw.card.suit?.name,
                                "meaning" to draw.card.meaning,
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
            append('\n')
            append("사용자의 핵심 질문은 다음과 같다: ")
            append(request.question ?: defaultQuestion(request.mode))
            append('\n')
            append("모든 섹션은 반드시 consulting_scenario와 question에 직접 답해야 한다. ")
            append("일반론이나 개념 설명으로 길게 빠지지 말고, 이번 질문의 의사결정에 필요한 해석만 남겨라.")
            append('\n')
            append("analysis_results.market_analysis.content는 현재 시장/섹터 흐름이 이 질문에 주는 시사점을 설명하고, 마지막 문장에서 행동 판단을 분명히 정리해라.")
            append('\n')
            append("analysis_results.saju_analysis.content는 ")
            if (request.mode.includesSaju()) {
                append("사주 원국, 십성, 현재 운 흐름을 이번 질문의 투자 판단과 직접 연결해 해석해라. 올해 재운 일반론만 반복하지 말고, 사용자의 진입 성향, 버티는 힘, 흔들리기 쉬운 지점을 질문 기준으로 설명해라.")
            } else {
                append("이번 상담에서는 사주 분석을 사용하지 않았습니다. 라고 정확히 써라.")
            }
            append('\n')
            append("analysis_results.tarot_analysis.content는 ")
            if (request.mode.includesTarot()) {
                append("각 카드의 상징을 이번 질문의 투자 심리, 타이밍, 리스크와 연결해 해석해라. 카드 뜻풀이 자체가 목적이 아니며, 주식 판단과 긴밀히 연결된 신호만 설명해라.")
            } else {
                append("이번 상담에서는 타로 분석을 사용하지 않았습니다. 라고 정확히 써라.")
            }
            append('\n')
            append("overall_summary는 시장 분석")
            if (request.mode.includesSaju()) append(", 사주 분석")
            if (request.mode.includesTarot()) append(", 타로 분석")
            append("을 종합해 이번 질문에 대한 최종 행동 결론을 먼저 말하고, 그 결론의 근거를 짧게 덧붙여라.")
        }

    private fun validateTarotRequest(request: ConsultRequest) {
        if (!request.mode.includesTarot()) {
            if (!request.tarotIndices.isNullOrEmpty() || request.tarotInterpretationMode != null || request.tarotDeckVersionId != null) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tarotIndices, tarotDeckVersionId and tarotInterpretationMode are only allowed for tarot modes"
                )
            }
            return
        }

        if (request.tarotIndices.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotIndices is required for tarot modes")
        }
        if (request.tarotDeckVersionId.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotDeckVersionId is required for tarot modes")
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
    val tarotDeckVersionId: String? = null,
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
    // Stable card index within a deck version. This is not the UI slot index.
    val selectedIndex: Int,
    val code: String,
    val deckType: TarotDeckType,
    val deckVersionId: String,
    val name: String,
    val sortOrder: Int,
    val arcanaType: String,
    val suit: String?,
    val meaning: String,
    val imageUrl: String?,
    val videoUrl: String?
) {
    companion object {
        fun from(draw: com.hwcompany.fortune_index.tarot.TarotDrawResult): TarotCardConsultResponse =
            TarotCardConsultResponse(
                selectedIndex = draw.index,
                code = draw.card.code,
                deckType = draw.card.deckType,
                deckVersionId = draw.card.deckVersionId,
                name = draw.card.name,
                sortOrder = draw.card.sortOrder,
                arcanaType = draw.card.arcanaType.name,
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
    val stockCode: String,
    val stockName: String,
    val consultedAt: LocalDateTime,
    val aiSummary: String,
    val tarotInterpretationMode: String?,
    val tarotCardCodes: List<String>,
    val tarotCardNames: List<String>
)

private fun SajuConsultingResult.toAiPayload(): Map<String, Any?> =
    linkedMapOf(
        "summary" to mapOf(
            "dayMaster" to dayMaster.toAiPayload("일간"),
            "dayBranch" to dayBranch.toAiPayload("일지"),
            "monthBranch" to monthBranch.toAiPayload("월지")
        ),
        "natalChart" to mapOf(
            "year" to analysis.natalChart.year.toAiPayload("연주"),
            "month" to analysis.natalChart.month.toAiPayload("월주"),
            "day" to analysis.natalChart.day.toAiPayload("일주"),
            "hour" to analysis.natalChart.hour.toAiPayload("시주")
        ),
        "characters" to analysis.characters.map { it.toAiPayload() },
        "tenGods" to analysis.tenGods.map { it.toAiPayload() },
        "fiveElementBalance" to mapOf(
            "wood" to analysis.fiveElementBalance.wood,
            "fire" to analysis.fiveElementBalance.fire,
            "earth" to analysis.fiveElementBalance.earth,
            "metal" to analysis.fiveElementBalance.metal,
            "water" to analysis.fiveElementBalance.water,
            "description" to "각 오행이 사주 원국에 몇 개 분포하는지 나타내는 개수다."
        ),
        "yinYangBalance" to mapOf(
            "yinCount" to analysis.yinYangBalance.yinCount,
            "yangCount" to analysis.yinYangBalance.yangCount,
            "description" to "음과 양의 분포 개수다."
        ),
        "currentFortune" to mapOf(
            "referenceYear" to currentFortune.referenceYear,
            "majorFortune" to mapOf(
                "sequence" to currentFortune.majorFortune.sequence,
                "startAge" to currentFortune.majorFortune.startAge,
                "endAge" to currentFortune.majorFortune.endAge,
                "pillar" to currentFortune.majorFortune.pillar.toAiPayload("대운"),
                "stemTenStar" to currentFortune.majorFortune.stemTenStar.toAiPayload(),
                "branchTenStar" to currentFortune.majorFortune.branchTenStar.toAiPayload(),
                "description" to "현재 속한 대운 구간 정보다."
            ),
            "yearlyFortune" to mapOf(
                "year" to currentFortune.yearlyFortune.year,
                "pillar" to currentFortune.yearlyFortune.pillar.toAiPayload("세운"),
                "stemTenStar" to currentFortune.yearlyFortune.stemTenStar.toAiPayload(),
                "branchTenStar" to currentFortune.yearlyFortune.branchTenStar.toAiPayload()
            )
        )
    )

private fun com.hwcompany.fortune_index.saju.Pillar.toAiPayload(label: String): Map<String, Any> =
    mapOf(
        "label" to label,
        "stem" to heavenlyStem.toAiPayload(),
        "branch" to earthlyBranch.toAiPayload(),
        "combinedLabelKo" to "${heavenlyStem.labelKo()}${earthlyBranch.labelKo()}"
    )

private fun HeavenlyStem.toAiPayload(): Map<String, Any> =
    mapOf(
        "code" to name,
        "labelKo" to labelKo()
    )

private fun EarthlyBranch.toAiPayload(): Map<String, Any> =
    mapOf(
        "code" to name,
        "labelKo" to labelKo()
    )

private fun SajuCoreEnergy.toAiPayload(label: String): Map<String, Any?> =
    mapOf(
        "label" to label,
        "code" to symbol,
        "labelKo" to symbol.toKoreanSymbol(),
        "fiveElement" to fiveElement.toAiPayload(),
        "yinYang" to yinYang.toAiPayload()
    )

private fun SajuCharacter.toAiPayload(): Map<String, Any?> =
    mapOf(
        "position" to position.name,
        "positionLabelKo" to position.toPositionLabelKo(),
        "type" to type.name,
        "symbolCode" to symbol,
        "symbolLabelKo" to symbol.toKoreanSymbol(),
        "fiveElement" to fiveElement.toAiPayload(),
        "yinYang" to yinYang.toAiPayload(),
        "referenceStemCode" to referenceStem?.name,
        "referenceStemLabelKo" to referenceStem?.labelKo()
    )

private fun TenGodMapping.toAiPayload(): Map<String, Any?> =
    mapOf(
        "position" to position.name,
        "positionLabelKo" to position.toPositionLabelKo(),
        "characterCode" to character,
        "characterLabelKo" to character.toKoreanSymbol(),
        "baseReferenceCode" to baseReference,
        "baseReferenceLabelKo" to baseReference?.toKoreanSymbol(),
        "tenGod" to tenGod.toAiPayload()
    )

private fun com.hwcompany.fortune_index.saju.FiveElement.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "목"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "화"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "토"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "금"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "수"
        }
    )

private fun com.hwcompany.fortune_index.saju.YinYang.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            com.hwcompany.fortune_index.saju.YinYang.YIN -> "음"
            com.hwcompany.fortune_index.saju.YinYang.YANG -> "양"
        }
    )

private fun TenGod.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            TenGod.BIGYEON -> "비견"
            TenGod.GEOPJAE -> "겁재"
            TenGod.SIKSIN -> "식신"
            TenGod.SANGGWAN -> "상관"
            TenGod.PYEONJAE -> "편재"
            TenGod.JEONGJAE -> "정재"
            TenGod.PYEONGWAN -> "편관"
            TenGod.JEONGGWAN -> "정관"
            TenGod.PYEONIN -> "편인"
            TenGod.JEONGIN -> "정인"
        }
    )

private fun TenStar.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            TenStar.BIGYEON -> "비견"
            TenStar.GEOPJAE -> "겁재"
            TenStar.SIKSIN -> "식신"
            TenStar.SANGGWAN -> "상관"
            TenStar.PYEONJAE -> "편재"
            TenStar.JEONGJAE -> "정재"
            TenStar.PYEONGWAN -> "편관"
            TenStar.JEONGGWAN -> "정관"
            TenStar.PYEONIN -> "편인"
            TenStar.JEONGIN -> "정인"
        }
    )

private fun com.hwcompany.fortune_index.saju.SajuPosition.toPositionLabelKo(): String =
    when (this) {
        com.hwcompany.fortune_index.saju.SajuPosition.YEAR_STEM -> "연간"
        com.hwcompany.fortune_index.saju.SajuPosition.YEAR_BRANCH -> "연지"
        com.hwcompany.fortune_index.saju.SajuPosition.MONTH_STEM -> "월간"
        com.hwcompany.fortune_index.saju.SajuPosition.MONTH_BRANCH -> "월지"
        com.hwcompany.fortune_index.saju.SajuPosition.DAY_STEM -> "일간"
        com.hwcompany.fortune_index.saju.SajuPosition.DAY_BRANCH -> "일지"
        com.hwcompany.fortune_index.saju.SajuPosition.HOUR_STEM -> "시간"
        com.hwcompany.fortune_index.saju.SajuPosition.HOUR_BRANCH -> "시지"
        com.hwcompany.fortune_index.saju.SajuPosition.FORTUNE_STEM -> "운간"
        com.hwcompany.fortune_index.saju.SajuPosition.FORTUNE_BRANCH -> "운지"
    }

private fun String.toKoreanSymbol(): String =
    HeavenlyStem.entries.firstOrNull { it.name == this }?.labelKo()
        ?: EarthlyBranch.entries.firstOrNull { it.name == this }?.labelKo()
        ?: this

private fun pillarLabel(pillarOrder: Int, isStem: Boolean): String =
    when (pillarOrder) {
        1 -> if (isStem) "연간" else "연지"
        2 -> if (isStem) "월간" else "월지"
        3 -> if (isStem) "일간" else "일지"
        4 -> if (isStem) "시간" else "시지"
        else -> if (isStem) "천간" else "지지"
    }
