package com.hwcompany.fortune_index.auth.email

import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class EmailVerificationService(
    private val emailVerificationRepository: EmailVerificationRepository,
    private val emailVerificationMailSender: EmailVerificationMailSender,
    private val emailVerificationTokenGenerator: EmailVerificationTokenGenerator,
    private val properties: EmailVerificationProperties
) {
    @Transactional
    fun requestVerification(email: String) {
        val normalizedEmail = normalizeEmail(email)
        val latestVerification = emailVerificationRepository.findTopByEmailOrderByRequestedAtDescIdDesc(normalizedEmail)

        if (latestVerification?.status == EmailVerificationStatus.VERIFIED) {
            return
        }

        val now = LocalDateTime.now()
        val token = emailVerificationTokenGenerator.generate()
        val verification = emailVerificationRepository.save(
            EmailVerification(
                email = normalizedEmail,
                token = token,
                status = EmailVerificationStatus.PENDING,
                requestedAt = now,
                expiresAt = now.plusMinutes(properties.expirationMinutes)
            )
        )

        emailVerificationMailSender.send(verification.email, properties.verificationUrl(token))
    }

    @Transactional(readOnly = true)
    fun getLatestStatus(email: String): EmailVerificationStatus {
        val normalizedEmail = normalizeEmail(email)
        return emailVerificationRepository.findTopByEmailOrderByRequestedAtDescIdDesc(normalizedEmail)?.status
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "email verification not found: $normalizedEmail"
            )
    }

    @Transactional
    fun verifyToken(token: String): EmailVerificationResult {
        val verification = emailVerificationRepository.findByToken(token) ?: return EmailVerificationResult.FAILURE
        val latestVerification = emailVerificationRepository.findTopByEmailOrderByRequestedAtDescIdDesc(verification.email)
            ?: return EmailVerificationResult.FAILURE

        if (latestVerification.id != verification.id) {
            return EmailVerificationResult.EXPIRED
        }

        if (verification.status == EmailVerificationStatus.VERIFIED) {
            return EmailVerificationResult.SUCCESS
        }

        val now = LocalDateTime.now()
        if (verification.isExpired(now)) {
            return EmailVerificationResult.EXPIRED
        }

        verification.status = EmailVerificationStatus.VERIFIED
        verification.verifiedAt = now
        return EmailVerificationResult.SUCCESS
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase()
}
