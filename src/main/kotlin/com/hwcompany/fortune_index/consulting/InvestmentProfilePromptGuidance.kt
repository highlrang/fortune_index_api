package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        commonGuidance(profileLabel = riskProfileLabel(riskProfile))

    private fun commonGuidance(profileLabel: String): String =
        "사용자의 마음 성향은 ${profileLabel}이다. 판단 지시는 피하되, 감정의 흔들림만 말하지 말고 더 대담하게 움직이고 싶은 욕구와 스스로 용기를 확인하려는 마음도 짧게 짚어라."

    private fun riskProfileLabel(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> "신중형"
            InvestmentRiskProfile.AGGRESSIVE -> "직진형"
        }
}
