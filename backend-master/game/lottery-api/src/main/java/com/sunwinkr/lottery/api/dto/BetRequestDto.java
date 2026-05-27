package com.sunwinkr.lottery.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Wire DTO for {@code POST /api/v2/lottery/xsmb/bet}. Plan §5.2.
 *
 * <p>{@code clientNonce} is an idempotency key. JDK 8 — plain class with
 * public fields (no record).
 *
 * <p>FE-compat alias: legacy Cocos client + agency CMS send {@code mode}
 * and {@code num}; the new spec calls these {@code modeId} and {@code
 * ticket}. {@link JsonAlias} accepts BOTH names for the SAME field — FE
 * doesn't need to migrate.
 */
public final class BetRequestDto {

    @NotNull
    @JsonAlias("mode")
    public Integer modeId;

    @NotNull
    @JsonAlias("num")
    public String ticket;

    @Min(1)
    public long betValue;

    public String clientNonce;
}
