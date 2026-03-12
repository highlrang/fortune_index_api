package com.hwcompany.fortune_index.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val requestMdcLoggingFilter: RequestMdcLoggingFilter,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val restAccessDeniedHandler: RestAccessDeniedHandler
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(restAuthenticationEntryPoint)
                it.accessDeniedHandler(restAccessDeniedHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/error",
                    "/api/v1/investment-index",
                    "/api/kis/**",
                    "/api/history/share/**",
                    "/api/auth/**"
                ).permitAll()
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers("/api/**").authenticated()
                it.anyRequest().permitAll()
            }
            .addFilterBefore(requestMdcLoggingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(jwtAuthenticationFilter, RequestMdcLoggingFilter::class.java)

        return http.build()
    }
}
