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
        DefaultConsultingPromptContent.get(code)
}

internal object DefaultConsultingPromptContent {
    fun get(code: LlmPromptCode): String =
        when (code) {
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_SAJU ->
                "사주 상담이다. payload.signals.saju의 개인 명식과 운 흐름을 주근거로 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. payload.signals.tarot.drawnCards 3장을 메인, birthTarotCard를 서브로 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. payload.signals.zodiac의 element, moodKeyword, consultingAngle을 질문에 연결해 JSON만 반환해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. 사주, 타로, 별자리 역할을 분리해 JSON만 반환해라."
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
