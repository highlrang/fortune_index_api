package com.hwcompany.fortune_index.consulting

enum class AnalysisMode {
    INVESTMENT_SAJU,
    INVESTMENT_TAROT,
    INVESTMENT_ZODIAC,
    INVESTMENT_ALL;

    fun includesSaju(): Boolean = this == INVESTMENT_SAJU || this == INVESTMENT_ALL

    fun includesTarot(): Boolean = this == INVESTMENT_TAROT || this == INVESTMENT_ALL

    fun includesZodiac(): Boolean = this == INVESTMENT_ZODIAC || this == INVESTMENT_ALL
}
