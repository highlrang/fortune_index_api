package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.domain.model.ConsultingTone
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
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatureService
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatures
import com.hwcompany.fortune_index.tarot.DEFAULT_TAROT_DECK_VERSION_ID
import com.hwcompany.fortune_index.tarot.TarotCard
import com.hwcompany.fortune_index.tarot.TarotDeckRole
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import com.hwcompany.fortune_index.tarot.TarotDeckVersionRepository
import com.hwcompany.fortune_index.tarot.TarotDeckService
import com.hwcompany.fortune_index.tarot.TarotDrawGroupResult
import com.hwcompany.fortune_index.tarot.TarotDrawResult
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
    private val sajuResultRepository: SajuResultRepository,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val promptStrategies: List<com.hwcompany.fortune_index.consulting.prompt.PromptProvider>,
    private val hybridConsultingAiClient: HybridConsultingAiClient,
    private val consultingRiskScoreCalculator: ConsultingRiskScoreCalculator,
    private val sajuInvestmentFeatureService: SajuInvestmentFeatureService,
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
            riskProfile = prepared.riskProfile,
            sajuFeatures = prepared.sajuFeatures
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
            history = savedHistory
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
        val resolvedQuestion = resolveQuestion(request)
        val resolvedFocusLabel = resolveFocusLabel(resolvedScenario)
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
        val homeSummary = homeService.getSummary(now = referenceDateTime.atZone(DEFAULT_ZONE_ID))
        val userId = requireNotNull(user.id)
        val sajuInvestmentFeatures = saju?.let(sajuInvestmentFeatureService::extract)
        val payload = buildPayload(
            request = request,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            focusLabel = resolvedFocusLabel,
            saju = personalSaju,
            sajuInvestmentFeatures = sajuInvestmentFeatures,
            zodiac = personalZodiacProfile,
            tarotReading = tarotReading,
            riskProfile = user.investmentRiskProfile,
            homeSummary = homeSummary
        )
        val prompt = buildScenarioAwareSystemMessage(
            request = request,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            riskProfile = user.investmentRiskProfile,
            consultingTone = user.consultingTone,
            sajuInvestmentFeatures = sajuInvestmentFeatures
        )
        return PreparedConsultation(
            userId = userId,
            riskProfile = user.investmentRiskProfile,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            focusLabel = resolvedFocusLabel,
            saju = saju,
            sajuFeatures = sajuInvestmentFeatures,
            zodiac = zodiacProfile,
            tarotReading = tarotReading,
            payload = payload,
            prompt = prompt,
            consultedAt = referenceDateTime
        )
    }

    private fun buildPayload(
        request: ConsultRequest,
        question: String,
        scenario: ConsultingScenario,
        focusLabel: String,
        saju: SajuConsultingResult?,
        sajuInvestmentFeatures: SajuInvestmentFeatures?,
        zodiac: ZodiacConsultingProfile?,
        tarotReading: TarotReadingResult?,
        riskProfile: InvestmentRiskProfile,
        homeSummary: HomeSummaryResponse
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "scenario" to scenario.title,
                "question" to question,
                "userProfile" to riskProfile.toKoreanLabel(),
                "interpretationPolicy" to mapOf(
                    "factSource" to "사주, 타로, 별자리의 원자료는 서버 계산, DB 조회, 일별 캐시에서 확정된 값이다.",
                    "llmRole" to "LLM은 payload에 들어 있는 확정 값을 바꾸지 않고 사용자 질문에 맞게 해석 문장만 작성한다.",
                    "doNotInvent" to "payload에 없는 팔자, 대운, 세운, 타로 카드, 별자리, 오늘 흐름은 새로 만들거나 추정하지 않는다."
                ),
                "dailyFlow" to mapOf(
                    "saju" to homeSummary.saju.name.takeIf { request.mode.includesSaju() },
                    "tarot" to homeSummary.tarot.name.takeIf { request.mode.includesTarot() },
                    "zodiac" to homeSummary.zodiac.name.takeIf { request.mode.includesZodiac() }
                ),
                "focusLabel" to focusLabel,
                "saju" to buildSajuPayload(request.userId, saju, sajuInvestmentFeatures),
                "zodiac" to zodiac?.toAiPayload(homeSummary.zodiac.name),
                "tarot" to tarotReading?.toAiPayload(
                    birthTarotCard = storedBirthTarotCardPayload(request.userId),
                    todayTarotCard = homeSummary.tarot.name
                )
            )
        )

    private fun buildScenarioAwareSystemMessage(
        request: ConsultRequest,
        question: String,
        scenario: ConsultingScenario,
        riskProfile: InvestmentRiskProfile,
        consultingTone: ConsultingTone,
        sajuInvestmentFeatures: SajuInvestmentFeatures?
    ): String =
        buildString {
            append(promptStrategyByMode.getValue(request.mode).buildSystemMessage())
            append('\n')
            append(InvestmentPartnerPersonaPromptGuidance.build())
            append('\n')
            append(ConsultingTonePromptGuidance.forTone(consultingTone))
            append('\n')
            append(InvestmentProfilePromptGuidance.forRiskProfile(riskProfile))
            append('\n')
            append("상담 종류, 시나리오, 사용자 질문을 이 답변의 핵심 기준으로 삼아라.")
            append('\n')
            append("상담 종류는 해석의 재료를 정하고, 시나리오는 해석의 관점을 정하며, 사용자 질문은 답변의 직접적인 목표를 정한다.")
            append('\n')
            append("모든 분석과 요약은 이 세 기준에 직접 연결되도록 일관되게 작성하고, 서로 다른 결의 일반론으로 흩어지지 마라.")
            append('\n')
            append("사용자의 불안을 회피 신호로만 보지 마라. 불안 밑에는 더 크게 움직이고 싶은 마음, 지금 선택을 정당화받고 싶은 마음, 용기를 얻고 싶은 마음이 함께 있을 수 있다.")
            append('\n')
            append("그 심리의 양면을 짚은 뒤 오늘 더 강한 방향을 분명히 말해라. 단, 무엇을 사거나 팔라는 투자 지시는 금지다.")
            append('\n')
            append("시나리오=${scenario.name}(${scenario.title}). ")
            append(scenario.responseInstructionAddon())
            append('\n')
            append("질문: ")
            append(question)
            append('\n')
            append("payload에 들어 있는 정보만 활용해 질문에 직접 답해라. 질문과 무관한 일반론은 줄여라.")
            append('\n')
            append("사주, 타로, 별자리의 기준 값은 서버 계산, DB 조회, 일별 캐시에서 이미 확정된 원자료다. LLM은 원자료를 새로 만들거나 수정하지 말고 해석 문장만 작성해라.")
            append('\n')
            append("payload에 없는 팔자, 대운, 세운, 타로 카드명, 카드 의미, 별자리, 오늘 흐름은 추정하거나 보완하지 마라.")
            append('\n')
            if (request.mode.includesSaju()) {
                append("사주 해석은 payload.saju의 palza, majorFortune, yearlyFortune, investmentFeatures와 dailyFlow.saju만 사용해 질문에 답해라.")
                append('\n')
                if (sajuInvestmentFeatures != null) {
                    append("payload.saju.investmentFeatures의 내부 label은 투자 성향, 심리, 변동성, 리밸런싱 필요성을 설명하는 보조 신호로만 활용해라.")
                    append('\n')
                }
            }
            if (request.mode.includesTarot()) {
                append("타로 해석은 payload.tarot의 birthTarotCard, todayTarotCard, drawnCards, assistantDecks에 들어 있는 카드 코드, 이름, 의미만 사용해 질문에 답해라.")
                append('\n')
            }
            if (request.mode.includesZodiac()) {
                append("별자리 해석은 payload.zodiac의 sign, element, moodKeyword, headline, todayZodiacFlow만 사용해 질문에 답해라.")
                append('\n')
            }
            append("투자 지시, 장황한 설명, 결과 보장은 금지다.")
            append('\n')
            append("사주, 타로, 별자리 상징은 돈, 소비, 투자 심리로 바로 번역해라.")
            append('\n')
            append("돈 상담 선배처럼 조언해라.")
            append('\n')
            append("답변은 설정된 말투에 맞춰 젊고 톡톡 튀게, 쉽고 짧게 써라. 어려운 말, 학문적인 표현, 추상적인 은유, 어색한 합성어는 쓰지 마라.")
            append('\n')
            append("조언은 분명하게 해라. 과한 밈, 유행어, 드립 남발은 금지다.")
            append('\n')
            append("결론은 하나마나한 균형론으로 끝내지 말고, 오늘은 기다림, 유지, 덜어내기 중 어느 쪽으로 마음의 무게를 둬야 하는지 선명하게 말해라.")
            append('\n')
            append("각 문장은 오늘의 상황, 판단 기준, 바로 할 행동 중 하나를 분명히 말해라.")
            append('\n')
            append("사용자가 '이 정도면 해볼 수 있겠다'고 느끼게 작고 쉬운 행동을 제안해라.")
            append('\n')
            append("단호하지만 따뜻하게 말해라. 겁주기, 훈계, 과한 장난, 근거 없는 낙관은 금지다.")
            append('\n')
            append("모호한 말은 금지다. '흐름', '기운', '에너지', '현실 감각', '분석적인 흐름' 같은 표현만으로 설명하지 마라.")
            append('\n')
            append("예: 괜찮아, 오늘 큰 결정을 안 해도 돼. 새로 사기보다 보유 이유를 다시 확인하고, 손실 한도나 이번 달 지출액 하나만 숫자로 적어봐.")
            append('\n')
            append("saju_analysis, tarot_analysis, zodiac_analysis, overall_summary는 모두 2~3문장으로 써라.")
            append('\n')
            append("각 분석 섹션은 서로 다른 재료가 주는 판단 근거와 심리 방향을 말해라. 같은 실행 문장을 반복하지 마라.")
            append('\n')
            append("반드시 평평한 JSON만 반환해라. analysis_results 같은 중첩 객체와 mode, investment_analysis는 넣지 마라.")
            append('\n')
            append("saju_analysis는 ")
            if (request.mode.includesSaju()) {
                append("문자열 2~3문장으로 반환하고, 사주가 가리키는 성향과 오늘 더 강한 판단 방향을 말해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("tarot_analysis는 ")
            if (request.mode.includesTarot()) {
                append("문자열 2~3문장으로 반환하고, 타로가 보여 주는 감정의 속도와 용기를 얻고 싶은 마음을 말해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("zodiac_analysis는 ")
            if (request.mode.includesZodiac()) {
                append("문자열 2~3문장으로 반환하고, 별자리 흐름이 오늘 판단을 어느 쪽으로 선명하게 밀어 주는지 말해라.")
            } else {
                append("null로 반환해라.")
            }
            append('\n')
            append("overall_summary는")
            if (request.mode.includesSaju()) append(", 사주 분석")
            if (request.mode.includesTarot()) append(", 타로 분석")
            if (request.mode.includesZodiac()) append(", 별자리 분석")
            append("을 종합한 2~3문장으로 쓰고 마지막 문장은 바로 할 행동으로 끝내라.")
            append('\n')
            append("risk_score는 0~100 오늘의 재물 컨디션 점수로만 써라. 점수가 높을수록 오늘의 마음과 돈 판단이 안정적인 상태다.")
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
        if (request.question.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "question은 필수입니다.")
        }
        if (request.scenario == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "scenario는 필수입니다.")
        }
    }

    private fun resolveScenario(request: ConsultRequest): ConsultingScenario =
        requireNotNull(request.scenario)

    private fun resolveQuestion(request: ConsultRequest): String =
        requireNotNull(request.question).trim()

    private fun resolveFocusLabel(resolvedScenario: ConsultingScenario): String = resolvedScenario.title

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

    private companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val DEFAULT_BIRTH_TIME = java.time.LocalTime.NOON
    }

    private fun buildSajuPayload(
        userId: Long,
        saju: SajuConsultingResult?,
        sajuInvestmentFeatures: SajuInvestmentFeatures?
    ): Map<String, Any?>? {
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
        val natalChart = saju.analysis.natalChart
        val calculatedPalza = listOf(
            natalChart.year,
            natalChart.month,
            natalChart.day,
            natalChart.hour
        ).joinToString(" ") {
            it.heavenlyStem.toKoreanCode() + it.earthlyBranch.toKoreanCode()
        }
        return mapOf(
            "source" to "server_calculated_and_stored",
            "palza" to (storedPalza ?: calculatedPalza),
            "majorFortune" to "${saju.currentFortune.majorFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.majorFortune.pillar.earthlyBranch.toKoreanCode()}",
            "yearlyFortune" to "${saju.currentFortune.yearlyFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.yearlyFortune.pillar.earthlyBranch.toKoreanCode()}",
            "investmentFeatures" to sajuInvestmentFeatures
        )
    }

    private fun storedBirthTarotCardPayload(userId: Long): Map<String, String?>? =
        userRepository.findById(userId).orElse(null)?.let { user ->
            val card = user.birthTarotCardCode
                ?.let { code -> runCatching { TarotCard.fromCode(code) }.getOrNull() }
                ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString())
            mapOf(
                "source" to "server_stored_or_calculated_birth_tarot",
                "code" to card.code,
                "name" to card.displayName,
                "meaning" to card.uprightMeaning,
                "description" to card.description,
                "arcanaType" to card.arcanaType.name,
                "suit" to card.suit?.name
            )
        }
}

