# 사주 상담 개선 작업 정리

## 목적

프로필 화면이 아니라 `사주 기반 투자 상담 품질 개선`에만 초점을 맞춘 변경 사항을 정리한다.

이번 변경의 목표는 다음과 같다.

- 사주 원국과 현재 흐름을 투자 상담용 feature로 정규화한다.
- 이 feature를 프론트에 직접 노출하지 않고 상담 payload, 프롬프트, 리스크 점수에만 간접 활용한다.
- 기존 사주 계산 엔진은 유지하고, 상담 해석용 로직만 별도 파일로 분리해 롤백을 쉽게 만든다.

## 적용 범위

이번 작업은 아래 범위에만 적용됐다.

- `생지/왕지/묘지` 카운트 기반 성향 분류
- `지장간` 비율 기반 내부 오행 성향 분류
- `합/충` 기반 시너지/변동성 신호 생성
- 상담 payload 내 `saju.investmentFeatures` 주입
- 프롬프트에 사주 feature를 보조 신호로만 사용하도록 가이드 추가
- 리스크 점수 계산 시 일부 feature 가중치 반영

다음 항목은 이번 문서 범위에서 제외한다.

- 프로필 상세 API 변경
- astrology/transits 처리
- 스케줄 모니터링

## 설계 원칙

### 1. 직접 추천 규칙이 아니라 보조 해석 신호로 사용

사주 feature는 종목 추천이나 매수/매도 신호로 직접 연결하지 않는다.

대신 아래 용도로만 사용한다.

- 투자 성향 설명
- 심리 상태 해석
- 변동성 경고
- 리밸런싱 필요성 안내
- AI 답변 톤 보정

### 2. 기존 사주 계산 엔진과 분리

기존 `SajuAnalyzer`는 원본 계산 엔진으로 유지한다.

투자 상담용 해석은 별도 패키지로 분리했다.

- [SajuInvestmentFeatures.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/saju/investment/SajuInvestmentFeatures.kt)
- [SajuInvestmentFeatureService.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/saju/investment/SajuInvestmentFeatureService.kt)

이렇게 분리한 이유는 다음과 같다.

- 롤백이 쉽다.
- 기존 사주 로직에 영향 범위를 줄인다.
- 추후 feature 확장 시 상담 전용 로직만 수정하면 된다.

### 3. 프론트 직접 노출 금지

`investmentFeatures`는 프론트에 직접 보여주기 위한 데이터가 아니다.

현재 의도는 다음과 같다.

- 백엔드 내부에서 AI 상담 입력으로 사용
- 리스크 점수 보정에 사용
- 필요 시 향후 내부 로그/분석용으로 활용

## 구현 구조

### 새로 추가된 전용 파일

#### `SajuInvestmentFeatures.kt`

상담용 사주 feature 모델을 정의한다.

- `baseTraits`
- `dynamicSignals`
- `riskFlags`
- `hiddenElementRatios`
- `branchStageCounts`
- `relationSignals`
- `confidence`

#### `SajuInvestmentFeatureService.kt`

사주 계산 결과를 투자 상담용 feature로 변환한다.

현재 포함된 규칙은 다음과 같다.

- 원국 지지 4개에서 `생지/왕지/묘지` 개수 계산
- 지장간 가중치 합산 후 오행 비율 정규화
- 일주 기준 `대운/세운`과의 `합` 판정
- 일지 기준 `대운/세운`과의 `충` 판정
- 위 결과를 `baseTraits`, `dynamicSignals`, `riskFlags`로 매핑

## 현재 연결 지점

### 1. 상담 준비 단계

[ConsultingService.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt) 에서 사주 모드일 때 아래 순서로 연결된다.

1. 기존 `SajuAnalyzer`로 `SajuConsultingResult` 생성
2. `SajuInvestmentFeatureService.extract()` 호출
3. 결과를 `PreparedConsultation.sajuFeatures`에 저장

### 2. payload 주입

동일 파일에서 AI payload 생성 시 아래 위치에 추가된다.

- `saju.investmentFeatures`

즉 AI는 기존 `palza`, `majorFortune`, `yearlyFortune` 외에 투자 상담용 사주 feature도 함께 받는다.

### 3. 프롬프트 가이드

동일 파일에서 시스템 메시지 생성 시 아래 원칙을 추가했다.

- `payload.signals.saju.investmentFeatures`는 투자 성향, 심리, 변동성, 리밸런싱 필요성 설명을 위한 보조 신호로만 활용

이 문구를 넣은 이유는 모델이 이 feature를 과하게 결정 규칙처럼 쓰지 않도록 제한하기 위해서다.

### 4. 리스크 점수 반영

[ConsultingRiskScoreCalculator.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingRiskScoreCalculator.kt) 에서 일부 feature를 가중치로 반영한다.

현재 반영 규칙은 다음과 같다.

