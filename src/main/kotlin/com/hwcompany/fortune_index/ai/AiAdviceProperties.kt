package com.hwcompany.fortune_index.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai")
data class AiAdviceProperties(
    var provider: AiProvider = AiProvider.OPENAI,
    var openai: OpenAiProperties = OpenAiProperties(),
    var gemini: GeminiProperties = GeminiProperties()
)

data class OpenAiProperties(
    var apiKey: String = "",
    var baseUrl: String = "https://api.openai.com/v1",
    var model: String = "gpt-5-mini"
)

data class GeminiProperties(
    var apiKey: String = "",
    var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    var model: String = "gemini-2.5-flash"
)

enum class AiProvider {
    OPENAI,
    GEMINI
}
