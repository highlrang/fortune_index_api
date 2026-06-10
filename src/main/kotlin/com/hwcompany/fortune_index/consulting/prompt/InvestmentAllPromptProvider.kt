package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class InvestmentAllPromptProvider : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.INVESTMENT_ALL

    override fun buildSystemMessage(): String =
        DefaultConsultingPromptContent.get(LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_ALL)
}
