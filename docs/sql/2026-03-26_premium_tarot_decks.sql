-- Premium tarot/oracle deck support
-- Target: MySQL 8.x
-- Apply after deploying code that includes:
-- - users.subscription_tier
-- - tarot_deck_versions deck metadata columns
-- - tarot_cards deck_role/card_set_id and nullable arcana_type

START TRANSACTION;

ALTER TABLE users
    ADD COLUMN subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE' AFTER investment_risk_profile,
    ADD COLUMN preferred_tarot_deck_id VARCHAR(100) NULL AFTER profile_image_url;

UPDATE users
SET preferred_tarot_deck_id = 'classic-rider-waite'
WHERE preferred_tarot_deck_id IS NULL;

ALTER TABLE tarot_deck_versions
    ADD COLUMN deck_type VARCHAR(20) NOT NULL DEFAULT 'TAROT' AFTER cover_image_url,
    ADD COLUMN deck_role VARCHAR(20) NOT NULL DEFAULT 'MAIN' AFTER deck_type,
    ADD COLUMN card_set_id VARCHAR(100) NOT NULL DEFAULT 'rider-waite-78' AFTER deck_role,
    ADD COLUMN draw_count INT NOT NULL DEFAULT 3 AFTER card_set_id,
    ADD COLUMN required_subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE' AFTER draw_count;

ALTER TABLE tarot_cards
    ADD COLUMN deck_role VARCHAR(20) NOT NULL DEFAULT 'MAIN' AFTER deck_type,
    ADD COLUMN card_set_id VARCHAR(100) NOT NULL DEFAULT 'rider-waite-78' AFTER deck_role;

ALTER TABLE tarot_cards
    MODIFY COLUMN arcana_type VARCHAR(20) NULL;

UPDATE tarot_deck_versions
SET
    deck_type = 'TAROT',
    deck_role = 'MAIN',
    card_set_id = 'rider-waite-78',
    draw_count = 3,
    required_subscription_tier = 'FREE'
WHERE id = 'classic-rider-waite';

UPDATE tarot_cards
SET
    deck_role = 'MAIN',
    card_set_id = 'rider-waite-78'
WHERE deck_version_id = 'classic-rider-waite';

