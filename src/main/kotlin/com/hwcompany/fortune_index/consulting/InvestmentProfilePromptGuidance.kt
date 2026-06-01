package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE ->
                "사용자의 투자 성향은 신중형이다. 신중함을 겁이나 무능함으로 해석하지 마라. 확신이 늦게 오는 타입으로 보고, 오늘 조언은 진입보다 기준 확인, 보유 이유 점검, 손실 한도 확인, 결정 보류의 가치에 초점을 둬라. 단순히 조심하라고 하지 말고, 사용자가 납득할 수 있는 작은 확인 행동 하나를 제안해라."
            InvestmentRiskProfile.AGGRESSIVE ->
                "사용자의 투자 성향은 직진형이다. 과감함을 무조건 위험으로 몰지 마라. 판단과 실행이 빠른 타입으로 보고, 오늘 조언은 속도 조절, 과열 체크, 진입 전 조건 확인, 감정 매수 방지에 초점을 둬라. 단순히 멈추라고 하지 말고, 사용자가 바로 적용할 수 있는 브레이크 기준 하나를 제안해라."
        }
}
