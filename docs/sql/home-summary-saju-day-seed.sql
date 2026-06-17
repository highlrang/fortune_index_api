-- Reviewed content seed for home_summary_interpretations.
-- Scope: SAJU_DAY, DEFAULT variant.
-- Source: saju_interpretations DAY_PILLAR rows.
-- Dialect: MySQL / H2 MySQL mode.

INSERT INTO home_summary_interpretations (
    category,
    code,
    variant,
    title,
    daily_body,
    personal_body_template,
    points_json,
    active
)
SELECT
    'SAJU_DAY' AS category,
    s.code,
    'DEFAULT' AS variant,
    CONCAT(s.title, ' - 오늘의 시장 기운') AS title,
    CASE
        WHEN s.summary_default IS NOT NULL AND s.summary_default <> '' THEN REPLACE(s.summary_default, '일주', '일')
        ELSE CONCAT(s.title, '의 흐름은 오늘 시장 심리와 판단 리듬을 점검하라는 신호입니다. 빠른 결정보다 기준 확인을 우선하세요.')
    END AS daily_body,
    CONCAT(
        '{userGanji}의 {userGanjiKeyword} 성향과 오늘의 {todayGanjiKeyword} 기운이 만납니다. ',
        '따라서 진입 속도보다 근거 확인을 먼저 두세요.'
    ) AS personal_body_template,
    '["매수/매도: 진입 이유가 한 문장으로 안 쓰이면 보류하세요.","리스크 관리: 손실 기준을 수익 목표보다 먼저 정하세요.","마인드셋: 좋아 보여도 비중은 나누어 들어가세요."]' AS points_json,
    TRUE AS active
FROM saju_interpretations s
WHERE s.category = 'DAY_PILLAR'
  AND s.active = TRUE
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    daily_body = VALUES(daily_body),
    personal_body_template = VALUES(personal_body_template),
    points_json = VALUES(points_json),
    active = VALUES(active),
    updated_at = CURRENT_TIMESTAMP;
