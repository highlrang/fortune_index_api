package com.hwcompany.fortune_index.tarot

import java.time.LocalDateTime
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TarotDeckSeeder(
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (tarotDeckVersionRepository.existsById(DEFAULT_TAROT_DECK_VERSION_ID)) {
            return
        }

        val now = LocalDateTime.now()
        val defaultDeck = tarotDeckVersionRepository.save(
            TarotDeckVersionEntity(
                id = DEFAULT_TAROT_DECK_VERSION_ID,
                name = "클래식 라이더",
                description = "기본 타로 덱",
                coverImageUrl = "https://cdn.example.com/tarot/classic/cover.png",
                active = true,
                createdAt = now,
                updatedAt = now
            )
        )

        tarotCardMetadataRepository.saveAll(
            TarotCard.entries.map { card ->
                val metadata = card.toMetadata(deckVersionId = defaultDeck.id)
                TarotCardMetadataEntity(
                    deckVersion = defaultDeck,
                    selectedIndex = metadata.selectedIndex,
                    code = metadata.code,
                    deckType = metadata.deckType,
                    name = metadata.name,
                    koreanName = requireNotNull(metadata.koreanName),
                    sortOrder = metadata.sortOrder,
                    arcanaType = metadata.arcanaType,
                    suit = metadata.suit,
                    meaning = metadata.meaning,
                    imageUrl = "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.png",
                    videoUrl = "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
                )
            }
        )
    }
}
