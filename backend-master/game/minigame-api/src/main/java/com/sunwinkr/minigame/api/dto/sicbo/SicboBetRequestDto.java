package com.sunwinkr.minigame.api.dto.sicbo;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Wire DTO for {@code POST /api/v2/sicbo/bet}. Plan §6 (Sicbo analog of
 * TaiXiu §5.2).
 *
 * <p>Distinct from TaiXiu in that {@code betSide} is a STRING name (e.g.
 * "TAI", "XIU", "POINT_8", "ONE_DICE_3") rather than a numeric id —
 * Sicbo has 52 bet types and a string is the wire-stable representation.
 *
 * <p>{@code clientNonce} is an idempotency key — the same nonce within
 * a 5-minute window returns the cached response (plan §5.6).
 */
public final class SicboBetRequestDto {

    @NotNull
    public Short moneyType;

    @Min(1)
    public long betValue;

    /**
     * Sicbo bet-side wire name (e.g. {@code "TAI"}, {@code "XIU"},
     * {@code "POINT_8"}, {@code "ONE_DICE_3"}). Decoded server-side via
     * {@code SicboBetType.byName(...)}; unknown names produce errorCode 6
     * (AMBIGUOUS #6 — replaces legacy NPE).
     */
    @NotNull
    public String betSide;

    public Short inputTime;

    public String clientNonce;
}
