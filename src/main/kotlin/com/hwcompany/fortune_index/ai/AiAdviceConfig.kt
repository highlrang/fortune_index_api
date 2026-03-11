package com.hwcompany.fortune_index.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AiAdviceProperties::class)
class AiAdviceConfig
