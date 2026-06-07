package com.hwcompany.fortune_index.consulting.prompt

import org.springframework.stereotype.Service

@Service
class LlmPromptTemplateService(
    private val llmPromptTemplateRepository: LlmPromptTemplateRepository
) {
    fun getContent(code: LlmPromptCode): String =
        llmPromptTemplateRepository.findByCodeAndEnabledTrue(code.code)?.content
            ?: getDefaultContent(code)

    fun getDefaultContent(code: LlmPromptCode): String =
        when (code) {
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_SAJU ->
                "사주 상담이다. JSON만 반환해라. payload.saju와 dailyFlow.saju만 근거로 용신·합충형파해를 우선 참고하여 투자 성향, 보유 기준, 진입 속도, 손실 한도 점검을 말해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. JSON만 반환해라. payload.tarot만 근거로 현재 감정, 충동, 확신 욕구, 진입 전 체크포인트를 말해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. JSON만 반환해라. payload.zodiac과 dailyFlow.zodiac만 근거로 오늘의 판단 분위기, 속도 조절, 보류 또는 유지 기준을 말해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. JSON만 반환해라. payload의 사주(용신·합충형파해 우선), 타로, 별자리 값만 근거로 투자 성향, 현재 감정, 오늘의 판단 기준을 말해라."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU ->
                "사주 기준으로 오늘 투자 판단을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT ->
                "타로 기준으로 오늘 투자 판단을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC ->
                "별자리 기준으로 오늘 투자 판단을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL ->
                "사주, 타로, 별자리 기준으로 오늘 투자 판단을 짧게 봐줘."
        }
}
