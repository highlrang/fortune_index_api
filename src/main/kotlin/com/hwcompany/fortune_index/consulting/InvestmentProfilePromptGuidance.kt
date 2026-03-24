package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile

internal object InvestmentProfilePromptGuidance {
    fun forRiskProfile(riskProfile: InvestmentRiskProfile): String =
        commonGuidance(profileLabel = riskProfileLabel(riskProfile))

    fun forInvestmentStyle(investmentStyle: InvestmentStyle): String =
        commonGuidance(profileLabel = investmentStyle.description)

    private fun commonGuidance(profileLabel: String): String =
        buildString {
            append("사용자 투자 성향은 ")
            append(profileLabel)
            append("이다. ")
            append("투자 성향 차이는 유망 섹터 자체를 뒤집는 기준이 아니라 실행 방식의 차이로 반영해라. ")
            append("시장/섹터 상승 신호가 강하고 전망이 좋다면 안정형과 공격형 모두 상승 가능성은 열어둬라. ")
            append("안정형이라고 해서 자동으로 익절, 관망, 보수적 결론만 내리지 마라. ")
            append("대신 안정형은 분산투자, 분할매수, 부분매수, 낮은 초기 비중, 현금 여유 유지 중심으로 설명해라. ")
            append("공격형은 집중투자, 빠른 비중 확대, 높은 허용 변동성 중심으로 설명하되 전제 조건과 손실 관리 기준을 함께 붙여라. ")
            append("같은 유망 섹터라도 안정형은 천천히 모아가는 방식, 공격형은 확신 구간에서 더 강하게 싣는 방식으로 구분해라. ")
            append("반대로 하락 위험이 큰 구간에서는 두 성향 모두 경계하되 안정형은 방어 비중을 더 높이고 공격형은 제한된 범위의 고위험 대응만 허용하는 식으로 차이를 둬라.")
        }

    private fun riskProfileLabel(riskProfile: InvestmentRiskProfile): String =
        when (riskProfile) {
            InvestmentRiskProfile.STABLE -> "안정형"
            InvestmentRiskProfile.AGGRESSIVE -> "공격형"
        }
}
