package com.hwcompany.fortune_index.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
    @Qualifier("geminiRestClientBuilder") geminiRestClientBuilder: RestClient.Builder,
    @Qualifier("openAiRestClientBuilder") openAiRestClientBuilder: RestClient.Builder,
    private val properties: AiAdviceProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val defaultGeminiMaxOutputTokens get() = properties.gemini.maxOutputTokens
    private val retryGeminiMaxOutputTokens get() = properties.gemini.retryMaxOutputTokens
    private val defaultOpenAiMaxOutputTokens get() = properties.openai.maxOutputTokens
    private val retryOpenAiMaxOutputTokens get() = properties.openai.retryMaxOutputTokens

    private val geminiClient = geminiRestClientBuilder
        .baseUrl(properties.gemini.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    private val openAiClient = openAiRestClientBuilder
        .baseUrl(properties.openai.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun requestJsonAdvice(
        systemMessage: String,
        payload: JsonNode
    ): HybridConsultingAiResponse =
        when (properties.provider) {
            AiProvider.GEMINI -> requestFromGemini(systemMessage, payload)
            AiProvider.OPENAI -> requestFromOpenAi(systemMessage, payload)
        }

    private fun requestFromGemini(
        systemMessage: String,
        payload: JsonNode
    ): HybridConsultingAiResponse {
        val requestedMode = payload.path("mode")
            .asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("하이브리드 상담 payload에는 mode가 포함되어야 합니다.")

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

    private fun requestFromOpenAi(
        systemMessage: String,
        payload: JsonNode
    ): HybridConsultingAiResponse {
        val requestedMode = payload.path("mode")
            .asText(null)
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("하이브리드 상담 payload에는 mode가 포함되어야 합니다.")

        val firstAttempt = requestOpenAiCandidate(
            systemMessage = systemMessage,
            payload = payload,
            maxOutputTokens = defaultOpenAiMaxOutputTokens
        )
        return try {
            parseJsonContent(
                rawContent = firstAttempt.jsonText,
                provider = AiProvider.OPENAI,
                model = properties.openai.model,
                requestedMode = requestedMode,
                finishReasons = firstAttempt.finishReasons
            )
        } catch (exception: IllegalStateException) {
            if (!shouldRetryOpenAiJsonParse(firstAttempt, exception)) {
                throw exception
            }

            logger.warn(
                "Retrying OpenAI request after truncated JSON. model={}, requestedMode={}, finishReasons={}, maxOutputTokens={}",
                properties.openai.model,
                requestedMode,
                firstAttempt.finishReasons,
                retryOpenAiMaxOutputTokens
            )

            val retryAttempt = requestOpenAiCandidate(
                systemMessage = systemMessage,
                payload = payload,
                maxOutputTokens = retryOpenAiMaxOutputTokens
            )

            parseJsonContent(
                rawContent = retryAttempt.jsonText,
                provider = AiProvider.OPENAI,
                model = properties.openai.model,
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
            objectMapper.readTree(jsonCandidate)
        } catch (exception: JsonProcessingException) {
            logger.warn(
                "Hybrid consulting response was not valid JSON. provider={}, model={}, requestedMode={}, finishReasons={}, rawContentChars={}, extractedJsonChars={}, truncated={}",
                provider,
                model,
                requestedMode,
                finishReasons,
                sanitized.length,
                jsonCandidate.length,
                looksLikeTruncatedJson(sanitized)
            )
            val failureReason = when {
                exception is JsonEOFException || looksLikeTruncatedJson(sanitized) ->
                    "AI 상담 JSON 응답이 중간에 잘렸습니다"
                else -> "AI 상담 응답이 JSON 형식이 아닙니다"
            }
            val finishReasonText = finishReasons.ifEmpty { listOf("UNKNOWN") }.joinToString(",")
            throw IllegalStateException(
                "$failureReason. provider=$provider, model=$model, requestedMode=$requestedMode, finishReason=$finishReasonText, rawContentChars=${sanitized.length}",
                exception
            )
        }
        val analysisResultsNode = payload.path("analysis_results")
        val analysisResults = AnalysisResultsPayload(
            investment_analysis = null,
            tarot_analysis = payload.extractOptionalSection("tarot_analysis", "마음의 파동")
                ?: analysisResultsNode.extractOptionalSection("tarot_analysis", "마음의 파동"),
            saju_analysis = payload.extractOptionalSection("saju_analysis", "투자 기질 해석")
                ?: analysisResultsNode.extractOptionalSection("saju_analysis", "투자 기질 해석"),
            zodiac_analysis = payload.extractOptionalSection("zodiac_analysis", "별자리 판단 해석")
                ?: analysisResultsNode.extractOptionalSection("zodiac_analysis", "별자리 판단 해석")
        )
        return HybridConsultingAiResponse(
            provider = provider,
            model = model,
            mode = payload.path("mode").asText(null)?.takeIf { it.isNotBlank() } ?: requestedMode,
            analysisResults = analysisResults,
            finalAdvice = payload.extractRequiredText("overall_summary", "final_advice"),
            stabilityScore = payload.scoreNode().asInt(),
            rawJson = jsonCandidate
        )
    }

    private fun requestGeminiCandidate(
        systemMessage: String,
        payload: JsonNode,
        maxOutputTokens: Int
    ): GeminiCandidatePayload {
        val serializedPayload = objectMapper.writeValueAsString(payload)
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
                    "parts" to listOf(mapOf("text" to serializedPayload))
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

        if (properties.gemini.logUsageMetadata) {
            val usage = response.usageMetadata
            logger.info(
                "Gemini usage. model={}, maxOutputTokens={}, promptChars={}, payloadChars={}, finishReasons={}, promptTokens={}, candidateTokens={}, totalTokens={}, thoughtsTokens={}",
                properties.gemini.model,
                maxOutputTokens,
                systemMessage.length,
                serializedPayload.length,
                finishReasons,
                usage?.promptTokenCount,
                usage?.candidatesTokenCount,
                usage?.totalTokenCount,
                usage?.thoughtsTokenCount
            )
        }

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
                    "Gemini JSON text missing. model={}, finishReasons={}, promptBlocked={}, candidateCount={}, usageMetadata={}",
                    properties.gemini.model,
                    finishReasons,
                    promptBlocked,
                    response.candidates.orEmpty().size,
                    response.usageMetadata
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

        logger.info(
            "Gemini response summary. model={}, maxOutputTokens={}, finishReasons={}, responseChars={}, usageMetadata={}",
            properties.gemini.model,
            maxOutputTokens,
            finishReasons,
            jsonText.length,
            response.usageMetadata
        )

        return GeminiCandidatePayload(
            jsonText = jsonText,
            finishReasons = finishReasons
        )
    }

    private fun requestOpenAiCandidate(
        systemMessage: String,
        payload: JsonNode,
        maxOutputTokens: Int
    ): OpenAiCandidatePayload {
        val serializedPayload = objectMapper.writeValueAsString(payload)
        val responseBody = linkedMapOf<String, Any>(
            "model" to properties.openai.model,
            "instructions" to systemMessage,
            "input" to serializedPayload,
            "max_output_tokens" to maxOutputTokens,
            "text" to mapOf(
                "format" to mapOf(
                    "type" to "json_schema",
                    "name" to "hybrid_consulting_advice",
                    "strict" to true,
                    "schema" to mapOf(
                        "type" to "object",
                        "additionalProperties" to false,
                        "properties" to mapOf(
                            "mode" to mapOf("type" to "string"),
                            "saju_analysis" to analysisSectionSchema(),
                            "tarot_analysis" to analysisSectionSchema(),
                            "zodiac_analysis" to analysisSectionSchema(),
                            "overall_summary" to mapOf("type" to "string"),
                            "stability_score" to mapOf(
                                "type" to "integer",
                                "minimum" to 0,
                                "maximum" to 100
                            )
                        ),
                        "required" to listOf(
                            "mode",
                            "saju_analysis",
                            "tarot_analysis",
                            "zodiac_analysis",
                            "overall_summary",
                            "stability_score"
                        )
                    )
                )
            )
        )

        val response = openAiClient.post()
            .uri("/responses")
            .headers { headers ->
                headers.setBearerAuth(properties.openai.apiKey)
            }
            .body(responseBody)
            .retrieve()
            .body(OpenAiResponse::class.java)
            ?: throw IllegalStateException("OpenAI 응답이 비어 있습니다.")

        val incompleteReason = response.incompleteDetails?.reason
        val finishReasons = listOfNotNull(response.status, incompleteReason).distinct()

        if (properties.openai.logUsageMetadata) {
            val usage = response.usage
            logger.info(
                "OpenAI usage. model={}, maxOutputTokens={}, promptChars={}, payloadChars={}, status={}, incompleteReason={}, inputTokens={}, outputTokens={}, totalTokens={}",
                properties.openai.model,
                maxOutputTokens,
                systemMessage.length,
                serializedPayload.length,
                response.status,
                incompleteReason,
                usage?.inputTokens,
                usage?.outputTokens,
                usage?.totalTokens
            )
        }

        val jsonText = response.output
            .orEmpty()
            .asSequence()
            .flatMap { item -> item.content.orEmpty().asSequence() }
            .mapNotNull { content -> content.text }
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: run {
                logger.warn(
                    "OpenAI JSON text missing. model={}, status={}, incompleteReason={}, outputCount={}, usage={}",
                    properties.openai.model,
                    response.status,
                    incompleteReason,
                    response.output.orEmpty().size,
                    response.usage
                )
                throw IllegalStateException(
                    buildString {
                        append("OpenAI 응답에서 JSON 텍스트를 찾을 수 없습니다")
                        if (!response.status.isNullOrBlank()) {
                            append(" (status=")
                            append(response.status)
                            append(')')
                        }
                        if (!incompleteReason.isNullOrBlank()) {
                            append(" (incompleteReason=")
                            append(incompleteReason)
                            append(')')
                        }
                        append('.')
                    }
                )
            }

        logger.info(
            "OpenAI response summary. model={}, maxOutputTokens={}, status={}, incompleteReason={}, responseChars={}, usage={}",
            properties.openai.model,
            maxOutputTokens,
            response.status,
            incompleteReason,
            jsonText.length,
            response.usage
        )

        return OpenAiCandidatePayload(
            jsonText = jsonText,
            finishReasons = finishReasons
        )
    }

    private fun analysisSectionSchema(): Map<String, Any> =
        mapOf(
            "type" to listOf("object", "null"),
            "additionalProperties" to false,
            "properties" to mapOf(
                "title" to mapOf("type" to "string"),
                "content" to mapOf("type" to "string")
            ),
            "required" to listOf("title", "content")
        )

    private fun shouldRetryGeminiJsonParse(
        candidate: GeminiCandidatePayload,
        exception: IllegalStateException
    ): Boolean =
        candidate.finishReasons.any { it.equals("MAX_TOKENS", ignoreCase = true) } ||
            looksLikeTruncatedJson(candidate.jsonText) ||
            exception.cause is JsonEOFException

    private fun shouldRetryOpenAiJsonParse(
        candidate: OpenAiCandidatePayload,
        exception: IllegalStateException
    ): Boolean =
        candidate.finishReasons.any {
            it.equals("incomplete", ignoreCase = true) ||
                it.equals("max_output_tokens", ignoreCase = true)
        } ||
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

    private fun JsonNode.extractRequiredText(vararg fieldNames: String): String {
        fieldNames.forEach { fieldName ->
            val value = path(fieldName).asText(null)?.trim()
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        throw IllegalStateException("AI 상담 응답에 필수 텍스트 필드가 없습니다: ${fieldNames.joinToString(",")}")
    }

    private fun JsonNode.scoreNode(): JsonNode {
        val stabilityScoreNode = path("stability_score")
        if (!stabilityScoreNode.isMissingNode && !stabilityScoreNode.isNull) {
            return stabilityScoreNode
        }
        return path("risk_score")
    }

    private fun JsonNode.extractOptionalSection(fieldName: String, fallbackTitle: String): AnalysisSectionPayload? {
        val sectionNode = path(fieldName)
        if (sectionNode.isMissingNode || sectionNode.isNull) {
            return null
        }

        if (sectionNode.isTextual) {
            return sectionNode.asText().trim().ifBlank { null }?.let { content ->
                AnalysisSectionPayload(
                    title = fallbackTitle,
                    content = content
                )
            }
        }

        if (sectionNode.isObject) {
            val title = sectionNode.path("title").asText(null)?.trim()?.ifBlank { null } ?: fallbackTitle
            val candidate = listOf("content", "analysis", "description", "text")
                .asSequence()
                .mapNotNull { key -> sectionNode.path(key).asText(null)?.trim() }
                .firstOrNull { it.isNotBlank() }
            if (candidate != null) {
                return AnalysisSectionPayload(
                    title = title,
                    content = candidate
                )
            }
        }

        return sectionNode.asText(null)?.trim()?.ifBlank { null }?.let { content ->
            AnalysisSectionPayload(
                title = fallbackTitle,
                content = content
            )
        }
    }
}

data class HybridConsultingAiResponse(
    val provider: AiProvider,
    val model: String,
    val mode: String,
    val analysisResults: AnalysisResultsPayload,
    val finalAdvice: String,
    val stabilityScore: Int,
    val safetyGuard: SafetyGuardPayload? = null,
    @JsonIgnore
    val rawJson: String
)

data class HybridConsultingPayload(
    val mode: String? = null,
    val analysis_results: AnalysisResultsPayload,
    @JsonAlias("final_advice")
    val overall_summary: String,
    @JsonAlias("risk_score")
    val stability_score: Int,
    val safety_guard: SafetyGuardPayload? = null
)

data class SafetyGuardPayload(
    val applied: Boolean,
    val reason: String,
    val matchedRules: List<String> = emptyList(),
    val fallbackType: String? = null,
    val originalText: String? = null,
    val sanitizedText: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AnalysisResultsPayload(
    @JsonSetter(nulls = Nulls.SKIP)
    val investment_analysis: AnalysisSectionPayload? = null,
    @JsonSetter(nulls = Nulls.SKIP)
    val tarot_analysis: AnalysisSectionPayload? = null,
    @JsonSetter(nulls = Nulls.SKIP)
    val saju_analysis: AnalysisSectionPayload? = null,
    @JsonSetter(nulls = Nulls.SKIP)
    val zodiac_analysis: AnalysisSectionPayload? = null
)

data class AnalysisSectionPayload(
    val title: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiHybridResponse(
    val candidates: List<GeminiHybridCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null,
    val usageMetadata: GeminiUsageMetadata? = null
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

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null
)

private data class GeminiCandidatePayload(
    val jsonText: String,
    val finishReasons: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiResponse(
    val status: String? = null,
    val output: List<OpenAiOutputItem>? = null,
    val usage: OpenAiUsage? = null,
    @JsonAlias("incomplete_details")
    val incompleteDetails: OpenAiIncompleteDetails? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutputItem(
    val type: String? = null,
    val content: List<OpenAiOutputContent>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiOutputContent(
    val type: String? = null,
    val text: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiIncompleteDetails(
    val reason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OpenAiUsage(
    @JsonAlias("input_tokens")
    val inputTokens: Int? = null,
    @JsonAlias("output_tokens")
    val outputTokens: Int? = null,
    @JsonAlias("total_tokens")
    val totalTokens: Int? = null
)

private data class OpenAiCandidatePayload(
    val jsonText: String,
    val finishReasons: List<String>
)
