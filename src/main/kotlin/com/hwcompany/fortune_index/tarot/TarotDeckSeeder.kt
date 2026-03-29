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
        val now = LocalDateTime.now()

        upsertMainDeck(
            id = DEFAULT_TAROT_DECK_VERSION_ID,
            name = "클래식 라이더",
            description = "기본 타로 덱",
            coverImageUrl = "https://cdn.example.com/tarot/classic/cover.png",
            requiredSubscriptionTier = SubscriptionTier.FREE,
            now = now,
            imageUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.png"
            },
            videoUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/classic/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
            }
        )

        upsertMainDeck(
            id = "classic-rider-waite-signature",
            name = "시그니처 라이더",
            description = "프리미엄 전용 메인 타로 덱",
            coverImageUrl = "https://cdn.example.com/tarot/signature/cover.png",
            requiredSubscriptionTier = SubscriptionTier.PREMIUM,
            now = now,
            imageUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.png"
            },
            videoUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
            }
        )

        upsertMainDeck(
            id = "classic-rider-waite-v2",
            name = "클래식 라이더 V2",
            description = "기존 78장 구성을 유지하고 이미지 URL만 /tarot/v2 경로로 교체한 메인 타로 덱",
            coverImageUrl = "/tarot/v2/0_THE_FOOL.png",
            requiredSubscriptionTier = SubscriptionTier.FREE,
            now = now,
            imageUrlResolver = { metadata ->
                when (metadata.selectedIndex) {
                    0 -> "/tarot/v2/0_THE_FOOL.png"
                    1 -> "/tarot/v2/1_THE_MAGICAN.png"
                    2 -> "/tarot/v2/2_THE_HIGH_PRIESTESS.png"
                    3 -> "/tarot/v2/3_THE_EMPRESS.png"
                    4 -> "/tarot/v2/4_THE_EMPEROR.png"
                    5 -> "/tarot/v2/5_THE_HIEROPHANT.png"
                    6 -> "/tarot/v2/6_THE_LOVERS.png"
                    7 -> "/tarot/v2/7_THE_CHARIOT.png"
                    8 -> "/tarot/v2/8_STRENGTH.png"
                    9 -> "/tarot/v2/9_THE_HERMIT.png"
                    10 -> "/tarot/v2/10_THE_WHEEL_OF_FORTUNE.png"
                    11 -> "/tarot/v2/11_JUSTICE.png"
                    12 -> "/tarot/v2/12_THE_HANGED_MAN.png"
                    13 -> "/tarot/v2/13_DEATH.png"
                    14 -> "/tarot/v2/14_TEMPERANCE.png"
                    15 -> "/tarot/v2/15_THE_DEVIL.png"
                    16 -> "/tarot/v2/16_THE_TOWER.png"
                    17 -> "/tarot/v2/17_THE_STAR.png"
                    18 -> "/tarot/v2/18_THE_MOON.png"
                    19 -> "/tarot/v2/19_THE_SUN.png"
                    20 -> "/tarot/v2/20_JUDGEMENT.png"
                    21 -> "/tarot/v2/21_THE_WORLD.png"
                    else -> "/tarot/v2/${metadata.selectedIndex}_${metadata.code}.png"
                }
            },
            videoUrlResolver = { null }
        )

        upsertOracleDeck(now)
    }

    private fun upsertMainDeck(
        id: String,
        name: String,
        description: String,
        coverImageUrl: String,
        requiredSubscriptionTier: SubscriptionTier,
        now: LocalDateTime,
        imageUrlResolver: (TarotCardMetadata) -> String,
        videoUrlResolver: (TarotCardMetadata) -> String?
    ) {
        val deck = upsertDeckVersion(
            id = id,
            name = name,
            description = description,
            coverImageUrl = coverImageUrl,
            deckType = TarotDeckType.TAROT,
            deckRole = TarotDeckRole.MAIN,
            cardSetId = DEFAULT_TAROT_CARD_SET_ID,
            drawCount = 3,
            requiredSubscriptionTier = requiredSubscriptionTier,
            now = now
        )

        TarotCard.entries.forEach { card ->
            val metadata = card.toMetadata(deckVersionId = deck.id)
            upsertCard(
                deckVersion = deck,
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
                imageUrl = imageUrlResolver(metadata),
                videoUrl = videoUrlResolver(metadata)
            )
        }
    }

    private fun upsertOracleDeck(now: LocalDateTime) {
        val deck = upsertDeckVersion(
            id = "market-signal-oracle",
            name = "마켓 시그널 오라클",
            description = "프리미엄 전용 보조 오라클 카드",
            coverImageUrl = "https://cdn.example.com/oracle/market-signal/cover.png",
            deckType = TarotDeckType.ORACLE,
            deckRole = TarotDeckRole.ASSISTANT,
            cardSetId = "market-signal-oracle",
            drawCount = 1,
            requiredSubscriptionTier = SubscriptionTier.PREMIUM,
            now = now
        )

        listOf(
            OracleSeedCard(
                selectedIndex = 0,
                code = "ENTRY_WINDOW",
                name = "Entry Window",
                koreanName = "진입 창",
                meaning = "진입 타이밍이 열리지만 분할 접근이 유효하다.",
                description = "추세를 무작정 추격하기보다 진입 창이 열릴 때 천천히 비중을 실으라는 보조 신호다.",
                imageUrl = "https://cdn.example.com/oracle/market-signal/000.png",
                videoUrl = "https://cdn.example.com/oracle/market-signal/000.mp4"
            ),
            OracleSeedCard(
                selectedIndex = 1,
                code = "VOLATILITY_SPIKE",
                name = "Volatility Spike",
                koreanName = "변동성 급등",
                meaning = "방향성보다 변동성 관리가 먼저다.",
                description = "좋은 종목이어도 진입 속도와 손절 기준을 더 촘촘하게 잡아야 하는 구간을 뜻한다.",
                imageUrl = "https://cdn.example.com/oracle/market-signal/001.png",
                videoUrl = "https://cdn.example.com/oracle/market-signal/001.mp4"
            ),
            OracleSeedCard(
                selectedIndex = 2,
                code = "CONFIRMATION",
                name = "Confirmation",
                koreanName = "확인 신호",
                meaning = "기존 판단을 재확인해도 되는 구간이다.",
                description = "보조 지표와 타이밍이 맞물리는 만큼, 기존 전략을 유지하되 과신은 피하라는 카드다.",
                imageUrl = "https://cdn.example.com/oracle/market-signal/002.png",
                videoUrl = "https://cdn.example.com/oracle/market-signal/002.mp4"
            )
        ).forEach { card ->
            upsertCard(
                deckVersion = deck,
                selectedIndex = card.selectedIndex,
                code = card.code,
                deckType = TarotDeckType.ORACLE,
                deckRole = TarotDeckRole.ASSISTANT,
                cardSetId = "market-signal-oracle",
                name = card.name,
                koreanName = card.koreanName,
                sortOrder = card.selectedIndex,
                arcanaType = null,
                suit = null,
                meaning = card.meaning,
                description = card.description,
                imageUrl = card.imageUrl,
                videoUrl = card.videoUrl
            )
        }
    }

    private fun upsertDeckVersion(
        id: String,
        name: String,
        description: String,
        coverImageUrl: String,
        deckType: TarotDeckType,
        deckRole: TarotDeckRole,
        cardSetId: String,
        drawCount: Int,
        requiredSubscriptionTier: SubscriptionTier,
        now: LocalDateTime
    ): TarotDeckVersionEntity {
        val existing = tarotDeckVersionRepository.findById(id).orElse(null)
        return tarotDeckVersionRepository.save(
            existing?.apply {
                this.name = name
                this.description = description
                this.coverImageUrl = coverImageUrl
                this.deckType = deckType
                this.deckRole = deckRole
                this.cardSetId = cardSetId
                this.drawCount = drawCount
                this.requiredSubscriptionTier = requiredSubscriptionTier
                this.active = true
                this.updatedAt = now
            } ?: TarotDeckVersionEntity(
                id = id,
                name = name,
                description = description,
                coverImageUrl = coverImageUrl,
                deckType = deckType,
                deckRole = deckRole,
                cardSetId = cardSetId,
                drawCount = drawCount,
                requiredSubscriptionTier = requiredSubscriptionTier,
                active = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun upsertCard(
        deckVersion: TarotDeckVersionEntity,
        selectedIndex: Int,
        code: String,
        deckType: TarotDeckType,
        deckRole: TarotDeckRole,
        cardSetId: String,
        name: String,
        koreanName: String,
        sortOrder: Int,
        arcanaType: TarotArcanaType?,
        suit: TarotSuit?,
        meaning: String,
        description: String,
        imageUrl: String,
        videoUrl: String?
    ) {
        val existing = tarotCardMetadataRepository.findByDeckVersion_IdAndSelectedIndex(deckVersion.id, selectedIndex)
        tarotCardMetadataRepository.save(
            existing?.apply {
                this.deckVersion = deckVersion
                this.code = code
                this.deckType = deckType
                this.deckRole = deckRole
                this.cardSetId = cardSetId
                this.name = name
                this.koreanName = koreanName
                this.sortOrder = sortOrder
                this.arcanaType = arcanaType
                this.suit = suit
                this.meaning = meaning
                this.description = description
                this.imageUrl = imageUrl
                this.videoUrl = videoUrl
            } ?: TarotCardMetadataEntity(
                deckVersion = deckVersion,
                selectedIndex = selectedIndex,
                code = code,
                deckType = deckType,
                deckRole = deckRole,
                cardSetId = cardSetId,
                name = name,
                koreanName = koreanName,
                sortOrder = sortOrder,
                arcanaType = arcanaType,
                suit = suit,
                meaning = meaning,
                description = description,
                imageUrl = imageUrl,
                videoUrl = videoUrl
            )
        )
    }
}

private data class OracleSeedCard(
    val selectedIndex: Int,
    val code: String,
    val name: String,
    val koreanName: String,
    val meaning: String,
    val description: String,
    val imageUrl: String,
    val videoUrl: String?
)
