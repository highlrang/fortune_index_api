package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.stereotype.Component

@Component
class SajuAnalyzer {
    fun analyze(
        birthDateTime: LocalDateTime,
        majorFortunePillar: Pillar,
        referenceDateTime: LocalDateTime = LocalDateTime.now(DEFAULT_ZONE_ID),
        zoneId: ZoneId = DEFAULT_ZONE_ID
    ): SajuAnalysisResult {
        val natal = GanzhiCalculator.calculate(birthDateTime, zoneId)
        val hourPillar = buildHourPillar(natal.day.heavenlyStem, birthDateTime.hour)

        val characters = listOf(
            SajuCharacter.fromYearStem(natal.year.heavenlyStem),
            SajuCharacter.fromYearBranch(natal.year.earthlyBranch),
            SajuCharacter.fromMonthStem(natal.month.heavenlyStem),
            SajuCharacter.fromMonthBranch(natal.month.earthlyBranch),
            SajuCharacter.fromDayStem(natal.day.heavenlyStem),
            SajuCharacter.fromDayBranch(natal.day.earthlyBranch),
            SajuCharacter.fromHourStem(hourPillar.heavenlyStem),
            SajuCharacter.fromHourBranch(hourPillar.earthlyBranch)
        )

        val dayMaster = characters.first { it.position == SajuPosition.DAY_STEM }
        val keyPalaces = KeyPalaces(
            dayMaster = dayMaster,
            dayBranch = characters.first { it.position == SajuPosition.DAY_BRANCH },
            monthBranch = characters.first { it.position == SajuPosition.MONTH_BRANCH }
        )

        val tenGods = characters
            .filterNot { it.position == SajuPosition.DAY_STEM }
            .map { character ->
                TenGodMapping(
                    position = character.position,
                    character = character.symbol,
                    baseReference = if (character.type == SajuCharacterType.EARTHLY_BRANCH) {
                        character.referenceStem?.name
                    } else {
                        null
                    },
                    tenGod = resolveTenGod(natal.day.heavenlyStem, character)
                )
            }

        val annualFortunePillar = GanzhiCalculator.calculate(referenceDateTime, zoneId).year
        val elementBalance = calculateElementBalance(characters)
        val yinYangBalance = calculateYinYangBalance(characters)

        return SajuAnalysisResult(
            natalChart = NatalChart(
                year = natal.year,
                month = natal.month,
                day = natal.day,
                hour = hourPillar
            ),
            keyPalaces = keyPalaces,
            characters = characters,
            tenGods = tenGods,
            fiveElementBalance = elementBalance,
            yinYangBalance = yinYangBalance,
            annualFortune = buildFortuneRelationship(
                type = FortuneType.YEARLY,
                pillar = annualFortunePillar,
                dayMaster = natal.day.heavenlyStem
            ),
            majorFortune = buildFortuneRelationship(
                type = FortuneType.MAJOR,
                pillar = majorFortunePillar,
                dayMaster = natal.day.heavenlyStem
            )
        )
    }

    private fun buildFortuneRelationship(
        type: FortuneType,
        pillar: Pillar,
        dayMaster: HeavenlyStem
    ): FortuneRelationship =
        FortuneRelationship(
            type = type,
            pillar = pillar,
            stemTenGod = resolveTenGod(dayMaster, SajuCharacter.fromFortuneStem(pillar.heavenlyStem)),
            branchTenGod = resolveTenGod(dayMaster, SajuCharacter.fromFortuneBranch(pillar.earthlyBranch))
        )

    private fun calculateElementBalance(characters: List<SajuCharacter>): FiveElementBalance {
        val counts = FiveElement.entries.associateWith { element ->
            characters.count { it.fiveElement == element }
        }

        return FiveElementBalance(
            wood = counts.getValue(FiveElement.WOOD),
            fire = counts.getValue(FiveElement.FIRE),
            earth = counts.getValue(FiveElement.EARTH),
            metal = counts.getValue(FiveElement.METAL),
            water = counts.getValue(FiveElement.WATER)
        )
    }

    private fun calculateYinYangBalance(characters: List<SajuCharacter>): YinYangBalance =
        YinYangBalance(
            yinCount = characters.count { it.yinYang == YinYang.YIN },
            yangCount = characters.count { it.yinYang == YinYang.YANG }
        )

