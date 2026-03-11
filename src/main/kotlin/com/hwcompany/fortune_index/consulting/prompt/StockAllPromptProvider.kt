package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class StockAllPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.STOCK_ALL

    override fun systemMessage(): String =
        "너는 데이터, 운명, 직관을 모두 통합한 마스터 상담가야."
}
