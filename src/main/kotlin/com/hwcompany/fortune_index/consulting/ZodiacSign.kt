package com.hwcompany.fortune_index.consulting

import java.time.LocalDate
import java.time.MonthDay

enum class ZodiacSign(
    val koreanName: String,
    val englishName: String,
    val start: MonthDay,
    val end: MonthDay,
    val element: String,
    val moodKeyword: String
) {
    ARIES("양자리", "Aries", MonthDay.of(3, 21), MonthDay.of(4, 19), "불", "돌파력"),
    TAURUS("황소자리", "Taurus", MonthDay.of(4, 20), MonthDay.of(5, 20), "흙", "지속력"),
    GEMINI("쌍둥이자리", "Gemini", MonthDay.of(5, 21), MonthDay.of(6, 21), "바람", "민감한 기민함"),
    CANCER("게자리", "Cancer", MonthDay.of(6, 22), MonthDay.of(7, 22), "물", "보호 본능"),
    LEO("사자자리", "Leo", MonthDay.of(7, 23), MonthDay.of(8, 22), "불", "표현력"),
    VIRGO("처녀자리", "Virgo", MonthDay.of(8, 23), MonthDay.of(9, 23), "흙", "정리 감각"),
    LIBRA("천칭자리", "Libra", MonthDay.of(9, 24), MonthDay.of(10, 22), "바람", "균형감"),
    SCORPIO("전갈자리", "Scorpio", MonthDay.of(10, 23), MonthDay.of(11, 22), "물", "집중력"),
    SAGITTARIUS("사수자리", "Sagittarius", MonthDay.of(11, 23), MonthDay.of(12, 24), "불", "확장성"),
    CAPRICORN("염소자리", "Capricorn", MonthDay.of(12, 25), MonthDay.of(1, 19), "흙", "현실 감각"),
    AQUARIUS("물병자리", "Aquarius", MonthDay.of(1, 20), MonthDay.of(2, 18), "바람", "관점 전환"),
    PISCES("물고기자리", "Pisces", MonthDay.of(2, 19), MonthDay.of(3, 20), "물", "직감");

    companion object {
        fun from(date: LocalDate): ZodiacSign {
            val monthDay = MonthDay.from(date)
            return entries.first { sign ->
                if (sign.start <= sign.end) {
                    monthDay >= sign.start && monthDay <= sign.end
                } else {
                    monthDay >= sign.start || monthDay <= sign.end
                }
            }
        }
    }
}

data class ZodiacConsultingProfile(
    val sign: ZodiacSign,
    val birthDate: LocalDate,
    val headline: String
)

fun ZodiacSign.toHeadline(): String =
    "${koreanName}의 $moodKeyword, $element 기운이 오늘 재물 감각의 바탕이 됩니다."
