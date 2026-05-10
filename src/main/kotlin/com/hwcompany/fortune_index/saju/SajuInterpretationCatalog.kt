package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.common.Sinsung
import com.hwcompany.fortune_index.common.Zodiac
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

enum class SajuInterpretationCategory {
    DAY_PILLAR,
    MONTH_BRANCH,
    TEN_STAR,
    FIVE_ELEMENT,
    FORTUNE_TYPE
}

@Entity
@Table(
    name = "saju_interpretations",
    indexes = [
        Index(name = "uk_saju_interpretations_category_code", columnList = "category, code", unique = true)
    ]
)
data class SajuInterpretationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: SajuInterpretationCategory,

    @Column(nullable = false, length = 50)
    var code: String,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(name = "summary_easy", nullable = false, length = 1000)
    var summaryEasy: String,

    @Column(name = "summary_default", nullable = false, length = 1000)
    var summaryDefault: String,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

interface SajuInterpretationRepository : JpaRepository<SajuInterpretationEntity, Long> {
    fun findByCategoryAndCode(
        category: SajuInterpretationCategory,
        code: String
    ): SajuInterpretationEntity?

    fun findByCategoryAndCodeAndActiveTrue(
        category: SajuInterpretationCategory,
        code: String
    ): SajuInterpretationEntity?
}

data class SajuInterpretation(
    val category: SajuInterpretationCategory,
    val code: String,
    val title: String,
    val summaryEasy: String,
    val summaryDefault: String
)

fun SajuInterpretationEntity.toModel(): SajuInterpretation =
    SajuInterpretation(
        category = category,
        code = code,
        title = title,
        summaryEasy = summaryEasy,
        summaryDefault = summaryDefault
    )

@Service
class SajuInterpretationService(
    private val sajuInterpretationRepository: SajuInterpretationRepository
) {
    @Transactional(readOnly = true)
    fun getInterpretation(
        category: SajuInterpretationCategory,
        code: String
    ): SajuInterpretation? =
        sajuInterpretationRepository.findByCategoryAndCodeAndActiveTrue(category, code)?.toModel()
}

