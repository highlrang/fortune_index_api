package com.hwcompany.fortune_index.auth.email

import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {
    fun findByToken(token: String): EmailVerification?

    fun findTopByEmailOrderByRequestedAtDescIdDesc(email: String): EmailVerification?
}
