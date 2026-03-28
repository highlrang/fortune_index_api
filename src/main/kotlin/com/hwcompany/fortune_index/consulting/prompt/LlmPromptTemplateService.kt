package com.hwcompany.fortune_index.consulting.prompt

import org.springframework.stereotype.Service

@Service
class LlmPromptTemplateService(
    private val llmPromptTemplateRepository: LlmPromptTemplateRepository
) {
    fun getContent(code: LlmPromptCode): String =
        llmPromptTemplateRepository.findByCodeAndEnabledTrue(code.code)?.content
            ?: error("LLM prompt template is missing or disabled: ${code.code}")
}
