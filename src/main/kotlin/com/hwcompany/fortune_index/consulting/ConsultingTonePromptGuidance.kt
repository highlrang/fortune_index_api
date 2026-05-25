package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.ConsultingTone

internal object ConsultingTonePromptGuidance {
    fun forTone(tone: ConsultingTone): String =
        when (tone) {
            ConsultingTone.FRIENDLY ->
                "말투는 젊고 톡톡 튀는 반말로 써라. 문장은 짧고 명확하게 끝내라. 예: 괜찮아, 오늘 큰 결정은 패스. 자동결제 하나만 먼저 봐."
            ConsultingTone.POLITE ->
                "말투는 밝고 가벼운 해요체로 써라. 문장은 짧고 명확하게 끝내라. 예: 괜찮아요, 오늘 큰 결정은 패스해도 돼요. 자동결제 하나만 먼저 봐요."
            ConsultingTone.WITTY_SENIOR ->
                "말투는 센스 있고 유쾌한 돈 상담 선배처럼 써라. 짧고 명확하게 말하고, 장난은 한 끗만 넣어라. 예: 괜찮아, 지갑이랑 기싸움은 그만. 자동결제 하나만 봐."
        }
}
