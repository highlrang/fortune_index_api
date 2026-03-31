# 상담 시장 분위기 데이터 설계

## 목적

재물 운세 및 투자 심리 케어 상담에서 최신 시장 분위기가 필요할 때, 개별 종목 KIS 실시간 시세를 기본값으로 쓰지 않고 섹터, 지수, ETF 중심의 현상 지표를 조회하도록 구조를 재설계한다.

핵심 목표는 아래와 같다.

- 고정 데이터는 DB에 저장해 반복 조회를 줄인다.
- 실시간 데이터는 사용자 질문에 따라 필요한 경우에만 KIS API를 호출한다.
- 상담 기본 경로는 섹터, 지수, ETF 기반의 시장 분위기 해석으로 통일한다.
- 개별 종목 KIS 시세 조회는 예외 시나리오에서만 허용한다.
- KIS 장애가 발생해도 상담 응답 자체는 계속 생성되도록 degrade 가능하게 설계한다.

## 문제 정의

기존 구조는 `stockCode`가 있고 최신성이 필요한 질문으로 분류되면 개별 종목 KIS 조회를 우선 시도한다.

이 방식의 문제는 아래와 같다.

- 서비스 철학과 구현이 어긋난다.
  현재 프롬프트는 KIS 숫자를 투자 추천 근거가 아니라 현상 지표라고 정의하지만, 구현은 개별 종목 시세에 의존한다.
- 요청 수가 많아진다.
  홈과 상담이 모두 개별 종목 기준으로 KIS를 여러 번 호출하면 API 안정성이 떨어진다.
- 장애 전파 범위가 넓다.
  개별 종목 조회 실패가 상담의 전체 품질과 안정성을 흔들 수 있다.
- 상담의 본질과 맞지 않는다.
  재물 운세 및 심리 케어는 정밀 체결 보조보다 오늘의 공기와 군중 심리를 읽는 쪽에 더 가깝다.

## 설계 원칙

### 1. 고정 데이터와 실시간 데이터를 분리한다

고정 데이터는 DB에 저장한다.

- 종목명, 종목코드
- 시장 구분
- 대표 섹터
- theme 코드
- 대표 ETF, 대표 지수, 대표 업종 매핑
- 사용자 표현용 별칭

실시간 데이터는 KIS에서 필요 시점에만 조회한다.

- 업종 지수
- 대표 지수
- 대표 ETF
- 필요 시 예외적으로 개별 종목 현재가

### 2. 기본 상담은 시장 분위기 지표 중심으로 처리한다

상담에서 시장 데이터가 필요하더라도 기본값은 아래 순서를 따른다.

1. 섹터 snapshot
2. 대표 지수 snapshot
3. 대표 ETF snapshot
4. 필요 시 웹검색
5. 정말 예외일 때만 개별 종목 quote

### 3. 개별 종목 KIS는 예외 경로로만 유지한다

개별 종목 최신 시세가 의미 있는 경우에만 KIS 종목 시세를 허용한다.

예외 후보 시나리오:

- `TIMING_ENTRY`
- `TIMING_EXIT`
- `RESCUE_PLAN`
- 보유 평단, 손실 압박, 포지션 감정 해석이 직접 필요한 질문

그 외 일반 재물운, 멘탈 케어, 사주 궁합형 질문은 개별 종목 시세 없이도 처리한다.

## 데이터 모델 설계

### `market_theme`

상담에서 사용하는 시장 해석 단위를 저장한다.

- `id`
- `code`
- `name_ko`
- `market_type` (`KR`, `US`)
- `description`

예시:

- `KR_SEMI`
- `KR_SECONDARY_BATTERY`
- `US_BIG_TECH`
- `US_SEMICONDUCTOR`

### `stock_master`

종목과 theme를 연결한다.

- `ticker`
- `name`
- `market`
- `sector_code`
- `theme_id`

예시:

- `005930` -> 반도체
- `NVDA` -> 미국 반도체

### `market_theme_mapping`

theme별 대표 참조 대상을 저장한다.

- `theme_id`
- `reference_type` (`SECTOR`, `INDEX`, `ETF`)
- `reference_code`
- `reference_name`
- `priority`

예시:

- 미국 반도체 -> `ETF`, `SOXX`
- 미국 반도체 -> `INDEX`, `NASDAQ`
- 국내 반도체 -> `SECTOR`, `KR_SEMI_INDEX`

### `market_reference`

KIS 호출 메타와 파라미터 템플릿을 저장한다.

- `reference_code`
- `reference_type`
- `market`
- `kis_api_type`
- `kis_param_json`
- `active`

