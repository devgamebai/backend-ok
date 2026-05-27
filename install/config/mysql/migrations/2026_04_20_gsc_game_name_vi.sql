-- =============================================================================
-- SUN-990 [GSC-1]: Add game_name_vi column to gsc_game_catalog
-- =============================================================================
-- Vietnamese display name for agency bet history. Seed/on-demand fetch
-- never touches game_name_vi — only admin/migration populates it.
-- GscGameNameResolver returns game_name_vi when non-null, fallback game_name.
-- =============================================================================

ALTER TABLE vinplay.gsc_game_catalog ADD COLUMN IF NOT EXISTS
    game_name_vi VARCHAR(255) DEFAULT NULL AFTER game_name;

-- Also add to AWC catalog for consistency
ALTER TABLE vinplay_minigame.awc_game_catalog ADD COLUMN IF NOT EXISTS
    game_name_vi VARCHAR(255) DEFAULT NULL AFTER game_name;
