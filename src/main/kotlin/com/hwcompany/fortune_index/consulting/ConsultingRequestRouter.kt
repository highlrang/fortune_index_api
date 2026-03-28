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
        val requiresMarketData = hasStockCode && (
            requiresPositionData ||
                requiresWebSearch ||
                request.scenario in setOf(
                    ConsultingScenario.TIMING_ENTRY,
                    ConsultingScenario.TIMING_EXIT,
                    ConsultingScenario.RESCUE_PLAN
                ) ||
                MARKET_KEYWORDS.any { normalizedQuestion.contains(it) }
            )

        return ConsultingRoutingDecision(
            isInvestmentQuery = true,
            requiresMarketData = requiresMarketData,
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
            needsFreshnessGate = requiresMarketData || requiresPositionData || requiresWebSearch,
            reason = buildReason(
                requiresMarketData = requiresMarketData,
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
            requiresPositionData -> "position_decision"
            requiresWebSearch -> "news_sensitive"
            scenario == ConsultingScenario.TIMING_ENTRY -> "entry_timing"
            scenario == ConsultingScenario.TIMING_EXIT -> "exit_timing"
            scenario == ConsultingScenario.SAJU_MATCH -> "saju_match"
            scenario == ConsultingScenario.MENTAL_GUIDE -> "mental_guide"
            MARKET_KEYWORDS.any { question.contains(it) } -> "price_outlook"
            else -> "general_investment"
        }

    private fun buildReason(
        requiresMarketData: Boolean,
        requiresPositionData: Boolean,
        requiresWebSearch: Boolean
    ): String =
        buildList {
            if (requiresMarketData) add("KIS 최신 시세 필요")
            if (requiresPositionData) add("사용자 포지션 재조회 필요")
            if (requiresWebSearch) add("최신 뉴스 검색 필요")
        }.joinToString(", ").ifBlank { "투자 상담이지만 추가 실시간 데이터 조회는 선택적" }

    private companion object {
        val MARKET_KEYWORDS = listOf(
            "매수", "매도", "홀딩", "보유", "물타기", "추매", "추가매수", "손절", "익절", "비중", "주가", "가격", "흐름", "진입"
        )
        val POSITION_KEYWORDS = listOf(
            "내 평단", "평단", "수익률", "내 기준", "내가 지금", "물려", "보유", "손실", "평균단가"
        )
        val NEWS_KEYWORDS = listOf(
            "오늘", "최근", "이슈", "뉴스", "왜 떨어", "왜 오르", "공시", "실적", "기사", "동향"
        )
    }
}

data class ConsultingRoutingDecision(
    val isInvestmentQuery: Boolean,
    val requiresMarketData: Boolean,
    val requiresPositionData: Boolean,
    val requiresWebSearch: Boolean,
    val symbol: String?,
    val entityName: String?,
    val questionType: String,
    val needsFreshnessGate: Boolean,
    val reason: String
)
