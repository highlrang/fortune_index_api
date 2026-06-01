package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.AnalysisSectionPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.ai.SafetyGuardPayload
import com.hwcompany.fortune_index.domain.model.ConsultingTone
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import org.springframework.stereotype.Component

@Component
class FortuneSafetyGuard {
    fun enforce(
        request: ConsultRequest,
        response: HybridConsultingAiResponse,
        consultingTone: ConsultingTone,
        riskProfile: InvestmentRiskProfile
    ): HybridConsultingAiResponse {
        val maskedTerms = listOfNotNull(
            request.focusLabel?.takeIf { it.isNotBlank() }
        )
        val originalTextBundle = response.toSafetyTextBundle()
        val sanitized = response.copy(
            analysisResults = AnalysisResultsPayload(
                investment_analysis = null,
                tarot_analysis = response.analysisResults.tarot_analysis?.sanitize(
                    fallbackTitle = "마음의 파동",
                    maskedTerms = maskedTerms
                ),
                saju_analysis = response.analysisResults.saju_analysis?.sanitize(
                    fallbackTitle = "투자 기질 해석",
                    maskedTerms = maskedTerms
                ),
                zodiac_analysis = response.analysisResults.zodiac_analysis?.sanitize(
                    fallbackTitle = "별자리 판단 해석",
                    maskedTerms = maskedTerms
                )
            ),
            finalAdvice = response.finalAdvice.sanitize(maskedTerms),
            rawJson = response.rawJson
        )

        val originalMatches = findForbiddenMatches(originalTextBundle)
        val sanitizedTextBundle = sanitized.toSafetyTextBundle()
        val sanitizedMatches = findForbiddenMatches(sanitizedTextBundle)

        val guarded = if (originalMatches.isNotEmpty() || sanitizedMatches.isNotEmpty()) {
            buildSafeFallback(
                request = request,
                response = sanitized,
                consultingTone = consultingTone,
                riskProfile = riskProfile,
                matchedRules = (originalMatches + sanitizedMatches).distinct(),
                originalText = originalTextBundle,
                sanitizedText = sanitizedTextBundle
            )
        } else {
            sanitized
        }

        return guarded.copy(rawJson = guarded.toCanonicalJson())
    }

    fun sanitizeFreeform(content: String): String {
        val sanitized = content.sanitize(emptyList())
        return if (containsForbiddenExpression(content) || containsForbiddenExpression(sanitized)) {
            "오늘은 숫자보다 투자 심리를 먼저 살필 때예요. 이 서비스는 주식 투자 심리 케어를 위한 안내만 제공합니다."
        } else {
            sanitized
        }
    }

    private fun HybridConsultingAiResponse.toSafetyTextBundle(): String =
        listOfNotNull(
            analysisResults.tarot_analysis?.title,
            analysisResults.tarot_analysis?.content,
            analysisResults.saju_analysis?.title,
            analysisResults.saju_analysis?.content,
            analysisResults.zodiac_analysis?.title,
            analysisResults.zodiac_analysis?.content,
            finalAdvice
        ).joinToString("\n")

