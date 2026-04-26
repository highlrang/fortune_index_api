package com.hwcompany.fortune_index.domain.model

import java.time.LocalDate
import java.time.MonthDay

enum class WesternZodiacSign(
    val sign: String,
    val englishName: String,
    val start: MonthDay,
    val end: MonthDay,
    val dateRange: String,
    val element: String,
    val keyword: String,
    val summary: String,
    val traits: List<String>
) {
    ARIES(
        sign = "양자리",
        englishName = "Aries",
        start = MonthDay.of(3, 21),
        end = MonthDay.of(4, 19),
        dateRange = "3월 21일 - 4월 19일",
        element = "불",
        keyword = "돌파력",
        summary = "빠르게 결을 읽고 먼저 움직이려는 힘이 강한 별자리예요.",
        traits = listOf("직진성", "결단력", "생동감")
    ),
    TAURUS(
        sign = "황소자리",
        englishName = "Taurus",
        start = MonthDay.of(4, 20),
        end = MonthDay.of(5, 20),
        dateRange = "4월 20일 - 5월 20일",
        element = "흙",
        keyword = "안정성",
        summary = "급한 변화보다 안정감과 실속을 중시하는 재물 감각이 강해요.",
        traits = listOf("안정감", "인내심", "실리성")
    ),
    GEMINI(
        sign = "쌍둥이자리",
        englishName = "Gemini",
        start = MonthDay.of(5, 21),
        end = MonthDay.of(6, 21),
        dateRange = "5월 21일 - 6월 21일",
        element = "바람",
        keyword = "기민함",
        summary = "정보 흐름을 빠르게 읽고 관점을 전환하는 힘이 돋보이는 별자리예요.",
        traits = listOf("호기심", "순발력", "유연함")
    ),
    CANCER(
        sign = "게자리",
        englishName = "Cancer",
        start = MonthDay.of(6, 22),
        end = MonthDay.of(7, 22),
        dateRange = "6월 22일 - 7월 22일",
        element = "물",
        keyword = "보호본능",
        summary = "재물 흐름에서도 안전지대와 심리적 안정감을 먼저 살피는 경향이 있어요.",
        traits = listOf("공감력", "보호성", "섬세함")
    ),
    LEO(
        sign = "사자자리",
        englishName = "Leo",
        start = MonthDay.of(7, 23),
        end = MonthDay.of(8, 22),
        dateRange = "7월 23일 - 8월 22일",
        element = "불",
        keyword = "표현력",
        summary = "자신감 있게 흐름을 읽고 존재감을 드러내는 힘이 큰 별자리예요.",
        traits = listOf("자신감", "리더십", "따뜻함")
    ),
    VIRGO(
        sign = "처녀자리",
        englishName = "Virgo",
        start = MonthDay.of(8, 23),
        end = MonthDay.of(9, 23),
        dateRange = "8월 23일 - 9월 23일",
        element = "흙",
        keyword = "정리력",
        summary = "작은 신호를 세밀하게 읽고 정돈된 판단을 선호하는 별자리예요.",
        traits = listOf("분석력", "성실함", "정돈감")
    ),
    LIBRA(
        sign = "천칭자리",
        englishName = "Libra",
        start = MonthDay.of(9, 24),
        end = MonthDay.of(10, 22),
        dateRange = "9월 24일 - 10월 22일",
        element = "바람",
        keyword = "균형감",
        summary = "한쪽으로 치우치지 않고 흐름의 균형점을 찾으려는 힘이 강해요.",
        traits = listOf("균형감", "관계감각", "세련됨")
    ),
    SCORPIO(
        sign = "전갈자리",
        englishName = "Scorpio",
        start = MonthDay.of(10, 23),
        end = MonthDay.of(11, 22),
        dateRange = "10월 23일 - 11월 22일",
        element = "물",
        keyword = "집중력",
        summary = "겉보다 속을 읽고 깊게 파고드는 재물 감각이 강한 별자리예요.",
        traits = listOf("집중력", "통찰력", "몰입감")
    ),
    SAGITTARIUS(
        sign = "사수자리",
        englishName = "Sagittarius",
        start = MonthDay.of(11, 23),
        end = MonthDay.of(12, 24),
        dateRange = "11월 23일 - 12월 24일",
        element = "불",
        keyword = "확장성",
        summary = "큰 흐름과 가능성을 읽으며 시야를 넓히는 쪽에 강점이 있어요.",
        traits = listOf("낙관성", "확장성", "직관력")
    ),
    CAPRICORN(
        sign = "염소자리",
        englishName = "Capricorn",
        start = MonthDay.of(12, 25),
        end = MonthDay.of(1, 19),
        dateRange = "12월 25일 - 1월 19일",
        element = "흙",
        keyword = "현실감각",
        summary = "감정보다 구조와 지속 가능성을 먼저 보는 현실적인 별자리예요.",
        traits = listOf("책임감", "현실감", "지구력")
    ),
    AQUARIUS(
        sign = "물병자리",
        englishName = "Aquarius",
        start = MonthDay.of(1, 20),
        end = MonthDay.of(2, 18),
        dateRange = "1월 20일 - 2월 18일",
        element = "바람",
        keyword = "관점전환",
        summary = "기존 방식에 얽매이지 않고 다른 해석을 찾는 힘이 큰 별자리예요.",
        traits = listOf("독창성", "객관성", "유연함")
    ),
    PISCES(
        sign = "물고기자리",
        englishName = "Pisces",
        start = MonthDay.of(2, 19),
        end = MonthDay.of(3, 20),
        dateRange = "2월 19일 - 3월 20일",
        element = "물",
        keyword = "직감",
        summary = "미세한 분위기와 감정의 물결을 잘 읽는 감수성이 강한 별자리예요.",
        traits = listOf("직감", "감수성", "공감력")
    );

    companion object {
        fun from(date: LocalDate): WesternZodiacSign {
            val monthDay = MonthDay.from(date)
            return entries.first { sign ->
                if (sign.start <= sign.end) {
                    monthDay >= sign.start && monthDay <= sign.end
                } else {
                    monthDay >= sign.start || monthDay <= sign.end
                }
            }
        }

        fun fromLongitude(longitude: Double): WesternZodiacSign {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            val index = (normalized / 30.0).toInt()
            return entries[index]
        }
    }

    fun degreeInSign(longitude: Double): Double {
        val normalized = ((longitude % 360.0) + 360.0) % 360.0
        return normalized % 30.0
    }
}
