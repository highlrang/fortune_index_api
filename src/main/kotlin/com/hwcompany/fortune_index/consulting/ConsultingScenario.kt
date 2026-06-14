package com.hwcompany.fortune_index.consulting

enum class ConsultingScenario(
    val title: String,
    val description: String
) {
    FLOW_CHECK("오늘의 흐름", "오늘 내 재물운과 투자 심리 흐름을 가볍게 확인하고 싶을 때"),
    ENTRY_READY("진입할까?", "새로운 선택 앞에서 지금 움직여도 될지 가늠하고 싶을 때"),
    HOLD_OR_EXIT("버틸까 나올까", "이미 물렸거나 수익 중일 때, 계속 쥘지 놓을지 고민될 때"),
    MENTAL_CARE("손실과 멘탈 관리", "FOMO, 불안감, 혹은 손실 직후 다시 균형을 찾고 싶을 때");

    fun responseInstructionAddon(): String =
        when (this) {
            FLOW_CHECK -> "오늘의 재물운 흐름과 특히 조심해야 할 투자 심리 패턴을 명리학(또는 타로) 관점에서 빗대어 말해라."
            ENTRY_READY -> "운세 흐름상 지금 진입해도 좋은 시기인지, 더 지켜봐야 할 신호가 있는지 조언해라."
            HOLD_OR_EXIT -> "현재 시점에서 선택을 유지했을 때의 이점과, 당장 내려놓거나 손절해야 할 운명적 신호를 나누어 말해라."
            MENTAL_CARE -> "흔들리는 감정의 원인을 짚어주고, 잃어버린 페이스를 되찾기 위해 지금 당장 멈춰야 할 행동을 말해라."
        }

    fun crossValidationInstruction(): String {
        val priorityRule = when (this) {
            FLOW_CHECK, MENTAL_CARE ->
                "우선순위: 타로·별자리를 메인 방향성으로, 사주는 백그라운드 기운으로 해석하라."
            ENTRY_READY, HOLD_OR_EXIT ->
                "우선순위: 사주를 메인 방향성으로, 타로·별자리는 단기 리스크 주의점으로 해석하라."
        }
        return "[교차 검증] $priorityRule " +
            "충돌 융합: 2긍정+1부정→'전반 흐름은 좋으나 부정 지표의 리스크를 주의하면 기회 포착 가능', " +
            "2부정+1긍정→'리스크가 크므로 방어 우선, 긍정 조건 만족 시에만 제한적 행동', " +
            "1긍정+1부정+1혼합→행동 유보·현금 관망·포트폴리오 재점검 최우선."
    }
}

data class ConsultingScenarioOptionResponse(
    val code: String,
    val title: String,
    val description: String
)
