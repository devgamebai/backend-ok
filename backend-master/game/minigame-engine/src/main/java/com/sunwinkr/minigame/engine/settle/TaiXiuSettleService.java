package com.sunwinkr.minigame.engine.settle;

import com.sunwinkr.minigame.engine.port.MoneyResult;
import com.sunwinkr.minigame.engine.port.TransKind;
import com.sunwinkr.minigame.engine.port.WalletPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Settle loop for the standalone TaiXiu round scheduler (SUN-1341 E1).
 *
 * <p>Mirrors {@link com.sunwinkr.lottery.engine.settle.LotterySettleService}'s
 * per-row try/catch pattern: one bet failing does NOT halt the loop.
 *
 * <h3>Payout rules (legacy MGRoomTaiXiu cadence)</h3>
 * <ul>
 *   <li>TAI wins when dice sum ∈ [11..17]; XIU wins when sum ∈ [4..10].
 *       Triples (3 or 18) count as XIU per operator convention.</li>
 *   <li>Winning payout: {@code prize = betValue + floor(betValue * (100 − TAX_PCT) / 100)}</li>
 *   <li>Losers: wallet call omitted (bet was already debited); a settle-confirmation
 *       row is written via {@link WalletPort#writeSettleConfirmationRow} when
 *       the port supports it (optional — default-method stub used otherwise).</li>
 * </ul>
 *
 * <h3>Idempotency</h3>
 * The caller ({@link com.sunwinkr.minigame.api.scheduler.TaiXiuRoundScheduler})
 * is responsible for ensuring {@code settle} is called exactly once per round.
 * The settle port's {@code markSettled(betId, roundId)} provides a second
 * line of defence at the DB layer via
 * {@link com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort}.
 *
 * <p>This class lives in {@code minigame-engine} so it remains pure POJO
 * with no Spring dependency — same contract as the engine boundary.
 *
 * <p>Plan SUN-1341 §E1.
 */
public final class TaiXiuSettleService {

    private static final Logger LOG = LoggerFactory.getLogger(TaiXiuSettleService.class);

    /** Source tag written to wallet log rows. */
    private static final String SOURCE = "TaiXiu";

    /** Game id tag for wallet log rows (legacy Games.TAI_XIU.getId()). */
    private static final long GAME_ID = 2L;

    /** Tax percent applied to the winning side prize (matches legacy default). */
    static final float TAX_PCT = 5.0f;

    private final WalletPort walletPort;
    private final TaiXiuBetSettlePort settlePort;
    private final Consumer<SettleFailureEvent> failureSink;

    /**
     * @param walletPort   adapter to credit winners
     * @param settlePort   port for persisting settle status rows
     * @param failureSink  callback for per-bet failures (null = swallow, test-friendly)
     */
    public TaiXiuSettleService(WalletPort walletPort,
                                TaiXiuBetSettlePort settlePort,
                                Consumer<SettleFailureEvent> failureSink) {
        if (walletPort == null) {
            throw new NullPointerException("walletPort");
        }
        if (settlePort == null) {
            throw new NullPointerException("settlePort");
        }
        this.walletPort  = walletPort;
        this.settlePort  = settlePort;
        this.failureSink = failureSink;
    }

    /**
     * Settle all bets for a completed round.
     *
     * @param roundId   the canonical round identifier
     * @param dice      three-element dice values [d1, d2, d3]
     * @param bets      pending bets collected during the open window
     * @return settle summary
     */
    public SettleSummary settle(long roundId, short[] dice, List<BetEntry> bets) {
        if (dice == null || dice.length < 3) {
            throw new IllegalArgumentException("dice must be short[3]");
        }
        if (bets == null) {
            throw new NullPointerException("bets");
        }
        int sum = dice[0] + dice[1] + dice[2];
        // TAI wins when sum ∈ [11..17]. XIU wins when sum ∈ [3..10] (incl. triples).
        short winSide = (sum >= 11) ? (short) 1 : (short) 0;

        int settled = 0;
        int failed  = 0;
        long totalCredited = 0L;

        for (BetEntry bet : bets) {
            try {
                settled += settleOne(roundId, bet, winSide, sum) ? 1 : 0;
                totalCredited += winSide == bet.betSide ? computePrize(bet.betValue) : 0L;
            } catch (RuntimeException e) {
                failed++;
                LOG.warn("TaiXiuSettleService: settle failed roundId={} nickname={} betSide={} betValue={}",
                         roundId, bet.nickname, bet.betSide, bet.betValue, e);
                emit(new SettleFailureEvent(roundId, bet.nickname,
                     e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }
        LOG.info("TaiXiuSettleService.settle: roundId={} dice=[{},{},{}] sum={} winSide={} " +
                 "settled={} failed={} totalCredited={}",
                 roundId, dice[0], dice[1], dice[2], sum, winSide, settled, failed, totalCredited);
        return new SettleSummary(settled, failed, totalCredited);
    }

    // -----------------------------------------------------------------------
    // Per-bet settle
    // -----------------------------------------------------------------------

    /**
     * Settle one bet. Returns true if the wallet was credited (winner).
     * Losers return false — no wallet call but settle-confirmation row written.
     */
    private boolean settleOne(long roundId, BetEntry bet, short winSide, int diceSum) {
        boolean isWinner = (bet.betSide == winSide);
        long prize = isWinner ? computePrize(bet.betValue) : 0L;
        long txId  = System.currentTimeMillis();

        // Idempotency via settle port — marks the bet row SETTLED; returns false
        // if already settled (idempotency hit).
        boolean marked;
        try {
            marked = settlePort.markSettled(bet.perBetTxId, roundId, prize);
        } catch (TaiXiuBetSettlePort.SettlePortException e) {
            throw new RuntimeException("settlePort.markSettled failed", e);
        }
        if (!marked) {
            LOG.info("TaiXiuSettleService.settleOne: already settled perBetTxId={} roundId={} — skip",
                     bet.perBetTxId, roundId);
            return false;
        }

        String moneyType = bet.moneyType == 1 ? "vin" : "xu";
        String detail = "TaiXiu sum=" + diceSum + " side=" + (bet.betSide == 1 ? "TAI" : "XIU")
                        + " roundId=" + roundId;

        if (isWinner) {
            MoneyResult credit = walletPort.credit(
                    bet.nickname,
                    prize,
                    moneyType,
                    SOURCE,
                    GAME_ID,
                    "Thắng TaiXiu (" + detail + ")",
                    0L,
                    txId,
                    TransKind.END);
            if (!credit.isSuccess()) {
                emit(new SettleFailureEvent(roundId, bet.nickname,
                     "wallet credit failed: " + credit.getErrorCode()));
                LOG.error("TaiXiuSettleService.settleOne: credit FAILED nickname={} prize={} txId={}",
                          bet.nickname, prize, txId);
            }
            return true;
        } else {
            // Losing bet — write 0-exchange settle-confirmation row so the ledger
            // is symmetric (SUN-1306 pattern: both winners and losers get 2 rows).
            try {
                walletPort.writeSettleConfirmationRow(
                        bet.nickname,
                        moneyType,
                        SOURCE,
                        "TaiXiu",
                        "Thua TaiXiu (" + detail + ")",
                        "Kết quả TaiXiu",
                        txId);
            } catch (Throwable t) {
                LOG.warn("TaiXiuSettleService.settleOne: writeSettleConfirmationRow failed " +
                         "nickname={} roundId={}", bet.nickname, roundId, t);
            }
            return false;
        }
    }

    /**
     * Prize formula for the winning side:
     * {@code prize = betValue + floor(betValue * (100 − TAX_PCT) / 100)}
     */
    public static long computePrize(long betValue) {
        return betValue + (long) ((float) betValue * (100.0f - TAX_PCT) / 100.0f);
    }

    private void emit(SettleFailureEvent ev) {
        if (failureSink != null) {
            try {
                failureSink.accept(ev);
            } catch (RuntimeException ignore) {
                // Sink failure must not interrupt the loop.
            }
        }
    }

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    /**
     * Immutable record of a single bet passed to the settle loop.
     * Decoupled from the scheduler's {@code TaiXiuRoundState.PendingBet} so
     * the engine stays Spring-free.
     */
    public static final class BetEntry {
        public final String nickname;
        public final long   roundId;
        public final long   betValue;
        public final short  betSide;
        public final short  moneyType;
        public final long   perBetTxId;

        public BetEntry(String nickname,
                        long   roundId,
                        long   betValue,
                        short  betSide,
                        short  moneyType,
                        long   perBetTxId) {
            this.nickname   = nickname;
            this.roundId    = roundId;
            this.betValue   = betValue;
            this.betSide    = betSide;
            this.moneyType  = moneyType;
            this.perBetTxId = perBetTxId;
        }
    }

    /** Settle outcome summary (mirrors LotterySettleService). */
    public static final class SettleSummary {
        public final int  settled;
        public final int  failed;
        public final long totalCredited;

        public SettleSummary(int settled, int failed, long totalCredited) {
            this.settled       = settled;
            this.failed        = failed;
            this.totalCredited = totalCredited;
        }
    }

    /** Per-bet settle failure event for ops alerting. */
    public static final class SettleFailureEvent {
        public final long   roundId;
        public final String nickname;
        public final String reason;

        public SettleFailureEvent(long roundId, String nickname, String reason) {
            this.roundId  = roundId;
            this.nickname = nickname;
            this.reason   = reason;
        }
    }

    /**
     * Port for persisting bet settle status. Implemented by
     * {@link com.sunwinkr.minigame.api.adapter.JdbcTaixiuBetSettlePort}
     * (in minigame-api). Lives here so the engine can reference it without
     * pulling Spring.
     */
    public interface TaiXiuBetSettlePort {

        /**
         * Mark a bet row SETTLED with idempotency guard.
         *
         * @param perBetTxId  the bet's wallet transaction id (used as the DB key)
         * @param roundId     round identifier for audit correlation
         * @param prize       prize amount (0 for losers)
         * @return {@code true} if the row was newly flipped PENDING→SETTLED;
         *         {@code false} if already settled (idempotency hit)
         * @throws SettlePortException on DB failure
         */
        boolean markSettled(long perBetTxId, long roundId, long prize) throws SettlePortException;

        /** DB-level failure from the settle port. */
        final class SettlePortException extends Exception {
            public SettlePortException(String msg, Throwable cause) {
                super(msg, cause);
            }
        }
    }
}