    private fun buildHourPillar(dayStem: HeavenlyStem, hour: Int): Pillar {
        val hourBranch = resolveHourBranch(hour)
        val firstHourStem = when (dayStem) {
            HeavenlyStem.GAP, HeavenlyStem.GI -> HeavenlyStem.GAP
            HeavenlyStem.EUL, HeavenlyStem.GYEONG -> HeavenlyStem.BYEONG
            HeavenlyStem.BYEONG, HeavenlyStem.SIN -> HeavenlyStem.MU
            HeavenlyStem.JEONG, HeavenlyStem.IM -> HeavenlyStem.GYEONG
            HeavenlyStem.MU, HeavenlyStem.GYE -> HeavenlyStem.IM
        }

        val branchOffset = HOUR_BRANCHES.indexOf(hourBranch)
        val hourStem = STEMS[(STEMS.indexOf(firstHourStem) + branchOffset) % STEMS.size]
        return Pillar(hourStem, hourBranch)
    }

    private fun resolveHourBranch(hour: Int): EarthlyBranch =
        when (hour) {
            23, 0 -> EarthlyBranch.JA
            1, 2 -> EarthlyBranch.CHUK
            3, 4 -> EarthlyBranch.IN
            5, 6 -> EarthlyBranch.MYO
            7, 8 -> EarthlyBranch.JIN
            9, 10 -> EarthlyBranch.SA
            11, 12 -> EarthlyBranch.O
            13, 14 -> EarthlyBranch.MI
            15, 16 -> EarthlyBranch.SIN
            17, 18 -> EarthlyBranch.YU
            19, 20 -> EarthlyBranch.SUL
            else -> EarthlyBranch.HAE
        }

    private fun resolveTenGod(dayMaster: HeavenlyStem, character: SajuCharacter): TenGod {
        val targetStem = when (character.type) {
            SajuCharacterType.HEAVENLY_STEM -> HeavenlyStem.valueOf(character.symbol)
            SajuCharacterType.EARTHLY_BRANCH -> character.referenceStem
                ?: error("branch reference stem is required for ten god mapping")
        }

        val dayElement = STEM_PROPERTIES.getValue(dayMaster).element
        val targetProperty = STEM_PROPERTIES.getValue(targetStem)
        val samePolarity = STEM_PROPERTIES.getValue(dayMaster).yinYang == targetProperty.yinYang

        return when {
            dayElement == targetProperty.element ->
                if (samePolarity) TenGod.BIGYEON else TenGod.GEOPJAE

            GENERATES.getValue(dayElement) == targetProperty.element ->
                if (samePolarity) TenGod.SIKSIN else TenGod.SANGGWAN

            CONTROLS.getValue(dayElement) == targetProperty.element ->
                if (samePolarity) TenGod.PYEONJAE else TenGod.JEONGJAE

            GENERATES.getValue(targetProperty.element) == dayElement ->
                if (samePolarity) TenGod.PYEONIN else TenGod.JEONGIN

            else ->
                if (samePolarity) TenGod.PYEONGWAN else TenGod.JEONGGWAN
        }
    }