예시:

- `KR_SEMI_INDEX` -> 국내업종 현재지수 호출 파라미터
- `NASDAQ` -> 해외지수분봉조회 호출 파라미터
- `SOXX` -> ETF 또는 해외 복수시세 조회용 파라미터

### `market_alias`

사용자 질문의 다양한 표현을 theme로 정규화한다.

- `alias`
- `theme_id`
- `weight`

예시:

- `반도체`
- `AI반도체`
- `칩`
- `미국기술주`

## 라우팅 설계

기존의 `requiresMarketData` 하나로는 목적이 섞인다. 따라서 아래처럼 분리한다.

- `requiresMarketMoodData`
- `requiresSectorSnapshot`
- `requiresIndexSnapshot`
- `requiresEtfSnapshot`
- `requiresSymbolQuote`
- `requiresPositionData`
- `requiresWebSearch`

### 라우팅 기준

#### 시장 분위기 지표

아래 질문은 개별 종목이 아니라 시장 분위기 지표로 해석한다.

- 오늘 흐름
- 최근 분위기
- 업종 기류
- 시장 공기
- 최근 불안감
- 섹터가 열리는지 닫히는지

이 경우:

- `requiresMarketMoodData=true`
- 필요 시 `requiresSectorSnapshot=true`
- 필요 시 `requiresIndexSnapshot=true`
- 필요 시 `requiresEtfSnapshot=true`
- `requiresSymbolQuote=false`

#### 개별 종목 최신 시세

아래 질문은 예외적으로 개별 종목 quote가 필요할 수 있다.

- 지금 진입해도 되는가
- 지금 정리할까
- 구조대가 필요한가
- 내 평단 기준으로 압박이 어떤가

이 경우:

- `requiresSymbolQuote=true`

### 질문 예시

`엔비디아 지금 들어가도 될까?`

- 종목은 NVDA로 식별
- theme는 미국 반도체
- 기본은 미국 반도체 업종 분위기 + 나스닥 + 대표 ETF
- 시나리오가 `TIMING_ENTRY`이면 개별 종목 quote 허용

`2차전지 오늘 분위기 어때?`

- theme는 국내 2차전지
- 업종 지표 + 대표 ETF 위주
- 개별 종목 quote 없음

`미국 기술주 요즘 왜 약해?`

- 미국 기술주 theme
- 해외 지수 + 대표 ETF
- 최신 이슈 해석 필요 시 웹검색

## 서비스 계층 구조

상담 서비스가 KIS 엔드포인트 상세를 직접 알지 않도록 아래 계층으로 분리한다.

### `MarketThemeResolver`

입력:

- 사용자 질문
- stockCode
- stockName

출력:

- theme
- market
- 대표 참조 대상 목록

역할:

- 종목 또는 사용자 표현을 theme로 정규화
- 대표 ETF, 지수, 업종 매핑 결정

### `MarketMoodQueryPlanner`

입력:

- 라우팅 결과
- theme
- 대표 참조 대상 목록

출력:

- 호출할 KIS API 목록
- 호출 우선순위
- degrade 순서

역할:

- 질문별로 최소 호출 계획 생성

### `KisMarketMoodClient`

역할:

- 업종, 지수, ETF 관련 KIS 호출 담당
- 개별 종목용 KIS 클라이언트와 책임 분리

### `MarketMoodAssembler`

입력:

- 업종 응답
- 지수 응답
- ETF 응답

출력:

- 표준화된 `MarketMoodSnapshot`
- 표준화된 `MarketMoodBundle`

역할:

- KIS 원본 응답을 LLM 친화적 구조로 변환

### `ConsultingService`

역할:

- planner가 결정한 요약 결과만 받아 payload 생성
- 직접 KIS 세부 파라미터를 알지 않음

## 사용자 선호 섹터 반영 상태

현재 백엔드 사용자 모델에는 `preferredSectors`가 존재한다.

- 회원가입과 내 정보 수정에서 저장 가능
- 현재 사용자 조회 응답에도 포함 가능

하지만 현재 상담 생성 경로에서는 아직 사용하지 않는다.

- `ConsultingService.buildPayload()`의 `user` 정보에는 `investmentRiskProfile`만 포함된다.
- 상담의 `focusArea`는 사용자 `preferredSectors`가 아니라 요청으로 들어온 시장 컨텍스트 또는 종목의 `sector`를 사용한다.
- 시스템 프롬프트에도 사용자 선호 섹터 우선 해석 규칙은 아직 없다.

