package com.hwcompany.fortune_index.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

@Component
class RequestMdcLoggingFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)
        val requestId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        MDC.put(MDC_REQUEST_ID, requestId)
        wrappedResponse.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            requestLogger.info(
                "request completed method={} uri={} query={} status={} durationMs={} requestContentType={} requestBody={} responseContentType={} responseBody={}",
                wrappedRequest.method,
                wrappedRequest.requestURI,
                wrappedRequest.queryString,
                wrappedResponse.status,
                System.currentTimeMillis() - startedAt,
                wrappedRequest.contentType,
                extractRequestBody(wrappedRequest),
                wrappedResponse.contentType,
                extractResponseBody(wrappedResponse)
            )
            wrappedResponse.copyBodyToResponse()
            MDC.clear()
        }
    }

    private fun extractRequestBody(request: ContentCachingRequestWrapper): String {
        if (request.contentAsByteArray.isEmpty()) {
            return ""
        }
        return extractPayload(request.contentAsByteArray, request.contentType, request.characterEncoding)
    }

    private fun extractResponseBody(response: ContentCachingResponseWrapper): String {
        if (response.contentAsByteArray.isEmpty()) {
            return ""
        }
        return extractPayload(response.contentAsByteArray, response.contentType, response.characterEncoding)
    }

    private fun extractPayload(
        content: ByteArray,
        contentType: String?,
        characterEncoding: String?
    ): String {
        if (!isVisibleContentType(contentType)) {
            return "[${content.size} bytes omitted]"
        }

        val charset = characterEncoding?.let(Charset::forName) ?: StandardCharsets.UTF_8
        val payload = String(content, charset)
        val sanitized = sanitizePayload(payload, contentType)
        return truncate(sanitized)
    }

    private fun sanitizePayload(payload: String, contentType: String?): String {
        if (payload.isBlank()) {
            return payload
        }

        if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE, ignoreCase = true)) {
            return sanitizeJson(payload)
        }

        return sanitizePlainText(payload)
    }

    private fun sanitizeJson(payload: String): String {
        val root = runCatching { objectMapper.readTree(payload) }
            .getOrElse { return sanitizePlainText(payload) }
        sanitizeNode(root)
        return objectMapper.writeValueAsString(root)
    }

    private fun sanitizeNode(node: JsonNode) {
        when (node) {
            is ObjectNode -> {
                val fieldNames = node.fieldNames().asSequence().toList()
                fieldNames.forEach { fieldName ->
                    val child = node.get(fieldName)
                    if (isSensitiveField(fieldName)) {
                        node.put(fieldName, MASKED_VALUE)
                    } else {
                        sanitizeNode(child)
                    }
                }
            }

            is ArrayNode -> node.forEach { sanitizeNode(it) }
        }
    }

    private fun sanitizePlainText(payload: String): String {
        var sanitized = payload
        quotedFieldPatterns.forEach { pattern ->
            sanitized = pattern.replace(sanitized) { matchResult ->
                matchResult.groupValues[1] + MASKED_VALUE + matchResult.groupValues[3]
            }
        }
        assignmentFieldPatterns.forEach { pattern ->
            sanitized = pattern.replace(sanitized) { matchResult ->
                matchResult.groupValues[1] + MASKED_VALUE
            }
        }
        return sanitized
    }

    private fun isVisibleContentType(contentType: String?): Boolean {
        if (contentType.isNullOrBlank()) {
            return true
        }
        return visibleContentTypes.any { contentType.startsWith(it, ignoreCase = true) }
    }

    private fun isSensitiveField(fieldName: String): Boolean {
        val normalized = fieldName.lowercase()
        return sensitiveFieldNames.any { normalized.contains(it) }
    }

    private fun truncate(payload: String): String =
        if (payload.length <= MAX_PAYLOAD_LENGTH) {
            payload
        } else {
            payload.take(MAX_PAYLOAD_LENGTH) + "...(truncated)"
        }

    companion object {
        private val requestLogger = LoggerFactory.getLogger(RequestMdcLoggingFilter::class.java)
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        const val MDC_REQUEST_ID = "requestId"
        private const val MAX_PAYLOAD_LENGTH = 4000
        private const val MASKED_VALUE = "***"
        private val visibleContentTypes = listOf(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.TEXT_HTML_VALUE
        )
        private val sensitiveFieldNames = listOf(
            "password",
            "token",
            "secret",
            "authorization",
            "cookie",
            "apikey",
            "api_key"
        )
        private val quotedFieldPatterns = listOf(
            Regex("""((?i)"(?:password|token|secret|authorization|cookie|api[_-]?key)"\s*:\s*")([^"]*)(")"""),
        )
        private val assignmentFieldPatterns = listOf(
            Regex("""((?i)(?:password|token|secret|authorization|cookie|api[_-]?key)\s*=\s*)([^&,\s]+)"""),
            Regex("""((?i)(?:^|[&?])(password|token|secret|authorization|cookie|api[_-]?key)=)([^&]*)""")
        )
    }
}
