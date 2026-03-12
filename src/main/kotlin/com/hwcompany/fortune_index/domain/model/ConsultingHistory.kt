package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.consulting.AnalysisMode
import com.hwcompany.fortune_index.consulting.ConsultingScenario
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "consulting_histories")
data class ConsultingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_mode", nullable = false, length = 30)
    var analysisMode: AnalysisMode = AnalysisMode.STOCK_ALL,

    @Enumerated(EnumType.STRING)
    @Column(name = "consulting_scenario", length = 30)
    var scenario: ConsultingScenario? = null,

    @Column(nullable = false)
    var consultedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "selected_stock_name", nullable = false, length = 100)
    var selectedStockName: String,

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "ticker", column = Column(name = "stock_ticker", nullable = false, length = 20)),
        AttributeOverride(name = "companyName", column = Column(name = "stock_company_name", nullable = false, length = 100)),
        AttributeOverride(name = "marketPrice", column = Column(name = "stock_market_price", nullable = false, precision = 19, scale = 4)),
        AttributeOverride(name = "priceChangeRate", column = Column(name = "stock_price_change_rate", nullable = false, precision = 7, scale = 4)),
        AttributeOverride(name = "capturedAt", column = Column(name = "stock_captured_at", nullable = false))
    )
    var stockSnapshot: StockQuoteSnapshot,

    @Embedded
    var sajuSnapshot: SajuSnapshot,

    @Embedded
    var tarotSnapshot: TarotHistorySnapshot,

    @Column(name = "ai_answer_text", nullable = false, columnDefinition = "TEXT")
    var aiAnswerText: String,

    @Column(name = "analysis_result_json", nullable = false, columnDefinition = "TEXT")
    var analysisResultJson: String = "{}",

    @Column(name = "ai_response_json", nullable = false, columnDefinition = "TEXT")
    var aiResponseJson: String = "{}",

    @Column(name = "share_key", nullable = false, unique = true, length = 36)
    var shareKey: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback", length = 20)
    var feedback: ConsultingFeedback? = null,

    @Column(name = "realized_profit_rate", precision = 7, scale = 4)
    var realizedProfitRate: java.math.BigDecimal? = null,

    @Column(name = "retro_note", length = 1000)
    var retroNote: String? = null,

    @Column(name = "retrospected_at")
    var retrospectedAt: LocalDateTime? = null
)
