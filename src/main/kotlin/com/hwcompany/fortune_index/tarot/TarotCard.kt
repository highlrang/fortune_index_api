package com.hwcompany.fortune_index.tarot

enum class TarotArcanaType {
    MAJOR,
    MINOR
}

enum class TarotSuit {
    WANDS,
    CUPS,
    SWORDS,
    PENTACLES
}

enum class TarotCard(
    val displayName: String,
    val arcanaType: TarotArcanaType,
    val uprightMeaning: String,
    val imageUrl: String,
    val suit: TarotSuit? = null
) {
    THE_FOOL("The Fool", TarotArcanaType.MAJOR, "새로운 시작, 자유로운 도전, 순수한 가능성", image("major/the-fool")),
    THE_MAGICIAN("The Magician", TarotArcanaType.MAJOR, "의지, 실행력, 자원 활용, 현실화", image("major/the-magician")),
    THE_HIGH_PRIESTESS("The High Priestess", TarotArcanaType.MAJOR, "직관, 내면의 지혜, 숨은 정보", image("major/the-high-priestess")),
    THE_EMPRESS("The Empress", TarotArcanaType.MAJOR, "풍요, 돌봄, 성장, 결실", image("major/the-empress")),
    THE_EMPEROR("The Emperor", TarotArcanaType.MAJOR, "질서, 통제, 책임, 리더십", image("major/the-emperor")),
    THE_HIEROPHANT("The Hierophant", TarotArcanaType.MAJOR, "전통, 제도, 배움, 조언", image("major/the-hierophant")),
    THE_LOVERS("The Lovers", TarotArcanaType.MAJOR, "관계, 선택, 조화, 가치 일치", image("major/the-lovers")),
    THE_CHARIOT("The Chariot", TarotArcanaType.MAJOR, "전진, 승리, 집중력, 추진력", image("major/the-chariot")),
    STRENGTH("Strength", TarotArcanaType.MAJOR, "인내, 내면의 힘, 침착한 통제", image("major/strength")),
    THE_HERMIT("The Hermit", TarotArcanaType.MAJOR, "성찰, 탐구, 신중한 판단", image("major/the-hermit")),
    WHEEL_OF_FORTUNE("Wheel of Fortune", TarotArcanaType.MAJOR, "전환점, 흐름의 변화, 기회", image("major/wheel-of-fortune")),
    JUSTICE("Justice", TarotArcanaType.MAJOR, "균형, 공정함, 책임 있는 결정", image("major/justice")),
    THE_HANGED_MAN("The Hanged Man", TarotArcanaType.MAJOR, "관점 전환, 유예, 내려놓음", image("major/the-hanged-man")),
    DEATH("Death", TarotArcanaType.MAJOR, "종결, 정리, 본질적 변화", image("major/death")),
    TEMPERANCE("Temperance", TarotArcanaType.MAJOR, "조화, 절제, 안정적 조율", image("major/temperance")),
    THE_DEVIL("The Devil", TarotArcanaType.MAJOR, "집착, 유혹, 얽매임에 대한 경고", image("major/the-devil")),
    THE_TOWER("The Tower", TarotArcanaType.MAJOR, "급격한 변화, 붕괴, 충격 후 재구성", image("major/the-tower")),
    THE_STAR("The Star", TarotArcanaType.MAJOR, "희망, 회복, 영감", image("major/the-star")),
    THE_MOON("The Moon", TarotArcanaType.MAJOR, "불확실성, 감정의 파동, 숨은 리스크", image("major/the-moon")),
    THE_SUN("The Sun", TarotArcanaType.MAJOR, "성공, 명확성, 활력, 긍정", image("major/the-sun")),
    JUDGEMENT("Judgement", TarotArcanaType.MAJOR, "각성, 결산, 재도약", image("major/judgement")),
    THE_WORLD("The World", TarotArcanaType.MAJOR, "완성, 성취, 마무리", image("major/the-world")),
    ACE_OF_WANDS("Ace of Wands", TarotArcanaType.MINOR, "새로운 열정, 창의적 시작, 강한 동력", image("minor/wands/ace"), TarotSuit.WANDS),
    TWO_OF_WANDS("Two of Wands", TarotArcanaType.MINOR, "계획 수립, 확장 가능성, 방향 설정", image("minor/wands/two"), TarotSuit.WANDS),
    THREE_OF_WANDS("Three of Wands", TarotArcanaType.MINOR, "기대, 시장 확장, 외부 기회", image("minor/wands/three"), TarotSuit.WANDS),
    FOUR_OF_WANDS("Four of Wands", TarotArcanaType.MINOR, "안정, 축하, 기반 확보", image("minor/wands/four"), TarotSuit.WANDS),
    FIVE_OF_WANDS("Five of Wands", TarotArcanaType.MINOR, "경쟁, 충돌, 의견 대립", image("minor/wands/five"), TarotSuit.WANDS),
    SIX_OF_WANDS("Six of Wands", TarotArcanaType.MINOR, "승리, 인정, 긍정적 결과", image("minor/wands/six"), TarotSuit.WANDS),
    SEVEN_OF_WANDS("Seven of Wands", TarotArcanaType.MINOR, "방어, 입지 수성, 버티기", image("minor/wands/seven"), TarotSuit.WANDS),
    EIGHT_OF_WANDS("Eight of Wands", TarotArcanaType.MINOR, "속도, 급진전, 빠른 소식", image("minor/wands/eight"), TarotSuit.WANDS),
    NINE_OF_WANDS("Nine of Wands", TarotArcanaType.MINOR, "경계, 피로 속 지속, 신중함", image("minor/wands/nine"), TarotSuit.WANDS),
    TEN_OF_WANDS("Ten of Wands", TarotArcanaType.MINOR, "과부하, 책임 증가, 부담", image("minor/wands/ten"), TarotSuit.WANDS),
    PAGE_OF_WANDS("Page of Wands", TarotArcanaType.MINOR, "호기심, 탐색, 가능성 탐험", image("minor/wands/page"), TarotSuit.WANDS),
    KNIGHT_OF_WANDS("Knight of Wands", TarotArcanaType.MINOR, "대담한 행동, 돌진, 변동성", image("minor/wands/knight"), TarotSuit.WANDS),
    QUEEN_OF_WANDS("Queen of Wands", TarotArcanaType.MINOR, "자신감, 매력, 주도성", image("minor/wands/queen"), TarotSuit.WANDS),
    KING_OF_WANDS("King of Wands", TarotArcanaType.MINOR, "비전, 리더십, 큰 그림", image("minor/wands/king"), TarotSuit.WANDS),
    ACE_OF_CUPS("Ace of Cups", TarotArcanaType.MINOR, "감정의 시작, 공감, 열린 관계", image("minor/cups/ace"), TarotSuit.CUPS),
    TWO_OF_CUPS("Two of Cups", TarotArcanaType.MINOR, "상호 신뢰, 파트너십, 조화", image("minor/cups/two"), TarotSuit.CUPS),
    THREE_OF_CUPS("Three of Cups", TarotArcanaType.MINOR, "협력, 교류, 기쁨의 공유", image("minor/cups/three"), TarotSuit.CUPS),
    FOUR_OF_CUPS("Four of Cups", TarotArcanaType.MINOR, "권태, 무관심, 놓치는 기회", image("minor/cups/four"), TarotSuit.CUPS),
    FIVE_OF_CUPS("Five of Cups", TarotArcanaType.MINOR, "실망, 상실감, 감정적 회복 필요", image("minor/cups/five"), TarotSuit.CUPS),
    SIX_OF_CUPS("Six of Cups", TarotArcanaType.MINOR, "추억, 익숙함, 과거의 연결", image("minor/cups/six"), TarotSuit.CUPS),
    SEVEN_OF_CUPS("Seven of Cups", TarotArcanaType.MINOR, "선택지 과다, 환상, 분산된 집중", image("minor/cups/seven"), TarotSuit.CUPS),
    EIGHT_OF_CUPS("Eight of Cups", TarotArcanaType.MINOR, "거리두기, 이탈, 새로운 탐색", image("minor/cups/eight"), TarotSuit.CUPS),
    NINE_OF_CUPS("Nine of Cups", TarotArcanaType.MINOR, "만족, 소원 성취, 정서적 풍요", image("minor/cups/nine"), TarotSuit.CUPS),
    TEN_OF_CUPS("Ten of Cups", TarotArcanaType.MINOR, "화목, 장기적 안정, 만족감", image("minor/cups/ten"), TarotSuit.CUPS),
    PAGE_OF_CUPS("Page of Cups", TarotArcanaType.MINOR, "감성적 제안, 직관적 메시지", image("minor/cups/page"), TarotSuit.CUPS),
    KNIGHT_OF_CUPS("Knight of Cups", TarotArcanaType.MINOR, "이상 추구, 제안, 감정적 추진", image("minor/cups/knight"), TarotSuit.CUPS),
    QUEEN_OF_CUPS("Queen of Cups", TarotArcanaType.MINOR, "공감, 섬세함, 깊은 직관", image("minor/cups/queen"), TarotSuit.CUPS),
    KING_OF_CUPS("King of Cups", TarotArcanaType.MINOR, "감정 통제, 성숙함, 균형 감각", image("minor/cups/king"), TarotSuit.CUPS),
    ACE_OF_SWORDS("Ace of Swords", TarotArcanaType.MINOR, "명확한 판단, 통찰, 결단", image("minor/swords/ace"), TarotSuit.SWORDS),
    TWO_OF_SWORDS("Two of Swords", TarotArcanaType.MINOR, "보류, 갈등, 판단 유예", image("minor/swords/two"), TarotSuit.SWORDS),
    THREE_OF_SWORDS("Three of Swords", TarotArcanaType.MINOR, "상처, 충격, 냉정한 진실", image("minor/swords/three"), TarotSuit.SWORDS),
    FOUR_OF_SWORDS("Four of Swords", TarotArcanaType.MINOR, "휴식, 회복, 재정비", image("minor/swords/four"), TarotSuit.SWORDS),
    FIVE_OF_SWORDS("Five of Swords", TarotArcanaType.MINOR, "소모적 갈등, 승패 집착", image("minor/swords/five"), TarotSuit.SWORDS),
    SIX_OF_SWORDS("Six of Swords", TarotArcanaType.MINOR, "이동, 전환, 점진적 회복", image("minor/swords/six"), TarotSuit.SWORDS),
    SEVEN_OF_SWORDS("Seven of Swords", TarotArcanaType.MINOR, "은밀함, 전략, 정보 비대칭", image("minor/swords/seven"), TarotSuit.SWORDS),
    EIGHT_OF_SWORDS("Eight of Swords", TarotArcanaType.MINOR, "제약, 심리적 압박, 시야 제한", image("minor/swords/eight"), TarotSuit.SWORDS),
    NINE_OF_SWORDS("Nine of Swords", TarotArcanaType.MINOR, "불안, 걱정, 과도한 염려", image("minor/swords/nine"), TarotSuit.SWORDS),
    TEN_OF_SWORDS("Ten of Swords", TarotArcanaType.MINOR, "끝맺음, 바닥 확인, 재시작 전 단계", image("minor/swords/ten"), TarotSuit.SWORDS),
    PAGE_OF_SWORDS("Page of Swords", TarotArcanaType.MINOR, "탐색, 질문, 예민한 관찰", image("minor/swords/page"), TarotSuit.SWORDS),
    KNIGHT_OF_SWORDS("Knight of Swords", TarotArcanaType.MINOR, "빠른 판단, 돌파, 공격적 실행", image("minor/swords/knight"), TarotSuit.SWORDS),
    QUEEN_OF_SWORDS("Queen of Swords", TarotArcanaType.MINOR, "분석력, 냉정함, 기준의 명확성", image("minor/swords/queen"), TarotSuit.SWORDS),
    KING_OF_SWORDS("King of Swords", TarotArcanaType.MINOR, "전략, 논리, 객관적 통솔", image("minor/swords/king"), TarotSuit.SWORDS),
    ACE_OF_PENTACLES("Ace of Pentacles", TarotArcanaType.MINOR, "현실적 기회, 자산의 시작, 수익 기반", image("minor/pentacles/ace"), TarotSuit.PENTACLES),
    TWO_OF_PENTACLES("Two of Pentacles", TarotArcanaType.MINOR, "균형, 자금 운용, 유연한 대응", image("minor/pentacles/two"), TarotSuit.PENTACLES),
    THREE_OF_PENTACLES("Three of Pentacles", TarotArcanaType.MINOR, "협업, 숙련, 실무 성과", image("minor/pentacles/three"), TarotSuit.PENTACLES),
    FOUR_OF_PENTACLES("Four of Pentacles", TarotArcanaType.MINOR, "보수성, 축적, 방어적 관리", image("minor/pentacles/four"), TarotSuit.PENTACLES),
    FIVE_OF_PENTACLES("Five of Pentacles", TarotArcanaType.MINOR, "재정 압박, 결핍, 지원 필요", image("minor/pentacles/five"), TarotSuit.PENTACLES),
    SIX_OF_PENTACLES("Six of Pentacles", TarotArcanaType.MINOR, "지원, 균형 있는 분배, 실질 도움", image("minor/pentacles/six"), TarotSuit.PENTACLES),
    SEVEN_OF_PENTACLES("Seven of Pentacles", TarotArcanaType.MINOR, "기다림, 점검, 장기 수확 준비", image("minor/pentacles/seven"), TarotSuit.PENTACLES),
    EIGHT_OF_PENTACLES("Eight of Pentacles", TarotArcanaType.MINOR, "반복 훈련, 성실함, 기술 축적", image("minor/pentacles/eight"), TarotSuit.PENTACLES),
    NINE_OF_PENTACLES("Nine of Pentacles", TarotArcanaType.MINOR, "자립, 안정, 축적된 성과", image("minor/pentacles/nine"), TarotSuit.PENTACLES),
    TEN_OF_PENTACLES("Ten of Pentacles", TarotArcanaType.MINOR, "장기 자산, 유산, 견고한 기반", image("minor/pentacles/ten"), TarotSuit.PENTACLES),
    PAGE_OF_PENTACLES("Page of Pentacles", TarotArcanaType.MINOR, "현실적 학습, 씨앗 투자, 성실한 출발", image("minor/pentacles/page"), TarotSuit.PENTACLES),
    KNIGHT_OF_PENTACLES("Knight of Pentacles", TarotArcanaType.MINOR, "꾸준함, 느리지만 안정적 전진", image("minor/pentacles/knight"), TarotSuit.PENTACLES),
    QUEEN_OF_PENTACLES("Queen of Pentacles", TarotArcanaType.MINOR, "실속, 관리 능력, 안정적 운영", image("minor/pentacles/queen"), TarotSuit.PENTACLES),
    KING_OF_PENTACLES("King of Pentacles", TarotArcanaType.MINOR, "물질적 성취, 재정 통제, 신뢰성", image("minor/pentacles/king"), TarotSuit.PENTACLES);

    val code: String
        get() = name

    val key: String
        get() = "tarot.${cardNumber.toString().padStart(2, '0')}"

    val cardNumber: Int
        get() = ordinal

    companion object {
        private const val IMAGE_BASE_URL = "https://fortune-index-assets.s3.ap-northeast-2.amazonaws.com/tarot"

        fun fromIndex(index: Int): TarotCard = entries[index]

        fun fromCode(code: String): TarotCard =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown tarot card code: $code")

        fun deck(): List<TarotCard> = entries.toList()

        private fun image(path: String): String = "$IMAGE_BASE_URL/$path.jpg"
    }
}
