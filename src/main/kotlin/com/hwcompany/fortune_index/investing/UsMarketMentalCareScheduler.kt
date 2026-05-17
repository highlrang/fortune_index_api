package com.hwcompany.fortune_index.investing

import com.hwcompany.fortune_index.domain.model.UserAccountStatus
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.monitoring.ScheduleMonitoringService
import com.hwcompany.fortune_index.saju.SajuResultRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 미국 주식 개장 전(한국 시간 22:00) 멘탈 케어 알림 준비 스케줄러.
 * 미장 개장 30분 전에 실행하여 활성 사용자별 당일 리스크 지수를 갱신한다.
 * 푸시 알림 인프라 연동 시 prepareDailyRiskSnapshots() 내부에서 발송 로직을 추가한다.
 */
@Component
class UsMarketMentalCareScheduler(
    private val userRepository: UserRepository,
    private val sajuResultRepository: SajuResultRepository,
    private val dailyInvestmentRiskIndexService: DailyInvestmentRiskIndexService,
    private val scheduleMonitoringService: ScheduleMonitoringService
) {
    @Scheduled(cron = "0 0 22 * * MON-FRI", zone = "Asia/Seoul")
    fun prepareDailyRiskSnapshots() {
        val trigger = "us-market-pre-open"
        val activeUsers = userRepository.findByAccountStatus(UserAccountStatus.ACTIVE)
        var preparedCount = 0
        var failedCount = 0
        val jobDetails = mutableMapOf(
            "trigger" to trigger,
            "activeUsers" to activeUsers.size.toString()
        )

        scheduleMonitoringService.markStarted(
            jobName = JOB_NAME,
            trigger = trigger,
            details = jobDetails
        )

        activeUsers.forEach { user ->
            runCatching {
                dailyInvestmentRiskIndexService.computeForUser(requireNotNull(user.id))
                preparedCount++
                // TODO: 푸시 알림 인프라 연동 시 여기서 개인화 알림 발송
            }.onFailure { ex ->
                failedCount++
                logger.warn("Failed to prepare daily risk snapshot. userId={}", user.id, ex)
            }
        }

        jobDetails["preparedCount"] = preparedCount.toString()
        jobDetails["failedCount"] = failedCount.toString()

        if (failedCount > 0) {
            scheduleMonitoringService.markFailed(
                jobName = JOB_NAME,
                trigger = trigger,
                errorMessage = "$failedCount users failed",
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
            "US market pre-open mental care prep completed. activeUsers={}, prepared={}, failed={}",
            activeUsers.size, preparedCount, failedCount
        )
    }

    companion object {
        private const val JOB_NAME = "us-market-mental-care-prep"
        private val logger = LoggerFactory.getLogger(UsMarketMentalCareScheduler::class.java)
    }
}
