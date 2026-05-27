package com.sunwinkr.minigame.api.dto.sicbo;

/**
 * Admin {@code POST /api/v2/admin/sicbo/kill-switch} payload.
 *
 * <p>{@code paused = true} freezes new bet acceptance; {@code false}
 * resumes. The engine respects this flag inside the bet path
 * (operator-only safety lever).
 */
public final class SicboKillSwitchRequest {

    public Boolean paused;
}
