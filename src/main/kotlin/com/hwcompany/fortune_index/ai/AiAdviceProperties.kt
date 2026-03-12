package com.hwcompany.fortune_index.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai")
data class AiAdviceProperties(
    var provider: AiProvider = AiProvider.GEMINI,
    var gemini: GeminiProperties = GeminiProperties()
)

data class GeminiProperties(
    var apiKey: String = "",
    var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    var model: String = "gemini-2.5-flash"
)

enum class AiProvider {
    GEMINI
}
