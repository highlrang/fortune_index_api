package com.hwcompany.fortune_index.history

import com.hwcompany.fortune_index.domain.model.ConsultingHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ConsultingHistoryRepository : JpaRepository<ConsultingHistory, Long> {
    fun findByUserIdOrderByConsultedAtDesc(userId: Long, pageable: Pageable): Page<ConsultingHistory>
    fun findByIdAndUserId(id: Long, userId: Long): ConsultingHistory?
}
