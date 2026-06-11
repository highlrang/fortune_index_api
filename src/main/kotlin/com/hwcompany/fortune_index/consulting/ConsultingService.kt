package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.ai.HybridConsultingAiClient
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import com.hwcompany.fortune_index.domain.model.User
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.history.ConsultingHistoryService
import com.hwcompany.fortune_index.history.SaveHybridConsultingHistoryCommand
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
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
    private val fortuneSafetyGuard: FortuneSafetyGuard
) {
    private val promptStrategyByMode = AnalysisMode.entries.associateWith { mode ->
        promptStrategies.firstOrNull { it.supports(mode) }
            ?: error("PromptProvider is missing for mode=$mode")
    }

    fun consult(request: ConsultRequest): ConsultResponse {
        val prepared = prepareConsultation(request)
        val aiResponse = requestAiAdvice(request, prepared)
        val safeAiResponse = fortuneSafetyGuard.enforce(
            request = request,
            response = aiResponse,
            riskProfile = prepared.riskProfile
        )
        val calculatedStabilityScore = consultingRiskScoreCalculator.calculate(
            mode = request.mode,
            scenario = prepared.scenario,
            riskProfile = prepared.riskProfile,
            sajuFeatures = prepared.sajuFeatures
        )
        val normalizedAiResponse = consultingRiskScoreCalculator.overrideStabilityScore(
            response = safeAiResponse,
            stabilityScore = calculatedStabilityScore,
            rawJson = safeAiResponse.copy(stabilityScore = calculatedStabilityScore).toCanonicalJson(objectMapper)
        )

        val savedHistory = consultingHistoryService.saveHybridHistory(
            SaveHybridConsultingHistoryCommand(
                userId = prepared.userId,
                mode = request.mode,
                focusLabel = prepared.focusLabel,
                question = prepared.question,
                scenario = prepared.scenario,
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

    private fun requestAiAdvice(
        request: ConsultRequest,
        prepared: PreparedConsultation
    ): HybridConsultingAiResponse {
        if (request.mode != AnalysisMode.INVESTMENT_ALL) {
            return hybridConsultingAiClient.requestJsonAdvice(
                systemMessage = prepared.prompt,
                payload = prepared.payload
            )
        }

        val sectionFutures = listOf(
            AnalysisMode.INVESTMENT_SAJU,
            AnalysisMode.INVESTMENT_TAROT,
            AnalysisMode.INVESTMENT_ZODIAC
        ).map { sectionMode ->
            val sectionRequest = request.copy(mode = sectionMode)
            CompletableFuture.supplyAsync {
                hybridConsultingAiClient.requestJsonAdvice(
                    systemMessage = buildScenarioAwareSystemMessage(
                        request = sectionRequest,
                        scenario = prepared.scenario
                    ),
                    payload = buildPayload(
                        request = sectionRequest,
                        question = prepared.question,
                        scenario = prepared.scenario,
                        saju = prepared.saju,
                        sajuInvestmentFeatures = prepared.sajuFeatures,
                        zodiac = prepared.zodiac,
                        tarotReading = prepared.tarotReading,
                        riskProfile = prepared.riskProfile
                    )
                )
            }
        }
        try {
            CompletableFuture.allOf(*sectionFutures.toTypedArray()).join()
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }
        val sectionResponses = sectionFutures.map { it.get() }

        val first = sectionResponses.first()
        return first.copy(
            mode = request.mode.name,
            analysisResults = AnalysisResultsPayload(
                tarot_analysis = sectionResponses.firstOrNull { it.mode == AnalysisMode.INVESTMENT_TAROT.name }
                    ?.analysisResults
                    ?.tarot_analysis,
                saju_analysis = sectionResponses.firstOrNull { it.mode == AnalysisMode.INVESTMENT_SAJU.name }
                    ?.analysisResults
                    ?.saju_analysis,
                zodiac_analysis = sectionResponses.firstOrNull { it.mode == AnalysisMode.INVESTMENT_ZODIAC.name }
                    ?.analysisResults
                    ?.zodiac_analysis
            ),
            finalAdvice = sectionResponses
                .map { it.finalAdvice.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .take(500),
            stabilityScore = sectionResponses.map { it.stabilityScore }.average().toInt(),
            rawJson = ""
        ).let { it.copy(rawJson = it.toCanonicalJson(objectMapper)) }
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
        val userId = requireNotNull(user.id)
        val sajuInvestmentFeatures = saju?.let(sajuInvestmentFeatureService::extract)
        val payload = buildPayload(
            request = request,
            question = resolvedQuestion,
            scenario = resolvedScenario,
            saju = saju,
            sajuInvestmentFeatures = sajuInvestmentFeatures,
            zodiac = zodiacProfile,
            tarotReading = tarotReading,
            riskProfile = user.investmentRiskProfile,
            user = user
        )
        val prompt = buildScenarioAwareSystemMessage(
            request = request,
            scenario = resolvedScenario
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
        saju: SajuConsultingResult?,
        sajuInvestmentFeatures: SajuInvestmentFeatures?,
        zodiac: ZodiacConsultingProfile?,
        tarotReading: TarotReadingResult?,
        riskProfile: InvestmentRiskProfile,
        user: User? = null
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "scenario" to mapOf(
                    "code" to scenario.name,
                    "title" to scenario.title
                ),
                "question" to question,
                "userContext" to mapOf(
                    "styleHint" to riskProfile.toStyleHint(),
                    "focusLabel" to request.focusLabel?.trim()?.takeIf { it.isNotBlank() }
                ),
                "signals" to mapOf(
                    "saju" to buildSajuPayload(request.userId, saju, sajuInvestmentFeatures),
                    "zodiac" to zodiac?.toAiPayload(),
                    "birthTarotCard" to user?.let { storedBirthTarotCardPayload(it) }
                        .takeIf { request.mode.includesTarot() },
                    "tarot" to tarotReading?.toAiPayload()
                )
            )
        )

    private fun buildScenarioAwareSystemMessage(
        request: ConsultRequest,
        scenario: ConsultingScenario
    ): String =
        buildString {
            append(promptStrategyByMode.getValue(request.mode).buildSystemMessage())
            append('\n')
            append("역할: 투자 심리 운세 상담. 투자 지시, 매수/매도 단정, 수익 보장은 금지.")
            append('\n')
            append("시나리오: ")
            append(scenario.name)
            append('(')
            append(scenario.title)
            append(") - ")
            append(scenario.responseInstructionAddon())
            append('\n')
            append(request.mode.sourceBoundaryInstruction())
            append('\n')
            append("null 신호와 payload에 없는 정보는 사용하지 마라.")
            append('\n')
            append(request.mode.analysisSectionInstruction())
            append('\n')
            append("JSON only. keys=mode,saju_analysis,tarot_analysis,zodiac_analysis,overall_summary,stability_score.")
            append('\n')
            append("활성 analysis={title,content}, content 1~2문장. 비활성 analysis=null. analysis에는 userContext.styleHint를 쓰지 마라. overall_summary에서만 styleHint를 약하게 반영해 2문장 이내로 정리. 전체 500자 이내. stability_score=투자 심리 안정도 0~100.")
            append('\n')
            append(
                "말투: 주식 입문자도 바로 이해하는 생활어로, 운세 서비스답게 가볍고 유쾌하지만 명확하게 말해라. " +
                    "투자 전문 용어와 수학적 표현은 피하라. "
            )
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

    // NOTE: 현재 활성 MAIN 덱이 1개임을 전제로 동작함. 활성 덱이 여러 개가 되면 사용자 선택 덱 우선순위 처리 로직 재검토 필요.
    private fun resolveMainTarotDeckVersionId(
        requestedDeckVersionId: String?,
        fallbackDeckVersionId: String?,
        subscriptionTier: SubscriptionTier
    ): String {
        val activeDecks = tarotDeckVersionRepository.findAllByOrderByActiveDescDisplayOrderAscNameAsc()
            .filter { it.active && it.deckRole == TarotDeckRole.MAIN && subscriptionTier.ordinal >= it.requiredSubscriptionTier.ordinal }

        val preferredId = requestedDeckVersionId?.trim()?.ifBlank { null }
            ?: fallbackDeckVersionId?.trim()?.ifBlank { null }

        if (preferredId != null) {
            val preferred = activeDecks.firstOrNull { it.id == preferredId }
            if (preferred != null) return preferred.id
        }

        return activeDecks.firstOrNull()?.id ?: DEFAULT_TAROT_DECK_VERSION_ID
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
        val storedPalza = sajuResultRepository.findLatestPalzaPillarsByUserId(userId)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")
        val natalChart = saju.analysis.natalChart
        val calculatedPalza = listOf(
            natalChart.year,
            natalChart.month,
            natalChart.day,
            natalChart.hour
        ).joinToString(" ") {
            it.heavenlyStem.toKoreanCode() + it.earthlyBranch.toKoreanCode()
        }
        return saju.toAiPayload() + mapOf(
            "palza" to (storedPalza ?: calculatedPalza),
            "majorFortune" to "${saju.currentFortune.majorFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.majorFortune.pillar.earthlyBranch.toKoreanCode()}",
            "yearlyFortune" to "${saju.currentFortune.yearlyFortune.pillar.heavenlyStem.toKoreanCode()}${saju.currentFortune.yearlyFortune.pillar.earthlyBranch.toKoreanCode()}",
            "readingHints" to sajuInvestmentFeatures?.toPromptPayload()
        )
    }

    private fun storedBirthTarotCardPayload(user: User): Map<String, String?> {
        val card = user.birthTarotCardCode
            ?.let { code -> runCatching { TarotCard.fromCode(code) }.getOrNull() }
            ?: resolveBirthTarotCard(user.birthInfo.birthDate.toString())
        return mapOf(
            "name" to card.displayName,
            "meaning" to card.uprightMeaning,
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

private fun ZodiacConsultingProfile.toAiPayload(): Map<String, Any> =
    mapOf(
        "signKo" to sign.koreanName,
        "element" to sign.element,
        "moodKeyword" to sign.moodKeyword,
        "consultingAngle" to sign.consultingAngle()
    )

private fun TarotReadingResult.toAiPayload(): Map<String, Any?> =
    mapOf(
        "readingStructure" to "drawnCards 3장을 메인 근거로 보고, birthTarotCard는 성향을 보조하는 서브 카드로만 사용한다.",
        "interpretationMode" to interpretationMode.name,
        "drawnCards" to cards.map { it.toAiPayload() },
        "assistantDecks" to assistantDecks
            .map { it.toAiPayload() }
            .takeIf { it.isNotEmpty() }
    )

private fun TarotDrawGroupResult.toAiPayload(): Map<String, Any> =
    mapOf(
        "deckRole" to deckRole.name,
        "cards" to cards.map { it.toAiPayload() }
    )

private fun TarotDrawResult.toAiPayload(): Map<String, Any?> =
    mapOf(
        "selectedIndex" to index,
        "name" to card.name,
        "koreanName" to card.koreanName,
        "meaning" to card.meaning,
        "arcanaType" to card.arcanaType?.name,
        "suit" to card.suit?.name
    )

private fun InvestmentRiskProfile.toStyleHint(): String =
    when (this) {
        InvestmentRiskProfile.STABLE -> "overall_summary에서만 안정 추구형 성향을 약하게 반영한다."
        InvestmentRiskProfile.AGGRESSIVE -> "overall_summary에서만 적극 투자형 성향을 약하게 반영한다."
    }

private fun SajuInvestmentFeatures.toPromptPayload(): Map<String, Any?> =
    mapOf(
        "baseTraits" to baseTraits,
        "dynamicSignals" to dynamicSignals,
        "riskFlags" to riskFlags
    )

private fun AnalysisMode.sourceBoundaryInstruction(): String =
    when (this) {
        AnalysisMode.INVESTMENT_SAJU ->
            "근거: signals.saju가 주근거. userContext.styleHint는 overall_summary에서만 약하게 사용."
        AnalysisMode.INVESTMENT_TAROT ->
            "근거: signals.tarot.drawnCards 3장이 메인, signals.birthTarotCard는 서브. userContext.styleHint는 overall_summary에서만 약하게 사용."
        AnalysisMode.INVESTMENT_ZODIAC ->
            "근거: signals.zodiac이 주근거. element, moodKeyword, consultingAngle을 질문 상황에 직접 연결. userContext.styleHint는 overall_summary에서만 약하게 사용."
        AnalysisMode.INVESTMENT_ALL ->
            "근거: 활성 신호 전체. 사주=개인 명식, 타로=선택 3장 메인+생일 카드 서브, 별자리=별자리 기질. userContext.styleHint는 overall_summary에서만 약하게 사용."
    }

private fun AnalysisMode.analysisSectionInstruction(): String =
    when (this) {
        AnalysisMode.INVESTMENT_SAJU ->
            "활성 섹션: saju_analysis. 나머지 analysis=null."
        AnalysisMode.INVESTMENT_TAROT ->
            "활성 섹션: tarot_analysis. 나머지 analysis=null."
        AnalysisMode.INVESTMENT_ZODIAC ->
            "활성 섹션: zodiac_analysis. 나머지 analysis=null."
        AnalysisMode.INVESTMENT_ALL ->
            "활성 섹션: saju_analysis, tarot_analysis, zodiac_analysis."
    }

private fun com.hwcompany.fortune_index.domain.model.HeavenlyStem.toKoreanCode(): String = labelKo()

private fun com.hwcompany.fortune_index.domain.model.EarthlyBranch.toKoreanCode(): String = labelKo()

private fun ZodiacSign.consultingAngle(): String =
    when (this) {
        ZodiacSign.ARIES -> "질문을 빠른 반응과 첫 판단의 균형 문제로 읽는다."
        ZodiacSign.TAURUS -> "질문을 유지할 힘과 고집이 섞이는 지점으로 읽는다."
        ZodiacSign.GEMINI -> "질문을 정보 과다와 판단 전환의 리듬으로 읽는다."
        ZodiacSign.CANCER -> "질문을 불안 방어와 익숙한 선택의 영향으로 읽는다."
        ZodiacSign.LEO -> "질문을 확신, 체면, 주도권의 균형으로 읽는다."
        ZodiacSign.VIRGO -> "질문을 세부 확인과 과도한 점검 사이의 문제로 읽는다."
        ZodiacSign.LIBRA -> "질문을 비교, 균형감, 타인 분위기에 흔들리는 정도로 읽는다."
        ZodiacSign.SCORPIO -> "질문을 집중력과 집착의 경계로 읽는다."
        ZodiacSign.SAGITTARIUS -> "질문을 확장 욕구와 낙관의 속도로 읽는다."
        ZodiacSign.CAPRICORN -> "질문을 현실 기준과 장기 부담의 균형으로 읽는다."
        ZodiacSign.AQUARIUS -> "질문을 독립적 관점과 거리 두기의 힘으로 읽는다."
        ZodiacSign.PISCES -> "질문을 직감과 분위기에 휩쓸리는 정도로 읽는다."
    }
