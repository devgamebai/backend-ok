package com.sunwinkr.minigame.api.dto;

import javax.validation.constraints.NotNull;

/** Admin {@code POST /api/v2/admin/taixiu/kill-switch} payload. */
public final class KillSwitchRequest {

    @NotNull
    public Boolean paused;
}
