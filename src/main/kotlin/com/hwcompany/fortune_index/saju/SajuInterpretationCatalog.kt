package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.common.SajuGanji
import com.hwcompany.fortune_index.common.SeoulTime
import com.hwcompany.fortune_index.common.Sinsung
import com.hwcompany.fortune_index.common.Zodiac
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
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
    var createdAt: LocalDateTime = SeoulTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = SeoulTime.now()
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
        val now = SeoulTime.now()
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
                summaryEasy = dayPillarSummaryEasy(ganji),
                summaryDefault = dayPillarInvestmentSummary(ganji),
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
                    Sinsung.PYEONJAE -> "새 기회와 실속을 넓게 살피기 좋아요."
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
            summaryEasy = "{timeContext} {stemSummary} {branchSummary}",
            now = now
        ),
        seed(
            category = SajuInterpretationCategory.FORTUNE_TYPE,
            code = "YEARLY",
            title = "세운",
            summaryEasy = "{timeContext} {stemSummary} {branchSummary}",
            now = now
        )
    )

    private fun seed(
        category: SajuInterpretationCategory,
        code: String,
        title: String,
        summaryEasy: String,
        summaryDefault: String = summaryEasy,
        now: LocalDateTime
    ): SajuInterpretationEntity =
        SajuInterpretationEntity(
            category = category,
            code = code,
            title = title,
            summaryEasy = summaryEasy,
            summaryDefault = summaryDefault,
            active = true,
            createdAt = now,
            updatedAt = now
        )

    private fun dayPillarSummaryEasy(ganji: SajuGanji): String =
        "${ganji.koreanName}일은 ${elementPhrase(ganji.stem)} 천간과 ${branchPhrase(ganji.zodiac)} 지지가 함께 흐르는 날이에요. " +
            "내 타고난 성향을 말하는 것이 아니라, 오늘 하루 판단과 분위기에 깔리는 기운으로 보면 좋아요."

    private fun dayPillarInvestmentSummary(ganji: SajuGanji): String =
        "${ganji.koreanName}일은 ${elementPhrase(ganji.stem)} 천간과 ${branchPhrase(ganji.zodiac)} 지지가 만난 날입니다. " +
            "이 설명은 개인의 사주나 타고난 투자 성향이 아니라, 오늘 하루 시장을 바라볼 때 두드러지기 쉬운 분위기와 판단 리듬을 뜻합니다. " +
            "투자 운세로 보면 ${stemInvestmentMeaning(ganji.stem)} 흐름이 판단의 앞단에 놓이고, ${branchInvestmentMeaning(ganji.zodiac)} 기운이 실제 대응 속도에 영향을 줄 수 있습니다. " +
            "${ganji.koreanName}의 기운이 좋게 쓰이면 ${stemPositiveInvestmentMeaning(ganji.stem)} 흐름으로 이어지지만, 급해지면 ${branchRiskInvestmentMeaning(ganji.zodiac)} 쪽으로 흐를 수 있으니 진입 이유와 방어 기준을 함께 세우는 편이 좋습니다."

    private fun elementPhrase(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (stem) {
            HeavenlyStem.GAP -> "큰 나무처럼 곧게 뻗는"
            HeavenlyStem.EUL -> "풀과 덩굴처럼 유연하게 자라는"
            HeavenlyStem.BYEONG -> "태양처럼 밝게 드러나는"
            HeavenlyStem.JEONG -> "촛불처럼 섬세하게 밝히는"
            HeavenlyStem.MU -> "산처럼 넓고 든든하게 버티는"
            HeavenlyStem.GI -> "밭흙처럼 차분히 품어내는"
            HeavenlyStem.GYEONG -> "큰 쇠처럼 단단하게 결단하는"
            HeavenlyStem.SIN -> "보석처럼 정교하게 다듬는"
            HeavenlyStem.IM -> "큰물처럼 넓게 움직이는"
            HeavenlyStem.GYE -> "비와 안개처럼 세밀하게 스며드는"
        }

    private fun branchPhrase(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "큰물처럼 빠르게 흐르는"
            Zodiac.CHUK -> "차가운 흙처럼 차분히 다지는"
            Zodiac.IN -> "큰 나무처럼 뻗어 나가는"
            Zodiac.MYO -> "풀잎처럼 부드럽게 자라는"
            Zodiac.JIN -> "넓은 흙처럼 판을 키우는"
            Zodiac.SA -> "은근한 불씨처럼 집중되는"
            Zodiac.O -> "한낮의 불처럼 활기 있게 드러나는"
            Zodiac.MI -> "밭흙처럼 천천히 품어내는"
            Zodiac.SIN -> "큰 쇠처럼 빠르게 정리하는"
            Zodiac.YU -> "보석처럼 정교하게 가다듬는"
            Zodiac.SUL -> "마른 흙처럼 단단히 지키는"
            Zodiac.HAE -> "깊은 물처럼 조용히 스며드는"
        }

    private fun fiveElementName(element: FiveElement): String =
        when (element) {
            FiveElement.WOOD -> "목"
            FiveElement.FIRE -> "화"
            FiveElement.EARTH -> "토"
            FiveElement.METAL -> "금"
            FiveElement.WATER -> "수"
        }

    private fun stemInvestmentMeaning(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (stem) {
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GAP -> "새로운 흐름을 먼저 세우려는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.EUL -> "작은 신호를 모아 유연하게 방향을 조정하는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.BYEONG -> "드러난 흐름과 분위기가 빠르게 커지는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.JEONG -> "한 가지 근거에 집중하게 되는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.MU -> "큰 판과 중심축을 보게 되는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GI -> "현실적인 부담과 관리 가능성을 먼저 보게 되는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYEONG -> "명확한 기준으로 결정을 분명히 하려는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.SIN -> "세밀하게 비교해 선택지를 선별하는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.IM -> "큰 흐름과 유동성을 넓게 살피는"
            com.hwcompany.fortune_index.domain.model.HeavenlyStem.GYE -> "미세한 변화와 분위기를 민감하게 감지하는"
        }

    private fun branchInvestmentMeaning(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "정보와 속도에 민감하게 반응하는"
            Zodiac.CHUK -> "천천히 축적하고 버티는"
            Zodiac.IN -> "새 국면을 열고 추진하는"
            Zodiac.MYO -> "섬세하게 비중과 타이밍을 조정하는"
            Zodiac.JIN -> "변화 직전의 조건을 내부에 쌓아두는"
            Zodiac.SA -> "숨은 재료와 변화를 드러내는"
            Zodiac.O -> "활력과 속도를 크게 끌어올리는"
            Zodiac.MI -> "정리하고 보완하며 균형을 맞추는"
            Zodiac.SIN -> "전환 신호를 기민하게 포착하는"
            Zodiac.YU -> "선별하고 정리해 핵심만 남기는"
            Zodiac.SUL -> "방어 기준과 원칙을 지키는"
            Zodiac.HAE -> "다음 국면을 준비하며 흐름을 관찰하는"
        }

    private fun stemPositiveInvestmentMeaning(stem: com.hwcompany.fortune_index.domain.model.HeavenlyStem): String =
        when (stem) {
            HeavenlyStem.GAP -> "새로운 기회를 구조화하는"
            HeavenlyStem.EUL -> "상황 변화에 맞춰 유연하게 조정하는"
            HeavenlyStem.BYEONG -> "흐름의 강약을 빠르게 파악하는"
            HeavenlyStem.JEONG -> "핵심 근거를 집중해서 파고드는"
            HeavenlyStem.MU -> "포트폴리오의 중심을 흔들림 없이 지키는"
            HeavenlyStem.GI -> "현금 흐름과 리스크 규모를 현실적으로 관리하는"
            HeavenlyStem.GYEONG -> "애매한 선택을 줄이고 결정을 분명히 하는"
            HeavenlyStem.SIN -> "질 좋은 선택지를 선별하는"
            HeavenlyStem.IM -> "큰 시장 방향과 유동성을 함께 보는"
            HeavenlyStem.GYE -> "작은 이상 신호를 먼저 감지하는"
        }

    private fun branchRiskInvestmentMeaning(zodiac: Zodiac): String =
        when (zodiac) {
            Zodiac.JA -> "빠른 정보에 흔들려 확인 전 움직이는"
            Zodiac.CHUK -> "너무 오래 버티며 손절 기준을 늦추는"
            Zodiac.IN -> "초반 추진력만 믿고 리스크를 작게 보는"
            Zodiac.MYO -> "작은 신호를 과하게 해석해 타이밍이 흔들리는"
            Zodiac.JIN -> "겉으로 조용한 흐름을 안정으로 착각하는"
            Zodiac.SA -> "이미 반영된 재료를 뒤늦게 따라가는"
            Zodiac.O -> "활력과 속도에 취해 추격 판단을 하는"
            Zodiac.MI -> "정리해야 할 포지션을 미루는"
            Zodiac.SIN -> "전환 신호마다 너무 자주 방향을 바꾸는"
            Zodiac.YU -> "선별이 지나쳐 기회를 너무 좁게 보는"
            Zodiac.SUL -> "방어에 치우쳐 필요한 조정까지 늦추는"
            Zodiac.HAE -> "관찰이 길어져 실행 타이밍을 놓치는"
        }
}
