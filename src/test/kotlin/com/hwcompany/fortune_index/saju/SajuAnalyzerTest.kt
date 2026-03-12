package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SajuAnalyzerTest {
    private val analyzer = SajuAnalyzer()

    @Test
    fun `핵심 궁위와 십성 밸런스 정보를 반환한다`() {
        val result = analyzer.analyze(
            birthDateTime = LocalDateTime.of(1984, 2, 4, 12, 0),
            majorFortunePillar = Pillar(HeavenlyStem.GYEONG, EarthlyBranch.O),
            referenceDateTime = LocalDateTime.of(2026, 3, 12, 9, 0)
        )

        assertEquals(SajuPosition.DAY_STEM, result.keyPalaces.dayMaster.position)
        assertEquals(7, result.tenGods.size)
        assertEquals(8, result.yinYangBalance.totalCount)
        assertEquals(
            result.fiveElementBalance.wood +
                result.fiveElementBalance.fire +
                result.fiveElementBalance.earth +
                result.fiveElementBalance.metal +
                result.fiveElementBalance.water,
            8
        )
        assertEquals(Pillar(HeavenlyStem.BYEONG, EarthlyBranch.O), result.annualFortune.pillar)
        assertEquals(TenGod.SIKSIN, result.majorFortune.stemTenGod)
    }
}
