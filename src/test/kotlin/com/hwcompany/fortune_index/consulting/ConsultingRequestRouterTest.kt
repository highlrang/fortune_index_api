package com.hwcompany.fortune_index.consulting

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ConsultingRequestRouterTest {
    private val router = ConsultingRequestRouter()

    @Test
    fun `routes market and web search for latest issue question`() {
        val request = ConsultRequest(
            userId = 1L,
            mode = AnalysisMode.ONLY_STOCK,
            scenario = ConsultingScenario.RESCUE_PLAN,
            stockName = "SK하이닉스",
            stockCode = "000660",
            question = "하이닉스 왜 떨어져? 최근 뉴스 반영해서 홀딩이 맞는지 알려줘"
        )

        val decision = router.route(request, requireNotNull(request.question))

        assertTrue(decision.requiresMarketData)
        assertTrue(decision.requiresWebSearch)
        assertTrue(decision.requiresPositionData)
        assertEquals("000660", decision.symbol)
    }

    @Test
    fun `routes only position data for average buy price question`() {
        val request = ConsultRequest(
            userId = 1L,
            mode = AnalysisMode.ONLY_STOCK,
            scenario = ConsultingScenario.MENTAL_GUIDE,
            stockName = "SK하이닉스",
            stockCode = "000660",
            question = "내 평단 기준으로 지금 보유 전략만 보고 싶어"
        )

        val decision = router.route(request, requireNotNull(request.question))

        assertTrue(decision.requiresPositionData)
        assertFalse(decision.requiresWebSearch)
        assertFalse(decision.questionType == "general_chat")
    }
}
