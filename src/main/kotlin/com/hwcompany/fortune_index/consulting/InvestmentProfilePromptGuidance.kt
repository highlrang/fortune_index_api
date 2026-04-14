package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        commonGuidance(profileLabel = riskProfileLabel(riskProfile))

    private fun commonGuidance(profileLabel: String): String =
        buildString {
            append("사용자의 마음 반응 성향은 ")
            append(profileLabel)
            append("이다. ")
            append("이 차이는 무엇을 고르라고 권하는 기준이 아니라, 같은 흐름 앞에서 마음이 어떻게 흔들리는지 읽는 기준으로 반영해라. ")
            append("안정형은 변화에 더 쉽게 지치거나 움츠러들 수 있으니 속도를 늦추고 불안을 달래는 표현을 우선해라. ")
            append("직진형은 열이 오르거나 확신이 너무 빨라질 수 있으니 속도를 조절하고 마음의 과속을 경계하는 표현을 우선해라. ")
            append("두 성향 모두에게 구체적인 방법을 지시하지 말고, 감정을 가다듬고 스스로 점검하는 방향만 제안해라.")
        }

    private fun riskProfileLabel(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> "신중형"
            InvestmentRiskProfile.AGGRESSIVE -> "직진형"
        }
}
