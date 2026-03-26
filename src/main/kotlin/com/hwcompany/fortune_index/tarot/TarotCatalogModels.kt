package com.hwcompany.fortune_index.tarot

import com.hwcompany.fortune_index.domain.model.SubscriptionTier

const val DEFAULT_TAROT_DECK_VERSION_ID = "classic-rider-waite"
const val DEFAULT_TAROT_CARD_SET_ID = "rider-waite-78"

data class TarotDeckVersionSummary(
    val id: String,
    val name: String,
    val description: String,
    val coverImageUrl: String,
    val active: Boolean,
    val deckType: TarotDeckType,
    val deckRole: TarotDeckRole,
    val cardSetId: String,
    val drawCount: Int,
    val requiredSubscriptionTier: SubscriptionTier,
    val selected: Boolean = false
)

data class TarotCardMetadata(
    // Stable card index within a deck version. This is not the UI slot index.
    val selectedIndex: Int,
    val code: String,
    val deckVersionId: String,
    val deckType: TarotDeckType,
    val deckRole: TarotDeckRole,
    val cardSetId: String,
    val name: String,
    val koreanName: String?,
    val sortOrder: Int,
    val arcanaType: TarotArcanaType?,
    val suit: TarotSuit?,
    val meaning: String,
    val description: String,
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
    val cards: List<TarotDrawResult>,
    val assistantDecks: List<TarotDrawGroupResult> = emptyList()
)

data class TarotDrawGroupResult(
    val deckVersionId: String,
    val deckType: TarotDeckType,
    val deckRole: TarotDeckRole,
    val cardSetId: String,
    val cards: List<TarotDrawResult>
)

data class TarotAssistantDeckSelection(
    val deckVersionId: String,
    val selectedIndices: List<Int>? = null
)
