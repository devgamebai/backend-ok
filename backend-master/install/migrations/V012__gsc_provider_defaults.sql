-- SUN-1201 Phase 3: per-provider default game attribution.
--
-- Used by BetContextResolver as the third fallback layer (after VENDOR
-- and SESSION) when a wager push arrives with an empty game_code AND
-- the player's recent c=3090 launch session has expired or doesn't
-- match the wager's product_code.
--
-- Each row says: "for this provider's empty-game_code wagers, attribute
-- to <default_game_code> so the rebate pipeline can resolve a category
-- via gsc_game_catalog and pay the configured live_cat_<X> rate".
--
-- The default_category column is informational — it pins the operator-
-- intent so ops can see at a glance how a provider's empty-game_code
-- bets will be classified, without having to chain through
-- gsc_game_catalog manually.
--
-- Reversible: DROP TABLE leaves the resolver fall-through to the
-- legacy alias path identical to pre-V012.

CREATE TABLE IF NOT EXISTS gsc_provider_defaults (
    product_code      INT          NOT NULL PRIMARY KEY,
    default_game_code VARCHAR(64)  NOT NULL,
    default_category  VARCHAR(32)  NULL,
    notes             TEXT         NULL,
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Seed: Dream Gaming (product_code 1052) — pushes wagers with empty
-- game_code; default to Baccarat A01 (the most common table in the
-- 11-table Dreaming RB tier) so unattributed bets still pay
-- live_cat_Baccarat commission.
INSERT INTO gsc_provider_defaults (product_code, default_game_code, default_category, notes)
VALUES (1052, '20101', 'Baccarat',
        'Dream Gaming sends empty game_code on wager pushes. Default attribution = Baccarat A01.')
ON DUPLICATE KEY UPDATE
    default_game_code = VALUES(default_game_code),
    default_category  = VALUES(default_category),
    notes             = VALUES(notes);
