package com.hwcompany.fortune_index.auth.email

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose

data class EmailVerificationOutcome(
    val result: EmailVerificationResult,
    val email: String? = null,
    val purpose: EmailVerificationPurpose? = null,
    val token: String? = null
)

enum class EmailVerificationStatus {
    PENDING,
    VERIFIED
}
