package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.market.StockInfo
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotReadingResult
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ConsultingService(
    private val userRepository: UserRepository,
    private val tarotDeckService: TarotDeckService,
    private val sajuAnalyzer: SajuAnalyzer,
    private val sajuResultRepository: SajuResultRepository,
    private val consultingRequestRouter: ConsultingRequestRouter,
    private val consultingPositionSnapshotService: ConsultingPositionSnapshotService,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val promptStrategies: List<com.hwcompany.fortune_index.consulting.prompt.PromptProvider>,
    private val hybridConsultingAiClient: HybridConsultingAiClient,
    private val consultingRiskScoreCalculator: ConsultingRiskScoreCalculator,
    private val consultingHistoryService: ConsultingHistoryService,
    private val objectMapper: ObjectMapper,
    private val fortuneSafetyGuard: FortuneSafetyGuard
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
        val resolvedQuestion = request.question.trim()
        val routingDecision = consultingRequestRouter.route(request, resolvedQuestion)
        val resolvedTarotDeckVersionId = resolveMainTarotDeckVersionId(
            requestedDeckVersionId = request.tarotDeckVersionId,
            fallbackDeckVersionId = user.preferredTarotDeckId,
            subscriptionTier = user.subscriptionTier
        )

        val stock = resolveStock(request, routingDecision)
        val positionSnapshot = resolvePositionSnapshot(request, routingDecision)
        val tarotReading = request.mode.includesTarot().takeIf { it }?.let {
            tarotDeckService.drawReading(
                subscriptionTier = user.subscriptionTier,
                deckVersionId = resolvedTarotDeckVersionId,
                indices = request.tarotIndices,
                assistantDeckSelections = request.assistantDeckSelections.orEmpty().map { it.toTarotAssistantDeckSelection() },
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
                zoneId = DEFAULT_ZONE_ID,
                gender = user.gender
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
        val freshness = evaluateFreshness(
            request = request,
            routingDecision = routingDecision,
            stock = stock,
            positionSnapshot = positionSnapshot
        )
        validatePreGenerationFreshness(freshness)
        val marketContext = request.scheduledInterestContext?.flowContext ?: stock.toSectorMarketContext()
        val payload = buildPayload(
            request = request,
            question = resolvedQuestion,
            stock = stock,
            marketContext = marketContext,
            tarotDeckVersionId = resolvedTarotDeckVersionId,
            saju = saju,
            sajuReference = sajuReference,
            tarotReading = tarotReading,
            riskProfile = user.investmentRiskProfile,
            routingDecision = routingDecision,
            positionSnapshot = positionSnapshot,
            freshness = freshness
        )
        val prompt = buildScenarioAwareSystemMessage(
            request = request,
            question = resolvedQuestion,
            riskProfile = user.investmentRiskProfile,
            routingDecision = routingDecision,
            freshness = freshness
        )
        val aiResponse = hybridConsultingAiClient.requestJsonAdvice(
            systemMessage = prompt,
            payload = payload,
            enableGoogleSearch = routingDecision.requiresWebSearch
        )
        val safeAiResponse = fortuneSafetyGuard.enforce(
            request = request,
            response = aiResponse,
            marketContext = marketContext
        )
        val evidence = freshness.withSearchEvidence(safeAiResponse.evidence)
        validatePostGenerationFreshness(evidence)
        val calculatedRiskScore = consultingRiskScoreCalculator.calculate(
            mode = request.mode,
            scenario = request.scenario,
            stockInfo = stock,
            riskProfile = user.investmentRiskProfile
        )
        val normalizedAiResponse = consultingRiskScoreCalculator.overrideRiskScore(
            response = safeAiResponse,
            riskScore = calculatedRiskScore,
            rawJson = safeAiResponse.copy(riskScore = calculatedRiskScore).toCanonicalJson()
        )

        val savedHistory = consultingHistoryService.saveHybridHistory(
            SaveHybridConsultingHistoryCommand(
                userId = requireNotNull(user.id),
                mode = request.mode,
                stockName = request.focusLabel,
                question = resolvedQuestion,
                stockInfo = stock,
                scenario = request.scenario,
                sajuResult = saju,
                tarotReading = tarotReading,
                analysisResultJson = objectMapper.writeValueAsString(payload),
                aiResponse = normalizedAiResponse,
                consultedAt = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID)
            )
        )

        return ConsultResponse(
            mode = request.mode,
            focus = FocusConsultResponse.from(stock, request.focusLabel),
            saju = saju,
            tarot = tarotReading?.let { TarotConsultResponse.from(it) },
            ai = normalizedAiResponse,
            history = savedHistory,
            marketEvidence = evidence
        )
    }

    private fun resolveStock(request: ConsultRequest, routingDecision: ConsultingRoutingDecision): StockInfo {
        return request.scheduledInterestContext?.toSyntheticStockInfo(request.focusLabel)
            ?: request.focusLabel.toSyntheticStockInfo(request.focusCode)
    }

    private fun resolvePositionSnapshot(
        request: ConsultRequest,
        routingDecision: ConsultingRoutingDecision
    ): ConsultingPositionSnapshot? = null

    private fun evaluateFreshness(
        request: ConsultRequest,
        routingDecision: ConsultingRoutingDecision,
        stock: StockInfo,
        positionSnapshot: ConsultingPositionSnapshot?
    ): MarketEvidenceResponse {
        val consultedAt = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID)
        val marketAsOf = stock.marketDataAsOf.atStartOfDay()
        val positionAsOf = positionSnapshot?.capturedAt

        return MarketEvidenceResponse(
            routing = RoutingEvidenceResponse.from(routingDecision),
            marketAsOf = marketAsOf,
            positionAsOf = positionAsOf,
            newsAsOf = null,
            priceFresh = true,
            positionFresh = true,
            newsFresh = !routingDecision.requiresWebSearch,
            marketDataUsed = false,
            marketMoodDataUsed = false,
            symbolQuoteUsed = false,
            positionDataUsed = false,
            webSearchUsed = routingDecision.requiresWebSearch,
            grounded = false,
            citations = emptyList(),
            staleReasons = emptyList()
        )
    }

    private fun buildPayload(
        request: ConsultRequest,
        question: String,
        stock: StockInfo,
        marketContext: SectorMarketContext,
        tarotDeckVersionId: String,
        saju: SajuConsultingResult?,
        sajuReference: Map<String, Any?>?,
        tarotReading: TarotReadingResult?,
        riskProfile: InvestmentRiskProfile,
        routingDecision: ConsultingRoutingDecision,
        positionSnapshot: ConsultingPositionSnapshot?,
        freshness: MarketEvidenceResponse
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "routing" to routingDecision,
                "user" to mapOf(
                    "id" to request.userId,
                    "investmentRiskProfile" to riskProfile.name,
                    "investmentRiskProfileLabel" to when (riskProfile) {
                        InvestmentRiskProfile.STABLE -> "신중형"
                        InvestmentRiskProfile.AGGRESSIVE -> "직진형"
                    }
                ),
                "scenario" to request.scenario?.let {
                    mapOf(
                        "code" to it.name,
                        "title" to it.title,
                        "description" to it.description,
                        "focusQuestion" to it.focusQuestion()
                    )
                },
                "question" to question,
                "freshness" to freshness,
                "focusArea" to marketContext.interestArea.ifBlank { "선택한 흐름" },
                "marketContext" to marketContext,
                "marketPhenomenon" to marketContext.toMarketPhenomenonContext(),
                "positionSnapshot" to positionSnapshot?.toEmotionPayload(stock),
                "saju" to saju?.toAiPayload(),
                "sajuReference" to sajuReference,
                "tarot" to tarotReading?.let {
                    mapOf(
                        "deckVersionId" to tarotDeckVersionId,
                        "interpretationMode" to it.interpretationMode.name,
                        "cards" to it.cards.map { draw ->
                            mapOf(
                                "selectedIndex" to draw.index,
                                "code" to draw.card.code,
                                "deckVersionId" to draw.card.deckVersionId,
                                "deckType" to draw.card.deckType.name,
                                "deckRole" to draw.card.deckRole.name,
                                "cardSetId" to draw.card.cardSetId,
                                "name" to draw.card.name,
                                "koreanName" to draw.card.koreanName,
                                "sortOrder" to draw.card.sortOrder,
                                "arcanaType" to draw.card.arcanaType?.name,
                                "suit" to draw.card.suit?.name,
                                "meaning" to draw.card.meaning,
                                "imageUrl" to draw.card.imageUrl,
                                "videoUrl" to draw.card.videoUrl
                            )
                        },
                        "assistantDecks" to it.assistantDecks.map { deck ->
                            mapOf(
                                "deckVersionId" to deck.deckVersionId,
                                "deckType" to deck.deckType.name,
                                "deckRole" to deck.deckRole.name,
                                "cardSetId" to deck.cardSetId,
                                "cards" to deck.cards.map { draw ->
                                    mapOf(
                                        "selectedIndex" to draw.index,
                                        "code" to draw.card.code,
                                        "deckVersionId" to draw.card.deckVersionId,
                                        "deckType" to draw.card.deckType.name,
                                        "deckRole" to draw.card.deckRole.name,
                                        "cardSetId" to draw.card.cardSetId,
                                        "name" to draw.card.name,
                                        "koreanName" to draw.card.koreanName,
                                        "sortOrder" to draw.card.sortOrder,
                                        "arcanaType" to draw.card.arcanaType?.name,
                                        "suit" to draw.card.suit?.name,
                                        "meaning" to draw.card.meaning,
                                        "imageUrl" to draw.card.imageUrl,
                                        "videoUrl" to draw.card.videoUrl
                                    )
                                }
                            )
                        }
                    )
                }
            )
        )

    private fun validatePreGenerationFreshness(freshness: MarketEvidenceResponse) {
        if (freshness.positionDataUsed && !freshness.positionFresh) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "latest position data is required but unavailable"
            )
        }
    }

    private fun validatePostGenerationFreshness(evidence: MarketEvidenceResponse) {
        if (evidence.webSearchUsed && !evidence.newsFresh) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "latest grounded web search evidence is required but unavailable"
            )
        }
    }

    private fun buildScenarioAwareSystemMessage(
        request: ConsultRequest,
        question: String,
        riskProfile: InvestmentRiskProfile,
        routingDecision: ConsultingRoutingDecision,
        freshness: MarketEvidenceResponse
    ): String =
        buildString {
            append(promptStrategyByMode.getValue(request.mode).buildSystemMessage())
            append('\n')
            append(InvestmentPartnerPersonaPromptGuidance.build())
            append('\n')
            append(InvestmentProfilePromptGuidance.forRiskProfile(riskProfile))
            append('\n')
            append(MarketEvidencePromptGuidance.build())
            append('\n')
            request.scenario?.let { scenario ->
                append("이번 상담 시나리오는 ${scenario.name}(${scenario.title})이다. ")
                append(scenario.systemInstructionAddon())
                append('\n')
            } ?: run {
                append("이번 상담은 별도 시나리오 없이 사용자의 자유 질문을 중심으로 해석한다. ")
                append("질문에 직접 연결되는 재물 흐름과 감정의 파동만 짧고 분명하게 설명해라.")
                append('\n')
            }
            append("사용자의 핵심 질문은 다음과 같다: ")
            append(question)
            append('\n')
            append("모든 섹션은 반드시 consulting_scenario와 question에 직접 답해야 한다. ")
            append("일반론이나 개념 설명으로 길게 빠지지 말고, 이번 질문의 의사결정에 필요한 해석만 남겨라.")
            append('\n')
            append("routing.questionType은 ${routingDecision.questionType} 이다. ")
            append("requiresFortuneFlowData=${routingDecision.requiresMarketMoodData}, requiresSymbolQuote=${routingDecision.requiresSymbolQuote}, requiresPositionData=${routingDecision.requiresPositionData}, requiresWebSearch=${routingDecision.requiresWebSearch} 로 판단되었다. ")
            append('\n')
            append("freshness 기준: priceFresh=${freshness.priceFresh}, positionFresh=${freshness.positionFresh}, newsFresh=${freshness.newsFresh} 이다. ")
            append("fresh가 아닌 데이터는 최신 데이터처럼 단정하지 마라. ")
            append('\n')
            append("이 서비스는 돈의 흐름과 마음 상태를 읽어 주는 서비스다. ")
            append("어려운 투자 용어나 전문가 말투, 무엇을 사거나 팔라는 식의 표현, 결과를 보장하는 표현은 절대 사용하지 마라. ")
            append("시장 데이터 연동은 제거되었으므로 지금의 실제 숫자나 바깥 상황을 정확히 알고 있는 것처럼 말하지 마라.")
            append('\n')
            append("이번 답변은 관심 분야 흐름과 질문, 사주, 타로를 중심으로 해석한다. 구체적인 값이나 순간 변화를 아는 것처럼 말하지 마라.")
            append('\n')
            if (routingDecision.requiresWebSearch) {
                append("이번 답변은 최신 소식 반영이 필요하다. 충분히 확인되지 않았다면 이유를 단정하지 말고, 전반적인 분위기 수준으로만 설명해라.")
                append('\n')
            }
            append("문장은 친절하고 쉬워야 하며, 어려운 말보다 상징과 흐름의 언어를 우선해라. 각 analysis 섹션은 1~2문장, overall_summary는 1~2문장 이내로 제한해라.")
            append('\n')
            append("analysis_results.market_analysis.title은 반드시 \"외부 기류 해석\"으로 고정하고, content는 현재 섹터/질문 흐름이 사용자의 감정과 재물 기운에 어떤 공기감을 주는지 설명해라.")
            append('\n')
            append("analysis_results.saju_analysis는 ")
            if (request.mode.includesSaju()) {
                append("title이 \"재물 기질 해석\"인 객체로 반환하고, content는 사주 원국과 현재 운 흐름을 바탕으로 사용자의 재물 감각, 흔들리기 쉬운 지점, 마음의 리듬을 질문 기준으로 설명해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("analysis_results.tarot_analysis는 ")
            if (request.mode.includesTarot()) {
                append("title이 \"마음의 파동\"인 객체로 반환하고, content는 각 카드의 상징을 이번 질문의 감정 진폭, 불안, 기대 과열과 연결해 해석해라. 카드 뜻풀이 자체가 목적이 아니며, 마음의 결만 짧게 드러내라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("overall_summary는 바깥 흐름 해석")
            if (request.mode.includesSaju()) append(", 사주 분석")
            if (request.mode.includesTarot()) append(", 타로 분석")
            append("을 종합해 오늘의 재물 운세와 마음 상태를 한 문장으로 먼저 정리하고, 이어서 마음을 지키는 태도를 짧게 덧붙여라.")
            append('\n')
            append("risk_score는 위험 예측 점수가 아니라 현재 마음 압박의 크기를 0~100으로 나타내는 긴장도 점수로 해석해라.")
        }

    private fun validateTarotRequest(request: ConsultRequest) {
        if (!request.mode.includesTarot()) {
            if (!request.tarotIndices.isNullOrEmpty() ||
                request.tarotInterpretationMode != null ||
                request.tarotDeckVersionId != null ||
                !request.assistantDeckSelections.isNullOrEmpty()
            ) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "tarotIndices, tarotDeckVersionId, assistantDeckSelections and tarotInterpretationMode are only allowed for tarot modes"
                )
            }
            return
        }

        if (request.tarotIndices.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "tarotIndices is required for tarot modes")
        }
    }

    private fun resolveMainTarotDeckVersionId(
        requestedDeckVersionId: String?,
        fallbackDeckVersionId: String?,
        subscriptionTier: SubscriptionTier
    ): String {
        val candidateId = requestedDeckVersionId?.trim()?.ifBlank { null }
            ?: fallbackDeckVersionId?.trim()?.ifBlank { null }
            ?: DEFAULT_TAROT_DECK_VERSION_ID
        val deck = tarotDeckVersionRepository.findById(candidateId).orElse(null)
            ?: return DEFAULT_TAROT_DECK_VERSION_ID
        if (!deck.active || deck.deckRole != TarotDeckRole.MAIN) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        if (subscriptionTier.ordinal < deck.requiredSubscriptionTier.ordinal) {
            return DEFAULT_TAROT_DECK_VERSION_ID
        }
        return deck.id
    }

    private companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val DEFAULT_BIRTH_TIME = java.time.LocalTime.NOON
    }
}

