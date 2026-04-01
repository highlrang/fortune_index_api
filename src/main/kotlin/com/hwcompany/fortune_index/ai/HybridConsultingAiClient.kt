package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    private val geminiClient = restClientBuilder
        .baseUrl(properties.gemini.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun requestJsonAdvice(
        systemMessage: String,
        payload: JsonNode,
        enableGoogleSearch: Boolean = false
    ): HybridConsultingAiResponse =
        when (properties.provider) {
            AiProvider.GEMINI -> requestFromGemini(systemMessage, payload, enableGoogleSearch)
        }

    private fun requestFromGemini(
        systemMessage: String,
        payload: JsonNode,
        enableGoogleSearch: Boolean
    ): HybridConsultingAiResponse {
        val requestedMode = payload.path("mode")
            .asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Hybrid consulting payload must include mode")

        val responseBody = linkedMapOf<String, Any>(
            "systemInstruction" to mapOf(
                "parts" to listOf(
                    mapOf(
                        "text" to buildString {
                            append(systemMessage)
                            if (enableGoogleSearch) {
                                append("\n반드시 단일 JSON 객체만 반환하고, 코드펜스나 설명 문장은 절대 추가하지 마라.")
                            }
                        }
                    )
                )
            ),
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to objectMapper.writeValueAsString(payload)))
                )
            )
        )
        if (enableGoogleSearch) {
            responseBody["tools"] = listOf(mapOf("google_search" to emptyMap<String, String>()))
        } else {
            responseBody["generationConfig"] = mapOf(
                "responseMimeType" to "application/json"
            )
        }

        val response = geminiClient.post()
            .uri("/models/${properties.gemini.model}:generateContent")
            .header("x-goog-api-key", properties.gemini.apiKey)
            .body(responseBody)
            .retrieve()
            .body(GeminiHybridResponse::class.java)
            ?: throw IllegalStateException("Gemini 응답이 비어 있습니다.")

        val jsonText = response.candidates
            .orEmpty()
            .asSequence()
            .flatMap { candidate -> candidate.content?.parts.orEmpty().asSequence() }
            .mapNotNull { part -> part.text }
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: run {
                val finishReasons = response.candidates.orEmpty()
                    .mapNotNull { it.finishReason }
                    .distinct()
                val promptBlocked = response.promptFeedback?.blockReason
                logger.warn(
                    "Gemini JSON text missing. model={}, finishReasons={}, promptBlocked={}, candidateCount={}",
                    properties.gemini.model,
                    finishReasons,
                    promptBlocked,
                    response.candidates.orEmpty().size
                )
                throw IllegalStateException(
                    buildString {
                        append("Gemini 응답에서 JSON 텍스트를 찾을 수 없습니다")
                        if (finishReasons.isNotEmpty()) {
                            append(" (finishReason=")
                            append(finishReasons.joinToString(","))
                            append(')')
                        }
                        if (!promptBlocked.isNullOrBlank()) {
                            append(" (blockReason=")
                            append(promptBlocked)
                            append(')')
                        }
                        append('.')
                    }
                )
            }

        return parseJsonContent(
            rawContent = jsonText,
            provider = AiProvider.GEMINI,
            model = properties.gemini.model,
            requestedMode = requestedMode,
            groundingMetadata = response.candidates
                ?.firstOrNull()
                ?.groundingMetadata
        )
    }

    private fun parseJsonContent(
        rawContent: String,
        provider: AiProvider,
        model: String,
        requestedMode: String,
        groundingMetadata: GeminiGroundingMetadata? = null
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
            rawJson = sanitized,
            evidence = HybridConsultingEvidence(
                grounded = !groundingMetadata?.groundingChunks.isNullOrEmpty(),
                citations = groundingMetadata?.groundingChunks.orEmpty()
                    .mapNotNull { chunk ->
                        val web = chunk.web ?: return@mapNotNull null
                        val url = web.uri?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        HybridConsultingCitation(
                            title = web.title?.takeIf { it.isNotBlank() } ?: url,
                            url = url
                        )
                    }
            )
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
    val rawJson: String,
    val evidence: HybridConsultingEvidence = HybridConsultingEvidence()
)

data class HybridConsultingEvidence(
    val grounded: Boolean = false,
    val citations: List<HybridConsultingCitation> = emptyList()
)

data class HybridConsultingCitation(
    val title: String,
    val url: String
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
    val candidates: List<GeminiHybridCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridCandidate(
    val content: GeminiHybridContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null,
    val finishReason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridContent(
    val parts: List<GeminiHybridPart>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridPart(
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiPromptFeedback(
    val blockReason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGroundingMetadata(
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGroundingChunk(
    val web: GeminiGroundingWeb? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGroundingWeb(
    val uri: String? = null,
    val title: String? = null
)
