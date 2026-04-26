package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuConsultingResult
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
    private val consultingRequestRouter: ConsultingRequestRouter,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val promptStrategies: List<com.hwcompany.fortune_index.consulting.prompt.PromptProvider>,
    private val llmPromptTemplateService: LlmPromptTemplateService,
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

    fun consult(request: ConsultRequest): ConsultResponse {
        val prepared = prepareConsultation(request)
        val aiResponse = hybridConsultingAiClient.requestJsonAdvice(
            systemMessage = prepared.prompt,
            payload = prepared.payload
        )
        val safeAiResponse = fortuneSafetyGuard.enforce(
            request = request,
            response = aiResponse
        )
        val calculatedRiskScore = consultingRiskScoreCalculator.calculate(
            mode = request.mode,
            scenario = prepared.scenario,
            riskProfile = prepared.riskProfile
        )
        val normalizedAiResponse = consultingRiskScoreCalculator.overrideRiskScore(
            response = safeAiResponse,
            riskScore = calculatedRiskScore,
            rawJson = safeAiResponse.copy(riskScore = calculatedRiskScore).toCanonicalJson()
        )

        val savedHistory = consultingHistoryService.saveHybridHistory(
            SaveHybridConsultingHistoryCommand(
                userId = prepared.userId,
                mode = request.mode,
                focusLabel = prepared.focusLabel,
                question = prepared.question,
                scenario = prepared.scenario,
                sajuResult = prepared.saju,
                tarotReading = prepared.tarotReading,
                analysisResultJson = objectMapper.writeValueAsString(prepared.payload),
                aiResponse = normalizedAiResponse,
                consultedAt = prepared.consultedAt
            )
        )

        return ConsultResponse(
            mode = request.mode,
            focus = FocusConsultResponse.fromLabel(prepared.focusLabel),
            saju = prepared.saju,
            zodiac = prepared.zodiac?.let { ZodiacConsultResponse.from(it) },
            tarot = prepared.tarotReading?.let { TarotConsultResponse.from(it) },
            ai = normalizedAiResponse,
            history = savedHistory,
            investmentEvidence = prepared.freshness
        )
    }

    /**
     * DB 조회와 로컬 계산만 수행한다. 외부 AI 호출은 트랜잭션 밖에서 실행한다.
     */
    @Transactional(readOnly = true)
    fun prepareConsultation(request: ConsultRequest): PreparedConsultation {
        val user = userRepository.findById(request.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${request.userId}") }
        validateRequest(request)
        validateTarotRequest(request)
        val resolvedScenario = resolveScenario(request)
        val resolvedQuestion = resolveQuestion(request, resolvedScenario)
        val resolvedFocusLabel = resolveFocusLabel(request)
        val routingDecision = consultingRequestRouter.route(
            request = request,
            resolvedQuestion = resolvedQuestion,
            resolvedScenario = resolvedScenario,
            resolvedFocusLabel = resolvedFocusLabel
        )
        val resolvedTarotDeckVersionId = resolveMainTarotDeckVersionId(
            requestedDeckVersionId = request.tarotDeckVersionId,
            fallbackDeckVersionId = user.preferredTarotDeckId,
            subscriptionTier = user.subscriptionTier
        )

        val tarotReading = request.mode.includesTarot().takeIf { it }?.let {
            tarotDeckService.drawReading(
                subscriptionTier = user.subscriptionTier,
                deckVersionId = resolvedTarotDeckVersionId,
                indices = request.tarotIndices,
                assistantDeckSelections = request.assistantDeckSelections.orEmpty().map { it.toTarotAssistantDeckSelection() },
                interpretationMode = request.tarotInterpretationMode ?: TarotInterpretationMode.MAIN_TRADITIONAL
            )
        }
        val zodiacProfile = request.mode.includesZodiac().takeIf { it }?.let {
            val sign = ZodiacSign.from(user.birthInfo.birthDate)
            ZodiacConsultingProfile(
                sign = sign,
                birthDate = user.birthInfo.birthDate,
                headline = sign.toHeadline()
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
        val userId = requireNotNull(user.id)
        val freshness = evaluateFreshness(
            routingDecision = routingDecision
        )
        validatePreGenerationFreshness(freshness)
        val payload = buildPayload(
            request = request,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            focusLabel = resolvedFocusLabel,
            tarotDeckVersionId = resolvedTarotDeckVersionId,
            saju = saju,
            zodiac = zodiacProfile,
            tarotReading = tarotReading,
            riskProfile = user.investmentRiskProfile,
            routingDecision = routingDecision,
            freshness = freshness
        )
        val prompt = buildScenarioAwareSystemMessage(
            request = request,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            riskProfile = user.investmentRiskProfile,
            routingDecision = routingDecision,
            freshness = freshness
        )
        return PreparedConsultation(
            userId = userId,
            riskProfile = user.investmentRiskProfile,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            focusLabel = resolvedFocusLabel,
            saju = saju,
            zodiac = zodiacProfile,
            tarotReading = tarotReading,
            payload = payload,
            prompt = prompt,
            freshness = freshness,
            consultedAt = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID)
        )
    }

    private fun evaluateFreshness(routingDecision: ConsultingRoutingDecision): InvestmentEvidenceResponse {
        return InvestmentEvidenceResponse(
            routing = RoutingEvidenceResponse.from(routingDecision),
            investmentAsOf = null,
            positionAsOf = null,
            newsAsOf = null,
            priceFresh = true,
            positionFresh = true,
            newsFresh = !routingDecision.requiresWebSearch,
            investmentDataUsed = false,
            investmentFlowDataUsed = false,
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
        scenario: ConsultingScenario,
        focusLabel: String,
        tarotDeckVersionId: String,
        saju: SajuConsultingResult?,
        zodiac: ZodiacConsultingProfile?,
        tarotReading: TarotReadingResult?,
        riskProfile: InvestmentRiskProfile,
        routingDecision: ConsultingRoutingDecision,
        freshness: InvestmentEvidenceResponse
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "scenario" to mapOf(
                    "code" to scenario.name,
                    "title" to scenario.title,
                    "description" to scenario.description
                ),
                "question" to question,
                "userProfile" to mapOf(
                    "riskProfile" to riskProfile.name,
                    "riskProfileLabel" to when (riskProfile) {
                        InvestmentRiskProfile.STABLE -> "신중형"
                        InvestmentRiskProfile.AGGRESSIVE -> "직진형"
                    }
                ),
                "focusLabel" to focusLabel,
                "routingHint" to routingDecision.questionType,
                "freshness" to mapOf(
                    "priceFresh" to freshness.priceFresh,
                    "positionFresh" to freshness.positionFresh,
                    "newsFresh" to freshness.newsFresh
                ),
                "saju" to saju?.toCompactAiPayload(),
                "zodiac" to zodiac?.toAiPayload(),
                "tarot" to tarotReading?.let {
                    mapOf(
                        "deckVersionId" to tarotDeckVersionId,
                        "interpretationMode" to it.interpretationMode.name,
                        "cards" to it.cards.map(::toAiTarotCardPayload),
                        "assistantDecks" to it.assistantDecks.map { deck ->
                            mapOf(
                                "deckVersionId" to deck.deckVersionId,
                                "deckType" to deck.deckType.name,
                                "cards" to deck.cards.map(::toAiTarotCardPayload)
                            )
                        }.takeIf { deckGroups -> deckGroups.isNotEmpty() }
                    )
                }
            )
        )

    private fun toAiTarotCardPayload(draw: com.hwcompany.fortune_index.tarot.TarotDrawResult): Map<String, Any?> =
        mapOf(
            "selectedIndex" to draw.index,
            "code" to draw.card.code,
            "name" to draw.card.name,
            "koreanName" to draw.card.koreanName,
            "meaning" to draw.card.meaning
        )

    private fun validatePreGenerationFreshness(freshness: InvestmentEvidenceResponse) {
        if (freshness.positionDataUsed && !freshness.positionFresh) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "latest position data is required but unavailable"
            )
        }
    }

    private fun buildScenarioAwareSystemMessage(
        request: ConsultRequest,
        question: String,
        scenario: ConsultingScenario,
        riskProfile: InvestmentRiskProfile,
        routingDecision: ConsultingRoutingDecision,
        freshness: InvestmentEvidenceResponse
    ): String =
        buildString {
            append(promptStrategyByMode.getValue(request.mode).buildSystemMessage())
            append('\n')
            append(InvestmentPartnerPersonaPromptGuidance.build())
            append('\n')
            append(InvestmentProfilePromptGuidance.forRiskProfile(riskProfile))
            append('\n')
            append("이번 상담 시나리오는 ${scenario.name}(${scenario.title})이다. ")
            append(scenario.systemInstructionAddon())
            append('\n')
            append("사용자의 핵심 질문은 다음과 같다: ")
            append(question)
            append('\n')
            append("모든 섹션은 반드시 consulting_scenario와 question에 직접 답해야 한다. ")
            append("일반론이나 개념 설명으로 길게 빠지지 말고, 이번 질문의 의사결정에 필요한 해석만 남겨라.")
            append('\n')
            append("routing.questionType은 ${routingDecision.questionType} 이다. ")
            append("이번 질문은 ${routingDecision.questionType} 성격으로 분류되었다. ")
            append('\n')
            append("freshness 기준: priceFresh=${freshness.priceFresh}, positionFresh=${freshness.positionFresh}, newsFresh=${freshness.newsFresh} 이다. ")
            append("fresh가 아닌 데이터는 최신 데이터처럼 단정하지 마라. ")
            append('\n')
            append("이 서비스는 돈의 흐름과 마음 상태를 읽어 주는 서비스다. ")
            append("어려운 투자 용어나 전문가 말투, 무엇을 사거나 팔라는 식의 표현, 결과를 보장하는 표현은 절대 사용하지 마라. ")
            append("바깥 시세나 시장 상황을 정확히 아는 것처럼 말하지 말고, 질문과 사주, 타로에 드러난 상징만 바탕으로 해석해라.")
            append('\n')
            append("이번 답변은 질문, 선택된 상담 종류의 상징 정보만 중심으로 해석한다. 구체적인 값이나 순간 변화를 아는 것처럼 말하지 마라.")
            append('\n')
            if (routingDecision.requiresWebSearch) {
                append("이번 답변은 최신 소식 반영이 필요하다. 충분히 확인되지 않았다면 이유를 단정하지 말고, 전반적인 분위기 수준으로만 설명해라.")
                append('\n')
            }
            append("문장은 친절하고 쉬워야 하며, 어려운 말보다 상징과 흐름의 언어를 우선해라. 각 analysis 섹션은 1~2문장, overall_summary는 1~2문장 이내로 제한해라.")
            append('\n')
            append("analysis_results.investment_analysis.title은 반드시 \"외부 기류 해석\"으로 고정하고, content는 오늘의 질문과 상징이 사용자의 감정과 재물 기운에 어떤 공기감을 주는지 설명해라.")
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
            append("analysis_results.zodiac_analysis는 ")
            if (request.mode.includesZodiac()) {
                append("title이 \"별자리 흐름 해석\"인 객체로 반환하고, content는 별자리의 성향과 오늘의 감정 리듬을 바탕으로 재물 감각과 마음의 방향을 짧게 설명해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("overall_summary는 바깥 흐름 해석")
            if (request.mode.includesSaju()) append(", 사주 분석")
            if (request.mode.includesTarot()) append(", 타로 분석")
            if (request.mode.includesZodiac()) append(", 별자리 분석")
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

    }

    private fun validateRequest(request: ConsultRequest) {
        val hasQuestion = !request.question.isNullOrBlank()
        val hasScenario = request.scenario != null

        if (!hasQuestion && !hasScenario) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "question 또는 scenario 중 하나 이상은 필요합니다."
            )
        }
    }

    private fun resolveScenario(request: ConsultRequest): ConsultingScenario =
        request.scenario ?: ConsultingScenario.MENTAL_GUIDE

    private fun resolveQuestion(
        request: ConsultRequest,
        resolvedScenario: ConsultingScenario
    ): String {
        val normalizedQuestion = request.question?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedQuestion != null) {
            return normalizedQuestion
        }
        if (request.scenario == null && !request.tarotIndices.isNullOrEmpty()) {
            return TAROT_ONLY_DEFAULT_QUESTION
        }
        return defaultQuestion(request.mode, resolvedScenario)
    }

    private fun resolveFocusLabel(request: ConsultRequest): String =
        request.focusLabel?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_FOCUS_LABEL

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

    private fun defaultQuestion(mode: AnalysisMode, scenario: ConsultingScenario): String =
        if (mode.includesTarot() && scenario == ConsultingScenario.MENTAL_GUIDE) {
            TAROT_ONLY_DEFAULT_QUESTION
        } else {
            defaultQuestion(mode)
        }

    private fun defaultQuestion(mode: AnalysisMode): String =
        when (mode) {
            AnalysisMode.INVESTMENT_SAJU -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU)
            AnalysisMode.INVESTMENT_TAROT -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT)
            AnalysisMode.INVESTMENT_ZODIAC -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC)
            AnalysisMode.INVESTMENT_ALL -> llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL)
        }

    private companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val DEFAULT_BIRTH_TIME = java.time.LocalTime.NOON
        const val DEFAULT_FOCUS_LABEL = "오늘의 흐름"
        const val TAROT_ONLY_DEFAULT_QUESTION = "선택된 타로 3장으로 오늘의 흐름과 주의점, 한마디 조언을 해석해줘"
    }
}

