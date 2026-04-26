ALTER TABLE users
    ADD COLUMN birth_place_name VARCHAR(100) NULL AFTER birth_time,
    ADD COLUMN birth_latitude DECIMAL(10,6) NULL AFTER birth_place_name,
    ADD COLUMN birth_longitude DECIMAL(10,6) NULL AFTER birth_latitude;
