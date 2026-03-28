package com.hwcompany.fortune_index.market

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(StockMarketProperties::class)
class StockMarketConfig {
    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()
}
