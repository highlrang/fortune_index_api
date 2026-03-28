package com.hwcompany.fortune_index.consulting

enum class ConsultingScenario(
    val title: String,
    val description: String
) {
    TIMING_ENTRY("지금 진입해도 될까요?", "매수 타이밍"),
    TIMING_EXIT("언제 파는 게 좋을까요?", "매도/익절 타이밍"),
    SAJU_MATCH("이 종목이 내 사주와 궁합이 맞나요?", "종목 궁합"),
    RESCUE_PLAN("물려 있는데 탈출 가능할까요?", "손절/물타기 전략"),
    MENTAL_GUIDE("현재 투자 멘탈을 위한 조언이 필요해요.", "심리 가이드");

    fun focusQuestion(): String =
        when (this) {
            // Stock > Saju > Tarot
            TIMING_ENTRY -> "기술적 흐름과 운의 신호를 함께 보고 최적의 매수 진입 시점과 분할 진입 전략을 분석해줘."

            // Stock > Tarot > Saju
            TIMING_EXIT -> "현재 추세 강도와 심리 신호를 합쳐 언제 비중을 줄이거나 익절하는 게 좋은지 분석해줘."

            // Saju > Stock > Tarot
            SAJU_MATCH -> "이 종목과 관련된 시장 흐름이 내 사주 성향과 얼마나 잘 맞는지, 장기적으로 궁합이 맞는지 분석해줘."

            // Stock > Saju > Tarot
            RESCUE_PLAN -> "현재 손실 구간에서 손절, 추가매수, 보유 중 어떤 탈출 전략이 현실적인지 리스크 중심으로 분석해줘."

            // Tarot > Saju > Stock
            MENTAL_GUIDE -> "현재 투자 멘탈과 감정 기복을 진정시키는 방향으로 심리 관리와 행동 원칙을 조언해줘."
        }

    fun systemInstructionAddon(): String =
        when (this) {
            TIMING_ENTRY ->
                "overall_summary의 첫 문장에서 지금 진입 가능 여부를 명확히 답하고, 이어서 진입 조건과 분할 접근법을 직설적으로 정리해라."

            TIMING_EXIT ->
                "overall_summary의 첫 문장에서 지금 매도/익절이 유리한지 명확히 답하고, 이어서 청산 조건과 남겨둘 비중 원칙을 직설적으로 정리해라."

            SAJU_MATCH ->
                "overall_summary의 첫 문장에서 이 종목 흐름이 사용자와 궁합이 맞는지 명확히 답하고, 이어서 맞는 이유 또는 피해야 할 이유를 직설적으로 정리해라."

            RESCUE_PLAN ->
                "overall_summary의 첫 문장에서 탈출 가능성과 우선 행동을 명확히 답하고, 이어서 손절/물타기/보유 중 무엇이 더 타당한지 직설적으로 정리해라."

            MENTAL_GUIDE ->
                "overall_summary의 첫 문장에서 현재 멘탈 상태에 필요한 핵심 조언을 명확히 답하고, 이어서 당장 지켜야 할 행동 원칙을 직설적으로 정리해라."
        }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String,
    val description: String
)
