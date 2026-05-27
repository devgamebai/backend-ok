package game.modules.minigame.utils;

import com.vinplay.vbee.common.rtp.RtpResolver;
import org.apache.log4j.Logger;

/**
 * Phase 4 — pct-aware force-side chooser for cân-cửa games (TaiXiu, XocDia, BauCua).
 *
 * Given the current bet totals per side and the configured RTP pct for a game,
 * picks the outcome whose house profit lands closest to the target house edge.
 *
 * Target house edge = (100 - pct)% of total bet.
 * Two-sided 1:1 payout model (TaiXiu, XocDia chan/le):
 *   If side A wins → house profit = betTotalB - betTotalA (gross, pre-fee).
 *
 * Kill switch: env CANCUA_USE_DYNAMIC_RTP=1. When off, returns -1 (no force, legacy).
 * On any error, returns -1 (safe fallback).
 */
public final class CanCuaRtpBalancer {
    private static final Logger logger = Logger.getLogger(CanCuaRtpBalancer.class);

    /** Return value meaning "don't force, let the RNG decide". */
    public static final short NO_FORCE = -1;

    private CanCuaRtpBalancer() {}

    /**
     * Two-sided 1:1 chooser. Subtract bots before calling for accurate real-player targeting.
     *
     * @param betSide0   total user bets on side 0 (TaiXiu: Xỉu; XocDia: chẵn)
     * @param betSide1   total user bets on side 1 (TaiXiu: Tài; XocDia: lẻ)
     * @param gameCode   RTP config key (e.g. "taixiu", "xocdia")
     * @return 0 or 1 (the side that should WIN), or -1 to skip force-bet
     */
    public static short chooseWinningSide(long betSide0, long betSide1, String gameCode) {
        if (!isEnabled()) return NO_FORCE;
        if (gameCode == null || gameCode.isEmpty()) return NO_FORCE;

        long totalBet = betSide0 + betSide1;
        if (totalBet <= 0) return NO_FORCE;

        // Tiny-pot guard — don't force-flip negligible rounds (avoids whipsaw behaviour).
        if (totalBet < 10_000L) return NO_FORCE;

        double pct;
        try {
            pct = RtpResolver.effectivePct(-1L, gameCode);
        } catch (Throwable t) {
            return NO_FORCE;
        }

        double targetEdge = 100.0 - pct;
        long targetHouseProfit = Math.round(totalBet * targetEdge / 100.0);

        // House profit scenarios (1:1 payout, gross, pre-fee):
        long profitIfSide0Wins = betSide1 - betSide0;
        long profitIfSide1Wins = betSide0 - betSide1;

        long d0 = Math.abs(profitIfSide0Wins - targetHouseProfit);
        long d1 = Math.abs(profitIfSide1Wins - targetHouseProfit);

        short chosen = (d0 <= d1) ? (short) 0 : (short) 1;
        logger.debug("CanCuaRtpBalancer " + gameCode + ": bet0=" + betSide0 + " bet1=" + betSide1
                + " pct=" + pct + " targetProfit=" + targetHouseProfit
                + " → side " + chosen + " (profit=" + (chosen == 0 ? profitIfSide0Wins : profitIfSide1Wins) + ")");
        return chosen;
    }

    /** Feature flag. */
    public static boolean isEnabled() {
        String v = System.getenv("CANCUA_USE_DYNAMIC_RTP");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }
}
