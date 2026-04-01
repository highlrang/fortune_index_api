-- Reset seeded tarot/oracle card metadata
-- Target: MySQL 8.x
-- Purpose:
-- - Fix broken tarot_cards mappings that cause duplicate key errors during TarotDeckSeeder startup
-- - Let the existing ApplicationRunner seed clean rows again on next server start
--
-- How to use:
-- 1. Stop the application
-- 2. Run this SQL
-- 3. Start the application again

START TRANSACTION;

DELETE FROM tarot_cards
WHERE deck_version_id IN (
    'classic-rider-waite',
    'classic-rider-waite-signature',
    'market-signal-oracle'
);

COMMIT;

-- Optional verification before restart
SELECT deck_version_id, COUNT(*) AS card_count
FROM tarot_cards
WHERE deck_version_id IN (
    'classic-rider-waite',
    'classic-rider-waite-signature',
    'market-signal-oracle'
)
GROUP BY deck_version_id;
