package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import org.springframework.stereotype.Component

@Component
class ConsultingRiskScoreCalculator {
    fun calculate(
        mode: AnalysisMode,
        scenario: ConsultingScenario,
        riskProfile: InvestmentRiskProfile
    ): Int {
        var score = 18

        score += scenarioScore(scenario)
        score += modeScore(mode)
        score += profileAdjustment(riskProfile)

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
            AnalysisMode.INVESTMENT_SAJU -> 3
            AnalysisMode.INVESTMENT_TAROT -> 3
            AnalysisMode.INVESTMENT_ZODIAC -> 2
            AnalysisMode.INVESTMENT_ALL -> 6
        }

    private fun profileAdjustment(riskProfile: InvestmentRiskProfile): Int =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> 5
            InvestmentRiskProfile.AGGRESSIVE -> -2
        }
}

internal fun HybridConsultingAiResponse.toCanonicalJson(): String {
    val analysisResults = linkedMapOf<String, Any?>(
        "investment_analysis" to analysisResults.investment_analysis.toMap(),
        "tarot_analysis" to analysisResults.tarot_analysis?.toMap(),
        "saju_analysis" to analysisResults.saju_analysis?.toMap(),
        "zodiac_analysis" to analysisResults.zodiac_analysis?.toMap()
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
