ALTER TABLE users
    ADD COLUMN western_zodiac ENUM(
        'ARIES',
        'TAURUS',
        'GEMINI',
        'CANCER',
        'LEO',
        'VIRGO',
        'LIBRA',
        'SCORPIO',
        'SAGITTARIUS',
        'CAPRICORN',
        'AQUARIUS',
        'PISCES'
    ) NULL AFTER gender;

UPDATE users
SET western_zodiac = CASE
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '03-21' AND '04-19' THEN 'ARIES'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '04-20' AND '05-20' THEN 'TAURUS'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '05-21' AND '06-21' THEN 'GEMINI'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '06-22' AND '07-22' THEN 'CANCER'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '07-23' AND '08-22' THEN 'LEO'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '08-23' AND '09-23' THEN 'VIRGO'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '09-24' AND '10-22' THEN 'LIBRA'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '10-23' AND '11-22' THEN 'SCORPIO'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '11-23' AND '12-24' THEN 'SAGITTARIUS'
    WHEN DATE_FORMAT(birth_date, '%m-%d') >= '12-25' OR DATE_FORMAT(birth_date, '%m-%d') <= '01-19' THEN 'CAPRICORN'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '01-20' AND '02-18' THEN 'AQUARIUS'
    WHEN DATE_FORMAT(birth_date, '%m-%d') BETWEEN '02-19' AND '03-20' THEN 'PISCES'
    ELSE western_zodiac
END
WHERE birth_date IS NOT NULL
  AND western_zodiac IS NULL;
