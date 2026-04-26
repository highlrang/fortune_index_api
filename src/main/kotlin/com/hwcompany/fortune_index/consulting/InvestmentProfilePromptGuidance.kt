package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        commonGuidance(profileLabel = riskProfileLabel(riskProfile))

    private fun commonGuidance(profileLabel: String): String =
        "사용자의 마음 성향은 ${profileLabel}이다. 판단 지시보다 감정의 속도와 흔들림만 짧게 짚어라."

    private fun riskProfileLabel(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> "신중형"
            InvestmentRiskProfile.AGGRESSIVE -> "직진형"
        }
}
