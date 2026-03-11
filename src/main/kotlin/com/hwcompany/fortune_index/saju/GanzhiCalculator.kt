package com.hwcompany.fortune_index.saju

import com.hwcompany.fortune_index.domain.model.EarthlyBranch
import com.hwcompany.fortune_index.domain.model.HeavenlyStem
import com.ibm.icu.util.ChineseCalendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object GanzhiCalculator {
    private val defaultZoneId: ZoneId = ZoneId.of("Asia/Seoul")
    private val referenceDay: LocalDate = LocalDate.of(1984, 2, 2)
    private const val referenceDaySexagenaryIndex: Int = 2 // 1984-02-02 = 丙寅

    fun calculate(dateTime: LocalDateTime, zoneId: ZoneId = defaultZoneId): GanzhiResult {
        val zonedDateTime = dateTime.atZone(zoneId)
        val chineseCalendar = ChineseCalendar(
            TimeZone.getTimeZone(zoneId.id),
            ULocale("ko_KR@calendar=chinese")
        ).apply {
            timeInMillis = zonedDateTime.toInstant().toEpochMilli()
        }

        val localDate = zonedDateTime.toLocalDate()
        val adjustedSolarYear = if (isBeforeIpchun(localDate)) localDate.year - 1 else localDate.year
        val solarMonthIndex = resolveSolarMonthIndex(localDate)

        val yearPillar = buildYearPillar(adjustedSolarYear)
        val monthPillar = buildMonthPillar(yearPillar.heavenlyStem, solarMonthIndex)
        val dayPillar = buildDayPillar(localDate)

        return GanzhiResult(
            year = yearPillar,
            month = monthPillar,
            day = dayPillar,
            lunarYearOfCycle = chineseCalendar.get(ChineseCalendar.YEAR),
            lunarMonth = chineseCalendar.get(ChineseCalendar.MONTH) + 1,
            lunarDay = chineseCalendar.get(ChineseCalendar.DAY_OF_MONTH),
            isLeapMonth = chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        )
    }

    private fun buildYearPillar(adjustedSolarYear: Int): Pillar {
        val stemIndex = Math.floorMod(adjustedSolarYear - 4, STEMS.size)
        val branchIndex = Math.floorMod(adjustedSolarYear - 4, BRANCHES.size)
        return Pillar(STEMS[stemIndex], BRANCHES[branchIndex])
    }

    private fun buildMonthPillar(yearStem: HeavenlyStem, solarMonthIndex: Int): Pillar {
        val firstMonthStemIndex = Math.floorMod(STEMS.indexOf(yearStem) * 2 + 2, STEMS.size)
        val stemIndex = Math.floorMod(firstMonthStemIndex + solarMonthIndex - 1, STEMS.size)
        val branchIndex = Math.floorMod(2 + solarMonthIndex - 1, BRANCHES.size)
        return Pillar(STEMS[stemIndex], BRANCHES[branchIndex])
    }

    private fun buildDayPillar(localDate: LocalDate): Pillar {
        val days = ChronoUnit.DAYS.between(referenceDay, localDate).toInt()
        val sexagenaryIndex = Math.floorMod(referenceDaySexagenaryIndex + days, 60)
        return Pillar(
            heavenlyStem = STEMS[sexagenaryIndex % STEMS.size],
            earthlyBranch = BRANCHES[sexagenaryIndex % BRANCHES.size]
        )
    }

    private fun isBeforeIpchun(date: LocalDate): Boolean = MonthDay.from(date) < MonthDay.of(2, 4)

    private fun resolveSolarMonthIndex(date: LocalDate): Int {
        val monthDay = MonthDay.from(date)
        return when {
            monthDay >= MonthDay.of(2, 4) && monthDay < MonthDay.of(3, 6) -> 1
            monthDay >= MonthDay.of(3, 6) && monthDay < MonthDay.of(4, 5) -> 2
            monthDay >= MonthDay.of(4, 5) && monthDay < MonthDay.of(5, 6) -> 3
            monthDay >= MonthDay.of(5, 6) && monthDay < MonthDay.of(6, 6) -> 4
            monthDay >= MonthDay.of(6, 6) && monthDay < MonthDay.of(7, 7) -> 5
            monthDay >= MonthDay.of(7, 7) && monthDay < MonthDay.of(8, 8) -> 6
            monthDay >= MonthDay.of(8, 8) && monthDay < MonthDay.of(9, 8) -> 7
            monthDay >= MonthDay.of(9, 8) && monthDay < MonthDay.of(10, 8) -> 8
            monthDay >= MonthDay.of(10, 8) && monthDay < MonthDay.of(11, 7) -> 9
            monthDay >= MonthDay.of(11, 7) && monthDay < MonthDay.of(12, 7) -> 10
            monthDay >= MonthDay.of(12, 7) || monthDay < MonthDay.of(1, 6) -> 11
            else -> 12
        }
    }

    private val STEMS = HeavenlyStem.entries.toTypedArray()
    private val BRANCHES = EarthlyBranch.entries.toTypedArray()
}

data class GanzhiResult(
    val year: Pillar,
    val month: Pillar,
    val day: Pillar,
    val lunarYearOfCycle: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean
)

data class Pillar(
    val heavenlyStem: HeavenlyStem,
    val earthlyBranch: EarthlyBranch
)
