package com.sunwinkr.minigame.engine.bet;

/**
 * Immutable result of {@link BetAcceptor#accept}. Mirrors the
 * {@code BetTaiXiuMsg} payload (TXR:499-502): error code + wallet
 * balance, plus engine-internal handles for later auditing
 * ({@link #perBetTxId}, {@link #txDetail}).
 *
 * <p>Error code encoding per spec §2:
 * <ul>
 *   <li>{@code 0} = OK</li>
 *   <li>{@code 1} = wallet failure OR mid-call betting-disabled refund</li>
 *   <li>{@code 2} = betting closed (round LOCKED)</li>
 *   <li>{@code 3} = insufficient balance</li>
 *   <li>{@code 4} = below MIN_BET_TAI_XIU_VALUE (100)</li>
 *   <li>{@code 5} = cross-side bet attempt</li>
 *   <li>{@code 7} = bet window closed by server wall-clock (SUN-1339 §A2,
 *       errorCode {@code "0007"} on the wire — BET_WINDOW_CLOSED)</li>
 * </ul>
 *
 * <p>Plan §2.2 row B1.
 */
public final class BetAcceptResult {

    public final int errorCode;
    public final long currentMoney;
    public final long perBetTxId;
    public final TransactionTaiXiuDetail txDetail;

    private BetAcceptResult(int errorCode,
                            long currentMoney,
                            long perBetTxId,
                            TransactionTaiXiuDetail txDetail) {
        this.errorCode = errorCode;
        this.currentMoney = currentMoney;
        this.perBetTxId = perBetTxId;
        this.txDetail = txDetail;
    }

    /** Bet accepted; wallet balance reflects post-debit value. */
    public static BetAcceptResult ok(long currentMoney,
                                     long perBetTxId,
                                     TransactionTaiXiuDetail txDetail) {
        if (txDetail == null) {
            throw new NullPointerException("txDetail");
        }
        return new BetAcceptResult(0, currentMoney, perBetTxId, txDetail);
    }

    /**
     * Non-OK result. {@link #txDetail} is {@code null} for codes 2/3/4/5;
     * for code 1 (post-debit refund) it may carry the original detail so
     * audit can correlate the refund.
     */
    public static BetAcceptResult error(int code, long currentMoney) {
        if (code <= 0) {
            throw new IllegalArgumentException("error code must be positive, got " + code);
        }
        return new BetAcceptResult(code, currentMoney, 0L, null);
    }

    /** Variant for code 1 race-refund path — carries the txId of the refund. */
    public static BetAcceptResult error(int code, long currentMoney, long perBetTxId) {
        if (code <= 0) {
            throw new IllegalArgumentException("error code must be positive, got " + code);
        }
        return new BetAcceptResult(code, currentMoney, perBetTxId, null);
    }

    public boolean isOk() {
        return errorCode == 0;
    }
}
