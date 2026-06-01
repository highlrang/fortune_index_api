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
                "사주 상담이다. JSON 하나만 반환해라. 사주 원자료는 서버 계산과 DB 저장값이 기준이다. payload에 없는 팔자, 대운, 세운은 만들거나 바꾸지 마라. 사주 상징은 장황하게 설명하지 말고 사용자의 주식 투자 성향, 보유 기준, 진입 속도, 손실 한도 점검으로 번역해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT ->
                "타로 상담이다. JSON 하나만 반환해라. 타로 원자료는 DB 카드 메타데이터와 서버에서 확정한 카드 뽑기 결과가 기준이다. payload에 없는 카드명이나 카드 의미는 만들거나 바꾸지 마라. 카드 상징은 장황하게 설명하지 말고 사용자의 지금 감정, 충동, 확신 욕구, 진입 전 체크포인트로 번역해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ZODIAC ->
                "별자리 상담이다. JSON 하나만 반환해라. 별자리 원자료는 서버 계산 프로필과 일별 캐시 값이 기준이다. payload에 없는 별자리나 오늘 흐름은 만들거나 바꾸지 마라. 별자리 상징은 장황하게 설명하지 말고 오늘의 판단 분위기, 속도 조절, 보류 또는 유지 기준으로 번역해라."
            LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL ->
                "종합 상담이다. JSON 하나만 반환해라. 사주, 타로, 별자리 원자료는 서버 계산, DB 조회, 일별 캐시에서 확정된 payload 값이 기준이다. payload에 없는 팔자, 카드, 별자리, 오늘 흐름은 만들거나 바꾸지 마라. 각 상징은 장황하게 설명하지 말고 주식 투자 성향, 현재 감정, 오늘의 판단 기준으로 나눠 번역해라."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_SAJU ->
                "내 사주 흐름을 바탕으로 오늘 주식 투자 판단을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_TAROT ->
                "타로로 지금 투자 심리와 오늘의 판단 기준을 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ZODIAC ->
                "별자리로 오늘 주식 투자 판단 분위기를 짧게 봐줘."
            LlmPromptCode.CONSULTING_QUESTION_INVESTMENT_ALL ->
                "사주, 타로, 별자리로 오늘 주식 투자 심리와 판단 기준을 짧게 봐줘."
        }
}
