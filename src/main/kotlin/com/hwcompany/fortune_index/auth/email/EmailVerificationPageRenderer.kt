package com.hwcompany.fortune_index.auth.email

import org.springframework.stereotype.Component

@Component
class EmailVerificationPageRenderer(
    private val properties: EmailVerificationProperties
) {
    fun render(result: EmailVerificationResult): String = when (result) {
        EmailVerificationResult.SUCCESS -> successPage()
        EmailVerificationResult.EXPIRED -> statusPage(
            title = "Verification link expired",
            message = "This verification link is no longer valid. Request a new email verification link and try again."
        )
        EmailVerificationResult.FAILURE -> statusPage(
            title = "Verification failed",
            message = "This verification link is invalid. Check the latest email you received and try again."
        )
    }

    private fun successPage(): String {
        val deepLinkUrl = escapeHtml(properties.deepLinkUrl)
        val fallbackUrl = escapeHtml(properties.successFallbackUrl)
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="refresh" content="2;url=$fallbackUrl">
                <title>Email verified</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f4f7fb; color: #14213d; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                    main { max-width: 420px; padding: 32px; background: #ffffff; border-radius: 18px; box-shadow: 0 20px 60px rgba(20, 33, 61, 0.12); text-align: center; }
                    h1 { margin-top: 0; margin-bottom: 12px; font-size: 28px; }
                    p { margin: 0 0 16px; line-height: 1.5; }
                    a { color: #0057ff; }
                </style>
            </head>
            <body>
                <main>
                    <h1>Email verified</h1>
                    <p>Your email verification is complete.</p>
                    <p>Opening the app now. If nothing happens, <a href="$deepLinkUrl">tap here</a>.</p>
                </main>
                <script>
                    window.location.href = ${toJsString(properties.deepLinkUrl)};
                    window.setTimeout(function () {
                        window.location.replace(${toJsString(properties.successFallbackUrl)});
                    }, 2000);
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun statusPage(title: String, message: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${escapeHtml(title)}</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f4f7fb; color: #14213d; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
                main { max-width: 420px; padding: 32px; background: #ffffff; border-radius: 18px; box-shadow: 0 20px 60px rgba(20, 33, 61, 0.12); text-align: center; }
                h1 { margin-top: 0; margin-bottom: 12px; font-size: 28px; }
                p { margin: 0; line-height: 1.5; }
            </style>
        </head>
        <body>
            <main>
                <h1>${escapeHtml(title)}</h1>
                <p>${escapeHtml(message)}</p>
            </main>
        </body>
        </html>
    """.trimIndent()

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun toJsString(value: String): String = "'" + value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "'"
}

enum class EmailVerificationResult {
    SUCCESS,
    EXPIRED,
    FAILURE
}
