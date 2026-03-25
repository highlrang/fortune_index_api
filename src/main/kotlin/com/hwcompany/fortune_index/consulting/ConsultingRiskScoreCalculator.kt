package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.market.StockInfo
import kotlin.math.abs
import org.springframework.stereotype.Component

@Component
class ConsultingRiskScoreCalculator {
    fun calculate(
        mode: AnalysisMode,
        scenario: ConsultingScenario,
        stockInfo: StockInfo,
        riskProfile: InvestmentRiskProfile
    ): Int {
        var score = 18

        score += marketVolatilityScore(stockInfo)
        score += dataReliabilityScore(stockInfo)
        score += scenarioScore(scenario)
        score += modeScore(mode)
        score += profileAdjustment(riskProfile, stockInfo)

        return score.coerceIn(10, 95)
    }

    fun overrideRiskScore(
        response: HybridConsultingAiResponse,
        riskScore: Int,
        rawJson: String
    ): HybridConsultingAiResponse =
        response.copy(
            riskScore = riskScore,
            rawJson = rawJson
        )

    private fun marketVolatilityScore(stockInfo: StockInfo): Int {
        val changeRate = abs(stockInfo.changeRate.toDouble())
        return when {
            changeRate >= 7.0 -> 18
            changeRate >= 4.0 -> 12
            changeRate >= 2.0 -> 7
            changeRate >= 1.0 -> 3
            else -> 0
        }
    }

    private fun dataReliabilityScore(stockInfo: StockInfo): Int =
        if (stockInfo.fallback) 12 else 0

    private fun scenarioScore(scenario: ConsultingScenario): Int =
        when (scenario) {
            ConsultingScenario.TIMING_ENTRY -> 7
            ConsultingScenario.TIMING_EXIT -> 5
            ConsultingScenario.SAJU_MATCH -> 4
            ConsultingScenario.RESCUE_PLAN -> 16
            ConsultingScenario.MENTAL_GUIDE -> 9
        }

    private fun modeScore(mode: AnalysisMode): Int =
        when (mode) {
            AnalysisMode.ONLY_STOCK -> 0
            AnalysisMode.STOCK_SAJU -> 3
            AnalysisMode.STOCK_TAROT -> 3
            AnalysisMode.STOCK_ALL -> 6
        }

    private fun profileAdjustment(
        riskProfile: InvestmentRiskProfile,
        stockInfo: StockInfo
    ): Int {
        val changeRate = abs(stockInfo.changeRate.toDouble())
        return when (riskProfile) {
            InvestmentRiskProfile.STABLE -> if (changeRate >= 2.0) 8 else 5
            InvestmentRiskProfile.AGGRESSIVE -> if (changeRate >= 4.0) 2 else -2
        }
    }
}

internal fun HybridConsultingAiResponse.toCanonicalJson(): String {
    val analysisResults = linkedMapOf<String, Any?>(
        "market_analysis" to analysisResults.market_analysis.toMap(),
        "tarot_analysis" to analysisResults.tarot_analysis?.toMap(),
        "saju_analysis" to analysisResults.saju_analysis?.toMap()
    )

    val payload = linkedMapOf<String, Any?>(
        "mode" to mode,
        "analysis_results" to analysisResults,
        "overall_summary" to finalAdvice,
        "risk_score" to riskScore
    )

    return com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(payload)
}

private fun com.hwcompany.fortune_index.ai.AnalysisSectionPayload.toMap(): Map<String, String> =
    mapOf(
        "title" to title,
        "content" to content
    )
