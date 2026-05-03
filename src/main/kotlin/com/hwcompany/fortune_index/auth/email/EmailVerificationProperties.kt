package com.hwcompany.fortune_index.auth.email

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.email.verification")
data class EmailVerificationProperties(
    var appName: String = "fortune_index",
    var baseUrl: String = "https://your-domain.com",
    var verifyPath: String = "/email/verify",
    var successFallbackUrl: String = "https://your-domain.com",
    var deepLinkUrl: String = "yourapp://verify-complete",
    var passwordResetFallbackUrl: String = "https://your-domain.com/password-reset",
    var passwordResetDeepLinkUrl: String = "yourapp://password-reset",
    var fromAddress: String = "no-reply@your-domain.com",
    var expirationMinutes: Long = 15
) {
    fun verificationUrl(token: String): String {
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        return "${baseUrl.trimEnd('/')}${normalizePath(verifyPath)}?token=$encodedToken"
    }

    fun signupSuccessRedirectUrl(
        email: String?,
        emailVerificationToken: String
    ): String {
        val baseUrl = successFallbackUrl.trimEnd('/')
        val withStatus = appendQueryParam(baseUrl, "status", "success")
        val withEmail = if (email.isNullOrBlank()) {
            withStatus
        } else {
            appendQueryParam(withStatus, "email", email)
        }
        return appendQueryParam(withEmail, "emailVerificationToken", emailVerificationToken)
    }

    fun passwordResetSuccessRedirectUrl(resetToken: String): String =
        appendQueryParam(passwordResetFallbackUrl.trimEnd('/'), "resetToken", resetToken)

    private fun normalizePath(path: String): String = if (path.startsWith("/")) path else "/$path"

    private fun appendQueryParam(baseUrl: String, name: String, value: String): String {
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
        return "$baseUrl$separator$name=$encodedValue"
    }
}
