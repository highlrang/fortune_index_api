package com.hwcompany.fortune_index.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class HttpClientConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()

    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()
}
