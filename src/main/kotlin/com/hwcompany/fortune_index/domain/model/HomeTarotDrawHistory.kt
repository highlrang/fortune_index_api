package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "home_tarot_draw_histories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_home_tarot_draw_histories_user_date",
            columnNames = ["user_id", "draw_date"]
        )
    ]
)
data class HomeTarotDrawHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "draw_date", nullable = false)
    var drawDate: LocalDate,

    @Column(name = "drawn_at", nullable = false)
    var drawnAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deck_version_id", nullable = false, length = 100)
    var deckVersionId: String,

    @Column(name = "cards_json", nullable = false, columnDefinition = "TEXT")
    var cardsJson: String
)
