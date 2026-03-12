package com.hwcompany.fortune_index.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FortuneScheduler(
    private val schedulerProperties: SchedulerProperties,
    private val dailyInvestmentFortuneService: DailyInvestmentFortuneService,
    private val automatedConsultingSchedulerService: AutomatedConsultingSchedulerService
) {
    @Scheduled(
        cron = "\${app.scheduler.daily-fortune.cron:0 0 9 * * *}",
        zone = "\${app.scheduler.daily-fortune.zone:Asia/Seoul}"
    )
    fun saveDailyFortune() {
        if (!schedulerProperties.dailyFortune.enabled) {
            return
        }

        dailyInvestmentFortuneService.saveTodayFortuneIfAbsent()
    }

    @Scheduled(
        cron = "\${app.scheduler.daily-consulting.only-stock-cron:0 0 10 * * *}",
        zone = "\${app.scheduler.daily-consulting.zone:Asia/Seoul}"
    )
    fun generateOnlyStockConsultings() = generateConsultingsForMode(com.hwcompany.fortune_index.consulting.AnalysisMode.ONLY_STOCK)

    @Scheduled(
        cron = "\${app.scheduler.daily-consulting.stock-saju-cron:0 0 11 * * *}",
        zone = "\${app.scheduler.daily-consulting.zone:Asia/Seoul}"
    )
    fun generateStockSajuConsultings() = generateConsultingsForMode(com.hwcompany.fortune_index.consulting.AnalysisMode.STOCK_SAJU)

    @Scheduled(
        cron = "\${app.scheduler.daily-consulting.stock-tarot-cron:0 0 12 * * *}",
        zone = "\${app.scheduler.daily-consulting.zone:Asia/Seoul}"
    )
    fun generateStockTarotConsultings() = generateConsultingsForMode(com.hwcompany.fortune_index.consulting.AnalysisMode.STOCK_TAROT)

    @Scheduled(
        cron = "\${app.scheduler.daily-consulting.stock-all-cron:0 0 13 * * *}",
        zone = "\${app.scheduler.daily-consulting.zone:Asia/Seoul}"
    )
    fun generateStockAllConsultings() = generateConsultingsForMode(com.hwcompany.fortune_index.consulting.AnalysisMode.STOCK_ALL)

    private fun generateConsultingsForMode(mode: com.hwcompany.fortune_index.consulting.AnalysisMode) {
        if (!schedulerProperties.dailyConsulting.enabled) {
            return
        }

        val result = automatedConsultingSchedulerService.generateDailyConsultings(listOf(mode))
        logger.info(
            "Scheduled consulting completed mode={} users={} created={} skipped={} failed={}",
            mode,
            result.userCount,
            result.createdCount,
            result.skippedCount,
            result.failedCount
        )
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(FortuneScheduler::class.java)
    }
}
