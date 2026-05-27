package com.sunwinkr.minigame.engine.sicbo.bet;

/**
 * Immutable result of {@code SicboBetService.accept()}.
 *
 * <h3>Error codes</h3>
 * <pre>
 *   0 — success
 *   1 — wallet error (debit failed for non-insufficient-funds reason,
 *       or race re-check found betting closed after debit — refund issued)
 *   2 — betting round closed (pre-check failed: round.isBetting()==false)
 *   3 — insufficient funds (betValue &gt; currentMoney)
 *   4 — below minimum bet (betValue &lt; 100, INV-13 / SBR:545)
 *   5 — cross-side guard rejected (PRESERVED-DEAD-CODE — never triggered
 *       in practice; kept for AMBIGUOUS #3 compatibility)
 *   6 — invalid bet type (betSideName not found in SicboBetType lookup)
 *   7 — bet window closed by server wall-clock (SUN-1339 §A3,
 *       errorCode {@code "0007"} on the wire — BET_WINDOW_CLOSED)
 * </pre>
 *
 * <p>Error codes 0-5 mirror the legacy {@code BetSicboBotMsg.Error} values
 * (SBR:630). Code 6 is new (engine-only; not in the legacy path because the
 * legacy code lets {@code PotSicbo.getEnumByName()} throw NPE on unknown names
 * — AMBIGUOUS #6 — which the engine replaces with an explicit code).
 */
public final class SicboBetAcceptResult {

    /** 0 = success; see class javadoc for error codes. */
    public final int errorCode;

    /** Post-debit wallet balance; 0 on error. */
    public final long currentMoney;

    /**
     * Unique per-bet transaction ID: {@code referenceId * 1_000_000 + sequence}.
     * 0 on error (INV-12).
     */
    public final long perBetTxId;

    /**
     * Transaction code: {@code referenceId + "-" + betIndex} (INV-21).
     * {@code null} on error.
     */
    public final String transactionCode;

    /**
     * Resolved numeric bet-side ID (1..52 from {@code SicboBetType.getId()}).
     * -1 on error.
     */
    public final int betSideId;

    private SicboBetAcceptResult(int errorCode, long currentMoney, long perBetTxId,
                                  String transactionCode, int betSideId) {
        this.errorCode       = errorCode;
        this.currentMoney    = currentMoney;
        this.perBetTxId      = perBetTxId;
        this.transactionCode = transactionCode;
        this.betSideId       = betSideId;
    }

    /** Construct a successful result. */
    public static SicboBetAcceptResult success(long currentMoney, long perBetTxId,
                                                String transactionCode, int betSideId) {
        return new SicboBetAcceptResult(0, currentMoney, perBetTxId, transactionCode, betSideId);
    }

    /** Construct an error result with no balance info. */
    public static SicboBetAcceptResult error(int errorCode) {
        return new SicboBetAcceptResult(errorCode, 0L, 0L, null, -1);
    }

    /**
     * Construct an error result carrying the pre-debit balance for display
     * (SUN-1339 §A3 — BET_WINDOW_CLOSED guard runs after balance read).
     */
    public static SicboBetAcceptResult error(int errorCode, long currentMoney) {
        return new SicboBetAcceptResult(errorCode, currentMoney, 0L, null, -1);
    }

    /** {@code true} when {@link #errorCode} is 0. */
    public boolean isSuccess() {
        return errorCode == 0;
    }

    @Override
    public String toString() {
        return "SicboBetAcceptResult{errorCode=" + errorCode
            + ", currentMoney=" + currentMoney
            + ", perBetTxId=" + perBetTxId
            + ", transactionCode='" + transactionCode + "'"
            + ", betSideId=" + betSideId + "}";
    }
}
