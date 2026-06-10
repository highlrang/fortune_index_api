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
        var tensionScore = 18

        tensionScore += scenarioScore(scenario)
        tensionScore += modeScore(mode)
        tensionScore += profileAdjustment(riskProfile)
        tensionScore += sajuFeatureAdjustment(sajuFeatures)

        val normalizedTensionScore = tensionScore.coerceIn(10, 95)
        return 105 - normalizedTensionScore
    }

    fun overrideStabilityScore(
        response: HybridConsultingAiResponse,
        stabilityScore: Int,
        rawJson: String
    ): HybridConsultingAiResponse =
        response.copy(
            stabilityScore = stabilityScore,
            rawJson = rawJson
        )

    private fun scenarioScore(scenario: ConsultingScenario): Int =
        when (scenario) {
            ConsultingScenario.FLOW_CHECK -> 8
            ConsultingScenario.ENTRY_READY -> 7
            ConsultingScenario.HOLD_OR_EXIT -> 6
            ConsultingScenario.MENTAL_CARE -> 16
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
            InvestmentRiskProfile.STABLE -> 2
            InvestmentRiskProfile.AGGRESSIVE -> 2
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
        "stability_score" to stabilityScore,
        "safety_guard" to safetyGuard
    )

    return com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(payload)
}

private fun com.hwcompany.fortune_index.ai.AnalysisSectionPayload.toMap(): Map<String, String> =
    mapOf(
        "title" to title,
        "content" to content
    )
