# 투자 심리 운세 상담 — 모드별 프롬프트 & 페이로드 명세

**API** `POST /api/consult`  
**관련 코드** `ConsultingService`, `LlmPromptTemplateService`, `SajuAiPayload`

---

## 목차

1. [공통 구조](#1-공통-구조)
2. [INVESTMENT_SAJU — 사주 모드](#2-investment_saju--사주-모드)
3. [INVESTMENT_TAROT — 타로 모드](#3-investment_tarot--타로-모드)
4. [INVESTMENT_ZODIAC — 별자리 모드](#4-investment_zodiac--별자리-모드)
5. [INVESTMENT_ALL — 종합 모드](#5-investment_all--종합-모드)
6. [시나리오 목록](#6-시나리오-목록)
7. [AI 응답 스키마](#7-ai-응답-스키마)
8. [출력 제약 조건](#8-출력-제약-조건)

---

## 1. 공통 구조

### 1-1. 시스템 프롬프트 조립 순서

단일 모드(SAJU / TAROT / ZODIAC)는 아래 순서로 하나의 시스템 메시지를 조립하여 AI에 전달한다.

```
[1]  모드 기본 프롬프트      ← DefaultConsultingPromptContent (DB 오버라이드 가능)
[2]  역할 제약               ← "투자 지시, 매수/매도 단정, 수익 보장은 금지"
[3]  시나리오 지시           ← scenario.responseInstructionAddon()
[4]  근거 범위 제한          ← sourceBoundaryInstruction()
[5]  어조 통제 및 점수 동기화 ← serverScore.toToneInstruction()
[6]  활성 섹션 지정          ← analysisSectionInstruction()
[7]  JSON 출력 형식          ← 키·글자 수·줄 수 제약
[8]  말투 지시               ← 생활어, 운세 서비스 톤
[9]  내부 키값 노출 금지      ← 영문 식별자 → 자연어 번역 강제  ★
[10] 질문 응답 가이드        ← 시나리오 틀 안에서 question 연결
```

INVESTMENT_ALL 종합 합성 메시지 조립 순서는 [§5-2](#5-2-합성synthesis-프롬프트) 참조.

### 1-2. 어조 통제 및 점수 동기화 (`[5]`)

서버가 AI 호출 전에 `serverCalculatedScore`를 계산하여 시스템 메시지와 페이로드에 선행 주입한다.  
AI는 텍스트 어조를 이 점수에 맞추고, `stability_score` 필드에 이 값을 그대로 반환해야 한다.

| 점수 범위 | 요구 어조 |
|---|---|
| 65 이상 | 긍정적이고 여유 있는 어조 |
| 40 ~ 64 | 기회와 리스크를 동시에 짚어주는 신중한 어조 |
| 39 이하 | 단호하고 경계심을 주는 보수적인 어조 |

> **목적**: AI가 낙관적 텍스트를 쓴 뒤 서버가 30점으로 덮어쓰는 어조 모순을 방지한다.

### 1-3. 내부 키값 노출 금지 (`[9]`)

`readingHints` 등 페이로드에 포함된 영문 식별자와 시스템 내부 명칭을 AI가 그대로 출력하지 않도록 시스템 메시지에 강제 규칙을 주입한다.

| 내부 식별자 (노출 금지) | 자연어 번역 예시 |
|---|---|
| `conviction_strong` | "강한 확신으로 일관된 매매 스타일을 유지합니다" |
| `volatility_risk` | "오늘은 감정적 마찰로 인한 투자 변동성에 유의하세요" |
| `self_conflict` | "내면의 갈등이 판단을 흐릴 수 있는 날입니다" |
| `asset_locking` | "자산이 묶이는 흐름으로 유동성 확보가 필요합니다" |
| `transition_signal` | "변화의 기운이 감지되는 전환점입니다" |

> **배경**: AI는 페이로드 JSON 키를 그대로 발화하려는 습성이 있어 사용자에게 내부 시스템 명칭이 노출될 수 있다.

### 1-4. 질문 응답 가이드 (`[10]`)

```
payload.question의 의도를 파악하되, 반드시 해당 시나리오의 틀 안에서 답을 구성하라.
질문이 시나리오를 벗어나더라도 운세 지표를 활용해 질문과 시나리오를 논리적으로 연결하라.
```

### 1-4. 공통 페이로드 최상위 필드

모든 단일 모드 AI 호출에서 공통으로 전달되는 필드다.

```json
{
  "mode": "INVESTMENT_SAJU",
  "serverCalculatedScore": 58,
  "scenario": {
    "code": "FLOW_CHECK",
    "title": "오늘의 흐름"
  },
  "question": "오늘 투자 흐름 어때?",
  "userContext": {
    "styleHint": "overall_summary에서만 안정 추구형 성향을 약하게 반영한다.",
    "focusLabel": null
  },
  "signals": {
    "saju": null,
    "zodiac": null,
    "birthTarotCard": null,
    "tarot": null
  }
}
```

| 필드 | 설명 |
|---|---|
| `serverCalculatedScore` | 서버가 선행 계산한 투자 심리 안정도(0~100). AI 어조 통제 및 score 반환에 사용 |
| `userContext.styleHint` | 투자 성향(`STABLE` / `AGGRESSIVE`). overall_summary에서만 약하게 반영 |
| `signals.*` | 모드에 따라 해당 신호만 채워지고 나머지는 `null` |

---

## 2. INVESTMENT_SAJU — 사주 모드

### 2-1. AI 역할 (기본 프롬프트)

> 당신은 명리학에 기반하여 냉철하고 전문적인 투자 조언을 제공하는 사주 기반 자산 관리사다.
> signals.saju의 사주 원국(natalChart)과 현재 운 흐름(대운·세운·월운·일진), 십성(tenGods) 분포를 주근거로 삼아 투자 심리를 해석한다.
>
> - 재성(편재·정재)이 여러 운에 집중되면 단기 차익 실현이나 적극적 매수 시점으로 해석한다.
> - 인성(편인·정인)이 강한 흐름은 장기 보유·우량주·부동산 또는 문서 계약에 유리하다고 조언한다.
> - 비견·겁재가 기구신으로 작용하면 군중 심리를 경계하고 철저한 리스크 관리와 현금 보유를 강조한다.
> - 운세는 투자의 참고 지표일 뿐임을 이성적 어조로 명시한다.

**핵심 분석 원칙**: 대운·세운·월운·일진 4계층의 십성 조합을 교차 분석. 같은 십성이 여러 운에 겹칠수록 강하게 반영, 혼재 시 균형과 리스크 함께 강조.

**[신강/신약 해석 기준]** `readingHints.energyBalance` 값에 따라 투자 전략 방향을 다르게 조언한다.

| energyBalance | 투자 성향 | 핵심 전략 조언 |
|---|---|---|
| `STRONG` (신강) | 주관과 확신이 매우 강한 타입. 넘치는 에너지를 발산하고 제어해야 함 | 포트폴리오 분산으로 에너지 순환. 자신감 과열 시 손절 라인 기계적 준수 |
| `WEAK` (신약) | 흐름에 유연하게 편승하는 타입. 부족한 에너지를 보호받아야 함 | 시장 대세·검증된 우량주 집중. 단타보다 근거 있는 자산에 묵직하게 집중 |
| `BALANCED` | 공격과 수비를 상황에 따라 조율하는 타입 | 현재 운 흐름의 십성에 따라 균형 있는 전략 제시 |

### 2-2. 추가 시스템 지시

```
근거: signals.saju가 주근거. userContext.styleHint는 overall_summary에서만 약하게 사용.
활성 섹션: saju_analysis. 나머지 analysis=null.
```

### 2-3. 페이로드 — `signals.saju` 구조

```json
{
  "summary": {
    "dayMaster": { "label": "일간", "labelKo": "무", "fiveElement": {"labelKo": "토"}, "yinYang": {"labelKo": "양"} },
    "dayBranch":  { "label": "일지", "labelKo": "신", "fiveElement": {"labelKo": "금"}, "yinYang": {"labelKo": "양"} },
    "monthBranch": { "label": "월지", "..." : "..." }
  },
  "natalChart": {
    "year":  { "label": "연주", "stem": {"labelKo": "갑"}, "branch": {"labelKo": "진"}, "combinedLabelKo": "갑진" },
    "month": { "label": "월주", "combinedLabelKo": "병인" },
    "day":   { "label": "일주", "combinedLabelKo": "무신" },
    "hour":  { "label": "시주", "combinedLabelKo": "임자" }
  },
  "characters": [
    { "positionLabelKo": "연간", "symbolLabelKo": "갑", "fiveElement": {"labelKo": "목"}, "yinYang": {"labelKo": "양"}, "referenceStemLabelKo": "무" },
    "..."
  ],
  "tenGods": [
    { "positionLabelKo": "연간", "characterLabelKo": "갑", "tenGod": {"labelKo": "편재"} },
    "..."
  ],
  "fiveElementBalance": { "wood": 2, "fire": 1, "earth": 3, "metal": 1, "water": 1,
    "description": "각 오행이 사주 원국에 몇 개 분포하는지 나타내는 개수다." },
  "yinYangBalance": { "yinCount": 3, "yangCount": 5 },
  "currentFortune": {
    "referenceYear": 2026,
    "majorFortune": {
      "pillar": { "label": "대운", "combinedLabelKo": "신미" },
      "stemTenStar": { "labelKo": "상관" },
      "branchTenStar": { "labelKo": "겁재" },
      "description": "현재 속한 대운 구간 정보다."
    },
    "yearlyFortune": {
      "year": 2026,
      "pillar": { "label": "세운", "combinedLabelKo": "병오" },
      "stemTenStar": { "labelKo": "편인" },
      "branchTenStar": { "labelKo": "정인" }
    }
  },
  "palza": "갑진 병인 무신 임자",
  "대운": "신미",
  "세운": "병오",
  "월운": {
    "기둥": "갑오",
    "천간십성": "편재",
    "지지십성": "정관"
  },
  "일진": {
    "기둥": "경자",
    "천간십성": "편관",
    "지지십성": "정재"
  },
  "readingHints": {
    "energyBalance": "STRONG",
    "energyBalanceDescription": "타고난 기운이 강해 고집을 버리고 포트폴리오를 분산할 때 수익이 나는 성향입니다.",
    "baseTraits":     ["conviction_strong", "style_consistent"],
    "dynamicSignals": ["재성이 일진에 집중되어 단기 수익 기회 포착 가능"],
    "riskFlags":      []
  }
}
```

| 필드 | 출처 | 비고 |
|---|---|---|
| `summary` | `SajuConsultingResult` | 일간·일지·월지 핵심 에너지 |
| `natalChart` | `SajuConsultingResult.analysis` | 출생 사주 4기둥 |
| `characters` / `tenGods` | `SajuConsultingResult.analysis` | 원국 전체 글자·십성 배열 |
| `fiveElementBalance` / `yinYangBalance` | `SajuConsultingResult.analysis` | 원국 오행·음양 분포 |
| `currentFortune.majorFortune` | `SajuConsultingResult.currentFortune` | 현재 대운 구간 |
| `currentFortune.yearlyFortune` | `SajuConsultingResult.currentFortune` | 올해 세운 |
| `대운` / `세운` | 위와 동일 (간단 표기) | 기둥 한 쌍 문자열 |
| `월운` | `GanzhiCalculator.calculate(referenceDateTime).month` | 요청 시점 기준 계산 |
| `일진` | `GanzhiCalculator.calculate(referenceDateTime).day` | 요청 시점 기준 계산 |
| `readingHints.energyBalance` | `SajuInvestmentFeatureService` | 신강/신약 에너지 균형: `STRONG` / `WEAK` / `BALANCED`. `saju_results.energy_balance`에 저장됨 |
| `readingHints.energyBalanceDescription` | `EnergyBalance.toDescription()` | 투자 성향 설명문 (한국어 자연어) |
| `readingHints.baseTraits` 외 | `SajuInvestmentFeatureService` | 투자 기질·신호·위험 플래그 (상담 시 동적 계산) |

---

## 3. INVESTMENT_TAROT — 타로 모드

### 3-1. AI 역할 (기본 프롬프트)

> 당신은 타로 카드의 상징성을 금융·투자 시장 상황에 빗대어 해석하는 타로 투자 애널리스트다.
> signals.tarot.drawnCards 3장을 서사로 엮어 투자 관점에서 해석하고, birthTarotCard는 성향 보조로만 참고한다.
>
> - 슈트별 투자 의미: 펜타클=장기투자·실물자산·안정성 / 소드=정보·분석·손절매 / 완드=단기타이밍·행동력 / 컵=투자심리·군중심리
> - 긍정적인 펜타클 카드는 장기 투자 진입점 또는 가치주 매수 기회로 해석한다.
> - 부정적 카드는 단순 실패가 아닌 손절매·비중 축소·악재 대비 등 리스크 관리 조언으로 승화시킨다.
> - 카드 3장의 흐름으로 '현재 주의할 점'과 '취해야 할 액션'을 명확히 제시한다.

**[타로 해석 제약]** 모든 카드는 정방향(Upright)으로만 추출된다. 긍정 카드는 온전히 긍정 에너지로, 부정 카드(예: 소드 3, 타워 등)는 지연·이중 부정 없이 강력한 경고 및 리스크 시그널로 직관적으로 해석한다.

### 3-2. 추가 시스템 지시

```
근거: signals.tarot.drawnCards 3장이 메인, signals.birthTarotCard는 서브.
      userContext.styleHint는 overall_summary에서만 약하게 사용.
활성 섹션: tarot_analysis. 나머지 analysis=null.
```

### 3-3. 페이로드 — `signals.tarot` 및 `signals.birthTarotCard` 구조

```json
{
  "birthTarotCard": {
    "name": "The Hierophant",
    "meaning": "전통, 체계, 신뢰",
    "arcanaType": "MAJOR",
    "suit": null
  },
  "tarot": {
    "readingStructure": "drawnCards 3장을 메인 근거로 보고, birthTarotCard는 성향을 보조하는 서브 카드로만 사용한다.",
    "interpretationMode": "MAIN_TRADITIONAL",
    "drawnCards": [
      {
        "위치": "현재 상황",
        "selectedIndex": 12,
        "name": "Ace of Pentacles",
        "koreanName": "펜타클 에이스",
        "meaning": "새로운 물질적 시작, 번영의 씨앗",
        "arcanaType": "MINOR",
        "suit": "PENTACLES"
      },
      {
        "위치": "투자의 장애물·리스크",
        "selectedIndex": 35,
        "name": "3 of Swords",
        "koreanName": "소드 3",
        "meaning": "슬픔, 심리적 상처, 명확한 손실 인식",
        "arcanaType": "MINOR",
        "suit": "SWORDS"
      },
      {
        "위치": "조언 및 결과",
        "selectedIndex": 7,
        "name": "The Chariot",
        "koreanName": "전차",
        "meaning": "의지, 통제, 승리를 향한 추진력",
        "arcanaType": "MAJOR",
        "suit": null
      }
    ],
    "assistantDecks": null
  }
}
```

| 필드 | 설명 |
|---|---|
| `drawnCards[].위치` | 스프레드 위치. 순서 고정: 현재 상황 → 투자의 장애물·리스크 → 조언 및 결과 |
| `drawnCards[].selectedIndex` | 덱 내 카드 인덱스 (0-based) |
| `drawnCards[].suit` | `PENTACLES` / `SWORDS` / `WANDS` / `CUPS` / `null`(메이저 아르카나) |
| `birthTarotCard` | 생년월일 기반 생일 타로카드. 성향 보조용 서브 카드 |
| `assistantDecks` | 어시스턴트 덱 사용 시 포함. 위치 레이블 없음 |

> **미지원**: `orientation`(정/역방향) — 현재 시스템에 역방향 뽑기 개념 없음.

---

## 4. INVESTMENT_ZODIAC — 별자리 모드

### 4-1. AI 역할 (기본 프롬프트)

> 당신은 점성술의 행성 이동과 하우스를 기반으로 시장 흐름과 개인 투자 타이밍을 읽어내는 금융 점성술사다.
> signals.zodiac의 element·moodKeyword·consultingAngle을 바탕으로 오늘의 투자 심리와 판단 방향을 해석한다.
>
> - 확장·팽창 기운의 기질(불·양기)은 포트폴리오 확장이나 과감한 수익 실현 기회로 해석한다.
> - 안정·수축 기운의 기질(흙·물·음기)은 장기 가치 투자, 리스크 축소, 보수적 자산 운용을 권장한다.
> - 정보 과신이나 군중 심리에 흔들리기 쉬운 기질이면 섣부른 판단과 정보 왜곡을 경계하라는 조언을 덧붙인다.

**[동적 기운 융합 규칙]** 사용자의 고정된 태양궁(`signKo`) 성향에, 오늘 하루 시장에 영향을 미치는 요일 지배 행성(`todayPlanetRule`)의 단기 기운을 결합하여 해석한다. 태양궁 기질이 오늘의 행성 기운(예: 수성의 정보 민감성, 화성의 공격성 등)과 만났을 때 발생하는 특별한 심리 변화나 투자 타이밍을 포착하여 텍스트를 다채롭게 구성한다.

### 4-2. 추가 시스템 지시

```
근거: signals.zodiac이 주근거. element, moodKeyword, consultingAngle을 질문 상황에 직접 연결.
      userContext.styleHint는 overall_summary에서만 약하게 사용.
활성 섹션: zodiac_analysis. 나머지 analysis=null.
```

### 4-3. 페이로드 — `signals.zodiac` 구조

```json
{
  "zodiac": {
    "signKo": "황소자리",
    "element": "흙",
    "moodKeyword": "지속력",
    "consultingAngle": "질문을 유지할 힘과 고집이 섞이는 지점으로 읽는다.",
    "todayPlanetRule": "수요일 - 수성 (정보 매매, 거래량, 뉴스)"
  }
}
```

| 필드 | 출처 | 설명 |
|---|---|---|
| `signKo` | 생년월일 → `ZodiacSign.from(birthDate)` | 태양궁 별자리 (한국어) |
| `element` | `ZodiacSign.element` | 불 / 흙 / 바람 / 물 |
| `moodKeyword` | `ZodiacSign.moodKeyword` | 기질 키워드 |
| `consultingAngle` | `ZodiacSign.consultingAngle()` | 투자 질문을 읽는 관점 서술 |
| `todayPlanetRule` | `referenceDateTime.dayOfWeek.toPlanetRuleKo()` | 요일 지배 행성 및 투자 키워드 ★ |

#### 요일별 지배 행성 매핑

| 요일 | 지배 행성 | 투자 키워드 |
|---|---|---|
| 월요일 | 달 | 감정, 변동성, 군중 심리 |
| 화요일 | 화성 | 공격성, 투기, 빠른 결단 |
| 수요일 | 수성 | 정보 매매, 거래량, 뉴스 |
| 목요일 | 목성 | 확장, 낙관, 우량주 |
| 금요일 | 금성 | 수익 실현, 배당, 소비 |
| 토요일 | 토성 | 인내, 가치 투자, 리스크 관리 |
| 일요일 | 태양 | 중심, 큰 흐름, 자아 |

> 천문 계산 라이브러리 없이 매일 달라지는 별자리 운세를 구현하기 위한 하드코딩 매핑이다.  
> `moon_sign`, `ascendant`(어센던트), 실시간 행성 트랜짓은 미지원.

---

## 5. INVESTMENT_ALL — 종합 모드

### 5-1. 처리 흐름

```
① INVESTMENT_SAJU AI 호출  ─┐
② INVESTMENT_TAROT AI 호출  ├─ 병렬 (CompletableFuture)
③ INVESTMENT_ZODIAC AI 호출 ─┘
           ↓
④ 세 응답의 stabilityScore → 신호 레이블 변환(긍정/혼합/부정)
           ↓
⑤ 합성(Synthesis) AI 호출  ← 교차 검증 전용 프롬프트
           ↓
⑥ 최종 응답 조합
   - analysisResults: ①②③의 각 analysis 섹션 병합
   - finalAdvice:     ⑤ 합성 응답의 overall_summary
   - stabilityScore:  prepared.serverScore (서버 선행 계산값으로 덮어씀)
```

①②③ 각 호출은 해당 단일 모드의 프롬프트와 페이로드(`serverCalculatedScore` 포함)를 그대로 사용한다.

### 5-2. 합성(Synthesis) 프롬프트 조립 순서

```
[1] 종합 모드 기본 프롬프트  ← CONSULTING_SYSTEM_INVESTMENT_ALL
[2] 역할 제약
[3] 시나리오 지시
[4] 교차 검증 규칙           ← scenario.crossValidationInstruction()  ★
[5] 어조 통제 및 점수 동기화 ← serverScore.toToneInstruction()        ★
[6] JSON 출력 형식
[7] 말투 지시
```

#### AI 역할 (기본 프롬프트)

> 당신은 사주·타로·별자리 세 가지 운세 지표를 종합하여 최종 투자 포지션을 결정하는 수석 자산 전략가다.
> sections[]의 각 지표별 signal(긍정·혼합·부정)과 summary를 교차 검증(크로스 밸리데이션)한다.
>
> - 세 지표가 모두 일치하면 강력한 행동 지침을, 지표가 상충하면 철저한 리스크 관리와 선별적 투자를 지시한다.
> - overall_summary에 오늘의 투자 기상도(맑음/흐림/비)와 권장 자산 대 현금 비중(예: 자산 60%/현금 40%)을 반드시 명시하라.

#### 교차 검증 규칙 (`[4]`)

**시나리오별 우선순위**

| 시나리오 | 메인 지표 | 보조 지표 |
|---|---|---|
| `FLOW_CHECK`, `MENTAL_CARE` | 타로 · 별자리 (단기 심리·흐름) | 사주 (백그라운드 기운) |
| `ENTRY_READY`, `HOLD_OR_EXIT` | 사주 (장기 그릇·타이밍) | 타로 · 별자리 (단기 리스크) |

**충돌 융합 공식**

| 시그널 조합 | 해석 방향 |
|---|---|
| 2긍정 + 1부정 | 전반 흐름은 좋으나, 부정 지표의 리스크를 주의하면 기회 포착 가능 |
| 2부정 + 1긍정 | 리스크가 크므로 방어 우선, 긍정 조건 만족 시에만 제한적 행동 |
| 1긍정 + 1부정 + 1혼합 | 행동 유보 · 현금 관망 · 포트폴리오 재점검 최우선 |

**`overall_summary` 제약**: 충돌 시 두루뭉술한 결론 금지. "자산 40%/현금 60%" 같은 명확한 수치나 "신규 진입 절대 금지" 같은 단호한 행동 지침으로 결론을 맺는다.

### 5-3. 합성 페이로드 구조

```json
{
  "mode": "INVESTMENT_ALL",
  "serverCalculatedScore": 58,
  "sections": [
    {
      "name": "사주",
      "signal": "긍정",
      "summary": "재성이 월운·일진에 집중되어 단기 차익 기회가 엿보인다."
    },
    {
      "name": "타로",
      "signal": "혼합",
      "summary": "펜타클 에이스로 기회는 보이나 소드3 카드로 예상치 못한 악재 주의."
    },
    {
      "name": "별자리",
      "signal": "긍정",
      "summary": "황소자리 지속력 기운으로 보유 유지에 유리한 흐름."
    }
  ]
}
```

#### 신호 레이블 변환 기준

| AI 섹션 안정도 점수 | signal |
|---|---|
| 65 이상 | `긍정` |
| 40 이상 64 이하 | `혼합` |
| 39 이하 | `부정` |

---

## 6. 시나리오 목록

모든 모드에서 `scenario`는 필수 파라미터다. 시나리오에 따라 AI 응답 방향과 교차 검증 우선순위가 달라진다.

| code | title | 응답 방향 | 교차 검증 우선순위 |
|---|---|---|---|
| `FLOW_CHECK` | 오늘의 흐름 | 재물운 흐름과 투자 심리 패턴을 운세 관점에서 해석 | 타로·별자리 메인 |
| `ENTRY_READY` | 진입할까? | 지금 진입해도 좋은 시기인지, 대기해야 할 신호가 있는지 조언 | 사주 메인 |
| `HOLD_OR_EXIT` | 버틸까 나올까 | 유지할 때의 이점과 손절·청산해야 할 신호를 구분해 제시 | 사주 메인 |
| `MENTAL_CARE` | 손실과 멘탈 관리 | 흔들리는 감정의 원인 짚기 + 지금 당장 멈춰야 할 행동 제시 | 타로·별자리 메인 |

---

## 7. AI 응답 스키마

모든 AI 호출은 아래 JSON 형식으로만 응답한다.

```json
{
  "mode": "INVESTMENT_SAJU",
  "saju_analysis": {
    "title": "투자 기질 해석",
    "content": "일진에 정재가 들어와 오늘 하루 소소한 수익 기회가 있다."
  },
  "tarot_analysis": null,
  "zodiac_analysis": null,
  "overall_summary": "오늘은 재성이 일진에 집중된 날.\n정보 확인 후 소량 분산 진입을 고려하라.",
  "stability_score": 58
}
```

#### 합성(ALL) 응답 예시

```json
{
  "mode": "INVESTMENT_ALL",
  "saju_analysis": null,
  "tarot_analysis": null,
  "zodiac_analysis": null,
  "overall_summary": "☀️ 맑음 — 자산 60% / 현금 40%\n기회는 있으나 타로 악재 신호로 분산 진입 후 손절선을 미리 정하라.",
  "stability_score": 58
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `mode` | String | 요청 모드 그대로 반환 |
| `saju_analysis` | `{title, content}` \| null | 사주 모드에서만 활성 |
| `tarot_analysis` | `{title, content}` \| null | 타로 모드에서만 활성 |
| `zodiac_analysis` | `{title, content}` \| null | 별자리 모드에서만 활성 |
| `overall_summary` | String | 핵심 요약. 줄바꿈(`\n`)으로 두 줄 구성 |
| `stability_score` | Int (0–100) | `serverCalculatedScore` 값을 그대로 반환. AI가 임의 생성 금지 |

---

## 8. 출력 제약 조건

| 항목 | 제약 |
|---|---|
| `analysis.content` | 1~2문장, 160자 이내 |
| `overall_summary` (단일 모드) | 2줄 이내, 140자 이내 |
| `overall_summary` (ALL 종합) | 2줄 이내, 180자 이내 |
| 전체 JSON 텍스트 (단일 모드) | 500자 이내 |
| `analysis` 내 `styleHint` 반영 | 금지 — overall_summary에서만 약하게 허용 |
| 투자 지시 / 종목 단정 / 수익 보장 | 금지 (`FortuneSafetyGuard`로 후처리 적용) |
| `stability_score` 최종값 | 서버가 AI 호출 전 선행 계산(`serverCalculatedScore`)하여 주입. AI는 그 값을 그대로 반환해야 하며, 서버는 `overrideStabilityScore()`로 최종 덮어씀 |
| 타로 카드 방향 | 정방향(Upright)만 사용. 부정 카드는 이중 부정 없이 강력한 경고 시그널로 직해석 |
| AI 내부 키값 노출 | 금지 — `readingHints`의 영문 식별자는 반드시 자연어로 번역하여 서술 |

---

## 9. 서버 점수 계산 방어 로직

`ConsultingRiskScoreCalculator`는 플래그 중첩으로 인한 점수 발산을 두 겹으로 방어한다.

| 단계 | 클램핑 | 범위 |
|---|---|---|
| `computeDivinationScore()` | `coerceIn(-30, 30)` | 개별 플래그 누적값 제한 |
| `calculate()` 최종 반환 | `coerceIn(0, 100)` | 성향 배수·시나리오 보정 후 최종 제한 |

이론상 최대·최솟값: 기본 50 ± (최대 중간점수 30 × 배수 1.3) ± 시너지 5 ± 시나리오 보정 5 → **약 6 ~ 95** 이내로 유지됨.
