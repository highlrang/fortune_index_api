package com.hwcompany.fortune_index.config

import com.hwcompany.fortune_index.ai.AiAdviceProperties
import com.hwcompany.fortune_index.ai.AiProvider
import java.net.http.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig {
    @Bean
    fun restClientBuilder(aiAdviceProperties: AiAdviceProperties): RestClient.Builder {
        val connectTimeout = when (aiAdviceProperties.provider) {
            AiProvider.GEMINI -> aiAdviceProperties.gemini.connectTimeout
            AiProvider.OPENAI -> aiAdviceProperties.openai.connectTimeout
        }
        val readTimeout = when (aiAdviceProperties.provider) {
            AiProvider.GEMINI -> aiAdviceProperties.gemini.readTimeout
            AiProvider.OPENAI -> aiAdviceProperties.openai.readTimeout
        }
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(readTimeout)
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
    }
}
