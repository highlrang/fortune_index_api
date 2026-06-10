package com.hwcompany.fortune_index.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import com.hwcompany.fortune_index.tarot.TarotInterpretationMode
import java.math.BigDecimal
import java.time.LocalDate
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
data class TarotHistorySnapshot(
    @Enumerated(EnumType.STRING)
    @Column(name = "tarot_interpretation_mode", length = 30)
    var interpretationMode: TarotInterpretationMode? = null,

    @Column(name = "tarot_cards_json", nullable = false, columnDefinition = "TEXT")
    var cardsJson: String = "[]",

    @Column(name = "tarot_summary", nullable = false, length = 300)
    var summary: String
)
