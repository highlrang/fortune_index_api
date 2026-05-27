package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.AnalysisSectionPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import org.springframework.stereotype.Component

@Component
class FortuneSafetyGuard {
    fun enforce(
        request: ConsultRequest,
        response: HybridConsultingAiResponse
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
                    fallbackTitle = "재물 기질 해석",
                    maskedTerms = maskedTerms
                ),
                zodiac_analysis = response.analysisResults.zodiac_analysis?.sanitize(
                    fallbackTitle = "별자리 흐름 해석",
                    maskedTerms = maskedTerms
                )
            ),
            finalAdvice = response.finalAdvice.sanitize(maskedTerms),
            rawJson = response.rawJson
        )

        val sanitizedTextBundle = sanitized.toSafetyTextBundle()

        val guarded = if (containsForbiddenExpression(originalTextBundle) || containsForbiddenExpression(sanitizedTextBundle)) {
            buildSafeFallback(
                request = request,
                response = sanitized
            )
        } else {
            sanitized
        }

        return guarded.copy(rawJson = guarded.toCanonicalJson())
    }

    fun sanitizeFreeform(content: String): String {
        val sanitized = content.sanitize(emptyList())
        return if (containsForbiddenExpression(content) || containsForbiddenExpression(sanitized)) {
            "오늘은 숫자보다 마음의 파동을 먼저 살필 때예요. 이 서비스는 재물 운세와 심리 케어를 위한 안내만 제공합니다."
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
        response: HybridConsultingAiResponse
    ): HybridConsultingAiResponse {
        val sajuSection = response.analysisResults.saju_analysis?.let {
            AnalysisSectionPayload(
                title = "재물 기질 해석",
                content = "무리해서 빨리 맞히는 쪽보다 기준을 세우고 지키는 쪽이 더 편한 결이에요."
            )
        }
        val tarotSection = response.analysisResults.tarot_analysis?.let {
            AnalysisSectionPayload(
                title = "마음의 파동",
                content = "마음은 빨리 답을 원하지만, 카드는 속도보다 원칙부터 잡으라고 말해요."
            )
        }
        val overallSummary = when (request.scenario ?: ConsultingScenario.MENTAL_GUIDE) {
            ConsultingScenario.TIMING_ENTRY ->
                "오늘은 바로 뛰기보다 작은 기준 하나를 먼저 잡는 쪽이에요. 시작은 작게, 확인은 또렷하게 가세요."
            ConsultingScenario.TIMING_EXIT ->
                "오늘은 계속 밀기보다 덜어낼 것부터 보는 날이에요. 붙잡는 이유가 흐리면 잠깐 내려놓는 쪽이 낫습니다."
            ConsultingScenario.SAJU_MATCH ->
                buildChoiceFallbackSummary(request)
            ConsultingScenario.RESCUE_PLAN ->
                "꼬였을수록 한 방 해결은 금물이에요. 오늘은 손댈 일 하나만 정하고 나머지는 건드리지 마세요."
            ConsultingScenario.MENTAL_GUIDE ->
                "오늘은 돈보다 내 속도가 먼저예요. 충동 결제 하나만 멈춰도 꽤 잘한 날입니다."
        }

        return response.copy(
            analysisResults = AnalysisResultsPayload(
                investment_analysis = null,
                tarot_analysis = tarotSection,
                saju_analysis = sajuSection,
                zodiac_analysis = response.analysisResults.zodiac_analysis?.let {
                    AnalysisSectionPayload(
                        title = "별자리 흐름 해석",
                        content = "밖의 분위기보다 내 선택 기준을 또렷하게 잡는 쪽이 더 유리해요."
                    )
                }
            ),
            finalAdvice = overallSummary
        )
    }

    private fun buildChoiceFallbackSummary(request: ConsultRequest): String {
        val question = request.question.orEmpty()
        return when {
            question.containsLongTermShortTermChoice() ->
                "성향 기준으로는 장기투자 쪽이 더 맞아 보여요. 단타처럼 계속 맞히는 게임보다, 기준을 정하고 오래 지키는 쪽에서 덜 흔들립니다. 오늘은 오래 지킬 규칙 하나만 적어보세요."
            question.containsBinaryChoice() ->
                "둘 중 하나라면 오래 버틸 수 있는 쪽을 고르는 게 맞아요. 잠깐 끌리는 쪽보다, 흔들릴 때도 지킬 수 있는 선택이 이깁니다. 오늘은 선택 기준 하나만 딱 정하세요."
            else ->
                "지금은 성향에 오래 맞는 쪽을 고르는 게 답이에요. 급한 결정보다 계속 지킬 수 있는 기준 하나가 더 셉니다."
        }
    }

    private fun String.containsLongTermShortTermChoice(): Boolean =
        LONG_TERM_PATTERNS.any { it.containsMatchIn(this) } &&
            SHORT_TERM_PATTERNS.any { it.containsMatchIn(this) }

    private fun String.containsBinaryChoice(): Boolean =
        BINARY_CHOICE_PATTERN.containsMatchIn(this)

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
        FORBIDDEN_PATTERNS.any { it.containsMatchIn(text) }

    private companion object {
        val FORBIDDEN_PATTERNS = listOf(
            Regex("매수|매도|손절|익절|추매|추가매수|물타기|청산|홀딩"),
            Regex("지금\\s*사|사세요|팔아|팔아야|들어가야|비중\\s*확대|비중\\s*축소"),
            Regex("무조건\\s*수익|원금\\s*보장|확정\\s*수익|반드시\\s*오"),
            Regex("투자\\s*전문가|투자\\s*자문|재무\\s*설계사"),
            Regex("목표가|목표\\s*주가|전량\\s*매도|전량\\s*매수|반드시\\s*팔|종목\\s*추천|추천\\s*종목"),
            Regex("[0-9][0-9,]*원\\s*까지|[0-9]+%\\s*(수익|상승|하락)|수익률\\s*[0-9]")
        )

        val SANITIZE_REPLACEMENTS = listOf(
            "종목" to "흐름",
            "주식" to "재물 흐름",
            "주가" to "자산 흐름",
            "시세" to "흐름",
            "매매" to "투자 흐름",
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
    }
}
