package com.sunwinkr.minigame.api.dto.sicbo;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Admin {@code POST /api/v2/admin/sicbo/force-result} payload.
 *
 * <p>Three dice values in {@code [1..6]}. The controller writes them
 * into the Hazelcast {@code ketquataixiusicbo} map for consumption by
 * the engine pipeline on the next round.
 */
public final class SicboForceResultRequest {

    @NotNull
    @Size(min = 3, max = 3)
    public short[] dice;
}
