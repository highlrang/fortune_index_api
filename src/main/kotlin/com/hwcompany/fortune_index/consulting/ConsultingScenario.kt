package com.hwcompany.fortune_index.consulting

enum class ConsultingScenario(
    val title: String,
    val description: String
) {
    TIMING_ENTRY("시작", "새로운 선택을 해도 되는 때인지 알고 싶을 때"),
    TIMING_EXIT("정리", "계속 가야 할지, 한발 물러서야 할지 고민될 때"),
    SAJU_MATCH("선택", "지금 마음이 끌리는 방향이 나와 잘 맞는지 궁금할 때"),
    RESCUE_PLAN("회복", "마음이 급하거나 상황이 꼬여서 다시 균형을 찾고 싶을 때"),
    MENTAL_GUIDE("흐름", "지금 내 운세와 상태가 어떤지 가볍게 확인하고 싶을 때");

    fun responseInstructionAddon(): String =
        when (this) {
            TIMING_ENTRY -> "이번 시나리오는 '시작'이므로, 새로운 선택을 시작해도 되는지와 그 근거에 초점을 맞춰라."
            TIMING_EXIT -> "이번 시나리오는 '정리'이므로, 계속 가는 편이 나은지 잠시 멈추는 편이 나은지에 초점을 맞춰라."
            SAJU_MATCH -> "이번 시나리오는 '선택'이므로, 이 방향이 사용자의 성향과 잘 맞는지에 초점을 맞춰라."
            RESCUE_PLAN -> "이번 시나리오는 '회복'이므로, 꼬인 상황에서 균형을 되찾는 방향과 회복 포인트에 초점을 맞춰라."
            MENTAL_GUIDE -> "이번 시나리오는 '흐름'이므로, 지금의 전체 운세와 현재 상태를 읽어주는 데 초점을 맞춰라."
        }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String,
    val description: String
)
