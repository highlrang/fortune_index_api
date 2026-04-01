package com.hwcompany.fortune_index.auth.email

import kotlin.test.Test
import kotlin.test.assertTrue

class EmailVerificationPageRendererTest {
    private val renderer = EmailVerificationPageRenderer(
        EmailVerificationProperties(
            appName = "fortune_index",
            successFallbackUrl = "https://example.com/fallback",
            deepLinkUrl = "yourapp://verify-complete"
        )
    )

    @Test
    fun `성공 페이지는 딥링크와 fallback 링크를 포함한다`() {
        val html = renderer.render(EmailVerificationResult.SUCCESS)

        assertTrue(html.contains("yourapp://verify-complete"))
        assertTrue(html.contains("https://example.com/fallback"))
        assertTrue(html.contains("이메일 인증이 완료되었습니다"))
        assertTrue(html.contains("fortune_index"))
    }
}
