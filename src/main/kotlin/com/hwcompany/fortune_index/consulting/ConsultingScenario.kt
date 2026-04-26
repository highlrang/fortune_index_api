package com.hwcompany.fortune_index.consulting

enum class ConsultingScenario(
    val title: String,
    val description: String
) {
    TIMING_ENTRY("오늘 재물의 문이 열리는 흐름인가요?", "재물 기운 진입감"),
    TIMING_EXIT("한 걸음 물러서 마음을 정리할 때인가요?", "정비의 흐름"),
    SAJU_MATCH("지금 바라보는 흐름이 내 재물 기질과 잘 맞나요?", "재물 궁합"),
    RESCUE_PLAN("불안이 큰 구간에서 마음을 어떻게 지켜야 하나요?", "불안 회복 가이드"),
    MENTAL_GUIDE("지금 내 마음 상태를 돌보고 싶어요.", "심리 케어");

    fun focusQuestion(): String =
        when (this) {
            TIMING_ENTRY -> "오늘 진입해도 되는지 짧게 봐줘."

            TIMING_EXIT -> "지금은 쉬어갈 때인지 짧게 봐줘."

            SAJU_MATCH -> "지금 흐름과 내 성향이 맞는지 봐줘."

            RESCUE_PLAN -> "불안할 때 마음을 어떻게 지킬지 봐줘."

            MENTAL_GUIDE -> "지금 마음 상태를 짧게 정리해줘."
        }

    fun systemInstructionAddon(): String =
        when (this) {
            TIMING_ENTRY -> "overall_summary는 오늘 진입 가능성을 한 문장으로만 써라."
            TIMING_EXIT -> "overall_summary는 쉬어갈 필요를 한 문장으로만 써라."
            SAJU_MATCH -> "overall_summary는 궁합의 맞고 어긋남을 한 문장으로만 써라."
            RESCUE_PLAN -> "overall_summary는 불안을 다루는 태도를 한 문장으로만 써라."
            MENTAL_GUIDE -> "overall_summary는 마음 상태를 한 문장으로만 써라."
        }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String
)
