# Premium Consulting Backend Spec Draft

## 목적
유료 사용자 전용 `연속 상담 스레드` 기능을 `Gemini 중심 + 자체 스레드 저장 + 최신 웹검색 grounding` 기준으로 재정의한다.

이 문서는 Spring Boot 기준 백엔드 설계 초안이며, 아래 목표를 동시에 만족시키는 것을 전제로 한다.

1. 유료 사용자는 이전 상담 흐름을 이어서 질문할 수 있어야 한다.
2. 모델 벤더의 서버 측 스레드 기능에 종속되지 않아야 한다.
3. 투자 상담은 반드시 최신 시장 데이터와 최신 웹 근거를 반영해야 한다.
4. 오래된 뉴스, 오래된 평균단가, 오래된 시세로 답변하는 일을 운영 정책상 차단해야 한다.

## 이번 설계의 결론

### 채택안
1. 메인 모델은 `Gemini`를 사용한다.
2. 스레드 상태는 `서비스 DB`가 직접 관리한다.
3. 긴 대화 전체를 매번 보내지 않고 `최근 대화 + 요약 메모리 + 최신 데이터`만 모델에 전달한다.
4. 최신성이 필요한 투자 질문은 `Gemini Google Search grounding`을 반드시 켠다.
5. 종목/포지션 관련 정량 데이터는 항상 `KIS + 내부 DB`를 우선 진실 원천으로 사용한다.
6. 뉴스/동향은 웹검색 결과가 일정 freshness 조건을 만족하지 못하면 답변을 차단하거나 강하게 제한한다.

### 채택 이유
- OpenAI는 Conversations/Responses 기준으로 서버 측 대화 상태 관리가 더 편하다. 공식 문서상 Responses API는 stateful하게 문맥을 다룰 수 있고 Conversations API는 durable identifier를 갖는 long-running object를 제공한다.
- 그러나 제품의 기준 데이터는 원래부터 우리 DB여야 한다.
- Gemini는 공식적으로 `google_search` grounding과 `groundingMetadata`를 제공하므로 최신 뉴스/동향이 중요한 투자 상담과 더 잘 맞는다.
- Gemini는 공식적으로 context caching을 제공하므로, 고정 프롬프트와 반복 prefix 비용 최적화가 가능하다.

참고한 공식 문서:
- OpenAI Web Search: https://developers.openai.com/api/docs/guides/tools-web-search
- OpenAI Conversation State: https://developers.openai.com/api/docs/guides/conversation-state
- Gemini Google Search grounding: https://ai.google.dev/gemini-api/docs/google-search
- Gemini Context caching: https://ai.google.dev/gemini-api/docs/caching

검토 기준일:
- 2026-03-27

## 설계 원칙

1. 서비스 스레드 상태와 모델 실행 상태를 분리한다.
2. 서비스의 기준 데이터는 자체 DB와 KIS다.
3. 뉴스와 동향은 모델 내부 기억이 아니라 실시간 웹검색 결과만 사용한다.
4. 평균단가, 보유 수량, 실현 손익 같은 사용자 포지션 데이터는 내부 DB에서만 읽는다.
5. 답변 생성 전에 `freshness gate`를 통과하지 못하면 최신성 판단을 포함한 투자 조언을 제한한다.
6. 모델에는 원문 전체 스레드가 아니라 압축된 메모리와 최신 상태만 보낸다.
7. UI에 노출하는 모든 정량 근거는 `capturedAt/asOf`를 함께 저장하고 내려준다.

## 현재 코드베이스 기준 참고점

