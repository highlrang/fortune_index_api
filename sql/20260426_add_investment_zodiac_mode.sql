ALTER TABLE consulting_histories
    MODIFY COLUMN analysis_mode ENUM(
        'INVESTMENT_SAJU',
        'INVESTMENT_TAROT',
        'INVESTMENT_ZODIAC',
        'INVESTMENT_ALL'
    ) NOT NULL;