    private fun buildSafeFallback(
        request: ConsultRequest,
        response: HybridConsultingAiResponse,
        consultingTone: ConsultingTone,
        riskProfile: InvestmentRiskProfile,
        matchedRules: List<String>,
        originalText: String,
        sanitizedText: String
    ): HybridConsultingAiResponse {
        val casual = consultingTone == ConsultingTone.FRIENDLY || consultingTone == ConsultingTone.WITTY_SENIOR
        val question = request.question.orEmpty()
        val sajuSection = response.analysisResults.saju_analysis?.let {
            AnalysisSectionPayload(
                title = "투자 기질 해석",
                content = buildSajuFallbackContent(
                    riskProfile = riskProfile,
                    question = question,
                    casual = casual
                )
            )
        }
        val tarotSection = response.analysisResults.tarot_analysis?.let {
            AnalysisSectionPayload(
                title = "마음의 파동",
                content = if (casual) {
                    "마음은 빨리 답을 원해도, 카드는 속도보다 원칙부터 잡으라고 해."
                } else {
                    "마음은 빨리 답을 원하지만, 카드는 속도보다 원칙부터 잡으라고 말해요."
                }
            )
        }
        val overallSummary = when (request.scenario ?: ConsultingScenario.MENTAL_GUIDE) {
            ConsultingScenario.TIMING_ENTRY ->
                if (casual) {
                    "오늘은 바로 뛰기보다 작은 기준 하나 먼저 잡는 쪽이야. 시작은 작게, 확인은 또렷하게 가."
                } else {
                    "오늘은 바로 뛰기보다 작은 기준 하나를 먼저 잡는 쪽이에요. 시작은 작게, 확인은 또렷하게 가세요."
                }
            ConsultingScenario.TIMING_EXIT ->
                if (casual) {
                    "오늘은 계속 밀기보다 덜어낼 것부터 보는 날이야. 붙잡는 이유가 흐리면 잠깐 내려놔."
                } else {
                    "오늘은 계속 밀기보다 덜어낼 것부터 보는 날이에요. 붙잡는 이유가 흐리면 잠깐 내려놓는 쪽이 낫습니다."
            }
            ConsultingScenario.SAJU_MATCH ->
                buildChoiceFallbackSummary(request, riskProfile, casual)
            ConsultingScenario.RESCUE_PLAN ->
                if (casual) {
                    "꼬였을수록 한 방 해결은 금물이야. 오늘은 손댈 일 하나만 정하고 나머지는 건드리지 마."
                } else {
                    "꼬였을수록 한 방 해결은 금물이에요. 오늘은 손댈 일 하나만 정하고 나머지는 건드리지 마세요."
                }
            ConsultingScenario.MENTAL_GUIDE ->
                if (casual) {
                    "오늘은 수익 욕심보다 네 속도가 먼저야. 보유 이유 하나만 다시 확인해도 꽤 잘한 날이야."
                } else {
                    "오늘은 수익 욕심보다 내 속도가 먼저예요. 보유 이유 하나만 다시 확인해도 꽤 잘한 날입니다."
                }
        }

        return response.copy(
            analysisResults = AnalysisResultsPayload(
                investment_analysis = null,
                tarot_analysis = tarotSection,
                saju_analysis = sajuSection,
                zodiac_analysis = response.analysisResults.zodiac_analysis?.let {
                    AnalysisSectionPayload(
                        title = "별자리 판단 해석",
                        content = if (casual) {
                            "밖의 분위기보다 네 선택 기준을 또렷하게 잡는 쪽이 더 유리해."
                        } else {
                            "밖의 분위기보다 내 선택 기준을 또렷하게 잡는 쪽이 더 유리해요."
                        }
                    )
                }
            ),
            finalAdvice = overallSummary,
            safetyGuard = SafetyGuardPayload(
                applied = true,
                reason = "forbidden_expression_detected",
                matchedRules = matchedRules,
                fallbackType = (request.scenario ?: ConsultingScenario.MENTAL_GUIDE).name,
                originalText = originalText,
                sanitizedText = sanitizedText
            )
        )
    }

    private fun buildSajuFallbackContent(
        riskProfile: InvestmentRiskProfile,
        question: String,
        casual: Boolean
    ): String {
        val content = when (riskProfile) {
            InvestmentRiskProfile.STABLE ->
                when {
                    question.containsVolatilityKeyword() ->
                        "신중형한테 변동 큰 선택은 실력보다 버티는 기준이 먼저야. 감당 가능 여부는 기대감이 아니라 손실 한도 숫자로 봐야 해."
                    question.containsPositionSizingKeyword() ->
                        "신중형은 비중을 키울수록 마음 부담이 먼저 커질 수 있어. 늘릴지보다 늘린 뒤에도 지킬 기준이 있는지가 핵심이야."
                    else ->
                        "신중형은 빠르게 맞히는 쪽보다 기준 세우고 오래 지키는 쪽이 더 편한 결이야. 오늘은 확신보다 감당 가능한 범위를 먼저 봐."
                }
            InvestmentRiskProfile.AGGRESSIVE ->
                when {
                    question.containsVolatilityKeyword() ->
                        "직진형은 변동 큰 선택에 끌릴 수 있지만, 속도가 붙으면 기준이 늦게 따라올 수 있어. 감당 가능 여부는 들어가고 싶은 마음보다 멈출 기준으로 봐야 해."
                    question.containsPositionSizingKeyword() ->
                        "직진형은 비중을 키우는 판단이 빠를 수 있어. 무리 없는지는 더 밀고 싶은 마음보다 과열됐을 때 줄일 기준이 있는지로 봐야 해."
                    else ->
                        "직진형은 판단과 실행이 빠른 결이야. 오늘은 과감함을 죽이기보다, 속도 붙기 전에 브레이크 기준 하나를 먼저 세워."
                }
        }

        return if (casual) content else content.toPoliteFallback()
    }

