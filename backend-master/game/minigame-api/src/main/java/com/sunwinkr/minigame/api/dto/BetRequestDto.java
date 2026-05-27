package com.sunwinkr.minigame.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Wire DTO for {@code POST /api/v2/taixiu/bet}. Plan §5.2.
 *
 * <p>{@code clientNonce} is an idempotency key — the same nonce within
 * a 5-minute window returns the cached response (plan §5.6).
 */
public final class BetRequestDto {

    @NotNull
    public Short moneyType;

    @Min(1)
    public long betValue;

    @NotNull
    public Short betSide;

    public Short inputTime;

    public String clientNonce;
}
