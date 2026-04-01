package com.hwcompany.fortune_index.consulting

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.hwcompany.fortune_index.domain.model.labelKo
import com.hwcompany.fortune_index.saju.SajuCharacter
import com.hwcompany.fortune_index.saju.SajuConsultingResult
import com.hwcompany.fortune_index.saju.SajuCoreEnergy
import com.hwcompany.fortune_index.saju.SajuPosition
import com.hwcompany.fortune_index.saju.TenGod
import com.hwcompany.fortune_index.saju.TenGodMapping
import com.hwcompany.fortune_index.saju.TenStar

internal fun SajuConsultingResult.toAiPayload(): Map<String, Any?> =
    linkedMapOf(
        "summary" to mapOf(
            "dayMaster" to dayMaster.toAiPayload("일간"),
            "dayBranch" to dayBranch.toAiPayload("일지"),
            "monthBranch" to monthBranch.toAiPayload("월지")
        ),
        "natalChart" to mapOf(
            "year" to analysis.natalChart.year.toAiPayload("연주"),
            "month" to analysis.natalChart.month.toAiPayload("월주"),
            "day" to analysis.natalChart.day.toAiPayload("일주"),
            "hour" to analysis.natalChart.hour.toAiPayload("시주")
        ),
        "characters" to analysis.characters.map { it.toAiPayload() },
        "tenGods" to analysis.tenGods.map { it.toAiPayload() },
        "fiveElementBalance" to mapOf(
            "wood" to analysis.fiveElementBalance.wood,
            "fire" to analysis.fiveElementBalance.fire,
            "earth" to analysis.fiveElementBalance.earth,
            "metal" to analysis.fiveElementBalance.metal,
            "water" to analysis.fiveElementBalance.water,
            "description" to "각 오행이 사주 원국에 몇 개 분포하는지 나타내는 개수다."
        ),
        "yinYangBalance" to mapOf(
            "yinCount" to analysis.yinYangBalance.yinCount,
            "yangCount" to analysis.yinYangBalance.yangCount,
            "description" to "음과 양의 분포 개수다."
        ),
        "currentFortune" to mapOf(
            "referenceYear" to currentFortune.referenceYear,
            "majorFortune" to mapOf(
                "sequence" to currentFortune.majorFortune.sequence,
                "startAge" to currentFortune.majorFortune.startAge,
                "endAge" to currentFortune.majorFortune.endAge,
                "pillar" to currentFortune.majorFortune.pillar.toAiPayload("대운"),
                "stemTenStar" to currentFortune.majorFortune.stemTenStar.toAiPayload(),
                "branchTenStar" to currentFortune.majorFortune.branchTenStar.toAiPayload(),
                "description" to "현재 속한 대운 구간 정보다."
            ),
            "yearlyFortune" to mapOf(
                "year" to currentFortune.yearlyFortune.year,
                "pillar" to currentFortune.yearlyFortune.pillar.toAiPayload("세운"),
                "stemTenStar" to currentFortune.yearlyFortune.stemTenStar.toAiPayload(),
                "branchTenStar" to currentFortune.yearlyFortune.branchTenStar.toAiPayload()
            )
        )
    )

internal fun com.hwcompany.fortune_index.saju.Pillar.toAiPayload(label: String): Map<String, Any> =
    mapOf(
        "label" to label,
        "stem" to heavenlyStem.toAiPayload(),
        "branch" to earthlyBranch.toAiPayload(),
        "combinedLabelKo" to "${heavenlyStem.labelKo()}${earthlyBranch.labelKo()}"
    )

internal fun HeavenlyStem.toAiPayload(): Map<String, Any> =
    mapOf(
        "code" to name,
        "labelKo" to labelKo()
    )

internal fun EarthlyBranch.toAiPayload(): Map<String, Any> =
    mapOf(
        "code" to name,
        "labelKo" to labelKo()
    )

internal fun SajuCoreEnergy.toAiPayload(label: String): Map<String, Any?> =
    mapOf(
        "label" to label,
        "code" to symbol,
        "labelKo" to symbol.toKoreanSymbol(),
        "fiveElement" to fiveElement.toAiPayload(),
        "yinYang" to yinYang.toAiPayload()
    )

