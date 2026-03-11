package com.hwcompany.fortune_index.market

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StockMarketProperties::class)
class StockMarketConfig
