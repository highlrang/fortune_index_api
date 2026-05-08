package com.hwcompany.fortune_index.config

import com.hwcompany.fortune_index.ai.AiAdviceProperties
import java.net.http.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig {
    @Bean
    fun restClientBuilder(aiAdviceProperties: AiAdviceProperties): RestClient.Builder {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(aiAdviceProperties.gemini.connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(aiAdviceProperties.gemini.readTimeout)
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
    }
}
