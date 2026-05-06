package com.hwcompany.fortune_index.auth

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import com.hwcompany.fortune_index.domain.model.EmailVerificationToken
import com.hwcompany.fortune_index.domain.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenValue(tokenValue: String): RefreshToken?
    fun findByUserId(userId: Long): RefreshToken?
}

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, Long> {
    fun findFirstByEmailAndPurposeOrderByCreatedAtDesc(
        email: String,
        purpose: EmailVerificationPurpose
    ): EmailVerificationToken?

    fun findFirstByPurposeAndVerificationCodeOrderByCreatedAtDesc(
        purpose: EmailVerificationPurpose,
        verificationCode: String
    ): EmailVerificationToken?

    fun findFirstByVerificationCodeOrderByCreatedAtDesc(
        verificationCode: String
    ): EmailVerificationToken?

    fun findFirstByEmailAndPurposeAndVerificationCodeOrderByCreatedAtDesc(
        email: String,
        purpose: EmailVerificationPurpose,
        verificationCode: String
    ): EmailVerificationToken?

    fun deleteAllByEmailAndPurposeAndExpiresAtBefore(
        email: String,
        purpose: EmailVerificationPurpose,
        expiresAt: LocalDateTime
    )
}
