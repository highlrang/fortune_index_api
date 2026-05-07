package com.hwcompany.fortune_index.home

import com.hwcompany.fortune_index.domain.model.HomeTarotDrawHistory
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface HomeTarotDrawHistoryRepository : JpaRepository<HomeTarotDrawHistory, Long> {
    fun findByUserIdAndDrawDate(userId: Long, drawDate: LocalDate): HomeTarotDrawHistory?
}
