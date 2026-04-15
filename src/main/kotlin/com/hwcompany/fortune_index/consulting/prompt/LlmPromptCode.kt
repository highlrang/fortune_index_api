package com.hwcompany.fortune_index.consulting.prompt

enum class LlmPromptCode(val code: String, val title: String) {
    CONSULTING_SYSTEM_STOCK_SAJU("consulting.system.stock-saju", "종합 상담 시스템 프롬프트 - 사주"),
    CONSULTING_SYSTEM_STOCK_TAROT("consulting.system.stock-tarot", "종합 상담 시스템 프롬프트 - 타로"),
    CONSULTING_SYSTEM_STOCK_ALL("consulting.system.stock-all", "종합 상담 시스템 프롬프트 - 사주+타로"),
    CONSULTING_QUESTION_STOCK_SAJU("consulting.question.stock-saju", "종합 상담 기본 질문 - 사주"),
    CONSULTING_QUESTION_STOCK_TAROT("consulting.question.stock-tarot", "종합 상담 기본 질문 - 타로"),
    CONSULTING_QUESTION_STOCK_ALL("consulting.question.stock-all", "종합 상담 기본 질문 - 사주+타로");
}
