package com.sunwinkr.minigame.api.dto;

import javax.validation.constraints.NotNull;

/**
 * Admin {@code POST /api/v2/admin/taixiu/force-result} payload.
 *
 * <p>{@code side}: {@code 0} = XIU, {@code 1} = TAI. The controller
 * generates concrete dice matching the side and writes them into the
 * Hazelcast {@code ketquataixiu} map for consumption by the engine
 * pipeline on the next round.
 */
public final class ForceResultRequest {

    @NotNull
    public Integer side;
}
