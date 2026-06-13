-- Reviewed content seed for home_summary_interpretations.
-- Scope: TAROT_CARD, DEFAULT variant.
-- Source: tarot_cards from the active/default tarot catalog.
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
    'TAROT_CARD' AS category,
    c.code,
    'DEFAULT' AS variant,
    CONCAT(c.name, ' - ', investment_action.action) AS title,
    CONCAT(
        c.name,
        '은 투자 관점에서 ''',
        investment_action.action,
        ''' 신호로 읽을 수 있습니다. ',
        CASE investment_action.action
            WHEN '현금 확보' THEN '불확실한 자리를 줄이고 여력을 남기라는 뜻입니다. '
            WHEN '수익 실현' THEN '성과가 있다면 일부를 현실화하라는 뜻입니다. '
            WHEN '과감한 베팅' THEN '충동이 아니라 준비된 기준 위에서 움직이라는 뜻입니다. '
            ELSE '기반을 흔들지 말고 균형을 점검하라는 뜻입니다. '
        END,
        '감정보다 포지션의 안정성을 먼저 확인하세요.'
    ) AS daily_body,
    CONCAT(
        '{birthCard} 탄생 카드의 {birthCardKeyword} 성향과 오늘 카드의 ',
        c.meaning,
        ' 메시지가 만납니다. 따라서 오늘은 {todayCardKeyword} 흐름을 우선 보세요.'
    ) AS personal_body_template,
    CONCAT(
        '["매수/매도: ',
        CASE investment_action.action
            WHEN '현금 확보' THEN '신규 진입보다 불확실한 포지션 축소에 집중하세요.'
            WHEN '수익 실현' THEN '오른 자리는 일부 익절하고 남은 비중만 유지하세요.'
            WHEN '과감한 베팅' THEN '확인된 자리만 작게 진입하고 추격 매수는 피하세요.'
            ELSE '신규 진입보다 보유 비중 유지에 집중하세요.'
        END,
        '","리스크 관리: ',
        CASE investment_action.action
            WHEN '현금 확보' THEN '손절 기준을 넓히지 말고 현금 비중을 먼저 확보하세요.'
            WHEN '수익 실현' THEN '수익 구간의 되돌림 한도를 미리 정하세요.'
            WHEN '과감한 베팅' THEN '진입 전 손절 라인을 숫자로 고정하세요.'
            ELSE '리밸런싱은 하되 총 위험 노출은 늘리지 마세요.'
        END,
        '","마인드셋: ',
        CASE investment_action.action
            WHEN '현금 확보' THEN '기다리는 것도 오늘의 선택지로 인정하세요.'
            WHEN '수익 실현' THEN '더 벌고 싶은 마음보다 지킨 수익을 먼저 보세요.'
            WHEN '과감한 베팅' THEN '확신보다 기준이 먼저라는 점을 잊지 마세요.'
            ELSE '시장이 지루해도 조급함을 줄이세요.'
        END,
        '"]'
    ) AS points_json,
    TRUE AS active
FROM tarot_cards c
JOIN tarot_deck_versions d ON d.id = c.deck_version_id
JOIN (
    SELECT
        c2.id AS card_id,
        CASE
            WHEN c2.code IN ('FOUR_OF_WANDS', 'TEMPERANCE', 'THE_EMPEROR', 'KING_OF_PENTACLES') THEN '포트폴리오 유지'
            WHEN c2.code IN ('NINE_OF_CUPS', 'TEN_OF_CUPS', 'SIX_OF_WANDS', 'THE_SUN', 'THE_WORLD') THEN '수익 실현'
            WHEN c2.code IN ('FOUR_OF_SWORDS', 'THE_HERMIT', 'THE_HANGED_MAN', 'THE_MOON') THEN '현금 확보'
            WHEN c2.code IN ('THE_FOOL', 'THE_MAGICIAN', 'THE_CHARIOT', 'ACE_OF_WANDS') THEN '과감한 베팅'
            WHEN c2.suit = 'PENTACLES' THEN '포트폴리오 유지'
            WHEN c2.suit = 'SWORDS' THEN '현금 확보'
            WHEN c2.suit = 'WANDS' THEN '과감한 베팅'
            WHEN c2.suit = 'CUPS' THEN '수익 실현'
            ELSE '포트폴리오 유지'
        END AS action
    FROM tarot_cards c2
) investment_action ON investment_action.card_id = c.id
WHERE d.deck_role = 'MAIN'
  AND d.active = TRUE
  AND d.display_order = (
      SELECT MIN(d2.display_order)
      FROM tarot_deck_versions d2
      WHERE d2.deck_role = 'MAIN'
        AND d2.active = TRUE
  )
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    daily_body = VALUES(daily_body),
    personal_body_template = VALUES(personal_body_template),
    points_json = VALUES(points_json),
    active = VALUES(active),
    updated_at = CURRENT_TIMESTAMP;
