package com.hwcompany.fortune_index.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AiAdviceProperties::class)
class AiAdviceConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()
}
