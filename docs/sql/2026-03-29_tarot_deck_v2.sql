-- Tarot main deck V2
-- Target: MySQL 8.x
--
-- Purpose:
-- - add a new 78-card tarot main deck version
-- - keep the same Rider-Waite card metadata
-- - change only image_url to the /tarot/v2 directory
--
-- Assumptions:
-- 1. The source deck is classic-rider-waite.
-- 2. card_set_id remains rider-waite-78 because the card composition is unchanged.
-- 3. Major arcana filenames use the exact names provided by product.
-- 4. Minor arcana filenames follow: /tarot/v2/{selected_index}_{code}.png
--    If the actual minor filenames differ, edit the ELSE branch before applying.

START TRANSACTION;

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
    'classic-rider-waite-v2',
    '네오 라이더',
    '',
    '/tarot/v2/0_THE_FOOL.png',
    'TAROT',
    'MAIN',
    'rider-waite-78',
    3,
    'FREE',
    TRUE,
    NOW(),
    NOW()
);

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
    'classic-rider-waite-v2' AS deck_version_id,
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
    CASE selected_index
        WHEN 0 THEN '/tarot/v2/0_THE_FOOL.png'
        WHEN 1 THEN '/tarot/v2/1_THE_MAGICAN.png'
        WHEN 2 THEN '/tarot/v2/2_THE_HIGH_PRIESTESS.png'
        WHEN 3 THEN '/tarot/v2/3_THE_EMPRESS.png'
        WHEN 4 THEN '/tarot/v2/4_THE_EMPEROR.png'
        WHEN 5 THEN '/tarot/v2/5_THE_HIEROPHANT.png'
        WHEN 6 THEN '/tarot/v2/6_THE_LOVERS.png'
        WHEN 7 THEN '/tarot/v2/7_THE_CHARIOT.png'
        WHEN 8 THEN '/tarot/v2/8_STRENGTH.png'
        WHEN 9 THEN '/tarot/v2/9_THE_HERMIT.png'
        WHEN 10 THEN '/tarot/v2/10_THE_WHEEL_OF_FORTUNE.png'
        WHEN 11 THEN '/tarot/v2/11_JUSTICE.png'
        WHEN 12 THEN '/tarot/v2/12_THE_HANGED_MAN.png'
        WHEN 13 THEN '/tarot/v2/13_DEATH.png'
        WHEN 14 THEN '/tarot/v2/14_TEMPERANCE.png'
        WHEN 15 THEN '/tarot/v2/15_THE_DEVIL.png'
        WHEN 16 THEN '/tarot/v2/16_THE_TOWER.png'
        WHEN 17 THEN '/tarot/v2/17_THE_STAR.png'
        WHEN 18 THEN '/tarot/v2/18_THE_MOON.png'
        WHEN 19 THEN '/tarot/v2/19_THE_SUN.png'
        WHEN 20 THEN '/tarot/v2/20_JUDGEMENT.png'
        WHEN 21 THEN '/tarot/v2/21_THE_WORLD.png'
        ELSE CONCAT('/tarot/v2/', selected_index, '_', code, '.png')
    END AS image_url,
    video_url
FROM tarot_cards
WHERE deck_version_id = 'classic-rider-waite';

COMMIT;
