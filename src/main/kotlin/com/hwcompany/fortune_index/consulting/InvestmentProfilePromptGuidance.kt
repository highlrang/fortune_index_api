package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        commonGuidance(profileLabel = riskProfileLabel(riskProfile))

    fun forInvestmentStyle(investmentStyle: InvestmentStyle): String =
        commonGuidance(profileLabel = investmentStyle.description)

    private fun commonGuidance(profileLabel: String): String =
        buildString {
            append("사용자의 투자 심리 성향은 ")
            append(profileLabel)
            append("이다. ")
            append("이 차이는 추천 대상을 바꾸는 기준이 아니라, 같은 흐름 앞에서 마음이 어떻게 흔들리는지 해석하는 기준으로 반영해라. ")
            append("안정형은 변동성에 더 쉽게 지치거나 움츠러들 수 있으니 속도를 늦추고 불안을 달래는 표현을 우선해라. ")
            append("공격형은 과열과 확신 과잉으로 치우치기 쉬우니 속도를 조절하고 마음의 과속을 경계하는 표현을 우선해라. ")
            append("두 성향 모두에게 직접 실행 전략을 주지 말고, 감정 조율과 자기 점검의 방향만 제안해라.")
        }

    private fun riskProfileLabel(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> "신중형"
            InvestmentRiskProfile.AGGRESSIVE -> "직진형"
        }
}
