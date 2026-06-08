package com.hwcompany.fortune_index.monitoring

import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "schedule_job_status")
data class ScheduleJobStatus(
    @Id
    @Column(name = "job_name", nullable = false, length = 100)
    var jobName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", nullable = false, length = 20)
    var lastStatus: ScheduleExecutionStatus = ScheduleExecutionStatus.NEVER_RUN,

    @Column(name = "last_trigger", length = 50)
    var lastTrigger: String? = null,

    @Column(name = "last_started_at")
    var lastStartedAt: LocalDateTime? = null,

    @Column(name = "last_finished_at")
    var lastFinishedAt: LocalDateTime? = null,

    @Column(name = "success_count", nullable = false)
    var successCount: Long = 0,

    @Column(name = "failure_count", nullable = false)
    var failureCount: Long = 0,

    @Column(name = "last_error_message", length = 1000)
    var lastErrorMessage: String? = null,

    @Column(name = "last_details_json", nullable = false, columnDefinition = "TEXT")
    var lastDetailsJson: String = "{}",

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = SeoulTime.now()
)

enum class ScheduleExecutionStatus {
    NEVER_RUN,
    RUNNING,
    SUCCESS,
    FAILURE
}
