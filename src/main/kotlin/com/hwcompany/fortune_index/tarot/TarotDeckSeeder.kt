package com.hwcompany.fortune_index.tarot

import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.domain.model.SubscriptionTier
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

class TarotDeckSeeder(
    private val tarotDeckVersionRepository: TarotDeckVersionRepository,
    private val tarotCardMetadataRepository: TarotCardMetadataRepository,
    private val tarotBirthCardRepository: TarotBirthCardRepository
) {

    @Transactional
    fun seed() {
        val now = SeoulTime.now()

        upsertMainDeck(
            id = DEFAULT_TAROT_DECK_VERSION_ID,
            name = "클래식 라이더",
            description = "기본 타로 덱",
            coverImageUrl = "https://cdn.example.com/tarot/classic/cover.png",
            displayOrder = 1,
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
            displayOrder = 2,
            requiredSubscriptionTier = SubscriptionTier.PREMIUM,
            now = now,
            imageUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.png"
            },
            videoUrlResolver = { metadata ->
                "https://cdn.example.com/tarot/signature/${metadata.sortOrder.toString().padStart(3, '0')}.mp4"
            }
        )

        upsertOracleDeck(now)
        upsertBirthCards()
    }

    private fun upsertMainDeck(
        id: String,
        name: String,
        description: String,
        coverImageUrl: String,
        displayOrder: Int,
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
            displayOrder = displayOrder,
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
            id = "investment-signal-oracle",
            name = "투자 시그널 오라클",
            description = "프리미엄 전용 보조 오라클 카드",
            coverImageUrl = "https://cdn.example.com/oracle/investment-signal/cover.png",
            deckType = TarotDeckType.ORACLE,
            deckRole = TarotDeckRole.ASSISTANT,
            cardSetId = "investment-signal-oracle",
            drawCount = 1,
            displayOrder = 3,
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
                imageUrl = "https://cdn.example.com/oracle/investment-signal/000.png",
                videoUrl = "https://cdn.example.com/oracle/investment-signal/000.mp4"
            ),
            OracleSeedCard(
                selectedIndex = 1,
                code = "VOLATILITY_SPIKE",
                name = "Volatility Spike",
                koreanName = "변동성 급등",
                meaning = "방향성보다 변동성 관리가 먼저다.",
                description = "좋은 흐름이어도 진입 속도와 위험 관리 기준을 더 촘촘하게 잡아야 하는 구간을 뜻한다.",
                imageUrl = "https://cdn.example.com/oracle/investment-signal/001.png",
                videoUrl = "https://cdn.example.com/oracle/investment-signal/001.mp4"
            ),
            OracleSeedCard(
                selectedIndex = 2,
                code = "CONFIRMATION",
                name = "Confirmation",
                koreanName = "확인 신호",
                meaning = "기존 판단을 재확인해도 되는 구간이다.",
                description = "보조 지표와 타이밍이 맞물리는 만큼, 기존 전략을 유지하되 과신은 피하라는 카드다.",
                imageUrl = "https://cdn.example.com/oracle/investment-signal/002.png",
                videoUrl = "https://cdn.example.com/oracle/investment-signal/002.mp4"
            )
        ).forEach { card ->
            upsertCard(
                deckVersion = deck,
                selectedIndex = card.selectedIndex,
                code = card.code,
                deckType = TarotDeckType.ORACLE,
                deckRole = TarotDeckRole.ASSISTANT,
                cardSetId = "investment-signal-oracle",
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

    private fun upsertBirthCards() {
        majorBirthCardSeeds().forEach { seed ->
            val existing = tarotBirthCardRepository.findByCardSetIdAndCode(
                cardSetId = DEFAULT_TAROT_CARD_SET_ID,
                code = seed.card.code
            )
            tarotBirthCardRepository.save(
                existing?.apply {
                    this.arcanaType = TarotArcanaType.MAJOR
                    this.birthMeaning = seed.birthMeaning
                    this.birthDescription = seed.birthDescription
                } ?: TarotBirthCardEntity(
                    cardSetId = DEFAULT_TAROT_CARD_SET_ID,
                    code = seed.card.code,
                    arcanaType = TarotArcanaType.MAJOR,
                    birthMeaning = seed.birthMeaning,
                    birthDescription = seed.birthDescription
                )
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
        displayOrder: Int,
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
                this.displayOrder = displayOrder
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
                displayOrder = displayOrder,
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

private data class MajorBirthCardSeed(
    val card: TarotCard,
    val birthMeaning: String,
    val birthDescription: String
)

private fun majorBirthCardSeeds(): List<MajorBirthCardSeed> = listOf(
    MajorBirthCardSeed(
        card = TarotCard.THE_FOOL,
        birthMeaning = "타고난 개척성, 순수한 추진력, 새 판을 여는 사람",
        birthDescription = "생일카드로서의 바보는 운의 기복보다 삶을 시작하는 방식에 더 가깝다. 익숙한 틀에 오래 머무르기보다 직접 부딪치며 길을 만들고, 주변이 망설일 때 먼저 움직이며 흐름을 연다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_MAGICIAN,
        birthMeaning = "의지, 설계력, 재능을 현실로 바꾸는 사람",
        birthDescription = "생일카드로서의 마법사는 가진 자원을 조합해 결과를 만드는 성향을 말한다. 말과 생각만 많은 사람이 아니라 손에 잡히는 형태로 구현해 내며, 스스로 판을 설계할 때 힘이 가장 잘 살아난다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_HIGH_PRIESTESS,
        birthMeaning = "깊은 직관, 관찰력, 쉽게 흔들리지 않는 내면",
        birthDescription = "생일카드로서의 여사제는 조용하지만 밀도 높은 감각을 상징한다. 겉으로 많이 드러내지 않아도 핵심을 빨리 읽고, 서두르지 않고 충분히 감지한 뒤 움직일 때 판단의 정밀도가 높아진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_EMPRESS,
        birthMeaning = "풍요를 키우는 감각, 돌봄, 현실적 성장력",
        birthDescription = "생일카드로서의 여황제는 무언가를 안정적으로 자라게 하는 재능을 뜻한다. 사람과 일, 자원 모두를 메마르지 않게 관리하는 힘이 있고, 단기 성과보다 건강하게 키우는 과정에서 존재감이 커진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_EMPEROR,
        birthMeaning = "구조화 능력, 책임감, 기준을 세우는 리더십",
        birthDescription = "생일카드로서의 황제는 혼란 속에서도 기준을 세우려는 성향을 드러낸다. 감정만으로 결정하기보다 원칙과 역할을 분명히 하고, 책임을 떠안을 준비가 되었을 때 주변의 신뢰도 함께 따라온다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_HIEROPHANT,
        birthMeaning = "배움, 전수, 검증된 가치를 이어 가는 사람",
        birthDescription = "생일카드로서의 교황은 전통과 지식을 자기식으로 정리해 나누는 성향을 뜻한다. 새로움만 좇기보다 축적된 기준 안에서 의미를 찾고, 누군가에게 방향을 설명하거나 안내할 때 강점이 살아난다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_LOVERS,
        birthMeaning = "관계 감각, 가치 선택, 마음과 기준의 정렬",
        birthDescription = "생일카드로서의 연인은 관계 운보다 어떤 기준으로 연결되고 선택하는 사람인가를 보여준다. 사람 사이의 온도와 가치 충돌에 민감하며, 진심과 방향이 맞을 때 삶의 추진력도 크게 올라간다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_CHARIOT,
        birthMeaning = "집중력, 추진력, 목표를 향해 밀고 가는 힘",
        birthDescription = "생일카드로서의 전차는 여러 감정과 조건을 하나의 방향으로 모으는 능력을 뜻한다. 흔들림이 없을수록 속도가 붙고, 명확한 목표를 잡았을 때 주변 환경보다 자기 통제력으로 결과를 만든다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.STRENGTH,
        birthMeaning = "부드러운 강인함, 감정 조율, 오래 버티는 힘",
        birthDescription = "생일카드로서의 힘은 세게 누르는 방식보다 차분히 다루는 내구성을 상징한다. 감정이 크더라도 쉽게 무너지지 않고, 사람과 상황을 세심하게 다루며 긴 호흡으로 성과를 만드는 타입에 가깝다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_HERMIT,
        birthMeaning = "성찰, 탐구, 자기 기준으로 답을 찾는 사람",
        birthDescription = "생일카드로서의 은둔자는 고립보다 깊이를 뜻한다. 시끄러운 환경보다 혼자 정리하는 시간에서 통찰이 잘 나오고, 남의 속도보다 자신의 기준을 확인한 뒤 움직여야 실수가 줄어든다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.WHEEL_OF_FORTUNE,
        birthMeaning = "변화 대응력, 전환 감각, 흐름을 타는 사람",
        birthDescription = "생일카드로서의 운명의 수레바퀴는 운 자체보다 변화 국면에서 유연하게 움직이는 자질을 말한다. 한 자리에 고정되기보다 국면 전환을 빠르게 읽고, 타이밍을 잡을 때 존재감이 커진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.JUSTICE,
        birthMeaning = "균형 감각, 공정성, 사실 중심의 판단",
        birthDescription = "생일카드로서의 정의는 정서보다 기준과 균형을 우선하는 성향을 뜻한다. 편을 들기보다 맥락을 정리하고, 감정이 복잡한 상황에서도 핵심 기준을 다시 세우는 역할을 자주 맡게 된다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_HANGED_MAN,
        birthMeaning = "관점 전환, 유예의 지혜, 쉽게 단정하지 않는 사람",
        birthDescription = "생일카드로서의 매달린 남자는 느림이 약점이 아니라 사고의 방식임을 보여준다. 당장 결론을 내리기보다 다른 시각을 찾고, 멈춤 속에서 의미를 재구성할 때 오히려 더 큰 통찰을 만든다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.DEATH,
        birthMeaning = "정리 능력, 본질적 전환, 낡은 것을 끝내는 힘",
        birthDescription = "생일카드로서의 죽음은 불길함이 아니라 삶의 단계 전환과 정리력을 상징한다. 붙잡고 버티기보다 필요한 끝맺음을 받아들이는 편이며, 덕분에 더 빨리 다음 구조를 만들 수 있다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.TEMPERANCE,
        birthMeaning = "조율, 완급 조절, 서로 다른 것을 섞는 감각",
        birthDescription = "생일카드로서의 절제는 극단으로 치우치지 않고 균형점을 만들어 내는 성향을 뜻한다. 사람, 일, 감정 사이의 간격을 잘 맞추며, 상반된 요소를 무리 없이 연결하는 중재자 기질이 있다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_DEVIL,
        birthMeaning = "강한 욕망, 몰입, 인간적 본능을 직시하는 사람",
        birthDescription = "생일카드로서의 악마는 단순한 부정성이 아니라 욕망과 집착의 메커니즘을 강하게 체감하는 성향을 보여준다. 무엇에 강하게 끌리는지 자각할수록 몰입은 재능이 되고, 무의식적 반복은 줄어든다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_TOWER,
        birthMeaning = "가짜 구조를 깨는 힘, 급진적 갱신, 정직한 전환",
        birthDescription = "생일카드로서의 탑은 충격을 부르는 사람이기보다, 맞지 않는 구조를 더는 유지하지 못하는 성향에 가깝다. 불편한 진실을 피하지 않고, 무너진 뒤 다시 세우는 과정에서 성장 폭이 커진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_STAR,
        birthMeaning = "회복력, 희망, 멀리 보는 비전",
        birthDescription = "생일카드로서의 별은 낙관만이 아니라 회복 후에도 방향을 잃지 않는 성향을 뜻한다. 쉽게 소진되지 않도록 자신만의 리듬을 지키고, 긴 흐름 안에서 의미를 찾을 때 매력이 또렷해진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_MOON,
        birthMeaning = "섬세한 감수성, 상상력, 보이지 않는 흐름 감지",
        birthDescription = "생일카드로서의 달은 예민함과 상상력이 함께 큰 유형을 보여준다. 분위기와 미세한 변화를 잘 읽지만 감정의 파도도 크게 느끼므로, 스스로를 안정시키는 기준을 가질수록 재능이 선명해진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_SUN,
        birthMeaning = "생기, 명료함, 존재 자체로 주변을 밝히는 힘",
        birthDescription = "생일카드로서의 태양은 단순한 행운보다 자기 에너지를 바깥으로 선명하게 드러내는 성향을 의미한다. 솔직하고 직선적인 매력이 강하고, 숨기지 않을수록 주변에도 활력과 신뢰를 만든다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.JUDGEMENT,
        birthMeaning = "각성, 자기 갱신, 지난 경험을 새 단계로 바꾸는 힘",
        birthDescription = "생일카드로서의 심판은 과거를 덮기보다 해석을 바꿔 다시 일어서는 성향을 뜻한다. 삶의 전환점을 자주 의식하고, 이전 경험을 새로운 부름으로 연결할 때 성장의 속도가 빨라진다."
    ),
    MajorBirthCardSeed(
        card = TarotCard.THE_WORLD,
        birthMeaning = "완성도, 통합력, 넓은 시야로 마무리하는 사람",
        birthDescription = "생일카드로서의 세계는 큰 그림 속에서 각각의 조각을 연결하는 성향을 보여준다. 시작보다 마무리와 통합에서 강하고, 경험을 하나의 구조로 정리해 다음 단계의 기반으로 바꾸는 힘이 있다."
    )
)
