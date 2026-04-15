CREATE TABLE tarot_birth_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    card_set_id VARCHAR(100) NOT NULL,
    code VARCHAR(60) NOT NULL,
    arcana_type VARCHAR(20) NOT NULL,
    birth_meaning VARCHAR(500) NOT NULL,
    birth_description VARCHAR(1000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tarot_birth_cards_card_set_code (card_set_id, code)
);