private fun ConsultingPositionSnapshot.toEmotionPayload(stock: StockInfo): Map<String, Any?> {
    val drawdownRatio = averageBuyPrice.takeIf { it > java.math.BigDecimal.ZERO }
        ?.let { stock.currentPrice.subtract(it).divide(it, 4, java.math.RoundingMode.HALF_UP) }
        ?: java.math.BigDecimal.ZERO
    val emotionalBurden = when {
        drawdownRatio <= java.math.BigDecimal("-0.10") -> "손실 기억이 마음을 강하게 누르기 쉬운 상태"
        drawdownRatio < java.math.BigDecimal.ZERO -> "불안이 서서히 쌓이기 쉬운 상태"
        drawdownRatio >= java.math.BigDecimal("0.10") -> "안도감 속 과속을 경계해야 하는 상태"
        else -> "수익과 불안이 교차하며 판단이 흔들리기 쉬운 상태"
    }

    return mapOf(
        "capturedAt" to capturedAt,
        "emotionalBurden" to emotionalBurden,
        "attachmentSignal" to if (buyQuantity > 0) "이미 마음이 걸린 흐름" else "가벼운 관찰 상태",
        "interpretationRule" to "포지션 정보는 행동 지시가 아니라 사용자의 심리 압박과 집착 정도를 읽는 보조 단서다"
    )
}
