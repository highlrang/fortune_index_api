package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "investment_logs")
data class InvestmentLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var consultedAt: LocalDateTime = LocalDateTime.now(),

    @Lob
    @Column(nullable = false)
    var consultationSummary: String,

    @ElementCollection
    @CollectionTable(
        name = "investment_log_tarot_cards",
        joinColumns = [JoinColumn(name = "investment_log_id")]
    )
    var tarotCards: MutableList<TarotCardDraw> = mutableListOf(),

    @ElementCollection
    @CollectionTable(
        name = "investment_log_stock_quotes",
        joinColumns = [JoinColumn(name = "investment_log_id")]
    )
    var stockQuotes: MutableList<StockQuoteSnapshot> = mutableListOf()
)
