package com.hwcompany.fortune_index.scheduler

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SchedulerProperties::class)
class SchedulerConfig
