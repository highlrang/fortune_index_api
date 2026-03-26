# Premium Consulting Backend Spec Draft

## 목적
유료 사용자 전용 `연속 상담 스레드` 기능의 Spring Boot 기준 백엔드 설계 초안을 정리한다.

이 문서는 MVP 범위의 엔티티, DTO, API, 서비스 규칙을 정의한다.

## 설계 원칙

1. 서비스 스레드 상태와 OpenAI conversation 상태를 분리한다.
2. 서비스의 기준 데이터는 자체 DB다.
3. OpenAI conversation은 모델 문맥 유지를 위한 실행 상태 저장소로 사용한다.
4. 기존 `consulting_histories`는 그대로 유지하고, 스레드와 연결만 추가한다.
5. 기본 상담은 새 상담이며, 스레드 연결은 명시적 선택일 때만 허용한다.

## 현재 코드베이스 기준 참고점

현재 상담 저장 로직은 [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt#L104)에 있고, 상담 처리 진입점은 [ConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt#L76)에 있다.

현재 `User`에는 유료 구독 필드가 없으므로 [User.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/domain/model/User.kt#L17), entitlement 또는 subscription 모델이 추가로 필요하다.

## MVP 범위

### 포함
1. 유료 사용자만 스레드 생성 및 이어받기
2. 스레드 목록 조회
3. 스레드 종료
4. 기존 상담 이력과 스레드 연결
5. OpenAI conversation ID 저장

### 제외
1. 사용자 전체 장기 기억
2. 스레드 자동 병합
3. 스레드 요약 압축 자동화
4. 복잡한 추천 알고리즘

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
- `primaryStockCode: String?`
- `primaryStockName: String?`
- `analysisMode: AnalysisMode?`
- `scenario: ConsultingScenario?`
- `openAiConversationId: String`
- `lastResponseId: String?`
- `lastConsultedAt: LocalDateTime`
- `expiresAt: LocalDateTime`
- `closedAt: LocalDateTime?`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`

설명:

- `openAiConversationId`는 OpenAI Conversations API의 `conv_*` 식별자 저장 용도
- `lastResponseId`는 필요 시 마지막 response 참조용
- `primaryStockCode`, `primaryStockName`은 프론트 목록/추천용
- `analysisMode`, `scenario`는 스레드 대표 성격 판단에 도움

### 2. ConsultingThreadStatus
추천 enum:

```kotlin
enum class ConsultingThreadStatus {
    OPEN,
    CLOSED,
    EXPIRED
}
```

### 3. ConsultingThreadTopicType
MVP에서는 단순하게 시작한다.

```kotlin
enum class ConsultingThreadTopicType {
    STOCK
}
```

향후 확장 시 `PORTFOLIO`, `RETRO`, `GENERAL` 등을 추가할 수 있다.

## 기존 엔티티 변경

### ConsultingHistory 변경안
기존 `consulting_histories`에 스레드 연결 필드를 추가한다.

추천 필드:

- `thread: ConsultingThread?`

의미:

- 단발 상담은 `null`
- 연속 상담으로 이어진 경우 해당 스레드 참조

JPA 예시:

```kotlin
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "thread_id")
var thread: ConsultingThread? = null
```

## 사용자 entitlement 모델

현재 `User` 엔티티에는 유료 사용자 판별 필드가 없다.

MVP에서는 아래 두 가지 중 하나를 권장한다.

### 옵션 A. User에 직접 필드 추가
빠른 MVP용

- `subscriptionTier: SubscriptionTier`

```kotlin
enum class SubscriptionTier {
    FREE,
    PREMIUM
}
```

장점:

- 구현 빠름

단점:

- 향후 결제 상태 확장에 약함

### 옵션 B. 별도 Subscription 테이블
확장성 우선

추천 필드:

- `user_id`
- `plan_code`
- `status`
- `started_at`
- `expires_at`

결론:
MVP면 A도 충분하지만, 실제 결제 연동 예정이면 B가 더 낫다.

## API 설계 초안

### 1. 최근 스레드 목록 조회
`GET /api/consulting/threads`

설명:
현재 사용자의 최근 스레드 목록을 조회한다.

쿼리 파라미터 예시:

- `status=open`
- `limit=10`

응답 예시:

```json
{
  "items": [
    {
      "threadId": 55,
      "title": "삼성전자 분할매수 상담",
      "status": "OPEN",
      "primaryStockCode": "005930",
      "primaryStockName": "삼성전자",
      "analysisMode": "STOCK_ALL",
      "scenario": "BUY_TIMING",
      "lastQuestionSummary": "지금 분할매수 계속해도 될까?",
      "lastAiSummary": "추격 매수보다 분할 접근이 유리합니다.",
      "lastConsultedAt": "2026-03-26T11:10:00",
      "expiresAt": "2026-03-27T11:10:00",
      "continueAllowed": true
    }
  ]
}
```

### 2. 스레드 생성
`POST /api/consulting/threads`

설명:
유료 사용자가 연속 상담용 스레드를 새로 만든다.

요청 예시:

```json
{
  "primaryStockCode": "005930",
  "primaryStockName": "삼성전자",
  "analysisMode": "STOCK_ALL",
  "scenario": "BUY_TIMING",
  "title": "삼성전자 분할매수 상담"
}
```

응답 예시:

```json
{
  "threadId": 55,
  "status": "OPEN",
  "openAiConversationId": "conv_123",
  "expiresAt": "2026-03-27T11:10:00"
}
```

### 3. 상담 요청
기존 상담 API를 확장하는 방식을 권장한다.

`POST /api/consulting`

요청 필드 추가:

- `threadId: Long?`
- `continueThread: Boolean = false`

요청 예시:

```json
{
  "userId": 12,
  "mode": "STOCK_ALL",
  "stockName": "삼성전자",
  "question": "어제 조언대로 분할매수 이어가도 될까?",
  "threadId": 55,
  "continueThread": true
}
```

동작 규칙:

1. `continueThread=false` 또는 `threadId` 없음
   - 기존 단발 상담처럼 처리
2. `continueThread=true` and `threadId` 존재
   - 스레드 유효성 검증 후 연속 상담 처리

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
    "continued": true,
    "expiresAt": "2026-03-27T11:40:00"
  }
}
```

### 4. 스레드 종료
`POST /api/consulting/threads/{threadId}/close`

설명:
사용자가 더 이상 이어받지 않겠다고 명시적으로 종료한다.

응답 예시:

```json
{
  "threadId": 55,
  "status": "CLOSED",
  "closedAt": "2026-03-26T12:00:00"
}
```

### 5. 스레드 상세 조회
선택 사항이지만 프론트 편의를 위해 권장한다.

`GET /api/consulting/threads/{threadId}`

포함 정보:

- 스레드 메타데이터
- 최근 상담 이력 일부
- 이어받기 가능 여부

## DTO 초안

### ThreadSummaryResponse
```kotlin
data class ThreadSummaryResponse(
    val threadId: Long,
    val title: String?,
    val status: ConsultingThreadStatus,
    val primaryStockCode: String?,
    val primaryStockName: String?,
    val analysisMode: AnalysisMode?,
    val scenario: ConsultingScenario?,
    val lastQuestionSummary: String?,
    val lastAiSummary: String?,
    val lastConsultedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val continueAllowed: Boolean
)
```

### CreateThreadRequest
```kotlin
data class CreateThreadRequest(
    val primaryStockCode: String?,
    val primaryStockName: String?,
    val analysisMode: AnalysisMode?,
    val scenario: ConsultingScenario?,
    val title: String? = null
)
```

### CreateThreadResponse
```kotlin
data class CreateThreadResponse(
    val threadId: Long,
    val status: ConsultingThreadStatus,
    val openAiConversationId: String,
    val expiresAt: LocalDateTime
)
```

### ConsultRequest 확장안
기존 `ConsultRequest`에 아래 필드를 추가한다.

```kotlin
val threadId: Long? = null
val continueThread: Boolean = false
```

### ConsultThreadInfoResponse
```kotlin
data class ConsultThreadInfoResponse(
    val threadId: Long,
    val status: ConsultingThreadStatus,
    val continued: Boolean,
    val expiresAt: LocalDateTime
)
```

## 서비스 계층 초안

### 1. ConsultingThreadService
추천 책임:

1. 스레드 생성
2. 스레드 조회
3. 스레드 종료
4. 이어받기 가능 여부 검증
5. 만료 처리

추천 메서드 예시:

```kotlin
fun createThread(userId: Long, request: CreateThreadRequest): CreateThreadResponse
fun getRecentThreads(userId: Long, onlyOpen: Boolean = false): List<ThreadSummaryResponse>
fun validateContinuableThread(userId: Long, threadId: Long): ConsultingThread
fun touchThread(thread: ConsultingThread, consultedAt: LocalDateTime): ConsultingThread
fun closeThread(userId: Long, threadId: Long): CloseThreadResponse
```

### 2. ConsultingService 변경 포인트
현재 [ConsultingService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt#L76)에서 상담 전체를 처리하므로, 아래 분기 추가가 필요하다.

#### 단발 상담
기존 로직 유지

#### 연속 상담
추가 흐름:

1. 유료 entitlement 확인
2. `threadId` 유효성 검증
3. thread의 `openAiConversationId` 로 OpenAI Responses 호출
4. 응답 후 `lastResponseId`, `lastConsultedAt`, `expiresAt` 갱신
5. 저장하는 `ConsultingHistory`에 `thread` 연결

## OpenAI 연동 권장 방식

### 원칙
1. OpenAI conversation ID는 DB에 저장
2. 새 스레드 생성 시 conversation 생성
3. 연속 상담 시 `conversation` 파라미터로 호출
4. 필요 시 `lastResponseId`도 저장하지만, 서비스 기준은 conversation 중심

### 이유
`previous_response_id` 체인만으로 운영하는 것보다 conversation 기반이 장기 스레드에 더 적합하다.

## 검증 규칙

### 이어받기 요청 검증
1. 유료 사용자여야 함
2. 스레드 소유자가 현재 사용자여야 함
3. 스레드 상태가 `OPEN`
4. `expiresAt`이 현재보다 이후여야 함

검증 실패 시 추천 에러:

- `403 FORBIDDEN`: 유료 권한 없음
- `404 NOT FOUND`: 본인 스레드 아님 또는 없음
- `409 CONFLICT`: 스레드 상태상 이어받기 불가
- `410 GONE`: 만료된 스레드

## 상태 전이

### OPEN
정상 이어받기 가능 상태

전이 가능:

- `CLOSED`
- `EXPIRED`

### CLOSED
사용자 종료 상태

전이 가능:

- 없음

### EXPIRED
시간 만료 상태

전이 가능:

- 없음

## 만료 정책

### 권장 기준
- 마지막 활동 후 24시간까지 이어받기 가능
- 마지막 활동 후 7일 경과 시 만료 스캔 또는 조회 시 `EXPIRED` 반영

구현 선택지:

### 옵션 A. 조회 시 계산
장점:

- 구현 간단

단점:

- 상태값과 실제 계산이 분리될 수 있음

### 옵션 B. 스케줄러 만료 처리
장점:

- 상태값 일관성 좋음

단점:

- 스케줄링 추가 필요

MVP 권장:
조회/검증 시 계산 + 필요 시 상태 업데이트

## 저장 전략

### 반드시 자체 DB에 저장할 것
1. 스레드 메타데이터
2. 상담 결과 요약
3. 질문 원문
4. AI 응답 요약 또는 원문
5. OpenAI conversation ID

이유:

- 운영 추적성 확보
- 사용자 이력 화면 제공
- OpenAI 상태만 믿지 않기 위함

## 마이그레이션 초안

### 신규 테이블
- `consulting_threads`

### 기존 테이블 변경
- `consulting_histories.thread_id` nullable 추가

### 유료 사용자 판별용 변경
아래 중 하나:

- `users.subscription_tier` 추가
- `subscriptions` 테이블 추가

## 구현 우선순위

### 1차
1. entitlement 모델 추가
2. `consulting_threads` 엔티티/리포지토리 추가
3. `consulting_histories.thread_id` 추가
4. 스레드 생성/조회/종료 API 추가

### 2차
1. `ConsultRequest` 확장
2. `ConsultingService`에서 연속 상담 분기 추가
3. OpenAI conversation 연동

### 3차
1. 만료 처리 정교화
2. 추천 이어받기 로직
3. 요약 압축

## 최종 권장안 요약
백엔드는 `서비스 스레드`를 중심으로 설계하고, OpenAI conversation은 그 스레드에 연결된 실행 문맥 저장소로 사용한다.

즉,

1. 서비스 기준은 자체 DB
2. 문맥 유지 범위는 스레드 내부
3. 유료 사용자만 이어받기 허용
4. 기존 상담 이력은 유지하면서 thread 연결만 추가
