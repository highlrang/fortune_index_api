package com.hwcompany.fortune_index.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockHttpServletRequest

class SecurityConfigTest {
    private val securityConfig = SecurityConfig(
        jwtAuthenticationFilter = mock(JwtAuthenticationFilter::class.java),
        requestMdcLoggingFilter = mock(RequestMdcLoggingFilter::class.java),
        restAuthenticationEntryPoint = mock(RestAuthenticationEntryPoint::class.java),
        restAccessDeniedHandler = mock(RestAccessDeniedHandler::class.java)
    )

    @Test
    fun `allows any host on port 5173`() {
        val configuration = securityConfig.corsConfigurationSource()
            .getCorsConfiguration(MockHttpServletRequest("OPTIONS", "/api/test"))

        requireNotNull(configuration)
        assertTrue(configuration.allowedOriginPatterns.orEmpty().contains("http://*:[5173]"))
        assertTrue(configuration.allowedOriginPatterns.orEmpty().contains("https://*:[5173]"))
        assertEquals(true, configuration.allowCredentials)
    }
}
