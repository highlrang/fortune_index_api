package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "virtual_investments",
    indexes = [
        Index(name = "idx_virtual_investments_user_id", columnList = "user_id"),
        Index(name = "idx_virtual_investments_stock_code", columnList = "stock_code")
    ]
)
data class VirtualInvestment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "stock_code", nullable = false, length = 20)
    var stockCode: String,

    @Column(name = "average_buy_price", nullable = false, precision = 19, scale = 4)
    var averageBuyPrice: BigDecimal,

    @Column(name = "buy_quantity", nullable = false)
    var buyQuantity: Long,

    @Column(name = "tracked_profit_rate", nullable = false, precision = 7, scale = 4)
    var trackedProfitRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "bought_at", nullable = false)
    var boughtAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_holding", nullable = false)
    var isHolding: Boolean = true
)
