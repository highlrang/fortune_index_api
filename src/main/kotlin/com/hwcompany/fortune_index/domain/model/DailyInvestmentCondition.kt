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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_daily_investment_conditions",
    indexes = [
        Index(
            name = "idx_user_daily_condition_lookup",
            columnList = "user_id, condition_date, deleted, created_at"
        )
    ]
)
data class DailyInvestmentCondition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "condition_date", nullable = false)
    var conditionDate: LocalDate,

    @Column(name = "total_score", nullable = false)
    var totalScore: Int,

    @Column(nullable = false, length = 255)
    var summary: String,

    @Column(name = "daily_ganji", nullable = false, length = 20)
    var dailyGanji: String,

    @Column(name = "saju_score", nullable = false)
    var sajuScore: Int,

    @Column(name = "tarot_card_name", nullable = false, length = 100)
    var tarotCardName: String,

    @Column(name = "tarot_score", nullable = false)
    var tarotScore: Int,

    @Column(nullable = false, length = 20)
    var source: String = SOURCE_USER_ADJUSTED,

    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val SOURCE_USER_ADJUSTED = "USER_ADJUSTED"
    }
}
