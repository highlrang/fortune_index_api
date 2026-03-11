package com.hwcompany.fortune_index.investment

import java.math.BigDecimal
import java.math.RoundingMode
import org.springframework.stereotype.Component

@Component
class VirtualInvestmentProfitCalculator {
    fun calculate(currentPrice: BigDecimal, averageBuyPrice: BigDecimal, quantity: Long): VirtualInvestmentProfitResult {
        val returnRate = if (averageBuyPrice.signum() <= 0) {
            BigDecimal.ZERO
        } else {
            currentPrice.subtract(averageBuyPrice)
                .multiply(HUNDRED)
                .divide(averageBuyPrice, 2, RoundingMode.HALF_UP)
        }

        val evaluationProfit = currentPrice.subtract(averageBuyPrice)
            .multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP)

        return VirtualInvestmentProfitResult(
            currentReturnRate = returnRate,
            evaluationProfit = evaluationProfit
        )
    }

    private companion object {
        val HUNDRED: BigDecimal = BigDecimal("100")
    }
}

data class VirtualInvestmentProfitResult(
    val currentReturnRate: BigDecimal,
    val evaluationProfit: BigDecimal
)
