package com.hwcompany.fortune_index.auth.email

import java.security.SecureRandom
import java.util.Base64
import org.springframework.stereotype.Component

fun interface EmailVerificationTokenGenerator {
    fun generate(): String
}

@Component
class SecureEmailVerificationTokenGenerator : EmailVerificationTokenGenerator {
    private val secureRandom = SecureRandom()

    override fun generate(): String {
        val buffer = ByteArray(32)
        secureRandom.nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }
}
