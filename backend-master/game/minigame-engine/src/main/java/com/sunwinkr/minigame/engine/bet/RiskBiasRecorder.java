package com.sunwinkr.minigame.engine.bet;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Records risk-bias accumulators for the black-list / white-list
 * machinery (TXR:448-463). For each VIN-room real-player bet, the
 * legacy code rolls {@code rd.nextInt(100)} and, if {@code n < percent},
 * accumulates the bet value into one of four side-specific buckets
 * that the RTP balancer later consults.
 *
 * <p>Preconditions per TXR:444:
 * <ul>
 *   <li>{@code moneyType == 1} (VIN room)</li>
 *   <li>{@code !isBot} (real player only)</li>
 *   <li>The minimum-money gate is enforced by the caller via
 *       {@code blackPercent}/{@code whitePercent} being non-zero — the
 *       recorder itself is pure arithmetic and does not pull config.</li>
 * </ul>
 *
 * <p>Plan §2.2 row B6.
 */
public final class RiskBiasRecorder {

    private volatile long blackListBetTai;
    private volatile long blackListBetXiu;
    private volatile long whiteListBetTai;
    private volatile long whiteListBetXiu;

    /**
     * Roll the recorder for a single bet. No-ops if {@code moneyType != 1}
     * or {@code isBot} (matching TXR:444). The two percentage gates are
     * independent — both can fire on a single bet.
     *
     * @param bet            request whose value/side is being recorded
     * @param isBlack        {@code true} when the user is on the black list
     *                       AND the bet is &ge; the min-money threshold
     * @param isWhite        {@code true} when the user is on the white list
     *                       AND the bet is &ge; the min-money threshold
     * @param blackPercent   percentage gate for the black-list branch
     *                       (e.g. 50 → ~50% of qualifying bets get recorded)
     * @param whitePercent   percentage gate for the white-list branch
     */
    public void record(BetRequest bet,
                       boolean isBlack,
                       boolean isWhite,
                       int blackPercent,
                       int whitePercent) {
        if (bet == null) {
            return;
        }
        if (bet.moneyType != 1 || bet.isBot) {
            return;
        }
        if (isBlack && rollUnder(blackPercent)) {
            if (bet.isTai()) {
                blackListBetTai += bet.betValue;
            } else {
                blackListBetXiu += bet.betValue;
            }
        }
        if (isWhite && rollUnder(whitePercent)) {
            if (bet.isTai()) {
                whiteListBetTai += bet.betValue;
            } else {
                whiteListBetXiu += bet.betValue;
            }
        }
    }

    private static boolean rollUnder(int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < percent;
    }

    public long blackListBetTai() {
        return blackListBetTai;
    }

    public long blackListBetXiu() {
        return blackListBetXiu;
    }

    public long whiteListBetTai() {
        return whiteListBetTai;
    }

    public long whiteListBetXiu() {
        return whiteListBetXiu;
    }

    /** Reset for a new round. */
    public void renew() {
        blackListBetTai = 0L;
        blackListBetXiu = 0L;
        whiteListBetTai = 0L;
        whiteListBetXiu = 0L;
    }
}
