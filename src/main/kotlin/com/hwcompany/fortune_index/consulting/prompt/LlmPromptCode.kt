package com.hwcompany.fortune_index.consulting.prompt

enum class LlmPromptCode(val code: String, val title: String) {
    CONSULTING_SYSTEM_ONLY_STOCK("consulting.system.only-stock", "종합 상담 시스템 프롬프트 - 주식 전용"),
    CONSULTING_SYSTEM_STOCK_SAJU("consulting.system.stock-saju", "종합 상담 시스템 프롬프트 - 주식+사주"),
    CONSULTING_SYSTEM_STOCK_TAROT("consulting.system.stock-tarot", "종합 상담 시스템 프롬프트 - 주식+타로"),
    CONSULTING_SYSTEM_STOCK_ALL("consulting.system.stock-all", "종합 상담 시스템 프롬프트 - 주식+사주+타로"),
    CONSULTING_QUESTION_ONLY_STOCK("consulting.question.only-stock", "종합 상담 기본 질문 - 주식 전용"),
    CONSULTING_QUESTION_STOCK_SAJU("consulting.question.stock-saju", "종합 상담 기본 질문 - 주식+사주"),
    CONSULTING_QUESTION_STOCK_TAROT("consulting.question.stock-tarot", "종합 상담 기본 질문 - 주식+타로"),
    CONSULTING_QUESTION_STOCK_ALL("consulting.question.stock-all", "종합 상담 기본 질문 - 주식+사주+타로"),
    COMPARE_QUESTION_ONLY_STOCK("compare.question.only-stock", "비교 상담 기본 질문 - 주식 전용"),
    COMPARE_QUESTION_STOCK_SAJU("compare.question.stock-saju", "비교 상담 기본 질문 - 주식+사주"),
    COMPARE_QUESTION_STOCK_TAROT("compare.question.stock-tarot", "비교 상담 기본 질문 - 주식+타로"),
    COMPARE_QUESTION_STOCK_ALL("compare.question.stock-all", "비교 상담 기본 질문 - 주식+사주+타로"),
    STOCK_FORTUNE_SYSTEM_DEFAULT("stock-fortune.system.default", "주식 운세 기본 시스템 프롬프트"),
    STOCK_FORTUNE_QUESTION_DEFAULT("stock-fortune.question.default", "주식 운세 기본 질문"),
    ADVANCED_SYSTEM_DEFAULT("advanced.system.default", "심화 상담 기본 시스템 프롬프트"),
    ADVANCED_QUESTION_DEFAULT("advanced.question.default", "심화 상담 기본 질문"),
    COMPARATIVE_SYSTEM_MARKET_ONLY("comparative.system.market-only", "복합 상담 시스템 프롬프트 - 증시 전용"),
    COMPARATIVE_SYSTEM_MARKET_SAJU("comparative.system.market-saju", "복합 상담 시스템 프롬프트 - 증시+사주"),
    COMPARATIVE_SYSTEM_MARKET_TAROT("comparative.system.market-tarot", "복합 상담 시스템 프롬프트 - 증시+타로"),
    COMPARATIVE_SYSTEM_MARKET_SAJU_TAROT("comparative.system.market-saju-tarot", "복합 상담 시스템 프롬프트 - 증시+사주+타로"),
    COMPARATIVE_QUESTION_MARKET_ONLY("comparative.question.market-only", "복합 상담 기본 질문 - 증시 전용"),
    COMPARATIVE_QUESTION_MARKET_SAJU("comparative.question.market-saju", "복합 상담 기본 질문 - 증시+사주"),
    COMPARATIVE_QUESTION_MARKET_TAROT("comparative.question.market-tarot", "복합 상담 기본 질문 - 증시+타로"),
    COMPARATIVE_QUESTION_MARKET_SAJU_TAROT("comparative.question.market-saju-tarot", "복합 상담 기본 질문 - 증시+사주+타로");
}
