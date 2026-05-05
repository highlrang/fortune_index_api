package com.hwcompany.fortune_index.saju.investment

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.saju.Pillar
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import org.springframework.stereotype.Service

@Service
class SajuInvestmentFeatureService {
    fun extract(saju: SajuConsultingResult): SajuInvestmentFeatures {
        val natalBranches = listOf(
            saju.analysis.natalChart.year.earthlyBranch,
            saju.analysis.natalChart.month.earthlyBranch,
            saju.analysis.natalChart.day.earthlyBranch,
            saju.analysis.natalChart.hour.earthlyBranch
        )
        val natalDayPillar = saju.analysis.natalChart.day
        val branchStageCounts = BranchStageCounts(
            saengji = natalBranches.count { it in SAENGJI_BRANCHES },
            wangji = natalBranches.count { it in WANGJI_BRANCHES },
            myoji = natalBranches.count { it in MYOJI_BRANCHES }
        )
        val hiddenElementRatios = calculateHiddenElementRatios(natalBranches)
        val relationSignals = buildList {
            addAll(
                analyzeStemHap(
                    natalDayPillar.heavenlyStem,
                    saju.currentFortune.majorFortune.pillar.heavenlyStem,
                    "MAJOR_FLOW"
                )
            )
            addAll(
                analyzeStemHap(
                    natalDayPillar.heavenlyStem,
                    saju.currentFortune.yearlyFortune.pillar.heavenlyStem,
                    "YEARLY_FLOW"
                )
            )
            addAll(
                analyzeBranchChung(
                    natalDayPillar.earthlyBranch,
                    saju.currentFortune.majorFortune.pillar.earthlyBranch,
                    "MAJOR_FLOW"
                )
            )
            addAll(
                analyzeBranchChung(
                    natalDayPillar.earthlyBranch,
                    saju.currentFortune.yearlyFortune.pillar.earthlyBranch,
                    "YEARLY_FLOW"
                )
            )
        }

        val baseTraits = linkedSetOf<String>().apply {
            if (branchStageCounts.wangji >= 2) {
                add("conviction_strong")
                add("style_consistent")
            }
            if (branchStageCounts.saengji >= 2) {
                add("execution_fast")
                add("trend_sensitive")
            }
            if (branchStageCounts.myoji >= 2) {
                add("asset_locking")
                add("rebalancing_needed")
            }
            if (isHighRatio(hiddenElementRatios, "WATER")) add("inner_water_high")
            if (isHighRatio(hiddenElementRatios, "FIRE")) add("inner_fire_high")
            if (isHighRatio(hiddenElementRatios, "WOOD")) add("inner_wood_high")
            if (isHighRatio(hiddenElementRatios, "METAL")) add("inner_metal_high")
            if (isHighRatio(hiddenElementRatios, "EARTH")) add("inner_earth_high")
            if (isEmpty()) add("balanced_temper")
        }.toList()

        val dynamicSignals = linkedSetOf<String>().apply {
            add(stageSignalFor(saju.currentFortune.majorFortune.pillar, "major"))
            add(stageSignalFor(saju.currentFortune.yearlyFortune.pillar, "yearly"))
            relationSignals.filter { it.type == "HAP" }.forEach { add("transition_signal") }
        }.toList()

        val riskFlags = linkedSetOf<String>().apply {
            relationSignals.filter { it.type == "CHUNG" }.forEach { add("volatility_risk") }
            if (relationSignals.any { it.type == "CHUNG" && it.target == "YEARLY_FLOW" }) {
                add("self_conflict")
            }
            if (branchStageCounts.myoji >= 2) add("asset_locking")
            if (branchStageCounts.wangji >= 3) add("conviction_overheat")
        }.toList()

        val confidence = (55 + baseTraits.size * 5 + relationSignals.size * 3).coerceIn(55, 90)

        return SajuInvestmentFeatures(
            baseTraits = baseTraits,
            dynamicSignals = dynamicSignals,
            riskFlags = riskFlags,
            hiddenElementRatios = hiddenElementRatios,
            branchStageCounts = branchStageCounts,
            relationSignals = relationSignals,
            confidence = confidence
        )
    }

    private fun stageSignalFor(pillar: Pillar, scope: String): String =
        when (pillar.earthlyBranch) {
            in SAENGJI_BRANCHES -> "${scope}_stage_saengji"
            in WANGJI_BRANCHES -> "${scope}_stage_wangji"
            in MYOJI_BRANCHES -> "${scope}_stage_myoji"
            else -> "${scope}_stage_neutral"
        }

    private fun analyzeStemHap(
        natalStem: HeavenlyStem,
        flowStem: HeavenlyStem,
        target: String
    ): List<RelationSignal> {
        val code = STEM_HAP_CODES[setOf(natalStem, flowStem)] ?: return emptyList()
        return listOf(
            RelationSignal(
                type = "HAP",
                code = code,
                target = target,
                weight = 4,
                messageKey = "transition_signal"
            )
        )
    }

