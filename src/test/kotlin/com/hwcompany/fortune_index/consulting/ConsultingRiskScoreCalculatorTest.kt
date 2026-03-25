package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.market.MarketDataProvider
import com.hwcompany.fortune_index.market.StockInfo
import java.math.BigDecimal
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ConsultingRiskScoreCalculatorTest {
    private val calculator = ConsultingRiskScoreCalculator()

    @Test
    fun `rescue plan with fallback data scores higher than normal entry consulting`() {
        val lowRiskScore = calculator.calculate(
            mode = AnalysisMode.ONLY_STOCK,
            scenario = ConsultingScenario.TIMING_ENTRY,
            stockInfo = stockInfo(changeRate = "0.8", fallback = false),
            riskProfile = InvestmentRiskProfile.AGGRESSIVE
        )
        val highRiskScore = calculator.calculate(
            mode = AnalysisMode.STOCK_ALL,
            scenario = ConsultingScenario.RESCUE_PLAN,
            stockInfo = stockInfo(changeRate = "5.3", fallback = true),
            riskProfile = InvestmentRiskProfile.STABLE
        )

        assertTrue(highRiskScore > lowRiskScore)
    }

    @Test
    fun `stable investor receives more conservative score than aggressive investor`() {
        val stableScore = calculator.calculate(
            mode = AnalysisMode.ONLY_STOCK,
            scenario = ConsultingScenario.TIMING_ENTRY,
            stockInfo = stockInfo(changeRate = "2.4", fallback = false),
            riskProfile = InvestmentRiskProfile.STABLE
        )
        val aggressiveScore = calculator.calculate(
            mode = AnalysisMode.ONLY_STOCK,
            scenario = ConsultingScenario.TIMING_ENTRY,
            stockInfo = stockInfo(changeRate = "2.4", fallback = false),
            riskProfile = InvestmentRiskProfile.AGGRESSIVE
        )

        assertTrue(stableScore > aggressiveScore)
    }

    @Test
    fun `score stays within configured bounds`() {
        val score = calculator.calculate(
            mode = AnalysisMode.STOCK_ALL,
            scenario = ConsultingScenario.RESCUE_PLAN,
            stockInfo = stockInfo(changeRate = "12.0", fallback = true),
            riskProfile = InvestmentRiskProfile.STABLE
        )

        assertTrue(score in 10..95)
    }

    private fun stockInfo(changeRate: String, fallback: Boolean): StockInfo =
        StockInfo(
            ticker = "테스트 종목",
            currentPrice = BigDecimal.ZERO,
            changeRate = BigDecimal(changeRate),
            sector = "TECHNOLOGY",
            source = MarketDataProvider.KIS,
            fallback = fallback
        )
}
