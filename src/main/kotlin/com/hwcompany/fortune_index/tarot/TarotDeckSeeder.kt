package com.hwcompany.fortune_index.tarot

import com.hwcompany.fortune_index.domain.model.SubscriptionTier
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
                deckType = TarotDeckType.TAROT,
                deckRole = TarotDeckRole.MAIN,
                cardSetId = DEFAULT_TAROT_CARD_SET_ID,
                drawCount = 3,
                requiredSubscriptionTier = SubscriptionTier.FREE,
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
                    deckRole = metadata.deckRole,
                    cardSetId = metadata.cardSetId,
                    name = metadata.name,
                    koreanName = requireNotNull(metadata.koreanName),
                    sortOrder = metadata.sortOrder,
                    arcanaType = metadata.arcanaType,
                    suit = metadata.suit,
                    meaning = metadata.meaning,
                    description = metadata.description,
                    imageUrl = "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.png",
                    videoUrl = "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
                )
            }
        )

        val premiumMainDeck = tarotDeckVersionRepository.save(
            TarotDeckVersionEntity(
                id = "classic-rider-waite-signature",
                name = "시그니처 라이더",
                description = "프리미엄 전용 메인 타로 덱",
                coverImageUrl = "https://cdn.example.com/tarot/signature/cover.png",
                deckType = TarotDeckType.TAROT,
                deckRole = TarotDeckRole.MAIN,
                cardSetId = DEFAULT_TAROT_CARD_SET_ID,
                drawCount = 3,
                requiredSubscriptionTier = SubscriptionTier.PREMIUM,
                active = true,
                createdAt = now,
                updatedAt = now
            )
        )

        tarotCardMetadataRepository.saveAll(
            TarotCard.entries.map { card ->
                val metadata = card.toMetadata(deckVersionId = premiumMainDeck.id)
                TarotCardMetadataEntity(
                    deckVersion = premiumMainDeck,
                    selectedIndex = metadata.selectedIndex,
                    code = metadata.code,
                    deckType = metadata.deckType,
                    deckRole = metadata.deckRole,
                    cardSetId = metadata.cardSetId,
                    name = metadata.name,
                    koreanName = requireNotNull(metadata.koreanName),
                    sortOrder = metadata.sortOrder,
                    arcanaType = metadata.arcanaType,
                    suit = metadata.suit,
                    meaning = metadata.meaning,
                    description = metadata.description,
                    imageUrl = "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.png",
                    videoUrl = "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
                )
            }
        )

        val oracleDeck = tarotDeckVersionRepository.save(
            TarotDeckVersionEntity(
                id = "market-signal-oracle",
                name = "마켓 시그널 오라클",
                description = "프리미엄 전용 보조 오라클 카드",
                coverImageUrl = "https://cdn.example.com/oracle/market-signal/cover.png",
                deckType = TarotDeckType.ORACLE,
                deckRole = TarotDeckRole.ASSISTANT,
                cardSetId = "market-signal-oracle",
                drawCount = 1,
                requiredSubscriptionTier = SubscriptionTier.PREMIUM,
                active = true,
                createdAt = now,
                updatedAt = now
            )
        )

        tarotCardMetadataRepository.saveAll(
            listOf(
                TarotCardMetadataEntity(
                    deckVersion = oracleDeck,
                    selectedIndex = 0,
                    code = "ENTRY_WINDOW",
                    deckType = TarotDeckType.ORACLE,
                    deckRole = TarotDeckRole.ASSISTANT,
                    cardSetId = "market-signal-oracle",
                    name = "Entry Window",
                    koreanName = "진입 창",
                    sortOrder = 0,
                    arcanaType = null,
                    suit = null,
                    meaning = "진입 타이밍이 열리지만 분할 접근이 유효하다.",
                    description = "추세를 무작정 추격하기보다 진입 창이 열릴 때 천천히 비중을 실으라는 보조 신호다.",
                    imageUrl = "https://cdn.example.com/oracle/market-signal/000.png",
                    videoUrl = "https://cdn.example.com/oracle/market-signal/000.mp4"
                ),
                TarotCardMetadataEntity(
                    deckVersion = oracleDeck,
                    selectedIndex = 1,
                    code = "VOLATILITY_SPIKE",
                    deckType = TarotDeckType.ORACLE,
                    deckRole = TarotDeckRole.ASSISTANT,
                    cardSetId = "market-signal-oracle",
                    name = "Volatility Spike",
                    koreanName = "변동성 급등",
                    sortOrder = 1,
                    arcanaType = null,
                    suit = null,
                    meaning = "방향성보다 변동성 관리가 먼저다.",
                    description = "좋은 종목이어도 진입 속도와 손절 기준을 더 촘촘하게 잡아야 하는 구간을 뜻한다.",
                    imageUrl = "https://cdn.example.com/oracle/market-signal/001.png",
                    videoUrl = "https://cdn.example.com/oracle/market-signal/001.mp4"
                ),
                TarotCardMetadataEntity(
                    deckVersion = oracleDeck,
                    selectedIndex = 2,
                    code = "CONFIRMATION",
                    deckType = TarotDeckType.ORACLE,
                    deckRole = TarotDeckRole.ASSISTANT,
                    cardSetId = "market-signal-oracle",
                    name = "Confirmation",
                    koreanName = "확인 신호",
                    sortOrder = 2,
                    arcanaType = null,
                    suit = null,
                    meaning = "기존 판단을 재확인해도 되는 구간이다.",
                    description = "보조 지표와 타이밍이 맞물리는 만큼, 기존 전략을 유지하되 과신은 피하라는 카드다.",
                    imageUrl = "https://cdn.example.com/oracle/market-signal/002.png",
                    videoUrl = "https://cdn.example.com/oracle/market-signal/002.mp4"
                )
            )
        )
    }
}
