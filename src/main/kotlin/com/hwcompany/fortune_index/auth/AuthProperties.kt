package com.hwcompany.fortune_index.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    var jwt: JwtProperties = JwtProperties(),
    var email: EmailAuthProperties = EmailAuthProperties()
)

data class JwtProperties(
    var issuer: String = "fortune-index-api",
    var secret: String = "change-me-change-me-change-me-change-me",
    var accessTokenValidityMinutes: Long = 5,
    var refreshTokenValidityDays: Long = 14
)

data class EmailAuthProperties(
    var codeValidityMinutes: Long = 10,
    var fromAddress: String = "no-reply@fortune-index.local"
)
