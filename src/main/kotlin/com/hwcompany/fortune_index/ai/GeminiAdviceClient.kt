package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GeminiAdviceClient(
    restClientBuilder: RestClient.Builder,
    private val properties: AiAdviceProperties
) {
    private val restClient = restClientBuilder
        .baseUrl(properties.gemini.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun generateAdvice(requestJson: String): StockFortuneAdviceResponse =
        generateAdvice(
            AiChatRequest(
                systemPersona = DEFAULT_SYSTEM_PERSONA,
                userMessage = requestJson
            )
        )

    fun generateAdvice(request: AiChatRequest): StockFortuneAdviceResponse {
        val response = restClient.post()
            .uri("/models/${properties.gemini.model}:generateContent")
            .header("x-goog-api-key", properties.gemini.apiKey)
            .body(
                GeminiGenerateContentRequest(
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = request.systemPersona))
                    ),
                    contents = listOf(
                        GeminiUserContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = request.userMessage))
                        )
                    )
                )
            )
            .retrieve()
            .body(GeminiResponse::class.java)
            ?: throw IllegalStateException("Gemini 응답이 비어 있습니다.")

        val content = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Gemini 응답 본문에서 텍스트를 찾을 수 없습니다.")

        return StockFortuneAdviceResponse(
            provider = AiProvider.GEMINI,
            model = properties.gemini.model,
            content = content
        )
    }

    private companion object {
        private const val DEFAULT_SYSTEM_PERSONA =
            "너는 주식 데이터와 사주 오행을 결합해 조언하는 전문가야. " +
                "사용자가 제공한 JSON만 근거로 해석하고, 과장 없이 자연스러운 한국어로 답변해. " +
                "답변은 3~5문장으로 작성하고, 오행 균형과 섹터 등락률을 함께 연결해서 설명해."
    }
}

private data class GeminiGenerateContentRequest(
    val systemInstruction: GeminiContent,
    val contents: List<GeminiUserContent>
)

private data class GeminiContent(
    val parts: List<GeminiPart>
)

private data class GeminiUserContent(
    val role: String,
    val parts: List<GeminiPart>
)

private data class GeminiPart(
    val text: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiCandidate(
    val content: GeminiCandidateContent? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiCandidateContent(
    val parts: List<GeminiTextPart>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiTextPart(
    val text: String? = null
)
