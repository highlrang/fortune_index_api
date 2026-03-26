package com.hwcompany.fortune_index.tarot

import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.web.server.ResponseStatusException

@DataJpaTest
@Import(TarotDeckService::class, TarotDeckSeeder::class)
class TarotDeckServiceTest @Autowired constructor(
    private val tarotDeckService: TarotDeckService,
    private val tarotDeckSeeder: TarotDeckSeeder,
    private val tarotDeckVersionRepository: TarotDeckVersionRepository
) {
    @Test
    fun `기본 덱 버전과 선택 카드 메타데이터를 조회한다`() {
        tarotDeckSeeder.run(null)

        val deckVersions = tarotDeckService.getDeckVersions()
        val cards = tarotDeckService.getDeckCards(DEFAULT_TAROT_DECK_VERSION_ID, listOf(1, 17, 43))

        assertEquals(3, deckVersions.size)
        assertEquals(DEFAULT_TAROT_DECK_VERSION_ID, deckVersions.first().id)
        assertTrue(deckVersions.first().active)
        assertTrue(deckVersions.first().selected)
        assertEquals(listOf(1, 17, 43), cards.map { it.selectedIndex })
        assertEquals(listOf("THE_MAGICIAN", "THE_STAR", "EIGHT_OF_CUPS"), cards.map { it.code })
        assertEquals(listOf("마법사", "별", "컵 8"), cards.map { it.koreanName })
    }

    @Test
    fun `selectedIndices는 요청한 순서대로 반환한다`() {
        tarotDeckSeeder.run(null)

        val cards = tarotDeckService.getDeckCards(DEFAULT_TAROT_DECK_VERSION_ID, listOf(17, 1, 43))

        assertEquals(listOf(17, 1, 43), cards.map { it.selectedIndex })
        assertEquals(listOf("THE_STAR", "THE_MAGICIAN", "EIGHT_OF_CUPS"), cards.map { it.code })
    }

    @Test
    fun `덱 버전 기준으로 상담용 카드 메타데이터를 뽑는다`() {
        tarotDeckSeeder.run(null)

        val reading = tarotDeckService.drawReading(
            subscriptionTier = SubscriptionTier.FREE,
            deckVersionId = DEFAULT_TAROT_DECK_VERSION_ID,
            indices = listOf(1, 17, 43)
        )

        assertEquals(TarotInterpretationMode.MAIN_TRADITIONAL, reading.interpretationMode)
        assertEquals(listOf(1, 17, 43), reading.cards.map { it.index })
        assertEquals(DEFAULT_TAROT_DECK_VERSION_ID, reading.cards.first().card.deckVersionId)
        assertTrue(reading.cards.first().card.description.isNotBlank())
        assertEquals("https://cdn.example.com/tarot/classic/001.png", reading.cards.first().card.imageUrl)
        assertEquals("https://cdn.example.com/tarot/classic/017.mp4", reading.cards[1].card.videoUrl)
    }

    @Test
    fun `비활성 덱은 목록에 노출되지 않고 조회시 400을 반환한다`() {
        tarotDeckSeeder.run(null)
        tarotDeckVersionRepository.save(
            TarotDeckVersionEntity(
                id = "inactive-deck",
                name = "비활성 덱",
                description = "숨김 처리된 덱",
                coverImageUrl = "https://cdn.example.com/tarot/inactive/cover.png",
                cardSetId = "inactive-deck",
                active = false
            )
        )

        val deckVersions = tarotDeckService.getDeckVersions()
        val exception = assertFailsWith<ResponseStatusException> {
            tarotDeckService.getDeckCards("inactive-deck", listOf(1))
        }

        assertEquals(
            listOf(DEFAULT_TAROT_DECK_VERSION_ID, "classic-rider-waite-signature", "market-signal-oracle"),
            deckVersions.map { it.id }
        )
        assertEquals(400, exception.statusCode.value())
    }

    @Test
    fun `무료 사용자는 프리미엄 메인 덱과 보조 오라클을 사용할 수 없다`() {
        tarotDeckSeeder.run(null)

        val premiumDeckException = assertFailsWith<ResponseStatusException> {
            tarotDeckService.drawReading(
                subscriptionTier = SubscriptionTier.FREE,
                deckVersionId = "classic-rider-waite-signature",
                indices = listOf(1, 17, 43)
            )
        }
        val assistantException = assertFailsWith<ResponseStatusException> {
            tarotDeckService.drawReading(
                subscriptionTier = SubscriptionTier.FREE,
                deckVersionId = DEFAULT_TAROT_DECK_VERSION_ID,
                indices = listOf(1, 17, 43),
                assistantDeckSelections = listOf(
                    TarotAssistantDeckSelection(
                        deckVersionId = "market-signal-oracle",
                        selectedIndices = listOf(1)
                    )
                )
            )
        }

        assertEquals(403, premiumDeckException.statusCode.value())
        assertEquals(403, assistantException.statusCode.value())
    }

    @Test
    fun `프리미엄 사용자는 메인 덱 변형과 보조 오라클을 함께 사용할 수 있다`() {
        tarotDeckSeeder.run(null)

        val reading = tarotDeckService.drawReading(
            subscriptionTier = SubscriptionTier.PREMIUM,
            deckVersionId = "classic-rider-waite-signature",
            indices = listOf(1, 17, 43),
            assistantDeckSelections = listOf(
                TarotAssistantDeckSelection(
                    deckVersionId = "market-signal-oracle",
                    selectedIndices = listOf(2)
                )
            )
        )

        assertEquals("classic-rider-waite-signature", reading.cards.first().card.deckVersionId)
        assertEquals(1, reading.assistantDecks.size)
        assertEquals("market-signal-oracle", reading.assistantDecks.first().deckVersionId)
        assertEquals(listOf("CONFIRMATION"), reading.assistantDecks.first().cards.map { it.card.code })
    }
}
