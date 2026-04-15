package com.hwcompany.fortune_index.consulting.prompt

import com.hwcompany.fortune_index.consulting.AnalysisMode
import org.springframework.stereotype.Component

@Component
class InvestmentTarotPromptProvider(
    private val llmPromptTemplateService: LlmPromptTemplateService
) : PromptProvider {
    override fun supports(mode: AnalysisMode): Boolean = mode == AnalysisMode.INVESTMENT_TAROT

    override fun buildSystemMessage(): String =
        llmPromptTemplateService.getContent(LlmPromptCode.CONSULTING_SYSTEM_INVESTMENT_TAROT)
}
