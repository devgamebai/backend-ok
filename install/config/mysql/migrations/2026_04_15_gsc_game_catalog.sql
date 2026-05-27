-- SUN-889 follow-up: optional catalog table to store human-readable game
-- names from GSC c=9894. Populated by a one-shot script (or admin cache
-- refresh). GscGameNameResolver reads this table to surface real game
-- names like "Baccarat D07" / "Mega Ball" in c=303 + c=9843 history.
-- Without a row, resolver falls back to "<provider_name> - <game_code>".

CREATE TABLE IF NOT EXISTS vinplay.gsc_game_catalog (
    product_code  INT           NOT NULL,
    game_code     VARCHAR(64)   NOT NULL,
    game_name     VARCHAR(255)  NOT NULL,
    game_type     VARCHAR(32)   DEFAULT NULL,
    category      VARCHAR(64)   DEFAULT NULL,
    image_url     VARCHAR(512)  DEFAULT NULL,
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_code, game_code),
    KEY idx_catalog_name (game_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