INSERT INTO tarot_deck_versions (
    id,
    name,
    description,
    cover_image_url,
    deck_type,
    deck_role,
    card_set_id,
    draw_count,
    required_subscription_tier,
    active,
    created_at,
    updated_at
)
VALUES (
    'classic-rider-waite-signature',
    '시그니처 라이더',
    '프리미엄 전용 메인 타로 덱',
    'https://cdn.example.com/tarot/signature/cover.png',
    'TAROT',
    'MAIN',
    'rider-waite-78',
    3,
    'PREMIUM',
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    cover_image_url = VALUES(cover_image_url),
    deck_type = VALUES(deck_type),
    deck_role = VALUES(deck_role),
    card_set_id = VALUES(card_set_id),
    draw_count = VALUES(draw_count),
    required_subscription_tier = VALUES(required_subscription_tier),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

INSERT INTO tarot_cards (
    deck_version_id,
    selected_index,
    code,
    deck_type,
    deck_role,
    card_set_id,
    name,
    korean_name,
    sort_order,
    arcana_type,
    suit,
    meaning,
    description,
    image_url,
    video_url
)
SELECT
    'classic-rider-waite-signature' AS deck_version_id,
    selected_index,
    code,
    'TAROT' AS deck_type,
    'MAIN' AS deck_role,
    'rider-waite-78' AS card_set_id,
    name,
    korean_name,
    sort_order,
    arcana_type,
    suit,
    meaning,
    description,
    REPLACE(image_url, '/classic/', '/signature/') AS image_url,
    CASE
        WHEN video_url IS NULL THEN NULL
        ELSE REPLACE(video_url, '/classic/', '/signature/')
    END AS video_url
FROM tarot_cards
WHERE deck_version_id = 'classic-rider-waite'
ON DUPLICATE KEY UPDATE
    deck_type = VALUES(deck_type),
    deck_role = VALUES(deck_role),
    card_set_id = VALUES(card_set_id),
    name = VALUES(name),
    korean_name = VALUES(korean_name),
    sort_order = VALUES(sort_order),
    arcana_type = VALUES(arcana_type),
    suit = VALUES(suit),
    meaning = VALUES(meaning),
    description = VALUES(description),
    image_url = VALUES(image_url),
    video_url = VALUES(video_url);

INSERT INTO tarot_deck_versions (
    id,
    name,
    description,
    cover_image_url,
    deck_type,
    deck_role,
    card_set_id,
    draw_count,
    required_subscription_tier,
    active,
    created_at,
    updated_at
)
VALUES (
    'market-signal-oracle',
    '마켓 시그널 오라클',
    '프리미엄 전용 보조 오라클 카드',
    'https://cdn.example.com/oracle/market-signal/cover.png',
    'ORACLE',
    'ASSISTANT',
    'market-signal-oracle',
    1,
    'PREMIUM',
    TRUE,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    cover_image_url = VALUES(cover_image_url),
    deck_type = VALUES(deck_type),
    deck_role = VALUES(deck_role),
    card_set_id = VALUES(card_set_id),
    draw_count = VALUES(draw_count),
    required_subscription_tier = VALUES(required_subscription_tier),
    active = VALUES(active),
    updated_at = VALUES(updated_at);

INSERT INTO tarot_cards (
    deck_version_id,
    selected_index,
    code,
    deck_type,
    deck_role,
    card_set_id,
    name,
    korean_name,
    sort_order,
    arcana_type,
    suit,
    meaning,
    description,
    image_url,
    video_url
)
VALUES
(
    'market-signal-oracle',
    0,
    'ENTRY_WINDOW',
    'ORACLE',
    'ASSISTANT',
    'market-signal-oracle',
    'Entry Window',
    '진입 창',
    0,
    NULL,
    NULL,
    '진입 타이밍이 열리지만 분할 접근이 유효하다.',
    '추세를 무작정 추격하기보다 진입 창이 열릴 때 천천히 비중을 실으라는 보조 신호다.',
    'https://cdn.example.com/oracle/market-signal/000.png',
    'https://cdn.example.com/oracle/market-signal/000.mp4'
),
(
    'market-signal-oracle',
    1,
    'VOLATILITY_SPIKE',
    'ORACLE',
    'ASSISTANT',
    'market-signal-oracle',
    'Volatility Spike',
    '변동성 급등',
    1,
    NULL,
    NULL,
    '방향성보다 변동성 관리가 먼저다.',
    '좋은 종목이어도 진입 속도와 손절 기준을 더 촘촘하게 잡아야 하는 구간을 뜻한다.',
    'https://cdn.example.com/oracle/market-signal/001.png',
    'https://cdn.example.com/oracle/market-signal/001.mp4'
),
(
    'market-signal-oracle',
    2,
    'CONFIRMATION',
    'ORACLE',
    'ASSISTANT',
    'market-signal-oracle',
    'Confirmation',
    '확인 신호',
    2,
    NULL,
    NULL,
    '기존 판단을 재확인해도 되는 구간이다.',
    '보조 지표와 타이밍이 맞물리는 만큼, 기존 전략을 유지하되 과신은 피하라는 카드다.',
    'https://cdn.example.com/oracle/market-signal/002.png',
    'https://cdn.example.com/oracle/market-signal/002.mp4'
)
ON DUPLICATE KEY UPDATE
    deck_type = VALUES(deck_type),
    deck_role = VALUES(deck_role),
    card_set_id = VALUES(card_set_id),
    name = VALUES(name),
    korean_name = VALUES(korean_name),
    sort_order = VALUES(sort_order),
    arcana_type = VALUES(arcana_type),
    suit = VALUES(suit),
    meaning = VALUES(meaning),
    description = VALUES(description),
    image_url = VALUES(image_url),
    video_url = VALUES(video_url);

COMMIT;
