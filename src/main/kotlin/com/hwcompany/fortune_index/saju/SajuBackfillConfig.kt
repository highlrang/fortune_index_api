package com.hwcompany.fortune_index.saju

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.slf4j.LoggerFactory

@Configuration
@EnableConfigurationProperties(SajuBackfillProperties::class)
class SajuBackfillConfig {
    @Bean
    fun sajuBackfillRunner(
        properties: SajuBackfillProperties,
        sajuPersistenceService: SajuPersistenceService
    ): ApplicationRunner = ApplicationRunner {
        if (!properties.backfill.enabled) {
            return@ApplicationRunner
        }

        val summary = sajuPersistenceService.backfillMissingResults(properties.backfill.batchSize)
        logger.info(
            "Startup saju backfill finished. scannedUsers={}, createdResults={}",
            summary.scannedUsers,
            summary.createdResults
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SajuBackfillConfig::class.java)
    }
}

@ConfigurationProperties(prefix = "app.saju")
data class SajuBackfillProperties(
    var backfill: SajuBackfillJobProperties = SajuBackfillJobProperties()
)

data class SajuBackfillJobProperties(
    var enabled: Boolean = false,
    var batchSize: Int = 500
)
