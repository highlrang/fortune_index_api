package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_refresh_tokens_token_value", columnList = "token_value", unique = true)
    ]
)
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_value", nullable = false, unique = true, length = 1000)
    var tokenValue: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: RefreshTokenStatus = RefreshTokenStatus.ACTIVE,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = SeoulTime.now()
)

@Entity
@Table(
    name = "email_verification_tokens",
    indexes = [
        Index(name = "idx_email_verification_tokens_email", columnList = "email"),
        Index(name = "idx_email_verification_tokens_code", columnList = "verification_code")
    ]
)
data class EmailVerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 120)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    var purpose: EmailVerificationPurpose,

    @Column(name = "verification_code", nullable = false, length = 20)
    var verificationCode: String,

    @Column(name = "verified", nullable = false)
    var verified: Boolean = false,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = SeoulTime.now()
)
