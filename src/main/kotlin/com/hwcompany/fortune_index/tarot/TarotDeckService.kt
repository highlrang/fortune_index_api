package com.hwcompany.fortune_index.tarot

import com.hwcompany.fortune_index.domain.model.SubscriptionTier
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
    fun getDeckVersions(
        subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
        preferredDeckVersionId: String? = null
    ): List<TarotDeckVersionSummary> =
        tarotDeckVersionRepository.findAllByOrderByActiveDescDisplayOrderAscNameAsc()
            .filter { it.active }
            .filter { subscriptionTier.ordinal >= it.requiredSubscriptionTier.ordinal }
            .let { decks ->
                val activeMainDeckId = tarotDeckVersionRepository
                    .findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(TarotDeckRole.MAIN)
                    ?.id
                    ?: preferredDeckVersionId?.takeIf { id -> decks.any { deck -> deck.id == id } }
                    ?: DEFAULT_TAROT_DECK_VERSION_ID

                decks.map { deck ->
                    deck.toSummary(selected = deck.id == activeMainDeckId)
                }
            }
    fun getActiveMainDeckVersionId(): String =
        tarotDeckVersionRepository.findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(TarotDeckRole.MAIN)?.id
            ?: DEFAULT_TAROT_DECK_VERSION_ID

    @Transactional(readOnly = true)
    fun getDeckCards(
        deckVersionId: String,
        selectedIndices: List<Int>? = null,
        subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
    ): List<TarotCardMetadata> {
        requireDeckAccess(deckVersionId, subscriptionTier, null)
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
                "타로 덱에서 찾을 수 없는 selectedIndices입니다. deckVersionId=$deckVersionId, indices=$missingIndices"
            )
        }

        return indices.map { selectedIndex ->
            requireNotNull(cardsByIndex[selectedIndex]).toMetadata()
        }
    }

    @Transactional(readOnly = true)
    fun drawReading(
        subscriptionTier: SubscriptionTier,
        deckVersionId: String,
        indices: List<Int>?,
        assistantDeckSelections: List<TarotAssistantDeckSelection> = emptyList(),
        interpretationMode: TarotInterpretationMode = TarotInterpretationMode.MAIN_TRADITIONAL
    ): TarotReadingResult =
        requireDeckAccess(deckVersionId, subscriptionTier, TarotDeckRole.MAIN).let { mainDeck ->
            TarotReadingResult(
                interpretationMode = interpretationMode,
                cards = drawCards(mainDeck, indices),
                assistantDecks = assistantDeckSelections.map { selection ->
                    val assistantDeck = requireDeckAccess(
                        deckVersionId = selection.deckVersionId,
                        subscriptionTier = subscriptionTier,
                        expectedRole = TarotDeckRole.ASSISTANT
                    )
                    TarotDrawGroupResult(
                        deckVersionId = assistantDeck.id,
                        deckType = assistantDeck.deckType,
                        deckRole = assistantDeck.deckRole,
                        cardSetId = assistantDeck.cardSetId,
                        cards = drawCards(assistantDeck, selection.selectedIndices)
                    )
                }
            )
        }

    @Transactional(readOnly = true)
    fun drawCards(deckVersionId: String, indices: List<Int>?): List<TarotDrawResult> {
        val deckVersion = requireActiveDeckVersion(deckVersionId)
        return drawCards(deckVersion, indices)
    }

    private fun drawCards(deckVersion: TarotDeckVersionEntity, indices: List<Int>?): List<TarotDrawResult> {
        val selectedCards = if (indices.isNullOrEmpty()) {
            val deck = getDeckCards(
                deckVersionId = deckVersion.id,
                subscriptionTier = deckVersion.requiredSubscriptionTier
            ).toMutableList()
            Collections.shuffle(deck)
            deck.take(deckVersion.drawCount)
        } else {
            require(indices.size == deckVersion.drawCount) {
                "selectedIndices는 deckVersionId=${deckVersion.id}에서 정확히 ${deckVersion.drawCount}장의 카드를 포함해야 합니다. 현재 개수=${indices.size}"
            }
            getDeckCards(
                deckVersionId = deckVersion.id,
                selectedIndices = indices,
                subscriptionTier = deckVersion.requiredSubscriptionTier
            )
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

    private fun requireDeckAccess(
        deckVersionId: String,
        subscriptionTier: SubscriptionTier,
        expectedRole: TarotDeckRole?
    ): TarotDeckVersionEntity {
        val deckVersion = requireActiveDeckVersion(deckVersionId)
        if (expectedRole != null && deckVersion.deckRole != expectedRole) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "타로 덱 역할이 올바르지 않습니다. deckVersionId=$deckVersionId, 필요 역할=$expectedRole, 현재 역할=${deckVersion.deckRole}"
            )
        }
        if (subscriptionTier.ordinal < deckVersion.requiredSubscriptionTier.ordinal) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "현재 구독 등급($subscriptionTier)으로는 deckVersionId=$deckVersionId 덱에 접근할 수 없습니다."
            )
        }
        return deckVersion
    }

    private fun requireActiveDeckVersion(deckVersionId: String): TarotDeckVersionEntity {
        val deckVersion = tarotDeckVersionRepository.findById(deckVersionId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "타로 덱 버전 ID가 올바르지 않습니다: $deckVersionId")
        }
        if (!deckVersion.active) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비활성화된 타로 덱 버전 ID입니다: $deckVersionId")
        }
        return deckVersion
    }

    private fun validateSelectedIndices(selectedIndices: List<Int>?) {
        if (selectedIndices == null) {
            return
        }
        require(selectedIndices.distinct().size == selectedIndices.size) {
            "selectedIndices에는 중복된 카드가 포함될 수 없습니다: $selectedIndices"
        }
    }
}
