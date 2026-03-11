package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GanzhiCalculatorTest {
    @Test
    fun `입춘 전 날짜는 이전 간지년으로 계산한다`() {
        val result = GanzhiCalculator.calculate(LocalDateTime.of(1984, 2, 3, 12, 0))

        assertEquals(Pillar(HeavenlyStem.GYE, EarthlyBranch.HAE), result.year)
        assertEquals(Pillar(HeavenlyStem.EUL, EarthlyBranch.CHUK), result.month)
        assertEquals(Pillar(HeavenlyStem.JEONG, EarthlyBranch.MYO), result.day)
    }

    @Test
    fun `입춘 이후 날짜는 새로운 간지년과 인월로 계산한다`() {
        val result = GanzhiCalculator.calculate(LocalDateTime.of(1984, 2, 4, 12, 0))

        assertEquals(Pillar(HeavenlyStem.GAP, EarthlyBranch.JA), result.year)
        assertEquals(Pillar(HeavenlyStem.BYEONG, EarthlyBranch.IN), result.month)
        assertEquals(Pillar(HeavenlyStem.MU, EarthlyBranch.JIN), result.day)
    }

    @Test
    fun `절입 기준 월주는 연간에 따라 계산한다`() {
        val result = GanzhiCalculator.calculate(LocalDateTime.of(1984, 6, 2, 9, 30))

        assertEquals(Pillar(HeavenlyStem.GAP, EarthlyBranch.JA), result.year)
        assertEquals(Pillar(HeavenlyStem.GI, EarthlyBranch.SA), result.month)
        assertEquals(Pillar(HeavenlyStem.JEONG, EarthlyBranch.MYO), result.day)
    }
}
