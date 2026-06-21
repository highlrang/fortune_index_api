package com.hwcompany.fortune_index.tarot

private const val IMAGE_BASE_URL = "https://fortune-index-assets.s3.ap-northeast-2.amazonaws.com/tarot"

private fun image(path: String): String = "$IMAGE_BASE_URL/$path.jpg"

enum class TarotDeckType {
    TAROT,
    ORACLE
}

enum class TarotDeckRole {
    MAIN,
    ASSISTANT
}

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
    val description: String,
    val imageUrl: String,
    val suit: TarotSuit? = null
) {
    THE_FOOL("The Fool", TarotArcanaType.MAJOR, "새로운 시작, 자유로운 도전, 순수한 가능성", "틀에 갇히지 않고 첫걸음을 내딛는 카드입니다. 완전히 준비되지 않았더라도 경험을 통해 배우며 길을 만들어 가는 흐름을 보여줍니다.", image("major/the-fool")),
    THE_MAGICIAN("The Magician", TarotArcanaType.MAJOR, "의지, 실행력, 자원 활용, 현실화", "손에 쥔 자원과 기술을 실제 결과로 바꾸는 힘을 뜻합니다. 생각에만 머무르지 않고 가진 능력을 조합해 바로 실행할 때 성과가 납니다.", image("major/the-magician")),
    THE_HIGH_PRIESTESS("The High Priestess", TarotArcanaType.MAJOR, "직관, 내면의 지혜, 숨은 정보", "겉으로 드러난 정보보다 아직 보이지 않는 흐름을 읽어야 하는 카드입니다. 조용히 관찰하고 서두르지 않을수록 본질이 더 선명해집니다.", image("major/the-high-priestess")),
    THE_EMPRESS("The Empress", TarotArcanaType.MAJOR, "풍요, 돌봄, 성장, 결실", "무언가를 키우고 안정적으로 자라게 하는 에너지를 상징합니다. 관계든 일이든 꾸준한 보살핌이 결국 풍성한 결과로 이어집니다.", image("major/the-empress")),
    THE_EMPEROR("The Emperor", TarotArcanaType.MAJOR, "질서, 통제, 책임, 리더십", "원칙과 구조를 세워 혼란을 정리하는 카드입니다. 감정보다 기준과 책임을 앞세울 때 상황을 안정적으로 이끌 수 있습니다.", image("major/the-emperor")),
    THE_HIEROPHANT("The Hierophant", TarotArcanaType.MAJOR, "전통, 제도, 배움, 조언", "검증된 방식과 공적인 기준 안에서 답을 찾으라는 의미가 큽니다. 스승이나 조직, 제도권의 조언을 따를수록 실수가 줄어듭니다.", image("major/the-hierophant")),
    THE_LOVERS("The Lovers", TarotArcanaType.MAJOR, "관계, 선택, 조화, 가치 일치", "단순한 감정 문제보다 나와 맞는 가치와 방향을 고르는 카드에 가깝습니다. 진짜 중요한 기준이 무엇인지 분명히 해야 선택이 흔들리지 않습니다.", image("major/the-lovers")),
    THE_CHARIOT("The Chariot", TarotArcanaType.MAJOR, "전진, 승리, 집중력, 추진력", "흩어진 힘을 한 방향으로 모아 밀고 나갈 때 승산이 커진다는 뜻입니다. 흔들리는 감정보다 목표 의식과 통제력이 핵심이 됩니다.", image("major/the-chariot")),
    STRENGTH("Strength", TarotArcanaType.MAJOR, "인내, 내면의 힘, 침착한 통제", "힘으로 누르기보다 부드럽게 다루는 강인함을 말합니다. 감정이나 충동을 억압하기보다 성숙하게 길들이는 태도가 중요합니다.", image("major/strength")),
    THE_HERMIT("The Hermit", TarotArcanaType.MAJOR, "성찰, 탐구, 신중한 판단", "바깥의 소음에서 한 발 떨어져 자신의 기준을 다시 세우는 카드입니다. 속도를 늦추고 깊이 생각할수록 더 정확한 결론에 닿습니다.", image("major/the-hermit")),
    WHEEL_OF_FORTUNE("Wheel of Fortune", TarotArcanaType.MAJOR, "전환점, 흐름의 변화, 기회", "내 의지만으로는 통제하기 어려운 큰 흐름의 변화를 보여줍니다. 운이 움직일 때는 타이밍을 읽고 유연하게 올라타는 것이 중요합니다.", image("major/wheel-of-fortune")),
    JUSTICE("Justice", TarotArcanaType.MAJOR, "균형, 공정함, 책임 있는 결정", "감정적 편향보다 사실과 균형으로 판단해야 할 때 나오는 카드입니다. 지금의 선택은 나중에 그대로 되돌아오므로 책임감 있는 결정이 필요합니다.", image("major/justice")),
    THE_HANGED_MAN("The Hanged Man", TarotArcanaType.MAJOR, "관점 전환, 유예, 내려놓음", "억지로 밀어붙이기보다 잠시 멈추며 시선을 바꿔야 한다는 뜻입니다. 손해처럼 보여도 관점을 바꾸는 시간이 결국 다음 단계의 열쇠가 됩니다.", image("major/the-hanged-man")),
    DEATH("Death", TarotArcanaType.MAJOR, "종결, 정리, 본질적 변화", "무서운 결말이라기보다 더는 유지할 수 없는 것을 끝내고 새 흐름으로 넘어가는 카드입니다. 미련을 내려놓을수록 변화는 더 건강하게 진행됩니다.", image("major/death")),
    TEMPERANCE("Temperance", TarotArcanaType.MAJOR, "조화, 절제, 안정적 조율", "서로 다른 성질을 섞어 균형을 만드는 조율의 카드입니다. 급하게 결론 내리기보다 속도를 맞추고 중간 지점을 찾는 태도가 효과적입니다.", image("major/temperance")),
    THE_DEVIL("The Devil", TarotArcanaType.MAJOR, "집착, 유혹, 얽매임에 대한 경고", "욕망이나 익숙한 패턴에 스스로 묶여 있는 상태를 보여줍니다. 문제의 핵심은 외부보다 내가 놓지 못하는 집착이 무엇인지 보는 데 있습니다.", image("major/the-devil")),
    THE_TOWER("The Tower", TarotArcanaType.MAJOR, "급격한 변화, 붕괴, 충격 후 재구성", "기반이 약한 구조가 갑작스럽게 무너지는 카드입니다. 충격은 크지만 거짓 안정이 깨진 뒤에야 더 단단한 토대를 다시 세울 수 있습니다.", image("major/the-tower")),
    THE_STAR("The Star", TarotArcanaType.MAJOR, "희망, 회복, 영감", "지친 뒤에 다시 숨을 고르고 앞을 바라보게 하는 카드입니다. 조급함을 내려놓고 자신의 가능성을 믿을 때 회복과 영감이 함께 옵니다.", image("major/the-star")),
    THE_MOON("The Moon", TarotArcanaType.MAJOR, "불확실성, 감정의 파동, 숨은 리스크", "확실하지 않은 정보와 흔들리는 감정 속에서 방향을 잃기 쉬운 시기를 뜻합니다. 불안이 사실을 과장할 수 있으니 확인과 검증이 필요합니다.", image("major/the-moon")),
    THE_SUN("The Sun", TarotArcanaType.MAJOR, "성공, 명확성, 활력, 긍정", "어둡던 흐름이 걷히고 결과와 의도가 분명하게 드러나는 카드입니다. 자신감과 활력이 높아지며 주변과의 관계도 밝게 풀리기 쉽습니다.", image("major/the-sun")),
    JUDGEMENT("Judgement", TarotArcanaType.MAJOR, "각성, 결산, 재도약", "과거의 선택을 돌아보고 더 높은 단계로 다시 일어서는 카드입니다. 미뤄 둔 문제를 직면할수록 새로운 출발의 문도 함께 열립니다.", image("major/judgement")),
    THE_WORLD("The World", TarotArcanaType.MAJOR, "완성, 성취, 마무리", "하나의 사이클이 완성되고 노력의 결실이 정리되는 카드입니다. 끝맺음이 분명할수록 다음 단계로도 더 자연스럽게 넘어갈 수 있습니다.", image("major/the-world")),
    ACE_OF_WANDS("Ace of Wands", TarotArcanaType.MINOR, "새로운 열정, 창의적 시작, 강한 동력", "가슴을 뛰게 하는 아이디어와 시작의 불씨가 붙는 카드입니다. 지금은 완성도보다 에너지를 믿고 시동을 거는 것이 중요합니다.", image("minor/wands/ace"), TarotSuit.WANDS),
    TWO_OF_WANDS("Two of Wands", TarotArcanaType.MINOR, "계획 수립, 확장 가능성, 방향 설정", "이미 출발선은 넘었고 이제 더 넓은 방향을 설계할 시점이라는 뜻입니다. 가능성은 많지만 어느 길에 힘을 실을지 결정해야 합니다.", image("minor/wands/two"), TarotSuit.WANDS),
    THREE_OF_WANDS("Three of Wands", TarotArcanaType.MINOR, "기대, 시장 확장, 외부 기회", "내가 던진 시도가 바깥에서 어떤 반응을 가져오는지 지켜보는 카드입니다. 시야를 넓히고 외부 기회를 받아들일수록 성장 폭이 커집니다.", image("minor/wands/three"), TarotSuit.WANDS),
    FOUR_OF_WANDS("Four of Wands", TarotArcanaType.MINOR, "안정, 축하, 기반 확보", "함께 기뻐할 수 있는 성과와 안정된 기반을 의미합니다. 큰 도약 전이라도 지금의 성취를 인정하고 기반을 다지는 것이 중요합니다.", image("minor/wands/four"), TarotSuit.WANDS),
    FIVE_OF_WANDS("Five of Wands", TarotArcanaType.MINOR, "경쟁, 충돌, 의견 대립", "서로의 힘이 부딪히며 방향이 쉽게 정리되지 않는 상황을 보여줍니다. 갈등 자체보다 그 속에서 기준을 세우는 과정이 핵심입니다.", image("minor/wands/five"), TarotSuit.WANDS),
    SIX_OF_WANDS("Six of Wands", TarotArcanaType.MINOR, "승리, 인정, 긍정적 결과", "노력이 외부에서 인정받고 자신감이 올라가는 흐름입니다. 다만 박수에 취하기보다 다음 단계까지 이어 갈 준비가 필요합니다.", image("minor/wands/six"), TarotSuit.WANDS),
    SEVEN_OF_WANDS("Seven of Wands", TarotArcanaType.MINOR, "방어, 입지 수성, 버티기", "어렵게 얻은 자리를 지키기 위해 분명한 태도가 필요한 카드입니다. 모두를 만족시키기보다 내 기준을 선명히 하는 편이 낫습니다.", image("minor/wands/seven"), TarotSuit.WANDS),
    EIGHT_OF_WANDS("Eight of Wands", TarotArcanaType.MINOR, "속도, 급진전, 빠른 소식", "정체됐던 일이 갑자기 빠르게 움직이기 시작하는 카드입니다. 타이밍이 빠르므로 준비가 끝났다면 망설이지 말고 반응해야 합니다.", image("minor/wands/eight"), TarotSuit.WANDS),
    NINE_OF_WANDS("Nine of Wands", TarotArcanaType.MINOR, "경계, 피로 속 지속, 신중함", "쉽게 무너지지 않는 버팀목을 뜻하지만 피로도 함께 큰 상태입니다. 끝까지 가되 같은 실수를 반복하지 않도록 경계를 유지해야 합니다.", image("minor/wands/nine"), TarotSuit.WANDS),
    TEN_OF_WANDS("Ten of Wands", TarotArcanaType.MINOR, "과부하, 책임 증가, 부담", "책임과 짐이 한꺼번에 몰려오는 카드입니다. 다 떠안는 태도보다 우선순위를 조정하고 일부를 덜어 내는 결단이 필요합니다.", image("minor/wands/ten"), TarotSuit.WANDS),
    PAGE_OF_WANDS("Page of Wands", TarotArcanaType.MINOR, "호기심, 탐색, 가능성 탐험", "새로운 자극을 향한 호기심과 가벼운 모험심이 강한 카드입니다. 아직 미완성이어도 즐겁게 탐색하는 태도가 다음 기회를 엽니다.", image("minor/wands/page"), TarotSuit.WANDS),
    KNIGHT_OF_WANDS("Knight of Wands", TarotArcanaType.MINOR, "대담한 행동, 돌진, 변동성", "확신이 서면 빠르게 뛰어드는 추진력을 의미합니다. 에너지는 강하지만 속도 조절을 놓치면 지속성이 약해질 수 있습니다.", image("minor/wands/knight"), TarotSuit.WANDS),
    QUEEN_OF_WANDS("Queen of Wands", TarotArcanaType.MINOR, "자신감, 매력, 주도성", "자기 확신과 밝은 존재감으로 주변을 끌어당기는 카드입니다. 움츠리기보다 내 장점을 드러낼수록 영향력이 커집니다.", image("minor/wands/queen"), TarotSuit.WANDS),
    KING_OF_WANDS("King of Wands", TarotArcanaType.MINOR, "비전, 리더십, 큰 그림", "당장의 성과보다 더 큰 방향과 판을 읽는 리더의 카드입니다. 실행력에 비전을 더하면 사람과 자원이 자연스럽게 따라옵니다.", image("minor/wands/king"), TarotSuit.WANDS),
    ACE_OF_CUPS("Ace of Cups", TarotArcanaType.MINOR, "감정의 시작, 공감, 열린 관계", "마음이 새롭게 열리고 감정의 흐름이 살아나는 카드입니다. 관계와 감정의 출발선에서는 계산보다 진심이 더 큰 힘을 냅니다.", image("minor/cups/ace"), TarotSuit.CUPS),
    TWO_OF_CUPS("Two of Cups", TarotArcanaType.MINOR, "상호 신뢰, 파트너십, 조화", "서로가 같은 감정을 주고받으며 균형 있게 연결되는 흐름입니다. 관계에서는 일방향보다 상호성의 회복이 중요합니다.", image("minor/cups/two"), TarotSuit.CUPS),
    THREE_OF_CUPS("Three of Cups", TarotArcanaType.MINOR, "협력, 교류, 기쁨의 공유", "혼자보다 함께할 때 기쁨이 커지는 카드입니다. 친밀한 교류와 협업 속에서 좋은 소식이나 축하할 일이 생기기 쉽습니다.", image("minor/cups/three"), TarotSuit.CUPS),
    FOUR_OF_CUPS("Four of Cups", TarotArcanaType.MINOR, "권태, 무관심, 놓치는 기회", "마음이 무뎌져 눈앞의 가능성을 제대로 보지 못하는 상태를 뜻합니다. 지금은 외부보다 내 감정이 왜 닫혀 있는지 살피는 것이 먼저입니다.", image("minor/cups/four"), TarotSuit.CUPS),
    FIVE_OF_CUPS("Five of Cups", TarotArcanaType.MINOR, "실망, 상실감, 감정적 회복 필요", "잃은 것에 시선이 머물러 아직 남은 가능성을 놓치기 쉬운 카드입니다. 슬픔을 부정하지 말되 거기서만 멈추지는 않아야 합니다.", image("minor/cups/five"), TarotSuit.CUPS),
    SIX_OF_CUPS("Six of Cups", TarotArcanaType.MINOR, "추억, 익숙함, 과거의 연결", "과거의 사람이나 기억이 현재에 영향을 주는 흐름입니다. 익숙함이 위로가 될 수 있지만 현재의 성장까지 멈추게 해서는 안 됩니다.", image("minor/cups/six"), TarotSuit.CUPS),
    SEVEN_OF_CUPS("Seven of Cups", TarotArcanaType.MINOR, "선택지 과다, 환상, 분산된 집중", "가능성이 많아 보이지만 실제로는 집중력이 흩어질 위험이 큰 카드입니다. 매력적인 상상보다 현실적으로 잡을 수 있는 선택지가 중요합니다.", image("minor/cups/seven"), TarotSuit.CUPS),
    EIGHT_OF_CUPS("Eight of Cups", TarotArcanaType.MINOR, "거리두기, 이탈, 새로운 탐색", "겉으로는 충분해 보여도 마음이 더 이상 머물지 않는 상태를 뜻합니다. 익숙한 것을 떠나는 결심이 다음 성장을 위한 조건이 됩니다.", image("minor/cups/eight"), TarotSuit.CUPS),
    NINE_OF_CUPS("Nine of Cups", TarotArcanaType.MINOR, "만족, 소원 성취, 정서적 풍요", "바라던 것이 이루어지며 만족감이 커지는 카드입니다. 지금의 성취를 누리는 것도 필요하지만 욕망이 과해지지 않도록 균형을 보는 눈도 중요합니다.", image("minor/cups/nine"), TarotSuit.CUPS),
    TEN_OF_CUPS("Ten of Cups", TarotArcanaType.MINOR, "화목, 장기적 안정, 만족감", "감정적 안정과 관계의 조화가 길게 이어지는 그림입니다. 개인의 만족을 넘어 함께 만드는 행복이 핵심 주제로 떠오릅니다.", image("minor/cups/ten"), TarotSuit.CUPS),
    PAGE_OF_CUPS("Page of Cups", TarotArcanaType.MINOR, "감성적 제안, 직관적 메시지", "예상치 못한 감정의 신호나 부드러운 제안이 들어오는 카드입니다. 논리로만 재단하지 말고 작지만 진심 어린 메시지를 받아들일 필요가 있습니다.", image("minor/cups/page"), TarotSuit.CUPS),
    KNIGHT_OF_CUPS("Knight of Cups", TarotArcanaType.MINOR, "이상 추구, 제안, 감정적 추진", "마음이 이끄는 방향으로 부드럽게 다가가는 카드입니다. 로맨틱하고 매력적이지만 현실 감각을 잃지 않아야 약속이 실제가 됩니다.", image("minor/cups/knight"), TarotSuit.CUPS),
    QUEEN_OF_CUPS("Queen of Cups", TarotArcanaType.MINOR, "공감, 섬세함, 깊은 직관", "타인의 마음을 세심하게 읽고 감정을 담아내는 힘이 강한 카드입니다. 지금은 판단보다 공감과 수용이 더 큰 해답이 될 수 있습니다.", image("minor/cups/queen"), TarotSuit.CUPS),
    KING_OF_CUPS("King of Cups", TarotArcanaType.MINOR, "감정 통제, 성숙함, 균형 감각", "감정을 억누르지 않으면서도 휘둘리지 않는 성숙함을 의미합니다. 복잡한 상황일수록 차분한 중심을 잡는 사람이 흐름을 이끕니다.", image("minor/cups/king"), TarotSuit.CUPS),
    ACE_OF_SWORDS("Ace of Swords", TarotArcanaType.MINOR, "명확한 판단, 통찰, 결단", "안개가 걷히듯 문제의 핵심이 선명해지는 카드입니다. 이제는 애매하게 남겨 두기보다 정확한 판단과 결단이 필요합니다.", image("minor/swords/ace"), TarotSuit.SWORDS),
    TWO_OF_SWORDS("Two of Swords", TarotArcanaType.MINOR, "보류, 갈등, 판단 유예", "둘 중 하나를 골라야 하지만 아직 마음이 닫혀 있는 상태입니다. 균형을 유지하는 척하며 미루기보다 필요한 정보를 마주해야 합니다.", image("minor/swords/two"), TarotSuit.SWORDS),
    THREE_OF_SWORDS("Three of Swords", TarotArcanaType.MINOR, "상처, 충격, 냉정한 진실", "아프지만 외면할 수 없는 진실이 드러나는 카드입니다. 상처를 피하려 하기보다 받아들이는 과정이 회복의 시작이 됩니다.", image("minor/swords/three"), TarotSuit.SWORDS),
    FOUR_OF_SWORDS("Four of Swords", TarotArcanaType.MINOR, "휴식, 회복, 재정비", "지금은 싸움을 멈추고 정신과 몸을 회복해야 하는 시기라는 뜻입니다. 잠시 쉬는 것이 후퇴가 아니라 다음 움직임을 위한 준비가 됩니다.", image("minor/swords/four"), TarotSuit.SWORDS),
    FIVE_OF_SWORDS("Five of Swords", TarotArcanaType.MINOR, "소모적 갈등, 승패 집착", "이겨도 개운하지 않은 싸움을 의미하는 카드입니다. 자존심을 지키려다 관계나 신뢰를 잃고 있지 않은지 점검해야 합니다.", image("minor/swords/five"), TarotSuit.SWORDS),
    SIX_OF_SWORDS("Six of Swords", TarotArcanaType.MINOR, "이동, 전환, 점진적 회복", "거칠었던 구간을 지나 더 나은 방향으로 이동하는 흐름입니다. 완전히 해결된 것은 아니어도 분명 이전보다는 나아지는 과정에 있습니다.", image("minor/swords/six"), TarotSuit.SWORDS),
    SEVEN_OF_SWORDS("Seven of Swords", TarotArcanaType.MINOR, "은밀함, 전략, 정보 비대칭", "정면 승부보다 계산과 전략이 앞서는 상황을 뜻합니다. 누군가의 의도가 감춰져 있을 수 있으니 정보 확인이 특히 중요합니다.", image("minor/swords/seven"), TarotSuit.SWORDS),
    EIGHT_OF_SWORDS("Eight of Swords", TarotArcanaType.MINOR, "제약, 심리적 압박, 시야 제한", "실제로는 탈출구가 있는데도 스스로 갇혀 있다고 느끼는 카드입니다. 가장 먼저 풀어야 할 것은 외부 조건보다 내 인식의 한계일 수 있습니다.", image("minor/swords/eight"), TarotSuit.SWORDS),
    NINE_OF_SWORDS("Nine of Swords", TarotArcanaType.MINOR, "불안, 걱정, 과도한 염려", "밤에 혼자 커지는 생각처럼 걱정이 증폭된 상태를 의미합니다. 모든 두려움이 현실은 아니므로 사실과 상상을 분리할 필요가 있습니다.", image("minor/swords/nine"), TarotSuit.SWORDS),
    TEN_OF_SWORDS("Ten of Swords", TarotArcanaType.MINOR, "끝맺음, 바닥 확인, 재시작 전 단계", "완전히 끝났다는 자각이 들어오는 카드입니다. 고통스럽지만 바닥을 확인한 뒤라서 오히려 새로운 시작이 가능해집니다.", image("minor/swords/ten"), TarotSuit.SWORDS),
    PAGE_OF_SWORDS("Page of Swords", TarotArcanaType.MINOR, "탐색, 질문, 예민한 관찰", "궁금한 것을 놓치지 않고 계속 살피는 카드입니다. 아직 결론보다 탐색 단계이므로 섣부른 확정보다 질문과 관찰이 더 중요합니다.", image("minor/swords/page"), TarotSuit.SWORDS),
    KNIGHT_OF_SWORDS("Knight of Swords", TarotArcanaType.MINOR, "빠른 판단, 돌파, 공격적 실행", "생각이 서면 바로 돌진하는 추진력이 매우 강한 카드입니다. 속도는 장점이지만 주변 맥락까지 보지 않으면 충돌도 커질 수 있습니다.", image("minor/swords/knight"), TarotSuit.SWORDS),
    QUEEN_OF_SWORDS("Queen of Swords", TarotArcanaType.MINOR, "분석력, 냉정함, 기준의 명확성", "감정을 배제한다기보다 감정에 휘둘리지 않고 핵심을 가르는 카드입니다. 모호한 관계나 상황일수록 분명한 기준을 세우게 만듭니다.", image("minor/swords/queen"), TarotSuit.SWORDS),
    KING_OF_SWORDS("King of Swords", TarotArcanaType.MINOR, "전략, 논리, 객관적 통솔", "큰 그림 속에서 규칙과 논리로 판을 운영하는 카드입니다. 개인 감정보다 원칙과 구조, 객관성이 결과를 좌우합니다.", image("minor/swords/king"), TarotSuit.SWORDS),
    ACE_OF_PENTACLES("Ace of Pentacles", TarotArcanaType.MINOR, "현실적 기회, 자산의 시작, 수익 기반", "실질적인 보상과 자산의 씨앗이 들어오는 카드입니다. 작아 보여도 현실적인 기회를 잡으면 장기 기반으로 키울 수 있습니다.", image("minor/pentacles/ace"), TarotSuit.PENTACLES),
    TWO_OF_PENTACLES("Two of Pentacles", TarotArcanaType.MINOR, "균형, 자금 운용, 유연한 대응", "여러 현실 과제를 동시에 다루며 균형을 맞춰야 하는 상황입니다. 완벽함보다 유연한 운영 능력이 성패를 가릅니다.", image("minor/pentacles/two"), TarotSuit.PENTACLES),
    THREE_OF_PENTACLES("Three of Pentacles", TarotArcanaType.MINOR, "협업, 숙련, 실무 성과", "혼자보다 역할을 나눠 협업할 때 결과가 더 좋아지는 카드입니다. 실력은 인정받고 있고, 구체적 완성도가 중요해집니다.", image("minor/pentacles/three"), TarotSuit.PENTACLES),
    FOUR_OF_PENTACLES("Four of Pentacles", TarotArcanaType.MINOR, "보수성, 축적, 방어적 관리", "지키는 힘은 강하지만 지나치면 흐름이 막히는 카드입니다. 안전을 챙기되 움켜쥐기만 하면 성장 기회를 놓칠 수 있습니다.", image("minor/pentacles/four"), TarotSuit.PENTACLES),
    FIVE_OF_PENTACLES("Five of Pentacles", TarotArcanaType.MINOR, "재정 압박, 결핍, 지원 필요", "현실적 부족감과 외로움이 크게 느껴지는 흐름입니다. 혼자 버티려 하기보다 도움을 요청하고 연결을 회복하는 것이 중요합니다.", image("minor/pentacles/five"), TarotSuit.PENTACLES),
    SIX_OF_PENTACLES("Six of Pentacles", TarotArcanaType.MINOR, "지원, 균형 있는 분배, 실질 도움", "주고받음의 균형과 현실적 지원을 뜻하는 카드입니다. 도움을 줄 때도 받을 때도 건강한 균형 감각이 필요합니다.", image("minor/pentacles/six"), TarotSuit.PENTACLES),
    SEVEN_OF_PENTACLES("Seven of Pentacles", TarotArcanaType.MINOR, "기다림, 점검, 장기 수확 준비", "당장 큰 결과가 없더라도 중간 점검과 인내가 필요한 시기입니다. 지금은 속도보다 축적의 질을 확인하는 편이 맞습니다.", image("minor/pentacles/seven"), TarotSuit.PENTACLES),
    EIGHT_OF_PENTACLES("Eight of Pentacles", TarotArcanaType.MINOR, "반복 훈련, 성실함, 기술 축적", "꾸준한 연습과 성실한 반복이 실력을 만드는 카드입니다. 화려함은 없더라도 디테일을 쌓는 사람이 결국 앞서게 됩니다.", image("minor/pentacles/eight"), TarotSuit.PENTACLES),
    NINE_OF_PENTACLES("Nine of Pentacles", TarotArcanaType.MINOR, "자립, 안정, 축적된 성과", "스스로의 힘으로 일군 안정과 여유를 상징합니다. 단단하게 쌓아 온 결과가 드러나며 자기 만족도도 높아집니다.", image("minor/pentacles/nine"), TarotSuit.PENTACLES),
    TEN_OF_PENTACLES("Ten of Pentacles", TarotArcanaType.MINOR, "장기 자산, 유산, 견고한 기반", "개인의 성취를 넘어 오래 남을 기반과 축적을 뜻하는 카드입니다. 가족과 조직, 공동체 차원의 안정까지 연결되기 쉽습니다.", image("minor/pentacles/ten"), TarotSuit.PENTACLES),
    PAGE_OF_PENTACLES("Page of Pentacles", TarotArcanaType.MINOR, "현실적 학습, 씨앗 투자, 성실한 출발", "배우고 익히며 차근차근 현실 성과로 연결하는 카드입니다. 크게 뛰기보다 작아도 확실한 첫 투자와 연습이 중요합니다.", image("minor/pentacles/page"), TarotSuit.PENTACLES),
    KNIGHT_OF_PENTACLES("Knight of Pentacles", TarotArcanaType.MINOR, "꾸준함, 느리지만 안정적 전진", "속도는 느려도 흔들림 없이 끝까지 가는 힘을 의미합니다. 지루해 보여도 지속성과 성실함이 가장 큰 자산이 됩니다.", image("minor/pentacles/knight"), TarotSuit.PENTACLES),
    QUEEN_OF_PENTACLES("Queen of Pentacles", TarotArcanaType.MINOR, "실속, 관리 능력, 안정적 운영", "현실 감각과 돌봄 능력을 함께 갖춘 카드입니다. 돈과 생활, 관계를 모두 안정적으로 운영하는 실무 감각이 돋보입니다.", image("minor/pentacles/queen"), TarotSuit.PENTACLES),
    KING_OF_PENTACLES("King of Pentacles", TarotArcanaType.MINOR, "물질적 성취, 재정 통제, 신뢰성", "결과를 내고 자산을 지켜 내는 성숙한 현실 감각의 카드입니다. 신뢰를 기반으로 판을 안정적으로 운영하는 힘이 강합니다.", image("minor/pentacles/king"), TarotSuit.PENTACLES);

    val code: String
        get() = name

    val deckType: TarotDeckType
        get() = TarotDeckType.TAROT

    val koreanDisplayName: String
        get() = MAJOR_ARCANA_KOREAN_NAMES[this] ?: buildMinorArcanaKoreanName()

    val key: String
        get() = "tarot.${cardNumber.toString().padStart(2, '0')}"

    val cardNumber: Int
        get() = ordinal

    val sortOrder: Int
        get() = ordinal

    val videoUrl: String?
        get() = null

    fun toMetadata(deckVersionId: String = DEFAULT_TAROT_DECK_VERSION_ID): TarotCardMetadata =
        TarotCardMetadata(
            selectedIndex = ordinal,
            code = code,
            deckVersionId = deckVersionId,
            deckType = deckType,
            deckRole = TarotDeckRole.MAIN,
            cardSetId = DEFAULT_TAROT_CARD_SET_ID,
            name = displayName,
            koreanName = koreanDisplayName,
            sortOrder = sortOrder,
            arcanaType = arcanaType,
            suit = suit,
            meaning = uprightMeaning,
            description = description,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )

    companion object {
        private val MAJOR_ARCANA_KOREAN_NAMES = mapOf(
            THE_FOOL to "바보",
            THE_MAGICIAN to "마법사",
            THE_HIGH_PRIESTESS to "여사제",
            THE_EMPRESS to "여황제",
            THE_EMPEROR to "황제",
            THE_HIEROPHANT to "교황",
            THE_LOVERS to "연인",
            THE_CHARIOT to "전차",
            STRENGTH to "힘",
            THE_HERMIT to "은둔자",
            WHEEL_OF_FORTUNE to "운명의 수레바퀴",
            JUSTICE to "정의",
            THE_HANGED_MAN to "매달린 남자",
            DEATH to "죽음",
            TEMPERANCE to "절제",
            THE_DEVIL to "악마",
            THE_TOWER to "탑",
            THE_STAR to "별",
            THE_MOON to "달",
            THE_SUN to "태양",
            JUDGEMENT to "심판",
            THE_WORLD to "세계"
        )

        private val MINOR_RANK_KOREAN_NAMES = mapOf(
            "ACE" to "에이스",
            "TWO" to "2",
            "THREE" to "3",
            "FOUR" to "4",
            "FIVE" to "5",
            "SIX" to "6",
            "SEVEN" to "7",
            "EIGHT" to "8",
            "NINE" to "9",
            "TEN" to "10",
            "PAGE" to "시종",
            "KNIGHT" to "기사",
            "QUEEN" to "여왕",
            "KING" to "왕"
        )

        fun fromIndex(index: Int): TarotCard = entries[index]

        fun fromCode(code: String): TarotCard =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("알 수 없는 타로 카드 코드입니다: $code")

        fun deck(): List<TarotCard> = entries.toList()
    }

    private fun buildMinorArcanaKoreanName(): String {
        val suitName = when (suit) {
            TarotSuit.WANDS -> "완드"
            TarotSuit.CUPS -> "컵"
            TarotSuit.SWORDS -> "소드"
            TarotSuit.PENTACLES -> "펜타클"
            null -> return displayName
        }
        val rank = requireNotNull(MINOR_RANK_KOREAN_NAMES[name.substringBefore("_OF_")]) {
            "Unknown minor arcana rank for tarot card code=$name"
        }
        return "$suitName $rank"
    }

}
