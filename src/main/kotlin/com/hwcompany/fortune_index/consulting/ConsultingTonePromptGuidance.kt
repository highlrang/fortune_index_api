package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.ConsultingTone

internal object ConsultingTonePromptGuidance {
    fun forTone(tone: ConsultingTone): String =
        when (tone) {
            ConsultingTone.FRIENDLY ->
                "말투는 유쾌하고 직설적인 젊은 친구처럼 친근한 반말로만 써라. 상담사, 선생님, 어른, 멘토처럼 점잖게 말하지 마라. 짧은 리액션과 쉬운 말을 써라. 해요체, 합니다체, 존댓말, 높임말은 절대 쓰지 마라. 예: 오케이, 오늘은 큰 결정 패스. 보유 이유 하나만 먼저 다시 보자."
            ConsultingTone.POLITE ->
                "말투는 밝고 가벼운 해요체로만 써라. 반말과 딱딱한 합니다체는 쓰지 마라. 문장은 짧고 명확하게 끝내라. 예: 괜찮아요, 오늘 큰 결정은 패스해도 돼요. 보유 이유 하나만 먼저 다시 봐요."
            ConsultingTone.WITTY_SENIOR ->
                "말투는 센스 있고 유쾌한 투자 상담 선배처럼 반말로 써라. 해요체, 합니다체, 존댓말, 높임말은 절대 쓰지 마라. 짧고 명확하게 말하고, 장난은 한 끗만 넣어라. 예: 괜찮아, 마음이랑 기싸움은 그만. 보유 이유 하나만 다시 봐."
        }
}
