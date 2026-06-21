package com.hwcompany.fortune_index.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        if (request.method == "OPTIONS") {
            response.status = HttpServletResponse.SC_OK
            return
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                mapOf(
                    "message" to authException.message.toKoreanOrDefault("인증이 필요합니다."),
                    "status" to HttpServletResponse.SC_UNAUTHORIZED
                )
            )
        )
    }
}

@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: org.springframework.security.access.AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                mapOf(
                    "message" to accessDeniedException.message.toKoreanOrDefault("접근 권한이 없습니다."),
                    "status" to HttpServletResponse.SC_FORBIDDEN
                )
            )
        )
    }
}

private fun String?.toKoreanOrDefault(defaultMessage: String): String =
    this?.takeIf { it.any { char -> char in '가'..'힣' } } ?: defaultMessage
