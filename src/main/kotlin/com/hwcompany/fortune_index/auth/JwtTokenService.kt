package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.common.SeoulTime
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
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
        val issuedAtInstant = Instant.now()
        val expiresAtInstant = issuedAtInstant.plusSeconds(authProperties.jwt.accessTokenValidityMinutes * 60)
        val expiresAt = LocalDateTime.ofInstant(expiresAtInstant, SeoulTime.ZONE_ID)
        val token = Jwts.builder()
            .issuer(authProperties.jwt.issuer)
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .claim("email", email)
            .claim("type", TOKEN_TYPE_ACCESS)
            .issuedAt(Date.from(issuedAtInstant))
            .expiration(Date.from(expiresAtInstant))
            .signWith(signingKey)
            .compact()

        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    fun generateRefreshToken(userId: Long, email: String): IssuedToken {
        val issuedAtInstant = Instant.now()
        val expiresAtInstant = issuedAtInstant.plusSeconds(authProperties.jwt.refreshTokenValidityDays * 24 * 60 * 60)
        val expiresAt = LocalDateTime.ofInstant(expiresAtInstant, SeoulTime.ZONE_ID)
        val token = Jwts.builder()
            .issuer(authProperties.jwt.issuer)
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .claim("email", email)
            .claim("type", TOKEN_TYPE_REFRESH)
            .issuedAt(Date.from(issuedAtInstant))
            .expiration(Date.from(expiresAtInstant))
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
            expiresAt = LocalDateTime.ofInstant(claims.expiration.toInstant(), SeoulTime.ZONE_ID)
        )
    }

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
