# consulting_histories Column Review

## Summary

이 문서는 현재 코드 기준으로 `consulting_histories` 테이블 컬럼을 정리한 검토본입니다.

- 이미 제거 로직 반영 완료
  - `investment_analysis_text`
  - `selected_investment_label`
- 아직 유지 필요
  - 회고 기능 컬럼
  - 사주/타로/focus 스냅샷 컬럼
- 정책 결정 후 제거 가능
  - 현재 응답/기능에서 의미가 약한 스냅샷 값들

## Quick Decision Table

| Column | Decision | Why |
| --- | --- | --- |
| `investment_analysis_text` | remove now | 현재 LLM 응답 구조에서 더 이상 사용하지 않음 |
| `selected_investment_label` | remove now | `investment_label`로 통일 완료 |
| `realized_profit_rate` | keep | 회고/리뷰 기능에서 사용 중 |
| `retro_note` | keep | 회고 상세/수정 기능에서 사용 중 |
| `saju_wood_ratio` ~ `saju_water_ratio` | keep | 저장 당시 사주 스냅샷 복원용 |
| `investment_current_value` | keep | `focus` 응답에 포함 |
| `investment_change_rate` | keep | `focus` 응답에 포함 |
| `tarot_interpretation_mode` | keep | 저장 당시 타로 해석 모드 복원용 |

## Status

### Already Removed In Code

아래 두 컬럼은 백엔드 코드에서 이미 더 이상 사용하지 않도록 정리되었습니다.

- `investment_analysis_text`
- `selected_investment_label`

DB에서는 아래 SQL로 제거 가능합니다.

```sql
ALTER TABLE consulting_histories
    DROP COLUMN investment_analysis_text,
    DROP COLUMN selected_investment_label;
```

## Keep

### Core History

- `id`
- `user_id`
- `analysis_mode`
- `consulting_scenario`
- `consulted_at`
- `question`
- `share_key`
- `ai_response_json`
- `analysis_result_json`

이 컬럼들은 이력 식별, 소유권, 상세 복원, 공유 기능에 직접 사용됩니다.

### Focus Snapshot

- `investment_ticker`
- `investment_label`
- `investment_captured_at`
- `investment_current_value`
- `investment_change_rate`

현재 `focus` 응답 생성에 사용됩니다.

참고:

- `selected_investment_label`은 더 이상 쓰지 않습니다.
- 현재는 `investment_label` 하나만 canonical 값으로 유지합니다.

관련 코드:

- [ValueObjects.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/domain/model/ValueObjects.kt:59)
- [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt:725)

### Saju Snapshot

- `saju_wood_ratio`
- `saju_fire_ratio`
- `saju_earth_ratio`
- `saju_metal_ratio`
- `saju_water_ratio`
- `saju_summary`

현재 이력 상세/공유 응답에서 저장 당시의 사주 스냅샷을 내려줄 때 사용됩니다.

관련 코드:

- [ValueObjects.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/domain/model/ValueObjects.kt:84)
- [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt:593)

### Tarot Snapshot

- `tarot_cards_json`
- `tarot_interpretation_mode`
- `tarot_summary`

`tarot_interpretation_mode`는 상담 당시 어떤 타로 해석 모드로 진행했는지 저장하는 컬럼입니다.
현재 값은 `MAIN_TRADITIONAL` 중심이지만, 구조상 모드 확장을 고려한 필드입니다.

관련 코드:

- [ValueObjects.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/domain/model/ValueObjects.kt:101)
- [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt:85)

### Review / Retro

- `feedback`
- `realized_profit_rate`
- `retro_note`
- `retrospected_at`

이 컬럼들은 회고, 리뷰, 만족도, 통계 API에서 실제 사용 중입니다.

관련 코드:

- [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt:222)
- [ConsultingHistoryService.kt](/Users/digitalmedic_hw/hwdev/fortune_index_api/src/main/kotlin/com/hwcompany/fortune_index/history/ConsultingHistoryService.kt:264)

## Removable Later

아래 컬럼들은 현재는 유지해야 하지만, 정책을 바꾸면 제거 검토가 가능합니다.

### `investment_current_value`, `investment_change_rate`

현재는 `focus` 응답에 노출되므로 유지 대상입니다.
다만 지금 저장값이 대부분 `0` 또는 의미 없는 스냅샷이라면, 프론트에서 실제로 사용하지 않는 시점에 제거를 검토할 수 있습니다.

제거 전 선행 작업:

- `FocusSnapshotResponse`에서 제거
- 목록/상세/공유 응답에서 제거
- 저장 시 `InvestmentFocusSnapshot` 구조 축소

### `saju_*_ratio`

현재는 저장 당시 사주 스냅샷 복원에 쓰입니다.
상세 응답에서 사주를 요약 텍스트만 보여주도록 바꾸면 제거 검토가 가능합니다.

제거 전 선행 작업:

- `SajuSnapshotResponse` 구조 단순화
- 이력 상세/공유 응답에서 오행 비율 제거

### `tarot_interpretation_mode`

현재는 이력 목록과 저장된 타로 스냅샷 복원에 쓰입니다.
향후 해석 모드를 항상 하나만 쓸 계획이라면 제거 검토가 가능합니다.

제거 전 선행 작업:

- 목록 응답의 `tarotInterpretationMode` 제거
- `TarotHistorySnapshot` 구조 단순화

### `realized_profit_rate`, `retro_note`

회고/리뷰 기능을 더 이상 유지하지 않을 때만 제거할 수 있습니다.

제거 전 선행 작업:

- 리뷰/회고 수정 API 제거
- 회고 통계 API 제거
- 상세 응답의 `retro` 제거

## Current Recommendation

지금 당장 안전하게 DB에서 제거 가능한 컬럼은 아래 두 개입니다.

- `investment_analysis_text`
- `selected_investment_label`

실행 SQL:

```sql
ALTER TABLE consulting_histories
    DROP COLUMN investment_analysis_text,
    DROP COLUMN selected_investment_label;
```

나머지 컬럼은 현재 코드와 응답 구조에서 아직 사용 중이므로, 선행 작업 없이 바로 제거하면 안 됩니다.
