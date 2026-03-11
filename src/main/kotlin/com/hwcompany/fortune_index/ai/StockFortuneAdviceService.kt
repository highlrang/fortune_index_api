package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class StockFortuneAdviceService(
    private val properties: AiAdviceProperties,
    private val openAiAdviceClient: OpenAiAdviceClient,
    private val geminiAdviceClient: GeminiAdviceClient,
    private val objectMapper: ObjectMapper
) {
    fun generateAdvice(request: AiChatRequest): StockFortuneAdviceResponse {
        return when (properties.provider) {
            AiProvider.OPENAI -> openAiAdviceClient.generateAdvice(request)
            AiProvider.GEMINI -> geminiAdviceClient.generateAdvice(request)
        }
    }

    fun generateAdvice(request: StockFortuneAdviceRequest): StockFortuneAdviceResponse {
        val requestJson = objectMapper.writeValueAsString(
            mapOf(
                "userName" to request.userName,
                "fiveElements" to request.fiveElements,
                "sectorChanges" to request.sectorChanges,
                "question" to (request.question ?: "오행과 섹터 흐름을 함께 해석해 투자 관점의 조언을 해줘.")
            )
        )

        return generateAdvice(
            AiChatRequest(
                systemPersona = DEFAULT_SYSTEM_PERSONA,
                userMessage = requestJson
            )
        )
    }

    private companion object {
        private const val DEFAULT_SYSTEM_PERSONA =
            "너는 주식 데이터와 사주 오행을 결합해 조언하는 전문가야. " +
                "사용자가 제공한 JSON만 근거로 해석하고, 과장 없이 자연스러운 한국어로 답변해. " +
                "답변은 3~5문장으로 작성하고, 오행 균형과 섹터 등락률을 함께 연결해서 설명해."
    }
}