    companion object {
        val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        val STEMS: List<HeavenlyStem> = HeavenlyStem.entries
        val HOUR_BRANCHES: List<EarthlyBranch> = listOf(
            EarthlyBranch.JA,
            EarthlyBranch.CHUK,
            EarthlyBranch.IN,
            EarthlyBranch.MYO,
            EarthlyBranch.JIN,
            EarthlyBranch.SA,
            EarthlyBranch.O,
            EarthlyBranch.MI,
            EarthlyBranch.SIN,
            EarthlyBranch.YU,
            EarthlyBranch.SUL,
            EarthlyBranch.HAE
        )
        val GENERATES: Map<FiveElement, FiveElement> = mapOf(
            FiveElement.WOOD to FiveElement.FIRE,
            FiveElement.FIRE to FiveElement.EARTH,
            FiveElement.EARTH to FiveElement.METAL,
            FiveElement.METAL to FiveElement.WATER,
            FiveElement.WATER to FiveElement.WOOD
        )
        val CONTROLS: Map<FiveElement, FiveElement> = mapOf(
            FiveElement.WOOD to FiveElement.EARTH,
            FiveElement.FIRE to FiveElement.METAL,
            FiveElement.EARTH to FiveElement.WATER,
            FiveElement.METAL to FiveElement.WOOD,
            FiveElement.WATER to FiveElement.FIRE
        )
        val STEM_PROPERTIES: Map<HeavenlyStem, StemProperty> = mapOf(
            HeavenlyStem.GAP to StemProperty(FiveElement.WOOD, YinYang.YANG),
            HeavenlyStem.EUL to StemProperty(FiveElement.WOOD, YinYang.YIN),
            HeavenlyStem.BYEONG to StemProperty(FiveElement.FIRE, YinYang.YANG),
            HeavenlyStem.JEONG to StemProperty(FiveElement.FIRE, YinYang.YIN),
            HeavenlyStem.MU to StemProperty(FiveElement.EARTH, YinYang.YANG),
            HeavenlyStem.GI to StemProperty(FiveElement.EARTH, YinYang.YIN),
            HeavenlyStem.GYEONG to StemProperty(FiveElement.METAL, YinYang.YANG),
            HeavenlyStem.SIN to StemProperty(FiveElement.METAL, YinYang.YIN),
            HeavenlyStem.IM to StemProperty(FiveElement.WATER, YinYang.YANG),
            HeavenlyStem.GYE to StemProperty(FiveElement.WATER, YinYang.YIN)
        )
        val BRANCH_PROPERTIES: Map<EarthlyBranch, BranchProperty> = mapOf(
            EarthlyBranch.JA to BranchProperty(FiveElement.WATER, YinYang.YANG, HeavenlyStem.IM),
            EarthlyBranch.CHUK to BranchProperty(FiveElement.EARTH, YinYang.YIN, HeavenlyStem.GI),
            EarthlyBranch.IN to BranchProperty(FiveElement.WOOD, YinYang.YANG, HeavenlyStem.GAP),
            EarthlyBranch.MYO to BranchProperty(FiveElement.WOOD, YinYang.YIN, HeavenlyStem.EUL),
            EarthlyBranch.JIN to BranchProperty(FiveElement.EARTH, YinYang.YANG, HeavenlyStem.MU),
            EarthlyBranch.SA to BranchProperty(FiveElement.FIRE, YinYang.YIN, HeavenlyStem.BYEONG),
            EarthlyBranch.O to BranchProperty(FiveElement.FIRE, YinYang.YANG, HeavenlyStem.JEONG),
            EarthlyBranch.MI to BranchProperty(FiveElement.EARTH, YinYang.YIN, HeavenlyStem.GI),
            EarthlyBranch.SIN to BranchProperty(FiveElement.METAL, YinYang.YANG, HeavenlyStem.GYEONG),
            EarthlyBranch.YU to BranchProperty(FiveElement.METAL, YinYang.YIN, HeavenlyStem.SIN),
            EarthlyBranch.SUL to BranchProperty(FiveElement.EARTH, YinYang.YANG, HeavenlyStem.MU),
            EarthlyBranch.HAE to BranchProperty(FiveElement.WATER, YinYang.YIN, HeavenlyStem.GYE)
        )
    }
}

data class SajuAnalysisResult(
    val natalChart: NatalChart,
    val keyPalaces: KeyPalaces,
    val characters: List<SajuCharacter>,
    val tenGods: List<TenGodMapping>,
    val fiveElementBalance: FiveElementBalance,
    val yinYangBalance: YinYangBalance,
    val annualFortune: FortuneRelationship,
    val majorFortune: FortuneRelationship
)

data class NatalChart(
    val year: Pillar,
    val month: Pillar,
    val day: Pillar,
    val hour: Pillar
)

data class KeyPalaces(
    val dayMaster: SajuCharacter,
    val dayBranch: SajuCharacter,
    val monthBranch: SajuCharacter
)

