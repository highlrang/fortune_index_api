package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
import com.hwcompany.fortune_index.home.HomeService
import com.hwcompany.fortune_index.home.HomeSummaryResponse
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.saju.SajuAnalyzer
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.saju.SajuResultRepository
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotReadingResult
import com.hwcompany.fortune_index.tarot.resolveBirthTarotCard
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
    private val sajuResultRepository: SajuResultRepository,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val promptStrategies: List<com.hwcompany.fortune_index.consulting.prompt.PromptProvider>,
    private val llmPromptTemplateService: LlmPromptTemplateService,
    private val hybridConsultingAiClient: HybridConsultingAiClient,
    private val consultingRiskScoreCalculator: ConsultingRiskScoreCalculator,
    private val consultingHistoryService: ConsultingHistoryService,
    private val objectMapper: ObjectMapper,
    private val fortuneSafetyGuard: FortuneSafetyGuard,
    private val homeService: HomeService
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

        val referenceDateTime = request.referenceDateTime ?: LocalDateTime.now(DEFAULT_ZONE_ID)
        val tarotReading = request.mode.includesTarot().takeIf { it }?.let {
            tarotDeckService.drawReading(
                subscriptionTier = user.subscriptionTier,
                deckVersionId = resolvedTarotDeckVersionId,
                indices = request.tarotIndices,
                assistantDeckSelections = request.assistantDeckSelections.orEmpty().map { it.toTarotAssistantDeckSelection() },
                interpretationMode = request.tarotInterpretationMode ?: TarotInterpretationMode.MAIN_TRADITIONAL
            )
        }
        val personalZodiacProfile = ZodiacSign.from(user.birthInfo.birthDate).let { sign ->
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
        val personalSaju = sajuAnalyzer.analyzeForConsulting(
            birthDateTime = birthDateTime,
            referenceDateTime = referenceDateTime,
            zoneId = DEFAULT_ZONE_ID,
            gender = user.gender
        )
        val saju = personalSaju.takeIf { request.mode.includesSaju() }
        val zodiacProfile = personalZodiacProfile.takeIf { request.mode.includesZodiac() }
        val homeSummary = homeService.getSummary(referenceDateTime.atZone(DEFAULT_ZONE_ID))
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
            saju = personalSaju,
            zodiac = personalZodiacProfile,
            tarotReading = tarotReading,
            riskProfile = user.investmentRiskProfile,
            routingDecision = routingDecision,
            freshness = freshness,
            homeSummary = homeSummary
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
            consultedAt = referenceDateTime
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
        freshness: InvestmentEvidenceResponse,
        homeSummary: HomeSummaryResponse
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "scenario" to scenario.title,
                "question" to question,
                "userProfile" to riskProfile.toKoreanLabel(),
                "dailyFlow" to mapOf(
                    "saju" to homeSummary.saju.name.takeIf { request.mode.includesSaju() },
                    "tarot" to homeSummary.tarot.name.takeIf { request.mode.includesTarot() },
                    "zodiac" to homeSummary.zodiac.name.takeIf { request.mode.includesZodiac() }
                ),
                "focusLabel" to focusLabel,
                "saju" to buildSajuPayload(request.userId, saju),
                "zodiac" to zodiac?.toMinimalAiPayload(),
                "tarot" to tarotReading?.let {
                    mapOf(
                        "birthTarotCard" to storedBirthTarotCardCode(request.userId),
                        "todayTarotCard" to homeSummary.tarot.name,
                        "drawnCards" to it.cards.map(::toAiTarotCardPayload),
                        "assistantCards" to it.assistantDecks
                            .flatMap { deck -> deck.cards.map(::toAiTarotCardPayload) }
                            .takeIf { cards -> cards.isNotEmpty() }
                    )
                }
            )
        )

    private fun toAiTarotCardPayload(draw: com.hwcompany.fortune_index.tarot.TarotDrawResult): String =
        draw.card.code

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
            append("시나리오=${scenario.name}(${scenario.title}). ")
            append(scenario.systemInstructionAddon())
            append('\n')
            append("질문: ")
            append(question)
            append('\n')
            append("payload에 들어 있는 질문에 직접 답해라. 질문과 무관한 일반론은 줄여라.")
            append('\n')
            if (request.mode.includesSaju()) {
                append("사주 해석은 사주팔자, 현재 대운, 세운, 오늘의 사주 흐름을 함께 묶어 질문에 답해라.")
                append('\n')
            }
            if (request.mode.includesTarot()) {
                append("타로 해석은 생일 타로 1장, 오늘의 타로 흐름 1장, 실제 뽑힌 카드 3장을 함께 묶어 질문에 답해라.")
                append('\n')
            }
            if (request.mode.includesZodiac()) {
                append("별자리 해석은 사용자의 별자리 코드와 오늘의 별자리 흐름을 함께 묶어 질문에 답해라.")
                append('\n')
            }
            append("투자 지시, 장황한 설명, 결과 보장은 금지다.")
            append('\n')
            if (routingDecision.requiresWebSearch) {
                append("최신 확인이 필요한 경우에도 단정하지 말고 분위기 수준으로만 답해라.")
                append('\n')
            }
            append("saju_analysis, tarot_analysis, zodiac_analysis, overall_summary는 모두 3문장 안팎으로 써라.")
            append('\n')
            append("반드시 평평한 JSON만 반환해라. analysis_results 같은 중첩 객체와 mode, investment_analysis는 넣지 마라.")
            append('\n')
            append("saju_analysis는 ")
            if (request.mode.includesSaju()) {
                append("문자열 3문장 안팎으로 반환해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("tarot_analysis는 ")
            if (request.mode.includesTarot()) {
                append("문자열 3문장 안팎으로 반환해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("zodiac_analysis는 ")
            if (request.mode.includesZodiac()) {
                append("문자열 3문장 안팎으로 반환해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("overall_summary는")
            if (request.mode.includesSaju()) append(", 사주 분석")
            if (request.mode.includesTarot()) append(", 타로 분석")
            if (request.mode.includesZodiac()) append(", 별자리 분석")
            append("을 종합한 3문장 안팎으로 써라.")
            append('\n')
            append("risk_score는 0~100 긴장도 점수로만 써라.")
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
        val activeDeckId = tarotDeckVersionRepository.findAllByOrderByActiveDescDisplayOrderAscNameAsc()
            .firstOrNull { it.active && it.deckRole == TarotDeckRole.MAIN && subscriptionTier.ordinal >= it.requiredSubscriptionTier.ordinal }
            ?.id
        if (activeDeckId != null) {
            return activeDeckId
        }

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

    private fun buildSajuPayload(userId: Long, saju: SajuConsultingResult?): Map<String, Any?>? {
        if (saju == null) return null
        val storedPalza = sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(userId)?.let { result ->
            val stemsByOrder = result.heavenlyStems.associateBy { it.pillarOrder }
            val branchesByOrder = result.earthlyBranches.associateBy { it.pillarOrder }
            (1..4).mapNotNull { order ->
                val stem = stemsByOrder[order]?.labelKo
                val branch = branchesByOrder[order]?.labelKo
                if (stem == null || branch == null) null else stem + branch
            }.joinToString(" ")
        }
        return mapOf(
            "palza" to storedPalza,
            "majorFortune" to "${saju.currentFortune.majorFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.majorFortune.pillar.earthlyBranch.toKoreanCode()}",
            "yearlyFortune" to "${saju.currentFortune.yearlyFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.yearlyFortune.pillar.earthlyBranch.toKoreanCode()}"
        )
    }

    private fun storedZodiacSnapshot(userId: Long): String? =
        userRepository.findById(userId).orElse(null)?.westernZodiac?.sign

    private fun storedBirthTarotCardCode(userId: Long): String? =
        userRepository.findById(userId).orElse(null)?.let { user ->
            user.birthTarotCardCode ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString()).code
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

private fun ZodiacConsultingProfile.toMinimalAiPayload(): String = sign.name

private fun InvestmentRiskProfile.toKoreanLabel(): String =
    when (this) {
        InvestmentRiskProfile.STABLE -> "신중형"
        InvestmentRiskProfile.AGGRESSIVE -> "직진형"
    }

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toKoreanCode(): String = labelKo()

private fun com.hwcompany.fortune_index.domain.model.EarthlyBranch.toKoreanCode(): String = labelKo()
