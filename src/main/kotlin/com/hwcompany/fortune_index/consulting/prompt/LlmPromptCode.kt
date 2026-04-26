package com.hwcompany.fortune_index.consulting.prompt

enum class LlmPromptCode(val code: String, val title: String) {
    CONSULTING_SYSTEM_INVESTMENT_SAJU("consulting.system.investment-saju", "종합 상담 시스템 프롬프트 - 사주"),
    CONSULTING_SYSTEM_INVESTMENT_TAROT("consulting.system.investment-tarot", "종합 상담 시스템 프롬프트 - 타로"),
    CONSULTING_SYSTEM_INVESTMENT_ZODIAC("consulting.system.investment-zodiac", "종합 상담 시스템 프롬프트 - 별자리"),
    CONSULTING_SYSTEM_INVESTMENT_ALL("consulting.system.investment-all", "종합 상담 시스템 프롬프트 - 사주+타로+별자리"),
    CONSULTING_QUESTION_INVESTMENT_SAJU("consulting.question.investment-saju", "종합 상담 기본 질문 - 사주"),
    CONSULTING_QUESTION_INVESTMENT_TAROT("consulting.question.investment-tarot", "종합 상담 기본 질문 - 타로"),
    CONSULTING_QUESTION_INVESTMENT_ZODIAC("consulting.question.investment-zodiac", "종합 상담 기본 질문 - 별자리"),
    CONSULTING_QUESTION_INVESTMENT_ALL("consulting.question.investment-all", "종합 상담 기본 질문 - 사주+타로+별자리");
}
