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
                "너는 재물/투자운세 상담 안내자다. 지금은 사주 상담이며 응답은 JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "너는 재물/투자운세 상담 안내자다. 지금은 타로 상담이며 응답은 JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "너는 재물/투자운세 상담 안내자다. 지금은 별자리 상담이며 응답은 JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "너는 재물/투자운세 상담 안내자다. 지금은 사주, 타로, 별자리를 함께 보는 종합 상담이며 응답은 JSON 하나만 반환해라."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU ->
                "내 사주 흐름을 바탕으로 오늘의 재물운과 마음 흐름을 읽어줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT ->
                "타로로 오늘의 재물운과 감정 흐름을 읽어줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC ->
                "내 별자리 흐름을 바탕으로 오늘의 재물운과 마음 흐름을 읽어줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL ->
                "사주, 타로, 별자리를 함께 보고 오늘의 재물운과 마음 흐름을 읽어줘."
        }
}
