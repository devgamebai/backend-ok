package com.sunwinkr.lottery.engine.bet;

import java.util.Objects;

/**
 * Inbound bet request — pre-validation. Field-direct value carrier.
 *
 * <p>Ported from {@code LotteryCmd} (decompiled at
 * {@code game.modules.minigame.cmd.rev.LotteryCmd}). The legacy cmd has
 * three fields: {@code mode}, {@code num}, {@code betValue}. We add
 * {@code nickname}, {@code userId} (resolved at the bridge layer) and a
 * {@code clientNonce} for idempotency at the REST boundary (PR-3).
 *
 * @see com.sunwinkr.lottery.engine.bet.BetValidator
 */
public final class BetRequest {

    private final String nickname;
    private final long userId;
    private final int modeId;
    private final String ticket;
    private final long betValue;
    private final String clientNonce;

    public BetRequest(String nickname,
                      long userId,
                      int modeId,
                      String ticket,
                      long betValue,
                      String clientNonce) {
        this.nickname = Objects.requireNonNull(nickname, "nickname");
        this.userId = userId;
        this.modeId = modeId;
        this.ticket = ticket; // null/empty rejected in validator (0005)
        this.betValue = betValue;
        this.clientNonce = clientNonce; // may be null pre-PR-3
    }

    public String getNickname() {
        return nickname;
    }

    public long getUserId() {
        return userId;
    }

    public int getModeId() {
        return modeId;
    }

    public String getTicket() {
        return ticket;
    }

    public long getBetValue() {
        return betValue;
    }

    public String getClientNonce() {
        return clientNonce;
    }
}
