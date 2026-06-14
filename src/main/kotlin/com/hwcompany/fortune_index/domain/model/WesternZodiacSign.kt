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
    val traits: List<String>,
    val traitDescriptions: List<String>
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
        traits = listOf("직진성", "결단력", "생동감"),
        traitDescriptions = listOf(
            "목표가 보이면 망설임 없이 먼저 움직이는 추진 성향",
            "순간의 흐름을 빠르게 읽고 명확하게 선택하는 힘",
            "활력 있게 에너지를 전달하며 분위기를 이끄는 기질"
        )
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
        traits = listOf("안정감", "인내심", "실리성"),
        traitDescriptions = listOf(
            "급한 변화보다 검증된 흐름을 선호하는 침착한 기질",
            "결과가 느려도 기준을 유지하며 오래 버티는 힘",
            "감각보다 실제 이익과 가치를 먼저 따지는 성향"
        )
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
        traits = listOf("호기심", "순발력", "유연함"),
        traitDescriptions = listOf(
            "새로운 정보와 관점을 빠르게 흡수하는 탐색 성향",
            "변화하는 상황에 빠르게 반응하고 전환하는 능력",
            "고정된 틀보다 맥락에 맞게 방식을 바꾸는 적응력"
        )
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
        traits = listOf("공감력", "보호성", "섬세함"),
        traitDescriptions = listOf(
            "상황의 감정 온도를 민감하게 감지하는 수용 능력",
            "소중한 것을 지키려는 방어적 판단과 안전 감각",
            "작은 변화도 놓치지 않고 세밀하게 살피는 주의력"
        )
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
        traits = listOf("자신감", "리더십", "따뜻함"),
        traitDescriptions = listOf(
            "자신의 판단을 믿고 흐름을 주도하는 존재감",
            "방향을 제시하고 주변을 자연스럽게 이끄는 힘",
            "에너지를 나누며 주변을 활기차게 만드는 기질"
        )
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
        traits = listOf("분석력", "성실함", "정돈감"),
        traitDescriptions = listOf(
            "조건과 수치를 꼼꼼히 살피며 문제를 파악하는 능력",
            "기준을 지키며 반복적으로 실행하는 꾸준한 태도",
            "흐트러진 정보를 체계적으로 정리하려는 성향"
        )
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
        traits = listOf("균형감", "관계감각", "세련됨"),
        traitDescriptions = listOf(
            "어느 한쪽으로 치우치지 않고 중심점을 찾는 조율 능력",
            "상대의 입장과 분위기를 읽으며 흐름을 맞추는 힘",
            "선택과 표현에서 과하거나 부족함 없이 균형을 잡는 감각"
        )
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
        traits = listOf("집중력", "통찰력", "몰입감"),
        traitDescriptions = listOf(
            "하나의 대상에 에너지를 모아 깊게 파고드는 힘",
            "겉에 보이지 않는 속의 구조와 흐름을 읽는 능력",
            "선택한 방향에 완전히 빠져들어 끝까지 가는 성향"
        )
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
        traits = listOf("낙관성", "확장성", "직관력"),
        traitDescriptions = listOf(
            "가능성을 넓게 보고 앞으로 나아가려는 기대 성향",
            "하나의 관심이 더 넓은 시야와 기회로 연결되는 힘",
            "데이터보다 큰 흐름의 방향을 먼저 감지하는 감각"
        )
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
        traits = listOf("책임감", "현실감", "지구력"),
        traitDescriptions = listOf(
            "맡은 역할과 목표를 끝까지 이행하려는 의지",
            "이상보다 실제 조건과 가능성을 기준으로 판단하는 성향",
            "결과가 늦어도 방향을 유지하며 오래 버티는 힘"
        )
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
        traits = listOf("독창성", "객관성", "유연함"),
        traitDescriptions = listOf(
            "기존 방식에 얽매이지 않고 다른 관점을 찾는 힘",
            "감정보다 사실과 구조를 기준으로 보는 냉정한 시각",
            "새로운 가능성을 열린 자세로 받아들이는 적응력"
        )
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
        traits = listOf("직감", "감수성", "공감력"),
        traitDescriptions = listOf(
            "논리 이전에 분위기와 흐름을 먼저 느끼는 감지 능력",
            "미세한 변화와 감정의 파동을 섬세하게 받아들이는 성질",
            "상대의 마음 상태를 자연스럽게 이해하고 흡수하는 능력"
        )
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
