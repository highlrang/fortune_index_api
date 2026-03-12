package com.hwcompany.fortune_index.tarot

import java.util.Collections
import org.springframework.stereotype.Service

@Service
class TarotService {
    fun drawReading(
        indices: List<Int>?,
        interpretationMode: TarotInterpretationMode = TarotInterpretationMode.MAIN_TRADITIONAL
    ): TarotReadingResult =
        TarotReadingResult(
            interpretationMode = interpretationMode,
            cards = drawCards(indices)
        )

    fun drawCards(indices: List<Int>?): List<TarotDrawResult> {
        val deck = TarotCard.deck().toMutableList()
        val selectedCards = if (indices.isNullOrEmpty()) {
            Collections.shuffle(deck)
            deck.take(DEFAULT_CARD_COUNT)
        } else {
            require(indices.size == DEFAULT_CARD_COUNT) {
                "tarotIndices must contain exactly $DEFAULT_CARD_COUNT cards: size=${indices.size}"
            }
            require(indices.distinct().size == indices.size) {
                "tarotIndices must not contain duplicates: $indices"
            }
            indices.map { index ->
                require(index in deck.indices) {
                    "tarot index must be between 0 and ${deck.lastIndex}: $index"
                }
                deck[index]
            }
        }

        return selectedCards.map { card ->
            TarotDrawResult(
                index = TarotCard.entries.indexOf(card),
                card = card
            )
        }
    }

    fun drawCard(index: Int?): TarotDrawResult {
        val deck = TarotCard.deck().toMutableList()
        val selectedCard = if (index == null) {
            Collections.shuffle(deck)
            deck.first()
        } else {
            require(index in deck.indices) {
                "tarot index must be between 0 and ${deck.lastIndex}: $index"
            }
            deck[index]
        }

        return TarotDrawResult(
            index = TarotCard.entries.indexOf(selectedCard),
            card = selectedCard
        )
    }

    private companion object {
        private const val DEFAULT_CARD_COUNT = 3
    }
}

data class TarotDrawResult(
    val index: Int,
    val card: TarotCard
)

data class TarotReadingResult(
    val interpretationMode: TarotInterpretationMode,
    val cards: List<TarotDrawResult>
)
