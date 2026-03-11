package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class StockOnlyPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.ONLY_STOCK

    override fun systemMessage(): String =
        "너는 냉철한 퀀트 투자자야. 사주나 타로는 미신이라 생각하고 오직 지표로만 말해."
}
