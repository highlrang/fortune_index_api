package com.hwcompany.fortune_index.tarot

import java.util.Collections
import org.springframework.stereotype.Service

@Service
class TarotService {
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
}

data class TarotDrawResult(
    val index: Int,
    val card: TarotCard
)