data class PreparedConsultation(
    val userId: Long,
    val riskProfile: InvestmentRiskProfile,
    val question: String,
    val scenario: ConsultingScenario,
    val focusLabel: String,
    val saju: SajuConsultingResult?,
    val zodiac: ZodiacConsultingProfile?,
    val tarotReading: TarotReadingResult?,
    val payload: JsonNode,
    val prompt: String,
    val freshness: InvestmentEvidenceResponse,
    val consultedAt: LocalDateTime
)

private fun SajuConsultingResult.toCompactAiPayload(): Map<String, Any?> =
    mapOf(
        "dayMaster" to analysis.keyPalaces.dayMaster.symbol,
        "dayBranch" to analysis.keyPalaces.dayBranch.symbol,
        "monthBranch" to analysis.keyPalaces.monthBranch.symbol,
        "fiveElements" to mapOf(
            "wood" to analysis.fiveElementBalance.wood,
            "fire" to analysis.fiveElementBalance.fire,
            "earth" to analysis.fiveElementBalance.earth,
            "metal" to analysis.fiveElementBalance.metal,
            "water" to analysis.fiveElementBalance.water
        ),
        "currentFortune" to mapOf(
            "referenceYear" to currentFortune.referenceYear,
            "majorFortunePillar" to "${currentFortune.majorFortune.pillar.heavenlyStem.name}${currentFortune.majorFortune.pillar.earthlyBranch.name}",
            "yearlyFortunePillar" to "${currentFortune.yearlyFortune.pillar.heavenlyStem.name}${currentFortune.yearlyFortune.pillar.earthlyBranch.name}"
        )
    )

private fun ZodiacConsultingProfile.toAiPayload(): Map<String, Any?> =
    mapOf(
        "sign" to sign.name,
        "signKo" to sign.koreanName,
        "element" to sign.element,
        "moodKeyword" to sign.moodKeyword,
        "headline" to headline
    )
