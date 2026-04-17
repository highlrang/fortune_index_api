package com.hwcompany.fortune_index.common

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {
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
}

data class ApiErrorResponse(
    val status: Int,
    val message: String
)
