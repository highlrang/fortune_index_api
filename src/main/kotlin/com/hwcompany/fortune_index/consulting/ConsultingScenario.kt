package com.hwcompany.fortune_index.consulting

enum class ConsultingScenario(
    val title: String,
    val description: String
) {
    TIMING_ENTRY("시작", "새로운 선택을 해도 되는 때인지 알고 싶을 때"),
    TIMING_EXIT("정리", "계속 가야 할지, 한발 물러서야 할지 고민될 때"),
    SAJU_MATCH("선택", "지금 마음이 끌리는 방향이 나와 잘 맞는지 궁금할 때"),
    RESCUE_PLAN("회복", "마음이 급하거나 상황이 꼬여서 다시 균형을 찾고 싶을 때"),
    MENTAL_GUIDE("흐름", "지금 내 운세와 투자 심리 상태가 어떤지 가볍게 확인하고 싶을 때");

    fun responseInstructionAddon(): String =
        when (this) {
            TIMING_ENTRY -> "시작 가능성과 확인 기준을 말해라."
            TIMING_EXIT -> "유지할 것과 줄일 것을 말해라."
            SAJU_MATCH -> "맞는 점과 조심할 점을 말해라."
            RESCUE_PLAN -> "먼저 정리할 일과 재확인 기준을 말해라."
            MENTAL_GUIDE -> "조심할 점과 확인할 숫자를 말해라."
        }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String,
    val description: String
)
