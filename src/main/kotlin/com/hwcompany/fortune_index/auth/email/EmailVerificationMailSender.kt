package com.hwcompany.fortune_index.auth.email

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

interface EmailVerificationMailSender {
    fun send(email: String, verificationUrl: String)

    fun send(email: String, verificationUrl: String, purpose: EmailVerificationPurpose) {
        send(email, verificationUrl)
    }
}

@Component
class SmtpEmailVerificationMailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: EmailVerificationProperties
) : EmailVerificationMailSender {
    override fun send(email: String, verificationUrl: String) {
        send(email, verificationUrl, EmailVerificationPurpose.SIGNUP)
    }

    override fun send(email: String, verificationUrl: String, purpose: EmailVerificationPurpose) {
        val message: MimeMessage = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, "UTF-8")

        helper.setFrom(properties.fromAddress)
        helper.setTo(email)
        helper.setSubject(subjectFor(purpose))
        helper.setText(renderEmailHtml(verificationUrl, purpose), true)

        javaMailSender.send(message)
    }

    private fun subjectFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "${properties.appName} 이메일 인증"
            EmailVerificationPurpose.PASSWORD_RESET -> "${properties.appName} 비밀번호 변경 인증"
        }

    private fun renderEmailHtml(verificationUrl: String, purpose: EmailVerificationPurpose): String =
        EMAIL_TEMPLATE
            .replace("{{appName}}", escapeHtml(properties.appName))
            .replace("{{emailTitle}}", escapeHtml(emailTitleFor(purpose)))
            .replace("{{expirationMinutes}}", properties.expirationMinutes.toString())
            .replace("{{verificationUrl}}", escapeHtmlAttribute(verificationUrl))
            .replace("{{preheader}}", escapeHtml(preheaderFor(purpose)))
            .replace("{{badge}}", escapeHtml(badgeFor(purpose)))
            .replace("{{title}}", escapeHtml(titleFor(purpose)))
            .replace("{{description}}", escapeHtml(descriptionFor(purpose)))
            .replace("{{buttonText}}", escapeHtml(buttonTextFor(purpose)))
            .replace("{{notice}}", escapeHtml(noticeFor(purpose)))

    private fun emailTitleFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "${properties.appName} 이메일 인증"
            EmailVerificationPurpose.PASSWORD_RESET -> "${properties.appName} 비밀번호 변경 인증"
        }

    private fun preheaderFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "${properties.appName} 회원가입을 완료하려면 이메일 인증을 진행해주세요."
            EmailVerificationPurpose.PASSWORD_RESET -> "${properties.appName} 비밀번호 변경을 완료하려면 이메일 인증을 진행해주세요."
        }

    private fun badgeFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "Email Verification"
            EmailVerificationPurpose.PASSWORD_RESET -> "Password Verification"
        }

    private fun titleFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "이메일 인증을 완료해주세요"
            EmailVerificationPurpose.PASSWORD_RESET -> "비밀번호 변경을 인증해주세요"
        }

    private fun descriptionFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP ->
                "${properties.appName} 회원가입을 계속하려면 아래 버튼을 눌러 이메일 인증을 완료해 주세요. 인증 링크는 발송 후 ${properties.expirationMinutes}분 동안 유효합니다."
            EmailVerificationPurpose.PASSWORD_RESET ->
                "${properties.appName} 계정의 비밀번호를 변경하려면 아래 버튼을 눌러 이메일 인증을 완료해 주세요. 인증 링크는 발송 후 ${properties.expirationMinutes}분 동안 유효합니다."
        }

    private fun buttonTextFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "이메일 인증하기"
            EmailVerificationPurpose.PASSWORD_RESET -> "비밀번호 변경 인증하기"
        }

    private fun noticeFor(purpose: EmailVerificationPurpose): String =
        when (purpose) {
            EmailVerificationPurpose.SIGNUP -> "본인이 요청하지 않은 인증 메일이라면 이 이메일을 무시하셔도 됩니다."
            EmailVerificationPurpose.PASSWORD_RESET ->
                "본인이 요청하지 않은 비밀번호 변경 메일이라면 이 이메일을 무시하고, 계정 보호를 위해 기존 비밀번호를 점검해 주세요."
        }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun escapeHtmlAttribute(value: String): String = escapeHtml(value)

    companion object {
        private val EMAIL_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko" xmlns="http://www.w3.org/1999/xhtml">
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <meta name="x-apple-disable-message-reformatting" />
                <meta name="format-detection" content="telephone=no,address=no,email=no,date=no,url=no" />
                <title>{{emailTitle}}</title>
              </head>
              <body style="margin:0; padding:0; background-color:#0f1326; word-break:keep-all;">
                <span style="display:none !important; visibility:hidden; opacity:0; color:transparent; height:0; width:0; overflow:hidden; mso-hide:all;">
                  {{preheader}}
                </span>

                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="border-collapse:collapse; width:100%; background-color:#0f1326; margin:0; padding:0;">
                  <tr>
                    <td align="center" style="padding:32px 16px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="border-collapse:collapse; width:100%; max-width:600px;">
                        <tr>
                          <td align="center" style="padding:0 0 16px 0; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:13px; line-height:20px; color:#d9c27b; letter-spacing:0.24em; text-transform:uppercase;">
                            {{appName}}
                          </td>
                        </tr>

                        <tr>
                          <td style="background-color:#171d36; border:1px solid #3f335f; border-radius:24px; padding:0;">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="border-collapse:collapse; width:100%;">
                              <tr>
                                <td style="padding:40px 32px 16px 32px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;">
                                  <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse;">
                                    <tr>
                                      <td style="padding:10px 14px; border:1px solid #6b588d; border-radius:999px; background-color:#21193a; font-size:12px; line-height:12px; color:#dfbe66; font-weight:700; letter-spacing:0.12em; text-transform:uppercase;">
                                        {{badge}}
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:0 32px 12px 32px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:30px; line-height:40px; font-weight:700; color:#f4f1ff;">
                                  {{title}}
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:0 32px 24px 32px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:15px; line-height:26px; color:#cdc6e7;">
                                  {{description}}
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:0 32px 28px 32px;">
                                  <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="border-collapse:separate;">
                                    <tr>
                                      <td align="center" bgcolor="#d9bb6a" style="border-radius:999px;">
                                        <a
                                          href="{{verificationUrl}}"
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          style="display:inline-block; padding:16px 28px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:16px; line-height:16px; font-weight:700; color:#161223; text-decoration:none; border-radius:999px; background-color:#d9bb6a;"
                                        >
                                          {{buttonText}}
                                        </a>
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:0 32px 24px 32px;">
                                  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="border-collapse:collapse; width:100%; background-color:#1d2443; border:1px solid #332b52; border-radius:18px;">
                                    <tr>
                                      <td style="padding:18px 20px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:13px; line-height:22px; color:#bdb5d7;">
                                        버튼이 동작하지 않으면 아래 링크를 복사해 브라우저에 붙여 넣어주세요.<br />
                                        <a href="{{verificationUrl}}" target="_blank" rel="noopener noreferrer" style="color:#f0d892; text-decoration:underline; word-break:break-all;">{{verificationUrl}}</a>
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:0 32px 32px 32px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:13px; line-height:22px; color:#8f88ab;">
                                  {{notice}}
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <tr>
                          <td align="center" style="padding:18px 16px 0 16px; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size:12px; line-height:20px; color:#7e7898;">
                            © {{appName}}. All rights reserved.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
