package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE ->
                "투자 성향=신중형. 기준 확인, 보유 이유, 손실 한도, 결정 보류의 가치를 중심으로 말해라."
            InvestmentRiskProfile.AGGRESSIVE ->
                "투자 성향=직진형. 속도 조절, 과열 체크, 진입 전 조건, 감정적 접근 방지를 중심으로 말해라."
        }
}
