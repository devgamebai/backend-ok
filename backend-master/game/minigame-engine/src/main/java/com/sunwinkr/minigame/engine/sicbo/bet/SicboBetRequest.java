package com.sunwinkr.minigame.engine.sicbo.bet;

/**
 * Immutable value object carrying the fields needed by
 * {@code SicboBetService.accept()} for one bet attempt.
 *
 * <p>Source mapping: fields extracted from
 * {@code BetSicboCmd} (cmd.betSide, cmd.betValue, cmd.userId,
 * cmd.inputTime, cmd.moneyType) and the {@code User} object passed into
 * {@code MGRoomSicbo.betTaiXiu(User, BetSicboCmd)} (SBR:401-433).
 */
public final class SicboBetRequest {

    /** Username / nickname of the player or bot. */
    public final String nickname;

    /** Numeric user ID; 0 for bots (matches legacy TransactionTaiXiuDetail convention). */
    public final int userId;

    /** Amount to wager (must be &ge; 100 per INV-13 / SBR:545). */
    public final long betValue;

    /**
     * Client-supplied input time (short from cmd.inputTime).
     * Overridden by {@code getRemainTime()} inside legacy code; engine
     * preserves it as supplied and lets the adapter layer substitute the
     * remain-time if needed.
     */
    public final short inputTime;

    /** Money type: 1 = VIN, 2 = XU. */
    public final short moneyType;

    /**
     * Wire name of the bet side (e.g. {@code "TAI"}, {@code "POINT_7"}).
     * Decoded via {@code SicboBetType.byName(betSideName)} inside the service.
     */
    public final String betSideName;

    /** {@code true} when this bet originates from a bot player. */
    public final boolean isBot;

    public SicboBetRequest(String nickname, int userId, long betValue,
                           short inputTime, short moneyType,
                           String betSideName, boolean isBot) {
        if (nickname == null || nickname.isEmpty()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        if (betSideName == null || betSideName.isEmpty()) {
            throw new IllegalArgumentException("betSideName must not be blank");
        }
        this.nickname    = nickname;
        this.userId      = userId;
        this.betValue    = betValue;
        this.inputTime   = inputTime;
        this.moneyType   = moneyType;
        this.betSideName = betSideName;
        this.isBot       = isBot;
    }

    @Override
    public String toString() {
        return "SicboBetRequest{nickname='" + nickname + "', userId=" + userId
            + ", betValue=" + betValue + ", moneyType=" + moneyType
            + ", betSideName='" + betSideName + "', isBot=" + isBot + "}";
    }
}