data class PreparedConsultation(
    val userId: Long,
    val riskProfile: InvestmentRiskProfile,
    val question: String,
    val scenario: ConsultingScenario,
    val focusLabel: String,
    val saju: SajuConsultingResult?,
    val sajuFeatures: SajuInvestmentFeatures?,
    val zodiac: ZodiacConsultingProfile?,
    val tarotReading: TarotReadingResult?,
    val payload: JsonNode,
    val prompt: String,
    val consultedAt: LocalDateTime
)

private fun ZodiacConsultingProfile.toAiPayload(todayZodiacFlow: String): Map<String, Any> =
    mapOf(
        "source" to "server_calculated_profile_and_daily_cache",
        "sign" to sign.name,
        "signKo" to sign.koreanName,
        "englishName" to sign.englishName,
        "birthDate" to birthDate.toString(),
        "element" to sign.element,
        "moodKeyword" to sign.moodKeyword,
        "headline" to headline,
        "todayZodiacFlow" to todayZodiacFlow
    )

private fun TarotReadingResult.toAiPayload(
    birthTarotCard: Map<String, String?>?,
    todayTarotCard: String
): Map<String, Any?> =
    mapOf(
        "source" to "server_selected_tarot_db",
        "interpretationMode" to interpretationMode.name,
        "birthTarotCard" to birthTarotCard,
        "todayTarotCard" to todayTarotCard,
        "drawnCards" to cards.map { it.toAiPayload() },
        "assistantDecks" to assistantDecks
            .map { it.toAiPayload() }
            .takeIf { it.isNotEmpty() }
    )

private fun TarotDrawGroupResult.toAiPayload(): Map<String, Any> =
    mapOf(
        "deckVersionId" to deckVersionId,
        "deckType" to deckType.name,
        "deckRole" to deckRole.name,
        "cardSetId" to cardSetId,
        "cards" to cards.map { it.toAiPayload() }
    )

private fun TarotDrawResult.toAiPayload(): Map<String, Any?> =
    mapOf(
        "selectedIndex" to index,
        "code" to card.code,
        "name" to card.name,
        "koreanName" to card.koreanName,
        "meaning" to card.meaning,
        "description" to card.description,
        "deckVersionId" to card.deckVersionId,
        "deckType" to card.deckType.name,
        "deckRole" to card.deckRole.name,
        "cardSetId" to card.cardSetId,
        "arcanaType" to card.arcanaType?.name,
        "suit" to card.suit?.name
    )

private fun InvestmentRiskProfile.toKoreanLabel(): String =
    when (this) {
        InvestmentRiskProfile.STABLE -> "신중형"
        InvestmentRiskProfile.AGGRESSIVE -> "직진형"
    }

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toKoreanCode(): String = labelKo()

private fun com.hwcompany.fortune_index.domain.model.EarthlyBranch.toKoreanCode(): String = labelKo()
