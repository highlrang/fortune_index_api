package com.hwcompany.fortune_index.auth.email

import com.hwcompany.fortune_index.domain.model.EmailVerificationPurpose
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.stereotype.Component

@Component
class EmailVerificationPageRenderer(
    private val properties: EmailVerificationProperties
) {
    fun render(result: EmailVerificationResult): String = when (result) {
        EmailVerificationResult.SUCCESS -> renderTemplate(SUCCESS_TEMPLATE, null)
        EmailVerificationResult.EXPIRED -> renderTemplate(EXPIRED_TEMPLATE, null)
        EmailVerificationResult.FAILURE -> renderTemplate(FAILURE_TEMPLATE, null)
    }

    fun render(outcome: EmailVerificationOutcome): String = when (outcome.result) {
        EmailVerificationResult.SUCCESS -> renderTemplate(SUCCESS_TEMPLATE, outcome)
        EmailVerificationResult.EXPIRED -> renderTemplate(EXPIRED_TEMPLATE, outcome)
        EmailVerificationResult.FAILURE -> renderTemplate(FAILURE_TEMPLATE, outcome)
    }

    private fun renderTemplate(template: String, outcome: EmailVerificationOutcome?): String =
        template
            .replace("{{appName}}", escapeHtml(properties.appName))
            .replace("{{deepLinkUrl}}", escapeHtml(continueUrl(outcome, deepLink = true)))
            .replace("{{fallbackUrl}}", escapeHtml(continueUrl(outcome, deepLink = false)))
            .replace("{{successTitle}}", escapeHtml(successTitle(outcome)))
            .replace("{{successDescription}}", escapeHtml(successDescription(outcome)))

    private fun continueUrl(outcome: EmailVerificationOutcome?, deepLink: Boolean): String {
        if (outcome?.token.isNullOrBlank()) {
            return if (deepLink) properties.deepLinkUrl else properties.successFallbackUrl
        }

        val baseUrl = when (outcome?.purpose) {
            EmailVerificationPurpose.PASSWORD_RESET ->
                if (deepLink) properties.passwordResetDeepLinkUrl else properties.passwordResetFallbackUrl
            else ->
                if (deepLink) properties.deepLinkUrl else properties.successFallbackUrl
        }
        val queryName = when (outcome?.purpose) {
            EmailVerificationPurpose.PASSWORD_RESET -> "resetToken"
            else -> "emailVerificationToken"
        }
        return appendQueryParam(baseUrl, queryName, outcome?.token.orEmpty())
    }

    private fun appendQueryParam(baseUrl: String, name: String, value: String): String {
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
        return "$baseUrl$separator$name=$encodedValue"
    }

    private fun successTitle(outcome: EmailVerificationOutcome?): String =
        when (outcome?.purpose) {
            EmailVerificationPurpose.PASSWORD_RESET -> "비밀번호 재설정 인증이 완료되었습니다"
            else -> "이메일 인증이 완료되었습니다"
        }

    private fun successDescription(outcome: EmailVerificationOutcome?): String =
        when (outcome?.purpose) {
            EmailVerificationPurpose.PASSWORD_RESET -> "이제 앱 또는 브라우저에서 새 비밀번호를 설정할 수 있습니다."
            else -> "${properties.appName}에서 회원가입을 계속 진행할 수 있습니다. 앱이 설치되어 있다면 바로 열어 이어서 진행해 주세요."
        }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    companion object {
        private val SUCCESS_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>{{appName}} 이메일 인증 완료</title>
                <style>
                  :root {
                    color-scheme: dark light;
                  }

                  * {
                    box-sizing: border-box;
                  }

                  body {
                    margin: 0;
                    min-height: 100vh;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    background:
                      radial-gradient(circle at top left, rgba(223, 188, 95, 0.16), transparent 32%),
                      radial-gradient(circle at bottom right, rgba(173, 136, 255, 0.2), transparent 30%),
                      linear-gradient(135deg, #12162a 0%, #51348a 52%, #101427 100%);
                    color: #f3efff;
                  }

                  .shell {
                    width: 100%;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                  }

                  .card {
                    width: 100%;
                    max-width: 560px;
                    padding: 32px 28px;
                    border-radius: 28px;
                    border: 1px solid rgba(223, 188, 95, 0.28);
                    background: rgba(26, 24, 52, 0.76);
                    backdrop-filter: blur(18px);
                    -webkit-backdrop-filter: blur(18px);
                    box-shadow: 0 24px 60px rgba(6, 8, 18, 0.32);
                  }

                  .brand {
                    margin: 0 0 12px;
                    text-align: center;
                    font-size: 12px;
                    letter-spacing: 0.24em;
                    text-transform: uppercase;
                    color: #dfbe66;
                  }

                  .badge {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 20px;
                    padding: 10px 14px;
                    border-radius: 999px;
                    border: 1px solid rgba(16, 185, 129, 0.28);
                    background: rgba(16, 185, 129, 0.12);
                    color: #9bf2c8;
                    font-size: 12px;
                    font-weight: 700;
                    letter-spacing: 0.12em;
                    text-transform: uppercase;
                  }

                  h1 {
                    margin: 0 0 14px;
                    text-align: center;
                    font-size: 32px;
                    line-height: 1.3;
                  }

                  p {
                    margin: 0;
                    text-align: center;
                    font-size: 15px;
                    line-height: 1.8;
                    color: rgba(241, 239, 255, 0.8);
                  }

                  .actions {
                    margin-top: 28px;
                    display: grid;
                    gap: 12px;
                  }

                  .button {
                    display: block;
                    width: 100%;
                    padding: 16px 20px;
                    border-radius: 18px;
                    text-align: center;
                    text-decoration: none;
                    font-size: 15px;
                    font-weight: 700;
                  }

                  .button-primary {
                    color: #161223;
                    background: linear-gradient(135deg, rgba(223, 188, 95, 0.96) 0%, rgba(234, 213, 158, 0.92) 100%);
                    border: 1px solid rgba(223, 188, 95, 0.58);
                  }

                  .button-secondary {
                    color: #f3efff;
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(223, 188, 95, 0.22);
                  }

                  .hint {
                    margin-top: 18px;
                    font-size: 13px;
                    color: rgba(241, 239, 255, 0.58);
                  }

                  @media (max-width: 640px) {
                    .card {
                      padding: 28px 20px;
                      border-radius: 24px;
                    }

                    h1 {
                      font-size: 28px;
                    }
                  }
                </style>
              </head>
              <body>
                <main class="shell">
                  <section class="card">
                    <p class="brand">{{appName}}</p>
                    <div class="badge">Verification Success</div>
                    <h1>{{successTitle}}</h1>
                    <p>{{successDescription}}</p>

                    <div class="actions">
                      <a class="button button-primary" href="{{deepLinkUrl}}">앱에서 계속하기</a>
                      <a class="button button-secondary" href="{{fallbackUrl}}">브라우저에서 계속하기</a>
                    </div>

                    <p class="hint">앱이 자동으로 열리지 않으면 아래 보조 경로를 이용해 계속 진행하세요.</p>
                  </section>
                </main>
              </body>
            </html>
        """.trimIndent()

        private val EXPIRED_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>{{appName}} 인증 링크 만료</title>
                <style>
                  :root {
                    color-scheme: dark light;
                  }

                  * {
                    box-sizing: border-box;
                  }

                  body {
                    margin: 0;
                    min-height: 100vh;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    background:
                      radial-gradient(circle at top left, rgba(223, 188, 95, 0.12), transparent 32%),
                      radial-gradient(circle at bottom right, rgba(255, 178, 84, 0.14), transparent 28%),
                      linear-gradient(135deg, #12162a 0%, #4f355b 52%, #101427 100%);
                    color: #f3efff;
                  }

                  .shell {
                    width: 100%;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                  }

                  .card {
                    width: 100%;
                    max-width: 560px;
                    padding: 32px 28px;
                    border-radius: 28px;
                    border: 1px solid rgba(245, 158, 11, 0.24);
                    background: rgba(26, 24, 52, 0.76);
                    backdrop-filter: blur(18px);
                    -webkit-backdrop-filter: blur(18px);
                    box-shadow: 0 24px 60px rgba(6, 8, 18, 0.32);
                  }

                  .brand {
                    margin: 0 0 12px;
                    text-align: center;
                    font-size: 12px;
                    letter-spacing: 0.24em;
                    text-transform: uppercase;
                    color: #dfbe66;
                  }

                  .badge {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 20px;
                    padding: 10px 14px;
                    border-radius: 999px;
                    border: 1px solid rgba(245, 158, 11, 0.28);
                    background: rgba(245, 158, 11, 0.14);
                    color: #f6d48f;
                    font-size: 12px;
                    font-weight: 700;
                    letter-spacing: 0.12em;
                    text-transform: uppercase;
                  }

                  h1 {
                    margin: 0 0 14px;
                    text-align: center;
                    font-size: 32px;
                    line-height: 1.3;
                  }

                  p {
                    margin: 0;
                    text-align: center;
                    font-size: 15px;
                    line-height: 1.8;
                    color: rgba(241, 239, 255, 0.8);
                  }

                  .actions {
                    margin-top: 28px;
                    display: grid;
                    gap: 12px;
                  }

                  .button {
                    display: block;
                    width: 100%;
                    padding: 16px 20px;
                    border-radius: 18px;
                    text-align: center;
                    text-decoration: none;
                    font-size: 15px;
                    font-weight: 700;
                  }

                  .button-primary {
                    color: #161223;
                    background: linear-gradient(135deg, rgba(223, 188, 95, 0.96) 0%, rgba(234, 213, 158, 0.92) 100%);
                    border: 1px solid rgba(223, 188, 95, 0.58);
                  }

                  .button-secondary {
                    color: #f3efff;
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(245, 158, 11, 0.22);
                  }

                  .hint {
                    margin-top: 18px;
                    font-size: 13px;
                    color: rgba(241, 239, 255, 0.58);
                  }

                  @media (max-width: 640px) {
                    .card {
                      padding: 28px 20px;
                      border-radius: 24px;
                    }

                    h1 {
                      font-size: 28px;
                    }
                  }
                </style>
              </head>
              <body>
                <main class="shell">
                  <section class="card">
                    <p class="brand">{{appName}}</p>
                    <div class="badge">Link Expired</div>
                    <h1>인증 링크가 만료되었습니다</h1>
                    <p>보안을 위해 인증 링크는 일정 시간이 지나면 사용할 수 없습니다. 아래 경로로 돌아가 새 인증 메일을 요청해 주세요.</p>

                    <div class="actions">
                      <a class="button button-primary" href="{{fallbackUrl}}">다시 인증 요청하기</a>
                      <a class="button button-secondary" href="{{deepLinkUrl}}">앱 열기</a>
                    </div>

                    <p class="hint">앱을 사용 중이라면 앱을 다시 열어 인증 절차를 이어갈 수 있습니다.</p>
                  </section>
                </main>
              </body>
            </html>
        """.trimIndent()

        private val FAILURE_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="ko">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>{{appName}} 인증 실패</title>
                <style>
                  :root {
                    color-scheme: dark light;
                  }

                  * {
                    box-sizing: border-box;
                  }

                  body {
                    margin: 0;
                    min-height: 100vh;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    background:
                      radial-gradient(circle at top left, rgba(223, 188, 95, 0.1), transparent 32%),
                      radial-gradient(circle at bottom right, rgba(239, 68, 68, 0.14), transparent 28%),
                      linear-gradient(135deg, #12162a 0%, #4d2948 52%, #101427 100%);
                    color: #f3efff;
                  }

                  .shell {
                    width: 100%;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 24px;
                  }

                  .card {
                    width: 100%;
                    max-width: 560px;
                    padding: 32px 28px;
                    border-radius: 28px;
                    border: 1px solid rgba(239, 68, 68, 0.24);
                    background: rgba(26, 24, 52, 0.76);
                    backdrop-filter: blur(18px);
                    -webkit-backdrop-filter: blur(18px);
                    box-shadow: 0 24px 60px rgba(6, 8, 18, 0.32);
                  }

                  .brand {
                    margin: 0 0 12px;
                    text-align: center;
                    font-size: 12px;
                    letter-spacing: 0.24em;
                    text-transform: uppercase;
                    color: #dfbe66;
                  }

                  .badge {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    margin: 0 auto 20px;
                    padding: 10px 14px;
                    border-radius: 999px;
                    border: 1px solid rgba(239, 68, 68, 0.28);
                    background: rgba(239, 68, 68, 0.14);
                    color: #ffb4b4;
                    font-size: 12px;
                    font-weight: 700;
                    letter-spacing: 0.12em;
                    text-transform: uppercase;
                  }

                  h1 {
                    margin: 0 0 14px;
                    text-align: center;
                    font-size: 32px;
                    line-height: 1.3;
                  }

                  p {
                    margin: 0;
                    text-align: center;
                    font-size: 15px;
                    line-height: 1.8;
                    color: rgba(241, 239, 255, 0.8);
                  }

                  .actions {
                    margin-top: 28px;
                    display: grid;
                    gap: 12px;
                  }

                  .button {
                    display: block;
                    width: 100%;
                    padding: 16px 20px;
                    border-radius: 18px;
                    text-align: center;
                    text-decoration: none;
                    font-size: 15px;
                    font-weight: 700;
                  }

                  .button-primary {
                    color: #161223;
                    background: linear-gradient(135deg, rgba(223, 188, 95, 0.96) 0%, rgba(234, 213, 158, 0.92) 100%);
                    border: 1px solid rgba(223, 188, 95, 0.58);
                  }

                  .button-secondary {
                    color: #f3efff;
                    background: rgba(255, 255, 255, 0.08);
                    border: 1px solid rgba(239, 68, 68, 0.22);
                  }

                  .hint {
                    margin-top: 18px;
                    font-size: 13px;
                    color: rgba(241, 239, 255, 0.58);
                  }

                  @media (max-width: 640px) {
                    .card {
                      padding: 28px 20px;
                      border-radius: 24px;
                    }

                    h1 {
                      font-size: 28px;
                    }
                  }
                </style>
              </head>
              <body>
                <main class="shell">
                  <section class="card">
                    <p class="brand">{{appName}}</p>
                    <div class="badge">Verification Failed</div>
                    <h1>인증을 완료하지 못했습니다</h1>
                    <p>링크가 올바르지 않거나 이미 처리된 요청일 수 있습니다. 아래 경로로 돌아가 다시 시도해 주세요.</p>

                    <div class="actions">
                      <a class="button button-primary" href="{{fallbackUrl}}">다시 시도하기</a>
                      <a class="button button-secondary" href="{{deepLinkUrl}}">앱 열기</a>
                    </div>

                    <p class="hint">문제가 계속되면 앱 또는 웹에서 새 인증 요청을 진행해 주세요.</p>
                  </section>
                </main>
              </body>
            </html>
        """.trimIndent()
    }
}

enum class EmailVerificationResult {
    SUCCESS,
    EXPIRED,
    FAILURE
}
