package com.hwcompany.fortune_index.admin

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AdminProperties::class)
class AdminConfig

@ConfigurationProperties(prefix = "app.admin")
data class AdminProperties(
    var emails: List<String> = emptyList()
) {
    fun isAdmin(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        return emails
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .any { it == normalizedEmail }
    }
}
