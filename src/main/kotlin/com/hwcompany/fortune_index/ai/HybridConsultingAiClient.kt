package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 하이브리드 상담 API 전용 AI 게이트웨이이다.
 * 모델이 JSON만 반환하도록 강하게 유도하고, 최종적으로 Kotlin data class로 파싱한다.
 */
@Component
class HybridConsultingAiClient(
    restClientBuilder: RestClient.Builder,
    private val properties: AiAdviceProperties,
    private val objectMapper: ObjectMapper
) {
    private val geminiClient = restClientBuilder
        .baseUrl(properties.gemini.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun requestJsonAdvice(systemMessage: String, payload: JsonNode): HybridConsultingAiResponse =
        when (properties.provider) {
            AiProvider.GEMINI -> requestFromGemini(systemMessage, payload)
        }

    private fun requestFromGemini(systemMessage: String, payload: JsonNode): HybridConsultingAiResponse {
        val requestedMode = payload.path("mode")
            .asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Hybrid consulting payload must include mode")

        val response = geminiClient.post()
            .uri("/models/${properties.gemini.model}:generateContent")
            .header("x-goog-api-key", properties.gemini.apiKey)
            .body(
                mapOf(
                    "systemInstruction" to mapOf(
                        "parts" to listOf(mapOf("text" to systemMessage))
                    ),
                    "generationConfig" to mapOf(
                        "responseMimeType" to "application/json"
                    ),
                    "contents" to listOf(
                        mapOf(
                            "role" to "user",
                            "parts" to listOf(mapOf("text" to objectMapper.writeValueAsString(payload)))
                        )
                    )
                )
            )
            .retrieve()
            .body(GeminiHybridResponse::class.java)
            ?: throw IllegalStateException("Gemini 응답이 비어 있습니다.")

        val jsonText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Gemini 응답에서 JSON 텍스트를 찾을 수 없습니다.")

        return parseJsonContent(
            rawContent = jsonText,
            provider = AiProvider.GEMINI,
            model = properties.gemini.model,
            requestedMode = requestedMode
        )
    }

    private fun parseJsonContent(
        rawContent: String,
        provider: AiProvider,
        model: String,
        requestedMode: String
    ): HybridConsultingAiResponse {
        val sanitized = rawContent
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val payload = objectMapper.readValue(sanitized, HybridConsultingPayload::class.java)
        return HybridConsultingAiResponse(
            provider = provider,
            model = model,
            mode = payload.mode ?: requestedMode,
            analysisResults = payload.analysis_results,
            finalAdvice = payload.overall_summary,
            riskScore = payload.risk_score,
            rawJson = sanitized
        )
    }
}

data class HybridConsultingAiResponse(
    val provider: AiProvider,
    val model: String,
    val mode: String,
    val analysisResults: AnalysisResultsPayload,
    val finalAdvice: String,
    val riskScore: Int,
    val rawJson: String
)

data class HybridConsultingPayload(
    val mode: String? = null,
    val analysis_results: AnalysisResultsPayload,
    @JsonAlias("final_advice")
    val overall_summary: String,
    val risk_score: Int
)

data class AnalysisResultsPayload(
    val market_analysis: AnalysisSectionPayload,
    val tarot_analysis: AnalysisSectionPayload? = null,
    val saju_analysis: AnalysisSectionPayload? = null
)

data class AnalysisSectionPayload(
    val title: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridResponse(
    val candidates: List<GeminiHybridCandidate>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridCandidate(
    val content: GeminiHybridContent? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridContent(
    val parts: List<GeminiHybridPart>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridPart(
    val text: String? = null
)
