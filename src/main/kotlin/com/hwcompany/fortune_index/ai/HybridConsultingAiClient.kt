package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.io.JsonEOFException
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
    private val defaultGeminiMaxOutputTokens = 800
    private val retryGeminiMaxOutputTokens = 1200

    private val geminiClient = restClientBuilder
        .baseUrl(properties.gemini.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun requestJsonAdvice(
        systemMessage: String,
        payload: JsonNode
    ): HybridConsultingAiResponse =
        when (properties.provider) {
            AiProvider.GEMINI -> requestFromGemini(systemMessage, payload)
        }

    private fun requestFromGemini(
        systemMessage: String,
        payload: JsonNode
    ): HybridConsultingAiResponse {
        val requestedMode = payload.path("mode")
            .asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Hybrid consulting payload must include mode")

        val firstAttempt = requestGeminiCandidate(
            systemMessage = systemMessage,
            payload = payload,
            maxOutputTokens = defaultGeminiMaxOutputTokens
        )
        return try {
            parseJsonContent(
                rawContent = firstAttempt.jsonText,
                provider = AiProvider.GEMINI,
                model = properties.gemini.model,
                requestedMode = requestedMode,
                finishReasons = firstAttempt.finishReasons
            )
        } catch (exception: IllegalStateException) {
            if (!shouldRetryGeminiJsonParse(firstAttempt, exception)) {
                throw exception
            }

            logger.warn(
                "Retrying Gemini request after truncated JSON. model={}, requestedMode={}, finishReasons={}, maxOutputTokens={}",
                properties.gemini.model,
                requestedMode,
                firstAttempt.finishReasons,
                retryGeminiMaxOutputTokens
            )

            val retryAttempt = requestGeminiCandidate(
                systemMessage = systemMessage,
                payload = payload,
                maxOutputTokens = retryGeminiMaxOutputTokens
            )

            parseJsonContent(
                rawContent = retryAttempt.jsonText,
                provider = AiProvider.GEMINI,
                model = properties.gemini.model,
                requestedMode = requestedMode,
                finishReasons = retryAttempt.finishReasons
            )
        }
    }

    private fun parseJsonContent(
        rawContent: String,
        provider: AiProvider,
        model: String,
        requestedMode: String,
        finishReasons: List<String> = emptyList()
    ): HybridConsultingAiResponse {
        val sanitized = rawContent
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonCandidate = extractJsonObject(sanitized)

        val payload = try {
            objectMapper.readValue(jsonCandidate, HybridConsultingPayloadRaw::class.java)
        } catch (exception: JsonProcessingException) {
            logger.warn(
                "Hybrid consulting response was not valid JSON. provider={}, model={}, requestedMode={}, finishReasons={}, rawContentPreview={}",
                provider,
                model,
                requestedMode,
                finishReasons,
                sanitized.take(300)
            )
            val failureReason = when {
                exception is JsonEOFException || looksLikeTruncatedJson(sanitized) ->
                    "AI 상담 JSON 응답이 중간에 잘렸습니다"
                else -> "AI 상담 응답이 JSON 형식이 아닙니다"
            }
            val finishReasonText = finishReasons.ifEmpty { listOf("UNKNOWN") }.joinToString(",")
            throw IllegalStateException(
                "$failureReason. provider=$provider, model=$model, requestedMode=$requestedMode, finishReason=$finishReasonText, preview=${sanitized.take(120)}",
                exception
            )
        }
        val analysisResults = AnalysisResultsPayload(
            investment_analysis = AnalysisSectionPayload(
                title = "외부 기류 해석",
                content = payload.analysis_results.investment_analysis
            ),
            tarot_analysis = payload.analysis_results.tarot_analysis?.let {
                AnalysisSectionPayload(
                    title = "마음의 파동",
                    content = it
                )
            },
            saju_analysis = payload.analysis_results.saju_analysis?.let {
                AnalysisSectionPayload(
                    title = "재물 기질 해석",
                    content = it
                )
            },
            zodiac_analysis = payload.analysis_results.zodiac_analysis?.let {
                AnalysisSectionPayload(
                    title = "별자리 흐름 해석",
                    content = it
                )
            }
        )
        return HybridConsultingAiResponse(
            provider = provider,
            model = model,
            mode = payload.mode ?: requestedMode,
            analysisResults = analysisResults,
            finalAdvice = payload.overall_summary,
            riskScore = payload.risk_score,
            rawJson = jsonCandidate
        )
    }

    private fun requestGeminiCandidate(
        systemMessage: String,
        payload: JsonNode,
        maxOutputTokens: Int
    ): GeminiCandidatePayload {
        val responseBody = linkedMapOf<String, Any>(
            "systemInstruction" to mapOf(
                "parts" to listOf(
                    mapOf(
                        "text" to systemMessage
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
        responseBody["generationConfig"] = mapOf(
            "responseMimeType" to "application/json",
            "maxOutputTokens" to maxOutputTokens,
            "temperature" to 0.1
        )

        val response = geminiClient.post()
            .uri("/models/${properties.gemini.model}:generateContent")
            .header("x-goog-api-key", properties.gemini.apiKey)
            .body(responseBody)
            .retrieve()
            .body(GeminiHybridResponse::class.java)
            ?: throw IllegalStateException("Gemini 응답이 비어 있습니다.")

        val finishReasons = response.candidates.orEmpty()
            .mapNotNull { it.finishReason }
            .distinct()

        val jsonText = response.candidates
            .orEmpty()
            .asSequence()
            .flatMap { candidate -> candidate.content?.parts.orEmpty().asSequence() }
            .mapNotNull { part -> part.text }
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: run {
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

        return GeminiCandidatePayload(
            jsonText = jsonText,
            finishReasons = finishReasons
        )
    }

    private fun shouldRetryGeminiJsonParse(
        candidate: GeminiCandidatePayload,
        exception: IllegalStateException
    ): Boolean =
        candidate.finishReasons.any { it.equals("MAX_TOKENS", ignoreCase = true) } ||
            looksLikeTruncatedJson(candidate.jsonText) ||
            exception.cause is JsonEOFException

    private fun looksLikeTruncatedJson(content: String): Boolean {
        val normalized = content.trim()
        if (!normalized.startsWith("{")) {
            return false
        }
        return !normalized.endsWith("}") || countCharOutsideString(normalized, '{') != countCharOutsideString(normalized, '}')
    }

    private fun countCharOutsideString(content: String, target: Char): Int {
        var count = 0
        var inString = false
        var escaping = false

        content.forEach { char ->
            if (inString) {
                if (escaping) {
                    escaping = false
                } else {
                    if (char == '\\') escaping = true
                    if (char == '"') inString = false
                }
                return@forEach
            }

            when (char) {
                '"' -> inString = true
                target -> count += 1
            }
        }

        return count
    }

    private fun extractJsonObject(content: String): String {
        if (content.startsWith("{") && content.endsWith("}")) {
            return content
        }

        val firstBrace = content.indexOf('{')
        if (firstBrace < 0) {
            return content
        }

        var depth = 0
        var inString = false
        var escaping = false

        for (index in firstBrace until content.length) {
            val char = content[index]
            if (inString) {
                if (escaping) {
                    escaping = false
                    continue
                }
                if (char == '\\') {
                    escaping = true
                    continue
                }
                if (char == '"') {
                    inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        return content.substring(firstBrace, index + 1)
                    }
                }
            }
        }

        return content
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

data class HybridConsultingPayloadRaw(
    val mode: String? = null,
    val analysis_results: AnalysisResultsRawPayload,
    @JsonAlias("final_advice")
    val overall_summary: String,
    val risk_score: Int
)

data class AnalysisResultsRawPayload(
    val investment_analysis: String,
    val tarot_analysis: String? = null,
    val saju_analysis: String? = null,
    val zodiac_analysis: String? = null
)

data class HybridConsultingPayload(
    val mode: String? = null,
    val analysis_results: AnalysisResultsPayload,
    @JsonAlias("final_advice")
    val overall_summary: String,
    val risk_score: Int
)

data class AnalysisResultsPayload(
    val investment_analysis: AnalysisSectionPayload,
    val tarot_analysis: AnalysisSectionPayload? = null,
    val saju_analysis: AnalysisSectionPayload? = null,
    val zodiac_analysis: AnalysisSectionPayload? = null
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

private data class GeminiCandidatePayload(
    val jsonText: String,
    val finishReasons: List<String>
)