- `self_conflict` : `+10`
- `volatility_risk` : `+8`
- `asset_locking` : `+5`
- `conviction_overheat` : `+4`
- `transition_signal` : `+3`
- `execution_fast` : `+2`

## 현재 feature 의미

### baseTraits

- `conviction_strong`
- `style_consistent`
- `execution_fast`
- `trend_sensitive`
- `asset_locking`
- `rebalancing_needed`
- `inner_water_high`
- `inner_fire_high`
- `inner_wood_high`
- `inner_metal_high`
- `inner_earth_high`
- `balanced_temper`

### dynamicSignals

- `major_stage_saengji`
- `major_stage_wangji`
- `major_stage_myoji`
- `major_stage_neutral`
- `yearly_stage_saengji`
- `yearly_stage_wangji`
- `yearly_stage_myoji`
- `yearly_stage_neutral`
- `transition_signal`

### riskFlags

- `volatility_risk`
- `self_conflict`
- `asset_locking`
- `conviction_overheat`

## 왜 이렇게 적용했는가

이번 구조는 다음 이유로 선택했다.

- 기존 사주 계산 로직을 건드리지 않고 확장 가능하다.
- 프론트 스펙을 바꾸지 않아도 된다.
- 상담 결과 품질만 먼저 개선할 수 있다.
- 위험한 직접 투자 추천 규칙으로 흘러가는 것을 막을 수 있다.
- feature 추가/삭제가 쉬워 A/B 성격의 운영 실험도 가능하다.

## 한계

현재 구현은 MVP 성격이다.

- `생지/왕지/묘지`, `합/충`, `지장간` 일부만 반영했다.
- 용신 기반 추천 로직은 아직 넣지 않았다.
- `형`, `파`, `해`, 삼합/방합 같은 확장 규칙은 아직 미적용이다.
- 현재 feature는 저장하지 않고 상담 시 계산한다.
- 프롬프트 규칙은 최소 가이드만 반영돼 있다.

## 향후 확장 아이디어

- `형/파/해` 추가
- 용신/희신 기반 보정
- 시나리오별 가중치 차등 적용
- `MENTAL_GUIDE`, `RESCUE_PLAN` 같은 시나리오별 문구 규칙 강화
- 상담 결과와 실제 피드백 데이터를 연결해 feature 유효성 점검

## 롤백 가이드

이번 작업은 분리형 구조라 롤백이 비교적 단순하다.

### 가장 쉬운 롤백

아래 파일 두 개를 제거한다.

- [SajuInvestmentFeatures.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/saju/investment/SajuInvestmentFeatures.kt)
- [SajuInvestmentFeatureService.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/saju/investment/SajuInvestmentFeatureService.kt)

그리고 아래 연결부를 되돌린다.

- [ConsultingService.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingService.kt)
- [ConsultingRiskScoreCalculator.kt](/Users/hwjeong/dev/fortune_index/src/main/kotlin/com/hwcompany/fortune_index/consulting/ConsultingRiskScoreCalculator.kt)

### 구체적인 롤백 포인트

#### `ConsultingService.kt`

되돌릴 항목:

- `SajuInvestmentFeatureService`, `SajuInvestmentFeatures` import 제거
- 생성자 주입의 `sajuInvestmentFeatureService` 제거
- `val sajuInvestmentFeatures = ...` 제거
- `PreparedConsultation.sajuFeatures` 제거
- `buildPayload()` 파라미터의 `sajuInvestmentFeatures` 제거
- `buildScenarioAwareSystemMessage()` 파라미터의 `sajuInvestmentFeatures` 제거
- `buildSajuPayload()`에서 `investmentFeatures` 필드 제거
- 프롬프트 가이드 문장 제거

#### `ConsultingRiskScoreCalculator.kt`

되돌릴 항목:

- `SajuInvestmentFeatures` import 제거
- `calculate(..., sajuFeatures)` 파라미터 제거
- `sajuFeatureAdjustment()` 제거

### 부분 롤백도 가능

상담 품질 확인 후 단계적으로 끌 수도 있다.

- payload만 제거
- 프롬프트 가이드만 제거
- 리스크 가중치만 제거

즉 전체 롤백 없이도 영향 범위를 줄이는 방식이 가능하다.

## 운영 체크 포인트

- 상담 응답이 지나치게 공격적이거나 단정적으로 변하지 않는지 확인
- `stability_score`가 과하게 하락하지 않는지 확인
- 사주 feature가 없는 모드에서 null 처리 이상이 없는지 확인
- AI 답변이 내부 label을 그대로 노출하지 않는지 확인

## 현재 상태

현재 구현은 `상담에만 간접 활용`하는 방향으로 정리돼 있다.

- 프론트 직접 노출 없음
- 프로필 화면 설명용 기능 아님
- 투자 상담 품질 개선용 내부 feature로만 사용
