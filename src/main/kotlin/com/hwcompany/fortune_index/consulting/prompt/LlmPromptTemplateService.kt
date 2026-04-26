package com.hwcompany.fortune_index.consulting.prompt

import org.springframework.stereotype.Service

@Service
class LlmPromptTemplateService(
    private val llmPromptTemplateRepository: LlmPromptTemplateRepository
) {
    fun getContent(code: LlmPromptCode): String =
        llmPromptTemplateRepository.findByCodeAndEnabledTrue(code.code)?.content
            ?: defaultContent(code)

    private fun defaultContent(code: LlmPromptCode): String =
        when (code) {
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_SAJU ->
                "사주 상담이다. JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU ->
                "오늘의 재물운을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT ->
                "타로로 오늘의 재물운을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC ->
                "별자리로 오늘의 재물운을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL ->
                "사주, 타로, 별자리로 오늘의 재물운을 짧게 봐줘."
        }
}
