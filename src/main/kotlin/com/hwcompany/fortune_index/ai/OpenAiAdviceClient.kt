package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenAiAdviceClient(
    restClientBuilder: RestClient.Builder,
    private val properties: AiAdviceProperties
) {
    private val restClient = restClientBuilder
        .baseUrl(properties.openai.baseUrl)
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
            .uri("/responses")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${properties.openai.apiKey}")
            .body(
                OpenAiResponseRequest(
                    model = properties.openai.model,
                    input = listOf(
                        OpenAiMessage(
                            role = "developer",
                            content = listOf(
                                OpenAiContent(
                                    type = "input_text",
                                    text = request.systemPersona
                                )
                            )
                        ),
                        OpenAiMessage(
                            role = "user",
                            content = listOf(
                                OpenAiContent(
                                    type = "input_text",
                                    text = request.userMessage
                                )
                            )
                        )
                    )
                )
            )
            .retrieve()
            .body(OpenAiResponse::class.java)
            ?: throw IllegalStateException("OpenAI 응답이 비어 있습니다.")

        val content = response.output
            ?.asSequence()
            ?.flatMap { it.content.orEmpty().asSequence() }
            ?.firstOrNull { it.type == "output_text" }
            ?.text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("OpenAI 응답 본문에서 텍스트를 찾을 수 없습니다.")

        return StockFortuneAdviceResponse(
            provider = AiProvider.OPENAI,
            model = properties.openai.model,
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

private data class OpenAiResponseRequest(
    val model: String,
    val input: List<OpenAiMessage>
)

private data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContent>
)

private data class OpenAiContent(
    val type: String,
    val text: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiResponse(
    val output: List<OpenAiOutputItem>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutputItem(
    val content: List<OpenAiOutputContent>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutputContent(
    val type: String? = null,
    val text: String? = null
)
