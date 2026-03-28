package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.investment.VirtualInvestmentRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class ConsultingPositionSnapshotService(
    private val virtualInvestmentRepository: VirtualInvestmentRepository
) {
    fun getLatestHolding(userId: Long, stockCode: String): ConsultingPositionSnapshot? =
        virtualInvestmentRepository.findTopByUserIdAndStockCodeAndIsHoldingTrueOrderByBoughtAtDesc(
            userId = userId,
            stockCode = stockCode
        )?.let {
            ConsultingPositionSnapshot(
                stockCode = it.stockCode,
                averageBuyPrice = it.averageBuyPrice,
                buyQuantity = it.buyQuantity,
                capturedAt = it.boughtAt
            )
        }
}

data class ConsultingPositionSnapshot(
    val stockCode: String,
    val averageBuyPrice: BigDecimal,
    val buyQuantity: Long,
    val capturedAt: LocalDateTime
)
