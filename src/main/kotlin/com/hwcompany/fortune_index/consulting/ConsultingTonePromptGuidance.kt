package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.ConsultingTone

internal object ConsultingTonePromptGuidance {
    fun forTone(tone: ConsultingTone): String =
        when (tone) {
            ConsultingTone.FRIENDLY ->
                "친근한 반말만 써라. 존댓말과 합니다체는 쓰지 마라."
            ConsultingTone.POLITE ->
                "밝고 가벼운 해요체만 써라. 반말과 합니다체는 쓰지 마라."
            ConsultingTone.WITTY_SENIOR ->
                "센스 있는 투자 선배처럼 반말만 써라. 장난은 한 끗만 넣어라."
        }
}
