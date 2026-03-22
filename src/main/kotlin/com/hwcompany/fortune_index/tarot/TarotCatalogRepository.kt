package com.hwcompany.fortune_index.tarot

import org.springframework.data.jpa.repository.JpaRepository

interface TarotDeckVersionRepository : JpaRepository<TarotDeckVersionEntity, String> {
    fun findAllByOrderByActiveDescNameAsc(): List<TarotDeckVersionEntity>
}

interface TarotCardMetadataRepository : JpaRepository<TarotCardMetadataEntity, Long> {
    fun findByDeckVersion_IdOrderBySelectedIndexAsc(deckVersionId: String): List<TarotCardMetadataEntity>
    fun findByDeckVersion_IdAndSelectedIndexIn(deckVersionId: String, selectedIndices: Collection<Int>): List<TarotCardMetadataEntity>
    fun countByDeckVersion_Id(deckVersionId: String): Long
}
