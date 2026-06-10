package com.hwcompany.fortune_index.consulting

import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.ai.AnalysisResultsPayload
import com.hwcompany.fortune_index.ai.HybridConsultingAiResponse
import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import com.hwcompany.fortune_index.saju.investment.SajuInvestmentFeatures
import org.springframework.stereotype.Component

@Component
class ConsultingRiskScoreCalculator(
    private val objectMapper: ObjectMapper
) {
    fun calculate(
        mode: AnalysisMode,
        scenario: ConsultingScenario,
        riskProfile: InvestmentRiskProfile,
        sajuFeatures: SajuInvestmentFeatures? = null
    ): Int {
        val divinationScore = computeDivinationScore(sajuFeatures)
        val profileMultiplier = profileMultiplier(riskProfile)
        val synergyBonus = if (mode == AnalysisMode.INVESTMENT_ALL) synergy(sajuFeatures) else 0

        val base = 50 + (divinationScore * profileMultiplier).toInt() + synergyBonus
        val withScenario = applyScenarioOffset(base, scenario)

        return withScenario.coerceIn(0, 100)
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

    private fun computeDivinationScore(sajuFeatures: SajuInvestmentFeatures?): Int {
        if (sajuFeatures == null) return 0
        var score = 0

        if ("transition_signal" in sajuFeatures.dynamicSignals) score += 8
        if (sajuFeatures.branchStageCounts.wangji >= 1) score += 6
        if (sajuFeatures.branchStageCounts.saengji >= 1) score += 4
        if ("conviction_strong" in sajuFeatures.baseTraits) score += 3
        if ("style_consistent" in sajuFeatures.baseTraits) score += 2
        if ("trend_sensitive" in sajuFeatures.baseTraits) score += 2

        if ("self_conflict" in sajuFeatures.riskFlags) score -= 10
        if ("volatility_risk" in sajuFeatures.riskFlags) score -= 8
        if ("asset_locking" in sajuFeatures.riskFlags) score -= 5
        if ("conviction_overheat" in sajuFeatures.riskFlags) score -= 4
        if ("flow_pressure_risk" in sajuFeatures.riskFlags) score -= 4
        if ("flow_disruption_risk" in sajuFeatures.riskFlags) score -= 3
        if ("flow_obstruction_risk" in sajuFeatures.riskFlags) score -= 3
        if ("pressure_signal" in sajuFeatures.dynamicSignals) score -= 3
        if ("obstruction_signal" in sajuFeatures.dynamicSignals) score -= 3
        if ("change_signal" in sajuFeatures.dynamicSignals) score -= 2
        if (sajuFeatures.branchStageCounts.myoji >= 1) score -= 3

        return score.coerceIn(-30, 30)
    }

    private fun profileMultiplier(riskProfile: InvestmentRiskProfile): Double =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> 0.7
            InvestmentRiskProfile.AGGRESSIVE -> 1.3
        }

    private fun applyScenarioOffset(base: Int, scenario: ConsultingScenario): Int =
        when (scenario) {
            ConsultingScenario.FLOW_CHECK -> base
            ConsultingScenario.ENTRY_READY -> base + 5
            ConsultingScenario.HOLD_OR_EXIT -> base - 5
            ConsultingScenario.MENTAL_CARE -> base.coerceAtLeast(40)
        }

    private fun synergy(sajuFeatures: SajuInvestmentFeatures?): Int {
        if (sajuFeatures == null) return 0
        var bonus = 0
        if (positiveSignalCount(sajuFeatures) >= 2) bonus += 5
        if (negativeSignalCount(sajuFeatures) >= 2) bonus -= 5
        return bonus
    }

    private fun positiveSignalCount(sajuFeatures: SajuInvestmentFeatures): Int {
        var count = 0
        if ("transition_signal" in sajuFeatures.dynamicSignals) count++
        if (sajuFeatures.branchStageCounts.wangji >= 1) count++
        if (sajuFeatures.branchStageCounts.saengji >= 1) count++
        if ("conviction_strong" in sajuFeatures.baseTraits) count++
        if ("style_consistent" in sajuFeatures.baseTraits) count++
        if ("trend_sensitive" in sajuFeatures.baseTraits) count++
        return count
    }

    private fun negativeSignalCount(sajuFeatures: SajuInvestmentFeatures): Int {
        var count = 0
        if ("self_conflict" in sajuFeatures.riskFlags) count++
        if ("volatility_risk" in sajuFeatures.riskFlags) count++
        if ("asset_locking" in sajuFeatures.riskFlags) count++
        if ("conviction_overheat" in sajuFeatures.riskFlags) count++
        if ("flow_pressure_risk" in sajuFeatures.riskFlags) count++
        if ("flow_disruption_risk" in sajuFeatures.riskFlags) count++
        if ("flow_obstruction_risk" in sajuFeatures.riskFlags) count++
        if ("pressure_signal" in sajuFeatures.dynamicSignals) count++
        if ("obstruction_signal" in sajuFeatures.dynamicSignals) count++
        if ("change_signal" in sajuFeatures.dynamicSignals) count++
        if (sajuFeatures.branchStageCounts.myoji >= 1) count++
        return count
    }
}

internal fun HybridConsultingAiResponse.toCanonicalJson(objectMapper: ObjectMapper): String {
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

    return objectMapper.writeValueAsString(payload)
}

private fun com.hwcompany.fortune_index.ai.AnalysisSectionPayload.toMap(): Map<String, String> =
    mapOf(
        "title" to title,
        "content" to content
    )
