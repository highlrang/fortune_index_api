package com.hwcompany.fortune_index.auth.email

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

fun interface EmailVerificationMailSender {
    fun send(email: String, verificationUrl: String)
}

@Component
class SmtpEmailVerificationMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: EmailVerificationProperties
) : EmailVerificationMailSender {
    override fun send(email: String, verificationUrl: String) {
        val message: MimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, "UTF-8")

        helper.setFrom(properties.fromAddress)
        helper.setTo(email)
        helper.setSubject("Verify your email address")
        helper.setText(
            """
                <p>Please verify your email address by clicking the link below.</p>
                <p><a href="$verificationUrl">$verificationUrl</a></p>
                <p>This link expires in ${properties.expirationMinutes} minutes.</p>
            """.trimIndent(),
            true
        )

        javaMailSender.send(message)
    }
}
