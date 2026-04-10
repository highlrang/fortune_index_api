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
            TIMING_ENTRY -> "지금의 바깥 흐름과 운의 흐름을 함께 보고 오늘 재물의 문이 열리는지, 아니면 마음을 다지는 편이 나은지 해석해줘."

            TIMING_EXIT -> "지금의 바깥 분위기와 마음 신호를 함께 보고 지금은 밀어붙일 때인지, 한 템포 쉬어갈 때인지 해석해줘."

            SAJU_MATCH -> "지금 바라보는 흐름이 내 사주 성향과 얼마나 잘 맞는지, 재물 기운의 궁합을 중심으로 해석해줘."

            RESCUE_PLAN -> "마음이 많이 흔들리는 상태에서 감정이 어디로 치우치는지 읽고, 균형을 어떻게 지킬지 해석해줘."

            MENTAL_GUIDE -> "지금 마음의 흔들림과 감정 기복을 가라앉히는 방향으로 마음 관리와 흐름 읽기를 해줘."
        }

    fun systemInstructionAddon(): String =
        when (this) {
            TIMING_ENTRY ->
                "overall_summary의 첫 문장에서는 오늘 재물 기운이 확장 국면인지 정비 국면인지 밝히고, 이어서 마음의 속도를 어떻게 조절하면 좋을지 정리해라."

            TIMING_EXIT ->
                "overall_summary의 첫 문장에서는 지금 밀어붙이기보다 숨을 고를 때인지 밝히고, 이어서 감정 과속을 어떻게 눌러야 하는지 정리해라."

            SAJU_MATCH ->
                "overall_summary의 첫 문장에서는 지금 바라보는 흐름이 사용자와 재물 궁합이 맞는지 밝히고, 이어서 맞물리는 이유 또는 어긋나는 지점을 정리해라."

            RESCUE_PLAN ->
                "overall_summary의 첫 문장에서는 현재 불안의 결을 밝히고, 이어서 마음을 다치지 않게 지키는 태도를 정리해라."

            MENTAL_GUIDE ->
                "overall_summary의 첫 문장에서는 현재 멘탈 상태에 필요한 핵심 돌봄을 밝히고, 이어서 오늘 지켜야 할 마음 수칙을 정리해라."
        }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String,
    val description: String
)
