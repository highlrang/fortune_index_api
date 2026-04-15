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
        val sanitized = response.copy(
            analysisResults = AnalysisResultsPayload(
                market_analysis = response.analysisResults.market_analysis.sanitize(
                    fallbackTitle = "외부 기류 해석",
                    maskedTerms = maskedTerms
                ),
                tarot_analysis = response.analysisResults.tarot_analysis?.sanitize(
                    fallbackTitle = "마음의 파동",
                    maskedTerms = maskedTerms
                ),
                saju_analysis = response.analysisResults.saju_analysis?.sanitize(
                    fallbackTitle = "재물 기질 해석",
                    maskedTerms = maskedTerms
                )
            ),
            finalAdvice = response.finalAdvice.sanitize(maskedTerms),
            rawJson = response.rawJson
        )

        val textBundle = listOfNotNull(
            sanitized.analysisResults.market_analysis.content,
            sanitized.analysisResults.tarot_analysis?.content,
            sanitized.analysisResults.saju_analysis?.content,
            sanitized.finalAdvice
        ).joinToString("\n")

        val guarded = if (containsForbiddenExpression(textBundle)) {
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
        return if (containsForbiddenExpression(sanitized)) {
            "오늘은 숫자보다 마음의 파동을 먼저 살필 때예요. 이 서비스는 재물 운세와 심리 케어를 위한 안내만 제공합니다."
        } else {
            sanitized
        }
    }

    private fun buildSafeFallback(
        request: ConsultRequest,
        response: HybridConsultingAiResponse
    ): HybridConsultingAiResponse {
        val marketSection = AnalysisSectionPayload(
            title = "외부 기류 해석",
            content = "오늘의 흐름은 바깥 정보보다 내 마음의 결을 먼저 살피라는 신호에 가까워요."
        )
        val sajuSection = response.analysisResults.saju_analysis?.let {
            AnalysisSectionPayload(
                title = "재물 기질 해석",
                content = "지금은 타고난 재물 감각보다 마음의 균형을 먼저 붙드는 해석이 필요한 흐름이에요."
            )
        }
        val tarotSection = response.analysisResults.tarot_analysis?.let {
            AnalysisSectionPayload(
                title = "마음의 파동",
                content = "감정이 앞서기 쉬운 날이라 카드의 신호도 결정보다 자기 점검 쪽에 무게를 둡니다."
            )
        }
        val overallSummary = when (request.scenario ?: ConsultingScenario.MENTAL_GUIDE) {
            ConsultingScenario.TIMING_ENTRY ->
                "오늘의 재물운은 문이 열리더라도 서두르기보다 호흡을 고르는 쪽에 가까워 보여요. 판단보다 마음의 속도를 먼저 다스리는 편이 좋겠습니다."
            ConsultingScenario.TIMING_EXIT ->
                "오늘은 밀어붙이기보다 한 걸음 물러서 공기를 읽는 흐름이에요. 성급한 결론보다 마음의 열기를 식히는 데 의미가 있습니다."
            ConsultingScenario.SAJU_MATCH ->
                "지금의 흐름은 재물 궁합을 따지기보다 내 마음의 결이 흔들리는 지점을 비춰 주는 쪽에 가깝습니다. 맞고 틀림보다 조율이 먼저예요."
            ConsultingScenario.RESCUE_PLAN ->
                "불안이 큰 날일수록 결정을 서두르지 않는 태도 자체가 재물운을 지키는 힘이 됩니다. 오늘은 손익보다 마음의 압박을 낮추는 데 초점을 두세요."
            ConsultingScenario.MENTAL_GUIDE ->
                "오늘의 핵심은 정답 찾기보다 마음의 파동을 가라앉히는 데 있어요. 이 해석은 운세와 심리 케어를 위한 안내로 받아들여 주세요."
            null ->
                "오늘은 재물 흐름의 정답을 단정하기보다, 질문 속에서 마음이 어디로 기울고 있는지 먼저 살피는 편이 좋겠어요. 이 해석은 운세와 심리 케어를 위한 안내로 받아들여 주세요."
        }

        return response.copy(
            analysisResults = AnalysisResultsPayload(
                market_analysis = marketSection,
                tarot_analysis = tarotSection,
                saju_analysis = sajuSection
            ),
            finalAdvice = overallSummary
        )
    }

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
            Regex("투자\\s*전문가|투자\\s*자문|재무\\s*설계사")
        )

        val SANITIZE_REPLACEMENTS = listOf(
            "종목" to "흐름",
            "주식" to "재물 흐름",
            "매매" to "판단",
            "매수" to "접근",
            "매도" to "정리",
            "손절" to "급한 결론",
            "익절" to "서둘러 거두려는 마음",
            "추매" to "다시 힘을 싣는 일",
            "추가매수" to "다시 힘을 싣는 일",
            "물타기" to "불안을 달래려는 반복 행동",
            "청산" to "급한 정리",
            "홀딩" to "붙잡고 싶은 마음"
        )
    }
}
