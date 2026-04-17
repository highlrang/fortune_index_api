package com.hwcompany.fortune_index.condition

import com.hwcompany.fortune_index.domain.model.DailyInvestmentCondition
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface InvestmentConditionRepository : JpaRepository<DailyInvestmentCondition, Long> {
    fun findTopByUserIdAndConditionDateAndDeletedFalseOrderByCreatedAtDescIdDesc(
        userId: Long,
        conditionDate: LocalDate
    ): DailyInvestmentCondition?

    fun findAllByUserIdAndConditionDateAndDeletedFalse(
        userId: Long,
        conditionDate: LocalDate
    ): List<DailyInvestmentCondition>
}
