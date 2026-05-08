package com.hwcompany.fortune_index.ai

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai")
data class AiAdviceProperties(
    var provider: AiProvider = AiProvider.GEMINI,
    var gemini: GeminiProperties = GeminiProperties()
)

data class GeminiProperties(
    var apiKey: String = "",
    var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    var model: String = "gemini-2.5-flash",
    var maxOutputTokens: Int = 4096,
    var retryMaxOutputTokens: Int = 8192,
    var logUsageMetadata: Boolean = true,
    var connectTimeout: Duration = Duration.ofSeconds(3),
    var readTimeout: Duration = Duration.ofSeconds(60)
)

enum class AiProvider {
    GEMINI
}
