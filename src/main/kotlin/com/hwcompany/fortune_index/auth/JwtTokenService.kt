package com.hwcompany.fortune_index.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import org.springframework.stereotype.Service

@Service
class JwtTokenService(
    private val authProperties: AuthProperties
) {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(
        authProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8)
    )

    fun generateAccessToken(userId: Long, email: String): IssuedToken {
        val issuedAt = LocalDateTime.now()
        val expiresAt = issuedAt.plusMinutes(authProperties.jwt.accessTokenValidityMinutes)
        val token = Jwts.builder()
            .issuer(authProperties.jwt.issuer)
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .claim("email", email)
            .claim("type", TOKEN_TYPE_ACCESS)
            .issuedAt(Date.from(issuedAt.toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(expiresAt.toInstant(ZoneOffset.UTC)))
            .signWith(signingKey)
            .compact()

        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    fun generateRefreshToken(userId: Long, email: String): IssuedToken {
        val issuedAt = LocalDateTime.now()
        val expiresAt = issuedAt.plusDays(authProperties.jwt.refreshTokenValidityDays)
        val token = Jwts.builder()
            .issuer(authProperties.jwt.issuer)
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .claim("email", email)
            .claim("type", TOKEN_TYPE_REFRESH)
            .issuedAt(Date.from(issuedAt.toInstant(ZoneOffset.UTC)))
            .expiration(Date.from(expiresAt.toInstant(ZoneOffset.UTC)))
            .signWith(signingKey)
            .compact()

        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    fun parse(token: String): JwtPrincipal {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return JwtPrincipal(
            userId = claims.subject.toLong(),
            email = claims.get("email", String::class.java),
            tokenType = claims.get("type", String::class.java),
            expiresAt = claims.expiration.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime()
        )
    }

    fun isRefreshToken(token: String): Boolean = parse(token).tokenType == TOKEN_TYPE_REFRESH

    data class IssuedToken(
        val token: String,
        val expiresAt: LocalDateTime
    )

    data class JwtPrincipal(
        val userId: Long,
        val email: String,
        val tokenType: String,
        val expiresAt: LocalDateTime
    )

    companion object {
        const val TOKEN_TYPE_ACCESS = "access"
        const val TOKEN_TYPE_REFRESH = "refresh"
    }
}
