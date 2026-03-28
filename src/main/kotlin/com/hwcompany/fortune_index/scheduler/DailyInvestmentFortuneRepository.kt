package com.hwcompany.fortune_index.scheduler

import com.hwcompany.fortune_index.domain.model.DailyInvestmentFortune
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface DailyInvestmentFortuneRepository : JpaRepository<DailyInvestmentFortune, Long> {
    fun existsByFortuneDate(fortuneDate: LocalDate): Boolean
}
