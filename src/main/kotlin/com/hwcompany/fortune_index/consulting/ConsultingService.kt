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
            response = aiResponse,
            consultingTone = prepared.consultingTone,
            riskProfile = prepared.riskProfile
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
            consultingTone = user.consultingTone,
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
            consultingTone = user.consultingTone,
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
        consultingTone: ConsultingTone,
        homeSummary: HomeSummaryResponse
    ): JsonNode =
        objectMapper.valueToTree(
            linkedMapOf<String, Any?>(
                "mode" to request.mode.name,
                "scenario" to scenario.title,
                "question" to question,
                "userProfile" to riskProfile.toKoreanLabel(),
                "consultingTone" to mapOf(
                    "code" to consultingTone.name
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
            append(InvestmentProfilePromptGuidance.forRiskProfile(riskProfile))
            append('\n')
            append("시나리오=${scenario.name}(${scenario.title}): ")
            append(scenario.responseInstructionAddon())
            append('\n')
            append("질문: ")
            append(question)
            append('\n')
            append("payload 확정 값만 근거로 질문에 직접 답해라. 없는 사주, 카드, 별자리, 오늘 흐름은 만들지 마라.")
            append('\n')
            if (request.mode.includesSaju()) {
                append(SajuYongshinPromptGuidance.build())
                append('\n')
                if (sajuInvestmentFeatures != null) {
                    append("investmentFeatures는 성향, 심리, 변동성, 리밸런싱 보조 신호로만 써라.")
                    append('\n')
                }
            }
            append("투자 지시, 결과 보장, 장황한 설명, 훈계는 금지다. 사거나 팔라고 지시하지 마라.")
            append('\n')
            append("말투: ")
            append(ConsultingTonePromptGuidance.forTone(consultingTone))
            append('\n')
            append("평평한 JSON만 반환해라. 키는 mode, saju_analysis, tarot_analysis, zodiac_analysis, overall_summary, risk_score만 사용해라.")
            append('\n')
            append("활성 analysis는 1~2문장, 비활성 analysis는 null이다. overall_summary는 2~3문장이고 첫 문장에서 기다림/유지/덜어내기 중 무게를 말해라.")
            append('\n')
            append("A/B 질문은 첫 문장에서 하나를 골라라. 마지막 문장은 오늘 할 작은 점검 행동으로 끝내라. 전체 650자 이내. risk_score는 안정도 0~100이다.")
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
    val consultingTone: ConsultingTone,
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
