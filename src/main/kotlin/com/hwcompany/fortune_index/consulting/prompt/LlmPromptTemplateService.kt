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
                "사주 상담이다. payload.signals.saju와 payload.userContext만 근거로 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. payload.signals.tarot과 payload.signals.birthTarotCard, payload.userContext만 근거로 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. payload.signals.zodiac과 payload.userContext만 근거로 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. payload.signals의 사주, 타로, 별자리 신호와 payload.userContext를 함께 근거로 JSON만 반환해라."
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
