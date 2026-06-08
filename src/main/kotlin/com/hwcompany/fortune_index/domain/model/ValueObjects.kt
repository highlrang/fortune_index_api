package com.hwcompany.fortune_index.domain.model

import com.hwcompany.fortune_index.common.SeoulTime
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Embeddable
data class BirthInfo(
    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Column(name = "birth_time")
    var birthTime: LocalTime? = null,

    @Column(name = "birth_place_name", length = 100)
    var birthPlaceName: String? = null,

    @Column(name = "birth_latitude")
    var birthLatitude: Double? = null,

    @Column(name = "birth_longitude")
    var birthLongitude: Double? = null
)

@Embeddable
data class FiveElementsProfile(
    @Column(name = "wood_ratio", nullable = false, precision = 5, scale = 2)
    var wood: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fire_ratio", nullable = false, precision = 5, scale = 2)
    var fire: BigDecimal = BigDecimal.ZERO,

    @Column(name = "earth_ratio", nullable = false, precision = 5, scale = 2)
    var earth: BigDecimal = BigDecimal.ZERO,

    @Column(name = "metal_ratio", nullable = false, precision = 5, scale = 2)
    var metal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "water_ratio", nullable = false, precision = 5, scale = 2)
    var water: BigDecimal = BigDecimal.ZERO
)

@Embeddable
data class TarotCardDraw(
    @Column(name = "card_name", nullable = false, length = 50)
    var cardName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "orientation", nullable = false, length = 10)
    var orientation: TarotOrientation,

    @Column(name = "interpretation", nullable = false, length = 300)
    var interpretation: String
)

@Embeddable
data class InvestmentFocusSnapshot(
    @Column(name = "ticker", nullable = false, length = 20)
    var ticker: String,

    @Column(name = "company_name", nullable = false, length = 100)
    var label: String,

    @Column(name = "investment_current_value", nullable = false, precision = 19, scale = 4)
    var currentValue: BigDecimal,

    @Column(name = "investment_change_rate", nullable = false, precision = 7, scale = 4)
    var changeRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "captured_at", nullable = false)
    var capturedAt: LocalDateTime = SeoulTime.now()
)

@Embeddable
data class SajuSnapshot(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "wood", column = Column(name = "saju_wood_ratio", nullable = false, precision = 5, scale = 2)),
        AttributeOverride(name = "fire", column = Column(name = "saju_fire_ratio", nullable = false, precision = 5, scale = 2)),
        AttributeOverride(name = "earth", column = Column(name = "saju_earth_ratio", nullable = false, precision = 5, scale = 2)),
        AttributeOverride(name = "metal", column = Column(name = "saju_metal_ratio", nullable = false, precision = 5, scale = 2)),
        AttributeOverride(name = "water", column = Column(name = "saju_water_ratio", nullable = false, precision = 5, scale = 2))
    )
    var fiveElements: FiveElementsProfile,

    @Column(name = "saju_summary", nullable = false, length = 500)
    var summary: String
)

@Embeddable
data class TarotHistorySnapshot(
    @Enumerated(EnumType.STRING)
    @Column(name = "tarot_interpretation_mode", length = 30)
    var interpretationMode: TarotInterpretationMode? = null,

    @Column(name = "tarot_cards_json", nullable = false, columnDefinition = "TEXT")
    var cardsJson: String = "[]",

    @Column(name = "tarot_summary", nullable = false, length = 300)
    var summary: String
)
