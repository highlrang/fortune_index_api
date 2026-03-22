package com.hwcompany.fortune_index.tarot

const val DEFAULT_TAROT_DECK_VERSION_ID = "classic-rider-waite"

data class TarotDeckVersionSummary(
    val id: String,
    val name: String,
    val description: String,
    val coverImageUrl: String,
    val active: Boolean
)

data class TarotCardMetadata(
    // Stable card index within a deck version. This is not the UI slot index.
    val selectedIndex: Int,
    val code: String,
    val deckVersionId: String,
    val deckType: TarotDeckType,
    val name: String,
    val koreanName: String?,
    val sortOrder: Int,
    val arcanaType: TarotArcanaType,
    val suit: TarotSuit?,
    val meaning: String,
    val imageUrl: String?,
    val videoUrl: String?
) {
    val cardNumber: Int
        get() = sortOrder
}

data class TarotDrawResult(
    val index: Int,
    val card: TarotCardMetadata
)

data class TarotReadingResult(
    val interpretationMode: TarotInterpretationMode,
    val cards: List<TarotDrawResult>
)
