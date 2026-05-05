package com.hwcompany.fortune_index.monitoring

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScheduleMonitoringService(
    private val scheduleJobStatusRepository: ScheduleJobStatusRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun markStarted(jobName: String, trigger: String, details: Map<String, String> = emptyMap()) {
        val state = scheduleJobStatusRepository.findById(jobName).orElse(ScheduleJobStatus(jobName = jobName))
        state.lastStatus = ScheduleExecutionStatus.RUNNING
        state.lastTrigger = trigger
        state.lastStartedAt = now()
        state.lastErrorMessage = null
        state.lastDetailsJson = toJson(details)
        state.updatedAt = now()
        scheduleJobStatusRepository.save(state)
    }

    @Transactional
    fun markSucceeded(jobName: String, trigger: String, details: Map<String, String> = emptyMap()) {
        val state = scheduleJobStatusRepository.findById(jobName).orElse(ScheduleJobStatus(jobName = jobName))
        state.lastStatus = ScheduleExecutionStatus.SUCCESS
        state.lastTrigger = trigger
        state.lastFinishedAt = now()
        state.successCount += 1
        state.lastErrorMessage = null
        state.lastDetailsJson = toJson(details)
        state.updatedAt = now()
        scheduleJobStatusRepository.save(state)
    }

    @Transactional
    fun markFailed(jobName: String, trigger: String, errorMessage: String, details: Map<String, String> = emptyMap()) {
        val state = scheduleJobStatusRepository.findById(jobName).orElse(ScheduleJobStatus(jobName = jobName))
        state.lastStatus = ScheduleExecutionStatus.FAILURE
        state.lastTrigger = trigger
        state.lastFinishedAt = now()
        state.failureCount += 1
        state.lastErrorMessage = errorMessage.take(1000)
        state.lastDetailsJson = toJson(details)
        state.updatedAt = now()
        scheduleJobStatusRepository.save(state)
    }

    private fun toJson(details: Map<String, String>): String =
        objectMapper.writeValueAsString(details)

    private fun now(): LocalDateTime = LocalDateTime.now(SEOUL_ZONE_ID)

    companion object {
        private val SEOUL_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