@Service
class SajuInterpretationSeeder(
    private val sajuInterpretationRepository: SajuInterpretationRepository
) {
    @Transactional
    fun seed() {
        val now = LocalDateTime.now()
        val seeds = buildDayPillarSeeds(now) +
            buildMonthBranchSeeds(now) +
            buildTenStarSeeds(now) +
            buildFiveElementSeeds(now) +
            buildFortuneTypeSeeds(now)

        val upserts = seeds.map { seed ->
            sajuInterpretationRepository.findByCategoryAndCode(seed.category, seed.code)
                ?.apply {
                    title = seed.title
                    summaryEasy = seed.summaryEasy
                    summaryDefault = seed.summaryDefault
                    active = true
                    updatedAt = now
                }
                ?: seed
        }
        sajuInterpretationRepository.saveAll(upserts)
    }

    private fun buildDayPillarSeeds(now: LocalDateTime): List<SajuInterpretationEntity> =
        SajuGanji.entries.map { ganji ->
            seed(
                category = SajuInterpretationCategory.DAY_PILLAR,
                code = ganji.code,
                title = "${ganji.koreanName} (${ganji.chineseCharacter})",
                summaryEasy =
                    "${ganji.koreanName} 일주는 ${elementPhrase(ganji.stem)} 마음과 ${branchPhrase(ganji.zodiac)} 분위기를 함께 가진 모습이에요. " +
                        "차분히 자기 속도로 힘을 키우면 장점이 더 잘 보여요.",
                now = now
            )
        }

    private fun buildMonthBranchSeeds(now: LocalDateTime): List<SajuInterpretationEntity> =
        Zodiac.entries.map { zodiac ->
            seed(
                category = SajuInterpretationCategory.MONTH_BRANCH,
                code = zodiac.code,
                title = "${zodiac.koreanName} (${zodiac.chineseCharacter})",
                summaryEasy =
                    "${zodiac.koreanName} 월지는 ${branchPhrase(zodiac)} 흐름이 강해서 생활 리듬과 기분에도 그 성향이 자주 묻어나요.",
                now = now
            )
        }

    private fun buildTenStarSeeds(now: LocalDateTime): List<SajuInterpretationEntity> =
        Sinsung.entries.map { sinsung ->
            seed(
                category = SajuInterpretationCategory.TEN_STAR,
                code = sinsung.code,
                title = sinsung.koreanName,
                summaryEasy = when (sinsung) {
                    Sinsung.BIGYEON -> "내 힘으로 직접 해보려는 마음이 커져요."
                    Sinsung.GEOPJAE -> "경쟁 속에서도 내 몫을 챙기려는 마음이 커져요."
                    Sinsung.SIKSIN -> "재능과 생각을 천천히 꺼내 보여주기 좋아요."
                    Sinsung.SANGGWAN -> "표현이 많아지고 하고 싶은 말이 커질 수 있어요."
                    Sinsung.PYEONJAE -> "새 기회와 실속을 넓게 보는 흐름이 와요."
                    Sinsung.JEONGJAE -> "돈과 계획을 차곡차곡 챙기기 좋아요."
                    Sinsung.PYEONGWAN -> "규칙과 책임을 더 신경 쓰게 돼요."
                    Sinsung.JEONGGWAN -> "질서를 잘 지켜 좋은 평가를 받기 쉬워요."
                    Sinsung.PYEONIN -> "새 생각을 배우고 시야를 넓히기 좋아요."
                    Sinsung.JEONGIN -> "도움받고 배우며 기본기를 쌓기 좋아요."
                },
                now = now
            )
        }

    private fun buildFiveElementSeeds(now: LocalDateTime): List<SajuInterpretationEntity> =
        FiveElement.entries.map { element ->
            seed(
                category = SajuInterpretationCategory.FIVE_ELEMENT,
                code = element.name,
                title = fiveElementName(element),
                summaryEasy = when (element) {
                    FiveElement.WOOD -> "나무처럼 천천히 자라며 앞으로 나아가는 힘이에요."
                    FiveElement.FIRE -> "불처럼 밝고 힘차게 움직이는 힘이에요."
                    FiveElement.EARTH -> "흙처럼 차분하고 안정적으로 버티는 힘이에요."
                    FiveElement.METAL -> "쇠처럼 단단하고 분명하게 정리하는 힘이에요."
                    FiveElement.WATER -> "물처럼 부드럽고 유연하게 흐르는 힘이에요."
                },
                now = now
            )
        }

    private fun buildFortuneTypeSeeds(now: LocalDateTime): List<SajuInterpretationEntity> = listOf(
        seed(
            category = SajuInterpretationCategory.FORTUNE_TYPE,
            code = "MAJOR",
            title = "대운",
            summaryEasy = "지금은 {stemSummary} {branchSummary}",
            now = now
        ),
        seed(
            category = SajuInterpretationCategory.FORTUNE_TYPE,
            code = "YEARLY",
            title = "세운",
            summaryEasy = "올해는 {stemSummary} {branchSummary}",
            now = now
        )
    )

    private fun seed(
        category: SajuInterpretationCategory,
        code: String,
        title: String,
        summaryEasy: String,
        now: LocalDateTime
    ): SajuInterpretationEntity =
        SajuInterpretationEntity(
            category = category,
            code = code,
            title = title,
            summaryEasy = summaryEasy,
            summaryDefault = summaryEasy,
            active = true,
            createdAt = now,
            updatedAt = now
        )

    private fun elementPhrase(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (SajuAnalyzer.STEM_PROPERTIES.getValue(stem).element) {
            FiveElement.WOOD -> "나무처럼 자라나는"
            FiveElement.FIRE -> "불처럼 밝게 움직이는"
            FiveElement.EARTH -> "흙처럼 차분하게 버티는"
            FiveElement.METAL -> "쇠처럼 단단하게 정리하는"
            FiveElement.WATER -> "물처럼 유연하게 흐르는"
        }

    private fun branchPhrase(zodiac: Zodiac): String =
        when (zodiac.element) {
            FiveElement.WOOD -> "새싹처럼 자라나는"
            FiveElement.FIRE -> "따뜻하고 활기 있는"
            FiveElement.EARTH -> "안정감 있게 버티는"
            FiveElement.METAL -> "정리정돈이 잘 되는"
            FiveElement.WATER -> "부드럽게 흐르는"
        }

    private fun fiveElementName(element: FiveElement): String =
        when (element) {
            FiveElement.WOOD -> "목"
            FiveElement.FIRE -> "화"
            FiveElement.EARTH -> "토"
            FiveElement.METAL -> "금"
            FiveElement.WATER -> "수"
        }
}
