package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "daily_investment_fortunes",
    indexes = [
        Index(name = "uk_daily_investment_fortunes_date", columnList = "fortune_date", unique = true)
    ]
)
data class DailyInvestmentFortune(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "fortune_date", nullable = false, unique = true)
    var fortuneDate: LocalDate,

    @Column(name = "total_score", nullable = false)
    var totalScore: Int,

    @Column(name = "selected_market", nullable = false, length = 30)
    var selectedMarket: String,

    @Column(name = "market_score", nullable = false)
    var marketScore: Int,

    @Column(name = "market_raw_value", nullable = false, precision = 7, scale = 4)
    var marketRawValue: java.math.BigDecimal,

    @Column(name = "saju_score", nullable = false)
    var sajuScore: Int,

    @Column(name = "daily_ganji", nullable = false, length = 20)
    var dailyGanji: String,

    @Column(name = "tarot_score", nullable = false)
    var tarotScore: Int,

    @Column(name = "tarot_card_name", nullable = false, length = 100)
    var tarotCardName: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
