package com.hwcompany.fortune_index.consulting

import org.springframework.stereotype.Component

@Component
class ConsultingRequestRouter {
    fun route(request: ConsultRequest, resolvedQuestion: String): ConsultingRoutingDecision {
        val normalizedQuestion = resolvedQuestion.lowercase()
        val requiresPositionData = false
        val requiresWebSearch = NEWS_KEYWORDS.any { normalizedQuestion.contains(it) }

        return ConsultingRoutingDecision(
            isInvestmentQuery = true,
            requiresMarketMoodData = false,
            requiresSymbolQuote = false,
            requiresPositionData = false,
            requiresWebSearch = requiresWebSearch,
            symbol = null,
            entityName = request.focusLabel,
            questionType = determineQuestionType(
                question = normalizedQuestion,
                scenario = request.scenario,
                requiresPositionData = requiresPositionData,
                requiresWebSearch = requiresWebSearch
            ),
            needsFreshnessGate = requiresWebSearch,
            reason = buildReason(
                requiresMarketMoodData = false,
                requiresSymbolQuote = false,
                requiresPositionData = false,
                requiresWebSearch = requiresWebSearch
            )
        )
    }

    private fun determineQuestionType(
        question: String,
        scenario: ConsultingScenario,
        requiresPositionData: Boolean,
        requiresWebSearch: Boolean
    ): String =
        when {
            requiresPositionData -> "emotional_burden"
            requiresWebSearch -> "external_atmosphere"
            scenario == ConsultingScenario.TIMING_ENTRY -> "wealth_opening"
            scenario == ConsultingScenario.TIMING_EXIT -> "cooling_phase"
            scenario == ConsultingScenario.SAJU_MATCH -> "fortune_alignment"
            scenario == ConsultingScenario.MENTAL_GUIDE -> "mental_care"
            FORTUNE_KEYWORDS.any { question.contains(it) } -> "fortune_flow"
            else -> "general_fortune"
        }

    private fun buildReason(
        requiresMarketMoodData: Boolean,
        requiresSymbolQuote: Boolean,
        requiresPositionData: Boolean,
        requiresWebSearch: Boolean
    ): String =
        buildList {
            if (requiresPositionData) add("보유 불안도 해석용 포지션 확인 필요")
            if (requiresWebSearch) add("최신 분위기 확인용 웹 검색 필요")
        }.joinToString(", ").ifBlank { "복잡한 숫자 정보 없이 기본 재물 운세 해석으로 처리 가능" }

    private companion object {
        val FORTUNE_KEYWORDS = listOf(
            "매수", "매도", "홀딩", "보유", "물타기", "추매", "추가매수", "손절", "익절", "비중", "주가", "가격", "흐름", "진입",
            "재물", "운세", "심리", "불안", "조급", "기운"
        )
        val NEWS_KEYWORDS = listOf(
            "오늘", "최근", "이슈", "뉴스", "왜 떨어", "왜 오르", "공시", "실적", "기사", "동향"
        )
    }
}

data class ConsultingRoutingDecision(
    val isInvestmentQuery: Boolean,
    val requiresMarketMoodData: Boolean,
    val requiresSymbolQuote: Boolean,
    val requiresPositionData: Boolean,
    val requiresWebSearch: Boolean,
    val symbol: String?,
    val entityName: String?,
    val questionType: String,
    val needsFreshnessGate: Boolean,
    val reason: String
)