현재 상담 저장 로직은 [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt#L104)에 있고, 상담 처리 진입점은 [ConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt#L76)에 있다.

현재 `POST /api/consult`는 `stockName`만 받아 합성 `StockInfo`를 만들고 있어 실시간 KIS 종목 조회가 붙어 있지 않다. [ConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt#L84) [ConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt#L439)

현재 `ComparativeConsultingService`, `AdvancedConsultingService`, `CompareService`는 `stockCode` 기반 `StockService.getStockInfo()`로 KIS 시세를 조회한다. [ComparativeConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ComparativeConsultingService.kt#L37) [AdvancedConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/AdvancedConsultingService.kt#L33)

따라서 프리미엄 연속 상담 설계에서는 `POST /api/consult`도 `stockCode`와 최신 시장 데이터 조회를 강제하는 방향으로 바꿔야 한다.

## MVP 범위

### 포함
1. 유료 사용자만 스레드 생성 및 이어받기
2. 스레드 목록 조회
3. 스레드 종료
4. 기존 상담 이력과 스레드 연결
5. 자체 DB 기반 메시지 저장
6. 요약 메모리 저장
7. KIS 최신 시세 조회
8. Gemini Google Search grounding 기반 최신 뉴스/동향 조회
9. stale data 차단용 freshness 검증

### 제외
1. 사용자 전체 장기 기억 자동 통합
2. 스레드 자동 병합
3. 복잡한 종목 추천 알고리즘
4. 뉴스 전문 저장
5. 멀티 벤더 동시 추론

## 도메인 모델

### 1. ConsultingThread
추천 엔티티명:
`ConsultingThread`

추천 테이블명:
`consulting_threads`

필드 초안:

- `id: Long`
- `user: User`
- `title: String?`
- `status: ConsultingThreadStatus`
- `topicType: ConsultingThreadTopicType`
- `primaryStockCode: String`
- `primaryStockName: String`
- `analysisMode: AnalysisMode?`
- `scenario: ConsultingScenario?`
- `lastConsultedAt: LocalDateTime`
- `lastSummarizedAt: LocalDateTime?`
- `expiresAt: LocalDateTime`
- `closedAt: LocalDateTime?`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`

설명:

- 더 이상 `openAiConversationId`를 스레드의 기준 식별자로 두지 않는다.
- 스레드는 전적으로 서비스 DB에서 관리한다.
- 모델 벤더 전환 시에도 데이터 모델을 유지할 수 있다.

### 2. ConsultingThreadMessage
추천 엔티티명:
`ConsultingThreadMessage`

추천 테이블명:
`consulting_thread_messages`

필드 초안:

- `id: Long`
- `thread: ConsultingThread`
- `role: ConsultingMessageRole`
- `messageText: Text`
- `questionType: String?`
- `createdAt: LocalDateTime`
- `consultingHistoryId: Long?`

설명:

- 사용자 질문과 AI 최종 답변을 모두 저장한다.
- 툴 raw output은 별도 테이블 또는 JSON 컬럼에 저장한다.

### 3. ConsultingThreadMemory
추천 엔티티명:
`ConsultingThreadMemory`

추천 테이블명:
`consulting_thread_memories`

필드 초안:

- `id: Long`
- `thread: ConsultingThread`
- `memoryVersion: Long`
- `summaryText: Text`
- `portfolioStateJson: Text`
- `activeConstraintsJson: Text`
- `generatedAt: LocalDateTime`

설명:

- 최근 원문 대화 외에 모델에 재사용할 압축 메모리다.
- 스레드 전체를 다시 보내지 않기 위한 핵심 구조다.

### 4. ConsultingMarketSnapshot
추천 엔티티명:
`ConsultingMarketSnapshot`

추천 테이블명:
`consulting_market_snapshots`

필드 초안:

- `id: Long`
- `thread: ConsultingThread?`
- `stockCode: String`
- `stockName: String`
- `priceSnapshotJson: Text`
- `newsSnapshotJson: Text`
- `capturedAt: LocalDateTime`
- `marketAsOf: LocalDateTime`
- `newsFreshUntil: LocalDateTime`

설명:

- 답변 생성 시 사용한 최신 시세와 뉴스 근거를 저장한다.
- 회고, 감사 로그, 분쟁 대응에 유용하다.

### 5. PositionSnapshot
기존 가상투자 또는 실제 연동 포지션이 있으면 별도 조회 후 스냅샷으로 포함한다.

필드 초안:

- `userId`
- `stockCode`
- `avgBuyPrice`
- `quantity`
- `unrealizedPnl`
- `capturedAt`

규칙:

- 평균단가와 보유 수량은 내부 DB 기준이어야 한다.
- 모델 메모리에서 과거 수치를 재사용하지 않는다.

## 기존 엔티티 변경

### ConsultingHistory 변경안
기존 `consulting_histories`에 스레드 연결 필드를 추가한다.

추천 필드:

- `thread: ConsultingThread?`
- `marketSnapshotId: Long?`

의미:

- 단발 상담은 `null`
- 연속 상담은 해당 스레드와 당시 사용한 시장 스냅샷을 참조

## 사용자 entitlement 모델

현재 `User` 엔티티에는 유료 사용자 판별 필드가 없다.

MVP에서는 아래 두 가지 중 하나를 권장한다.

### 옵션 A. User에 직접 필드 추가
- `subscriptionTier: SubscriptionTier`

```kotlin
enum class SubscriptionTier {
    FREE,
    PREMIUM
}
```

### 옵션 B. 별도 Subscription 테이블
추천 필드:

- `user_id`
- `plan_code`
- `status`
- `started_at`
- `expires_at`

결론:
실제 결제 연동 예정이면 B가 더 낫다.

## 프롬프트/컨텍스트 구성 원칙

## 전체 시스템 설계도

이번 설계는 크게 `필터링/추출 단계`와 `응답 생성 단계`로 나눈다.

### 단계 1. Router / Intent & Entity Extractor
역할:
모든 투자 상담 질문에 대해 어떤 데이터 소스가 필요한지 결정한다.

입력:
1. 사용자 질문 원문
2. 현재 스레드 메타데이터
3. 사용자 투자 성향 메타데이터
4. 현재 선택 종목 후보값

출력:
정형 JSON 하나만 반환한다.

예시 A:

```json
{
  "is_investment_query": true,
  "requires_market_data": true,
  "requires_position_data": false,
  "requires_web_search": true,
  "symbol": "000660",
  "entity_name": "SK하이닉스",
  "question_type": "price_outlook",
  "needs_freshness_gate": true,
  "reason": "개별 종목의 현재 대응과 최근 이슈 반영이 필요한 질문"
}
```

예시 B:

```json
{
  "is_investment_query": true,
  "requires_market_data": false,
  "requires_position_data": true,
  "requires_web_search": false,
  "symbol": "000660",
  "entity_name": "SK하이닉스",
  "question_type": "position_review",
  "needs_freshness_gate": true,
  "reason": "현재 보유 포지션 기준 판단은 필요하지만 뉴스 검색까지는 필요하지 않은 질문"
}
```

### 단계 2. Branching / Data Fetch
Router 출력값에 따라 필요한 데이터만 조회한다.

분기 규칙:
1. `requires_market_data=true`이면 KIS 최신 시세를 조회한다.
2. `requires_position_data=true`이면 내부 DB에서 최신 평균단가/보유수량을 재조회한다.
3. `requires_web_search=true`이면 Gemini Google Search grounding 또는 별도 검색 수집 단계를 실행한다.
4. `needs_freshness_gate=true`이면 조회 직후 freshness gate를 평가한다.
5. 질문마다 필요한 데이터만 조회하고, 불필요한 호출은 생략한다.

### 단계 3. Final Response Generator
역할:
정제된 최신 데이터와 스레드 메모리를 바탕으로 최종 답변을 생성한다.

입력:
1. 사용자 질문
2. Router 결과 JSON
3. 최신 KIS 데이터
4. 최신 포지션 스냅샷
5. 최신 웹검색 요약과 출처
6. 스레드 요약 메모리
7. 최근 대화 N턴
8. 사주/타로 정보

출력:
사용자 응답 JSON

### 핵심 원칙
1. Router와 Final Generator의 역할을 분리한다.
2. Final Generator는 라우팅 판단을 다시 하지 않는다.
3. Final Generator는 전달받지 않은 데이터를 추측해서 쓰지 않는다.
4. 평균단가, 뉴스, 시세 같은 최신성 데이터는 Branching 단계에서만 주입한다.

## Router 설계 원칙

### 권장 구현 순서
1. 1차는 규칙 기반 분류
2. 애매한 경우에만 소형 LLM Router 호출
3. 최종 응답 생성은 메인 Gemini 호출

이유:
1. 비용 절감
2. 라우팅 안정성 향상
3. 투자 상담 안에서도 불필요한 검색/API 호출 방지

### 규칙 기반 1차 분류 예시
아래 조건이면 KIS 시세 조회를 우선 고려한다.

1. 종목명 또는 종목코드 존재
2. `매수`, `매도`, `홀딩`, `물타기`, `평단`, `수익률`, `손절` 같은 키워드 존재
3. 현재 스레드의 대표 종목과 질문이 연결됨

아래 조건이면 최신 뉴스 검색을 강제한다.

1. `오늘`, `최근`, `이슈`, `왜 떨어져`, `뉴스`, `공시`, `실적`
2. 가격 원인 질문
3. 정책/산업 동향 질문

아래 조건이면 포지션 조회를 강제한다.

1. `내 평단`
2. `내 수익률`
3. `내가 지금 더 사도 돼`
4. `내 기준으로 홀딩할까`

### Router 전용 LLM 사용 시 규칙
1. Router는 짧은 JSON만 반환한다.
2. Router는 최종 답변을 생성하지 않는다.
3. Router는 뉴스 내용을 요약하지 않는다.
4. Router가 종목을 못 잡으면 KIS 호출을 생략하고, 스레드 대표 종목 또는 명시 입력값 기준으로만 진행한다.

### 매 요청에 보내는 정보
1. 시스템 프롬프트
2. 사용자 투자 성향 요약
3. 현재 보유 포지션 최신 스냅샷
4. 스레드 요약 메모리
5. 최근 원문 대화 N턴
6. KIS 최신 시장 스냅샷
7. 최신 웹검색 뉴스 요약과 출처
8. 현재 질문

### 매 요청에 보내지 않는 정보
1. 스레드 전체 원문
2. 오래된 뉴스 본문
3. 오래된 평균단가 문자열
4. 이전 턴의 과거 시장 가격
5. KIS 응답 원문 전체 JSON
6. Router가 처리해야 할 라우팅 고민 전체를 Final Generator에 넘기는 것

### 권장 토큰 예산
1. 시스템 프롬프트: 800~1,500 tokens
2. 투자 성향/포지션 메모리: 200~500 tokens
3. 스레드 요약 메모리: 300~800 tokens
4. 최근 원문 3~6턴: 600~2,000 tokens
5. 시장/뉴스 요약: 500~1,200 tokens

### 요약 메모리 갱신 규칙
1. 매 턴 응답 후 비동기 메모리 갱신
2. 이전 메모리와 최근 2~4턴을 바탕으로 새 메모리 생성
3. 숫자 데이터는 텍스트 메모리에 오래 보존하지 않고 최신 스냅샷으로만 전달
4. 메모리에는 `현재 쟁점`, `이미 안내한 리스크 관리 원칙`, `사용자 선호`만 남긴다

## Freshness Gate

이 문서의 핵심 규칙이다.

### 1. 가격 데이터 freshness
- `KIS marketDataAsOf` 또는 서버 수집 시각이 현재 요청 시각 기준 `5분` 초과면 `stale`
- 장 마감 후에도 `asOf`와 `capturedAt`를 모두 저장
- stale이면 `실시간 시세 기반 단정 표현` 금지

### 2. 뉴스 데이터 freshness
- 뉴스/웹검색 결과는 요청 시각 기준 `24시간` 이내 핵심 근거가 1건 이상 있어야 `fresh`
- 사용자가 `오늘`, `방금`, `최근 뉴스`, `이슈`를 언급했는데 fresh 근거가 없으면 답변 강등 또는 실패 처리
- 검색 결과는 최소 2개 이상의 독립 출처를 우선 사용

### 3. 포지션 데이터 freshness
- 평균단가, 보유 수량, 수익률 계산 기준은 요청 직전 DB 조회값만 사용
- 메시지 텍스트에 있던 이전 평균단가를 재사용하지 않는다
- 포지션 스냅샷 시각이 `1분` 초과 stale이면 다시 조회한다

### 4. stale 차단 응답 정책
아래 경우는 상담을 제한한다.

1. 최신 뉴스가 필요한 질문인데 fresh 검색 근거 없음
2. 보유 포지션 질문인데 최신 포지션 스냅샷 없음
3. 실시간 시세 판단 질문인데 KIS 최신 시세 조회 실패

제한 응답 예시:
- `최신 시세 확인에 실패해 지금 매수/매도 판단을 단정할 수 없습니다. 잠시 후 다시 시도해 주세요.`
- `최근 뉴스 근거를 확보하지 못해 이슈 기반 판단은 보류합니다.`

## 웹검색 정책

### 검색을 반드시 켜야 하는 질문
1. `왜 떨어져?`
2. `최근 이슈 뭐야?`
3. `오늘 뉴스 반영해서 말해줘`
4. `실적 발표/공시/정책 이슈 때문이야?`
5. `하이닉스 지금 홀딩이 맞아?`처럼 최신 이슈 반영이 필요한 종목 질문

### 검색을 선택적으로 켜는 질문
1. 장기 투자 성향 위주 질문
2. 리스크 관리 원칙 질문
3. 사주/타로 중심 해석 질문

### 검색을 끄는 질문
1. 과거 상담 회고만 하는 경우
2. UI/설명형 질문
3. 최신 외부 사실이 중요하지 않은 경우

### Gemini 검색 사용 방식
요청 시 `tools: [{ google_search: {} }]`를 켠다.

운영 규칙:
1. 검색 여부는 서버가 결정한다.
2. 모델이 반환한 `groundingMetadata`를 저장한다.
3. 응답 본문에는 출처 링크를 표시한다.
4. `groundingMetadata`가 없으면 최신 뉴스 기반 답변으로 간주하지 않는다.

## KIS 데이터 정책

### 투자 상담에 항상 포함할 필드
- `currentPrice`
- `changeRate`
- `sector`
- `marketDataAsOf`
- `tradingSnapshot.openPrice`
- `tradingSnapshot.highPrice`
- `tradingSnapshot.lowPrice`
- `tradingSnapshot.volume`
- `fundamentals.marketCap`
- `fundamentals.trailingPe`
- `fundamentals.priceToBook`
- `fundamentals.eps`
- `fundamentals.bps`

### 선택 포함 필드
- 업종 지수
- 상승/하락 상위 랭킹
- 거래량 상위
- 시장 전체 위험 선호/회피 요약

### 중요 규칙
1. 개별 종목의 정량 데이터는 뉴스 검색 결과보다 KIS를 우선한다.
2. 뉴스는 가격 원인 추정 보조 근거로만 쓴다.
3. KIS 실패 시 fallback=true인 합성 데이터로 투자 단정 결론을 내리지 않는다.

## API 설계 초안

### 1. 최근 스레드 목록 조회
`GET /api/consulting/threads`

응답 예시:

```json
{
  "items": [
    {
      "threadId": 55,
      "title": "하이닉스 분할매수 상담",
      "status": "OPEN",
      "primaryStockCode": "000660",
      "primaryStockName": "SK하이닉스",
      "analysisMode": "STOCK_ALL",
      "scenario": "RESCUE_PLAN",
      "lastQuestionSummary": "하이닉스 주가가 떨어지는데 홀딩이 맞을까?",
      "lastAiSummary": "최신 뉴스와 시세 기준으로 비중 확대보다 보유 비중 관리가 우선입니다.",
      "lastConsultedAt": "2026-03-27T10:12:00",
      "expiresAt": "2026-03-28T10:12:00",
      "continueAllowed": true
    }
  ]
}
```

### 2. 스레드 생성
`POST /api/consulting/threads`

요청 예시:

```json
{
  "primaryStockCode": "000660",
  "primaryStockName": "SK하이닉스",
  "analysisMode": "STOCK_ALL",
  "scenario": "RESCUE_PLAN",
  "title": "하이닉스 손실구간 대응 상담"
}
```

응답 예시:

```json
{
  "threadId": 55,
  "status": "OPEN",
  "expiresAt": "2026-03-28T10:12:00"
}
```

### 3. 상담 요청
기존 상담 API를 확장한다.

`POST /api/consult`

요청 필드 추가:

- `threadId: Long?`
- `continueThread: Boolean = false`
- `stockCode: String`
- `requireFreshNews: Boolean?`
- `requireFreshPosition: Boolean?`

요청 예시:

```json
{
  "userId": 12,
  "mode": "STOCK_ALL",
  "scenario": "RESCUE_PLAN",
  "stockCode": "000660",
  "stockName": "SK하이닉스",
  "question": "하이닉스 주가가 떨어지는데 홀딩, 매수, 매도 중 뭐가 나아?",
  "threadId": 55,
  "continueThread": true,
  "requireFreshNews": true,
  "requireFreshPosition": true
}
```

동작 규칙:
1. `stockCode`는 프리미엄 연속 상담에서 필수다.
2. 현재 포지션 기반 질문이면 포지션 스냅샷을 다시 읽는다.
3. 뉴스 필요 질문이면 검색 grounding을 켠다.
4. fresh 검증 실패 시 제한 응답 또는 409/422 계열 오류를 반환한다.

응답 확장 예시:

```json
{
  "mode": "STOCK_ALL",
  "history": {
    "id": 302,
    "shareKey": "..."
  },
  "thread": {
    "threadId": 55,
    "status": "OPEN",
    "continueAllowed": true
  },
  "marketEvidence": {
    "marketAsOf": "2026-03-27T10:11:20+09:00",
    "positionAsOf": "2026-03-27T10:11:18+09:00",
    "newsAsOf": "2026-03-27T10:11:30+09:00",
    "newsFresh": true,
    "priceFresh": true,
    "positionFresh": true,
    "citations": [
      {
        "title": "Example source",
        "url": "https://example.com/news/1"
      }
    ]
  }
}
```

### 4. 스레드 종료
`POST /api/consulting/threads/{threadId}/close`

### 5. 스레드 메시지 조회
`GET /api/consulting/threads/{threadId}/messages`

목적:
- 프론트 렌더링용
- 디버깅용
- 메모리 재생성용

## 서비스 처리 플로우

### 프리미엄 연속 상담 요청 처리 순서
1. 사용자 entitlement 검증
2. threadId 검증
3. 규칙 기반 1차 분류
4. 필요 시 Router LLM 호출
5. Router 결과 JSON 확정
6. 종목 코드 검증
7. 최신 포지션 스냅샷 조회
8. KIS 최신 시세 조회
9. 질문 분류 결과에 따라 Gemini Google Search grounding 실행
10. freshness gate 평가
11. 스레드 메모리 로드
12. 최근 원문 메시지 로드
13. Final Generator용 prompt payload 구성
14. Gemini 호출
15. grounding metadata, market snapshot, answer 저장
16. 비동기 메모리 요약 갱신

### 서버 측 의사코드 예시

```text
user question
 -> rule classifier
 -> if ambiguous then router llm
 -> routing json
 -> branch:
    - market data?
    - position data?
    - web search?
 -> freshness gate
 -> final generator llm
 -> save answer + evidence + memory
```

## Gemini Context Caching 적용 포인트

### 캐시 대상
1. 고정 시스템 프롬프트
2. 사주/타로 해석 규칙
3. 응답 JSON schema 안내

### 캐시 비대상
1. 최신 시세
2. 최신 뉴스
3. 포지션 스냅샷
4. 최근 대화

이유:
- 최신성이 중요한 데이터는 캐시 대상이 되면 안 된다.
- cache hit를 노리기 위해 stale 입력을 재사용하면 안 된다.

## 실패 처리 정책

### KIS 실패
- fresh price가 필요한 질문이면 제한 응답
- 일반 조언만 가능한 경우에는 `시장 데이터 확인 실패`를 명시

### 뉴스 grounding 실패
- 최신 이슈 판단 요청이면 제한 응답
- 일반 리스크 관리 원칙만 제공 가능한 경우 그 범위로 강등

### 포지션 조회 실패
- 평균단가/수익률 질문이면 실패 처리
- 포지션 비의존 질문이면 진행 가능

## 향후 확장 방향

1. 뉴스 신뢰도 scoring
2. 공시/실적 캘린더 전용 소스 추가
3. 포트폴리오 단위 메모리
4. 종목별 대표 스레드 추천
5. stale 응답 자동 감사 리포트

## 최종 권장안 요약

유료 기능은 `Gemini 기반 연속 상담 + 자체 스레드/메모리 저장 + 최신 근거 강제`로 설계한다.

- 모델: Gemini
- 스레드 기준: 자체 DB
- 대화 압축: 메모리 요약 + 최근 N턴
- 가격 데이터 기준: KIS 최신 조회
- 포지션 데이터 기준: 내부 DB 최신 조회
- 뉴스 기준: Gemini Google Search grounding
- stale data 정책: 조건 미충족 시 답변 제한
