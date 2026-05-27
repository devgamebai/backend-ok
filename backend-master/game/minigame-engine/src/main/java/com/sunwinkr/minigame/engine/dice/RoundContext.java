package com.sunwinkr.minigame.engine.dice;

/**
 * Inputs to {@link DiceGenerator#generate}. Captures the pot totals,
 * tax percentage, and triggering userId needed by
 * {@link HouseEdgeDiceGenerator} without requiring direct coupling to
 * {@link com.sunwinkr.minigame.engine.core.TaiXiuRound} or
 * {@link com.sunwinkr.minigame.engine.bet.BetLedger}.
 *
 * <p>Plan §2.3 row D1.
 */
public final class RoundContext {

    /** Sum of real (non-bot) bets on Tài. */
    public final long realPotTai;
    /** Sum of real (non-bot) bets on Xỉu. */
    public final long realPotXiu;
    /** TaiXiu tax percent — {@code MINIGAME_TAX_TX = 5.0f} (MC:13). */
    public final float taxPct;
    /** UserId driving RTP override; {@code 0L} when none. */
    public final long userId;
    /** Game id for RTP resolver lookup — {@code "taixiu"} or {@code "sicbo"}. */
    public final String gameId;

    public RoundContext(long realPotTai,
                        long realPotXiu,
                        float taxPct,
                        long userId,
                        String gameId) {
        if (gameId == null) {
            throw new NullPointerException("gameId");
        }
        this.realPotTai = realPotTai;
        this.realPotXiu = realPotXiu;
        this.taxPct = taxPct;
        this.userId = userId;
        this.gameId = gameId;
    }

    /** Convenience builder for tests. */
    public static RoundContext of(long realPotTai, long realPotXiu) {
        return new RoundContext(realPotTai, realPotXiu, 5.0f, 0L, "taixiu");
    }
}
