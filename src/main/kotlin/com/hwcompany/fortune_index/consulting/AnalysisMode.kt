package com.hwcompany.fortune_index.consulting

enum class AnalysisMode {
    STOCK_SAJU,
    STOCK_TAROT,
    STOCK_ALL;

    fun includesSaju(): Boolean = this == STOCK_SAJU || this == STOCK_ALL

    fun includesTarot(): Boolean = this == STOCK_TAROT || this == STOCK_ALL
}
