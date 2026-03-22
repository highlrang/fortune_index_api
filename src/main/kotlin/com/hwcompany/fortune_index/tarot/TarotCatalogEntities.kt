package com.hwcompany.fortune_index.tarot

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tarot_deck_versions")
data class TarotDeckVersionEntity(
    @Id
    @Column(length = 100)
    val id: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 500)
    var description: String,

    @Column(name = "cover_image_url", nullable = false, length = 1000)
    var coverImageUrl: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "tarot_cards",
    indexes = [
        Index(name = "idx_tarot_cards_deck_version_sort_order", columnList = "deck_version_id, sort_order"),
        Index(name = "uk_tarot_cards_deck_version_selected_index", columnList = "deck_version_id, selected_index", unique = true),
        Index(name = "uk_tarot_cards_deck_version_code", columnList = "deck_version_id, code", unique = true)
    ]
)
data class TarotCardMetadataEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_version_id", nullable = false)
    var deckVersion: TarotDeckVersionEntity,

    @Column(name = "selected_index", nullable = false)
    var selectedIndex: Int,

    @Column(nullable = false, length = 60)
    var code: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "deck_type", nullable = false, length = 20)
    var deckType: TarotDeckType,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "korean_name", nullable = false, length = 100)
    var koreanName: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "arcana_type", nullable = false, length = 20)
    var arcanaType: TarotArcanaType,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var suit: TarotSuit? = null,

    @Column(nullable = false, length = 500)
    var meaning: String,

    @Column(name = "image_url", nullable = false, length = 1000)
    var imageUrl: String,

    @Column(name = "video_url", length = 1000)
    var videoUrl: String? = null
)

fun TarotDeckVersionEntity.toSummary(): TarotDeckVersionSummary =
    TarotDeckVersionSummary(
        id = id,
        name = name,
        description = description,
        coverImageUrl = coverImageUrl,
        active = active
    )

fun TarotCardMetadataEntity.toMetadata(): TarotCardMetadata =
    TarotCardMetadata(
        selectedIndex = selectedIndex,
        code = code,
        deckVersionId = deckVersion.id,
        deckType = deckType,
        name = name,
        koreanName = koreanName,
        sortOrder = sortOrder,
        arcanaType = arcanaType,
        suit = suit,
        meaning = meaning,
        imageUrl = imageUrl,
        videoUrl = videoUrl
    )
