package com.hwcompany.fortune_index.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
                message = ex.reason ?: status.toString()
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
                message = ex.message ?: "AI 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            )
        )
    }
}

data class ApiErrorResponse(
    val status: Int,
    val message: String
)