    private fun buildChoiceFallbackSummary(
        request: ConsultRequest,
        riskProfile: InvestmentRiskProfile,
        casual: Boolean
    ): String {
        val question = request.question.orEmpty()
        val summary = when {
            question.containsLongTermShortTermChoice() ->
                when (riskProfile) {
                    InvestmentRiskProfile.STABLE ->
                        "성향 기준으로는 장기투자 쪽이 더 맞아 보여. 단타처럼 계속 맞히는 게임보다, 기준 정하고 오래 지키는 쪽에서 덜 흔들려. 오늘은 오래 지킬 규칙 하나만 적어봐."
                    InvestmentRiskProfile.AGGRESSIVE ->
                        "성향 기준으로는 단기 판단도 가능하지만, 기준 없는 속도전은 금방 과열돼. 오늘은 들어가기 전 조건과 멈출 조건을 한 줄씩 적어봐."
                }
            question.containsBinaryChoice() ->
                when (riskProfile) {
                    InvestmentRiskProfile.STABLE ->
                        "둘 중 하나라면 오래 버틸 수 있는 쪽을 고르는 게 맞아. 잠깐 끌리는 쪽보다, 흔들릴 때도 지킬 수 있는 선택이 이겨. 오늘은 선택 기준 하나만 딱 정해."
                    InvestmentRiskProfile.AGGRESSIVE ->
                        "둘 중 하나라면 끌리는 쪽보다 기준이 선명한 쪽이 맞아. 마음이 먼저 달리기 쉬우니, 오늘은 선택 전에 브레이크 기준 하나만 딱 정해."
                }
            question.containsVolatilityKeyword() ->
                when (riskProfile) {
                    InvestmentRiskProfile.STABLE ->
                        "변동 큰 성장주는 네 성향엔 감당 기준이 먼저야. 할 수 있냐보다, 흔들릴 때도 지킬 손실 한도가 있냐가 핵심이야. 오늘은 감당 가능한 손실 한도 하나만 숫자로 적어봐."
                    InvestmentRiskProfile.AGGRESSIVE ->
                        "변동 큰 선택은 네 성향엔 끌릴 수 있지만, 속도 붙으면 과열 체크가 먼저야. 무리 없는지는 자신감보다 멈출 기준이 있냐로 봐야 해. 오늘은 들어가기 전 조건 하나랑 줄일 조건 하나를 적어봐."
                }
            question.containsPositionSizingKeyword() ->
                when (riskProfile) {
                    InvestmentRiskProfile.STABLE ->
                        "비중을 늘리는 건 네 성향엔 마음 부담까지 같이 키우는 선택이야. 무리 없는지는 확신보다 흔들릴 때 지킬 비율이 있냐로 봐야 해. 오늘은 더 늘려도 잠이 편한 한도만 숫자로 적어봐."
                    InvestmentRiskProfile.AGGRESSIVE ->
                        "비중을 늘리는 선택은 네 성향엔 빠르게 기울 수 있어. 그래서 답은 가능 여부보다 과열 방지 기준이야. 오늘은 늘리기 전 확인할 조건 하나만 먼저 적어봐."
                }
            else ->
                when (riskProfile) {
                    InvestmentRiskProfile.STABLE ->
                        "지금은 성향에 오래 맞는 쪽을 고르는 게 답이야. 급한 결정보다 계속 지킬 수 있는 기준 하나가 더 세."
                    InvestmentRiskProfile.AGGRESSIVE ->
                        "지금은 하고 싶은 쪽으로 바로 밀기보다, 속도를 잡아 줄 기준이 먼저야. 과감함은 살리되 브레이크 없는 선택은 피하는 게 맞아."
                }
        }

        return if (casual) summary else summary.toPoliteFallback()
    }

    private fun String.containsLongTermShortTermChoice(): Boolean =
        LONG_TERM_PATTERNS.any { it.containsMatchIn(this) } &&
            SHORT_TERM_PATTERNS.any { it.containsMatchIn(this) }

    private fun String.containsBinaryChoice(): Boolean =
        BINARY_CHOICE_PATTERN.containsMatchIn(this)

    private fun String.containsVolatilityKeyword(): Boolean =
        VOLATILITY_PATTERNS.any { it.containsMatchIn(this) }