internal fun SajuCharacter.toAiPayload(): Map<String, Any?> =
    mapOf(
        "position" to position.name,
        "positionLabelKo" to position.toPositionLabelKo(),
        "type" to type.name,
        "symbolCode" to symbol,
        "symbolLabelKo" to symbol.toKoreanSymbol(),
        "fiveElement" to fiveElement.toAiPayload(),
        "yinYang" to yinYang.toAiPayload(),
        "referenceStemCode" to referenceStem?.name,
        "referenceStemLabelKo" to referenceStem?.labelKo()
    )

internal fun TenGodMapping.toAiPayload(): Map<String, Any?> =
    mapOf(
        "position" to position.name,
        "positionLabelKo" to position.toPositionLabelKo(),
        "characterCode" to character,
        "characterLabelKo" to character.toKoreanSymbol(),
        "baseReferenceCode" to baseReference,
        "baseReferenceLabelKo" to baseReference?.toKoreanSymbol(),
        "tenGod" to tenGod.toAiPayload()
    )

internal fun com.hwcompany.fortune_index.saju.FiveElement.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            com.hwcompany.fortune_index.saju.FiveElement.WOOD -> "목"
            com.hwcompany.fortune_index.saju.FiveElement.FIRE -> "화"
            com.hwcompany.fortune_index.saju.FiveElement.EARTH -> "토"
            com.hwcompany.fortune_index.saju.FiveElement.METAL -> "금"
            com.hwcompany.fortune_index.saju.FiveElement.WATER -> "수"
        }
    )

internal fun com.hwcompany.fortune_index.saju.YinYang.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            com.hwcompany.fortune_index.saju.YinYang.YIN -> "음"
            com.hwcompany.fortune_index.saju.YinYang.YANG -> "양"
        }
    )

internal fun TenGod.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            TenGod.BIGYEON -> "비견"
            TenGod.GEOPJAE -> "겁재"
            TenGod.SIKSIN -> "식신"
            TenGod.SANGGWAN -> "상관"
            TenGod.PYEONJAE -> "편재"
            TenGod.JEONGJAE -> "정재"
            TenGod.PYEONGWAN -> "편관"
            TenGod.JEONGGWAN -> "정관"
            TenGod.PYEONIN -> "편인"
            TenGod.JEONGIN -> "정인"
        }
    )

internal fun TenStar.toAiPayload(): Map<String, String> =
    mapOf(
        "code" to name,
        "labelKo" to when (this) {
            TenStar.BIGYEON -> "비견"
            TenStar.GEOPJAE -> "겁재"
            TenStar.SIKSIN -> "식신"
            TenStar.SANGGWAN -> "상관"
            TenStar.PYEONJAE -> "편재"
            TenStar.JEONGJAE -> "정재"
            TenStar.PYEONGWAN -> "편관"
            TenStar.JEONGGWAN -> "정관"
            TenStar.PYEONIN -> "편인"
            TenStar.JEONGIN -> "정인"
        }
    )

internal fun SajuPosition.toPositionLabelKo(): String =
    when (this) {
        SajuPosition.YEAR_STEM -> "연간"
        SajuPosition.YEAR_BRANCH -> "연지"
        SajuPosition.MONTH_STEM -> "월간"
        SajuPosition.MONTH_BRANCH -> "월지"
        SajuPosition.DAY_STEM -> "일간"
        SajuPosition.DAY_BRANCH -> "일지"
        SajuPosition.HOUR_STEM -> "시간"
        SajuPosition.HOUR_BRANCH -> "시지"
        SajuPosition.FORTUNE_STEM -> "운간"
        SajuPosition.FORTUNE_BRANCH -> "운지"
    }

internal fun String.toKoreanSymbol(): String =
    HeavenlyStem.entries.firstOrNull { it.name == this }?.labelKo()
        ?: EarthlyBranch.entries.firstOrNull { it.name == this }?.labelKo()
        ?: this

internal fun pillarLabel(pillarOrder: Int, isStem: Boolean): String =
    when (pillarOrder) {
        1 -> if (isStem) "연간" else "연지"
        2 -> if (isStem) "월간" else "월지"
        3 -> if (isStem) "일간" else "일지"
        4 -> if (isStem) "시간" else "시지"
        else -> if (isStem) "천간" else "지지"
    }
