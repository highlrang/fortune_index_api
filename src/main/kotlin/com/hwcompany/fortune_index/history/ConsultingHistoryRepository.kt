package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import com.hwcompany.fortune_index.consulting.AnalysisMode
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ConsultingHistoryRepository : JpaRepository<ConsultingHistory, Long> {
    fun findByUserIdOrderByConsultedAtDesc(userId: Long, pageable: Pageable): Page<ConsultingHistory>
    fun findByUserIdOrderByConsultedAtDesc(userId: Long): List<ConsultingHistory>
    fun findByUserIdAndLikedAtIsNotNullOrderByConsultedAtDesc(
        userId: Long,
        pageable: Pageable
    ): Page<ConsultingHistory>
    fun findByUserIdAndConsultedAtBetweenOrderByConsultedAtDesc(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ConsultingHistory>
    fun findByIdAndUserId(id: Long, userId: Long): ConsultingHistory?
    fun existsByUserIdAndAnalysisModeAndConsultedAtBetween(
        userId: Long,
        analysisMode: AnalysisMode,
        start: LocalDateTime,
        end: LocalDateTime
    ): Boolean

    @Query(
        value = "SELECT DISTINCT DATE(consulted_at) FROM consulting_history WHERE user_id = :userId ORDER BY DATE(consulted_at) DESC",
        countQuery = "SELECT COUNT(DISTINCT DATE(consulted_at)) FROM consulting_history WHERE user_id = :userId",
        nativeQuery = true
    )
    fun findDistinctConsultedDatesByUserId(userId: Long, pageable: Pageable): Page<LocalDate>
}
