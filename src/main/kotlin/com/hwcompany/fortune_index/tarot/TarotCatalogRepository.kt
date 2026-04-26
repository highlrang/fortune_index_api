package com.hwcompany.fortune_index.tarot

import org.springframework.data.jpa.repository.JpaRepository

interface TarotDeckVersionRepository : JpaRepository<TarotDeckVersionEntity, String> {
    fun findAllByOrderByActiveDescDisplayOrderAscNameAsc(): List<TarotDeckVersionEntity>
    fun findFirstByActiveTrueAndDeckRoleOrderByDisplayOrderAscNameAsc(deckRole: TarotDeckRole): TarotDeckVersionEntity?
}

interface TarotCardMetadataRepository : JpaRepository<TarotCardMetadataEntity, Long> {
    fun findByDeckVersion_IdOrderBySelectedIndexAsc(deckVersionId: String): List<TarotCardMetadataEntity>
    fun findByDeckVersion_IdAndSelectedIndexIn(deckVersionId: String, selectedIndices: Collection<Int>): List<TarotCardMetadataEntity>
    fun findByDeckVersion_IdAndSelectedIndex(deckVersionId: String, selectedIndex: Int): TarotCardMetadataEntity?
    fun findByDeckVersion_IdAndCode(deckVersionId: String, code: String): TarotCardMetadataEntity?
    fun countByDeckVersion_Id(deckVersionId: String): Long
}

interface TarotBirthCardRepository : JpaRepository<TarotBirthCardEntity, Long> {
    fun findByCardSetIdAndCode(cardSetId: String, code: String): TarotBirthCardEntity?
}
