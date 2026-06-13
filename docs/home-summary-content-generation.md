# Home Summary Content Generation

## Runtime Policy

Home summary API must not call an LLM at request time.

The API should keep using deterministic server-side composition for:

- stable response latency
- predictable wording length
- low operating cost
- easier QA for investment-sensitivity wording
- consistent frontend rendering of `dailyBody`, `personalBody`, and `points`

Use an LLM only as an offline content production assistant. Generated copy must be reviewed before being stored in seed code or DB rows.

## Content Workflow

1. Generate draft copy with an LLM using the prompts below.
2. Review for length, tone, and investment safety.
3. Store approved copy in code seed data or DB update SQL.
4. The home summary API reads approved content or uses deterministic fallback text.

Do not store raw LLM output without review.

## Output Rules

Every generated item must follow this shape:

```json
{
  "dailyBody": "1-2 short sentences.",
  "personalBody": "1-2 short sentences.",
  "points": [
    "매수/매도: one concrete instruction.",
    "리스크 관리: one concrete instruction.",
    "마인드셋: one concrete instruction."
  ]
}
```

Rules:

- Avoid direct investment advice such as guaranteed profit, exact target prices, or certainty language.
- Prefer guidance words such as `확인`, `점검`, `보류`, `비중`, `기준`, `관망`.
- Keep `dailyBody` and `personalBody` short enough for a modal.
- `personalBody` must start with the user's concrete trait, not generic wording like `평소 방식` or `오늘 분위기`.
- `points` must cover three distinct roles: position, risk, mindset.

## Saju Prompt

Use this prompt to produce copy for a daily stem/branch and a user's natal day pillar.

```text
You are writing Korean copy for a fortune-investment care app.

Input:
- 오늘의 천간/지지: {todayGanji} ({todayHanja})
- 오늘의 기운 키워드: {todayStemKeyword}, {todayBranchKeyword}
- 사용자의 출생 일주: {userGanji}
- 사용자의 일주 키워드: {userStemKeyword}, {userBranchKeyword}

Write JSON only:
{
  "dailyBody": string,
  "personalBody": string,
  "points": string[]
}

dailyBody:
- 1-2 concise Korean sentences.
- Describe today's market psychology from the daily stem/branch.
- Explain whether the day favors fast short-term profit chasing, defensive holding, or measured adjustment.

personalBody:
- 1-2 concise Korean sentences.
- Start by naming the user's concrete day-pillar trait.
- Explain how it meets today's stem/branch energy.
- Do not write "오늘은 OO이고 내 기운은 OO입니다."
- Do not use vague phrases like "평소 방식" or "오늘 분위기."

points:
- exactly 3 Korean strings.
- Use these roles in order:
  1. "매수/매도: ..."
  2. "리스크 관리: ..."
  3. "마인드셋: ..."
- Each point must be one short practical instruction.

Safety:
- Do not promise profit.
- Do not recommend specific stocks.
- Avoid medical, legal, or financial certainty language.
```

## Tarot Prompt

Use this prompt to produce copy for a daily tarot card and a user's birth tarot card.

```text
You are writing Korean copy for a fortune-investment care app.

Input:
- 오늘 뽑힌 카드: {todayCardName}
- 오늘 카드 정방향 의미: {todayCardMeaning}
- 오늘 카드 투자 행동 분류: {investmentAction}
- 사용자의 탄생 카드: {birthCardName}
- 탄생 카드 키워드: {birthCardKeyword}

Write JSON only:
{
  "dailyBody": string,
  "personalBody": string,
  "points": string[]
}

dailyBody:
- 1-2 concise Korean sentences.
- Interpret the daily card as one of: 현금 확보, 수익 실현, 과감한 베팅, 포트폴리오 유지.
- Explain the action symbolically, not as guaranteed financial advice.

personalBody:
- 1-2 concise Korean sentences.
- Start with the user's birth-card trait.
- Explain how the birth-card trait harmonizes or conflicts with today's card action.
- Avoid vague phrases like "평소 판단 방식."

points:
- exactly 3 Korean strings.
- Use these roles in order:
  1. "매수/매도: ..."
  2. "리스크 관리: ..."
  3. "마인드셋: ..."
- Make the three points non-overlapping.

Safety:
- Do not promise profit.
- Do not recommend specific stocks.
- Avoid certainty language.
```

## Zodiac Prompt

Use this prompt to produce copy for today's moon sign and a user's sun sign.

```text
You are writing Korean copy for a fortune-investment care app.

Input:
- 오늘 달의 별자리: {moonSign}
- 오늘 달 별자리 키워드: {moonKeyword}
- 오늘 유리한 자산/호흡: {marketBias}, {marketPace}
- 사용자의 태양 별자리: {sunSign}
- 사용자 태양 별자리 키워드: {sunKeyword}

Write JSON only:
{
  "dailyBody": string,
  "personalBody": string,
  "points": string[]
}

dailyBody:
- 1-2 concise Korean sentences.
- Describe which asset type or investment pace the moon sign favors.

personalBody:
- 1-2 concise Korean sentences.
- Start with the user's sun-sign keyword.
- Mention the moon-sign keyword directly.
- Explain how the two traits collide or support each other.
- Do not use vague phrases like "평소 방식" or "오늘 분위기."

Expected style example:
"물병자리 특유의 빠른 관점 전환으로 새로운 테마주에 눈이 가기 쉬운 날입니다. 하지만 황소자리의 실물자산과 보수성 기운은 혁신적인 아이디어보다 실적 기반의 가치주를 먼저 보라고 말합니다."

points:
- exactly 3 Korean strings.
- Use these roles in order:
  1. "매수/매도: ..."
  2. "리스크 관리: ..."
  3. "마인드셋: ..."

Safety:
- Do not promise profit.
- Do not recommend specific stocks.
- Avoid certainty language.
```

## Applying Approved Saju Copy

The current DB has `saju_interpretations` for day-pillar copy. If approved content needs to override a row, apply SQL like this after review:

```sql
UPDATE saju_interpretations
SET
  summary_default = :approved_summary_default,
  summary_easy = :approved_summary_easy,
  updated_at = CURRENT_TIMESTAMP
WHERE category = 'DAY_PILLAR'
  AND code = :ganji_code;
```

For tarot and zodiac content, keep runtime composition deterministic unless a dedicated reviewed-content table is introduced later. If a new table is needed, propose a migration before changing application code.
