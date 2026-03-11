package com.hwcompany.fortune_index.investment

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class VirtualInvestmentProfitCalculatorTest {
    private val calculator = VirtualInvestmentProfitCalculator()

    @Test
    fun `현재 수익률과 평가 손익을 계산한다`() {
        val result = calculator.calculate(
            currentPrice = BigDecimal("12000"),
            averageBuyPrice = BigDecimal("10000"),
            quantity = 3L
        )

        assertEquals(BigDecimal("20.00"), result.currentReturnRate)
        assertEquals(BigDecimal("6000.00"), result.evaluationProfit)
    }

    @Test
    fun `매수 평단가가 0 이하이면 수익률은 0으로 처리한다`() {
        val result = calculator.calculate(
            currentPrice = BigDecimal("12000"),
            averageBuyPrice = BigDecimal.ZERO,
            quantity = 3L
        )

        assertEquals(BigDecimal.ZERO, result.currentReturnRate)
        assertEquals(BigDecimal("36000.00"), result.evaluationProfit)
    }
}
