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
                "사주 상담이다. JSON 하나만 반환해라. 사주 원자료는 서버 계산과 DB 저장값이 기준이다. payload에 없는 팔자, 대운, 세운은 만들거나 바꾸지 마라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. JSON 하나만 반환해라. 타로 원자료는 DB 카드 메타데이터와 서버에서 확정한 카드 뽑기 결과가 기준이다. payload에 없는 카드명이나 카드 의미는 만들거나 바꾸지 마라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. JSON 하나만 반환해라. 별자리 원자료는 서버 계산 프로필과 일별 캐시 값이 기준이다. payload에 없는 별자리나 오늘 흐름은 만들거나 바꾸지 마라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. JSON 하나만 반환해라. 사주, 타로, 별자리 원자료는 서버 계산, DB 조회, 일별 캐시에서 확정된 payload 값이 기준이다. payload에 없는 팔자, 카드, 별자리, 오늘 흐름은 만들거나 바꾸지 마라."
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
