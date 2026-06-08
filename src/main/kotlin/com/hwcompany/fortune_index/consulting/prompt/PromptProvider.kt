package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode

interface PromptProvider {
    fun supports(mode: AnalysisMode): Boolean

    /**
     * 기존 비교 기능과 호환되도록 기본 시스템 메시지 접근자를 유지한다.
     */
    fun systemMessage(): String = buildSystemMessage()

    /**
     * 신규 상담 API에서는 모드별 출력 스키마를 강제하기 위해 이 메서드를 사용한다.
     */
    fun buildSystemMessage(): String
}
