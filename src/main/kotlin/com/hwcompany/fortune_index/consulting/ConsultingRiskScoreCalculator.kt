package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatures
import org.springframework.stereotype.Component

@Component
class ConsultingRiskScoreCalculator {
    fun calculate(
        mode: AnalysisMode,
        scenario: ConsultingScenario,
        riskProfile: InvestmentRiskProfile,
        sajuFeatures: SajuInvestmentFeatures? = null
    ): Int {
        var score = 18

        score += scenarioScore(scenario)
        score += modeScore(mode)
        score += profileAdjustment(riskProfile)
        score += sajuFeatureAdjustment(sajuFeatures)

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

    private fun sajuFeatureAdjustment(sajuFeatures: SajuInvestmentFeatures?): Int {
        if (sajuFeatures == null) return 0

        var adjustment = 0
        if ("self_conflict" in sajuFeatures.riskFlags) adjustment += 10
        if ("volatility_risk" in sajuFeatures.riskFlags) adjustment += 8
        if ("asset_locking" in sajuFeatures.riskFlags) adjustment += 5
        if ("conviction_overheat" in sajuFeatures.riskFlags) adjustment += 4
        if ("transition_signal" in sajuFeatures.dynamicSignals) adjustment += 3
        if ("execution_fast" in sajuFeatures.baseTraits) adjustment += 2

        return adjustment
    }
}

internal fun HybridConsultingAiResponse.toCanonicalJson(): String {
    val analysisResults = linkedMapOf<String, Any?>(
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
