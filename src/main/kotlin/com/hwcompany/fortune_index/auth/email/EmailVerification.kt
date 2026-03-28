package com.hwcompany.fortune_index.auth.email

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_verifications",
    indexes = [
        Index(name = "idx_email_verifications_email_requested_at", columnList = "email, requested_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_email_verifications_token", columnNames = ["token"])
    ]
)
data class EmailVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 320)
    val email: String,

    @Column(nullable = false, length = 128)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EmailVerificationStatus = EmailVerificationStatus.PENDING,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null
) {
    fun isExpired(now: LocalDateTime): Boolean = !expiresAt.isAfter(now)
}

enum class EmailVerificationStatus {
    PENDING,
    VERIFIED
}
