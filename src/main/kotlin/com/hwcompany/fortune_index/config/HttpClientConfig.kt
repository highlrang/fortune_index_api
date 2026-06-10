package com.hwcompany.fortune_index.config

import com.hwcompany.fortune_index.ai.AiAdviceProperties
import java.net.http.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class HttpClientConfig {
    @Bean("geminiRestClientBuilder")
    fun geminiRestClientBuilder(aiAdviceProperties: AiAdviceProperties): RestClient.Builder {
        val props = aiAdviceProperties.gemini
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(props.connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(props.readTimeout)
        }
        return RestClient.builder().requestFactory(requestFactory)
    }

    @Bean("openAiRestClientBuilder")
    fun openAiRestClientBuilder(aiAdviceProperties: AiAdviceProperties): RestClient.Builder {
        val props = aiAdviceProperties.openai
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(props.connectTimeout)
            .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(props.readTimeout)
        }
        return RestClient.builder().requestFactory(requestFactory)
    }
}