data class SajuCharacter(
    val position: SajuPosition,
    val type: SajuCharacterType,
    val symbol: String,
    val fiveElement: FiveElement,
    val yinYang: YinYang,
    val referenceStem: HeavenlyStem? = null
) {
    companion object {
        fun fromYearStem(stem: HeavenlyStem): SajuCharacter = fromStem(SajuPosition.YEAR_STEM, stem)
        fun fromMonthStem(stem: HeavenlyStem): SajuCharacter = fromStem(SajuPosition.MONTH_STEM, stem)
        fun fromDayStem(stem: HeavenlyStem): SajuCharacter = fromStem(SajuPosition.DAY_STEM, stem)
        fun fromHourStem(stem: HeavenlyStem): SajuCharacter = fromStem(SajuPosition.HOUR_STEM, stem)
        fun fromFortuneStem(stem: HeavenlyStem): SajuCharacter = fromStem(SajuPosition.FORTUNE_STEM, stem)

        fun fromYearBranch(branch: EarthlyBranch): SajuCharacter = fromBranch(SajuPosition.YEAR_BRANCH, branch)
        fun fromMonthBranch(branch: EarthlyBranch): SajuCharacter = fromBranch(SajuPosition.MONTH_BRANCH, branch)
        fun fromDayBranch(branch: EarthlyBranch): SajuCharacter = fromBranch(SajuPosition.DAY_BRANCH, branch)
        fun fromHourBranch(branch: EarthlyBranch): SajuCharacter = fromBranch(SajuPosition.HOUR_BRANCH, branch)
        fun fromFortuneBranch(branch: EarthlyBranch): SajuCharacter = fromBranch(SajuPosition.FORTUNE_BRANCH, branch)

        private fun fromStem(position: SajuPosition, stem: HeavenlyStem): SajuCharacter {
            val property = SajuAnalyzer.STEM_PROPERTIES.getValue(stem)
            return SajuCharacter(
                position = position,
                type = SajuCharacterType.HEAVENLY_STEM,
                symbol = stem.name,
                fiveElement = property.element,
                yinYang = property.yinYang,
                referenceStem = stem
            )
        }

        private fun fromBranch(position: SajuPosition, branch: EarthlyBranch): SajuCharacter {
            val property = SajuAnalyzer.BRANCH_PROPERTIES.getValue(branch)
            return SajuCharacter(
                position = position,
                type = SajuCharacterType.EARTHLY_BRANCH,
                symbol = branch.name,
                fiveElement = property.element,
                yinYang = property.yinYang,
                referenceStem = property.primaryHiddenStem
            )
        }
    }
}

data class TenGodMapping(
    val position: SajuPosition,
    val character: String,
    val baseReference: String? = null,
    val tenGod: TenGod
)

data class FiveElementBalance(
    val wood: Int,
    val fire: Int,
    val earth: Int,
    val metal: Int,
    val water: Int
)

data class YinYangBalance(
    val yinCount: Int,
    val yangCount: Int
) {
    val totalCount: Int
        get() = yinCount + yangCount
}

data class FortuneRelationship(
    val type: FortuneType,
    val pillar: Pillar,
    val stemTenGod: TenGod,
    val branchTenGod: TenGod
)

data class StemProperty(
    val element: FiveElement,
    val yinYang: YinYang
)

data class BranchProperty(
    val element: FiveElement,
    val yinYang: YinYang,
    val primaryHiddenStem: HeavenlyStem
)

enum class SajuPosition {
    YEAR_STEM,
    YEAR_BRANCH,
    MONTH_STEM,
    MONTH_BRANCH,
    DAY_STEM,
    DAY_BRANCH,
    HOUR_STEM,
    HOUR_BRANCH,
    FORTUNE_STEM,
    FORTUNE_BRANCH
}

enum class SajuCharacterType {
    HEAVENLY_STEM,
    EARTHLY_BRANCH
}

enum class FiveElement {
    WOOD,
    FIRE,
    EARTH,
    METAL,
    WATER
}

enum class YinYang {
    YIN,
    YANG
}

enum class FortuneType {
    YEARLY,
    MAJOR
}

enum class TenGod {
    BIGYEON,
    GEOPJAE,
    SIKSIN,
    SANGGWAN,
    PYEONJAE,
    JEONGJAE,
    PYEONGWAN,
    JEONGGWAN,
    PYEONIN,
    JEONGIN
}
