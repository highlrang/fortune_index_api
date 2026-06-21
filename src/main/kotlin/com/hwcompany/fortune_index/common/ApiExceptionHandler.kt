package com.hwcompany.fortune_index.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ApiErrorResponse> {
        val status = ex.statusCode
        return ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                message = ex.reason ?: status.defaultMessage()
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                message = ex.message.toKoreanOrDefault("요청 값이 올바르지 않습니다.")
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> {
        logger.error("AI service error: {}", ex.message, ex)
        val status = HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                message = ex.message.toKoreanOrDefault("AI 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            )
        )
    }

    private fun HttpStatusCode.defaultMessage(): String =
        when (value()) {
            400 -> "요청이 올바르지 않습니다."
            401 -> "인증이 필요합니다."
            403 -> "접근 권한이 없습니다."
            404 -> "요청한 리소스를 찾을 수 없습니다."
            409 -> "요청이 현재 상태와 충돌합니다."
            501 -> "아직 지원하지 않는 기능입니다."
            503 -> "서비스를 일시적으로 사용할 수 없습니다."
            else -> "요청을 처리하는 중 오류가 발생했습니다."
        }

    private fun String?.toKoreanOrDefault(defaultMessage: String): String =
        this?.takeIf { it.any { char -> char in '가'..'힣' } } ?: defaultMessage
}

data class ApiErrorResponse(
    val status: Int,
    val message: String
)