    private fun String.containsPositionSizingKeyword(): Boolean =
        POSITION_SIZING_PATTERNS.any { it.containsMatchIn(this) }

    private fun String.toPoliteFallback(): String =
        this
            .replace("이야", "이에요")
            .replace("해.", "해요.")
            .replace("봐.", "봐요.")
            .replace("적어.", "적어봐요.")
            .replace("세워.", "세워봐요.")
            .replace("맞아.", "맞아요.")
            .replace("핵심이야.", "핵심이에요.")
            .replace("먼저야.", "먼저예요.")
            .replace("답이야.", "답이에요.")
            .replace("가능해.", "가능해요.")

    private fun AnalysisSectionPayload.sanitize(
        fallbackTitle: String,
        maskedTerms: List<String>
    ): AnalysisSectionPayload =
        copy(
            title = title.sanitize(maskedTerms).ifBlank { fallbackTitle },
            content = content.sanitize(maskedTerms)
        )

    private fun String.sanitize(maskedTerms: List<String>): String {
        var sanitized = this
        maskedTerms.filter { it.isNotBlank() }.forEach { term ->
            sanitized = sanitized.replace(term, "선택한 흐름", ignoreCase = true)
        }
        SANITIZE_REPLACEMENTS.forEach { (target, replacement) ->
            sanitized = sanitized.replace(target, replacement)
        }
        return sanitized.replace(Regex("\\s+"), " ").trim()
    }

    private fun containsForbiddenExpression(text: String): Boolean =
        findForbiddenMatches(text).isNotEmpty()

    private fun findForbiddenMatches(text: String): List<String> =
        FORBIDDEN_PATTERNS
            .filter { it.pattern.containsMatchIn(text) }
            .map { it.name }

    private companion object {
        data class ForbiddenPattern(
            val name: String,
            val pattern: Regex
        )

        val FORBIDDEN_PATTERNS = listOf(
            ForbiddenPattern("direct_trading_term", Regex("매수|매도|손절|익절|추매|추가매수|물타기|청산|홀딩")),
            ForbiddenPattern("direct_action_instruction", Regex("지금\\s*사|사세요|팔아|팔아야|들어가야|비중\\s*확대|비중\\s*축소")),
            ForbiddenPattern("guaranteed_return", Regex("무조건\\s*수익|원금\\s*보장|확정\\s*수익|반드시\\s*오")),
            ForbiddenPattern("professional_claim", Regex("투자\\s*전문가|투자\\s*자문|재무\\s*설계사")),
            ForbiddenPattern("target_or_recommendation", Regex("목표가|목표\\s*주가|전량\\s*매도|전량\\s*매수|반드시\\s*팔|종목\\s*추천|추천\\s*종목")),
            ForbiddenPattern("specific_price_or_return", Regex("[0-9][0-9,]*원\\s*까지|[0-9]+%\\s*(수익|상승|하락)|수익률\\s*[0-9]"))
        )

        val SANITIZE_REPLACEMENTS = listOf(
            "종목" to "관심 대상",
            "주가" to "가격 흐름",
            "시세" to "가격 흐름",
            "매매" to "투자 판단",
            "매수" to "접근",
            "매도" to "정리",
            "손절" to "급한 결론",
            "익절" to "수확의 시기",
            "추매" to "다시 힘을 싣는 일",
            "추가매수" to "다시 힘을 싣는 일",
            "물타기" to "기반 다지기",
            "청산" to "급한 정리",
            "홀딩" to "붙잡고 싶은 마음"
        )

        val LONG_TERM_PATTERNS = listOf(
            Regex("장기\\s*투자"),
            Regex("장투")
        )
        val SHORT_TERM_PATTERNS = listOf(
            Regex("단기\\s*투자"),
            Regex("단타")
        )
        val BINARY_CHOICE_PATTERN = Regex("(둘\\s*중|무엇이|뭐가|어느\\s*쪽|어떤\\s*쪽).*(맞|나아|좋|골라|선택)")
        val VOLATILITY_PATTERNS = listOf(
            Regex("변동"),
            Regex("성장주"),
            Regex("급등|급락"),
            Regex("감당")
        )
        val POSITION_SIZING_PATTERNS = listOf(
            Regex("비중"),
            Regex("늘리"),
            Regex("줄이"),
            Regex("확대|축소")
        )
    }
}
