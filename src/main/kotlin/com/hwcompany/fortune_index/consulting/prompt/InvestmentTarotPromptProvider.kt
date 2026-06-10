package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class InvestmentTarotPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.INVESTMENT_TAROT

    override fun buildSystemMessage(): String =
        DefaultConsultingPromptContent.get(LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT)
}
