package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.InvestmentRiskProfile
import kotlin.test.Test
import kotlin.test.assertContains

class InvestmentProfilePromptGuidanceTest {
    @Test
    fun `stable profile guidance keeps upside view and shifts to execution style`() {
        val guidance = InvestmentProfilePromptGuidance.forRiskProfile(InvestmentRiskProfile.STABLE)

        assertContains(guidance, "안정형이라고 해서 자동으로 익절, 관망, 보수적 결론만 내리지 마라.")
        assertContains(guidance, "시장/섹터 상승 신호가 강하고 전망이 좋다면 안정형과 공격형 모두 상승 가능성은 열어둬라.")
        assertContains(guidance, "분산투자, 분할매수, 부분매수, 낮은 초기 비중, 현금 여유 유지")
    }

    @Test
    fun `aggressive style guidance emphasizes concentration with risk controls`() {
        val guidance = InvestmentProfilePromptGuidance.forInvestmentStyle(InvestmentStyle.AGGRESSIVE)

        assertContains(guidance, "사용자 투자 성향은 공격형이다.")
        assertContains(guidance, "집중투자, 빠른 비중 확대, 높은 허용 변동성")
        assertContains(guidance, "전제 조건과 손실 관리 기준")
    }
}
