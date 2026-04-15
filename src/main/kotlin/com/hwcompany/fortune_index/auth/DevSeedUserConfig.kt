package com.hwcompany.fortune_index.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.LocalTime

@Configuration
@EnableConfigurationProperties(DevSeedUserProperties::class)
class DevSeedUserConfig

@ConfigurationProperties(prefix = "app.dev.seed-user")
data class DevSeedUserProperties(
    var enabled: Boolean = true,
    var email: String = "dev@fortune-index.local",
    var password: String = "Dev1234!",
    var name: String = "개발용 계정",
    var birthDate: LocalDate = LocalDate.of(1990, 1, 1),
    var birthTime: LocalTime = LocalTime.NOON
)
