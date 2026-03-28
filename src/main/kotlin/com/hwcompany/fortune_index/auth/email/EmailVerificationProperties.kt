package com.hwcompany.fortune_index.auth.email

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.email.verification")
data class EmailVerificationProperties(
    var baseUrl: String = "https://your-domain.com",
    var verifyPath: String = "/email/verify",
    var successFallbackUrl: String = "https://your-domain.com",
    var deepLinkUrl: String = "yourapp://verify-complete",
    var fromAddress: String = "no-reply@your-domain.com",
    var expirationMinutes: Long = 15
) {
    fun verificationUrl(token: String): String {
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        return "${baseUrl.trimEnd('/')}${normalizePath(verifyPath)}?token=$encodedToken"
    }

    private fun normalizePath(path: String): String = if (path.startsWith("/")) path else "/$path"
}
