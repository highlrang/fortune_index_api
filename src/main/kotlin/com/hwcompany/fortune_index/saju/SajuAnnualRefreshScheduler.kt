package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.monitoring.ScheduleMonitoringService
import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SajuAnnualRefreshScheduler(
    private val userRepository: UserRepository,
    private val sajuResultRepository: SajuResultRepository,
    private val sajuPersistenceService: SajuPersistenceService,
    private val scheduleMonitoringService: ScheduleMonitoringService
) {
    @EventListener(ApplicationReadyEvent::class)
    fun catchUpAnnualProfileSajuSnapshotsOnStartup() {
        refreshOutdatedProfileSajuSnapshots("startup")
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    fun refreshAnnualProfileSajuSnapshots() {
        refreshOutdatedProfileSajuSnapshots("daily-schedule")
    }

    private fun refreshOutdatedProfileSajuSnapshots(trigger: String) {
        val currentYear = LocalDate.now(SEOUL_ZONE_ID).year
        val activeUsers = userRepository.findByAccountStatus(UserAccountStatus.ACTIVE)
        var refreshedUsers = 0
        var failedUsers = 0
        val jobDetails = mutableMapOf(
            "trigger" to trigger,
            "currentYear" to currentYear.toString(),
            "activeUsers" to activeUsers.size.toString()
        )

        scheduleMonitoringService.markStarted(
            jobName = JOB_NAME,
            trigger = trigger,
            details = jobDetails
        )

        activeUsers.forEach { user ->
            runCatching {
                val latestResult = sajuResultRepository.findTopByUserIdOrderByAnalyzedAtDesc(requireNotNull(user.id))
                if (latestResult == null || latestResult.analyzedAt.year < currentYear) {
                    sajuPersistenceService.refreshResult(user)
                    refreshedUsers += 1
                }
            }.onFailure { ex ->
                failedUsers += 1
                logger.warn("Failed to refresh annual saju snapshot. userId={}, trigger={}", user.id, trigger, ex)
            }
        }

        jobDetails["refreshedUsers"] = refreshedUsers.toString()
        jobDetails["failedUsers"] = failedUsers.toString()

        if (failedUsers > 0) {
            scheduleMonitoringService.markFailed(
                jobName = JOB_NAME,
                trigger = trigger,
                errorMessage = "$failedUsers user refreshes failed",
                details = jobDetails
            )
        } else {
            scheduleMonitoringService.markSucceeded(
                jobName = JOB_NAME,
                trigger = trigger,
                details = jobDetails
            )
        }

        logger.info(
            "Annual saju snapshot refresh completed. trigger={}, activeUsers={}, refreshedUsers={}, failedUsers={}, currentYear={}",
            trigger,
            activeUsers.size,
            refreshedUsers,
            failedUsers,
            currentYear
        )
    }

    companion object {
        private const val JOB_NAME = "saju-annual-refresh"
        private val logger = LoggerFactory.getLogger(SajuAnnualRefreshScheduler::class.java)
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
