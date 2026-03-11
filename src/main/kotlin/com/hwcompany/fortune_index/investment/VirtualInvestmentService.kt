package com.hwcompany.fortune_index.investment

import com.hwcompany.fortune_index.domain.model.VirtualInvestment
import com.hwcompany.fortune_index.history.UserRepository
import com.hwcompany.fortune_index.market.StockService
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class VirtualInvestmentService(
    private val virtualInvestmentRepository: VirtualInvestmentRepository,
    private val userRepository: UserRepository,
    private val stockService: StockService,
    private val profitCalculator: VirtualInvestmentProfitCalculator
) {
    @Transactional
    fun buy(request: BuyVirtualInvestmentRequest): VirtualInvestmentPositionResponse {
        require(request.stockCode.isNotBlank()) { "stockCode must not be blank" }
        require(request.buyQuantity > 0) { "buyQuantity must be greater than 0" }

        val user = userRepository.findById(request.userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: ${request.userId}") }

        val stockInfo = stockService.getStockInfo(request.stockCode)
        validateRealtimePrice(stockInfo.currentPrice, stockInfo.fallback, request.stockCode)

        val investment = virtualInvestmentRepository.save(
            VirtualInvestment(
                user = user,
                stockCode = stockInfo.ticker,
                averageBuyPrice = stockInfo.currentPrice,
                buyQuantity = request.buyQuantity,
                boughtAt = request.boughtAt,
                isHolding = request.isHolding
            )
        )

        val profit = profitCalculator.calculate(
            currentPrice = stockInfo.currentPrice,
            averageBuyPrice = investment.averageBuyPrice,
            quantity = investment.buyQuantity
        )

        return investment.toPositionResponse(
            currentPrice = stockInfo.currentPrice,
            priceFallback = false,
            profit = profit
        )
    }

    @Transactional(readOnly = true)
    fun getUserVirtualInvestments(userId: Long, holdingOnly: Boolean = false): List<VirtualInvestmentPositionResponse> {
        verifyUserExists(userId)

        val investments = if (holdingOnly) {
            virtualInvestmentRepository.findByUserIdAndIsHoldingTrueOrderByBoughtAtDesc(userId)
        } else {
            virtualInvestmentRepository.findByUserIdOrderByBoughtAtDesc(userId)
        }

        return investments.map { investment ->
            val stockInfo = stockService.getStockInfo(investment.stockCode)
            val currentPrice = if (stockInfo.fallback || stockInfo.currentPrice.signum() <= 0) {
                investment.averageBuyPrice
            } else {
                stockInfo.currentPrice
            }

            investment.toPositionResponse(
                currentPrice = currentPrice,
                priceFallback = stockInfo.fallback || stockInfo.currentPrice.signum() <= 0,
                profit = profitCalculator.calculate(
                    currentPrice = currentPrice,
                    averageBuyPrice = investment.averageBuyPrice,
                    quantity = investment.buyQuantity
                )
            )
        }
    }

    private fun verifyUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "user not found: $userId")
        }
    }

    private fun validateRealtimePrice(price: BigDecimal, fallback: Boolean, stockCode: String) {
        if (fallback || price.signum() <= 0) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "failed to fetch realtime stock price for stockCode=$stockCode"
            )
        }
    }
}

data class BuyVirtualInvestmentRequest(
    val userId: Long,
    val stockCode: String,
    val buyQuantity: Long,
    val boughtAt: LocalDateTime = LocalDateTime.now(),
    val isHolding: Boolean = true
)

data class VirtualInvestmentPositionResponse(
    val id: Long,
    val userId: Long,
    val stockCode: String,
    val averageBuyPrice: BigDecimal,
    val buyQuantity: Long,
    val boughtAt: LocalDateTime,
    val isHolding: Boolean,
    val currentPrice: BigDecimal,
    val currentReturnRate: BigDecimal,
    val evaluationProfit: BigDecimal,
    val priceFallback: Boolean
)

private fun VirtualInvestment.toPositionResponse(
    currentPrice: BigDecimal,
    priceFallback: Boolean,
    profit: VirtualInvestmentProfitResult
): VirtualInvestmentPositionResponse =
    VirtualInvestmentPositionResponse(
        id = requireNotNull(id),
        userId = requireNotNull(user.id),
        stockCode = stockCode,
        averageBuyPrice = averageBuyPrice,
        buyQuantity = buyQuantity,
        boughtAt = boughtAt,
        isHolding = isHolding,
        currentPrice = currentPrice,
        currentReturnRate = profit.currentReturnRate,
        evaluationProfit = profit.evaluationProfit,
        priceFallback = priceFallback
    )
