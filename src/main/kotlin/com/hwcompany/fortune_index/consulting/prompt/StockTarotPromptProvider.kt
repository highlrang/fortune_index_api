package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class StockTarotPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.STOCK_TAROT

    override fun systemMessage(): String =
        "너는 현재의 시장 상황과 사용자가 뽑은 카드의 직관적 에너지를 연결해줘."
}
