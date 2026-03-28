package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import com.hwcompany.fortune_index.consulting.AnalysisMode
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ConsultingHistoryRepository : JpaRepository<ConsultingHistory, Long> {
    fun findByUserIdOrderByConsultedAtDesc(userId: Long, pageable: Pageable): Page<ConsultingHistory>
    fun findByUserIdOrderByConsultedAtDesc(userId: Long): List<ConsultingHistory>
    fun findByUserIdAndConsultedAtBetweenOrderByConsultedAtDesc(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ConsultingHistory>
    fun findByIdAndUserId(id: Long, userId: Long): ConsultingHistory?
    fun findByShareKey(shareKey: String): ConsultingHistory?
    fun existsByUserIdAndAnalysisModeAndConsultedAtBetween(
        userId: Long,
        analysisMode: AnalysisMode,
        start: LocalDateTime,
        end: LocalDateTime
    ): Boolean
}
