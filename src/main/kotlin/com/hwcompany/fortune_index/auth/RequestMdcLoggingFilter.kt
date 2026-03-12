package com.hwcompany.fortune_index.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestMdcLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        MDC.put(MDC_REQUEST_ID, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            requestLogger.info(
                "request completed method={} uri={} status={} durationMs={}",
                request.method,
                request.requestURI,
                response.status,
                System.currentTimeMillis() - startedAt
            )
            MDC.clear()
        }
    }

    companion object {
        private val requestLogger = LoggerFactory.getLogger(RequestMdcLoggingFilter::class.java)
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        const val MDC_REQUEST_ID = "requestId"
    }
}
