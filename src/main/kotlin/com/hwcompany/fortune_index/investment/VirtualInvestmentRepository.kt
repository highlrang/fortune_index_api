package com.hwcompany.fortune_index.investment

import com.hwcompany.fortune_index.domain.model.VirtualInvestment
import org.springframework.data.jpa.repository.JpaRepository

interface VirtualInvestmentRepository : JpaRepository<VirtualInvestment, Long> {
    fun existsByUserId(userId: Long): Boolean

    fun findByUserIdOrderByBoughtAtDesc(userId: Long): List<VirtualInvestment>

    fun findByUserIdAndIsHoldingTrueOrderByBoughtAtDesc(userId: Long): List<VirtualInvestment>
}
