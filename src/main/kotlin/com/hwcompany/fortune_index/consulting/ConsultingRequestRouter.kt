package com.hwcompany.fortune_index.consulting

import org.springframework.stereotype.Component

@Component
class ConsultingRequestRouter {
    fun route(request: ConsultRequest, resolvedQuestion: String): ConsultingRoutingDecision {
        val normalizedQuestion = resolvedQuestion.lowercase()
        val normalizedStockCode = request.stockCode?.trim().orEmpty()
        val hasStockCode = normalizedStockCode.isNotBlank()

        val requiresPositionData = POSITION_KEYWORDS.any { normalizedQuestion.contains(it) } ||
            request.scenario == ConsultingScenario.RESCUE_PLAN
        val requiresWebSearch = NEWS_KEYWORDS.any { normalizedQuestion.contains(it) }
        val requiresMarketMoodData =
            requiresPositionData ||
                requiresWebSearch ||
                request.scenario in setOf(
                    ConsultingScenario.TIMING_ENTRY,
                    ConsultingScenario.TIMING_EXIT,
                    ConsultingScenario.RESCUE_PLAN
                ) ||
                MARKET_KEYWORDS.any { normalizedQuestion.contains(it) }
        val requiresSymbolQuote = hasStockCode && (
            requiresPositionData ||
                request.scenario in setOf(
                    ConsultingScenario.TIMING_ENTRY,
                    ConsultingScenario.TIMING_EXIT,
                    ConsultingScenario.RESCUE_PLAN
                )
            )

        return ConsultingRoutingDecision(
            isInvestmentQuery = true,
            requiresMarketMoodData = requiresMarketMoodData,
            requiresSymbolQuote = requiresSymbolQuote,
            requiresPositionData = requiresPositionData && hasStockCode,
            requiresWebSearch = requiresWebSearch,
            symbol = normalizedStockCode.ifBlank { null },
            entityName = request.stockName,
            questionType = determineQuestionType(
                question = normalizedQuestion,
                scenario = request.scenario,
                requiresPositionData = requiresPositionData,
                requiresWebSearch = requiresWebSearch
            ),
            needsFreshnessGate = requiresSymbolQuote || requiresPositionData || requiresWebSearch,
            reason = buildReason(
                requiresMarketMoodData = requiresMarketMoodData,
                requiresSymbolQuote = requiresSymbolQuote,
                requiresPositionData = requiresPositionData && hasStockCode,
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
            MARKET_KEYWORDS.any { question.contains(it) } -> "market_mood"
            else -> "general_fortune"
        }

    private fun buildReason(
        requiresMarketMoodData: Boolean,
        requiresSymbolQuote: Boolean,
        requiresPositionData: Boolean,
        requiresWebSearch: Boolean
    ): String =
        buildList {
            if (requiresMarketMoodData) add("외부 기류 해석용 시장 분위기 지표 필요")
            if (requiresSymbolQuote) add("예외적으로 개별 종목 최신 시세 확인 필요")
            if (requiresPositionData) add("보유 불안도 해석용 포지션 확인 필요")
            if (requiresWebSearch) add("최신 분위기 확인용 웹 검색 필요")
        }.joinToString(", ").ifBlank { "기본 재물 운세 해석으로 처리 가능" }

    private companion object {
        val MARKET_KEYWORDS = listOf(
            "매수", "매도", "홀딩", "보유", "물타기", "추매", "추가매수", "손절", "익절", "비중", "주가", "가격", "흐름", "진입",
            "재물", "운세", "심리", "불안", "조급", "기운"
        )
        val POSITION_KEYWORDS = listOf(
            "내 평단", "평단", "수익률", "내 기준", "내가 지금", "물려", "보유", "손실", "평균단가", "불안", "압박"
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
