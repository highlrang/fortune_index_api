package com.hwcompany.fortune_index.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
    private val authenticationEntryPoint: RestAuthenticationEntryPoint
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorization = request.getHeader(AUTHORIZATION_HEADER)
        if (authorization.isNullOrBlank() || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val accessToken = authorization.removePrefix(BEARER_PREFIX).trim()
        val parsed = runCatching { jwtTokenService.parse(accessToken) }
            .getOrElse {
                SecurityContextHolder.clearContext()
                authenticationEntryPoint.commence(request, response, BadCredentialsException("Invalid access token"))
                return
            }

        if (parsed.tokenType != JwtTokenService.TOKEN_TYPE_ACCESS) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(request, response, BadCredentialsException("Invalid access token type"))
            return
        }

        val principal = AuthenticatedUser(
            userId = parsed.userId,
            email = parsed.email
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        SecurityContextHolder.getContext().authentication = authentication
        MDC.put(MDC_USER_ID, parsed.userId.toString())
        filterChain.doFilter(request, response)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        const val MDC_USER_ID = "userId"
    }
}
