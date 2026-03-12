package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptCode
import com.hwcompany.fortune_index.consulting.prompt.LlmPromptTemplateService
import org.springframework.stereotype.Service

@Service
class StockFortuneAdviceService(
    private val properties: AiAdviceProperties,
    private val geminiAdviceClient: GeminiAdviceClient,
    private val objectMapper: ObjectMapper,
    private val llmPromptTemplateService: LlmPromptTemplateService
) {
    fun generateAdvice(request: AiChatRequest): StockFortuneAdviceResponse =
        when (properties.provider) {
            AiProvider.GEMINI -> geminiAdviceClient.generateAdvice(request)
        }

    fun generateAdvice(request: StockFortuneAdviceRequest): StockFortuneAdviceResponse {
        val requestJson = objectMapper.writeValueAsString(
            mapOf(
                "userName" to request.userName,
                "fiveElements" to request.fiveElements,
                "sectorChanges" to request.sectorChanges,
                "question" to (request.question ?: llmPromptTemplateService.getContent(LlmPromptCode.STOCK_FORTUNE_QUESTION_DEFAULT))
            )
        )

        return generateAdvice(
            AiChatRequest(
                systemPersona = llmPromptTemplateService.getContent(LlmPromptCode.STOCK_FORTUNE_SYSTEM_DEFAULT),
                userMessage = requestJson
            )
        )
    }
}
