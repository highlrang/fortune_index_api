package com.hwcompany.fortune_index.monitoring

import org.springframework.data.jpa.repository.JpaRepository

interface ScheduleJobStatusRepository : JpaRepository<ScheduleJobStatus, String>
