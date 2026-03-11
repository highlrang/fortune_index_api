package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class StockSajuPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.STOCK_SAJU

    override fun systemMessage(): String =
        "너는 투자 데이터와 사용자의 타고난 운명(십성)을 결합해 장기적인 흐름을 짚어줘."
}
