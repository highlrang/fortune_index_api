package com.hwcompany.fortune_index.tarot

import java.util.Collections
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class TarotDeckService(
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository
) {
    @Transactional(readOnly = true)
    fun getDeckVersions(): List<TarotDeckVersionSummary> =
        tarotDeckVersionRepository.findAllByOrderByActiveDescNameAsc()
            .filter { it.active }
            .map { it.toSummary() }

    @Transactional(readOnly = true)
    fun getDeckCards(deckVersionId: String, selectedIndices: List<Int>? = null): List<TarotCardMetadata> {
        requireActiveDeckVersion(deckVersionId)
        val indices = selectedIndices?.takeIf { it.isNotEmpty() }
        validateSelectedIndices(indices)

        if (indices == null) {
            return tarotCardMetadataRepository.findByDeckVersion_IdOrderBySelectedIndexAsc(deckVersionId)
                .map { it.toMetadata() }
        }

        val cardsByIndex = tarotCardMetadataRepository.findByDeckVersion_IdAndSelectedIndexIn(deckVersionId, indices)
            .associateBy { it.selectedIndex }

        if (cardsByIndex.size != indices.size) {
            val missingIndices = indices.filterNot(cardsByIndex::containsKey)
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "unknown tarot selectedIndices for deckVersionId=$deckVersionId: $missingIndices"
            )
        }

        return indices.map { selectedIndex ->
            requireNotNull(cardsByIndex[selectedIndex]).toMetadata()
        }
    }

    @Transactional(readOnly = true)
    fun drawReading(
        deckVersionId: String,
        indices: List<Int>?,
        interpretationMode: TarotInterpretationMode = TarotInterpretationMode.MAIN_TRADITIONAL
    ): TarotReadingResult =
        TarotReadingResult(
            interpretationMode = interpretationMode,
            cards = drawCards(deckVersionId, indices)
        )

    @Transactional(readOnly = true)
    fun drawCards(deckVersionId: String, indices: List<Int>?): List<TarotDrawResult> {
        val deck = getDeckCards(deckVersionId).toMutableList()
        val selectedCards = if (indices.isNullOrEmpty()) {
            Collections.shuffle(deck)
            deck.take(DEFAULT_CARD_COUNT)
        } else {
            require(indices.size == DEFAULT_CARD_COUNT) {
                "tarotIndices must contain exactly $DEFAULT_CARD_COUNT cards: size=${indices.size}"
            }
            getDeckCards(deckVersionId, indices)
        }

        return selectedCards.map { card ->
            TarotDrawResult(
                index = card.selectedIndex,
                card = card
            )
        }
    }

    @Transactional(readOnly = true)
    fun getDeckCardCount(deckVersionId: String = DEFAULT_TAROT_DECK_VERSION_ID): Int {
        requireActiveDeckVersion(deckVersionId)
        return tarotCardMetadataRepository.countByDeckVersion_Id(deckVersionId).toInt()
    }

    private fun requireActiveDeckVersion(deckVersionId: String): TarotDeckVersionEntity {
        val deckVersion = tarotDeckVersionRepository.findById(deckVersionId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid tarotDeckVersionId: $deckVersionId")
        }
        if (!deckVersion.active) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "inactive tarotDeckVersionId: $deckVersionId")
        }
        return deckVersion
    }

    private fun validateSelectedIndices(selectedIndices: List<Int>?) {
        if (selectedIndices == null) {
            return
        }
        require(selectedIndices.distinct().size == selectedIndices.size) {
            "selectedIndices must not contain duplicates: $selectedIndices"
        }
    }

    private companion object {
        private const val DEFAULT_CARD_COUNT = 3
    }
}