    private fun analyzeBranchChung(
        natalBranch: EarthlyBranch,
        flowBranch: EarthlyBranch,
        target: String
    ): List<RelationSignal> {
        val code = BRANCH_CHUNG_CODES[setOf(natalBranch, flowBranch)] ?: return emptyList()
        return listOf(
            RelationSignal(
                type = "CHUNG",
                code = code,
                target = target,
                weight = 8,
                messageKey = "volatility_risk"
            )
        )
    }

    private fun calculateHiddenElementRatios(branches: List<EarthlyBranch>): Map<String, Int> {
        val totals = linkedMapOf(
            "WOOD" to 0,
            "FIRE" to 0,
            "EARTH" to 0,
            "METAL" to 0,
            "WATER" to 0
        )
        branches.flatMap { HIDDEN_STEMS.getValue(it) }
            .forEach { hidden ->
                totals.compute(hidden.element) { _, current -> (current ?: 0) + hidden.weight }
            }
        val sum = totals.values.sum().coerceAtLeast(1)
        val normalized = totals.mapValues { (_, value) -> value * 100 / sum }.toMutableMap()
        val remainder = 100 - normalized.values.sum()
        val maxKey = totals.maxByOrNull { it.value }?.key
        if (maxKey != null) {
            normalized[maxKey] = (normalized[maxKey] ?: 0) + remainder
        }
        return normalized
    }

    private fun isHighRatio(ratios: Map<String, Int>, element: String): Boolean =
        (ratios[element] ?: 0) >= 30

    private data class HiddenStem(val element: String, val weight: Int)

    companion object {
        private val SAENGJI_BRANCHES = setOf(EarthlyBranch.IN, EarthlyBranch.SIN, EarthlyBranch.SA, EarthlyBranch.HAE)
        private val WANGJI_BRANCHES = setOf(EarthlyBranch.JA, EarthlyBranch.O, EarthlyBranch.MYO, EarthlyBranch.YU)
        private val MYOJI_BRANCHES = setOf(EarthlyBranch.JIN, EarthlyBranch.SUL, EarthlyBranch.CHUK, EarthlyBranch.MI)

        private val STEM_HAP_CODES = mapOf(
            setOf(HeavenlyStem.GAP, HeavenlyStem.GI) to "GAP_GI_HAP_TO",
            setOf(HeavenlyStem.EUL, HeavenlyStem.GYEONG) to "EUL_GYEONG_HAP_GEUM",
            setOf(HeavenlyStem.BYEONG, HeavenlyStem.SIN) to "BYEONG_SIN_HAP_SU",
            setOf(HeavenlyStem.JEONG, HeavenlyStem.IM) to "JEONG_IM_HAP_MOK",
            setOf(HeavenlyStem.MU, HeavenlyStem.GYE) to "MU_GYE_HAP_HWA"
        )

        private val BRANCH_CHUNG_CODES = mapOf(
            setOf(EarthlyBranch.JA, EarthlyBranch.O) to "JA_O_CHUNG",
            setOf(EarthlyBranch.SA, EarthlyBranch.HAE) to "SA_HAE_CHUNG",
            setOf(EarthlyBranch.MYO, EarthlyBranch.YU) to "MYO_YU_CHUNG",
            setOf(EarthlyBranch.IN, EarthlyBranch.SIN) to "IN_SIN_CHUNG",
            setOf(EarthlyBranch.JIN, EarthlyBranch.SUL) to "JIN_SUL_CHUNG",
            setOf(EarthlyBranch.CHUK, EarthlyBranch.MI) to "CHUK_MI_CHUNG"
        )

        private val HIDDEN_STEMS = mapOf(
            EarthlyBranch.JA to listOf(HiddenStem("WATER", 30)),
            EarthlyBranch.CHUK to listOf(HiddenStem("WATER", 9), HiddenStem("METAL", 3), HiddenStem("EARTH", 18)),
            EarthlyBranch.IN to listOf(HiddenStem("EARTH", 7), HiddenStem("FIRE", 7), HiddenStem("WOOD", 16)),
            EarthlyBranch.MYO to listOf(HiddenStem("WOOD", 30)),
            EarthlyBranch.JIN to listOf(HiddenStem("WOOD", 9), HiddenStem("WATER", 3), HiddenStem("EARTH", 18)),
            EarthlyBranch.SA to listOf(HiddenStem("EARTH", 7), HiddenStem("METAL", 7), HiddenStem("FIRE", 16)),
            EarthlyBranch.O to listOf(HiddenStem("FIRE", 21), HiddenStem("EARTH", 9)),
            EarthlyBranch.MI to listOf(HiddenStem("WOOD", 9), HiddenStem("FIRE", 3), HiddenStem("EARTH", 18)),
            EarthlyBranch.SIN to listOf(HiddenStem("EARTH", 7), HiddenStem("WATER", 7), HiddenStem("METAL", 16)),
            EarthlyBranch.YU to listOf(HiddenStem("METAL", 30)),
            EarthlyBranch.SUL to listOf(HiddenStem("METAL", 9), HiddenStem("FIRE", 3), HiddenStem("EARTH", 18)),
            EarthlyBranch.HAE to listOf(HiddenStem("EARTH", 7), HiddenStem("WOOD", 7), HiddenStem("WATER", 16))
        )
    }
}
