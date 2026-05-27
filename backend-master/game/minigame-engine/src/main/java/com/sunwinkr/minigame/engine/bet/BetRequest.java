package com.sunwinkr.minigame.engine.bet;

/**
 * Immutable input to {@link BetAcceptor#accept}. Mirrors the parameter
 * list of {@code MGRoomTaiXiu.betTaiXiu(nickname, userId, betValue,
 * inputTime, moneyType, betSide, isBot)} (TXR:388) plus the
 * {@code isLivestream} hint that legacy code resolves from Hazelcast
 * {@code usersSetWin} before {@code betTaiXiu} runs (TXR:389).
 *
 * <p>Field types match the legacy wire signature for behavior preservation:
 * {@code short} for {@code inputTime}, {@code moneyType}, {@code betSide}.
 *
 * <p>{@code moneyType} encoding (rules-spec §2):
 * <ul>
 *   <li>{@code 0} = XU room ("xu" wallet)</li>
 *   <li>{@code 1} = VIN room ("vin" wallet)</li>
 * </ul>
 *
 * <p>{@code betSide} encoding (rules-spec §2):
 * <ul>
 *   <li>{@code 0} = XIU (Low)</li>
 *   <li>{@code 1} = TAI (High)</li>
 *   <li>Other values are silently routed to potXiu by the legacy code
 *       (TXR:391, 465) — preserved at the {@link BetAcceptor} level.</li>
 * </ul>
 *
 * <p>Plan §2.2 row B1.
 */
public final class BetRequest {

    public final String nickname;
    public final int userId;
    public final long betValue;
    public final short inputTime;
    public final short moneyType;
    public final short betSide;
    public final boolean isBot;
    public final boolean isLivestream;

    public BetRequest(String nickname,
                      int userId,
                      long betValue,
                      short inputTime,
                      short moneyType,
                      short betSide,
                      boolean isBot,
                      boolean isLivestream) {
        if (nickname == null) {
            throw new NullPointerException("nickname");
        }
        this.nickname = nickname;
        this.userId = userId;
        this.betValue = betValue;
        this.inputTime = inputTime;
        this.moneyType = moneyType;
        this.betSide = betSide;
        this.isBot = isBot;
        this.isLivestream = isLivestream;
    }

    /** {@code true} when this bet targets the Tài (High) side. */
    public boolean isTai() {
        return betSide == 1;
    }

    /** Money type as the legacy wallet string ("vin" or "xu"). */
    public String moneyTypeStr() {
        return moneyType == 1 ? "vin" : "xu";
    }
}