즉 현재 상태는 `preferredSectors`를 프로필 저장 용도로만 유지하고, 상담 시장 분위기 해석에는 연결하지 않은 상태다.

향후 반영한다면 아래 순서가 자연스럽다.

1. `ConsultingService.buildPayload()`의 `user`에 `preferredSectors` 추가
2. `preferredSectors`가 존재하면 시장 분위기 해석의 우선 참조 섹터 후보로 사용
3. 사용자 선호 섹터와 현재 종목/질문 섹터가 다를 때 우선순위 규칙 정의
4. 프롬프트에 `사용자 관심 섹터를 우선 해석하되, 질문 맥락과 충돌하면 현재 요청 섹터를 우선한다`는 규칙 추가

## 표준 내부 모델

KIS raw 응답을 그대로 프롬프트에 넣지 않고 내부 표준 모델로 변환한다.

### `MarketMoodSnapshot`

- `asOf`
- `market`
- `theme`
- `referenceType`
- `referenceName`
- `changeRate`
- `trend`
- `volatility`
- `reliability`

### `MarketMoodBundle`

- `sectorSnapshot`
- `indexSnapshot`
- `etfSnapshot`
- `citations`
- `staleFlags`

장점:

- KIS API 세부 응답 포맷 변경이 LLM 입력 계약에 직접 영향을 주지 않는다.
- fallback과 degrade를 한 곳에서 통제할 수 있다.

## KIS API 활용 방향

KIS 공식 문서 기준으로 상담용 흐름 지표로 활용 가능한 범위는 아래와 같다.

국내:

- `국내업종 현재지수`
- `국내업종 일자별지수`
- `국내업종 시간별지수(초/분)`
- `업종 분봉조회`
- `국내주식업종기간별시세`
- `ETF/ETN 현재가`
- `ETF 구성종목시세`
- `국내지수 실시간체결`

해외:

- `해외지수분봉조회`
- `해외주식 업종별시세`
- `해외주식 업종별코드조회`
- `해외주식 종목/지수/환율기간별시세`
- `해외주식 복수종목 시세조회`

권장 순서:

- 국내는 업종 지수와 ETF를 먼저 연결
- 해외는 지수와 업종별 시세를 먼저 연결
- 개별 종목 quote는 마지막까지 축소 유지

## fallback 및 degrade 전략

### 1. theme 매핑 실패

- broad market index만 사용
- 예: 국내는 KOSPI, 미국은 NASDAQ

### 2. 섹터 API 실패

- ETF로 대체
- ETF도 실패하면 index로 대체

### 3. 지수 API 실패

- 남은 ETF 또는 업종 시세만으로 축약 해석

### 4. 모든 시장 지표 실패

- `fallback=true`
- `staleReasons`에 사유 기록
- 일반 운세 및 심리 케어 답변으로 하향

### 5. 개별 종목 quote 실패

- 상담 자체는 중단하지 않음
- 섹터, 지수, ETF 기준 시장 분위기 상담으로 자동 하향

## 구현 우선순위

### 1단계

- DB에 theme와 reference mapping 테이블 추가
- `MarketThemeResolver` 도입
- 라우팅을 market mood 중심으로 정리

### 2단계

- 국내 업종, 국내 지수, ETF KIS 연동
- `MarketMoodSnapshot` 표준 모델 도입

### 3단계

- 해외 지수, 해외 업종 KIS 연동
- theme별 해외 대표 지표 매핑 확장

### 4단계

- 개별 종목 quote를 예외 경로로 축소
- 현재 상담 흐름에서 기본 경로를 시장 분위기 기반으로 전환

### 5단계

- 장애 시 fallback 및 stale evidence 고도화
- 캐시 전략 도입

## 기대 효과

- 상담당 KIS 호출 수가 줄어든다.
- KIS 장애의 전파 범위가 작아진다.
- 서비스 철학과 구현이 정렬된다.
- 프롬프트가 개별 종목 숫자에 과몰입하지 않고 시장 분위기 해석에 집중한다.
- 홈, 상담, 스케줄러 간 API 사용량 충돌 가능성이 줄어든다.

## 오픈 이슈

- theme 매핑을 운영자가 수동 관리할지, 초기 자동 분류를 둘지 결정 필요
- 국내, 해외 각각 어떤 대표 ETF를 표준 참조로 둘지 정의 필요
- 웹검색과 시장 지표의 우선순위를 질문 유형별로 더 세분화할지 결정 필요
- 캐시 TTL을 몇 분으로 둘지 합의 필요
- 개별 종목 예외 시나리오를 더 줄일지 유지할지 제품 관점 판단 필요
